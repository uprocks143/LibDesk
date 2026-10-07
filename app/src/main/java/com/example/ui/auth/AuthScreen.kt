package com.example.ui.auth

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.MembershipPlanEntity
import com.example.data.local.entities.ShiftEntity
import com.example.data.local.entities.SuperAdminUserEntity
import com.example.ui.components.CountryCodePhoneField
import kotlinx.coroutines.launch

private fun isValidLoginEmail(email: String): Boolean {
    val clean = email.trim()
    return clean.isNotBlank() && clean.contains("@") && clean.substringAfterLast("@").contains(".") && !clean.contains(" ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    libraries: List<LibraryEntity> = emptyList(),
    shifts: List<ShiftEntity> = emptyList(),
    plans: List<MembershipPlanEntity> = emptyList(),
    superAdminProfile: SuperAdminUserEntity? = null,
    isAwaiting2Fa: Boolean = false,
    twoFaTargetEmail: String = "",
    otpTimerSeconds: Int = 60,
    onRequest2FaOtp: (email: String, accessCode: String, onOtpSent: (String) -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onVerify2FaOtp: (enteredOtp: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _ -> },
    onResend2FaOtp: (onOtpSent: (String) -> Unit) -> Unit = {},
    onCancel2Fa: () -> Unit = {},
    onClaimAdminSlot: (name: String, email: String, mobile: String, pin: String, is2Fa: Boolean, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    onResetAdminSlot: () -> Unit = {},
    onResetPassword: (email: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onLogin: (email: String, role: String, name: String) -> Unit = { _, _, _ -> },
    onAuthenticate: (identifier: String, password: String, role: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onRegister: (name: String, email: String, libraryName: String, phone: String, password: String) -> Unit = { _, _, _, _, _ -> },
    onRequestOwnerSignupOtp: (email: String, name: String, phone: String, onOtpSent: (String) -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onVerifyOwnerSignupOtp: (email: String, enteredOtp: String, onVerified: (Boolean, String?) -> Unit) -> Unit = { _, _, _ -> },
    onCheckLibraryTrialEligibility: ((email: String, phone: String, onResult: (Boolean, String?) -> Unit) -> Unit)? = null,
    onStudentQrSignup: (libraryId: String, name: String, mobile: String, email: String, exam: String, shift: ShiftEntity?, plan: MembershipPlanEntity?, password: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rememberPrefs = remember {
        context.getSharedPreferences("libdesk_remember_me_prefs", Context.MODE_PRIVATE)
    }

    // Role state: "OWNER", "STUDENT", "SUPER_ADMIN"
    val savedRole = rememberPrefs.getString("saved_main_role", "OWNER") ?: "OWNER"
    var selectedRole by remember { mutableStateOf(savedRole) }

    // Auth mode: 0 -> Sign In, 1 -> Register
    var isRegisterMode by remember { mutableStateOf(false) }

    // Saved Credentials
    val savedRememberMe = rememberPrefs.getBoolean("remember_me_main", false)
    var rememberMe by remember { mutableStateOf(savedRememberMe) }
    var identifierInput by remember {
        mutableStateOf(if (savedRememberMe) rememberPrefs.getString("saved_main_identifier", "") ?: "" else "")
    }
    var passwordInput by remember {
        mutableStateOf(if (savedRememberMe) rememberPrefs.getString("saved_main_password", "") ?: "" else "")
    }
    var showPassword by remember { mutableStateOf(false) }
    var showRegPassword by remember { mutableStateOf(false) }
    var showRegConfirmPassword by remember { mutableStateOf(false) }
    var showStudentRegPassword by remember { mutableStateOf(false) }
    var showStudentRegConfirmPassword by remember { mutableStateOf(false) }
    var showClaimPin by remember { mutableStateOf(false) }
    var showClaimConfirmPin by remember { mutableStateOf(false) }
    var showForgotNewPassword by remember { mutableStateOf(false) }
    var showForgotConfirmPassword by remember { mutableStateOf(false) }

    // Registration Fields
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regCountryCode by remember { mutableStateOf("+91") }
    var studentPhone by remember { mutableStateOf("") }
    var studentCountryCode by remember { mutableStateOf("+91") }
    var regLibraryName by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var selectedLibraryForStudent by remember { mutableStateOf<LibraryEntity?>(libraries.firstOrNull()) }

    // Owner Registration 2-Step OTP Dialog State
    var showOwnerSignupOtpDialog by remember { mutableStateOf(false) }
    var ownerSignupOtpInput by remember { mutableStateOf("") }
    var ownerSignupOtpTimer by remember { mutableIntStateOf(60) }
    var isOwnerOtpVerifying by remember { mutableStateOf(false) }
    var isOwnerOtpSending by remember { mutableStateOf(false) }

    // Super Admin Claim Fields
    var claimName by remember { mutableStateOf("") }
    var claimEmail by remember { mutableStateOf("") }
    var claimMobile by remember { mutableStateOf("") }
    var claimCountryCode by remember { mutableStateOf("+91") }
    var claimPin by remember { mutableStateOf("") }
    var claimConfirmPin by remember { mutableStateOf("") }
    var claim2FaEnabled by remember { mutableStateOf(true) }

    // 2FA OTP Input
    var entered2FaOtp by remember { mutableStateOf("") }

    // Status / Error Messages
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Forgot Password Dialog State
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotEmailInput by remember { mutableStateOf("") }
    var forgotNewPasswordInput by remember { mutableStateOf("") }
    var forgotConfirmPasswordInput by remember { mutableStateOf("") }
    var forgotIsSubmitting by remember { mutableStateOf(false) }

    val isSuperAdminClaimed = superAdminProfile?.isClaimed == true

    fun saveRememberedCredentials() {
        if (rememberMe) {
            rememberPrefs.edit()
                .putBoolean("remember_me_main", true)
                .putString("saved_main_identifier", identifierInput.trim())
                .putString("saved_main_password", passwordInput.trim())
                .putString("saved_main_role", selectedRole)
                .apply()
        } else {
            rememberPrefs.edit().clear().apply()
        }
    }

    BackHandler(enabled = selectedRole == "SUPER_ADMIN") {
        selectedRole = "OWNER"
        errorMessage = null
        successMessage = null
    }

    // 2FA Verification View
    if (isAwaiting2Fa) {
        Dialog(onDismissRequest = onCancel2Fa) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().systemBarsPadding().imePadding().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Super Admin 2FA Security",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Verification code dispatched to $twoFaTargetEmail",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = entered2FaOtp,
                        onValueChange = { if (it.length <= 6) entered2FaOtp = it },
                        label = { Text("OTP Code") },
                        placeholder = { Text("123456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("two_fa_otp_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            if (entered2FaOtp.length < 6) {
                                errorMessage = "Please enter valid 6-digit OTP."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            onVerify2FaOtp(
                                entered2FaOtp,
                                {
                                    isLoading = false
                                    Toast.makeText(context, "Super Admin Authorized!", Toast.LENGTH_SHORT).show()
                                },
                                { err ->
                                    isLoading = false
                                    errorMessage = err
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_2fa_btn"),
                        enabled = !isLoading && entered2FaOtp.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Verify & Access Console")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { onResend2FaOtp { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } },
                            enabled = otpTimerSeconds <= 0
                        ) {
                            Text(if (otpTimerSeconds > 0) "Resend in ${otpTimerSeconds}s" else "Resend OTP")
                        }
                        TextButton(onClick = onCancel2Fa) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Owner Registration 2-Step OTP Verification Dialog
    if (showOwnerSignupOtpDialog) {
        Dialog(onDismissRequest = {
            if (!isOwnerOtpVerifying) {
                showOwnerSignupOtpDialog = false
            }
        }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().systemBarsPadding().imePadding().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
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
                            Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Verify Library Owner Email",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "A 6-digit registration code was sent to\n${regEmail.trim()}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = ownerSignupOtpInput,
                        onValueChange = { if (it.length <= 6) ownerSignupOtpInput = it },
                        label = { Text("6-Digit OTP Code") },
                        placeholder = { Text("123456") },
                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("owner_reg_otp_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = {
                            if (ownerSignupOtpInput.length < 6) {
                                errorMessage = "Please enter the valid 6-digit OTP."
                                return@Button
                            }
                            isOwnerOtpVerifying = true
                            errorMessage = null
                            onVerifyOwnerSignupOtp(
                                regEmail.trim(),
                                ownerSignupOtpInput.trim()
                            ) { isVerified, err ->
                                isOwnerOtpVerifying = false
                                if (isVerified) {
                                    showOwnerSignupOtpDialog = false
                                    isLoading = true
                                    onRegister(
                                        regName.trim(),
                                        regEmail.trim().lowercase(),
                                        regLibraryName.trim(),
                                        regPhone.trim(),
                                        regPassword.trim()
                                    )
                                } else {
                                    errorMessage = err ?: "Invalid OTP code. Please re-enter."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("owner_reg_otp_verify_btn"),
                        enabled = !isOwnerOtpVerifying,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isOwnerOtpVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify OTP & Create Library", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                isOwnerOtpSending = true
                                errorMessage = null
                                onRequestOwnerSignupOtp(
                                    regEmail.trim(),
                                    regName.trim(),
                                    regPhone.trim(),
                                    { msg ->
                                        isOwnerOtpSending = false
                                        ownerSignupOtpTimer = 60
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    { err ->
                                        isOwnerOtpSending = false
                                        errorMessage = err
                                    }
                                )
                            },
                            enabled = ownerSignupOtpTimer <= 0 && !isOwnerOtpSending
                        ) {
                            Text(if (ownerSignupOtpTimer > 0) "Resend in ${ownerSignupOtpTimer}s" else "Resend Code")
                        }
                        TextButton(onClick = { showOwnerSignupOtpDialog = false }) {
                            Text("Edit Details")
                        }
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            icon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Reset Account Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter your registered email and your new password to update your login credentials.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = forgotEmailInput,
                        onValueChange = { forgotEmailInput = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = forgotNewPasswordInput,
                        onValueChange = { forgotNewPasswordInput = it },
                        label = { Text("New Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showForgotNewPassword = !showForgotNewPassword }) {
                                Icon(
                                    if (showForgotNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showForgotNewPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (showForgotNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = forgotConfirmPasswordInput,
                        onValueChange = { forgotConfirmPasswordInput = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showForgotConfirmPassword = !showForgotConfirmPassword }) {
                                Icon(
                                    if (showForgotConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showForgotConfirmPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (showForgotConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotEmailInput.isBlank() || !forgotEmailInput.contains("@")) {
                            Toast.makeText(context, "Please enter valid email.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (forgotNewPasswordInput.length < 6) {
                            Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (forgotNewPasswordInput != forgotConfirmPasswordInput) {
                            Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        forgotIsSubmitting = true
                        onResetPassword(
                            forgotEmailInput.trim(),
                            forgotNewPasswordInput.trim(),
                            {
                                forgotIsSubmitting = false
                                showForgotPasswordDialog = false
                                Toast.makeText(context, "Password updated successfully. You can now log in.", Toast.LENGTH_LONG).show()
                            },
                            { err ->
                                forgotIsSubmitting = false
                                Toast.makeText(context, "Failed: $err", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !forgotIsSubmitting
                ) {
                    if (forgotIsSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Update Password")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Branding Header
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalLibrary,
                        contentDescription = "LibDesk Logo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "LibDesk Cloud",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Smart Multi-Tenant Library Management System",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (selectedRole != "SUPER_ADMIN") {
                    // Unified Role Selector (Step 1) - Only General Roles (Owner & Student)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                Triple("OWNER", "🏢 Library Owner", Icons.Default.Business),
                                Triple("STUDENT", "🎓 Student Member", Icons.Default.School)
                            ).forEach { (roleKey, label, icon) ->
                                val isSelected = selectedRole == roleKey
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedRole = roleKey
                                            errorMessage = null
                                            successMessage = null
                                        }
                                        .testTag("role_tab_$roleKey")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Mode Selector for Owner and Student: Sign In vs Register
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilterChip(
                            selected = !isRegisterMode,
                            onClick = {
                                isRegisterMode = false
                                errorMessage = null
                            },
                            label = { Text("🔑 Sign In") },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("mode_sign_in")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        FilterChip(
                            selected = isRegisterMode,
                            onClick = {
                                isRegisterMode = true
                                errorMessage = null
                            },
                            label = { Text("📝 Register New Account") },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("mode_register")
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // Header when in Super Admin view with back button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                selectedRole = "OWNER"
                                errorMessage = null
                                successMessage = null
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back to User Login")
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "👑 Super Admin Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Error / Success Banner
                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (successMessage != null) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = successMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }

                // Main Form Card (Step 2)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (selectedRole) {
                            // ==========================================
                            // 1. SUPER ADMIN FLOW
                            // ==========================================
                            "SUPER_ADMIN" -> {
                                if (isRegisterMode) {
                                    // Super Admin Claim Slot Form
                                    Text(
                                        text = "Initial Super Admin Setup",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Claim the master platform slot to manage subscriptions and tenant libraries.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = claimName,
                                        onValueChange = { claimName = it },
                                        label = { Text("Full Name") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = claimEmail,
                                        onValueChange = { claimEmail = it },
                                        label = { Text("Email") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    CountryCodePhoneField(
                                        mobile = claimMobile,
                                        onMobileChange = { claimMobile = it.filter { ch -> ch.isDigit() }.take(10) },
                                        countryCode = claimCountryCode,
                                        onCountryCodeChange = { claimCountryCode = it },
                                        label = "Mobile",
                                        placeholder = "9876543210",
                                        modifier = Modifier.fillMaxWidth().testTag("admin_claim_phone"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = claimPin,
                                        onValueChange = { claimPin = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { showClaimPin = !showClaimPin }) {
                                                Icon(
                                                    if (showClaimPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (showClaimPin) "Hide PIN" else "Show PIN"
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showClaimPin) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = claimConfirmPin,
                                        onValueChange = { claimConfirmPin = it },
                                        label = { Text("Confirm Password") },
                                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { showClaimConfirmPin = !showClaimConfirmPin }) {
                                                Icon(
                                                    if (showClaimConfirmPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (showClaimConfirmPin) "Hide PIN" else "Show PIN"
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showClaimConfirmPin) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = claim2FaEnabled,
                                            onCheckedChange = { claim2FaEnabled = it }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Enable 2FA Email Verification on Login", style = MaterialTheme.typography.bodySmall)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (claimName.isBlank() || claimEmail.isBlank() || claimPin.length < 4) {
                                                errorMessage = "Please fill all required administrator fields."
                                                return@Button
                                            }
                                            if (claimPin != claimConfirmPin) {
                                                errorMessage = "PINs do not match. Please re-enter."
                                                return@Button
                                            }
                                            isLoading = true
                                            errorMessage = null
                                            onClaimAdminSlot(
                                                claimName.trim(),
                                                claimEmail.trim(),
                                                if (claimMobile.isNotBlank()) "$claimCountryCode${claimMobile.trim()}" else "",
                                                claimPin.trim(),
                                                claim2FaEnabled,
                                                { msg ->
                                                    isLoading = false
                                                    successMessage = msg
                                                },
                                                { err ->
                                                    isLoading = false
                                                    errorMessage = err
                                                }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Text("Claim Master Slot & Initialize")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    TextButton(
                                        onClick = {
                                            isRegisterMode = false
                                            errorMessage = null
                                            successMessage = null
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Already registered? Log in with Super Admin Email & Password", style = MaterialTheme.typography.bodySmall)
                                    }
                                } else {
                                    // Super Admin Normal Login
                                    Text(
                                        text = "Super Administrator Access",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Enter your registered Super Admin email address with your access PIN/Password.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = identifierInput,
                                        onValueChange = { identifierInput = it },
                                        label = { Text("Email") },
                                        placeholder = { Text("admin@email.com") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_identifier_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        trailingIcon = {
                                             IconButton(onClick = { showPassword = !showPassword }) {
                                                 Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                             }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (identifierInput.isBlank() || passwordInput.isBlank()) {
                                                errorMessage = "Please enter your Super Admin Email address and Password."
                                                return@Button
                                            }
                                            if (!isValidLoginEmail(identifierInput)) {
                                                errorMessage = "Only registered Email address is allowed for login (e.g. admin@libdesk.com)."
                                                return@Button
                                            }
                                            isLoading = true
                                            errorMessage = null
                                            saveRememberedCredentials()
                                            onAuthenticate(
                                                identifierInput.trim().lowercase(),
                                                passwordInput.trim(),
                                                "SUPER_ADMIN",
                                                { isLoading = false },
                                                { err ->
                                                    isLoading = false
                                                    errorMessage = err
                                                }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_login_btn"),
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Log In as Super Admin", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    TextButton(
                                        onClick = {
                                            isRegisterMode = true
                                            errorMessage = null
                                            successMessage = null
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("First time setup? Claim Super Admin Platform Slot", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            // ==========================================
                            // 2. LIBRARY OWNER FLOW (Sign In / Register)
                            // ==========================================
                            "OWNER" -> {
                                if (isRegisterMode) {
                                    // Owner Registration Form
                                    Text(
                                        text = "Register Your Library",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Start managing your study hub, shifts, and students instantly.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = regName,
                                        onValueChange = { regName = it },
                                        label = { Text("Full Name") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("reg_owner_name"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = regLibraryName,
                                        onValueChange = { regLibraryName = it },
                                        label = { Text("Library Name") },
                                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("reg_library_name"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = regEmail,
                                        onValueChange = { regEmail = it },
                                        label = { Text("Email") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        modifier = Modifier.fillMaxWidth().testTag("reg_owner_email"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    CountryCodePhoneField(
                                        mobile = regPhone,
                                        onMobileChange = { regPhone = it.filter { ch -> ch.isDigit() }.take(10) },
                                        countryCode = regCountryCode,
                                        onCountryCodeChange = { regCountryCode = it },
                                        label = "Mobile",
                                        placeholder = "9876543210",
                                        modifier = Modifier.fillMaxWidth().testTag("reg_owner_phone"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = regPassword,
                                        onValueChange = { regPassword = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { showRegPassword = !showRegPassword }) {
                                                Icon(
                                                    if (showRegPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (showRegPassword) "Hide password" else "Show password"
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth().testTag("reg_owner_password"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = regConfirmPassword,
                                        onValueChange = { regConfirmPassword = it },
                                        label = { Text("Confirm Password") },
                                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { showRegConfirmPassword = !showRegConfirmPassword }) {
                                                Icon(
                                                    if (showRegConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (showRegConfirmPassword) "Hide password" else "Show password"
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showRegConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth().testTag("reg_owner_confirm_password"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (regName.isBlank() || regLibraryName.isBlank() || regEmail.isBlank() || regPassword.length < 6) {
                                                errorMessage = "Please enter Library Name, Owner Name, Email, and Password (min 6 chars)."
                                                return@Button
                                            }
                                            if (!isValidLoginEmail(regEmail)) {
                                                errorMessage = "Please enter a valid owner email address (e.g. owner@library.com)."
                                                return@Button
                                            }
                                            if (regPhone.isBlank() || regPhone.length < 10) {
                                                errorMessage = "Please enter a valid 10-digit mobile number."
                                                return@Button
                                            }
                                            if (regPassword != regConfirmPassword) {
                                                errorMessage = "Passwords do not match. Please re-check."
                                                return@Button
                                            }
                                            isLoading = true
                                            errorMessage = null
                                            onRegister(
                                                regName.trim(),
                                                regEmail.trim().lowercase(),
                                                regLibraryName.trim(),
                                                "$regCountryCode${regPhone.trim()}",
                                                regPassword.trim()
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("register_owner_submit_btn"),
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Register Library", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    // Owner Login Form
                                    Text(
                                        text = "Library Owner Sign In",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Enter your registered email address to access your library dashboard.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = identifierInput,
                                        onValueChange = { identifierInput = it },
                                        label = { Text("Email") },
                                        placeholder = { Text("name@email.com") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        modifier = Modifier.fillMaxWidth().testTag("owner_identifier_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { showPassword = !showPassword }) {
                                                Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth().testTag("owner_password_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = rememberMe,
                                                onCheckedChange = { rememberMe = it }
                                            )
                                            Text("Remember Me", style = MaterialTheme.typography.bodySmall)
                                        }

                                        TextButton(onClick = {
                                            forgotEmailInput = identifierInput
                                            showForgotPasswordDialog = true
                                        }) {
                                            Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            if (identifierInput.isBlank() || passwordInput.isBlank()) {
                                                errorMessage = "Please enter your registered Email address and Password."
                                                return@Button
                                            }
                                            if (!isValidLoginEmail(identifierInput)) {
                                                errorMessage = "Login is only supported via Email. Please enter a valid registered email address (e.g. owner@library.com)."
                                                return@Button
                                            }
                                            isLoading = true
                                            errorMessage = null
                                            saveRememberedCredentials()
                                            onAuthenticate(
                                                identifierInput.trim().lowercase(),
                                                passwordInput.trim(),
                                                "OWNER",
                                                { isLoading = false },
                                                { err ->
                                                    isLoading = false
                                                    errorMessage = err
                                                }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("owner_login_btn"),
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sign In as Library Owner", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // ==========================================
                            // 3. STUDENT MEMBER FLOW (Sign In / Register)
                            // ==========================================
                            "STUDENT" -> {
                                if (isRegisterMode) {
                                    // Student Self-Registration Form
                                    Text(
                                        text = "Student Registration",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Register for your student pass and study material access.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = regName,
                                        onValueChange = { regName = it },
                                        label = { Text("Full Name") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("student_reg_name"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = regEmail,
                                        onValueChange = { regEmail = it },
                                        label = { Text("Email") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        modifier = Modifier.fillMaxWidth().testTag("student_reg_email"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    CountryCodePhoneField(
                                        mobile = studentPhone,
                                        onMobileChange = { studentPhone = it.filter { ch -> ch.isDigit() }.take(10) },
                                        countryCode = studentCountryCode,
                                        onCountryCodeChange = { studentCountryCode = it },
                                        label = "Mobile",
                                        placeholder = "9876543210",
                                        modifier = Modifier.fillMaxWidth().testTag("student_reg_phone"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Library Selection if multiple exist
                                    if (libraries.isNotEmpty()) {
                                        var libDropdownExpanded by remember { mutableStateOf(false) }
                                        ExposedDropdownMenuBox(
                                            expanded = libDropdownExpanded,
                                            onExpandedChange = { libDropdownExpanded = !libDropdownExpanded }
                                        ) {
                                            OutlinedTextField(
                                                value = selectedLibraryForStudent?.name ?: "Select Library",
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Library") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = libDropdownExpanded) },
                                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = libDropdownExpanded,
                                                onDismissRequest = { libDropdownExpanded = false }
                                            ) {
                                                libraries.forEach { lib ->
                                                    DropdownMenuItem(
                                                        text = { Text("${lib.name} (${lib.code})") },
                                                        onClick = {
                                                            selectedLibraryForStudent = lib
                                                            libDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }

                                    OutlinedTextField(
                                        value = regPassword,
                                        onValueChange = { regPassword = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { showStudentRegPassword = !showStudentRegPassword }) {
                                                Icon(
                                                    if (showStudentRegPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (showStudentRegPassword) "Hide password" else "Show password"
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showStudentRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth().testTag("student_reg_password"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = regConfirmPassword,
                                        onValueChange = { regConfirmPassword = it },
                                        label = { Text("Confirm Password") },
                                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { showStudentRegConfirmPassword = !showStudentRegConfirmPassword }) {
                                                Icon(
                                                    if (showStudentRegConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (showStudentRegConfirmPassword) "Hide password" else "Show password"
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showStudentRegConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth().testTag("student_reg_confirm_password"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            if (regName.isBlank() || regEmail.isBlank() || regPassword.length < 6) {
                                                errorMessage = "Please enter your Name, Email (for login), and a 6-character password."
                                                return@Button
                                            }
                                            if (!isValidLoginEmail(regEmail)) {
                                                errorMessage = "Please enter a valid email address (e.g. student@gmail.com)."
                                                return@Button
                                            }
                                            if (studentPhone.isBlank() || studentPhone.length < 10) {
                                                errorMessage = "Please enter a valid 10-digit mobile number."
                                                return@Button
                                            }
                                            if (regPassword != regConfirmPassword) {
                                                errorMessage = "Passwords do not match. Please re-enter."
                                                return@Button
                                            }
                                            val targetLibId = selectedLibraryForStudent?.id ?: libraries.firstOrNull()?.id ?: ""
                                            isLoading = true
                                            errorMessage = null
                                            onStudentQrSignup(
                                                targetLibId,
                                                regName.trim(),
                                                "$studentCountryCode${studentPhone.trim()}",
                                                regEmail.trim().lowercase(),
                                                "General",
                                                shifts.firstOrNull(),
                                                plans.firstOrNull(),
                                                regPassword.trim()
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("student_reg_submit_btn"),
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Enroll as Student", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    // Student Login Form
                                    Text(
                                        text = "Student Member Sign In",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Enter your registered email address to access your student portal.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = identifierInput,
                                        onValueChange = { identifierInput = it },
                                        label = { Text("Email") },
                                        placeholder = { Text("name@email.com") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        modifier = Modifier.fillMaxWidth().testTag("student_identifier_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = passwordInput,
                                        onValueChange = { passwordInput = it },
                                        label = { Text("Password") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { showPassword = !showPassword }) {
                                                Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth().testTag("student_password_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = rememberMe,
                                                onCheckedChange = { rememberMe = it }
                                            )
                                            Text("Remember Me", style = MaterialTheme.typography.bodySmall)
                                        }

                                        TextButton(onClick = {
                                            forgotEmailInput = identifierInput
                                            showForgotPasswordDialog = true
                                        }) {
                                            Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            if (identifierInput.isBlank() || passwordInput.isBlank()) {
                                                errorMessage = "Please enter your registered Email address and Password."
                                                return@Button
                                            }
                                            if (!isValidLoginEmail(identifierInput)) {
                                                errorMessage = "Login is only supported via Email. Please enter a valid registered email address (e.g. student@gmail.com). Mobile or Student ID cannot be used."
                                                return@Button
                                            }
                                            isLoading = true
                                            errorMessage = null
                                            saveRememberedCredentials()
                                            onAuthenticate(
                                                identifierInput.trim().lowercase(),
                                                passwordInput.trim(),
                                                "STUDENT",
                                                { isLoading = false },
                                                { err ->
                                                    isLoading = false
                                                    errorMessage = err
                                                }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("student_login_btn"),
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sign In as Student", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Subtle, compact Super Admin login button at bottom
                if (selectedRole != "SUPER_ADMIN") {
                    TextButton(
                        onClick = {
                            selectedRole = "SUPER_ADMIN"
                            isRegisterMode = false
                            errorMessage = null
                            successMessage = null
                        },
                        modifier = Modifier.testTag("super_admin_footer_link")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "👑 Super Admin Login",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer Info
                Text(
                    text = "🔒 LibDesk Enterprise Multi-Tenant Architecture",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            }
        }
    }
}
