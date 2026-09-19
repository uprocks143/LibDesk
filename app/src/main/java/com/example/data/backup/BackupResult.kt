package com.example.data.backup

sealed class BackupResult {
    data class Success(val metadata: BackupMetadata, val fileUri: String, val sizeBytes: Long) : BackupResult()
    data class Error(val message: String, val exception: Exception? = null) : BackupResult()
}
