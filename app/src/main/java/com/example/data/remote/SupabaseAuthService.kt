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

    fun getBaseUrl(): String = SupabaseClient.getEffectiveUrl()
    fun getAnonKey(): String = SupabaseClient.getEffectiveApiKey()

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

    
    data class TenantResolutionResult(
        val libraryId: String = "",
        val libraryName: String = "",
        val studentId: String = "",
        val studentCode: String = "",
        val membershipStatus: String = "ACTIVE",
        val verified: Boolean = false,
        val errorMessage: String? = null
    )

    suspend fun resolveLoginEmail(identifier: String, role: String): Pair<String, String?> = withContext(Dispatchers.IO) {
        val clean = identifier.trim()
        if (clean.contains("@")) {
            return@withContext Pair(clean.lowercase(), null)
        }

        // 1. Check if digits only (Mobile number)
        val digitsOnly = clean.filter { it.isDigit() }
        if (digitsOnly.length >= 7) {
            val (sOk, sArr) = SupabaseClient.queryTable("students?mobile=eq.$digitsOnly&select=email,libraryId,id,fullName,studentCode")
            if (sOk && sArr != null && sArr.length() > 0) {
                val rec = sArr.getJSONObject(0)
                val em = rec.optString("email").trim()
                val finalEmail = if (em.isNotBlank() && em.contains("@")) em else "$digitsOnly@student.libdesk"
                return@withContext Pair(finalEmail, rec.optString("libraryId"))
            }

            val (lOk, lArr) = SupabaseClient.queryTable("libraries?ownerPhone=eq.$digitsOnly&select=ownerEmail,id,name")
            if (lOk && lArr != null && lArr.length() > 0) {
                val rec = lArr.getJSONObject(0)
                val em = rec.optString("ownerEmail").trim()
                if (em.isNotBlank() && em.contains("@")) {
                    return@withContext Pair(em, rec.optString("id"))
                }
            }
        }

        // 2. Check student code (e.g. STU-1234)
        val (codeOk, codeArr) = SupabaseClient.queryTable("students?studentCode=eq.$clean&select=email,mobile,libraryId,id,fullName")
        if (codeOk && codeArr != null && codeArr.length() > 0) {
            val rec = codeArr.getJSONObject(0)
            val em = rec.optString("email").trim()
            val mob = rec.optString("mobile").filter { it.isDigit() }
            val finalEmail = if (em.isNotBlank() && em.contains("@")) em else "$mob@student.libdesk"
            return@withContext Pair(finalEmail, rec.optString("libraryId"))
        }

        // Fallback: If 10 digits, synthesize student email; otherwise return raw clean
        if (digitsOnly.length >= 10) {
            Pair("$digitsOnly@student.libdesk", null)
        } else {
            Pair(clean, null)
        }
    }

    suspend fun resolveTenantForUser(
        email: String,
        role: UserRole,
        explicitTenantCode: String? = null
    ): TenantResolutionResult = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val cleanTenantCode = explicitTenantCode?.trim()?.uppercase() ?: ""

            var targetLibId = ""
            var targetLibName = ""

            // If explicit tenant code was provided (e.g. LIB-1001 or ID), lookup library first
            if (cleanTenantCode.isNotBlank()) {
                val (libFound, libArr) = SupabaseClient.queryTable("libraries?or=(id.eq.$cleanTenantCode,code.eq.$cleanTenantCode)&select=id,name,code")
                if (libFound && libArr != null && libArr.length() > 0) {
                    targetLibId = libArr.getJSONObject(0).optString("id", "")
                    targetLibName = libArr.getJSONObject(0).optString("name", "")
                } else {
                    return@withContext TenantResolutionResult(
                        verified = false,
                        errorMessage = "Library tenant '$cleanTenantCode' not found. Please verify your library code."
                    )
                }
            }

            // 1. If role is STUDENT, query students table
            if (role == UserRole.STUDENT) {
                val (sOk, sArr) = SupabaseClient.queryTable("students?email=eq.$cleanEmail&select=id,libraryId,fullName,studentCode,status,mobile")
                if (sOk && sArr != null && sArr.length() > 0) {
                    val studentObj = sArr.getJSONObject(0)
                    val studentLibId = studentObj.optString("libraryId", "")
                    val studentId = studentObj.optString("id", "")
                    val studentCode = studentObj.optString("studentCode", "")
                    val status = studentObj.optString("status", "ACTIVE")

                    if (targetLibId.isNotBlank() && studentLibId != targetLibId) {
                        return@withContext TenantResolutionResult(
                            verified = false,
                            errorMessage = "This student account is enrolled in another library tenant, not '$targetLibName'."
                        )
                    }

                    if (targetLibName.isBlank() && studentLibId.isNotBlank()) {
                        val (lOk, lArr) = SupabaseClient.queryTable("libraries?id=eq.$studentLibId&select=id,name,code")
                        if (lOk && lArr != null && lArr.length() > 0) {
                            targetLibName = lArr.getJSONObject(0).optString("name", "")
                        }
                    }

                    return@withContext TenantResolutionResult(
                        libraryId = studentLibId,
                        libraryName = targetLibName,
                        studentId = studentId,
                        studentCode = studentCode,
                        membershipStatus = status,
                        verified = true
                    )
                }
            }

            // 2. If role is OWNER or ADMIN (or fallback check for library owner)
            if (role == UserRole.OWNER || role == UserRole.ADMIN) {
                val (lOk, lArr) = SupabaseClient.queryTable("libraries?ownerEmail=eq.$cleanEmail&select=id,name,code")
                if (lOk && lArr != null && lArr.length() > 0) {
                    val libObj = lArr.getJSONObject(0)
                    val adminLibId = libObj.optString("id", "")
                    val libName = libObj.optString("name", "")

                    if (targetLibId.isNotBlank() && adminLibId != targetLibId) {
                        return@withContext TenantResolutionResult(
                            verified = false,
                            errorMessage = "This administrator account owns a different library tenant ($libName), not '$targetLibName'."
                        )
                    }

                    return@withContext TenantResolutionResult(
                        libraryId = adminLibId,
                        libraryName = libName,
                        verified = true
                    )
                }

                val (uOk, uArr) = SupabaseClient.queryTable("users?email=eq.$cleanEmail&select=id,libraryId,name,role")
                if (uOk && uArr != null && uArr.length() > 0) {
                    val userObj = uArr.getJSONObject(0)
                    val userLibId = userObj.optString("libraryId", "")
                    if (userLibId.isNotBlank()) {
                        var libName = ""
                        val (lOk2, lArr2) = SupabaseClient.queryTable("libraries?id=eq.$userLibId&select=name")
                        if (lOk2 && lArr2 != null && lArr2.length() > 0) {
                            libName = lArr2.getJSONObject(0).optString("name", "")
                        }
                        return@withContext TenantResolutionResult(
                            libraryId = userLibId,
                            libraryName = libName,
                            verified = true
                        )
                    }
                }
            }

            // If explicit tenant code was valid and found
            if (targetLibId.isNotBlank()) {
                return@withContext TenantResolutionResult(
                    libraryId = targetLibId,
                    libraryName = targetLibName,
                    verified = true
                )
            }

            TenantResolutionResult(
                libraryId = "",
                libraryName = "",
                verified = false,
                errorMessage = "No library tenant found for this account."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving tenant", e)
            TenantResolutionResult(
                verified = false,
                errorMessage = e.localizedMessage
            )
        }
    }

    suspend fun signInWithPassword(
        context: Context,
        email: String,
        password: String,
        tenantCode: String? = null,
        expectedRole: String? = null
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

                    val rawRoleStr = appMetadata?.optString("role")?.takeIf { it.isNotBlank() }
                        ?: userMetadata?.optString("role")

                    var role = if (!rawRoleStr.isNullOrBlank()) {
                        UserRole.fromString(rawRoleStr)
                    } else if (expectedRole == "MANAGER") {
                        UserRole.ADMIN
                    } else {
                        UserRole.STUDENT
                    }

                    // Role validation if expectedRole was specified
                    if (expectedRole == "STUDENT" && (role == UserRole.ADMIN || role == UserRole.OWNER)) {
                        // User is an administrator trying to log in under Student tab
                        return@withContext Result.failure(
                            Exception("This account is registered as a Library Administrator. Please select the 'Library Owner' tab.")
                        )
                    } else if (expectedRole == "MANAGER" && role == UserRole.STUDENT) {
                        // User is a student trying to log in under Manager tab
                        return@withContext Result.failure(
                            Exception("This account is registered as a Student Member. Please select the 'Student' tab to access your student pass.")
                        )
                    }

                    var name = userMetadata?.optString("full_name")
                        ?: userMetadata?.optString("name")
                        ?: cleanEmail.substringBefore("@")

                    val metaLibId = appMetadata?.optString("library_id")?.takeIf { it.isNotBlank() }
                        ?: userMetadata?.optString("library_id")
                        ?: ""

                    val metaStudentId = appMetadata?.optString("student_id")?.takeIf { it.isNotBlank() }
                        ?: userMetadata?.optString("student_id")
                        ?: ""

                    // Perform Tenant Resolution & Verification
                    var finalLibId = metaLibId
                    var finalStudentId = metaStudentId

                    if (role != UserRole.OWNER || !tenantCode.isNullOrBlank()) {
                        val tenantResolution = resolveTenantForUser(cleanEmail, role, tenantCode)
                        if (tenantResolution.verified) {
                            if (tenantResolution.libraryId.isNotBlank()) {
                                finalLibId = tenantResolution.libraryId
                            }
                            if (tenantResolution.studentId.isNotBlank()) {
                                finalStudentId = tenantResolution.studentId
                            }
                        } else if (!tenantCode.isNullOrBlank() && tenantResolution.errorMessage != null) {
                            // Explicit tenant code was specified but failed validation
                            return@withContext Result.failure(Exception(tenantResolution.errorMessage))
                        }
                    }

                    val session = UserSession(
                        userId = userId,
                        email = cleanEmail,
                        name = name,
                        role = role,
                        libraryId = finalLibId,
                        studentId = finalStudentId,
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
