package com.example.ui.backup

sealed interface BackupState {
    object Idle : BackupState
    object Preparing : BackupState
    data class Creating(val progress: Int) : BackupState
    data class Encrypting(val progress: Int) : BackupState
    data class Uploading(val progress: Int) : BackupState
    data class Success(val timestamp: Long, val sizeBytes: Long, val destination: String) : BackupState
    data class Error(val message: String) : BackupState
}
