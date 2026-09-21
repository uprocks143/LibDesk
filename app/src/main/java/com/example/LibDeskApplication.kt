package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.worker.BackupWorker

class LibDeskApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            if (!WorkManager.isInitialized()) {
                WorkManager.initialize(this, workManagerConfiguration)
            }
            BackupWorker.scheduleFromPreferences(this)
        } catch (e: Throwable) {
            Log.w("LibDeskApplication", "WorkManager initialization check: ${e.message}")
        }
    }
}
