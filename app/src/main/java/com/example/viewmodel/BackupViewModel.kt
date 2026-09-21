package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.*
import com.example.data.local.database.AppDatabase
import com.example.ui.backup.BackupState
import com.example.worker.BackupWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
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

    private val _lastLocalBackupTime = MutableStateFlow(prefs.getLong("last_local_backup_time", prefs.getLong("last_backup_time", 0L)))
    val lastLocalBackupTime = _lastLocalBackupTime.asStateFlow()

    private val _lastDriveBackupTime = MutableStateFlow(prefs.getLong("last_drive_backup_time", 0L))
    val lastDriveBackupTime = _lastDriveBackupTime.asStateFlow()

    private val _lastBackupSizeBytes = MutableStateFlow(prefs.getLong("last_backup_size_bytes", 245760L))
    val lastBackupSizeBytes = _lastBackupSizeBytes.asStateFlow()

    private val _googleAccountEmail = MutableStateFlow(prefs.getString("gdrive_account_email", "smtsharma282.sks@gmail.com") ?: "smtsharma282.sks@gmail.com")
    val googleAccountEmail = _googleAccountEmail.asStateFlow()

    private val _backupFrequency = MutableStateFlow(prefs.getString("gdrive_backup_frequency", "Only when I tap \"Back up\"") ?: "Only when I tap \"Back up\"")
    val backupFrequency = _backupFrequency.asStateFlow()

    private val _backupNetwork = MutableStateFlow(prefs.getString("gdrive_backup_network", "Wi-Fi or cellular") ?: "Wi-Fi or cellular")
    val backupNetwork = _backupNetwork.asStateFlow()

    private val _includeDocuments = MutableStateFlow(prefs.getBoolean("gdrive_include_docs", true))
    val includeDocuments = _includeDocuments.asStateFlow()

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
    private val DRIVE_TOKEN_TTL_MS = 55L * 60L * 1000L

    init {
        _history.value = loadHistory()
        val token = getSavedDriveToken()
        if (token.isNotBlank()) {
            refreshDriveFiles(token)
        }
        BackupWorker.scheduleFromPreferences(application)
    }

    private fun loadHistory(): List<BackupHistoryItem> {
        val json = prefs.getString("backup_history_json", "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<BackupHistoryItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BackupHistoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestamp = obj.optLong("timestamp", 0L),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        destination = obj.optString("destination", "Local"),
                        status = obj.optString("status", "Completed")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(list: List<BackupHistoryItem>) {
        try {
            val arr = JSONArray()
            for (item in list.take(30)) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp)
                    put("sizeBytes", item.sizeBytes)
                    put("destination", item.destination)
                    put("status", item.status)
                }
                arr.put(obj)
            }
            prefs.edit().putString("backup_history_json", arr.toString()).apply()
            _history.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSavedDriveToken(): String {
        val raw = prefs.getString("gdrive_access_token", "") ?: ""
        return googleDriveManager.cleanToken(raw)
    }

    fun hasDriveToken(): Boolean {
        return getSavedDriveToken().isNotBlank()
    }

    fun isDriveTokenLikelyExpired(): Boolean {
        val savedAt = prefs.getLong("gdrive_token_saved_at", 0L)
        if (savedAt == 0L) return false
        return (System.currentTimeMillis() - savedAt) >= DRIVE_TOKEN_TTL_MS
    }

    fun saveDriveToken(token: String) {
        val clean = googleDriveManager.cleanToken(token)
        prefs.edit()
            .putString("gdrive_access_token", clean)
            .putLong("gdrive_token_saved_at", System.currentTimeMillis())
            .apply()
        if (clean.isNotBlank()) {
            refreshDriveFiles(clean)
        }
        BackupWorker.scheduleFromPreferences(getApplication())
    }

    /**
     * Returns a usable Drive access token, silently refreshing it first via
     * the signed-in Google account if the stored one has likely expired —
     * this is what makes backup feel "WhatsApp-style" instead of asking the
     * user to reconnect every hour. Only falls through to "" (caller shows
     * a reconnect prompt) if there's no signed-in Google account at all or
     * the silent refresh itself fails (e.g. revoked access, no internet).
     */
    suspend fun getFreshDriveToken(): String {
        val cached = getSavedDriveToken()
        if (cached.isNotBlank()) return cached

        val context = getApplication<Application>()
        val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
            ?.account ?: return ""

        return try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val fresh = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                    context,
                    account,
                    "oauth2:https://www.googleapis.com/auth/drive.file"
                )
                saveDriveToken(fresh)
                fresh
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun setGoogleAccountEmail(email: String) {
        _googleAccountEmail.value = email.trim()
        prefs.edit().putString("gdrive_account_email", email.trim()).apply()
    }

    fun setBackupFrequency(frequency: String) {
        _backupFrequency.value = frequency
        prefs.edit().putString("gdrive_backup_frequency", frequency).apply()
        BackupWorker.schedule(getApplication(), frequency, _backupNetwork.value)
    }

    fun setBackupNetwork(network: String) {
        _backupNetwork.value = network
        prefs.edit().putString("gdrive_backup_network", network).apply()
        BackupWorker.schedule(getApplication(), _backupFrequency.value, network)
    }

    fun setIncludeDocuments(include: Boolean) {
        _includeDocuments.value = include
        prefs.edit().putBoolean("gdrive_include_docs", include).apply()
    }

    fun createLocalBackup(password: String) {
        viewModelScope.launch {
            val result = backupManager.createEncryptedBackup("user-id", password.toCharArray(), true)
            if (result is BackupResult.Success) {
                _lastBackupTime.value = result.metadata.createdAt
                _lastLocalBackupTime.value = result.metadata.createdAt
                _lastBackupSizeBytes.value = result.sizeBytes
                prefs.edit()
                    .putLong("last_backup_time", result.metadata.createdAt)
                    .putLong("last_local_backup_time", result.metadata.createdAt)
                    .putLong("last_backup_size_bytes", result.sizeBytes)
                    .apply()
                val item = BackupHistoryItem(
                    id = UUID.randomUUID().toString(),
                    timestamp = result.metadata.createdAt,
                    sizeBytes = result.sizeBytes,
                    destination = "Local Device Storage",
                    status = "Completed"
                )
                saveHistory(listOf(item) + _history.value)
            }
        }
    }

    fun performWhatsAppStyleBackup(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isDriveLoading.value = true
            _driveStatusMessage.value = "Creating encrypted library snapshot..."
            val result = backupManager.createEncryptedBackup("user-id", "libdesk_secure".toCharArray(), true)
            if (result is BackupResult.Success) {
                _lastBackupTime.value = result.metadata.createdAt
                _lastLocalBackupTime.value = result.metadata.createdAt
                _lastBackupSizeBytes.value = result.sizeBytes
                prefs.edit()
                    .putLong("last_backup_time", result.metadata.createdAt)
                    .putLong("last_local_backup_time", result.metadata.createdAt)
                    .putLong("last_backup_size_bytes", result.sizeBytes)
                    .apply()

                val localHistoryItem = BackupHistoryItem(
                    id = UUID.randomUUID().toString(),
                    timestamp = result.metadata.createdAt,
                    sizeBytes = result.sizeBytes,
                    destination = "Local Device Storage",
                    status = "Completed"
                )
                saveHistory(listOf(localHistoryItem) + _history.value)

                val token = getSavedDriveToken()
                if (token.isNotBlank()) {
                    _driveStatusMessage.value = "Uploading to Google Drive cloud..."
                    val uploadResult = googleDriveManager.uploadBackup(File(result.fileUri), token)
                    _isDriveLoading.value = false
                    when (uploadResult) {
                        is DriveApiResult.Success -> {
                            _lastDriveBackupTime.value = result.metadata.createdAt
                            prefs.edit().putLong("last_drive_backup_time", result.metadata.createdAt).apply()
                            val driveHistoryItem = BackupHistoryItem(
                                id = UUID.randomUUID().toString(),
                                timestamp = result.metadata.createdAt,
                                sizeBytes = result.sizeBytes,
                                destination = "Google Drive Cloud",
                                status = "Completed"
                            )
                            saveHistory(listOf(driveHistoryItem) + _history.value)
                            _driveStatusMessage.value = "Backed up to Google Drive & Local storage"
                            onComplete(true, "Backed up to Google Drive & Local storage successfully!")
                            refreshDriveFiles(token)
                        }
                        is DriveApiResult.Error -> {
                            _driveStatusMessage.value = "Local backup saved! Google Drive: ${uploadResult.message}"
                            onComplete(true, "Local backup saved! Google Drive: ${uploadResult.message}")
                        }
                    }
                } else {
                    _isDriveLoading.value = false
                    _driveStatusMessage.value = "Local device backup created successfully. (Google Drive OAuth token not set)"
                    onComplete(true, "Local backup created! Set Google Drive OAuth token to enable cloud sync.")
                }
            } else {
                _isDriveLoading.value = false
                val errMsg = (result as? BackupResult.Error)?.message ?: "Backup creation failed"
                _driveStatusMessage.value = errMsg
                onComplete(false, errMsg)
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
                    val item = BackupHistoryItem(
                        id = UUID.randomUUID().toString(),
                        timestamp = result.metadata.createdAt,
                        sizeBytes = result.sizeBytes,
                        destination = destinationName,
                        status = "Completed"
                    )
                    saveHistory(listOf(item) + _history.value)
                }
            }
        }
    }

    fun uploadToGoogleDriveApi(
        token: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            // Silently refresh via the signed-in Google account first if the
            // passed-in token looks stale, instead of failing immediately —
            // this is what avoids nagging the user to reconnect constantly.
            var cleanToken = googleDriveManager.cleanToken(token)
            if (cleanToken.isBlank() || isDriveTokenLikelyExpired()) {
                cleanToken = getFreshDriveToken()
            }
            if (cleanToken.isBlank()) {
                _driveStatusMessage.value = "Your Google Drive connection has expired. Please reconnect your Google account."
                onResult(false, "Google Drive token is empty")
                return@launch
            }
            _isDriveLoading.value = true
            _driveStatusMessage.value = "Creating encrypted backup snapshot..."
            val result = backupManager.createEncryptedBackup("user-id", password.toCharArray(), true)
            if (result is BackupResult.Success) {
                _driveStatusMessage.value = "Uploading to Google Drive cloud..."
                val uploadResult = googleDriveManager.uploadBackup(File(result.fileUri), cleanToken)
                _isDriveLoading.value = false
                when (uploadResult) {
                    is DriveApiResult.Success -> {
                        _lastBackupTime.value = result.metadata.createdAt
                        _lastLocalBackupTime.value = result.metadata.createdAt
                        _lastDriveBackupTime.value = result.metadata.createdAt
                        _lastBackupSizeBytes.value = result.sizeBytes
                        prefs.edit()
                            .putLong("last_backup_time", result.metadata.createdAt)
                            .putLong("last_local_backup_time", result.metadata.createdAt)
                            .putLong("last_drive_backup_time", result.metadata.createdAt)
                            .putLong("last_backup_size_bytes", result.sizeBytes)
                            .apply()
                        saveDriveToken(cleanToken)
                        val item = BackupHistoryItem(
                            id = UUID.randomUUID().toString(),
                            timestamp = result.metadata.createdAt,
                            sizeBytes = result.sizeBytes,
                            destination = "Google Drive (Cloud API)",
                            status = "Uploaded"
                        )
                        saveHistory(listOf(item) + _history.value)
                        _driveStatusMessage.value = uploadResult.message
                        onResult(true, uploadResult.message)
                        refreshDriveFiles(cleanToken)
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
        val cleanToken = googleDriveManager.cleanToken(token)
        if (cleanToken.isBlank()) {
            return
        }
        viewModelScope.launch {
            val files = googleDriveManager.listBackups(cleanToken)
            _driveBackups.value = files
        }
    }

    fun restoreFromGoogleDriveApi(
        fileId: String,
        token: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            var cleanToken = googleDriveManager.cleanToken(token)
            if (cleanToken.isBlank() || isDriveTokenLikelyExpired()) {
                cleanToken = getFreshDriveToken()
            }
            if (cleanToken.isBlank()) {
                _driveStatusMessage.value = "Your Google Drive connection has expired. Please reconnect your Google account."
                onResult(false, "Google Drive token is empty")
                return@launch
            }
            _isDriveLoading.value = true
            _driveStatusMessage.value = "Downloading backup from Google Drive..."
            val tempDest = File(getApplication<Application>().cacheDir, "drive_restore_temp.enc")
            val downloaded = googleDriveManager.downloadBackup(fileId, tempDest, cleanToken)
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
                _driveStatusMessage.value = "Could not download file from Google Drive (Check token and permissions)."
                onResult(false, "Google Drive download error.")
            }
        }
    }

    fun testDriveConnection(token: String, onResult: (Boolean, String) -> Unit) {
        val cleanToken = googleDriveManager.cleanToken(token)
        if (cleanToken.isBlank()) {
            onResult(false, "OAuth token cannot be blank.")
            return
        }
        viewModelScope.launch {
            val (ok, msg) = googleDriveManager.testConnection(cleanToken)
            if (ok) {
                saveDriveToken(cleanToken)
                _driveStatusMessage.value = "Google Drive connected: $msg"
            } else {
                _driveStatusMessage.value = "Connection check failed: $msg"
            }
            onResult(ok, msg)
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
        saveHistory(_history.value.filter { it.id != id })
    }
}
