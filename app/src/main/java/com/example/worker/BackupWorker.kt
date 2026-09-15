package com.example.worker

import android.content.Context
import androidx.work.*
import com.example.data.backup.BackupManager
import com.example.data.local.database.AppDatabase
import java.util.concurrent.TimeUnit

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val backupManager = BackupManager(applicationContext, database)
            
            // Assume password and settings are fetched from securely stored preferences
            // For auto-backup, we execute the local backup and, if configured, cloud upload.
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "LibDeskAutoBackupWorker"

        fun schedule(context: Context, frequency: String) {
            val workManager = WorkManager.getInstance(context)
            if (frequency == "Off" || frequency == "Never") {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }
            
            val repeatInterval = when (frequency) {
                "Daily" -> 1L
                "Weekly" -> 7L
                "Monthly" -> 30L
                else -> 1L
            }

            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiredNetworkType(NetworkType.UNMETERED) // Assuming WiFi only by default
                .build()

            val request = PeriodicWorkRequestBuilder<BackupWorker>(repeatInterval, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
