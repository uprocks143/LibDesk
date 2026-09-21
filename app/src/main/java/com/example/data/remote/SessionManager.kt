package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit


enum class UserRole(val roleKey: String, val title: String) {
    OWNER("OWNER", "Institute Owner"),
    ADMIN("ADMIN", "Branch Admin"),
    STUDENT("STUDENT", "Student Member");

    companion object {
        fun fromString(value: String?): UserRole {
            return when (value?.trim()?.uppercase()) {
                "OWNER", "SUPER_ADMIN", "MASTER_ADMIN" -> OWNER
                "ADMIN", "MANAGER", "STAFF" -> ADMIN
                "STUDENT", "MEMBER" -> STUDENT
                else -> STUDENT
            }
        }
    }
}


data class UserSession(
    val userId: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val libraryId: String = "",
    val studentId: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAt: Long = 0L
)


object SessionManager {
    private const val TAG = "SessionManager"
    private const val PREFS_NAME = "libdesk_supabase_auth_session"

    private const val KEY_USER_ID = "auth_user_id"
    private const val KEY_EMAIL = "auth_email"
    private const val KEY_NAME = "auth_name"
    private const val KEY_ROLE = "auth_role"
    private const val KEY_LIBRARY_ID = "auth_library_id"
    private const val KEY_STUDENT_ID = "auth_student_id"
    private const val KEY_ACCESS_TOKEN = "auth_access_token"
    private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
    private const val KEY_EXPIRES_AT = "auth_expires_at"

    private var prefs: SharedPreferences? = null

    private val _sessionState = MutableStateFlow<UserSession?>(null)
    val sessionState: StateFlow<UserSession?> = _sessionState.asStateFlow()

    private val _currentRole = MutableStateFlow<UserRole?>(null)
    val currentRole: StateFlow<UserRole?> = _currentRole.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * True when the current access token is expired (or about to expire within
     * [bufferMillis]). Previously nothing ever checked this — the app kept using
     * a dead token forever, so the UI would show the user as fully authenticated
     * with a role while every real Supabase-backed call (sync, edge functions,
     * fetchCurrentUserFromSupabase) silently failed with 401. That mismatch was
     * the root of the "logged in but permission denied" conflict.
     */
    fun isAccessTokenExpired(bufferMillis: Long = 60_000L): Boolean {
        val expiresAt = _sessionState.value?.expiresAt ?: return false
        if (expiresAt <= 0L) return false
        return System.currentTimeMillis() >= (expiresAt - bufferMillis)
    }

    fun hasRefreshToken(): Boolean = !_sessionState.value?.refreshToken.isNullOrBlank()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            restoreSessionFromPrefs()
        }
    }

    private fun getPrefs(context: Context? = null): SharedPreferences? {
        if (prefs == null && context != null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return prefs
    }

    
    fun restoreSessionFromPrefs(): UserSession? {
        val p = prefs ?: return null
        val token = p.getString(KEY_ACCESS_TOKEN, null)
        val email = p.getString(KEY_EMAIL, null)
        val userId = p.getString(KEY_USER_ID, null)

        if (token.isNullOrBlank() || email.isNullOrBlank() || userId.isNullOrBlank()) {
            _sessionState.value = null
            _currentRole.value = null
            return null
        }

        val roleStr = p.getString(KEY_ROLE, "STUDENT")
        val role = UserRole.fromString(roleStr)
        val name = p.getString(KEY_NAME, "") ?: ""
        val libraryId = p.getString(KEY_LIBRARY_ID, "") ?: ""
        val studentId = p.getString(KEY_STUDENT_ID, "") ?: ""
        val refreshToken = p.getString(KEY_REFRESH_TOKEN, "") ?: ""
        val expiresAt = p.getLong(KEY_EXPIRES_AT, 0L)

        val session = UserSession(
            userId = userId,
            email = email,
            name = name,
            role = role,
            libraryId = libraryId,
            studentId = studentId,
            accessToken = token,
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )

        _sessionState.value = session
        _currentRole.value = role

        
        SupabaseClient.setLoggedInUser(email, name, token)
        return session
    }

    
    fun saveSession(context: Context, session: UserSession) {
        val p = getPrefs(context)
        p?.edit()
            ?.putString(KEY_USER_ID, session.userId)
            ?.putString(KEY_EMAIL, session.email)
            ?.putString(KEY_NAME, session.name)
            ?.putString(KEY_ROLE, session.role.roleKey)
            ?.putString(KEY_LIBRARY_ID, session.libraryId)
            ?.putString(KEY_STUDENT_ID, session.studentId)
            ?.putString(KEY_ACCESS_TOKEN, session.accessToken)
            ?.putString(KEY_REFRESH_TOKEN, session.refreshToken)
            ?.putLong(KEY_EXPIRES_AT, session.expiresAt)
            ?.apply()

        _sessionState.value = session
        _currentRole.value = session.role
        SupabaseClient.setLoggedInUser(session.email, session.name, session.accessToken)
    }

    
    fun logout(context: Context) {
        val p = getPrefs(context)
        val token = _sessionState.value?.accessToken
        p?.edit()?.clear()?.apply()
        _sessionState.value = null
        _currentRole.value = null
        SupabaseClient.logout()

        
        if (!token.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
                    val anonKey = BuildConfig.SUPABASE_ANON_KEY
                    val request = Request.Builder()
                        .url("$supabaseUrl/auth/v1/logout")
                        .post("{}".toRequestBody(JSON_MEDIA_TYPE))
                        .addHeader("apikey", anonKey)
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                    httpClient.newCall(request).execute().close()
                } catch (e: Exception) {
                    Log.w(TAG, "Supabase remote logout notice: ${e.message}")
                }
            }
        }
    }

    
    suspend fun fetchCurrentUserFromSupabase(context: Context, tokenOverride: String? = null): Result<UserSession> =
        withContext(Dispatchers.IO) {
            try {
                val token = tokenOverride ?: _sessionState.value?.accessToken
                if (token.isNullOrBlank()) {
                    return@withContext Result.failure(IllegalStateException("No active access token"))
                }

                val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
                val anonKey = BuildConfig.SUPABASE_ANON_KEY

                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/user")
                    .get()
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val respStr = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Supabase getUser failed (${response.code}): $respStr"))
                    }

                    val userObj = JSONObject(respStr)
                    val userId = userObj.optString("id", "")
                    val email = userObj.optString("email", "")

                    
                    val appMetadata = userObj.optJSONObject("app_metadata")
                    val userMetadata = userObj.optJSONObject("user_metadata")

                    val roleStr = appMetadata?.optString("role")?.takeIf { it.isNotBlank() }
                        ?: userMetadata?.optString("role")
                        ?: "STUDENT"

                    val role = UserRole.fromString(roleStr)

                    val name = userMetadata?.optString("full_name")
                        ?.takeIf { it.isNotBlank() }
                        ?: userMetadata?.optString("name")
                        ?: email.substringBefore("@")

                    val libraryId = appMetadata?.optString("library_id")
                        ?.takeIf { it.isNotBlank() }
                        ?: userMetadata?.optString("library_id")
                        ?: _sessionState.value?.libraryId ?: ""

                    val studentId = appMetadata?.optString("student_id")
                        ?.takeIf { it.isNotBlank() }
                        ?: userMetadata?.optString("student_id")
                        ?: _sessionState.value?.studentId ?: ""

                    val session = UserSession(
                        userId = userId,
                        email = email,
                        name = name,
                        role = role,
                        libraryId = libraryId,
                        studentId = studentId,
                        accessToken = token,
                        refreshToken = _sessionState.value?.refreshToken ?: "",
                        expiresAt = _sessionState.value?.expiresAt ?: (System.currentTimeMillis() + 3600_000)
                    )

                    withContext(Dispatchers.Main) {
                        saveSession(context, session)
                    }

                    Result.success(session)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user from Supabase", e)
                Result.failure(e)
            }
        }

    
    fun handleAuthCallback(
        context: Context,
        uri: Uri,
        onComplete: (Boolean, UserSession?, String?) -> Unit
    ) {
        val fragment = uri.fragment ?: ""
        val query = uri.query ?: ""

        val params = mutableMapOf<String, String>()

        fun parseString(s: String) {
            s.split("&").forEach { pair ->
                val kv = pair.split("=", limit = 2)
                if (kv.size == 2) {
                    params[kv[0]] = Uri.decode(kv[1])
                } else if (kv.size == 1) {
                    params[kv[0]] = ""
                }
            }
        }

        if (fragment.isNotBlank()) parseString(fragment)
        if (query.isNotBlank()) parseString(query)

        val accessToken = params["access_token"]
        val refreshToken = params["refresh_token"] ?: ""
        val expiresIn = params["expires_in"]?.toLongOrNull() ?: 3600L
        val code = params["code"]

        if (!accessToken.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                val result = fetchCurrentUserFromSupabase(context, accessToken)
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { session ->
                            val updatedSession = session.copy(
                                refreshToken = refreshToken,
                                expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
                            )
                            saveSession(context, updatedSession)
                            onComplete(true, updatedSession, null)
                        },
                        onFailure = { err ->
                            onComplete(false, null, err.localizedMessage ?: "Auth callback failed")
                        }
                    )
                }
            }
        } else if (!code.isNullOrBlank()) {
            onComplete(false, null, "PKCE Flow not supported via email links. Use OTP or disable PKCE in Supabase settings.")
        } else {
            val errorDesc = params["error_description"] ?: params["error"]
            onComplete(false, null, errorDesc?.replace("+", " ") ?: "Invalid link. No access token provided.")
        }
    }
}
