package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Supabase Client Configuration Singleton.
 *
 * Initializes the Supabase instance using environment variables defined in .env
 * (injected into BuildConfig by the Secrets Gradle Plugin) and provides thread-safe
 * singleton access to core Supabase services like Auth, Postgrest, Realtime, and Storage.
 */
object SupabaseClientConfig {
    private const val TAG = "SupabaseClientConfig"

    // Default project credentials from .env for instant connectivity
    const val DEFAULT_SUPABASE_URL = "https://rfhqbdwctqulvwwjcsgt.supabase.co"
    const val DEFAULT_SUPABASE_ANON_KEY = "sb_publishable_ZOtDEvWWBgdFJfaqe8fKfQ_-YMshS6S"

    /**
     * Validates whether a given URL is a legitimate Supabase project URL.
     */
    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim().lowercase()
        return !trimmed.contains("your-project") &&
                !trimmed.contains("example.com") &&
                !trimmed.contains("placeholder") &&
                (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
    }

    /**
     * Validates whether a given API key is a legitimate Supabase anon/publishable key.
     */
    fun isValidKey(key: String?): Boolean {
        if (key.isNullOrBlank()) return false
        val trimmed = key.trim().lowercase()
        return !trimmed.contains("your-anon-key") &&
                !trimmed.contains("placeholder") &&
                trimmed.length > 10
    }

    /**
     * Effective project URL from .env (injected via BuildConfig.SUPABASE_URL).
     */
    val supabaseUrl: String
        get() {
            val buildUrl = try { BuildConfig.SUPABASE_URL } catch (_: Throwable) { "" }
            return if (isValidUrl(buildUrl)) buildUrl.trim().removeSuffix("/") else DEFAULT_SUPABASE_URL
        }

    /**
     * Effective API key from .env (injected via BuildConfig.SUPABASE_ANON_KEY).
     */
    val supabaseKey: String
        get() {
            val buildKey = try { BuildConfig.SUPABASE_ANON_KEY } catch (_: Throwable) { "" }
            return if (isValidKey(buildKey)) buildKey.trim() else DEFAULT_SUPABASE_ANON_KEY
        }

    @Volatile
    private var _client: SupabaseClient? = null

    /**
     * Singleton instance of the official Supabase Kotlin SDK client.
     */
    val client: SupabaseClient
        get() = _client ?: synchronized(this) {
            _client ?: createClientInstance(supabaseUrl, supabaseKey).also { _client = it }
        }

    /**
     * Singleton access to the Supabase Auth service.
     */
    val auth: Auth
        get() = client.auth

    /**
     * Singleton access to the Supabase Postgrest service (Database queries & tables).
     */
    val postgrest: Postgrest
        get() = client.postgrest

    /**
     * Singleton access to the Supabase Realtime service (Live broadcast & channels).
     */
    val realtime: Realtime
        get() = client.realtime

    /**
     * Singleton access to the Supabase Storage service (Buckets & files).
     */
    val storage: Storage
        get() = client.storage

    /**
     * Internal factory to instantiate and configure the official Supabase Kotlin SDK client.
     */
    private fun createClientInstance(url: String, key: String): SupabaseClient {
        Log.i(TAG, "Initializing Supabase Client Singleton ($url)")
        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }

    /**
     * Dynamically updates the Supabase connection parameters at runtime.
     */
    fun reconfigure(url: String, key: String) {
        synchronized(this) {
            val validUrl = if (isValidUrl(url)) url.trim().removeSuffix("/") else DEFAULT_SUPABASE_URL
            val validKey = if (isValidKey(key)) key.trim() else DEFAULT_SUPABASE_ANON_KEY
            _client = createClientInstance(validUrl, validKey)
        }
    }

    /**
     * Checks if Supabase client has valid project configuration.
     */
    fun isConfigured(): Boolean {
        return isValidUrl(supabaseUrl) && isValidKey(supabaseKey)
    }

    /**
     * Signs up a new user using email and password via the Supabase Auth client.
     *
     * @param email The user's email address
     * @param password The user's chosen password (minimum 6 characters)
     * @param fullName The user's full name (stored in Supabase auth user_metadata)
     * @param role The user's application role (e.g. "OWNER" or "STUDENT")
     * @return Result containing the created UserInfo on success, or an exception on failure
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        fullName: String = "",
        role: String = "OWNER"
    ): Result<UserInfo?> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val cleanPassword = password.trim()
            val user = auth.signUpWith(Email) {
                this.email = cleanEmail
                this.password = cleanPassword
                this.data = buildJsonObject {
                    put("full_name", fullName.trim())
                    put("name", fullName.trim())
                    put("role", role.trim())
                }
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase signUpWithEmail error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs in an existing user using email and password via the Supabase Auth client.
     *
     * @param email The user's email address
     * @param password The user's password
     * @return Result containing the authenticated UserSession on success, or an exception on failure
     */
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<UserSession?> {
        return try {
            val cleanEmail = email.trim().lowercase()
            val cleanPassword = password.trim()
            auth.signInWith(Email) {
                this.email = cleanEmail
                this.password = cleanPassword
            }
            val session = auth.currentSessionOrNull()
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase signInWithEmail error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs out the currently authenticated user from the Supabase session.
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase signOut error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Dispatches a password reset email using the Supabase Auth client.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.resetPasswordForEmail(email.trim().lowercase())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sendPasswordReset error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Returns the currently active user session, or null if unauthenticated.
     */
    fun currentSessionOrNull(): UserSession? = auth.currentSessionOrNull()

    /**
     * Returns the currently active user info, or null if unauthenticated.
     */
    fun currentUserOrNull(): UserInfo? = auth.currentUserOrNull()
}
