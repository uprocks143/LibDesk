package com.example.data.remote

import android.util.Log
import com.example.viewmodel.LiveSubscriptionCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Global Authentication Guard Service.
 *
 * Checks the 'subscription_active' flag for the logged-in library organization
 * directly in Supabase before granting access to library features.
 *
 * Implements a strict fail-closed policy:
 * - Direct real-time verification against Supabase remote tables ('libraries' and 'library_subscriptions').
 * - If 'subscription_active' flag is false (or 0/"false"), feature access is immediately denied.
 * - If account is suspended, expired, or pending verification, access is blocked.
 * - If Supabase cannot be reached (e.g. network failure), fails closed to prevent unauthorized usage.
 */
object AuthGuardService {
    private const val TAG = "AuthGuardService"

    private val _guardState = MutableStateFlow<LiveSubscriptionCheck?>(null)
    val guardState: StateFlow<LiveSubscriptionCheck?> = _guardState.asStateFlow()

    /**
     * Checks if the user is authorized to access library features.
     * Platform Super Admins have universal platform access.
     * Organization Managers and staff must have 'subscription_active' == true in Supabase.
     */
    suspend fun canAccessLibraryFeatures(libraryId: String, userRole: String?): Boolean {
        val normalized = com.example.ui.components.normalizeUserRole(userRole)
        if (normalized == com.example.ui.components.LibDeskRoles.SUPER_ADMIN) {
            return true
        }
        if (libraryId.isBlank()) return false
        val check = verifyLibrarySubscription(libraryId)
        return check == LiveSubscriptionCheck.Active
    }

    /**
     * Primary verification routine checking Supabase:
     * 1. Inspects the logged-in library organization record in the 'libraries' Supabase table
     *    and checks the 'subscription_active' boolean flag.
     * 2. Inspects the active subscription record in the 'library_subscriptions' Supabase table
     *    and verifies 'subscription_active', status ('ACTIVE' / 'TRIAL'), and validity dates.
     *
     * Returns a [LiveSubscriptionCheck] result and updates [_guardState].
     */
    suspend fun verifyLibrarySubscription(libraryId: String): LiveSubscriptionCheck = withContext(Dispatchers.IO) {
        if (libraryId.isBlank()) {
            val res = LiveSubscriptionCheck.NoSubscription
            _guardState.value = res
            return@withContext res
        }

        try {
            // 1. Query Supabase 'libraries' organization record for this library
            val (libSuccess, libRecords) = SupabaseClient.queryTable("libraries?id=eq.$libraryId&select=*")
            if (!libSuccess || libRecords == null) {
                Log.w(TAG, "Failed to query organization record from Supabase for library: $libraryId")
                val err = LiveSubscriptionCheck.NetworkError
                _guardState.value = err
                return@withContext err
            }

            if (libRecords.length() > 0) {
                val libObj = libRecords.getJSONObject(0)

                // Check 'subscription_active' (or camelCase 'subscriptionActive') flag on organization
                if (libObj.has("subscription_active")) {
                    val isSubActive = parseBooleanFlag(libObj, "subscription_active")
                    if (!isSubActive) {
                        Log.w(TAG, "Organization $libraryId has subscription_active=false in Supabase. Access blocked.")
                        val res = LiveSubscriptionCheck.Inactive
                        _guardState.value = res
                        return@withContext res
                    }
                } else if (libObj.has("subscriptionActive")) {
                    val isSubActive = parseBooleanFlag(libObj, "subscriptionActive")
                    if (!isSubActive) {
                        Log.w(TAG, "Organization $libraryId has subscriptionActive=false in Supabase. Access blocked.")
                        val res = LiveSubscriptionCheck.Inactive
                        _guardState.value = res
                        return@withContext res
                    }
                }
            }

            // 2. Query Supabase 'library_subscriptions' for active plan and status
            val (subSuccess, subRecords) = SupabaseClient.fetchRecords("library_subscriptions", libraryId)
            if (!subSuccess || subRecords == null) {
                Log.w(TAG, "Failed to fetch library_subscriptions from Supabase for library: $libraryId")
                val err = LiveSubscriptionCheck.NetworkError
                _guardState.value = err
                return@withContext err
            }

            if (subRecords.length() == 0) {
                Log.w(TAG, "No subscription record found in Supabase for library: $libraryId")
                val res = LiveSubscriptionCheck.NoSubscription
                _guardState.value = res
                return@withContext res
            }

            val subObj = subRecords.getJSONObject(0)

            // Check explicit 'subscription_active' on the subscription record if present
            if (subObj.has("subscription_active")) {
                val isSubActive = parseBooleanFlag(subObj, "subscription_active")
                if (!isSubActive) {
                    Log.w(TAG, "library_subscriptions record has subscription_active=false. Access blocked.")
                    val res = LiveSubscriptionCheck.Inactive
                    _guardState.value = res
                    return@withContext res
                }
            } else if (subObj.has("subscriptionActive")) {
                val isSubActive = parseBooleanFlag(subObj, "subscriptionActive")
                if (!isSubActive) {
                    Log.w(TAG, "library_subscriptions record has subscriptionActive=false. Access blocked.")
                    val res = LiveSubscriptionCheck.Inactive
                    _guardState.value = res
                    return@withContext res
                }
            }

            val status = subObj.optString("status", "").uppercase()
            val expiryDate = subObj.optString("expiryDate", "")
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

            val outcome = when {
                status == "SUSPENDED" -> LiveSubscriptionCheck.Suspended
                status == "PENDING_VERIFICATION" -> LiveSubscriptionCheck.PendingVerification
                expiryDate.isNotBlank() && expiryDate < today -> LiveSubscriptionCheck.Expired
                status == "ACTIVE" || status == "TRIAL" -> LiveSubscriptionCheck.Active
                else -> LiveSubscriptionCheck.NoSubscription
            }

            _guardState.value = outcome
            outcome
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying subscription guard for library $libraryId", e)
            val err = LiveSubscriptionCheck.NetworkError
            _guardState.value = err
            err
        }
    }

    private fun parseBooleanFlag(json: JSONObject, key: String): Boolean {
        val opt = json.opt(key) ?: return true
        return when (opt) {
            is Boolean -> opt
            is Number -> opt.toInt() != 0
            is String -> opt.equals("true", ignoreCase = true) || opt == "1"
            else -> false
        }
    }

    /**
     * Resets the cached guard state (e.g. on user logout).
     */
    fun reset() {
        _guardState.value = null
    }
}
