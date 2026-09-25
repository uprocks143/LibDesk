package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.NcertBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local Data Access Object for NCERT official catalog.
 * Stores textbook metadata locally so students can browse even without connectivity.
 */
class NcertBookDao(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("libdesk_ncert_cache", Context.MODE_PRIVATE)
    private val _booksFlow = MutableStateFlow<List<NcertBook>>(loadFromDisk())

    val allBooks: Flow<List<NcertBook>> = _booksFlow.asStateFlow()

    fun filterBooks(
        classLevel: Int? = null,
        subject: String? = null,
        medium: String? = null,
        query: String? = null
    ): Flow<List<NcertBook>> {
        return _booksFlow.map { list ->
            list.filter { book ->
                val matchesClass = classLevel == null || book.classLevel == classLevel
                val matchesSubject = subject.isNullOrBlank() || subject.equals("All", ignoreCase = true) ||
                        book.subject.contains(subject, ignoreCase = true)
                val matchesMedium = medium.isNullOrBlank() || medium.equals("All", ignoreCase = true) ||
                        book.medium.equals(medium, ignoreCase = true)
                val matchesQuery = query.isNullOrBlank() ||
                        book.bookTitle.contains(query, ignoreCase = true) ||
                        book.subject.contains(query, ignoreCase = true)

                matchesClass && matchesSubject && matchesMedium && matchesQuery
            }
        }
    }

    suspend fun insertAll(books: List<NcertBook>) = withContext(Dispatchers.IO) {
        val current = _booksFlow.value.associateBy { it.id }.toMutableMap()
        for (b in books) {
            val existing = current[b.id]
            if (existing != null && existing.isDownloaded) {
                current[b.id] = b.copy(
                    isDownloaded = existing.isDownloaded,
                    localFilePath = existing.localFilePath
                )
            } else {
                current[b.id] = b
            }
        }
        val sorted = current.values.sortedWith(compareBy({ it.classLevel }, { it.subject }))
        _booksFlow.value = sorted
        saveToDisk(sorted)
    }

    suspend fun markDownloaded(bookId: String, localFilePath: String) = withContext(Dispatchers.IO) {
        val current = _materials()
        val index = current.indexOfFirst { it.id == bookId }
        if (index != -1) {
            current[index] = current[index].copy(
                isDownloaded = true,
                localFilePath = localFilePath,
                downloadProgress = 1f
            )
            _booksFlow.value = current
            saveToDisk(current)
        }
    }

    suspend fun updateDownloadProgress(bookId: String, progress: Float) = withContext(Dispatchers.IO) {
        val current = _materials()
        val index = current.indexOfFirst { it.id == bookId }
        if (index != -1) {
            current[index] = current[index].copy(
                downloadProgress = progress
            )
            _booksFlow.value = current
        }
    }

    private fun _materials(): MutableList<NcertBook> = _booksFlow.value.toMutableList()

    private fun loadFromDisk(): List<NcertBook> {
        val raw = prefs.getString("cached_ncert_books", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<NcertBook>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NcertBook(
                        id = obj.optString("id"),
                        classLevel = obj.optInt("classLevel", 10),
                        subject = obj.optString("subject"),
                        bookTitle = obj.optString("bookTitle"),
                        medium = obj.optString("medium", "english"),
                        language = obj.optString("language", "en"),
                        editionYear = obj.optString("editionYear", "2026-27"),
                        sourceName = obj.optString("sourceName", "ncert"),
                        sourceUrl = obj.optString("sourceUrl"),
                        thumbnailUrl = obj.optString("thumbnailUrl"),
                        pageCount = obj.optInt("pageCount"),
                        fileSizeBytes = obj.optLong("fileSizeBytes"),
                        isActive = obj.optBoolean("isActive", true),
                        lastVerified = obj.optString("lastVerified"),
                        createdAt = obj.optString("createdAt"),
                        updatedAt = obj.optString("updatedAt"),
                        localFilePath = obj.optString("localFilePath").takeIf { it.isNotBlank() },
                        isDownloaded = obj.optBoolean("isDownloaded", false)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToDisk(list: List<NcertBook>) {
        try {
            val array = JSONArray()
            for (b in list) {
                val obj = JSONObject().apply {
                    put("id", b.id)
                    put("classLevel", b.classLevel)
                    put("subject", b.subject)
                    put("bookTitle", b.bookTitle)
                    put("medium", b.medium)
                    put("language", b.language)
                    put("editionYear", b.editionYear)
                    put("sourceName", b.sourceName)
                    put("sourceUrl", b.sourceUrl)
                    put("thumbnailUrl", b.thumbnailUrl)
                    put("pageCount", b.pageCount)
                    put("fileSizeBytes", b.fileSizeBytes)
                    put("isActive", b.isActive)
                    put("lastVerified", b.lastVerified)
                    put("createdAt", b.createdAt)
                    put("updatedAt", b.updatedAt)
                    put("localFilePath", b.localFilePath ?: "")
                    put("isDownloaded", b.isDownloaded)
                }
                array.put(obj)
            }
            prefs.edit().putString("cached_ncert_books", array.toString()).apply()
        } catch (_: Exception) {}
    }
}
