package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

/**
 * Supabase SDK Configuration Object.
 * Initializes the official Supabase Kotlin SDK with the URL and API Key
 * provided via environment variables (BuildConfig / .env).
 */
object SupabaseConfig {
    private const val TAG = "SupabaseConfig"

    const val DEFAULT_PROJECT_URL = "https://rfhqbdwctqulvwwjcsgt.supabase.co"
    const val DEFAULT_PUBLISHABLE_KEY = "sb_publishable_ZOtDEvWWBgdFJfaqe8fKfQ_-YMshS6S"

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

    val supabaseUrl: String by lazy {
        val url = try {
            BuildConfig.SUPABASE_URL
        } catch (_: Throwable) {
            ""
        }
        if (isValidUrl(url)) url.trim().removeSuffix("/") else DEFAULT_PROJECT_URL
    }

    val supabaseKey: String by lazy {
        val key = try {
            BuildConfig.SUPABASE_ANON_KEY
        } catch (_: Throwable) {
            ""
        }
        if (isValidKey(key)) key.trim() else DEFAULT_PUBLISHABLE_KEY
    }

    @Volatile
    private var _client: SupabaseClient? = null

    /**
     * Singleton instance of the official Supabase Kotlin SDK client.
     */
    val client: SupabaseClient
        get() = _client ?: synchronized(this) {
            _client ?: createClient(supabaseUrl, supabaseKey).also { _client = it }
        }

    val postgrest: Postgrest
        get() = client.postgrest

    val auth: Auth
        get() = client.auth

    val realtime: Realtime
        get() = client.realtime

    val storage: Storage
        get() = client.storage

    /**
     * Creates and configures a new Supabase Kotlin SDK client with core plugins.
     */
    fun createClient(url: String, key: String): SupabaseClient {
        Log.i(TAG, "Initializing Supabase Kotlin SDK Client ($url)")
        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Postgrest)
            install(Auth)
            install(Realtime)
            install(Storage)
        }
    }

    /**
     * Reconfigures the Supabase client dynamically if updated at runtime.
     */
    fun reconfigure(url: String, key: String) {
        synchronized(this) {
            val validUrl = if (isValidUrl(url)) url.trim().removeSuffix("/") else DEFAULT_PROJECT_URL
            val validKey = if (isValidKey(key)) key.trim() else DEFAULT_PUBLISHABLE_KEY
            _client = createClient(validUrl, validKey)
        }
    }
}
