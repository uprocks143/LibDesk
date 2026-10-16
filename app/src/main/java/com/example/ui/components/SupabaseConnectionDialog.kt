package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.launch

private val SupabaseEmerald = Color(0xFF3ECF8E)
private val SupabaseDark = Color(0xFF1C1C1C)

/**
 * Enterprise Supabase Cloud Database & Authentication Configuration Dialog.
 * Allows administrators and library owners to view connection health, enter Project URL
 * and Anon API Key, test live connectivity to Database & Auth, and synchronize records.
 */
@Composable
fun SupabaseConnectionDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSaveCredentials: (String, String) -> Unit,
    onSyncDatabase: (() -> Unit)? = null,
    isSyncing: Boolean = false,
    syncStatusMessage: String? = null
) {
    if (!isOpen) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var projectUrl by remember { mutableStateOf(SupabaseClient.getEffectiveUrl()) }
    var apiKey by remember { mutableStateOf(SupabaseClient.getEffectiveApiKey()) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testReport by remember { mutableStateOf<SupabaseClient.SupabaseConnectionReport?>(null) }
    var showHelpGuide by remember { mutableStateOf(false) }

    // Initial silent check if keys exist
    LaunchedEffect(Unit) {
        if (projectUrl.isNotBlank() && apiKey.isNotBlank()) {
            isTestingConnection = true
            try {
                testReport = SupabaseClient.testConnectionDetailed(projectUrl, apiKey)
            } finally {
                isTestingConnection = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 520.dp)
                .fillMaxHeight(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with icon, title, and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SupabaseEmerald.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Supabase Logo",
                                tint = SupabaseEmerald,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Supabase Cloud Database & Auth",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "सुपाबेस डेटाबेस एवं ऑथेंटिकेशन कनेक्शन",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("supabase_dialog_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Status Badge Banner
                    val report = testReport
                    val isConfigured = SupabaseClient.isConfigured()
                    val statusBg = when {
                        report?.isFullyConnected == true -> Color(0xFFE8F5E9)
                        report?.isAuthConnected == true && !report.isDatabaseConnected -> Color(0xFFFFF3E0)
                        isConfigured -> Color(0xFFE3F2FD)
                        else -> Color(0xFFFFEBEE)
                    }
                    val statusText = when {
                        report?.isFullyConnected == true -> "Connected & Active (${report.latencyMs}ms)"
                        report?.isAuthConnected == true && !report.isDatabaseConnected -> "Auth Active • Database Needs Anon Key"
                        isConfigured -> "Configured • Ready to Test"
                        else -> "Not Connected • Please Enter Project Keys"
                    }
                    val statusDotColor = when {
                        report?.isFullyConnected == true -> Color(0xFF2E7D32)
                        report?.isAuthConnected == true && !report.isDatabaseConnected -> Color(0xFFE65100)
                        isConfigured -> Color(0xFF1976D2)
                        else -> Color(0xFFD32F2F)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusDotColor.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("supabase_live_status_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(statusDotColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = statusText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = statusDotColor
                                )
                                if (report != null) {
                                    Text(
                                        text = "🗄️ DB: ${if (report.isDatabaseConnected) "OK" else "Pending"} | 🔐 Auth: ${if (report.isAuthConnected) "OK" else "Pending"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Project URL input
                    Text(
                        text = "Supabase Project URL",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = projectUrl,
                        onValueChange = { projectUrl = it.trim() },
                        placeholder = { Text("https://your-project.supabase.co", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = null, tint = SupabaseEmerald)
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (projectUrl.isNotBlank()) {
                                    IconButton(onClick = { projectUrl = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text?.trim()
                                        if (!clip.isNullOrBlank() && clip.startsWith("http")) {
                                            projectUrl = clip
                                            Toast.makeText(context, "URL pasted from clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("supabase_input_project_url"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Anon / Public API Key input
                    Text(
                        text = "Supabase Anon / Public Key (or Publishable Key)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it.trim() },
                        placeholder = { Text("eyJhbGciOi... (anon public key)", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = SupabaseEmerald)
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text?.trim()
                                        if (!clip.isNullOrBlank()) {
                                            apiKey = clip
                                            Toast.makeText(context, "Key pasted from clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("supabase_input_api_key"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "Note: Database tables need the 'anon public' key from Supabase Dashboard > Project Settings > API.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test Connection Button & Indicator
                    OutlinedButton(
                        onClick = {
                            if (!SupabaseClient.isValidUrl(projectUrl)) {
                                Toast.makeText(context, "Please enter a valid Supabase URL", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            if (!SupabaseClient.isValidKey(apiKey)) {
                                Toast.makeText(context, "Please enter a valid API key", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            coroutineScope.launch {
                                isTestingConnection = true
                                try {
                                    testReport = SupabaseClient.testConnectionDetailed(projectUrl, apiKey)
                                } finally {
                                    isTestingConnection = false
                                }
                            }
                        },
                        enabled = !isTestingConnection && projectUrl.isNotBlank() && apiKey.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("supabase_btn_test_connection"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SupabaseEmerald)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = SupabaseEmerald
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pinging Database & Auth...", color = SupabaseEmerald, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Sensors, contentDescription = null, tint = SupabaseEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Live Connection", color = SupabaseEmerald, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Test Results Detailed Card
                    if (report != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (report.isDatabaseConnected) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (report.isDatabaseConnected) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Database (PostgREST): ${report.databaseMessage}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (report.isAuthConnected) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (report.isAuthConnected) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Auth Service: ${report.authMessage}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Collapsible "Where to find keys" guide
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHelpGuide = !showHelpGuide }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Where to find URL & Anon Key? (चाबी कहां मिलेगी?)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = if (showHelpGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            AnimatedVisibility(visible = showHelpGuide) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    Text("1. Open supabase.com/dashboard and open your project.", fontSize = 12.sp)
                                    Text("2. Click on ⚙️ Project Settings (bottom left).", fontSize = 12.sp)
                                    Text("3. Tap on \"API\" from the menu.", fontSize = 12.sp)
                                    Text("4. Under \"Project URL\", copy the URL.", fontSize = 12.sp)
                                    Text("5. Under \"Project API Keys\", copy the \"anon public\" key (starts with eyJ...).", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://supabase.com/dashboard"))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SupabaseEmerald),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Supabase Dashboard", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Optional Sync Section if connected
                    if (onSyncDatabase != null && SupabaseClient.isConfigured()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Cloud Database Sync",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Push your offline local library, seats, students, and plans to Supabase cloud database.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onSyncDatabase,
                                    enabled = !isSyncing,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("supabase_btn_sync_now"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Synchronizing Tables...", fontSize = 12.sp)
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sync Local Data to Supabase Now", fontSize = 12.sp)
                                    }
                                }
                                if (!syncStatusMessage.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = syncStatusMessage,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("supabase_dialog_cancel_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val cleanUrl = projectUrl.trim().removeSuffix("/")
                            val cleanKey = apiKey.trim()
                            if (!SupabaseClient.isValidUrl(cleanUrl)) {
                                Toast.makeText(context, "Invalid Supabase Project URL", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!SupabaseClient.isValidKey(cleanKey)) {
                                Toast.makeText(context, "Invalid Supabase API Key", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onSaveCredentials(cleanUrl, cleanKey)
                            Toast.makeText(context, "Supabase Database & Auth Connected!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("supabase_dialog_save_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SupabaseEmerald),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Connect", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
