package com.example.ui.auth

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.remote.SupabaseClientConfig
import kotlinx.coroutines.launch

/**
 * Validates standard email address pattern.
 */
private fun isValidEmailAddress(email: String): Boolean {
    val clean = email.trim()
    return clean.isNotBlank() && clean.contains("@") && clean.substringAfterLast("@").contains(".") && !clean.contains(" ")
}

/**
 * Dedicated Jetpack Compose Login and Registration UI that directly utilizes
 * the official Supabase Auth client [SupabaseClientConfig] for email/password authentication.
 *
 * Supports:
 * - Direct Supabase Email & Password Sign In
 * - Direct Supabase Email & Password Sign Up with User Metadata
 * - Supabase Self-Service Password Reset
 * - Role selection (Library Manager vs Student Member)
 * - Remember Me local persistence
 * - Edge-to-edge Material Design 3 layout with full accessibility compliance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseAuthScreen(
    onAuthSuccess: (email: String, role: String, name: String) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Local preferences for Remember Me
    val prefs = remember {
        context.getSharedPreferences("supabase_auth_prefs", Context.MODE_PRIVATE)
    }

    // Auth Mode: 0 = Log In (Sign In), 1 = Sign Up (Register)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Form Fields - Login
    val savedRememberMe = remember { prefs.getBoolean("remember_supabase_user", false) }
    var rememberMe by remember { mutableStateOf(savedRememberMe) }
    var loginEmail by remember {
        mutableStateOf(if (savedRememberMe) prefs.getString("saved_supabase_email", "") ?: "" else "")
    }
    var loginPassword by remember {
        mutableStateOf(if (savedRememberMe) prefs.getString("saved_supabase_password", "") ?: "" else "")
    }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Form Fields - Sign Up
    var regFullName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("OWNER") } // "OWNER" or "STUDENT"
    var showRegPassword by remember { mutableStateOf(false) }
    var showRegConfirmPassword by remember { mutableStateOf(false) }

    // UI States
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Forgot Password Dialog State
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var isForgotPasswordLoading by remember { mutableStateOf(false) }

    // Helper to persist credentials when Remember Me is enabled
    fun persistCredentialsIfRemembered(email: String, pass: String) {
        if (rememberMe) {
            prefs.edit()
                .putBoolean("remember_supabase_user", true)
                .putString("saved_supabase_email", email.trim())
                .putString("saved_supabase_password", pass.trim())
                .apply()
        } else {
            prefs.edit().clear().apply()
        }
    }

    BackHandler {
        onNavigateBack()
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        Dialog(onDismissRequest = { if (!isForgotPasswordLoading) showForgotPasswordDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.LockReset,
                            contentDescription = "Reset Password Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Enter your registered email address to receive a secure Supabase password recovery link.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { forgotPasswordEmail = it },
                        label = { Text("Registered Email") },
                        placeholder = { Text("user@example.com") },
                        leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("supabase_forgot_password_email_input")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showForgotPasswordDialog = false },
                            enabled = !isForgotPasswordLoading,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (!isValidEmailAddress(forgotPasswordEmail)) {
                                    Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isForgotPasswordLoading = true
                                coroutineScope.launch {
                                    val result = SupabaseClientConfig.sendPasswordReset(forgotPasswordEmail)
                                    isForgotPasswordLoading = false
                                    if (result.isSuccess) {
                                        showForgotPasswordDialog = false
                                        successMessage = "Password reset instructions sent to ${forgotPasswordEmail.trim()} via Supabase Auth."
                                        Toast.makeText(context, "Reset instructions sent to your email", Toast.LENGTH_LONG).show()
                                    } else {
                                        val err = result.exceptionOrNull()?.message ?: "Failed to dispatch password reset."
                                        Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isForgotPasswordLoading && forgotPasswordEmail.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("supabase_send_reset_button")
                        ) {
                            if (isForgotPasswordLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Send Link")
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("supabase_auth_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Main Portal",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Live Supabase Status Chip
                    val isConfigured = SupabaseClientConfig.isConfigured()
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isConfigured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.testTag("supabase_status_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConfigured) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isConfigured) "Supabase Online" else "Unconfigured",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isConfigured) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Max Width Container for Desktop/Tablet Responsiveness
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Branding
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Supabase Cloud Auth",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Supabase Auth",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Direct Email & Password Authentication",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tab Selector: Log In vs Sign Up
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                                successMessage = null
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Log In", fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("supabase_auth_tab_login")
                        )

                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                                successMessage = null
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sign Up", fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("supabase_auth_tab_register")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Status Messages (Error / Success Banners)
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { errorMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss error",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = successMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (successMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = successMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF1B5E20),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { successMessage = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss success",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Main Form Card
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            if (selectedTab == 0) {
                                // ==========================================
                                // 1. LOG IN VIEW (SIGN IN VIA SUPABASE)
                                // ==========================================
                                Text(
                                    text = "Welcome Back",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Sign in to access your cloud-synced library workspace.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Email Field
                                OutlinedTextField(
                                    value = loginEmail,
                                    onValueChange = {
                                        loginEmail = it
                                        errorMessage = null
                                    },
                                    label = { Text("Email Address") },
                                    placeholder = { Text("manager@library.com") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Email, contentDescription = null)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("supabase_auth_email_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Password Field
                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = {
                                        loginPassword = it
                                        errorMessage = null
                                    },
                                    label = { Text("Password") },
                                    placeholder = { Text("Enter password") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Lock, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { showLoginPassword = !showLoginPassword },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (showLoginPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (showLoginPassword) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("supabase_auth_password_input")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Remember Me & Forgot Password
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                                    ) {
                                        Checkbox(
                                            checked = rememberMe,
                                            onCheckedChange = { rememberMe = it },
                                            modifier = Modifier.testTag("supabase_auth_remember_me_checkbox")
                                        )
                                        Text(
                                            text = "Remember me",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            forgotPasswordEmail = loginEmail.trim()
                                            showForgotPasswordDialog = true
                                        },
                                        modifier = Modifier.testTag("supabase_forgot_password_button")
                                    ) {
                                        Text(
                                            text = "Forgot?",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Log In Button
                                Button(
                                    onClick = {
                                        val cleanEmail = loginEmail.trim()
                                        val cleanPass = loginPassword.trim()

                                        if (!isValidEmailAddress(cleanEmail)) {
                                            errorMessage = "Please enter a valid email address (e.g. user@example.com)"
                                            return@Button
                                        }
                                        if (cleanPass.isBlank()) {
                                            errorMessage = "Please enter your password."
                                            return@Button
                                        }

                                        isLoading = true
                                        errorMessage = null
                                        successMessage = null
                                        focusManager.clearFocus()

                                        coroutineScope.launch {
                                            val result = SupabaseClientConfig.signInWithEmail(cleanEmail, cleanPass)
                                            isLoading = false

                                            if (result.isSuccess) {
                                                val session = result.getOrNull()
                                                val user = session?.user
                                                val userMetadata = user?.userMetadata
                                                val fullName = userMetadata?.get("full_name")?.toString()?.replace("\"", "")
                                                    ?: userMetadata?.get("name")?.toString()?.replace("\"", "")
                                                    ?: cleanEmail.substringBefore("@")
                                                val role = userMetadata?.get("role")?.toString()?.replace("\"", "") ?: "OWNER"

                                                persistCredentialsIfRemembered(cleanEmail, cleanPass)
                                                Toast.makeText(context, "Logged in via Supabase successfully!", Toast.LENGTH_SHORT).show()
                                                onAuthSuccess(cleanEmail, role, fullName)
                                            } else {
                                                val rawErr = result.exceptionOrNull()?.message ?: ""
                                                val friendlyMsg = when {
                                                    rawErr.contains("Invalid login credentials", ignoreCase = true) ->
                                                        "Invalid email or password. Please verify and try again."
                                                    rawErr.contains("Email not confirmed", ignoreCase = true) ->
                                                        "Email confirmation pending. Please check your inbox or Supabase dashboard."
                                                    rawErr.contains("Failed to connect", ignoreCase = true) || rawErr.contains("timeout", ignoreCase = true) ->
                                                        "Unable to reach Supabase server. Please check your internet connection."
                                                    else ->
                                                        rawErr.ifBlank { "Sign in failed. Please check credentials." }
                                                }
                                                errorMessage = friendlyMsg
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("supabase_auth_login_button")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Log In with Supabase", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Quick Demo Helper Chips
                                Text(
                                    text = "Quick Demo Fill:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AssistChip(
                                        onClick = {
                                            loginEmail = "manager@libdesk.com"
                                            loginPassword = "Password@123"
                                            errorMessage = null
                                        },
                                        label = { Text("Manager Demo", style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    AssistChip(
                                        onClick = {
                                            loginEmail = "student@libdesk.com"
                                            loginPassword = "Password@123"
                                            errorMessage = null
                                        },
                                        label = { Text("Student Demo", style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = {
                                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                // ==========================================
                                // 2. SIGN UP VIEW (REGISTRATION VIA SUPABASE)
                                // ==========================================
                                Text(
                                    text = "Create Cloud Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Register with Supabase to sync your data across all devices.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Role Selector
                                Text(
                                    text = "Select Account Role:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = regRole == "OWNER",
                                        onClick = { regRole = "OWNER" },
                                        label = { Text("🏢 Library Manager") },
                                        leadingIcon = if (regRole == "OWNER") {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("supabase_role_owner_chip")
                                    )
                                    FilterChip(
                                        selected = regRole == "STUDENT",
                                        onClick = { regRole = "STUDENT" },
                                        label = { Text("🎓 Student") },
                                        leadingIcon = if (regRole == "STUDENT") {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("supabase_role_student_chip")
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Full Name Field
                                OutlinedTextField(
                                    value = regFullName,
                                    onValueChange = {
                                        regFullName = it
                                        errorMessage = null
                                    },
                                    label = { Text("Full Name") },
                                    placeholder = { Text("e.g. Ramesh Sharma") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Person, contentDescription = null)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("supabase_auth_name_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Email Field
                                OutlinedTextField(
                                    value = regEmail,
                                    onValueChange = {
                                        regEmail = it
                                        errorMessage = null
                                    },
                                    label = { Text("Email Address") },
                                    placeholder = { Text("name@example.com") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Email, contentDescription = null)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("supabase_auth_reg_email_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Password Field
                                OutlinedTextField(
                                    value = regPassword,
                                    onValueChange = {
                                        regPassword = it
                                        errorMessage = null
                                    },
                                    label = { Text("Password (Min 6 chars)") },
                                    placeholder = { Text("Create password") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Lock, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { showRegPassword = !showRegPassword },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (showRegPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (showRegPassword) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("supabase_auth_reg_password_input")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Confirm Password Field
                                OutlinedTextField(
                                    value = regConfirmPassword,
                                    onValueChange = {
                                        regConfirmPassword = it
                                        errorMessage = null
                                    },
                                    label = { Text("Confirm Password") },
                                    placeholder = { Text("Re-enter password") },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { showRegConfirmPassword = !showRegConfirmPassword },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (showRegConfirmPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (showRegConfirmPassword) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showRegConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { focusManager.clearFocus() }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("supabase_auth_confirm_password_input")
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Sign Up Button
                                Button(
                                    onClick = {
                                        val cleanName = regFullName.trim()
                                        val cleanEmail = regEmail.trim()
                                        val cleanPass = regPassword.trim()
                                        val cleanConfirm = regConfirmPassword.trim()

                                        if (cleanName.isBlank()) {
                                            errorMessage = "Please enter your full name."
                                            return@Button
                                        }
                                        if (!isValidEmailAddress(cleanEmail)) {
                                            errorMessage = "Please enter a valid email address."
                                            return@Button
                                        }
                                        if (cleanPass.length < 6) {
                                            errorMessage = "Password must be at least 6 characters long."
                                            return@Button
                                        }
                                        if (cleanPass != cleanConfirm) {
                                            errorMessage = "Passwords do not match. Please re-check."
                                            return@Button
                                        }

                                        isLoading = true
                                        errorMessage = null
                                        successMessage = null
                                        focusManager.clearFocus()

                                        coroutineScope.launch {
                                            val result = SupabaseClientConfig.signUpWithEmail(
                                                email = cleanEmail,
                                                password = cleanPass,
                                                fullName = cleanName,
                                                role = regRole
                                            )
                                            isLoading = false

                                            if (result.isSuccess) {
                                                val user = result.getOrNull()
                                                val activeSession = SupabaseClientConfig.currentSessionOrNull()
                                                val isConfirmed = user?.emailConfirmedAt != null || activeSession != null
                                                if (isConfirmed && activeSession != null) {
                                                    persistCredentialsIfRemembered(cleanEmail, cleanPass)
                                                    Toast.makeText(context, "Account created & authorized!", Toast.LENGTH_SHORT).show()
                                                    onAuthSuccess(cleanEmail, regRole, cleanName)
                                                } else {
                                                    successMessage = "Account created successfully for $cleanEmail! You can now log in below."
                                                    // Switch to login tab and prefill
                                                    loginEmail = cleanEmail
                                                    loginPassword = cleanPass
                                                    selectedTab = 0
                                                }
                                            } else {
                                                val rawErr = result.exceptionOrNull()?.message ?: ""
                                                val friendlyMsg = when {
                                                    rawErr.contains("already registered", ignoreCase = true) || rawErr.contains("User already exists", ignoreCase = true) ->
                                                        "An account with this email already exists in Supabase. Please log in instead."
                                                    rawErr.contains("weak_password", ignoreCase = true) ->
                                                        "Password is too weak. Please use at least 6 characters."
                                                    else ->
                                                        rawErr.ifBlank { "Sign up failed. Please check details." }
                                                }
                                                errorMessage = friendlyMsg
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("supabase_auth_register_button")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Create Supabase Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secondary Action: Return to Offline / Standard Mode
                    OutlinedButton(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("supabase_auth_offline_button")
                    ) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back to Standard Login Portal", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Security & Project Info Note
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "End-to-end encrypted with Supabase Auth & JWT. Credentials never stored in plain text.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
