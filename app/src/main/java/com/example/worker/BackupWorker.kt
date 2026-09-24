package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupResult
import com.example.data.backup.DriveApiResult
import com.example.data.backup.GoogleDriveBackupManager
import com.example.viewmodel.BackupHistoryItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Executing scheduled automated WhatsApp-style backup...")
        return try {
            val prefs = applicationContext.getSharedPreferences("libdesk_backup_prefs", Context.MODE_PRIVATE)
            val backupManager = BackupManager(applicationContext)
            val googleDriveManager = GoogleDriveBackupManager()

            val includeDocs = prefs.getBoolean("gdrive_include_docs", true)
            val rawToken = prefs.getString("gdrive_access_token", "") ?: ""
            val token = googleDriveManager.cleanToken(rawToken)

            // 1. Create local encrypted database backup snapshot
            val backupResult = backupManager.createEncryptedBackup("auto-daily", "libdesk_secure".toCharArray(), includeDocs)
            if (backupResult !is BackupResult.Success) {
                val errorMsg = (backupResult as? BackupResult.Error)?.message ?: "Backup creation failed"
                Log.e(TAG, "Local auto-backup failed: $errorMsg")
                prefs.edit().putString("last_auto_backup_status", "Auto backup failed: $errorMsg").apply()
                return Result.retry()
            }

            val timestamp = backupResult.metadata.createdAt
            val sizeBytes = backupResult.sizeBytes

            // Update local backup timestamps in SharedPreferences
            prefs.edit()
                .putLong("last_backup_time", timestamp)
                .putLong("last_local_backup_time", timestamp)
                .putLong("last_backup_size_bytes", sizeBytes)
                .apply()

            addHistoryItem(
                prefs,
                BackupHistoryItem(
                    id = UUID.randomUUID().toString(),
                    timestamp = timestamp,
                    sizeBytes = sizeBytes,
                    destination = "Local Device Storage (Auto)",
                    status = "Completed"
                )
            )

            // 2. Upload to Google Drive if OAuth token is configured
            if (token.isNotBlank()) {
                Log.i(TAG, "Uploading auto-backup snapshot to Google Drive cloud...")
                val file = File(backupResult.fileUri)
                val uploadResult = googleDriveManager.uploadBackup(file, token)
                when (uploadResult) {
                    is DriveApiResult.Success -> {
                        Log.i(TAG, "Google Drive auto-backup succeeded: ${uploadResult.message}")
                        prefs.edit()
                            .putLong("last_drive_backup_time", timestamp)
                            .putString("last_auto_backup_status", "Backed up to Google Drive & Local storage")
                            .apply()

                        addHistoryItem(
                            prefs,
                            BackupHistoryItem(
                                id = UUID.randomUUID().toString(),
                                timestamp = timestamp,
                                sizeBytes = sizeBytes,
                                destination = "Google Drive Cloud (Auto)",
                                status = "Completed"
                            )
                        )
                    }
                    is DriveApiResult.Error -> {
                        Log.w(TAG, "Google Drive upload failed during auto backup: ${uploadResult.message}")
                        prefs.edit()
                            .putString("last_auto_backup_status", "Local backup saved; Google Drive: ${uploadResult.message}")
                            .apply()
                    }
                }
            } else {
                Log.i(TAG, "Google Drive token not set; local automated backup completed successfully.")
                prefs.edit()
                    .putString("last_auto_backup_status", "Local device backup created (Google Drive token not configured)")
                    .apply()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in BackupWorker", e)
            Result.retry()
        }
    }

    private fun addHistoryItem(prefs: android.content.SharedPreferences, item: BackupHistoryItem) {
        try {
            val existingJson = prefs.getString("backup_history_json", "[]") ?: "[]"
            val array = JSONArray(existingJson)
            val newArray = JSONArray()
            val itemObj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("sizeBytes", item.sizeBytes)
                put("destination", item.destination)
                put("status", item.status)
            }
            newArray.put(itemObj)
            for (i in 0 until minOf(array.length(), 29)) {
                newArray.put(array.getJSONObject(i))
            }
            prefs.edit().putString("backup_history_json", newArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist history item", e)
        }
    }

    companion object {
        const val TAG = "BackupWorker"
        const val WORK_NAME = "LibDeskAutoBackupWorker"

        fun scheduleFromPreferences(context: Context) {
            val prefs = context.getSharedPreferences("libdesk_backup_prefs", Context.MODE_PRIVATE)
            val frequency = prefs.getString("gdrive_backup_frequency", "Only when I tap \"Back up\"") ?: "Only when I tap \"Back up\""
            val network = prefs.getString("gdrive_backup_network", "Wi-Fi or cellular") ?: "Wi-Fi or cellular"
            schedule(context, frequency, network)
        }

        fun schedule(context: Context, frequency: String, network: String = "Wi-Fi or cellular") {
            val workManager = WorkManager.getInstance(context)
            if (frequency == "Off" || frequency == "Never" || frequency == "Only when I tap \"Back up\"") {
                Log.i(TAG, "Cancelling scheduled automated backup work (frequency: $frequency)")
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val repeatIntervalDays = when (frequency) {
                "Daily" -> 1L
                "Weekly" -> 7L
                "Monthly" -> 30L
                else -> 1L
            }

            val networkType = if (network.contains("cellular", ignoreCase = true)) {
                NetworkType.CONNECTED
            } else {
                NetworkType.UNMETERED
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(networkType)
                .setRequiresBatteryNotLow(true)
                .build()

            // Calculate delay until next 2:00 AM (WhatsApp standard overnight backup)
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 2)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val initialDelayMs = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<BackupWorker>(repeatIntervalDays, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()

            Log.i(TAG, "Scheduled WhatsApp-style auto backup: frequency=$frequency, interval=${repeatIntervalDays}d, network=$networkType, initialDelay=${initialDelayMs / 1000 / 60}m")

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
