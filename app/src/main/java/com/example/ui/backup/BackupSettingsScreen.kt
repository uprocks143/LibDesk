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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.DriveFileInfo
import com.example.ui.theme.*
import com.example.viewmodel.BackupHistoryItem
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private val WhatsAppGreen = Color(0xFF00A884)
private val WhatsAppGreenDark = Color(0xFF075E54)

private fun formatWhatsAppDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Never"
    val now = Calendar.getInstance()
    val timeCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    return when {
        now.get(Calendar.YEAR) == timeCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == timeCal.get(Calendar.DAY_OF_YEAR) -> "Today, $timeFmt"
        now.get(Calendar.YEAR) == timeCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - timeCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday, $timeFmt"
        else -> SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * WhatsApp-style Google Drive & Local Backup Settings Screen.
 * Provides intuitive, authentic backup experience matching WhatsApp's Chat Backup UI.
 */
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
    onRefreshDriveFiles: (String) -> Unit = {},
    googleAccountEmail: String = "smtsharma282.sks@gmail.com",
    onSetGoogleAccountEmail: (String) -> Unit = {},
    backupFrequency: String = "Only when I tap \"Back up\"",
    onSetBackupFrequency: (String) -> Unit = {},
    backupNetwork: String = "Wi-Fi or cellular",
    onSetBackupNetwork: (String) -> Unit = {},
    includeDocuments: Boolean = true,
    onSetIncludeDocuments: (Boolean) -> Unit = {},
    lastLocalBackupTime: Long = 0L,
    lastDriveBackupTime: Long = 0L,
    lastBackupSizeBytes: Long = 245760L,
    onPerformWhatsAppBackup: () -> Unit = {},
    onTestDriveConnection: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }

    // Dialogs
    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showNetworkDialog by remember { mutableStateOf(false) }
    var showDriveFilesDialog by remember { mutableStateOf(false) }
    var driveFileToRestore by remember { mutableStateOf<DriveFileInfo?>(null) }
    val showRestoreConfirmDialog = remember { mutableStateOf<Uri?>(null) }
    val showDeleteConfirmDialog = remember { mutableStateOf<String?>(null) }

    // In-card Google Account / Drive token editing state
    var editAccountEmail by remember(googleAccountEmail) { mutableStateOf(googleAccountEmail) }
    var editDriveToken by remember(savedDriveToken) { mutableStateOf(savedDriveToken) }
    var isSigningIntoGoogle by remember { mutableStateOf(false) }
    var googleSignInError by remember { mutableStateOf<String?>(null) }
    var showManualTokenEntry by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Real Google Sign-In, using the app's registered Android OAuth client
    // (from google-services.json) with Drive.file scope. This replaces
    // manually pasting a raw access token, which expired after ~1 hour and
    // had to be re-copied by hand every time.
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val accountEmail = account.email ?: ""
            val googleAccount = account.account
            if (googleAccount == null) {
                googleSignInError = "Couldn't read the selected Google account. Please try again."
                isSigningIntoGoogle = false
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                try {
                    val token = withContext(Dispatchers.IO) {
                        GoogleAuthUtil.getToken(
                            context,
                            googleAccount,
                            "oauth2:https://www.googleapis.com/auth/drive.file"
                        )
                    }
                    onSetGoogleAccountEmail(accountEmail)
                    onSaveDriveToken(token)
                    googleSignInError = null
                    Toast.makeText(context, "Connected to Google Drive as $accountEmail", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    googleSignInError = "Signed in, but couldn't authorize Drive access: ${e.message}"
                } finally {
                    isSigningIntoGoogle = false
                }
            }
        } catch (e: ApiException) {
            isSigningIntoGoogle = false
            // Status code 10 = DEVELOPER_ERROR, almost always a SHA-1
            // fingerprint / package name mismatch against the OAuth client
            // registered in Google Cloud Console for this app.
            googleSignInError = if (e.statusCode == 10) {
                "Google Sign-In setup error (code 10). The app's signing certificate (SHA-1) isn't registered against your OAuth client in Google Cloud Console yet."
            } else {
                "Google Sign-In failed (code ${e.statusCode}). Please try again."
            }
        }
    }

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

    // -------------------------------------------------------------------------
    // DIALOG: Frequency Selection (WhatsApp Style)
    // -------------------------------------------------------------------------
    if (showFrequencyDialog) {
        val frequencies = listOf(
            "Never",
            "Only when I tap \"Back up\"",
            "Daily",
            "Weekly",
            "Monthly"
        )
        AlertDialog(
            onDismissRequest = { showFrequencyDialog = false },
            title = {
                Text(
                    "Back up to Google Drive",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    frequencies.forEach { freq ->
                        val isSelected = backupFrequency.equals(freq, ignoreCase = true) ||
                                (freq.startsWith("Only") && backupFrequency.startsWith("Only"))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetBackupFrequency(freq)
                                    showFrequencyDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSetBackupFrequency(freq)
                                    showFrequencyDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = WhatsAppGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = freq,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showFrequencyDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = WhatsAppGreen)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // -------------------------------------------------------------------------
    // DIALOG: Google Account (WhatsApp Style)
    // -------------------------------------------------------------------------
    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Google Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Connect your Google Account to back up library data to Google Drive.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            googleSignInError = null
                            isSigningIntoGoogle = true
                            showAccountDialog = false
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (googleAccountEmail.isBlank()) "Sign in with Google" else "Re-connect with Google")
                    }

                    if (googleAccountEmail.isNotBlank() || savedDriveToken.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                onSetGoogleAccountEmail("")
                                onSaveDriveToken("")
                                googleSignInClient.signOut()
                                showAccountDialog = false
                                Toast.makeText(context, "Disconnected from Google Drive", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Disconnect Google Account")
                        }
                    }

                    TextButton(onClick = { showManualTokenEntry = !showManualTokenEntry }) {
                        Text(
                            if (showManualTokenEntry) "Hide advanced option" else "Advanced: paste a token manually",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (showManualTokenEntry) {
                    OutlinedTextField(
                        value = editAccountEmail,
                        onValueChange = { editAccountEmail = it },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("e.g. name@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDriveToken,
                        onValueChange = { 
                            var clean = it.trim()
                            if (clean.startsWith("Bearer ", ignoreCase = true)) {
                                clean = clean.substring(7).trim()
                            }
                            editDriveToken = clean
                        },
                        label = { Text("Drive Access Token (Optional)") },
                        placeholder = { Text("Paste token here") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        trailingIcon = {
                            if (editDriveToken.isNotBlank()) {
                                IconButton(onClick = { editDriveToken = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text ?: ""
                                if (clip.isNotBlank()) {
                                    var clean = clip.trim()
                                    if (clean.startsWith("Bearer ", ignoreCase = true)) {
                                        clean = clean.substring(7).trim()
                                    }
                                    editDriveToken = clean
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp), tint = WhatsAppGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste from Clipboard", fontSize = 12.sp, color = WhatsAppGreen)
                        }
                    }
                    }
                }
            },
            confirmButton = {
                if (showManualTokenEntry) {
                Button(
                    onClick = {
                        onSetGoogleAccountEmail(editAccountEmail)
                        onSaveDriveToken(editDriveToken)
                        showAccountDialog = false
                        Toast.makeText(context, "Google Account saved", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                ) {
                    Text("Save")
                }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAccountDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = WhatsAppGreen)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // -------------------------------------------------------------------------
    // DIALOG: Backup Network (WhatsApp Style)
    // -------------------------------------------------------------------------
    if (showNetworkDialog) {
        val networkOptions = listOf("Wi-Fi only", "Wi-Fi or cellular")
        AlertDialog(
            onDismissRequest = { showNetworkDialog = false },
            title = {
                Text(
                    "Back up over",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    networkOptions.forEach { net ->
                        val isSelected = backupNetwork.equals(net, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetBackupNetwork(net)
                                    showNetworkDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSetBackupNetwork(net)
                                    showNetworkDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = WhatsAppGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = net,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showNetworkDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = WhatsAppGreen)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // -------------------------------------------------------------------------
    // DIALOG: Google Drive Backups Browser & Restore
    // -------------------------------------------------------------------------
    if (showDriveFilesDialog) {
        AlertDialog(
            onDismissRequest = { showDriveFilesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = WhatsAppGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Drive Backups", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    if (isDriveLoading) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = WhatsAppGreen)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Connecting to Google Drive...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else if (driveBackups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("No Google Drive backups found", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap 'BACK UP' to upload your first cloud backup snapshot.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(driveBackups, key = { it.id }) { file ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(file.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            val sizeKb = if (file.sizeBytes > 0) "${file.sizeBytes / 1024} KB" else "Encrypted"
                                            val dateStr = if (file.createdTime.isNotBlank()) file.createdTime.take(19).replace("T", " ") else "Recent"
                                            Text("$sizeKb • $dateStr", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Button(
                                            onClick = { driveFileToRestore = file },
                                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                ) {
                    Text("Close")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Confirm restore from Drive dialog
    if (driveFileToRestore != null) {
        val file = driveFileToRestore!!
        AlertDialog(
            onDismissRequest = { driveFileToRestore = null },
            title = { Text("Restore from Google Drive?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will restore '${file.name}' from your Google Drive into LibDesk.\n\nAll student records, shifts, fees, and attendance will be restored. Current local data will be replaced.",
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
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { driveFileToRestore = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = WhatsAppGreen)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Local file restore dialog
    if (showRestoreConfirmDialog.value != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog.value = null },
            title = { Text("Restore Backup File?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will decrypt and restore your LibDesk database from the selected local file.\n\nCurrent data will be replaced.",
                    fontSize = 13.sp,
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
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreConfirmDialog.value = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = WhatsAppGreen)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Delete history dialog
    if (showDeleteConfirmDialog.value != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog.value = null },
            title = { Text("Delete Backup Entry?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove this entry from backup history?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val id = showDeleteConfirmDialog.value!!
                        showDeleteConfirmDialog.value = null
                        onDeleteHistory(id)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog.value = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // -------------------------------------------------------------------------
    // MAIN SCREEN SCAFFOLD (WhatsApp Layout)
    // -------------------------------------------------------------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Google Drive backup",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("backup_back_button")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // 1. TOP EXPLANATORY PARAGRAPH (Exact WhatsApp style)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Back up your library data and student records to Google Drive. You can restore them when you reinstall LibDesk or switch to a new phone. Your data will also back up to your phone's internal storage.",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // 2. LAST BACKUP STATUS CARD (Exact WhatsApp style)
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Last Backup",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val localTime = if (lastLocalBackupTime > 0) lastLocalBackupTime else lastBackupTime
                        val driveTime = lastDriveBackupTime

                        Text(
                            text = "Local: ${formatWhatsAppDate(localTime)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Google Drive: ${if (driveTime > 0) formatWhatsAppDate(driveTime) else "Never"}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Size: ${Formatter.formatShortFileSize(context, lastBackupSizeBytes)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (backupFrequency != "Never" && backupFrequency != "Only when I tap \"Back up\"") {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Auto-backup: $backupFrequency at 2:00 AM (${if (backupNetwork == "Wi-Fi") "Wi-Fi only" else "Wi-Fi or cellular"})${if (savedDriveToken.isNotBlank()) " • Synced to Drive" else ""}",
                                fontSize = 13.sp,
                                color = WhatsAppGreenDark,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big WhatsApp Green "BACK UP" Button
                        Button(
                            onClick = {
                                if (onPerformWhatsAppBackup != {}) {
                                    onPerformWhatsAppBackup()
                                } else {
                                    onBackupNow()
                                }
                            },
                            modifier = Modifier
                                .testTag("whatsapp_backup_button")
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            if (isDriveLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    driveStatusMessage ?: "Backing up...",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "BACK UP",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        if (driveStatusMessage != null && !isDriveLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = driveStatusMessage,
                                fontSize = 12.sp,
                                color = if (driveStatusMessage.contains("error", ignoreCase = true) || driveStatusMessage.contains("fail", ignoreCase = true)) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    WhatsAppGreen
                                }
                            )
                        }
                    }
                }
            }

            // DIVIDER
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // 3. GOOGLE DRIVE SETTINGS SECTION (WhatsApp Style)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Google Drive settings",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreenDark,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Item: Back up to Google Drive (Frequency)
                    WhatsAppSettingItem(
                        icon = Icons.Default.Schedule,
                        title = "Back up to Google Drive",
                        subtitle = backupFrequency,
                        onClick = { showFrequencyDialog = true },
                        testTag = "backup_frequency_setting"
                    )

                    // Item: Google Account
                    val accountSubtitle = when {
                        googleAccountEmail.isNotBlank() -> googleAccountEmail
                        savedDriveToken.isNotBlank() -> "Connected"
                        else -> "None"
                    }
                    WhatsAppSettingItem(
                        icon = Icons.Default.AccountCircle,
                        title = "Google Account",
                        subtitle = if (isSigningIntoGoogle) "Connecting…" else accountSubtitle,
                        onClick = {
                            if (googleAccountEmail.isBlank() && savedDriveToken.isBlank()) {
                                googleSignInError = null
                                isSigningIntoGoogle = true
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            } else {
                                showAccountDialog = true
                            }
                        },
                        testTag = "backup_account_setting"
                    )
                    if (googleSignInError != null) {
                        Text(
                            googleSignInError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    // Item: Back up over
                    WhatsAppSettingItem(
                        icon = Icons.Default.Wifi,
                        title = "Back up over",
                        subtitle = backupNetwork,
                        onClick = { showNetworkDialog = true },
                        testTag = "backup_network_setting"
                    )

                    // Item: Include student documents & receipts (Switch)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetIncludeDocuments(!includeDocuments) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include student documents & receipts",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Include student ID proofs, admission photos, and receipts",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = includeDocuments,
                            onCheckedChange = { onSetIncludeDocuments(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WhatsAppGreen
                            )
                        )
                    }
                }
            }

            // DIVIDER
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // 4. END-TO-END ENCRYPTED BACKUP (WhatsApp Style)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "End-to-end encrypted backup",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreenDark,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "End-to-end encrypted backup",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = WhatsAppGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "On",
                                        color = WhatsAppGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Your library backup is encrypted with AES-256 GCM. No one, not even Google or LibDesk, can read it.",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // DIVIDER
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // 5. RESTORE & RECOVERY SECTION
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Restore & Export",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreenDark,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Restore from Google Drive
                    WhatsAppSettingItem(
                        icon = Icons.Default.CloudDownload,
                        title = "Restore from Google Drive",
                        subtitle = if (savedDriveToken.isNotBlank()) "View cloud backups and restore" else "Connect Google account to browse backups",
                        onClick = {
                            if (savedDriveToken.isNotBlank()) {
                                onRefreshDriveFiles(savedDriveToken)
                            }
                            showDriveFilesDialog = true
                        },
                        testTag = "restore_gdrive_button"
                    )

                    // Restore from local storage
                    WhatsAppSettingItem(
                        icon = Icons.Default.FolderOpen,
                        title = "Restore from internal storage",
                        subtitle = "Select an encrypted .libdeskbackup file from phone storage",
                        onClick = {
                            importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        },
                        testTag = "restore_local_button"
                    )

                    // Export backup file
                    WhatsAppSettingItem(
                        icon = Icons.Default.FileDownload,
                        title = "Export backup file",
                        subtitle = "Save a copy of encrypted backup to Downloads or SD card",
                        onClick = {
                            val timeTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            exportLauncher.launch("libdesk_backup_$timeTag.enc")
                        },
                        testTag = "export_backup_button"
                    )
                }
            }

            // 6. RECENT BACKUPS HISTORY (if any exist)
            if (history.isNotEmpty()) {
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                item {
                    Text(
                        text = "Recent Backup History",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreenDark,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                items(history, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    dateFormatter.format(Date(item.timestamp)),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${Formatter.formatShortFileSize(context, item.sizeBytes)} • ${item.destination}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { showDeleteConfirmDialog.value = item.id },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Reusable WhatsApp-style settings row with icon, title, subtitle, and chevron.
 */
@Composable
private fun WhatsAppSettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
