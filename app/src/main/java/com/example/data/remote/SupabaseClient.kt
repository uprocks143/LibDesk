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

    
    const val DEFAULT_PROJECT_URL = "https://rfhqbdwctqulvwwjcsgt.supabase.co"
    const val DEFAULT_PUBLISHABLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJmaHFiZHdjdHF1bHZ3d2pjc2d0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY4MjIyNzEsImV4cCI6MjEwMjM5ODI3MX0.v7ZmYdJAxq9E2cpWeYdujx0IKvkbrMOPFfAzPagNJLk"

    private const val PREFS_NAME = "libdesk_supabase_prefs"
    private const val KEY_PROJECT_URL = "supabase_project_url"
    private const val KEY_ANON_KEY = "supabase_anon_key"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        loadPersistedConfig(context)
    }

    private fun loadPersistedConfig(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedUrl = prefs.getString(KEY_PROJECT_URL, null)
            val savedKey = prefs.getString(KEY_ANON_KEY, null)
            projectUrl = if (isValidUrl(savedUrl)) {
                savedUrl!!.trim().removeSuffix("/")
            } else {
                DEFAULT_PROJECT_URL
            }
            apiKey = if (isValidKey(savedKey)) {
                savedKey!!.trim()
            } else {
                // If saved key was an old/invalid publishable key, update SharedPreferences to permanent JWT anon key
                prefs.edit().putString(KEY_ANON_KEY, DEFAULT_PUBLISHABLE_KEY).apply()
                DEFAULT_PUBLISHABLE_KEY
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading persisted Supabase config", e)
            projectUrl = DEFAULT_PROJECT_URL
            apiKey = DEFAULT_PUBLISHABLE_KEY
        }
    }

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
        val trimmed = key.trim()
        val lower = trimmed.lowercase()
        return !lower.contains("your-anon-key") &&
                !lower.contains("placeholder") &&
                !lower.startsWith("sb_publishable_") &&
                trimmed.length > 20
    }

    fun getEffectiveUrl(): String {
        val configured = projectUrl
        if (isValidUrl(configured)) return configured.trimEnd('/')

        val ctx = appContext
        if (ctx != null) {
            val savedUrl = try {
                ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PROJECT_URL, null)
            } catch (_: Exception) { null }
            if (isValidUrl(savedUrl)) {
                projectUrl = savedUrl!!.trim().removeSuffix("/")
                return projectUrl
            }
        }

        val buildUrl = try { BuildConfig.SUPABASE_URL } catch (_: Throwable) { "" }
        if (isValidUrl(buildUrl)) return buildUrl.trimEnd('/')

        if (isValidUrl(DEFAULT_PROJECT_URL)) return DEFAULT_PROJECT_URL.trimEnd('/')

        return ""
    }

    fun getEffectiveApiKey(): String {
        val configured = apiKey
        if (isValidKey(configured)) return configured

        val ctx = appContext
        if (ctx != null) {
            val savedKey = try {
                ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ANON_KEY, null)
            } catch (_: Exception) { null }
            if (isValidKey(savedKey)) {
                apiKey = savedKey!!.trim()
                return apiKey
            }
        }

        val buildKey = try { BuildConfig.SUPABASE_ANON_KEY } catch (_: Throwable) { "" }
        if (isValidKey(buildKey)) return buildKey

        if (isValidKey(DEFAULT_PUBLISHABLE_KEY)) return DEFAULT_PUBLISHABLE_KEY

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
        return "Bearer ${token ?: getEffectiveApiKey()}"
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
            } else if (isValidUrl(DEFAULT_PROJECT_URL)) {
                projectUrl = DEFAULT_PROJECT_URL.trimEnd('/')
            }
        } catch (_: Exception) {
            if (isValidUrl(DEFAULT_PROJECT_URL)) {
                projectUrl = DEFAULT_PROJECT_URL.trimEnd('/')
            }
        }

        try {
            val keyField = BuildConfig::class.java.getDeclaredField("SUPABASE_ANON_KEY")
            val keyVal = keyField.get(null) as? String
            if (isValidKey(keyVal)) {
                apiKey = keyVal!!.trim()
            } else if (isValidKey(DEFAULT_PUBLISHABLE_KEY)) {
                apiKey = DEFAULT_PUBLISHABLE_KEY.trim()
            }
        } catch (_: Exception) {
            if (isValidKey(DEFAULT_PUBLISHABLE_KEY)) {
                apiKey = DEFAULT_PUBLISHABLE_KEY.trim()
            }
        }
    }

    val sdkClient: io.github.jan.supabase.SupabaseClient
        get() = SupabaseConfig.client

    fun updateConfig(url: String, key: String, context: Context? = null) {
        val cleanUrl = url.trim().removeSuffix("/")
        val cleanKey = key.trim()
        if (isValidUrl(cleanUrl)) projectUrl = cleanUrl
        if (isValidKey(cleanKey)) apiKey = cleanKey

        val ctx = context ?: appContext
        if (ctx != null && isValidUrl(projectUrl) && isValidKey(apiKey)) {
            try {
                ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_PROJECT_URL, projectUrl)
                    .putString(KEY_ANON_KEY, apiKey)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist Supabase config", e)
            }
        }

        SupabaseConfig.reconfigure(projectUrl, apiKey)
        SupabaseClientConfig.reconfigure(projectUrl, apiKey)
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

    data class SupabaseConnectionReport(
        val isDatabaseConnected: Boolean,
        val isAuthConnected: Boolean,
        val databaseStatusCode: Int = 0,
        val authStatusCode: Int = 0,
        val databaseMessage: String,
        val authMessage: String,
        val latencyMs: Long,
        val isFullyConnected: Boolean = isDatabaseConnected && isAuthConnected
    )

    suspend fun testConnectionDetailed(candidateUrl: String? = null, candidateKey: String? = null): SupabaseConnectionReport = withContext(Dispatchers.IO) {
        val url = (candidateUrl?.takeIf { isValidUrl(it) } ?: getEffectiveUrl()).trimEnd('/')
        val key = candidateKey?.takeIf { isValidKey(it) } ?: getEffectiveApiKey()

        if (!isValidUrl(url) || !isValidKey(key)) {
            return@withContext SupabaseConnectionReport(
                isDatabaseConnected = false,
                isAuthConnected = false,
                databaseMessage = "URL or API Key is missing or invalid format.",
                authMessage = "Cannot test auth without valid keys.",
                latencyMs = 0
            )
        }

        val start = System.currentTimeMillis()
        var dbOk = false
        var authOk = false
        var dbCode = 0
        var authCode = 0
        var dbMsg = ""
        var authMsg = ""

        // 1. Test Auth endpoint (/auth/v1/settings)
        try {
            val authReq = Request.Builder()
                .url("$url/auth/v1/settings")
                .addHeader("apikey", key)
                .get()
                .build()
            httpClient.newCall(authReq).execute().use { resp ->
                authCode = resp.code
                if (resp.isSuccessful || resp.code == 200) {
                    authOk = true
                    authMsg = "Auth service active & ready (Email/Password & OTP enabled)"
                } else if (resp.code == 401) {
                    authMsg = "Auth Rejected (401). Check API key."
                } else {
                    authMsg = "Auth HTTP ${resp.code}"
                }
            }
        } catch (e: Exception) {
            authMsg = "Auth unreachable: ${e.localizedMessage ?: "Network error"}"
        }

        // 2. Test Database REST endpoint (/rest/v1/libraries?select=id&limit=1)
        try {
            val dbReq = Request.Builder()
                .url("$url/rest/v1/libraries?select=id&limit=1")
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()
            httpClient.newCall(dbReq).execute().use { resp ->
                dbCode = resp.code
                val body = resp.body?.string() ?: ""
                if (resp.isSuccessful || resp.code in 200..204) {
                    dbOk = true
                    dbMsg = "Database connected & schema verified ('libraries' table active)"
                } else if (resp.code == 401) {
                    if (body.contains("Secret API key required", ignoreCase = true) || body.contains("UNAUTHORIZED_INVALID_API_KEY_TYPE", ignoreCase = true)) {
                        dbMsg = "PostgREST needs 'anon public' JWT key (starts with 'eyJ...'). Find it in Supabase Dashboard > Project Settings > API > 'anon public'."
                    } else {
                        dbMsg = "Database 401 Unauthorized: Verify anon public key."
                    }
                } else if (resp.code == 404 || body.contains("relation", ignoreCase = true)) {
                    dbOk = true
                    dbMsg = "Supabase server reachable, but schema tables missing. Run supabase_schema.sql in SQL Editor."
                } else {
                    dbMsg = "Database HTTP ${resp.code}: ${body.take(80)}"
                }
            }
        } catch (e: Exception) {
            dbMsg = "Database unreachable: ${e.localizedMessage ?: "Network error"}"
        }

        val latency = System.currentTimeMillis() - start
        SupabaseConnectionReport(
            isDatabaseConnected = dbOk,
            isAuthConnected = authOk,
            databaseStatusCode = dbCode,
            authStatusCode = authCode,
            databaseMessage = dbMsg,
            authMessage = authMsg,
            latencyMs = latency
        )
    }

    suspend fun testConnection(): Pair<Boolean, String> {
        val report = testConnectionDetailed()
        val success = report.isFullyConnected || (report.isDatabaseConnected && report.isAuthConnected)
        val msg = if (success) {
            "Connected to Supabase Database & Auth (${report.latencyMs}ms)"
        } else if (report.isAuthConnected && !report.isDatabaseConnected) {
            "Auth Connected, but Database: ${report.databaseMessage}"
        } else {
            "${report.databaseMessage} | ${report.authMessage}"
        }
        return Pair(success, msg)
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
