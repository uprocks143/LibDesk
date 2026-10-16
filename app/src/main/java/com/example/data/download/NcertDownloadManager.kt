package com.example.data.download

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.NcertBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

sealed class DownloadState {
    data class Progress(val percentage: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data class Success(val file: File, val contentUri: Uri) : DownloadState()
    data class Error(val message: String, val throwable: Throwable? = null) : DownloadState()
}

class NcertDownloadManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "NcertDownloadMgr"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        private const val REFERER = "https://ncert.nic.in/textbook.php"
        // Minimum size of an authentic NCERT textbook chapter PDF (typically 1.5MB to 20MB, always > 120KB)
        private const val MIN_VALID_NCERT_PDF_BYTES = 120_000L
    }

    /**
     * Builds an HTTP/1.1 client with permissive TLS for Indian government educational portals (.nic.in),
     * which often experience TLS resets on HTTP/2 (ALPN h2) and require relaxed certificate validation.
     */
    private val okHttpClient: OkHttpClient = createPermissiveOkHttpClient()

    private fun createPermissiveOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )

            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, trustAllCerts, SecureRandom())
            }

            OkHttpClient.Builder()
                // CRITICAL: ncert.nic.in sends connection reset on HTTP/2. HTTP/1.1 is strictly required!
                .protocols(listOf(Protocol.HTTP_1_1))
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize permissive SSL context, falling back to standard client", e)
            OkHttpClient.Builder()
                .protocols(listOf(Protocol.HTTP_1_1))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }
    }

    /**
     * Returns destination file path in app-specific external storage:
     * context.getExternalFilesDir("ncert/class_{classLevel}/{subject}/")
     */
    fun getLocalFileForBook(book: NcertBook, chapterSuffix: String? = null): File {
        val sanitizedSubject = book.subject.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val sanitizedTitle = book.bookTitle.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(baseDir, "ncert/class_${book.classLevel}/$sanitizedSubject")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val chapterPart = if (!chapterSuffix.isNullOrBlank()) "_ch_$chapterSuffix" else ""
        return File(dir, "${sanitizedTitle}_${book.medium}$chapterPart.pdf")
    }

    /**
     * Validates whether a file is a genuine PDF by inspecting the %PDF magic header
     * and ensuring it has realistic textbook length.
     */
    private fun isAuthenticPdf(file: File): Boolean {
        if (!file.exists() || file.length() < MIN_VALID_NCERT_PDF_BYTES) {
            return false
        }
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(5)
                val read = input.read(header)
                read >= 4 &&
                        header[0] == '%'.code.toByte() &&
                        header[1] == 'P'.code.toByte() &&
                        header[2] == 'D'.code.toByte() &&
                        header[3] == 'F'.code.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if textbook file is already downloaded and is an authentic original PDF.
     * Deletes leftover fake/truncated placeholder files (< 120KB) so the real book will download.
     */
    fun isBookDownloaded(book: NcertBook, chapterSuffix: String? = null): Boolean {
        val file = getLocalFileForBook(book, chapterSuffix)
        if (file.exists() && file.length() < MIN_VALID_NCERT_PDF_BYTES) {
            // Delete old synthetic placeholder or interrupted download
            try { file.delete() } catch (_: Exception) {}
            return false
        }
        return isAuthenticPdf(file)
    }

    /**
     * Returns content Uri for sharing or reading using FileProvider.
     */
    fun getFileUri(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Computes the download URL for a given book and chapter suffix.
     * e.g., if book.sourceUrl is https://ncert.nic.in/textbook/pdf/jemh101.pdf and chapterSuffix is "02",
     * this returns https://ncert.nic.in/textbook/pdf/jemh102.pdf.
     */
    fun resolveDownloadUrl(book: NcertBook, chapterSuffix: String? = null): String {
        if (chapterSuffix.isNullOrBlank()) {
            return book.sourceUrl
        }
        val match = Regex("""([a-zA-Z]{4,6}\d)""", RegexOption.IGNORE_CASE).find(book.sourceUrl)
        return if (match != null) {
            val code = match.value.lowercase()
            "https://ncert.nic.in/textbook/pdf/$code$chapterSuffix.pdf"
        } else {
            book.sourceUrl
        }
    }

    /**
     * Downloads the original authentic PDF directly from the official NCERT / ePathshala server.
     * Emits DownloadState flow.
     */
    fun downloadBook(book: NcertBook, chapterSuffix: String? = null): Flow<DownloadState> = callbackFlow {
        val destinationFile = getLocalFileForBook(book, chapterSuffix)

        // Check if genuine original PDF is already on device
        if (isAuthenticPdf(destinationFile)) {
            trySend(DownloadState.Progress(100, destinationFile.length(), destinationFile.length()))
            val uri = getFileUri(destinationFile)
            trySend(DownloadState.Success(destinationFile, uri))
            close()
            return@callbackFlow
        } else if (destinationFile.exists()) {
            // Delete corrupt or synthetic small placeholder
            try { destinationFile.delete() } catch (_: Exception) {}
        }

        val parentDir = destinationFile.parentFile ?: (context.getExternalFilesDir(null) ?: context.filesDir)
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }
        val tempFile = File(parentDir, "${destinationFile.name}.download")

        var existingBytes = 0L
        if (tempFile.exists()) {
            existingBytes = tempFile.length()
        }

        val targetUrl = resolveDownloadUrl(book, chapterSuffix)

        var activeCall: okhttp3.Call? = null

        fun buildRequest(url: String, rangeFrom: Long): Request {
            val builder = Request.Builder()
                .url(url)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Referer", REFERER)
                .addHeader("Accept", "application/pdf,*/*")
            if (rangeFrom > 0) {
                builder.addHeader("Range", "bytes=$rangeFrom-")
            }
            return builder.build()
        }

        var downloadSucceeded = false

        // Attempt 1: Target URL with HTTP/1.1
        try {
            val request = buildRequest(targetUrl, existingBytes)
            val call = okHttpClient.newCall(request)
            activeCall = call
            val response = call.execute()

            if (response.isSuccessful || response.code == 206) {
                processResponseBody(response, tempFile, destinationFile, existingBytes)
                downloadSucceeded = true
            } else {
                Log.w(TAG, "Primary URL HTTP ${response.code} for ${book.bookTitle}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Primary attempt error for ${book.bookTitle}: ${e.message}")
        }

        // Attempt 2: If HTTPS gave an error or non-200, try cleartext HTTP fallback
        if (!downloadSucceeded) {
            val fallbackUrl = when {
                targetUrl.startsWith("https://") -> targetUrl.replace("https://", "http://")
                targetUrl.startsWith("http://") -> targetUrl.replace("http://", "https://")
                else -> null
            }
            if (fallbackUrl != null) {
                try {
                    val request = buildRequest(fallbackUrl, 0L)
                    val call = okHttpClient.newCall(request)
                    activeCall = call
                    val response = call.execute()
                    if (response.isSuccessful || response.code == 206) {
                        processResponseBody(response, tempFile, destinationFile, 0L)
                        downloadSucceeded = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Fallback attempt error for ${book.bookTitle}: ${e.message}")
                }
            }
        }

        if (!downloadSucceeded) {
            trySend(DownloadState.Error("Could not download original NCERT book from official server. Please check internet connection or view via Official Portal tab."))
            close()
        }

        awaitClose {
            try {
                activeCall?.cancel()
            } catch (_: Exception) {}
        }
    }

    private suspend fun kotlinx.coroutines.channels.ProducerScope<DownloadState>.processResponseBody(
        response: okhttp3.Response,
        tempFile: File,
        destinationFile: File,
        existingBytes: Long
    ) {
        val body = response.body
        if (body == null) {
            trySend(DownloadState.Error("Empty response body from official server"))
            close()
            return
        }

        val contentLength = body.contentLength()
        val totalBytes = if (contentLength > 0) contentLength + existingBytes else 4L * 1024L * 1024L

        val output = RandomAccessFile(tempFile, "rw")
        val inputStream: InputStream = body.byteStream()
        val buffer = ByteArray(32 * 1024)
        var bytesRead: Int
        var downloadedBytes = existingBytes
        var lastEmittedPercent = -1

        try {
            output.seek(existingBytes)
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                if (totalBytes > 0) {
                    val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                    if (percent != lastEmittedPercent) {
                        lastEmittedPercent = percent
                        trySend(DownloadState.Progress(percent, downloadedBytes, totalBytes))
                    }
                }
            }
        } finally {
            try { output.close() } catch (_: Exception) {}
            try { inputStream.close() } catch (_: Exception) {}
        }

        destinationFile.parentFile?.mkdirs()
        if (destinationFile.exists()) destinationFile.delete()

        val copySuccess = try {
            tempFile.copyTo(destinationFile, overwrite = true)
            tempFile.delete()
            true
        } catch (_: Exception) {
            tempFile.renameTo(destinationFile)
        }

        if (copySuccess && destinationFile.exists() && destinationFile.length() > 0L) {
            trySend(DownloadState.Progress(100, downloadedBytes, totalBytes))
            val uri = getFileUri(destinationFile)
            trySend(DownloadState.Success(destinationFile, uri))
        } else {
            trySend(DownloadState.Error("Failed to finalize downloaded textbook file."))
        }
        close()
    }

    /**
     * Clears all cached NCERT files if storage cleanup is requested.
     */
    suspend fun clearNcertCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(context.getExternalFilesDir(null), "ncert")
            if (rootDir.exists()) {
                rootDir.deleteRecursively()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
