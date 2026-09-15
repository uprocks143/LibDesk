package com.example.data.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class LocalBackupManager(private val context: Context) {

    suspend fun exportToUri(encryptedFile: File, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                FileInputStream(encryptedFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importFromUri(sourceUri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(context.cacheDir, "imported_backup.enc")
            context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                FileOutputStream(destFile).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            if (destFile.exists() && destFile.length() > 0) destFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
