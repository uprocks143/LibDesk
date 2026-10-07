package com.example.util

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build

object BiometricSecurityManager {
    private const val PREFS_NAME = "libdesk_security_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
    private const val KEY_APP_LOCKED = "key_app_locked"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isDeviceSecure(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager?.isDeviceSecure == true
        } else {
            keyguardManager?.isKeyguardSecure == true
        }
    }
}
