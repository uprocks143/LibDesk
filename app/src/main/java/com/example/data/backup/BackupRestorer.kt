package com.example.data.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class BackupRestorer(
    private val context: Context
) {
    suspend fun restoreEncryptedBackup(
        encryptedFile: File,
        password: CharArray
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val unencryptedZip = File(context.cacheDir, "libdesk_restore_unencrypted.zip")
            FileInputStream(encryptedFile).use { fis ->
                FileOutputStream(unencryptedZip).use { fos ->
                    BackupEncryption.decrypt(fis, fos, password)
                }
            }

            val restoreDir = File(context.cacheDir, "libdesk_restore_tmp").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            ZipInputStream(FileInputStream(unencryptedZip)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(restoreDir, entry.name)
                    FileOutputStream(outFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    entry = zis.nextEntry
                }
            }

            val dbFile = File(restoreDir, "database.sqlite")
            if (dbFile.exists()) {
                val currentDbFile = context.getDatabasePath("libdesk_v4.db")
                if (currentDbFile.exists()) {
                    currentDbFile.delete()
                }
                dbFile.copyTo(currentDbFile, overwrite = true)
                
                val shmBackup = File(restoreDir, "database.sqlite-shm")
                val walBackup = File(restoreDir, "database.sqlite-wal")
                val currentShm = context.getDatabasePath("libdesk_v4.db-shm")
                val currentWal = context.getDatabasePath("libdesk_v4.db-wal")
                
                if (shmBackup.exists()) shmBackup.copyTo(currentShm, overwrite = true) else currentShm.delete()
                if (walBackup.exists()) walBackup.copyTo(currentWal, overwrite = true) else currentWal.delete()
            }

            unencryptedZip.delete()
            restoreDir.deleteRecursively()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
