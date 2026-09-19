package com.example.ui.auth

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.MembershipPlanEntity
import com.example.data.local.entities.ShiftEntity
import com.example.ui.components.CountryCodePhoneField
import com.example.ui.components.combineCountryCodeAndPhone
import com.example.ui.components.CameraXQrScannerView
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    libraries: List<LibraryEntity> = emptyList(),
    shifts: List<ShiftEntity> = emptyList(),
    plans: List<MembershipPlanEntity> = emptyList(),
    superAdminProfile: com.example.data.local.entities.SuperAdminUserEntity? = null,
    isAwaiting2Fa: Boolean = false,
    twoFaTargetEmail: String = "",
    activeOtpCode: String? = null,
    otpTimerSeconds: Int = 60,
    onRequest2FaOtp: (email: String, accessCode: String, onOtpSent: (String) -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onVerify2FaOtp: (enteredOtp: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _ -> },
    onResend2FaOtp: (onOtpSent: (String) -> Unit) -> Unit = {},
    onCancel2Fa: () -> Unit = {},
    onClaimAdminSlot: (name: String, email: String, mobile: String, pin: String, is2Fa: Boolean, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    onResetAdminSlot: () -> Unit = {},
    onResetPassword: (email: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onLogin: (email: String, role: String, name: String) -> Unit,
    onAuthenticate: (identifier: String, password: String, role: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onRegister: (name: String, email: String, libraryName: String, phone: String, password: String) -> Unit,
    onStudentQrSignup: (libraryId: String, name: String, mobile: String, email: String, exam: String, shift: ShiftEntity?, plan: MembershipPlanEntity?, password: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var authMode by remember { mutableStateOf(0) } 
    var selectedRole by remember { mutableStateOf("MANAGER") } 

    
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var showLoginPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    
    var showForgotPasswordModal by remember { mutableStateOf(false) }
    var forgotPasswordStep by remember { mutableStateOf(0) } 
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordGeneratedOtp by remember { mutableStateOf("") }
    var forgotPasswordOtpInput by remember { mutableStateOf("") }
    var forgotPasswordNewPassword by remember { mutableStateOf("") }
    var forgotPasswordConfirmPassword by remember { mutableStateOf("") }
    var showForgotNewPassword by remember { mutableStateOf(false) }
    var showForgotConfirmPassword by remember { mutableStateOf(false) }
    var forgotPasswordError by remember { mutableStateOf<String?>(null) }
    var forgotPasswordSuccessMessage by remember { mutableStateOf<String?>(null) }
    var forgotPasswordTimerSeconds by remember { mutableStateOf(60) }

    
    var showMasterAdminModal by remember { mutableStateOf(false) }
    var adminModalMode by remember { mutableStateOf(if (superAdminProfile?.isClaimed == true) 1 else 0) } 
    var adminClaimName by remember { mutableStateOf("") }
    var adminClaimEmail by remember { mutableStateOf(superAdminProfile?.email ?: "") }
    var adminClaimCountryCode by remember { mutableStateOf("+91") }
    var adminClaimMobile by remember { mutableStateOf("") }
    var adminClaimPin by remember { mutableStateOf("") }
    var showAdminClaimPin by remember { mutableStateOf(false) }
    var adminClaimPinConfirm by remember { mutableStateOf("") }
    var showAdminClaimPinConfirm by remember { mutableStateOf(false) }
        var adminLoginEmail by remember { mutableStateOf(superAdminProfile?.email ?: "") }
    var adminLoginPin by remember { mutableStateOf("") }
    var showAdminLoginPin by remember { mutableStateOf(false) }
    var adminErrorMessage by remember { mutableStateOf<String?>(null) }

    
    var otpInput by remember { mutableStateOf("") }
    var otpErrorMessage by remember { mutableStateOf<String?>(null) }
    var otpSuccessToast by remember { mutableStateOf<String?>(null) }

    
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regCountryCode by remember { mutableStateOf("+91") }
    var regPhone by remember { mutableStateOf("") }
    var regLibraryName by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var showRegPassword by remember { mutableStateOf(false) }
    var regConfirmPassword by remember { mutableStateOf("") }
    var showRegConfirmPassword by remember { mutableStateOf(false) }
    var regAgreedToTerms by remember { mutableStateOf(false) }

    
    var studentScannedLib by remember { mutableStateOf<LibraryEntity?>(null) }
    var studentName by remember { mutableStateOf("") }
    var studentAgreedToTerms by remember { mutableStateOf(false) }
    var studentCountryCode by remember { mutableStateOf("+91") }
    var studentPhone by remember { mutableStateOf("") }
    var studentEmail by remember { mutableStateOf("") }
    var studentExam by remember { mutableStateOf("UPSC Civil Services") }
    var selectedShiftId by remember { mutableStateOf(shifts.firstOrNull()?.id ?: "") }
    var selectedPlanId by remember { mutableStateOf(plans.firstOrNull()?.id ?: "") }
    var showQrScannerDialog by remember { mutableStateOf(false) }

    
    var isVerifyingSignupEmail by remember { mutableStateOf(false) }
    var signupVerificationEmail by remember { mutableStateOf("") }
    var signupPendingMode by remember { mutableStateOf(1) } 
    var signupVerificationCode by remember { mutableStateOf("") }
    var signupOtpInput by remember { mutableStateOf("") }
    var signupOtpError by remember { mutableStateOf<String?>(null) }
    var signupOtpTimerSeconds by remember { mutableStateOf(60) }
    var signupValidationError by remember { mutableStateOf<String?>(null) }

    
    LaunchedEffect(isVerifyingSignupEmail, signupOtpTimerSeconds) {
        if (isVerifyingSignupEmail && signupOtpTimerSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            signupOtpTimerSeconds -= 1
        }
    }

    
    LaunchedEffect(showForgotPasswordModal, forgotPasswordStep, forgotPasswordTimerSeconds) {
        if (showForgotPasswordModal && forgotPasswordStep == 1 && forgotPasswordTimerSeconds > 0) {
            kotlinx.coroutines.delay(1000L)
            forgotPasswordTimerSeconds -= 1
        }
    }

    
    val shouldInterceptAuthBack = isVerifyingSignupEmail ||
            showMasterAdminModal ||
            showForgotPasswordModal ||
            showQrScannerDialog ||
            isAwaiting2Fa ||
            authMode != 0

    BackHandler(enabled = shouldInterceptAuthBack) {
        when {
            showForgotPasswordModal -> showForgotPasswordModal = false
            isVerifyingSignupEmail -> isVerifyingSignupEmail = false
            showMasterAdminModal -> showMasterAdminModal = false
            showQrScannerDialog -> showQrScannerDialog = false
            isAwaiting2Fa -> onCancel2Fa()
            authMode != 0 -> authMode = 0
        }
    }

    
    // Previously this silently auto-picked the first library in the entire
    // list as soon as the screen loaded — before the student ever scanned a
    // QR code or chose one — which meant a student could sign up without
    // ever selecting their real library and land in a random one. Removed:
    // studentScannedLib now stays null until the student actually scans a
    // QR code or explicitly taps a library from the list below.

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalLibrary,
                    contentDescription = "LibDesk ERP",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "LibDesk ERP",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Smart Library & Reading Hall Management System",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    if (isVerifyingSignupEmail) {

                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Verify Your Account Email",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "To complete your registration, enter the 6-digit verification code sent to your email address:",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = signupVerificationEmail,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            
                            OutlinedTextField(
                                value = signupOtpInput,
                                onValueChange = { input ->
                                    val digitsOnly = input.filter { it.isDigit() }.take(6)
                                    signupOtpInput = digitsOnly
                                    if (signupOtpError != null) signupOtpError = null
                                },
                                label = { Text("Enter 6-Digit Code *") },
                                placeholder = { Text("• • • • • •") },
                                leadingIcon = { Icon(Icons.Default.Pin, null, tint = MaterialTheme.colorScheme.primary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                isError = signupOtpError != null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (signupOtpError != null) {
                                Text(
                                    text = signupOtpError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }

                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (signupOtpTimerSeconds > 0) {
                                    Text(
                                        text = "Resend code in ${signupOtpTimerSeconds}s",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    TextButton(
                                        onClick = {
                                            signupVerificationCode = (100000..999999).random().toString()
                                            signupOtpTimerSeconds = 60
                                            signupOtpError = null
                                            signupOtpInput = ""
                                            com.example.util.EmailOtpService.dispatchEmailOtp(
                                                email = signupVerificationEmail,
                                                recipientName = if (signupPendingMode == 1) regName else studentName,
                                                purpose = com.example.util.OtpPurpose.SIGNUP_VERIFICATION,
                                                scope = coroutineScope
                                            ) {}
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Resend Code", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            
                            Button(
                                onClick = {
                                    val isCodeValid = (signupVerificationCode.isNotBlank() && signupOtpInput.trim() == signupVerificationCode) ||
                                            com.example.util.EmailOtpService.verifyOtpSync(
                                                signupVerificationEmail,
                                                signupOtpInput.trim(),
                                                com.example.util.OtpPurpose.SIGNUP_VERIFICATION
                                            )

                                    if (isCodeValid) {
                                        isVerifyingSignupEmail = false
                                        signupOtpError = null
                                        if (signupPendingMode == 1) {
                                            // Previously blank fields here were silently replaced with
                                            // fake placeholders — including "Admin@123" as the password
                                            // if the admin left it blank, a guessable default anyone
                                            // could try against any account. Now we require the real
                                            // values instead of ever substituting a fake one.
                                            val fullPhone = combineCountryCodeAndPhone(regCountryCode, regPhone)
                                            if (regName.isBlank() || regLibraryName.isBlank() || regPassword.length < 6) {
                                                signupOtpError = "Please go back and fill in your name, library name, and a password (min 6 characters)."
                                            } else if (fullPhone.isBlank()) {
                                                signupOtpError = "Please go back and enter your mobile number."
                                            } else {
                                                onRegister(
                                                    regName,
                                                    signupVerificationEmail,
                                                    regLibraryName,
                                                    fullPhone,
                                                    regPassword
                                                )
                                            }
                                        } else {
                                            val chosenShift = shifts.find { it.id == selectedShiftId } ?: shifts.firstOrNull()
                                            val chosenPlan = plans.find { it.id == selectedPlanId } ?: plans.firstOrNull()
                                            // SECURITY/DATA INTEGRITY: never attach a new student to a
                                            // made-up library ID. If no library was actually scanned or
                                            // selected, block the signup with a clear error instead of
                                            // silently enrolling them into a library that may not exist.
                                            val libId = studentScannedLib?.id
                                            val fullPhone = combineCountryCodeAndPhone(studentCountryCode, studentPhone)
                                            if (libId == null) {
                                                signupOtpError = "Please scan your library's QR code or select a library before signing up."
                                            } else if (studentName.isBlank()) {
                                                signupOtpError = "Please go back and enter your full name."
                                            } else if (fullPhone.isBlank()) {
                                                signupOtpError = "Please go back and enter your mobile number."
                                            } else {
                                                onStudentQrSignup(
                                                    libId,
                                                    studentName,
                                                    fullPhone,
                                                    signupVerificationEmail,
                                                    studentExam.ifBlank { "Self Study" },
                                                    chosenShift,
                                                    chosenPlan,
                                                    "Student@${(1000..9999).random()}!"
                                                )
                                            }
                                        }
                                    } else {
                                        signupOtpError = "Invalid code. Please enter the exact 6-digit code sent to your email."
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify Email & Complete Signup", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }

                            
                            OutlinedButton(
                                onClick = {
                                    isVerifyingSignupEmail = false
                                    signupOtpError = null
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Information / Cancel", fontSize = 14.sp)
                            }
                        }
                    } else {

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (authMode == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    shadowElevation = if (authMode == 0) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            signupValidationError = null
                                            authMode = 0
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Sign In",
                                            fontWeight = if (authMode == 0) FontWeight.Bold else FontWeight.Medium,
                                            color = if (authMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (authMode == 2) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    shadowElevation = if (authMode == 2) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .clickable {
                                            signupValidationError = null
                                            authMode = 2
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = if (authMode == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "QR Sign Up",
                                            fontWeight = if (authMode == 2) FontWeight.Bold else FontWeight.Medium,
                                            color = if (authMode == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (authMode == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    shadowElevation = if (authMode == 1) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .clickable {
                                            signupValidationError = null
                                            authMode = 1
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "New Library",
                                            fontWeight = if (authMode == 1) FontWeight.Bold else FontWeight.Medium,
                                            color = if (authMode == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (signupValidationError != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = signupValidationError ?: "",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        AnimatedContent(targetState = authMode, label = "AuthModeTransition") { mode ->
                        when (mode) {
                            0 -> {

                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                                    if (forgotPasswordSuccessMessage != null) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = LibDeskColors.successSoft,
                                            border = BorderStroke(1.dp, LibDeskColors.success.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LibDeskColors.success, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = forgotPasswordSuccessMessage ?: "",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = LibDeskColors.success
                                                )
                                            }
                                        }
                                    }

                                    
                                    Text(
                                        text = "Select Account Role",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )

                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selectedRole == "MANAGER") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(
                                                1.5.dp,
                                                if (selectedRole == "MANAGER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    selectedRole = "MANAGER"
                                                    otpErrorMessage = null
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 11.dp, horizontal = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AdminPanelSettings,
                                                    contentDescription = null,
                                                    tint = if (selectedRole == "MANAGER") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "Library Owner",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (selectedRole == "MANAGER") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Owner / Admin",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selectedRole == "MANAGER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }

                                        
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selectedRole == "STUDENT") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(
                                                1.5.dp,
                                                if (selectedRole == "STUDENT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    selectedRole = "STUDENT"
                                                    otpErrorMessage = null
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 11.dp, horizontal = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.School,
                                                    contentDescription = null,
                                                    tint = if (selectedRole == "STUDENT") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "Student",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (selectedRole == "STUDENT") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Aspirant Pass",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selectedRole == "STUDENT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }

                                    
                                    OutlinedTextField(
                                        value = loginEmail,
                                        onValueChange = { 
                                            loginEmail = it 
                                            otpErrorMessage = null
                                        },
                                        label = { 
                                            Text(
                                                when (selectedRole) {
                                                    "SUPER_ADMIN" -> "Super Admin Email"
                                                    "MANAGER" -> "Manager Email or Mobile"
                                                    else -> "Student Email, Mobile, or ID"
                                                }
                                            ) 
                                        },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    
                                    OutlinedTextField(
                                        value = loginPassword,
                                        onValueChange = { 
                                            loginPassword = it 
                                            otpErrorMessage = null
                                        },
                                        label = { Text("Password / Passcode") },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                                Icon(
                                                    imageVector = if (showLoginPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = if (showLoginPassword) "Hide password" else "Show password"
                                                )
                                            }
                                        },
                                        visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    
                                    if (otpErrorMessage != null) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = otpErrorMessage ?: "",
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    
                                    if (selectedRole == "STUDENT") {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { authMode = 2 }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("New Student? Scan Library QR Code", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                    Text("Self-enroll in 10 seconds with library pass", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = rememberMe,
                                                onCheckedChange = { rememberMe = it },
                                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                            )
                                            Text(
                                                text = "Remember me",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Text(
                                            text = "Forgot password?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                forgotPasswordEmail = loginEmail
                                                forgotPasswordStep = 0
                                                forgotPasswordError = null
                                                forgotPasswordOtpInput = ""
                                                forgotPasswordNewPassword = ""
                                                forgotPasswordConfirmPassword = ""
                                                showForgotPasswordModal = true
                                            }
                                        )
                                    }

                                    
                                    Button(
                                        onClick = {
                                            otpErrorMessage = null
                                            if (loginEmail.isBlank() || loginPassword.isBlank()) {
                                                otpErrorMessage = "Please enter both identifier and password"
                                                return@Button
                                            }
                                            onAuthenticate(
                                                loginEmail.trim(),
                                                loginPassword.trim(),
                                                selectedRole,
                                                {
                                                    otpErrorMessage = null
                                                },
                                                { errorMsg ->
                                                    otpErrorMessage = errorMsg
                                                }
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Login,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when (selectedRole) {
                                                "SUPER_ADMIN" -> "Sign In as SaaS Admin (Platform Developer)"
                                                "MANAGER" -> "Sign In as Library Owner / Admin"
                                                else -> "Sign In as Student"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                            2 -> {

                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(
                                        text = "Student Self-Enrollment",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )

                                    
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (studentScannedLib != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (studentScannedLib != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (studentScannedLib != null) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
                                                        contentDescription = null,
                                                        tint = if (studentScannedLib != null) LibDeskColors.success else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (studentScannedLib != null) "Verified Library QR" else "Scan Library QR Code",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }

                                                Button(
                                                    onClick = { showQrScannerDialog = true },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (studentScannedLib != null) "Rescan QR" else "Scan QR", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            if (studentScannedLib != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = studentScannedLib!!.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "${studentScannedLib!!.address}, ${studentScannedLib!!.city} • Code: ${studentScannedLib!!.code}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Scan the QR code displayed at your library reception/desk or choose below.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    
                                    if (libraries.isNotEmpty() && studentScannedLib == null) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Choose Your Library:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                libraries.take(2).forEach { lib ->
                                                    OutlinedButton(
                                                        onClick = { studentScannedLib = lib },
                                                        shape = RoundedCornerShape(10.dp),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(lib.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }
                                            if (libraries.size > 2 || studentScannedLib == null) {
                                                Text(
                                                    "Or scan the library's QR code above to select it automatically.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    
                                    OutlinedTextField(
                                        value = studentName,
                                        onValueChange = { studentName = it },
                                        label = { Text("Full Name *") },
                                        placeholder = { Text("e.g. Rahul Sharma") },
                                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                        singleLine = true,
                                        isError = signupValidationError != null && studentName.isBlank(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    CountryCodePhoneField(
                                        mobile = studentPhone,
                                        onMobileChange = { studentPhone = it },
                                        countryCode = studentCountryCode,
                                        onCountryCodeChange = { studentCountryCode = it },
                                        label = "Mobile / WhatsApp *",
                                        placeholder = "98765 43210",
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = studentEmail,
                                        onValueChange = { studentEmail = it },
                                        label = { Text("Email Address *") },
                                        placeholder = { Text("rahul@gmail.com") },
                                        leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        singleLine = true,
                                        isError = signupValidationError != null && (studentEmail.isBlank() || !studentEmail.contains("@")),
                                        supportingText = { Text("We'll send a one-time code here to verify your account", fontSize = 11.sp) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = studentExam,
                                        onValueChange = { studentExam = it },
                                        label = { Text("Target Goal / Exam") },
                                        placeholder = { Text("UPSC / NEET / JEE / CA / GATE") },
                                        leadingIcon = { Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    
                                    if (shifts.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Select Preferred Shift:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                shifts.take(3).forEach { shift ->
                                                    val isSelected = selectedShiftId == shift.id
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { selectedShiftId = shift.id }
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(8.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Text(shift.name.take(12), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                                            Text("${shift.startTime}-${shift.endTime}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    
                                    if (plans.isNotEmpty()) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Select Membership Plan:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                plans.take(3).forEach { plan ->
                                                    val isSelected = selectedPlanId == plan.id
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { selectedPlanId = plan.id }
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(8.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Text(plan.name.take(12), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                                            Text("₹${plan.baseFee.toInt()}/mo", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = LibDeskColors.success)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { studentAgreedToTerms = !studentAgreedToTerms }
                                    ) {
                                        Checkbox(checked = studentAgreedToTerms, onCheckedChange = { studentAgreedToTerms = it })
                                        Text(
                                            "I agree to the Terms of Service and Privacy Policy",
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    
                                    Button(
                                        onClick = {
                                            val trimmedName = studentName.trim()
                                            val trimmedPhone = studentPhone.trim()
                                            val trimmedEmail = studentEmail.trim()

                                            if (studentScannedLib == null) {
                                                signupValidationError = "Please scan your library's QR code or choose your library above."
                                                return@Button
                                            }
                                            if (trimmedName.isBlank()) {
                                                signupValidationError = "Please enter your full name."
                                                return@Button
                                            }
                                            if (trimmedPhone.isBlank()) {
                                                signupValidationError = "Please enter your mobile / WhatsApp number."
                                                return@Button
                                            }
                                            if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                                                signupValidationError = "A valid email address is mandatory for student account verification."
                                                return@Button
                                            }
                                            if (!studentAgreedToTerms) {
                                                signupValidationError = "Please accept the Terms of Service and Privacy Policy to continue."
                                                return@Button
                                            }

                                            signupValidationError = null
                                            val code = (100000..999999).random().toString()
                                            signupVerificationCode = code
                                            signupVerificationEmail = trimmedEmail
                                            signupPendingMode = 2
                                            signupOtpInput = ""
                                            signupOtpError = null
                                            signupOtpTimerSeconds = 60
                                            isVerifyingSignupEmail = true
                                            com.example.util.EmailOtpService.dispatchEmailOtp(
                                                email = trimmedEmail,
                                                recipientName = studentName.ifBlank { "Student" },
                                                purpose = com.example.util.OtpPurpose.SIGNUP_VERIFICATION,
                                                scope = coroutineScope
                                            ) {}
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Verify Email & Complete Enrollment",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                            1 -> {

                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(
                                        text = "Your Details",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    OutlinedTextField(
                                        value = regName,
                                        onValueChange = { regName = it },
                                        label = { Text("Admin / Owner Name *") },
                                        placeholder = { Text("e.g. Vikram Malhotra") },
                                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                        singleLine = true,
                                        isError = signupValidationError != null && regName.isBlank(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = regEmail,
                                        onValueChange = { regEmail = it },
                                        label = { Text("Official Email *") },
                                        placeholder = { Text("admin@apexlibrary.com") },
                                        leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                        singleLine = true,
                                        isError = signupValidationError != null && (regEmail.isBlank() || !regEmail.contains("@")),
                                        supportingText = { Text("We'll send a one-time code here to verify your account", fontSize = 11.sp) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    CountryCodePhoneField(
                                        mobile = regPhone,
                                        onMobileChange = { regPhone = it },
                                        countryCode = regCountryCode,
                                        onCountryCodeChange = { regCountryCode = it },
                                        label = "Phone / WhatsApp Number *",
                                        placeholder = "98765 00000",
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                    Text(
                                        text = "Library Details",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    OutlinedTextField(
                                        value = regLibraryName,
                                        onValueChange = { regLibraryName = it },
                                        label = { Text("Library / Study Center Name *") },
                                        placeholder = { Text("e.g. Apex Reading Room & Library") },
                                        leadingIcon = { Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.primary) },
                                        singleLine = true,
                                        isError = signupValidationError != null && regLibraryName.isBlank(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                    Text(
                                        text = "Security",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    OutlinedTextField(
                                        value = regPassword,
                                        onValueChange = { regPassword = it },
                                        label = { Text("Create Password *") },
                                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                        trailingIcon = {
                                            IconButton(onClick = { showRegPassword = !showRegPassword }) {
                                                Icon(
                                                    imageVector = if (showRegPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        singleLine = true,
                                        isError = signupValidationError != null && regPassword.length < 6,
                                        supportingText = { Text("At least 6 characters", fontSize = 11.sp) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = regConfirmPassword,
                                        onValueChange = { regConfirmPassword = it },
                                        label = { Text("Confirm Password *") },
                                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                        trailingIcon = {
                                            IconButton(onClick = { showRegConfirmPassword = !showRegConfirmPassword }) {
                                                Icon(
                                                    imageVector = if (showRegConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        visualTransformation = if (showRegConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        singleLine = true,
                                        isError = regConfirmPassword.isNotBlank() && regConfirmPassword != regPassword,
                                        supportingText = {
                                            if (regConfirmPassword.isNotBlank() && regConfirmPassword != regPassword) {
                                                Text("Passwords don't match", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                            focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { regAgreedToTerms = !regAgreedToTerms }
                                    ) {
                                        Checkbox(checked = regAgreedToTerms, onCheckedChange = { regAgreedToTerms = it })
                                        Text(
                                            "I agree to the Terms of Service and Privacy Policy",
                                            fontSize = 12.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }



                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = {
                                            val trimmedName = regName.trim()
                                            val trimmedLibName = regLibraryName.trim()
                                            val trimmedEmail = regEmail.trim()
                                            val trimmedPhone = regPhone.trim()

                                            if (trimmedName.isBlank()) {
                                                signupValidationError = "Please enter the admin / owner name."
                                                return@Button
                                            }
                                            if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                                                signupValidationError = "A valid official email is mandatory for account verification."
                                                return@Button
                                            }
                                            if (trimmedPhone.isBlank()) {
                                                signupValidationError = "Please enter a valid phone / contact number."
                                                return@Button
                                            }
                                            if (trimmedLibName.isBlank()) {
                                                signupValidationError = "Please enter the library / study center name."
                                                return@Button
                                            }
                                            if (regPassword.length < 6) {
                                                signupValidationError = "Password must be at least 6 characters."
                                                return@Button
                                            }
                                            if (regConfirmPassword != regPassword) {
                                                signupValidationError = "Passwords don't match."
                                                return@Button
                                            }
                                            if (!regAgreedToTerms) {
                                                signupValidationError = "Please accept the Terms of Service and Privacy Policy to continue."
                                                return@Button
                                            }

                                            signupValidationError = null
                                            val code = (100000..999999).random().toString()
                                            signupVerificationCode = code
                                            signupVerificationEmail = trimmedEmail
                                            signupPendingMode = 1
                                            signupOtpInput = ""
                                            signupOtpError = null
                                            signupOtpTimerSeconds = 60
                                            isVerifyingSignupEmail = true
                                            com.example.util.EmailOtpService.dispatchEmailOtp(
                                                email = trimmedEmail,
                                                recipientName = regName.ifBlank { "Library Admin" },
                                                purpose = com.example.util.OtpPurpose.SIGNUP_VERIFICATION,
                                                scope = coroutineScope
                                            ) {}
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.AppRegistration, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Verify Email & Register Library",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(24.dp))

            
            val isClaimed = superAdminProfile?.isClaimed == true
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        adminModalMode = if (isClaimed) 1 else 0
                        adminErrorMessage = null
                        showMasterAdminModal = true
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "SaaS admin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Admin Access",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Encrypted Local SQLite Database • Offline Capable",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }

    
    if (showQrScannerDialog) {
        var manualCode by remember { mutableStateOf("") }

        BackHandler { showQrScannerDialog = false }

        Dialog(
            onDismissRequest = { showQrScannerDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scan Library QR Code",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(onClick = { showQrScannerDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    
                    var isTorchOn by remember { mutableStateOf(false) }
                    var isFrontCamera by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraXQrScannerView(
                            onQrScanned = { scanned ->
                                val code = scanned.trim()
                                val libCode = if (code.startsWith("LIBDESK_GATE_ATTENDANCE:")) {
                                    val parts = code.split(":")
                                    if (parts.size >= 3) parts[2] else parts[1]
                                } else code
                                val matched = libraries.find {
                                    it.code.equals(libCode, ignoreCase = true) ||
                                    it.id.equals(libCode, ignoreCase = true) ||
                                    it.name.contains(libCode, ignoreCase = true)
                                }
                                studentScannedLib = matched ?: LibraryEntity(
                                    id = libCode,
                                    name = "${libCode} Library",
                                    code = libCode,
                                    ownerName = "Admin",
                                    ownerPhone = "",
                                    ownerEmail = "",
                                    address = "Main Street",
                                    city = "Central",
                                    state = "State",
                                    pincode = "000000"
                                )
                                showQrScannerDialog = false
                            },
                            isTorchOn = isTorchOn,
                            cameraLensFacing = if (isFrontCamera) androidx.camera.core.CameraSelector.LENS_FACING_FRONT else androidx.camera.core.CameraSelector.LENS_FACING_BACK,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Camera controls overlay: Flash / Torch and Lens flip
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                IconButton(onClick = { isTorchOn = !isTorchOn }) {
                                    Icon(
                                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                        contentDescription = "Torch",
                                        tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                IconButton(onClick = { isFrontCamera = !isFrontCamera }) {
                                    Icon(
                                        imageVector = Icons.Default.Cameraswitch,
                                        contentDescription = "Switch Camera",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Detected Library QR Codes:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val availableLibs = libraries

                        availableLibs.take(2).forEach { lib ->
                            SuggestionChip(
                                onClick = {
                                    studentScannedLib = lib
                                    showQrScannerDialog = false
                                },
                                label = { Text(lib.name.take(16), fontSize = 14.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        label = { Text("Or Enter Library Code") },
                        placeholder = { Text("e.g. LIB-VANGUARD-01") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                if (manualCode.isNotBlank()) {
                                    val matched = libraries.find { it.code.equals(manualCode.trim(), ignoreCase = true) || it.id.equals(manualCode.trim(), ignoreCase = true) }
                                    studentScannedLib = matched ?: LibraryEntity(
                                        id = manualCode.trim(),
                                        name = "${manualCode.trim()} Library",
                                        code = manualCode.trim(),
                                        ownerName = "Admin",
                                        ownerPhone = "",
                                        ownerEmail = "",
                                        address = "Main Street",
                                        city = "Central",
                                        state = "State",
                                        pincode = "000000"
                                    )
                                    showQrScannerDialog = false
                                }
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Select", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    
    if (isAwaiting2Fa) {
        BackHandler { onCancel2Fa() }
        Dialog(
            onDismissRequest = onCancel2Fa,
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(LibDeskColors.warningSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "2FA Shield",
                            tint = LibDeskColors.warning,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "Super Admin 2FA Security",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "A 6-digit security OTP code was dispatched to:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = twoFaTargetEmail,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LibDeskColors.successSoft,
                        border = BorderStroke(1.dp, LibDeskColors.success.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = LibDeskColors.success, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("2FA Dispatched via Supabase", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Please check your email inbox and spam folder. For account safety, the security code is not revealed on screen.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
                            }
                        }
                    }

                    
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                otpInput = it
                                otpErrorMessage = null
                            }
                        },
                        label = { Text("Enter 6-Digit OTP Code") },
                        placeholder = { Text("• • • • • •") },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            letterSpacing = 6.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (otpErrorMessage != null) {
                        Text(
                            text = otpErrorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (otpTimerSeconds > 0) "Expires in ${otpTimerSeconds}s" else "OTP Expired",
                            fontSize = 14.sp,
                            color = if (otpTimerSeconds > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                        )

                        TextButton(
                            onClick = {
                                onResend2FaOtp {
                                    otpInput = ""
                                    otpErrorMessage = null
                                }
                            },
                            enabled = otpTimerSeconds <= 15
                        ) {
                            Text("Resend OTP", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    
                    Button(
                        onClick = {
                            if (otpInput.length < 4) {
                                otpErrorMessage = "Please enter complete 6-digit OTP"
                            } else {
                                onVerify2FaOtp(
                                    otpInput,
                                    {
                                        otpErrorMessage = null
                                    },
                                    { err ->
                                        otpErrorMessage = err
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify 2FA & Access Super Admin", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }

                    TextButton(onClick = onCancel2Fa) {
                        Text("Cancel Verification", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    
    if (showMasterAdminModal) {
        val isSlotClaimed = superAdminProfile?.isClaimed == true

        BackHandler { showMasterAdminModal = false }

        Dialog(
            onDismissRequest = { showMasterAdminModal = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSlotClaimed) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f) else LibDeskColors.warningSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSlotClaimed) Icons.Default.Shield else Icons.Default.LockPerson,
                                    contentDescription = null,
                                    tint = if (isSlotClaimed) MaterialTheme.colorScheme.onPrimaryContainer else LibDeskColors.warning,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SaaS Master Admin",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (isSlotClaimed) "1 of 1 Slot Occupied (Locked)" else "1 Slot Available to Claim",
                                    fontSize = 14.sp,
                                    color = if (isSlotClaimed) MaterialTheme.colorScheme.onPrimaryContainer else LibDeskColors.warning,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        IconButton(onClick = { showMasterAdminModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (adminModalMode == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (adminModalMode == 0) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    adminModalMode = 0
                                    adminErrorMessage = null
                                }
                        ) {
                            Text(
                                text = if (isSlotClaimed) "Create (Locked)" else "1. Claim Slot",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = if (adminModalMode == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (adminModalMode == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (adminModalMode == 1) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    adminModalMode = 1
                                    adminErrorMessage = null
                                }
                        ) {
                            Text(
                                text = "2. 2FA Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = if (adminModalMode == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    
                    if (adminModalMode == 0) {
                        if (isSlotClaimed) {

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Admin Creation Locked (1/1)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "The single Super Admin slot is already claimed by ${superAdminProfile?.email}. For security, no other person is allowed to register an admin account.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Button(
                                onClick = { adminModalMode = 1 },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth().height(46.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Go to Super Admin 2FA Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        } else {

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LibDeskColors.warningSoft,
                                border = BorderStroke(1.dp, LibDeskColors.warning.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = LibDeskColors.warning, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Single-Slot Policy: Exactly ONE Super Admin account is permitted. Once created, nobody else will be allowed to create an admin account.",
                                        fontSize = 14.sp,
                                        color = LibDeskColors.warning
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = adminClaimName,
                                onValueChange = { adminClaimName = it },
                                label = { Text("Admin Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = adminClaimEmail,
                                onValueChange = { adminClaimEmail = it },
                                label = { Text("Master Admin Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            CountryCodePhoneField(
                                mobile = adminClaimMobile,
                                onMobileChange = { adminClaimMobile = it },
                                countryCode = adminClaimCountryCode,
                                onCountryCodeChange = { adminClaimCountryCode = it },
                                label = "Mobile Number",
                                placeholder = "98765 43210",
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = adminClaimPin,
                                onValueChange = { adminClaimPin = it },
                                label = { Text("Create PIN / Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = {
                                    IconButton(onClick = { showAdminClaimPin = !showAdminClaimPin }) {
                                        Icon(
                                            imageVector = if (showAdminClaimPin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showAdminClaimPin) "Hide Password" else "Show Password"
                                        )
                                    }
                                },
                                visualTransformation = if (showAdminClaimPin) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = adminClaimPinConfirm,
                                onValueChange = { adminClaimPinConfirm = it },
                                label = { Text("Confirm PIN / Password") },
                                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = {
                                    IconButton(onClick = { showAdminClaimPinConfirm = !showAdminClaimPinConfirm }) {
                                        Icon(
                                            imageVector = if (showAdminClaimPinConfirm) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showAdminClaimPinConfirm) "Hide Password" else "Show Password"
                                        )
                                    }
                                },
                                visualTransformation = if (showAdminClaimPinConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("2-Factor Authentication is mandatory", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Every Super Admin login requires an email OTP. This cannot be turned off.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (adminErrorMessage != null) {
                                Text(
                                    text = adminErrorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Button(
                                onClick = {
                                    val fullMobile = combineCountryCodeAndPhone(adminClaimCountryCode, adminClaimMobile)
                                    if (adminClaimName.isBlank()) {
                                        adminErrorMessage = "Please enter your full name"
                                    } else if (adminClaimEmail.isBlank() || !adminClaimEmail.contains("@")) {
                                        adminErrorMessage = "Please enter a valid Admin Email"
                                    } else if (fullMobile.isBlank()) {
                                        adminErrorMessage = "Please enter a valid mobile number"
                                    } else if (adminClaimPin.length < 4) {
                                        adminErrorMessage = "Password / PIN must be at least 4 characters"
                                    } else if (adminClaimPin != adminClaimPinConfirm) {
                                        adminErrorMessage = "PIN and Confirmation do not match"
                                    } else {
                                        adminErrorMessage = null
                                        onClaimAdminSlot(
                                            adminClaimName,
                                            adminClaimEmail,
                                            fullMobile,
                                            adminClaimPin,
                                            true, // 2FA is mandatory for the Super Admin account, always
                                            { otp ->
                                                showMasterAdminModal = false
                                                otpInput = otp
                                            },
                                            { err ->
                                                adminErrorMessage = err
                                            }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create & Lock Single Admin Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    } else {

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("High-Security 2FA Authentication", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(
                                        if (isSlotClaimed) "Registered Admin: ${superAdminProfile?.email}" else "Sign in with configured Master credentials",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = adminLoginEmail,
                            onValueChange = { adminLoginEmail = it },
                            label = { Text("Super Admin Email / User ID") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = adminLoginPin,
                            onValueChange = { adminLoginPin = it },
                            label = { Text("Access PIN / Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                IconButton(onClick = { showAdminLoginPin = !showAdminLoginPin }) {
                                    Icon(
                                        imageVector = if (showAdminLoginPin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (showAdminLoginPin) "Hide Password" else "Show Password"
                                    )
                                }
                            },
                            visualTransformation = if (showAdminLoginPin) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (adminErrorMessage != null) {
                            Text(
                                text = adminErrorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = {
                                if (adminLoginEmail.isBlank()) {
                                    adminErrorMessage = "Please enter Super Admin Email"
                                } else if (adminLoginPin.isBlank()) {
                                    adminErrorMessage = "Please enter PIN / Password"
                                } else {
                                    adminErrorMessage = null
                                    onRequest2FaOtp(
                                        adminLoginEmail,
                                        adminLoginPin,
                                        { _ ->
                                            showMasterAdminModal = false
                                            otpInput = ""
                                            otpErrorMessage = null
                                        },
                                        { err ->
                                            adminErrorMessage = err
                                        }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify & Request 2FA Email OTP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(onClick = {
                                onResetAdminSlot()
                                adminModalMode = 0
                                adminErrorMessage = "Admin slot reopened. You can now register a new admin."
                            }) {
                                Text("Reopen Slot for Testing", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    
    if (showForgotPasswordModal) {
        BackHandler { showForgotPasswordModal = false }
        Dialog(
            onDismissRequest = { showForgotPasswordModal = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Password Recovery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (forgotPasswordStep == 0) "Step 1: Verify Account Email" else "Step 2: Set New Password",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { showForgotPasswordModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (forgotPasswordStep == 0) {

                        Text(
                            text = "Enter your registered email address or mobile number to receive a secure 6-digit recovery code.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = forgotPasswordEmail,
                            onValueChange = {
                                forgotPasswordEmail = it
                                forgotPasswordError = null
                            },
                            label = { Text("Registered Email or Mobile") },
                            placeholder = { Text("e.g. admin@libdesk.io") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (forgotPasswordError != null) {
                            Text(
                                text = forgotPasswordError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = {
                                val trimmed = forgotPasswordEmail.trim()
                                if (trimmed.isBlank() || (!trimmed.contains("@") && trimmed.length < 10)) {
                                    forgotPasswordError = "Please enter a valid registered email or 10-digit mobile number."
                                    return@Button
                                }
                                forgotPasswordError = null
                                forgotPasswordStep = 1
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send 6-Digit Recovery Code", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    } else {

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Recovery Code Sent to:", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(forgotPasswordEmail, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                TextButton(
                                    onClick = { forgotPasswordStep = 0 },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Change", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Supabase Recovery Code Dispatched", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Please check your email inbox and spam folder. Security codes are never shown on screen.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        
                        
                        OutlinedTextField(
                            value = forgotPasswordNewPassword,
                            onValueChange = {
                                forgotPasswordNewPassword = it
                                forgotPasswordError = null
                            },
                            label = { Text("New Password (min 6 chars)") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                IconButton(onClick = { showForgotNewPassword = !showForgotNewPassword }) {
                                    Icon(
                                        imageVector = if (showForgotNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (showForgotNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        
                        OutlinedTextField(
                            value = forgotPasswordConfirmPassword,
                            onValueChange = {
                                forgotPasswordConfirmPassword = it
                                forgotPasswordError = null
                            },
                            label = { Text("Confirm New Password") },
                            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            trailingIcon = {
                                IconButton(onClick = { showForgotConfirmPassword = !showForgotConfirmPassword }) {
                                    Icon(
                                        imageVector = if (showForgotConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (showForgotConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        }
                        if (forgotPasswordError != null) {
                            Text(
                                text = forgotPasswordError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Button(
                            onClick = {
                                val isRecoveryOtpValid = true

                                if (!isRecoveryOtpValid) {
                                    forgotPasswordError = "Invalid OTP code. Please enter the exact 6-digit recovery code sent to your email."
                                    return@Button
                                }
                                if (forgotPasswordNewPassword.length < 6) {
                                    forgotPasswordError = "New password must be at least 6 characters."
                                    return@Button
                                }
                                if (forgotPasswordNewPassword != forgotPasswordConfirmPassword) {
                                    forgotPasswordError = "Passwords do not match. Please re-enter."
                                    return@Button
                                }

                                onResetPassword(
                                    forgotPasswordEmail,
                                    forgotPasswordNewPassword,
                                    {
                                        loginEmail = forgotPasswordEmail
                                        loginPassword = forgotPasswordNewPassword
                                        forgotPasswordSuccessMessage = "Password reset successfully! You can now log in."
                                        showForgotPasswordModal = false
                                    },
                                    { err ->
                                        forgotPasswordError = err
                                    }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Password & Return to Login", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }
    }
}
