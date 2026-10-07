package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit


object SupabaseClient {
    private const val TAG = "SupabaseClient"

    
    const val DEFAULT_PROJECT_URL = ""
    const val DEFAULT_PUBLISHABLE_KEY = ""

    fun isConfigured(): Boolean {
        return isValidUrl(getEffectiveUrl()) && isValidKey(getEffectiveApiKey())
    }

    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim().lowercase()
        return !trimmed.contains("your-project") &&
                !trimmed.contains("example.com") &&
                !trimmed.contains("placeholder") &&
                (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
    }

    fun isValidKey(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        val trimmed = key.trim().lowercase()
        return !trimmed.contains("your-anon-key") &&
                !trimmed.contains("placeholder") &&
                trimmed.length > 10
    }

    fun getEffectiveUrl(): String {
        val configured = projectUrl
        if (isValidUrl(configured)) return configured.trimEnd('/')

        val buildUrl = try { BuildConfig.SUPABASE_URL } catch (_: Throwable) { "" }
        if (isValidUrl(buildUrl)) return buildUrl.trimEnd('/')

        return ""
    }

    fun getEffectiveApiKey(): String {
        val configured = apiKey
        if (isValidKey(configured)) return configured

        val buildKey = try { BuildConfig.SUPABASE_ANON_KEY } catch (_: Throwable) { "" }
        if (isValidKey(buildKey)) return buildKey

        return ""
    }

    var projectUrl: String = ""
        private set

    var apiKey: String = ""
        private set

    var currentAuthToken: String? = null

    fun getEffectiveToken(): String? {
        val direct = currentAuthToken?.takeIf { it.isNotBlank() }
        if (direct != null) return direct
        val sessionToken = SessionManager.sessionState.value?.accessToken?.takeIf { it.isNotBlank() }
        if (sessionToken != null) {
            currentAuthToken = sessionToken
            return sessionToken
        }
        val prefToken = SessionManager.restoreSessionFromPrefs()?.accessToken?.takeIf { it.isNotBlank() }
        if (prefToken != null) {
            currentAuthToken = prefToken
            return prefToken
        }
        return null
    }

    fun getEffectiveAuthHeader(): String {
        val token = getEffectiveToken()
        return "Bearer ${token ?: apiKey}"
    }

    var currentUserEmail: String? = null
        private set

    var currentUserName: String? = null
        private set

    var isUserLoggedIn: Boolean = false
        private set

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    init {
        try {
            val urlField = BuildConfig::class.java.getDeclaredField("SUPABASE_URL")
            val urlVal = urlField.get(null) as? String
            if (isValidUrl(urlVal)) {
                projectUrl = urlVal!!.trim().removeSuffix("/")
            } else {
                projectUrl = ""
            }
        } catch (_: Exception) {
            projectUrl = ""
        }

        try {
            val keyField = BuildConfig::class.java.getDeclaredField("SUPABASE_ANON_KEY")
            val keyVal = keyField.get(null) as? String
            if (isValidKey(keyVal)) {
                apiKey = keyVal!!.trim()
            } else {
                apiKey = ""
            }
        } catch (_: Exception) {
            apiKey = ""
        }
    }

    val sdkClient: io.github.jan.supabase.SupabaseClient
        get() = SupabaseConfig.client

    fun updateConfig(url: String, key: String) {
        if (url.isNotBlank()) projectUrl = url.trim().removeSuffix("/")
        if (key.isNotBlank()) apiKey = key.trim()
        SupabaseConfig.reconfigure(projectUrl, apiKey)
    }

    fun setLoggedInUser(email: String, name: String, token: String) {
        currentUserEmail = email
        currentUserName = name
        currentAuthToken = token
        isUserLoggedIn = true
    }

    fun logout() {
        currentAuthToken = null
        currentUserEmail = null
        currentUserName = null
        isUserLoggedIn = false
    }

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Pair(false, "No Supabase project keys configured yet.")
        try {
            val request = Request.Builder()
                .url("${getEffectiveUrl()}/rest/v1/")
                .addHeader("apikey", getEffectiveApiKey())
                .addHeader("Authorization", getEffectiveAuthHeader())
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200 || response.code == 404 || response.code == 204) {
                    Pair(true, "Connected to Supabase successfully (${response.code})")
                } else if (response.code == 401) {
                    Pair(false, "Authentication Failed (401): Check Supabase Anon / Publishable Key")
                } else {
                    Pair(true, "Supabase Server Reachable (HTTP ${response.code})")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to connect to Supabase", e)
            Pair(false, "Connection error: ${e.localizedMessage ?: "Network unreachable"}")
        } catch (e: Exception) {
            Pair(false, "Error: ${e.localizedMessage}")
        }
    }

    suspend fun upsertRecords(tableName: String, jsonArray: JSONArray): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (jsonArray.length() == 0) return@withContext Pair(true, "0 records to sync")
        if (!isConfigured()) return@withContext Pair(true, "Local storage mode active (awaiting new Supabase keys)")
        val rawTableName = tableName.substringBefore("?")
        val authHeader = getEffectiveAuthHeader()

        val conflictParam = if (tableName.contains("on_conflict")) "" else (if (tableName.contains("?")) "&on_conflict=id" else "?on_conflict=id")
        val targetUrl = "${getEffectiveUrl()}/rest/v1/$tableName$conflictParam"

        try {
            val body = jsonArray.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", authHeader)
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(body)
                .build()

            var success = false
            var responseBody = ""
            httpClient.newCall(request).execute().use { response ->
                responseBody = response.body?.string() ?: ""
                if (response.isSuccessful || response.code in 200..204) {
                    success = true
                } else {
                    Log.w(TAG, "Bulk upsert to $rawTableName ($targetUrl) status ${response.code}: $responseBody")
                }
            }

            if (success) {
                return@withContext Pair(true, "Synced ${jsonArray.length()} records to $rawTableName")
            }

            // Resilient per-record fallback (handle PATCH / POST individually)
            var syncedCount = 0
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "").takeIf { it.isNotBlank() }
                    ?: obj.optString("studentId", "").takeIf { it.isNotBlank() }
                val itemBody = obj.toString().toRequestBody(JSON_MEDIA_TYPE)

                var itemSuccess = false
                // Try PATCH if ID exists
                if (id != null) {
                    try {
                        val patchRequest = Request.Builder()
                            .url("$projectUrl/rest/v1/$rawTableName?id=eq.$id")
                            .addHeader("apikey", apiKey)
                            .addHeader("Authorization", authHeader)
                            .addHeader("Prefer", "return=minimal")
                            .patch(itemBody)
                            .build()
                        httpClient.newCall(patchRequest).execute().use { patchResp ->
                            if (patchResp.isSuccessful || patchResp.code in 200..204) {
                                itemSuccess = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Patch item $id error: ${e.message}")
                    }
                }

                // If not updated via PATCH, try POST insert
                if (!itemSuccess) {
                    try {
                        val postRequest = Request.Builder()
                            .url("$projectUrl/rest/v1/$rawTableName")
                            .addHeader("apikey", apiKey)
                            .addHeader("Authorization", authHeader)
                            .addHeader("Prefer", "return=minimal")
                            .post(itemBody)
                            .build()
                        httpClient.newCall(postRequest).execute().use { postResp ->
                            if (postResp.isSuccessful || postResp.code in 200..204) {
                                itemSuccess = true
                            } else {
                                val err = postResp.body?.string() ?: ""
                                Log.e(TAG, "Insert item fallback status ${postResp.code}: $err")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Post item error: ${e.message}")
                    }
                }

                if (itemSuccess) syncedCount++
            }

            if (syncedCount > 0) {
                Pair(true, "Synced $syncedCount of ${jsonArray.length()} records to $rawTableName")
            } else {
                Pair(false, "Table '$rawTableName' error: ${responseBody.take(120)}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error upserting to $rawTableName", e)
            Pair(false, "Failed to sync $rawTableName: ${e.message}")
        }
    }

    suspend fun fetchRecords(tableName: String, libraryId: String? = null): Pair<Boolean, JSONArray?> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Pair(true, JSONArray())
        try {
            var url = "${getEffectiveUrl()}/rest/v1/$tableName?select=*"
            if (!libraryId.isNullOrBlank()) {
                url += "&libraryId=eq.$libraryId"
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getEffectiveApiKey())
                .addHeader("Authorization", getEffectiveAuthHeader())
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val array = JSONArray(respBody)
                    Pair(true, array)
                } else {
                    Log.e(TAG, "Fetch from $tableName failed (${response.code}): $respBody")
                    Pair(false, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from $tableName", e)
            Pair(false, null)
        }
    }

    suspend fun queryTable(pathWithQuery: String): Pair<Boolean, JSONArray?> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Pair(true, JSONArray())
        try {
            val url = if (pathWithQuery.startsWith("http")) pathWithQuery else "${getEffectiveUrl()}/rest/v1/$pathWithQuery"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getEffectiveApiKey())
                .addHeader("Authorization", getEffectiveAuthHeader())
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val array = JSONArray(respBody)
                    Pair(true, array)
                } else {
                    Log.w(TAG, "queryTable failed (${response.code}) for $pathWithQuery: $respBody")
                    Pair(false, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying $pathWithQuery", e)
            Pair(false, null)
        }
    }

    suspend fun deleteRecord(tableName: String, id: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext Pair(true, "Local delete completed")
        try {
            val url = "${getEffectiveUrl()}/rest/v1/$tableName?id=eq.$id"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", getEffectiveApiKey())
                .addHeader("Authorization", getEffectiveAuthHeader())
                .delete()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "Deleted successfully")
                } else {
                    val respBody = response.body?.string() ?: ""
                    Pair(false, "Delete failed (${response.code}): $respBody")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Exception deleting record: ${e.message}")
        }
    }
    fun getRecommendedSqlSchema(): String = ""
}
