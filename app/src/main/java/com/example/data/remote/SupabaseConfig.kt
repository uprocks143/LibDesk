package com.example.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Supabase SDK Configuration Object.
 * Delegates to the unified [SupabaseClientConfig] singleton.
 */
object SupabaseConfig {
    const val DEFAULT_PROJECT_URL = SupabaseClientConfig.DEFAULT_SUPABASE_URL
    const val DEFAULT_PUBLISHABLE_KEY = SupabaseClientConfig.DEFAULT_SUPABASE_ANON_KEY

    fun isValidUrl(url: String?): Boolean = SupabaseClientConfig.isValidUrl(url)
    fun isValidKey(key: String?): Boolean = SupabaseClientConfig.isValidKey(key)

    val supabaseUrl: String
        get() = SupabaseClientConfig.supabaseUrl

    val supabaseKey: String
        get() = SupabaseClientConfig.supabaseKey

    /**
     * Singleton instance of the official Supabase Kotlin SDK client.
     */
    val client: SupabaseClient
        get() = SupabaseClientConfig.client

    val postgrest: Postgrest
        get() = SupabaseClientConfig.postgrest

    val auth: Auth
        get() = SupabaseClientConfig.auth

    val realtime: Realtime
        get() = SupabaseClientConfig.realtime

    val storage: Storage
        get() = SupabaseClientConfig.storage

    /**
     * Reconfigures the Supabase client dynamically if updated at runtime.
     */
    fun reconfigure(url: String, key: String) {
        SupabaseClientConfig.reconfigure(url, key)
    }
}
