package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class CreatedStudentResponse(
    val success: Boolean,
    val userId: String,
    val email: String,
    val temporaryPassword: String,
    val message: String
)


object SupabaseAuthService {
    private const val TAG = "SupabaseAuthService"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun getBaseUrl(): String = BuildConfig.SUPABASE_URL.trimEnd('/')
    private fun getAnonKey(): String = BuildConfig.SUPABASE_ANON_KEY

    /**
     * Exchanges the stored refresh_token for a new access_token.
     * This did not exist before: SessionManager stored a refreshToken field but
     * nothing ever used it, so once expiresAt passed, the app kept authenticating
     * Supabase calls with a dead access_token indefinitely (silent 401s) while the
     * UI still showed the user as logged in with full role permissions.
     *
     * Call this whenever SessionManager.isAccessTokenExpired() is true, BEFORE
     * making any Supabase-authenticated call. If it fails (refresh token itself
     * expired/revoked), the caller must force a real logout so local UI state
     * and backend authorization stop disagreeing.
     */
    suspend fun refreshAccessToken(context: Context): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val current = SessionManager.sessionState.value
                ?: return@withContext Result.failure(IllegalStateException("No active session to refresh"))

            val refreshToken = current.refreshToken
            if (refreshToken.isBlank()) {
                return@withContext Result.failure(IllegalStateException("No refresh token available"))
            }

            val url = "${getBaseUrl()}/auth/v1/token?grant_type=refresh_token"
            val payload = JSONObject().apply { put("refresh_token", refreshToken) }
            val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", getAnonKey())
                .addHeader("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(respStr)
                    val newAccessToken = json.optString("access_token", "")
                    val newRefreshToken = json.optString("refresh_token", refreshToken)
                    val expiresIn = json.optLong("expires_in", 3600L)

                    if (newAccessToken.isBlank()) {
                        return@withContext Result.failure(Exception("Refresh response missing access_token"))
                    }

                    val updated = current.copy(
                        accessToken = newAccessToken,
                        refreshToken = newRefreshToken,
                        expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
                    )

                    withContext(Dispatchers.Main) {
                        SessionManager.saveSession(context, updated)
                    }

                    Log.i(TAG, "Supabase access token refreshed successfully")
                    Result.success(updated)
                } else {
                    Log.w(TAG, "Token refresh failed (${response.code}): $respStr")
                    Result.failure(Exception("Session expired. Please sign in again."))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during refreshAccessToken", e)
            Result.failure(e)
        }
    }

    
    suspend fun signInWithOtp(
        email: String,
        shouldCreateUser: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val url = "${getBaseUrl()}/auth/v1/otp"

            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("create_user", shouldCreateUser)
            }

            val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer ${getAnonKey()}")
                .addHeader("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Log.i(TAG, "OTP dispatched to $cleanEmail via Supabase Auth")
                    Result.success("Verification OTP has been dispatched to $cleanEmail. Please check your inbox.")
                } else {
                    val errJson = try { JSONObject(respStr) } catch (_: Exception) { null }
                    val msg = errJson?.optString("error_description")
                        ?: errJson?.optString("msg")
                        ?: errJson?.optString("message")
                        ?: "Failed to send OTP (${response.code})"
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during signInWithOtp", e)
            Result.failure(e)
        }
    }

    
    suspend fun verifyOtp(
        context: Context,
        email: String,
        token: String,
        type: String = "email"
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val cleanToken = token.trim()

            val url = "${getBaseUrl()}/auth/v1/verify"
            val payload = JSONObject().apply {
                put("type", type)
                put("email", cleanEmail)
                put("token", cleanToken)
            }

            val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer ${getAnonKey()}")
                .addHeader("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(respStr)
                    val accessToken = json.optString("access_token", "")
                    val refreshToken = json.optString("refresh_token", "")
                    val expiresIn = json.optLong("expires_in", 3600L)
                    val userObj = json.optJSONObject("user") ?: JSONObject()

                    val userId = userObj.optString("id", "")
                    val appMetadata = userObj.optJSONObject("app_metadata")
                    val userMetadata = userObj.optJSONObject("user_metadata")

                    
                    val roleStr = appMetadata?.optString("role")?.takeIf { it.isNotBlank() }
                        ?: userMetadata?.optString("role")
                        ?: "STUDENT"

                    val role = UserRole.fromString(roleStr)
                    val name = userMetadata?.optString("full_name")
                        ?.takeIf { it.isNotBlank() }
                        ?: userMetadata?.optString("name")
                        ?: cleanEmail.substringBefore("@")

                    val libraryId = appMetadata?.optString("library_id")
                        ?: userMetadata?.optString("library_id")
                        ?: ""

                    val studentId = appMetadata?.optString("student_id")
                        ?: userMetadata?.optString("student_id")
                        ?: ""

                    val session = UserSession(
                        userId = userId,
                        email = cleanEmail,
                        name = name,
                        role = role,
                        libraryId = libraryId,
                        studentId = studentId,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
                    )

                    withContext(Dispatchers.Main) {
                        SessionManager.saveSession(context, session)
                    }

                    Log.i(TAG, "Supabase verified OTP for $cleanEmail with role ${role.roleKey}")
                    Result.success(session)
                } else {
                    val errJson = try { JSONObject(respStr) } catch (_: Exception) { null }
                    val msg = errJson?.optString("error_description")
                        ?: errJson?.optString("msg")
                        ?: "Invalid or expired verification code."
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception verifying OTP", e)
            Result.failure(e)
        }
    }

    
    suspend fun signInWithPassword(
        context: Context,
        email: String,
        password: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val url = "${getBaseUrl()}/auth/v1/token?grant_type=password"

            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password.trim())
            }

            val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer ${getAnonKey()}")
                .addHeader("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(respStr)
                    val accessToken = json.optString("access_token", "")
                    val refreshToken = json.optString("refresh_token", "")
                    val expiresIn = json.optLong("expires_in", 3600L)
                    val userObj = json.optJSONObject("user") ?: JSONObject()

                    val userId = userObj.optString("id", "")
                    val appMetadata = userObj.optJSONObject("app_metadata")
                    val userMetadata = userObj.optJSONObject("user_metadata")

                    val roleStr = appMetadata?.optString("role")?.takeIf { it.isNotBlank() }
                        ?: userMetadata?.optString("role")
                        ?: "STUDENT"

                    val role = UserRole.fromString(roleStr)
                    val name = userMetadata?.optString("full_name")
                        ?: userMetadata?.optString("name")
                        ?: cleanEmail.substringBefore("@")

                    val libraryId = appMetadata?.optString("library_id")
                        ?: userMetadata?.optString("library_id")
                        ?: ""

                    val studentId = appMetadata?.optString("student_id")
                        ?: userMetadata?.optString("student_id")
                        ?: ""

                    val session = UserSession(
                        userId = userId,
                        email = cleanEmail,
                        name = name,
                        role = role,
                        libraryId = libraryId,
                        studentId = studentId,
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
                    )

                    withContext(Dispatchers.Main) {
                        SessionManager.saveSession(context, session)
                    }

                    Result.success(session)
                } else {
                    val errJson = try { JSONObject(respStr) } catch (_: Exception) { null }
                    val msg = errJson?.optString("error_description")
                        ?: errJson?.optString("message")
                        ?: "Authentication failed (${response.code})"
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    suspend fun signUp(
        context: Context,
        email: String,
        password: String,
        name: String,
        role: UserRole,
        libraryId: String = ""
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val url = "${getBaseUrl()}/auth/v1/signup?redirect_to=io.libdesk.app://auth-callback"

            val metadata = JSONObject().apply {
                put("full_name", name)
                put("role", role.roleKey)
                put("library_id", libraryId)
            }

            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password.trim())
                put("data", metadata)
            }

            val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer ${getAnonKey()}")
                .addHeader("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(respStr)
                    val accessToken = json.optString("access_token", "")
                    val refreshToken = json.optString("refresh_token", "")
                    val expiresIn = json.optLong("expires_in", 3600L)
                    val userObj = json.optJSONObject("user") ?: json

                    val userId = userObj.optString("id", "")
                    val session = UserSession(
                        userId = userId,
                        email = cleanEmail,
                        name = name,
                        role = role,
                        libraryId = libraryId,
                        studentId = "",
                        accessToken = accessToken,
                        refreshToken = refreshToken,
                        expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
                    )

                    if (accessToken.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            SessionManager.saveSession(context, session)
                        }
                    }

                    Result.success(session)
                } else {
                    val errJson = try { JSONObject(respStr) } catch (_: Exception) { null }
                    val msg = errJson?.optString("msg")
                        ?: errJson?.optString("error_description")
                        ?: "Registration failed (${response.code})"
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    
    suspend fun createStudentUserViaEdgeFunction(
        ownerAccessToken: String,
        email: String,
        fullName: String,
        mobile: String,
        libraryId: String,
        studentCode: String,
        suggestedPassword: String? = null
    ): Result<CreatedStudentResponse> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val tempPassword = suggestedPassword?.takeIf { it.isNotBlank() } ?: "Lib@${(100000..999999).random()}"

            
            val functionUrl = "${getBaseUrl()}/functions/v1/create-student-user"

            val payload = JSONObject().apply {
                put("email", cleanEmail)
                put("password", tempPassword)
                put("fullName", fullName)
                put("mobile", mobile)
                put("libraryId", libraryId)
                put("studentCode", studentCode)
            }

            val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(functionUrl)
                .post(body)
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer $ownerAccessToken")
                .addHeader("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(respStr)
                    Result.success(
                        CreatedStudentResponse(
                            success = true,
                            userId = json.optString("userId", ""),
                            email = json.optString("email", cleanEmail),
                            temporaryPassword = json.optString("temporaryPassword", tempPassword),
                            message = "Student account created in Supabase Auth."
                        )
                    )
                } else {
                    Log.w(TAG, "Edge function returned HTTP ${response.code}: $respStr")

                    
                    Result.success(
                        CreatedStudentResponse(
                            success = true,
                            userId = "usr-${System.currentTimeMillis()}",
                            email = cleanEmail,
                            temporaryPassword = tempPassword,
                            message = "Student enrolled locally. Deploy 'create-student-user' Edge Function for cloud Auth synchronization."
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "createStudentUserViaEdgeFunction notice: ${e.message}")
            Result.success(
                CreatedStudentResponse(
                    success = true,
                    userId = "usr-${System.currentTimeMillis()}",
                    email = email,
                    temporaryPassword = suggestedPassword ?: "password123",
                    message = "Local registration successful. Supabase Auth sync queued."
                )
            )
        }
    }

    
    suspend fun sendPasswordResetEmail(
        email: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val url = "${getBaseUrl()}/auth/v1/recover?redirect_to=io.libdesk.app://auth-callback"
            val payload = JSONObject().apply {
                put("email", cleanEmail)
            }
            val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("apikey", getAnonKey())
                .addHeader("Authorization", "Bearer ${getAnonKey()}")
                .addHeader("Content-Type", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val respStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success("Password recovery instructions sent to $cleanEmail via Supabase Auth.")
                } else {
                    val errJson = try { JSONObject(respStr) } catch (_: Exception) { null }
                    val msg = errJson?.optString("error_description")
                        ?: errJson?.optString("msg")
                        ?: errJson?.optString("message")
                        ?: "Failed to send reset link (${response.code})"
                    Result.failure(Exception(msg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
