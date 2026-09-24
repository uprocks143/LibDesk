package com.example.data.backup

import android.content.Context
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
    private val context: Context
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

        // Create cloud snapshot metadata and exported data
        progress.value = 40
        // 2. Export Metadata
        val metadata = BackupMetadata(
            backupFormatVersion = 2,
            appVersion = "1.0",
            databaseVersion = 0,
            createdAt = System.currentTimeMillis(),
            supabaseUserId = supabaseUserId,
            backupId = java.util.UUID.randomUUID().toString(),
            containsDatabase = false,
            containsMedia = includeMedia,
            source = "SUPABASE_CLOUD"
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
