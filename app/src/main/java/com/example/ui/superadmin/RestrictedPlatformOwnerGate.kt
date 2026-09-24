package com.example.ui.superadmin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LibDeskColors
import com.example.viewmodel.LibDeskViewModel

/**
 * Restricted Management Interface for the Platform Owner (SaaS Admin).
 * Accessible via dedicated bottom navigation entry or footer links.
 * Requires Platform Owner authentication (Password / Supabase 2FA) to unlock
 * global SaaS subscriptions, MRR metrics, library plans, and pricing tiers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictedPlatformOwnerGate(
    viewModel: LibDeskViewModel,
    onExit: () -> Unit,
    onOpenMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentRole by viewModel.currentRole.collectAsState()
    val superAdminProfile by viewModel.superAdminProfile.collectAsState()
    val isAwaiting2Fa by viewModel.isAwaiting2Fa.collectAsState()
    val otpTimerSeconds by viewModel.otpTimerSeconds.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val rememberPrefs = remember {
        context.getSharedPreferences("libdesk_remember_me_prefs", android.content.Context.MODE_PRIVATE)
    }
    val savedSuperAdminRememberMe = remember { rememberPrefs.getBoolean("remember_me_super_admin", false) }
    var rememberMe by remember { mutableStateOf(savedSuperAdminRememberMe) }
    var isUnlocked by rememberSaveable { mutableStateOf(currentRole == "SUPER_ADMIN") }
    var enteredEmail by remember {
        mutableStateOf(if (savedSuperAdminRememberMe) rememberPrefs.getString("saved_super_admin_email", "") ?: "" else "")
    }
    var enteredPin by remember {
        mutableStateOf(if (savedSuperAdminRememberMe) rememberPrefs.getString("saved_super_admin_password", "") ?: "" else "")
    }
    var showPin by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // First time claim state
    var claimName by remember { mutableStateOf("") }
    var claimEmail by remember { mutableStateOf("") }
    var claimMobile by remember { mutableStateOf("") }
    var claimPin by remember { mutableStateOf("") }

    // 2FA state
    var entered2FaOtp by remember { mutableStateOf("") }

    val isClaimed = superAdminProfile?.isClaimed == true
    var isClaimMode by rememberSaveable { mutableStateOf(!isClaimed) }

    LaunchedEffect(isClaimed) {
        if (isClaimed) {
            isClaimMode = false
        }
    }

    // If unlocked or already SUPER_ADMIN, show SuperAdminScreen directly
    if (isUnlocked || currentRole == "SUPER_ADMIN") {
        SuperAdminScreen(
            viewModel = viewModel,
            onOpenMenu = onOpenMenu,
            onLogout = {
                isUnlocked = false
                onExit()
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Platform Owner Console",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Restricted SaaS Admin Interface",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier.testTag("exit_saas_admin_gate")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Return to Dashboard"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Badge & Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "SaaS Admin",
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LibDeskColors.warningSoft.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, LibDeskColors.warning.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = LibDeskColors.warning
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RESTRICTED ACCESS • PLATFORM OWNER ONLY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LibDeskColors.warning
                    )
                }
            }

            Text(
                text = "SaaS Administrator Access",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "This restricted console allows the SaaS platform owner to manage global subscription tiers, verify incoming payments, enforce trial cutoffs, and monitor network health across all libraries.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Authentication Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Mode Switcher (Sign In vs Claim Account)
                    TabRow(
                        selectedTabIndex = if (isClaimMode) 1 else 0,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = !isClaimMode,
                            onClick = {
                                isClaimMode = false
                                errorMessage = null
                            },
                            text = { Text("Direct Sign In", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = isClaimMode,
                            onClick = {
                                isClaimMode = true
                                errorMessage = null
                            },
                            text = { Text("Claim Account", fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (!isClaimMode) {
                        // Sign In Flow
                        if (!isAwaiting2Fa) {
                            OutlinedTextField(
                                value = enteredEmail,
                                onValueChange = {
                                    enteredEmail = it
                                    errorMessage = null
                                },
                                label = { Text("Super Admin Email / ID") },
                                placeholder = { Text("Enter Super Admin Email / ID") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("saas_admin_email_input")
                            )

                            OutlinedTextField(
                                value = enteredPin,
                                onValueChange = {
                                    enteredPin = it
                                    errorMessage = null
                                },
                                label = { Text("Super Admin Password") },
                                placeholder = { Text("Enter account password") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showPin = !showPin }) {
                                        Icon(
                                            imageVector = if (showPin) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle visibility"
                                        )
                                    }
                                },
                                visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        // trigger unlock
                                    }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("saas_admin_pin_input")
                            )

                            // Remember Me Checkbox
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Remember me",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    if (enteredEmail.isBlank()) {
                                        errorMessage = "Please enter your Super Admin email"
                                        return@Button
                                    }
                                    if (enteredPin.isBlank()) {
                                        errorMessage = "Please enter your Super Admin password"
                                        return@Button
                                    }
                                    if (rememberMe) {
                                        rememberPrefs.edit()
                                            .putBoolean("remember_me_super_admin", true)
                                            .putString("saved_super_admin_email", enteredEmail.trim())
                                            .putString("saved_super_admin_password", enteredPin.trim())
                                            .apply()
                                    } else {
                                        rememberPrefs.edit()
                                            .putBoolean("remember_me_super_admin", false)
                                            .remove("saved_super_admin_email")
                                            .remove("saved_super_admin_password")
                                            .apply()
                                    }
                                    isLoading = true
                                    errorMessage = null

                                    viewModel.authenticateWithPassword(
                                        identifier = enteredEmail.trim(),
                                        passwordInput = enteredPin.trim(),
                                        role = "SUPER_ADMIN",
                                        onSuccess = {
                                            isLoading = false
                                            isUnlocked = true
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("unlock_saas_admin_button"),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unlock SaaS Admin Console", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // 2FA OTP Step
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Two-Factor Verification Required",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "A 6-digit verification code has been dispatched to $enteredEmail.",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = entered2FaOtp,
                                onValueChange = {
                                    if (it.length <= 6) entered2FaOtp = it
                                    errorMessage = null
                                },
                                label = { Text("6-Digit 2FA Code") },
                                placeholder = { Text("123456") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = {
                                    if (entered2FaOtp.length < 6) {
                                        errorMessage = "Please enter the full 6-digit OTP code"
                                        return@Button
                                    }
                                    isLoading = true
                                    viewModel.verifySuperAdminOtp(
                                        enteredOtp = entered2FaOtp,
                                        onSuccess = {
                                            isLoading = false
                                            isUnlocked = true
                                        },
                                        onError = { err ->
                                            isLoading = false
                                            errorMessage = err
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                enabled = !isLoading
                            ) {
                                Text("Verify 2FA & Access", fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.cancel2Fa()
                                    }
                                ) {
                                    Text("Cancel", fontSize = 12.sp)
                                }

                                TextButton(
                                    onClick = {
                                        viewModel.resendSuperAdminOtp { }
                                    },
                                    enabled = otpTimerSeconds == 0
                                ) {
                                    Text(
                                        text = if (otpTimerSeconds > 0) "Resend in ${otpTimerSeconds}s" else "Resend Code",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // Claim Platform Owner Slot: Setup Form
                        Text(
                            text = "Set Up Platform Owner Account",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Register as the primary SaaS owner to secure master administrative rights for this platform.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = claimName,
                            onValueChange = { claimName = it },
                            label = { Text("Owner Full Name") },
                            placeholder = { Text("e.g. Platform Administrator") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = claimEmail,
                            onValueChange = { claimEmail = it },
                            label = { Text("Owner Official Email") },
                            placeholder = { Text("e.g. owner@example.com") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = claimMobile,
                            onValueChange = { claimMobile = it },
                            label = { Text("Mobile Number") },
                            placeholder = { Text("+91 98765 43210") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = claimPin,
                            onValueChange = { claimPin = it },
                            label = { Text("Password (at least 6 characters)") },
                            placeholder = { Text("Enter secure password") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (claimEmail.isBlank() || claimPin.length < 6) {
                                    errorMessage = "Please enter official email and a password with at least 6 characters"
                                    return@Button
                                }
                                isLoading = true
                                viewModel.claimSuperAdminSlot(
                                    name = claimName.ifBlank { "Platform Owner" },
                                    email = claimEmail.trim(),
                                    mobile = claimMobile.trim(),
                                    accessCode = claimPin.trim(),
                                    is2Fa = false,
                                    onSuccess = {
                                        isLoading = false
                                        isUnlocked = true
                                    },
                                    onError = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isLoading
                        ) {
                            Text("Claim Platform Owner Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Return to Library Button
            OutlinedButton(
                onClick = onExit,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Return to Library Operations")
            }
        }
    }
}
