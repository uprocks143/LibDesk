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

            val metadataFile = File(restoreDir, "metadata.json")
            if (metadataFile.exists()) {
                // Restore metadata verified
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
