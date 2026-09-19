package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.*
import com.example.data.local.database.AppDatabase
import com.example.ui.backup.BackupState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class BackupHistoryItem(
    val id: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val destination: String,
    val status: String
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val backupManager = BackupManager(application, database)
    private val localBackupManager = LocalBackupManager(application)
    private val restorer = BackupRestorer(application, database)
    private val googleDriveManager = GoogleDriveBackupManager()
    private val prefs = application.getSharedPreferences("libdesk_backup_prefs", Application.MODE_PRIVATE)

    val backupState: StateFlow<BackupState> = backupManager.backupState
    
    private val _lastBackupTime = MutableStateFlow(prefs.getLong("last_backup_time", 0L))
    val lastBackupTime = _lastBackupTime.asStateFlow()

    private val _history = MutableStateFlow<List<BackupHistoryItem>>(emptyList())
    val history = _history.asStateFlow()

    private val _driveBackups = MutableStateFlow<List<DriveFileInfo>>(emptyList())
    val driveBackups = _driveBackups.asStateFlow()

    private val _driveStatusMessage = MutableStateFlow<String?>(null)
    val driveStatusMessage = _driveStatusMessage.asStateFlow()

    private val _isDriveLoading = MutableStateFlow(false)
    val isDriveLoading = _isDriveLoading.asStateFlow()

    // Google OAuth access tokens are short-lived (~1 hour) and this app has no
    // Sign-In / refresh-token flow for Drive (a real Google Sign-In integration
    // needs an OAuth client set up in your own Google Cloud project). Previously
    // a pasted token was trusted forever, so uploads/restores started failing
    // with a generic "network error" the moment it expired, with no indication
    // why. We now track when the token was saved and warn explicitly once it's
    // likely stale, instead of failing silently.
    private val DRIVE_TOKEN_TTL_MS = 55L * 60L * 1000L // treat as stale just under Google's 1hr expiry

    fun getSavedDriveToken(): String {
        val token = prefs.getString("gdrive_access_token", "") ?: ""
        if (token.isBlank()) return ""
        if (isDriveTokenLikelyExpired()) return "" // force re-entry instead of a doomed API call
        return token
    }

    fun isDriveTokenLikelyExpired(): Boolean {
        val savedAt = prefs.getLong("gdrive_token_saved_at", 0L)
        if (savedAt == 0L) return false
        return (System.currentTimeMillis() - savedAt) >= DRIVE_TOKEN_TTL_MS
    }

    fun saveDriveToken(token: String) {
        prefs.edit()
            .putString("gdrive_access_token", token.trim())
            .putLong("gdrive_token_saved_at", System.currentTimeMillis())
            .apply()
    }

    fun createLocalBackup(password: String) {
        viewModelScope.launch {
            val result = backupManager.createEncryptedBackup("user-id", password.toCharArray(), true)
            if (result is BackupResult.Success) {
                _lastBackupTime.value = result.metadata.createdAt
                prefs.edit().putLong("last_backup_time", result.metadata.createdAt).apply()
                _history.value = listOf(
                    BackupHistoryItem(
                        id = UUID.randomUUID().toString(),
                        timestamp = result.metadata.createdAt,
                        sizeBytes = result.sizeBytes,
                        destination = "Local Cache",
                        status = "Completed"
                    )
                ) + _history.value
            }
        }
    }

    fun exportBackup(uri: Uri, password: String, destinationName: String = "Local File") {
        viewModelScope.launch {
            val result = backupManager.createEncryptedBackup("user-id", password.toCharArray(), true)
            if (result is BackupResult.Success) {
                _lastBackupTime.value = result.metadata.createdAt
                prefs.edit().putLong("last_backup_time", result.metadata.createdAt).apply()
                val success = localBackupManager.exportToUri(File(result.fileUri), uri)
                if (success) {
                    _history.value = listOf(
                        BackupHistoryItem(
                            id = UUID.randomUUID().toString(),
                            timestamp = result.metadata.createdAt,
                            sizeBytes = result.sizeBytes,
                            destination = destinationName,
                            status = "Completed"
                        )
                    ) + _history.value
                }
            }
        }
    }

    fun uploadToGoogleDriveApi(
        token: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (isDriveTokenLikelyExpired()) {
            _driveStatusMessage.value = "Your Google Drive token has likely expired (tokens last ~1 hour). Please paste a fresh access token and try again."
            onResult(false, "Drive token expired")
            return
        }
        viewModelScope.launch {
            _isDriveLoading.value = true
            _driveStatusMessage.value = "Creating encrypted backup snapshot..."
            val result = backupManager.createEncryptedBackup("user-id", password.toCharArray(), true)
            if (result is BackupResult.Success) {
                _driveStatusMessage.value = "Uploading to Google Drive cloud..."
                val uploadResult = googleDriveManager.uploadBackup(File(result.fileUri), token)
                _isDriveLoading.value = false
                when (uploadResult) {
                    is DriveApiResult.Success -> {
                        _lastBackupTime.value = result.metadata.createdAt
                        prefs.edit().putLong("last_backup_time", result.metadata.createdAt).apply()
                        saveDriveToken(token)
                        _history.value = listOf(
                            BackupHistoryItem(
                                id = UUID.randomUUID().toString(),
                                timestamp = result.metadata.createdAt,
                                sizeBytes = result.sizeBytes,
                                destination = "Google Drive (Cloud API)",
                                status = "Uploaded"
                            )
                        ) + _history.value
                        _driveStatusMessage.value = uploadResult.message
                        onResult(true, uploadResult.message)
                        refreshDriveFiles(token)
                    }
                    is DriveApiResult.Error -> {
                        _driveStatusMessage.value = uploadResult.message
                        onResult(false, uploadResult.message)
                    }
                }
            } else {
                _isDriveLoading.value = false
                val errMsg = (result as? BackupResult.Error)?.message ?: "Backup creation failed"
                _driveStatusMessage.value = errMsg
                onResult(false, errMsg)
            }
        }
    }

    fun refreshDriveFiles(token: String) {
        if (token.isBlank() || isDriveTokenLikelyExpired()) {
            _driveStatusMessage.value = if (token.isBlank()) null else
                "Your Google Drive token has likely expired (tokens last ~1 hour). Please paste a fresh access token."
            return
        }
        viewModelScope.launch {
            val files = googleDriveManager.listBackups(token)
            _driveBackups.value = files
        }
    }

    fun restoreFromGoogleDriveApi(
        fileId: String,
        token: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (isDriveTokenLikelyExpired()) {
            _driveStatusMessage.value = "Your Google Drive token has likely expired (tokens last ~1 hour). Please paste a fresh access token and try again."
            onResult(false, "Drive token expired")
            return
        }
        viewModelScope.launch {
            _isDriveLoading.value = true
            _driveStatusMessage.value = "Downloading backup from Google Drive..."
            val tempDest = File(getApplication<Application>().cacheDir, "drive_restore_temp.enc")
            val downloaded = googleDriveManager.downloadBackup(fileId, tempDest, token)
            if (downloaded && tempDest.exists() && tempDest.length() > 0) {
                _driveStatusMessage.value = "Decrypting and restoring LibDesk database..."
                val success = restorer.restoreEncryptedBackup(tempDest, password.toCharArray())
                _isDriveLoading.value = false
                if (success) {
                    _driveStatusMessage.value = "Database restored successfully from Google Drive!"
                    onResult(true, "Database restored successfully from Google Drive!")
                } else {
                    _driveStatusMessage.value = "Failed to decrypt or parse backup file. Check password."
                    onResult(false, "Decryption or restoration failed.")
                }
            } else {
                _isDriveLoading.value = false
                _driveStatusMessage.value = "Could not download file from Google Drive."
                onResult(false, "Google Drive download error.")
            }
        }
    }
    
    fun restoreFromUri(uri: Uri, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val encryptedFile = localBackupManager.importFromUri(uri)
            if (encryptedFile != null) {
                val success = restorer.restoreEncryptedBackup(encryptedFile, password.toCharArray())
                onComplete(success)
            } else {
                onComplete(false)
            }
        }
    }

    fun deleteHistory(id: String) {
        _history.value = _history.value.filter { it.id != id }
    }
}
