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
            
            val encryptedFile = File(context.cacheDir, "libdesk_backup_final.enc")
            FileInputStream(zipFile).use { fis ->
                FileOutputStream(encryptedFile).use { fos ->
                    BackupEncryption.encrypt(fis, fos, password)
                }
            }
            
            zipFile.delete()
            
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
            
            backupState.value = BackupState.Success(System.currentTimeMillis(), encryptedFile.length(), "Local Cache")
            BackupResult.Success(metadata, encryptedFile.absolutePath, encryptedFile.length())
            
        } catch (e: Exception) {
            backupState.value = BackupState.Error(e.message ?: "Unknown error")
            BackupResult.Error(e.message ?: "Unknown error", e)
        }
    }
}
