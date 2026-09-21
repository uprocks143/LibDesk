package com.example.data.backup

import android.content.Context
import com.example.data.local.database.AppDatabase
import com.example.ui.backup.BackupState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val backupCreator = BackupCreator(context, database)
    val backupState = MutableStateFlow<BackupState>(BackupState.Idle)

    suspend fun createEncryptedBackup(
        supabaseUserId: String?,
        password: CharArray,
        includeMedia: Boolean
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            backupState.value = BackupState.Preparing
            
            val progress = MutableStateFlow(0)
            val zipFile = backupCreator.createLocalBackup(supabaseUserId, includeMedia, progress)
            
            backupState.value = BackupState.Encrypting(80)
            
            val timestampStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
            val filename = "LibDesk_Backup_$timestampStr.enc"
            val encryptedFile = File(context.cacheDir, filename)
            FileInputStream(zipFile).use { fis ->
                FileOutputStream(encryptedFile).use { fos ->
                    BackupEncryption.encrypt(fis, fos, password)
                }
            }
            
            zipFile.delete()

            // Save persistent local device backup copy (WhatsApp style internal & app storage)
            try {
                val localInternalDir = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }
                encryptedFile.copyTo(File(localInternalDir, "LibDesk_Backup_Latest.enc"), overwrite = true)
                encryptedFile.copyTo(File(localInternalDir, filename), overwrite = true)

                context.getExternalFilesDir(null)?.let { extRoot ->
                    val localExtDir = File(extRoot, "LibDesk_Backups").apply { if (!exists()) mkdirs() }
                    encryptedFile.copyTo(File(localExtDir, "LibDesk_Backup_Latest.enc"), overwrite = true)
                    encryptedFile.copyTo(File(localExtDir, filename), overwrite = true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val metadata = BackupMetadata(
                backupFormatVersion = 1,
                appVersion = "1.0",
                databaseVersion = 6,
                createdAt = System.currentTimeMillis(),
                supabaseUserId = supabaseUserId,
                backupId = "N/A",
                containsDatabase = true,
                containsMedia = includeMedia
            )
            
            backupState.value = BackupState.Success(System.currentTimeMillis(), encryptedFile.length(), "Local Device Storage")
            BackupResult.Success(metadata, encryptedFile.absolutePath, encryptedFile.length())
            
        } catch (e: Exception) {
            backupState.value = BackupState.Error(e.message ?: "Unknown error")
            BackupResult.Error(e.message ?: "Unknown error", e)
        }
    }
}
