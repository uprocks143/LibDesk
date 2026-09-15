package com.example.data.backup

import android.content.Context
import com.example.data.local.database.AppDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupCreator(
    private val context: Context,
    private val database: AppDatabase
) {
    
    suspend fun createLocalBackup(
        supabaseUserId: String?,
        includeMedia: Boolean,
        progress: MutableStateFlow<Int>
    ): File = withContext(Dispatchers.IO) {
        progress.value = 10
        val backupDir = File(context.cacheDir, "libdesk_backup_tmp").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        progress.value = 20
        // 1. Export Database
        val dbFile = context.getDatabasePath("libdesk_v4.db")
        val backupDbFile = File(backupDir, "database.sqlite")
        if (dbFile.exists()) {
            dbFile.copyTo(backupDbFile, overwrite = true)
        }
        
        // Write SHM and WAL for completeness if they exist
        val shmFile = context.getDatabasePath("libdesk_v4.db-shm")
        if (shmFile.exists()) shmFile.copyTo(File(backupDir, "database.sqlite-shm"), overwrite = true)
        val walFile = context.getDatabasePath("libdesk_v4.db-wal")
        if (walFile.exists()) walFile.copyTo(File(backupDir, "database.sqlite-wal"), overwrite = true)

        progress.value = 40
        // 2. Export Metadata
        val metadata = BackupMetadata(
            backupFormatVersion = 1,
            appVersion = "1.0",
            databaseVersion = 6,
            createdAt = System.currentTimeMillis(),
            supabaseUserId = supabaseUserId,
            backupId = java.util.UUID.randomUUID().toString(),
            containsDatabase = true,
            containsMedia = includeMedia
        )
        val metadataFile = File(backupDir, "metadata.json")
        metadataFile.writeText(Json.encodeToString(metadata))

        progress.value = 60
        // 3. Zip it all
        val zipFile = File(context.cacheDir, "libdesk_backup_unencrypted.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            backupDir.listFiles()?.forEach { file ->
                zos.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }

        progress.value = 80
        backupDir.deleteRecursively()
        
        zipFile
    }
}
