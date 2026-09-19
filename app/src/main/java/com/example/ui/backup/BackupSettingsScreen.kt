package com.example.ui.backup

import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.DriveFileInfo
import com.example.ui.theme.*
import com.example.viewmodel.BackupHistoryItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onBack: () -> Unit,
    onExportLocal: (Uri) -> Unit,
    onImportLocal: (Uri) -> Unit,
    onBackupNow: () -> Unit,
    lastBackupTime: Long,
    backupState: BackupState,
    history: List<BackupHistoryItem>,
    onDeleteHistory: (String) -> Unit,
    driveBackups: List<DriveFileInfo> = emptyList(),
    isDriveLoading: Boolean = false,
    driveStatusMessage: String? = null,
    savedDriveToken: String = "",
    onSaveDriveToken: (String) -> Unit = {},
    onUploadToGoogleDrive: (String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onRestoreFromGoogleDrive: (String, String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _, _ -> },
    onRefreshDriveFiles: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }
    
    // Dialog states
    val showRestoreConfirmDialog = remember { mutableStateOf<Uri?>(null) }
    val showDeleteConfirmDialog = remember { mutableStateOf<String?>(null) }
    var showDriveSetupDialog by remember { mutableStateOf(false) }
    var showDriveFilesDialog by remember { mutableStateOf(false) }
    var driveFileToRestore by remember { mutableStateOf<DriveFileInfo?>(null) }

    val sha1Fingerprint = "A0:A8:C5:90:48:C9:1D:F9:55:A9:64:38:13:17:06:75:81:04:5C:C4"
    val sha256Fingerprint = "1A:86:15:D1:FA:4B:23:58:B4:E8:AF:81:72:BA:B8:07:2F:3D:43:BB:8F:DC:3B:55:72:71:CB:2E:9C:F3:4D:4F"
    val appPackageName = "com.aistudio.libdesk.appmgr"

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { onExportLocal(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { showRestoreConfirmDialog.value = it }
    }


    if (showRestoreConfirmDialog.value != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog.value = null },
            title = { Text("Restore LibDesk backup?", fontWeight = FontWeight.Bold) },
            text = { 
                Text(
                    "This will replace the current LibDesk data with the selected backup.\n\n" +
                    "Make sure you have backed up your current data if you wish to keep it.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = showRestoreConfirmDialog.value!!
                        showRestoreConfirmDialog.value = null
                        onImportLocal(uri)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Restore Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog.value = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    
    if (showDeleteConfirmDialog.value != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog.value = null },
            title = { Text("Delete this backup?", fontWeight = FontWeight.Bold) },
            text = { Text("This removes the selected backup record from the history.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteHistory(showDeleteConfirmDialog.value!!)
                        showDeleteConfirmDialog.value = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog.value = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Google Drive Setup & SHA-1 Modal Dialog
    if (showDriveSetupDialog) {
        var tokenInput by remember { mutableStateOf(savedDriveToken) }
        AlertDialog(
            onDismissRequest = { showDriveSetupDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Drive & SHA-1 Setup", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    IconButton(onClick = { showDriveSetupDialog = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Apne Google Drive me LibDesk ka direct cloud backup aur restore enable karne ke liye niche di gayi SHA-1 aur Package details use karein:",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // SHA-1 Card with 1-click Copy
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SHA-1 Certificate Fingerprint", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(sha1Fingerprint))
                                        // Toast.makeText(context, "SHA-1 copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy SHA-1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                text = sha1Fingerprint,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Package Name Card with Copy
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Package Name (Application ID)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(appPackageName, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(appPackageName))
                                    // Toast.makeText(context, "Package Name copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Token input field
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Google Drive OAuth Access Token") },
                        placeholder = { Text("Bearer ya OAuth token paste karein...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Setup checklist summary
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Google Cloud Console Steps:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("1. Google Cloud Console me Google Drive API enable karein.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("2. OAuth Client ID (Android) banayein aur SHA-1 + Package Name paste karein.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("3. Scope 'https://www.googleapis.com/auth/drive.file' configure karein.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("4. Access Token yahan save karke 1-tap Google Drive Sync shuru karein.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveDriveToken(tokenInput.trim())
                        if (tokenInput.isNotBlank()) {
                            onRefreshDriveFiles(tokenInput.trim())
                            // Toast.makeText(context, "Drive token saved & verified!", Toast.LENGTH_SHORT).show()
                        } else {
                            // Toast.makeText(context, "Token cleared.", Toast.LENGTH_SHORT).show()
                        }
                        showDriveSetupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDriveSetupDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Google Drive Files Browser Modal Dialog
    if (showDriveFilesDialog) {
        AlertDialog(
            onDismissRequest = { showDriveFilesDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Drive Backups", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(
                        onClick = { onRefreshDriveFiles(savedDriveToken) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    if (isDriveLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (driveBackups.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No backups found in Google Drive", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Tap 'Upload to Drive' to backup your current LibDesk data to Google Drive.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(driveBackups) { file ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(file.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val sizeKb = if (file.sizeBytes > 0) "${file.sizeBytes / 1024} KB" else "Encrypted"
                                            val dateStr = if (file.createdTime.isNotBlank()) file.createdTime.take(19).replace("T", " ") else "Recent"
                                            Text("$sizeKb • $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = {
                                                driveFileToRestore = file
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Restore", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDriveFilesDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Google Drive Restore Confirmation Dialog
    if (driveFileToRestore != null) {
        val file = driveFileToRestore!!
        AlertDialog(
            onDismissRequest = { driveFileToRestore = null },
            title = { Text("Restore from Google Drive?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will download '${file.name}' from Google Drive, decrypt it, and restore all student records, payments, and shifts into LibDesk.\n\nCurrent local data will be replaced.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileId = file.id
                        driveFileToRestore = null
                        showDriveFilesDialog = false
                        onRestoreFromGoogleDrive(fileId, savedDriveToken, "libdesk_secure") { success, msg ->
                            // Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Restore Database")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { driveFileToRestore = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Backup & Restore", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Protect your LibDesk data", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // 1. BACKUP STATUS
            item {
                Text(
                    text = "LIBDESK BACKUP STATUS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Last backup", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text(
                                text = if (lastBackupTime > 0) dateFormatter.format(Date(lastBackupTime)) else "No backup yet",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Backup size", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Text(
                                text = if (history.isNotEmpty()) Formatter.formatShortFileSize(context, history.first().sizeBytes) else "--",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Status", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            
                            val statusColor = if (lastBackupTime > 0) LibDeskColors.success else LibDeskColors.warning
                            val statusText = if (lastBackupTime > 0) "Protected" else "Not backed up"
                            val statusIcon = if (lastBackupTime > 0) Icons.Default.Shield else Icons.Default.Warning
                            
                            Surface(
                                color = statusColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                // Progress Indicator
                if (backupState is BackupState.Preparing || backupState is BackupState.Encrypting || backupState is BackupState.Uploading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                            Text(
                                text = when (backupState) {
                                    is BackupState.Preparing -> "Preparing LibDesk data..."
                                    is BackupState.Encrypting -> "Encrypting and packaging..."
                                    is BackupState.Uploading -> "Uploading backup..."
                                    else -> "Processing..."
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.primaryContainer)
                        }
                    }
                }
                
                // Error Indicator
                if (backupState is BackupState.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Backup failed", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(backupState.message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 2. BACKUP ACTION & DESTINATIONS
            item {
                Text(
                    text = "BACKUP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Button(
                    onClick = onBackupNow,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = backupState is BackupState.Idle || backupState is BackupState.Success || backupState is BackupState.Error
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back Up Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        // Local Backup
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Local Backup", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Save a backup file on this device", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(
                                onClick = { 
                                    val timeFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
                                    val suggestedName = "LibDesk_Backup_${timeFormat.format(Date())}.libdeskbackup"
                                    exportLauncher.launch(suggestedName)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Export", fontWeight = FontWeight.Medium)
                            }
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        
                        // Google Drive Cloud Backup
                        val isDriveConnected = savedDriveToken.isNotBlank()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isDriveConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Cloud,
                                        contentDescription = "Google Drive",
                                        tint = if (isDriveConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Google Drive Cloud", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (isDriveConnected) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = LibDeskColors.success.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    "Connected ✓",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LibDeskColors.success,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        if (isDriveConnected) "Encrypted cloud backup active" else "Setup required • View SHA-1 & token",
                                        fontSize = 13.sp,
                                        color = if (isDriveConnected) LibDeskColors.success else LibDeskColors.warning
                                    )
                                }
                                IconButton(
                                    onClick = { showDriveSetupDialog = true },
                                    modifier = Modifier.testTag("btn_drive_setup_guide")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Google Cloud Setup & Keys",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (isDriveConnected) {
                                            onUploadToGoogleDrive(savedDriveToken, "libdesk_secure") { success, msg ->
                                                // Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            showDriveSetupDialog = true
                                        }
                                    },
                                    enabled = !isDriveLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_upload_to_google_drive")
                                ) {
                                    if (isDriveLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Processing...", fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isDriveConnected) "Upload to Drive" else "Connect & Sync", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { showDriveSetupDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("btn_view_sha1_keys")
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SHA-1 Keys", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            if (driveStatusMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = driveStatusMessage,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. RESTORE
            item {
                Text(
                    text = "RESTORE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { importLauncher.launch(arrayOf("*/*")) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Restore from Local File", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (savedDriveToken.isNotBlank()) {
                                        onRefreshDriveFiles(savedDriveToken)
                                        showDriveFilesDialog = true
                                    } else {
                                        showDriveSetupDialog = true
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = if (savedDriveToken.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Restore from Google Drive",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                                Text(
                                    if (savedDriveToken.isNotBlank()) {
                                        if (driveBackups.isNotEmpty()) "${driveBackups.size} cloud backups available • Tap to select"
                                        else "Connected • Tap to check backups"
                                    } else "Setup required • Tap to view SHA-1 & configure",
                                    color = if (savedDriveToken.isNotBlank()) LibDeskColors.success else LibDeskColors.warning,
                                    fontSize = 13.sp
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // 4. BACKUP HISTORY
            item {
                Text(
                    text = "BACKUP HISTORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            if (history.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No backups yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Create your first LibDesk backup to keep your data safe.", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(history) { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LibDesk Backup", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("${dateFormatter.format(Date(item.timestamp))}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(Formatter.formatShortFileSize(context, item.sizeBytes), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                    Text(" • ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(item.destination, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(" • ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(item.status, fontSize = 12.sp, color = LibDeskColors.success)
                                }
                            }
                            IconButton(onClick = { showDeleteConfirmDialog.value = item.id }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
