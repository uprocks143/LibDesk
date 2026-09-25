package com.example.data.remote

import android.util.Log
import com.example.data.model.NcertBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NcertCatalogDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "NcertCatalogDS"
    }

    private fun getBaseUrl(): String = SupabaseClient.getEffectiveUrl()
    private fun getApiKey(): String = SupabaseClient.getEffectiveApiKey()
    private fun getAuthToken(): String? = SupabaseClient.currentAuthToken

    /**
     * Fetches NCERT catalog books from Supabase PostgREST table ncert_catalog.
     */
    suspend fun fetchCatalog(
        classLevel: Int? = null,
        subject: String? = null,
        medium: String? = null,
        includeInactive: Boolean = false
    ): Result<List<NcertBook>> = withContext(Dispatchers.IO) {
        try {
            val queryParams = mutableListOf<String>()
            if (!includeInactive) {
                queryParams.add("is_active=eq.true")
            }
            if (classLevel != null && classLevel in 1..12) {
                queryParams.add("class_level=eq.$classLevel")
            }
            if (!subject.isNullOrBlank() && subject != "All") {
                queryParams.add("subject=ilike.*$subject*")
            }
            if (!medium.isNullOrBlank() && medium != "All") {
                queryParams.add("medium=eq.${medium.lowercase()}")
            }
            queryParams.add("order=class_level.asc,subject.asc")

            val queryString = queryParams.joinToString("&")
            val url = "${getBaseUrl()}/rest/v1/ncert_catalog?$queryString"

            val token = getAuthToken() ?: getApiKey()
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val books = mutableListOf<NcertBook>()
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        books.add(
                            NcertBook(
                                id = item.optString("id"),
                                classLevel = item.optInt("class_level"),
                                subject = item.optString("subject"),
                                bookTitle = item.optString("book_title"),
                                medium = item.optString("medium"),
                                language = item.optString("language"),
                                editionYear = item.optString("edition_year"),
                                sourceName = item.optString("source_name"),
                                sourceUrl = item.optString("source_url"),
                                thumbnailUrl = item.optString("thumbnail_url"),
                                pageCount = item.optInt("page_count"),
                                fileSizeBytes = item.optLong("file_size_bytes"),
                                isActive = item.optBoolean("is_active", true),
                                lastVerified = item.optString("last_verified"),
                                createdAt = item.optString("created_at"),
                                updatedAt = item.optString("updated_at")
                            )
                        )
                    }
                    Result.success(books)
                } else {
                    Result.failure(Exception("Failed to fetch NCERT catalog: HTTP ${response.code} $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching NCERT catalog", e)
            Result.failure(e)
        }
    }

    /**
     * Validates whether official source URL is live using a lightweight HTTP HEAD request.
     */
    suspend fun verifyUrl(sourceUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val headRequest = Request.Builder()
                .url(sourceUrl)
                .head()
                .addHeader("User-Agent", "Mozilla/5.0 LibDesk-Education-Client")
                .build()

            client.newCall(headRequest).execute().use { response ->
                val isLive = response.isSuccessful || response.code in 300..308
                Result.success(isLive)
            }
        } catch (e: Exception) {
            Log.w(TAG, "HEAD request verification failed for $sourceUrl: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Triggers Supabase Edge Function sync-ncert-catalog to refresh metadata from official government portals.
     */
    suspend fun triggerEdgeSync(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/functions/v1/sync-ncert-catalog"
            val token = getAuthToken() ?: getApiKey()

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Edge Function sync failed HTTP ${response.code} $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling sync-ncert-catalog", e)
            Result.failure(e)
        }
    }

    /**
     * Updates an NCERT book's official URL (Super Admin control).
     */
    suspend fun updateBookSourceUrl(bookId: String, newUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/rest/v1/ncert_catalog?id=eq.$bookId"
            val token = getAuthToken() ?: getApiKey()
            val payload = JSONObject().apply {
                put("source_url", newUrl)
                put("last_verified", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.US).format(java.util.Date()))
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to update source URL: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggles an NCERT book's active status (Super Admin control).
     */
    suspend fun toggleBookActive(bookId: String, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = "${getBaseUrl()}/rest/v1/ncert_catalog?id=eq.$bookId"
            val token = getAuthToken() ?: getApiKey()
            val payload = JSONObject().apply {
                put("is_active", isActive)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getApiKey())
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to toggle book active: HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
