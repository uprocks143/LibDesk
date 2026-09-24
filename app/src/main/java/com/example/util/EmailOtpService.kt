package com.example.util

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.remote.SessionManager
import com.example.data.remote.SupabaseClient
import com.example.data.remote.UserRole
import com.example.data.remote.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class OtpPurpose {
    SIGNUP_VERIFICATION,
    PASSWORD_RESET,
    SUPER_ADMIN_2FA
}

data class EmailDispatchResult(
    val isSuccess: Boolean,
    val message: String,
    val maskedEmail: String
)


object EmailOtpService {
    private const val TAG = "EmailOtpService"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val username = parts[0]
        val domain = parts[1]
        val maskedUser = when {
            username.length <= 2 -> "${username.first()}***"
            username.length <= 4 -> "${username.take(1)}***${username.takeLast(1)}"
            else -> "${username.take(2)}***${username.takeLast(2)}"
        }
        return "$maskedUser@$domain"
    }

    
    fun dispatchEmailOtp(
        email: String,
        recipientName: String,
        purpose: OtpPurpose,
        scope: CoroutineScope,
        onComplete: (EmailDispatchResult) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()

        scope.launch(Dispatchers.IO) {
            var isSuccess = false
            var message = ""

            try {
                val supabaseUrl = SupabaseClient.getEffectiveUrl()
                val supabaseAnonKey = SupabaseClient.getEffectiveApiKey()

                val targetEndpoint = when (purpose) {
                    OtpPurpose.PASSWORD_RESET -> "$supabaseUrl/auth/v1/recover"
                    else -> "$supabaseUrl/auth/v1/otp"
                }

                val payload = JSONObject().apply {
                    put("email", cleanEmail)
                    if (purpose == OtpPurpose.SIGNUP_VERIFICATION || purpose == OtpPurpose.SUPER_ADMIN_2FA) {
                        put("create_user", true)
                    }
                }

                val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
                val request = Request.Builder()
                    .url(targetEndpoint)
                    .post(requestBody)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Content-Type", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val respStr = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        isSuccess = true
                        message = "Verification OTP has been sent to ${maskEmail(cleanEmail)}. Please check your inbox & spam folder."
                        Log.i(TAG, "Supabase Auth dispatched OTP to $cleanEmail (HTTP ${response.code})")
                    } else {
                        val errJson = try { JSONObject(respStr) } catch (_: Exception) { null }
                        message = errJson?.optString("error_description")
                            ?: errJson?.optString("msg")
                            ?: "Failed to send OTP (${response.code})"
                        Log.w(TAG, "Supabase Auth OTP dispatch error: $respStr")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network exception during dispatchEmailOtp", e)
                message = "Network error connecting to Supabase Auth: ${e.message}"
            }

            withContext(Dispatchers.Main) {
                onComplete(
                    EmailDispatchResult(
                        isSuccess = isSuccess,
                        message = message,
                        maskedEmail = maskEmail(cleanEmail)
                    )
                )
            }
        }
    }

    
    fun verifyOtp(
        context: Context,
        email: String,
        enteredOtp: String,
        purpose: OtpPurpose,
        scope: CoroutineScope? = null,
        onVerified: (Boolean, String?) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()
        val cleanInput = enteredOtp.trim()

        if (cleanInput.length < 6) {
            onVerified(false, "Verification code must be at least 6 digits.")
            return
        }

        val coroutineScope = scope ?: CoroutineScope(Dispatchers.IO)
        coroutineScope.launch(Dispatchers.IO) {
            var supabaseVerified = false
            var errorMessage: String? = null

            try {
                val supabaseUrl = SupabaseClient.getEffectiveUrl()
                val supabaseAnonKey = SupabaseClient.getEffectiveApiKey()

                val typesToTest = when (purpose) {
                    OtpPurpose.PASSWORD_RESET -> listOf("recovery", "email")
                    OtpPurpose.SIGNUP_VERIFICATION -> listOf("signup", "email")
                    OtpPurpose.SUPER_ADMIN_2FA -> listOf("email", "magiclink", "signup")
                }

                for (type in typesToTest) {
                    val payload = JSONObject().apply {
                        put("type", type)
                        put("email", cleanEmail)
                        put("token", cleanInput)
                    }

                    val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
                    val request = Request.Builder()
                        .url("$supabaseUrl/auth/v1/verify")
                        .post(requestBody)
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .addHeader("Content-Type", "application/json")
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        val respStr = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            supabaseVerified = true
                            val json = JSONObject(respStr)
                            val accessToken = json.optString("access_token", "")
                            val refreshToken = json.optString("refresh_token", "")
                            val userObj = json.optJSONObject("user") ?: JSONObject()

                            val userId = userObj.optString("id", "")
                            val appMetadata = userObj.optJSONObject("app_metadata")
                            val userMetadata = userObj.optJSONObject("user_metadata")

                            val roleStr = appMetadata?.optString("role")?.takeIf { it.isNotBlank() }
                                ?: userMetadata?.optString("role")
                                ?: when (purpose) {
                                    OtpPurpose.SUPER_ADMIN_2FA -> "OWNER"
                                    else -> "STUDENT"
                                }

                            val role = UserRole.fromString(roleStr)
                            val name = userMetadata?.optString("full_name") ?: cleanEmail.substringBefore("@")

                            val session = UserSession(
                                userId = userId,
                                email = cleanEmail,
                                name = name,
                                role = role,
                                accessToken = accessToken,
                                refreshToken = refreshToken,
                                expiresAt = System.currentTimeMillis() + 3600_000
                            )

                            withContext(Dispatchers.Main) {
                                SessionManager.saveSession(context, session)
                            }
                            Log.i(TAG, "Supabase verified OTP for $cleanEmail with role ${role.roleKey}")
                            return@use
                        } else {
                            val errJson = try { JSONObject(respStr) } catch (_: Exception) { null }
                            errorMessage = errJson?.optString("error_description")
                                ?: errJson?.optString("msg")
                                ?: "Invalid or expired verification code."
                        }
                    }
                    if (supabaseVerified) break
                }
            } catch (e: Exception) {
                Log.e(TAG, "Supabase verify error", e)
                errorMessage = "Connection error: ${e.message}"
            }

            withContext(Dispatchers.Main) {
                onVerified(supabaseVerified, errorMessage)
            }
        }
    }

    
    fun verifyOtpSync(email: String, enteredOtp: String, purpose: OtpPurpose): Boolean {
        val cleanEmail = email.trim().lowercase()
        val cleanInput = enteredOtp.trim()
        if (cleanInput.length < 6) return false

        return try {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                val supabaseUrl = SupabaseClient.getEffectiveUrl()
                val supabaseAnonKey = SupabaseClient.getEffectiveApiKey()

                val typesToTest = when (purpose) {
                    OtpPurpose.PASSWORD_RESET -> listOf("recovery", "email")
                    OtpPurpose.SIGNUP_VERIFICATION -> listOf("signup", "email")
                    OtpPurpose.SUPER_ADMIN_2FA -> listOf("email", "magiclink", "signup")
                }

                for (type in typesToTest) {
                    val payload = JSONObject().apply {
                        put("type", type)
                        put("email", cleanEmail)
                        put("token", cleanInput)
                    }
                    val request = Request.Builder()
                        .url("$supabaseUrl/auth/v1/verify")
                        .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .addHeader("Content-Type", "application/json")
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            return@runBlocking true
                        }
                    }
                }
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
