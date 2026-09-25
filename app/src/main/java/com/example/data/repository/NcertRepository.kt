package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.download.DownloadState
import com.example.data.download.NcertDownloadManager
import com.example.data.local.NcertBookDao
import com.example.data.model.NcertBook
import com.example.data.remote.NcertCatalogDataSource
import com.example.data.remote.SupabaseStorageDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class NcertRepository(
    private val context: Context,
    private val catalogDataSource: NcertCatalogDataSource = NcertCatalogDataSource(),
    private val dao: NcertBookDao = NcertBookDao(context),
    private val downloadManager: NcertDownloadManager = NcertDownloadManager(context),
    private val storageDataSource: SupabaseStorageDataSource = SupabaseStorageDataSource(context)
) {
    companion object {
        private const val TAG = "NcertRepository"
    }

    val allBooks: Flow<List<NcertBook>> = dao.allBooks

    fun filterCatalog(
        classLevel: Int? = null,
        subject: String? = null,
        medium: String? = null,
        query: String? = null
    ): Flow<List<NcertBook>> = dao.filterBooks(classLevel, subject, medium, query)

    /**
     * Refreshes NCERT catalog from Supabase PostgreSQL table in the background.
     * Keeps local offline cache updated.
     */
    suspend fun refreshCatalog(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = catalogDataSource.fetchCatalog()
            if (result.isSuccess) {
                val books = result.getOrNull() ?: emptyList()
                dao.insertAll(books)
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to fetch catalog"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing NCERT catalog", e)
            Result.failure(e)
        }
    }

    /**
     * Verifies that the official URL is alive with a fast HEAD request.
     */
    suspend fun verifySourceUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        val result = catalogDataSource.verifyUrl(url)
        result.getOrDefault(true)
    }

    /**
     * Checks if a textbook has already been downloaded onto this device.
     */
    fun isDownloaded(book: NcertBook): Boolean = downloadManager.isBookDownloaded(book)

    /**
     * Gets the local file instance for reading if downloaded.
     */
    fun getDownloadedFile(book: NcertBook): File? {
        val file = downloadManager.getLocalFileForBook(book)
        return if (file.exists() && file.length() > 1024L) file else null
    }

    /**
     * Starts downloading book directly from official government server.
     */
    fun downloadBook(book: NcertBook): Flow<DownloadState> {
        return downloadManager.downloadBook(book)
    }

    /**
     * Marks book as downloaded in local database cache.
     */
    suspend fun markBookDownloaded(bookId: String, file: File) {
        dao.markDownloaded(bookId, file.absolutePath)
    }

    /**
     * Updates download progress in local state.
     */
    suspend fun updateDownloadProgress(bookId: String, progress: Float) {
        dao.updateDownloadProgress(bookId, progress)
    }

    /**
     * Logs download to Supabase download_logs table for audit/reporting.
     */
    suspend fun logNcertDownload(studentId: String, bookId: String) = withContext(Dispatchers.IO) {
        storageDataSource.logDownload(studentId, bookId, "NCERT_OFFICIAL")
    }

    /**
     * Triggers Edge Function sync for Super Admin.
     */
    suspend fun triggerSyncFromOfficial(): Result<String> {
        return catalogDataSource.triggerEdgeSync()
    }

    /**
     * Updates official source URL (Super Admin).
     */
    suspend fun updateSourceUrl(bookId: String, newUrl: String): Result<Unit> {
        return catalogDataSource.updateBookSourceUrl(bookId, newUrl)
    }

    /**
     * Toggles book active status (Super Admin).
     */
    suspend fun toggleActive(bookId: String, isActive: Boolean): Result<Unit> {
        return catalogDataSource.toggleBookActive(bookId, isActive)
    }
}
