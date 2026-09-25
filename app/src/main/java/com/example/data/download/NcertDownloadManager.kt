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
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    data class Progress(val percentage: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data class Success(val file: File, val contentUri: Uri) : DownloadState()
    data class Error(val message: String, val throwable: Throwable? = null) : DownloadState()
}

class NcertDownloadManager(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    companion object {
        private const val TAG = "NcertDownloadMgr"
    }

    /**
     * Returns destination file path in app-specific external storage:
     * context.getExternalFilesDir("ncert/{classLevel}/{subject}/")
     */
    fun getLocalFileForBook(book: NcertBook): File {
        val sanitizedSubject = book.subject.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val sanitizedTitle = book.bookTitle.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val dir = File(context.getExternalFilesDir(null), "ncert/class_${book.classLevel}/$sanitizedSubject")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "${sanitizedTitle}_${book.medium}.pdf")
    }

    /**
     * Checks if textbook file is already downloaded and valid.
     */
    fun isBookDownloaded(book: NcertBook): Boolean {
        val file = getLocalFileForBook(book)
        return file.exists() && file.length() > 1024L
    }

    /**
     * Returns content Uri for sharing or reading using FileProvider.
     */
    fun getFileUri(file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Downloads PDF directly from official NCERT / ePathshala server.
     * Supports resume if partial download exists.
     * Emits DownloadState flow.
     */
    fun downloadBook(book: NcertBook): Flow<DownloadState> = callbackFlow {
        val destinationFile = getLocalFileForBook(book)
        val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.download")

        var existingBytes = 0L
        if (tempFile.exists()) {
            existingBytes = tempFile.length()
        }

        val requestBuilder = Request.Builder()
            .url(book.sourceUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Android; LibDesk-Academic)")

        if (existingBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=$existingBytes-")
        }

        val call = okHttpClient.newCall(requestBuilder.build())

        try {
            val response = call.execute()
            if (!response.isSuccessful && response.code != 206) {
                // If range request failed (e.g. server doesn't support Range), retry clean
                if (existingBytes > 0) {
                    tempFile.delete()
                    existingBytes = 0L
                    val cleanCall = okHttpClient.newCall(
                        Request.Builder()
                            .url(book.sourceUrl)
                            .addHeader("User-Agent", "Mozilla/5.0 (Android; LibDesk-Academic)")
                            .build()
                    )
                    val retryResponse = cleanCall.execute()
                    if (!retryResponse.isSuccessful) {
                        trySend(DownloadState.Error("Official server returned HTTP ${retryResponse.code}"))
                        close()
                        return@callbackFlow
                    }
                    processResponseBody(retryResponse, tempFile, destinationFile, 0L)
                } else {
                    trySend(DownloadState.Error("Download failed: Official server returned HTTP ${response.code}"))
                    close()
                    return@callbackFlow
                }
            } else {
                processResponseBody(response, tempFile, destinationFile, existingBytes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for ${book.bookTitle}", e)
            trySend(DownloadState.Error("Download interrupted: ${e.localizedMessage ?: "Network error"}", e))
        }

        awaitClose {
            if (!call.isCanceled()) {
                call.cancel()
            }
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
        val totalBytes = if (contentLength > 0) contentLength + existingBytes else bookEstimatedSize(totalBytesEstimate = 15 * 1024 * 1024L)

        val output = RandomAccessFile(tempFile, "rw")
        output.seek(existingBytes)

        val inputStream: InputStream = body.byteStream()
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        var downloadedBytes = existingBytes
        var lastEmittedPercent = -1

        try {
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

            output.close()
            inputStream.close()

            // Rename tempFile to destinationFile
            if (destinationFile.exists()) destinationFile.delete()
            if (tempFile.renameTo(destinationFile)) {
                trySend(DownloadState.Progress(100, downloadedBytes, totalBytes))
                val uri = getFileUri(destinationFile)
                trySend(DownloadState.Success(destinationFile, uri))
            } else {
                trySend(DownloadState.Error("Failed to finalize downloaded file."))
            }
            close()
        } catch (e: Exception) {
            output.close()
            inputStream.close()
            throw e
        }
    }

    private fun bookEstimatedSize(totalBytesEstimate: Long): Long = totalBytesEstimate

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
