package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.*
import com.example.data.remote.SupabaseClient
import com.example.ui.auth.AuthScreen
import com.example.ui.components.*
import com.example.ui.manager.*
import com.example.ui.onboarding.OnboardingWizardScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.student.StudentPortalScreen
import com.example.ui.superadmin.SuperAdminScreen
import com.example.ui.theme.LibDeskTheme
import com.example.viewmodel.LibDeskViewModel
import com.example.viewmodel.BackupViewModel
import com.example.ui.backup.BackupSettingsScreen
import com.example.ui.backup.BackupState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: LibDeskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        
        handleAuthDeepLink(intent)
        handleNotificationAction(intent)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            LibDeskTheme(darkTheme = isDarkMode) {
                GlobalErrorBoundary(
                    onRetry = {
                        viewModel.clearUserMessage()
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LibDeskApp(
                            viewModel = viewModel
                        )

                        GlobalSnackbarHost(
                            isOnline = viewModel.isOnline.collectAsStateWithLifecycle().value,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleAuthDeepLink(intent)
        handleNotificationAction(intent)
    }

    private fun handleAuthDeepLink(intent: android.content.Intent?) {
        intent?.data?.let { uri ->
            viewModel.handleDeepLink(uri)
        }
    }

    private fun handleNotificationAction(intent: android.content.Intent?) {
        if (intent == null) return
        val action = intent.action
        val extra = intent.getStringExtra(com.example.notification.StudentNotificationHelper.EXTRA_NOTIFICATION_ACTION)
        if (action == com.example.notification.StudentNotificationHelper.ACTION_OPEN_QR_SCANNER || extra == "SCAN_ATTENDANCE") {
            viewModel.triggerOpenQrScanner()
        } else if (action == com.example.notification.StudentNotificationHelper.ACTION_OPEN_STUDENT_PORTAL || extra == "MEMBERSHIP_RENEWAL") {
            viewModel.triggerOpenStudentPortal()
        }
    }
}

@Composable
fun LibDeskApp(
    viewModel: LibDeskViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)


    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val currentUserEmail by viewModel.currentUserEmail.collectAsStateWithLifecycle()
    val currentUserName by viewModel.currentUserName.collectAsStateWithLifecycle()

    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val library by viewModel.currentLibrary.collectAsStateWithLifecycle()
    val allLibraries by viewModel.allLibraries.collectAsStateWithLifecycle()
    val halls by viewModel.halls.collectAsStateWithLifecycle()
    val shifts by viewModel.shifts.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val seats by viewModel.seats.collectAsStateWithLifecycle()
    val students by viewModel.students.collectAsStateWithLifecycle()
    val activeStudent by viewModel.activeStudent.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendance.collectAsStateWithLifecycle()
    val allAttendance by viewModel.attendanceLogs.collectAsStateWithLifecycle()
    val books by viewModel.books.collectAsStateWithLifecycle()
    val bookIssues by viewModel.bookIssues.collectAsStateWithLifecycle()
    val digitalMaterials by viewModel.digitalMaterials.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val notices by viewModel.notices.collectAsStateWithLifecycle()
    val feedbackList by viewModel.feedbackList.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val cachedUserBooking by viewModel.cachedUserBooking.collectAsStateWithLifecycle()
    val checkInConfirmation by viewModel.checkInConfirmation.collectAsStateWithLifecycle()

    var currentManagerTab by remember { mutableStateOf(0) }
    var superAdminTab by remember { mutableIntStateOf(0) }
    var managerDashboardSection by remember { mutableStateOf(0) }
    var isOnboardingMode by remember { mutableStateOf(false) }
    var showSettingsView by remember { mutableStateOf(false) }
    var settingsInitialTab by remember { mutableStateOf(0) }
    var isSpeedDialFabExpanded by remember { mutableStateOf(false) }

    
    var viewingIdCardForStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var viewingReceiptForPayment by remember { mutableStateOf<PaymentEntity?>(null) }
    var showQrScannerModal by remember { mutableStateOf(false) }
    var showLibraryQrModal by remember { mutableStateOf(false) }
    var showRegisterStudentModal by remember { mutableStateOf(false) }
    var showAddPaymentModal by remember { mutableStateOf(false) }
    var showPostNoticeModal by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showUserProfileModal by remember { mutableStateOf(false) }
    var showProfileScreenModal by remember { mutableStateOf(false) }
    var showLogoutConfirmationDialog by remember { mutableStateOf(false) }
    var showInfraEditorModal by remember { mutableStateOf(false) }
    var showBackupScreen by remember { mutableStateOf(false) }
    var editingStudentForProfile by remember { mutableStateOf<StudentEntity?>(null) }
    var showSaaSOffersModal by remember { mutableStateOf(false) }

    val currentSubscription by viewModel.currentSubscription.collectAsStateWithLifecycle()
    val saasPlans by viewModel.saasPlans.collectAsStateWithLifecycle()
    val trialDaysRemaining by viewModel.trialDaysRemaining.collectAsStateWithLifecycle()
    val isTrialActive by viewModel.isTrialActive.collectAsStateWithLifecycle()
    val isTrialExpired by viewModel.isTrialExpired.collectAsStateWithLifecycle()

    val activeAlertsCount = remember(students, notices) {
        val renewalAlerts = students.count { st ->
            st.status.equals("EXPIRED", ignoreCase = true) ||
            st.dueAmount > 0
        }
        val noticeAlerts = notices.count { it.isActive }
        renewalAlerts + noticeAlerts
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val openQrScannerRequested by viewModel.openQrScannerRequest.collectAsStateWithLifecycle()
    LaunchedEffect(openQrScannerRequested) {
        if (openQrScannerRequested) {
            showQrScannerModal = true
            viewModel.consumeOpenQrScanner()
        }
    }

    val openStudentPortalRequested by viewModel.openStudentPortalRequest.collectAsStateWithLifecycle()
    LaunchedEffect(openStudentPortalRequested) {
        if (openStudentPortalRequested) {
            viewModel.consumeOpenStudentPortal()
        }
    }

    val isSupabaseSyncing by viewModel.isSupabaseSyncing.collectAsStateWithLifecycle()
    val supabaseStatusMessage by viewModel.supabaseStatusMessage.collectAsStateWithLifecycle()

    val isAwaiting2Fa by viewModel.isAwaiting2Fa.collectAsStateWithLifecycle()
    val twoFaTargetEmail by viewModel.twoFaTargetEmail.collectAsStateWithLifecycle()
    val activeOtpCode by viewModel.activeOtpCode.collectAsStateWithLifecycle()
    val otpTimerSeconds by viewModel.otpTimerSeconds.collectAsStateWithLifecycle()
    val superAdminProfile by viewModel.superAdminProfile.collectAsStateWithLifecycle()

    
    if (!isAuthenticated) {
        AuthScreen(
            libraries = allLibraries,
            shifts = shifts,
            plans = plans,
            superAdminProfile = superAdminProfile,
            isAwaiting2Fa = isAwaiting2Fa,
            twoFaTargetEmail = twoFaTargetEmail,
            activeOtpCode = activeOtpCode,
            otpTimerSeconds = otpTimerSeconds,
            onRequest2FaOtp = { email, code, onOtpSent, onError ->
                viewModel.requestSuperAdmin2FaOtp(email, code, onOtpSent, onError)
            },
            onVerify2FaOtp = { otp, onSuccess, onError ->
                viewModel.verifySuperAdminOtp(otp, onSuccess, onError)
            },
            onResend2FaOtp = { onOtpSent ->
                viewModel.resendSuperAdminOtp(onOtpSent)
            },
            onCancel2Fa = { viewModel.cancel2Fa() },
            onClaimAdminSlot = { name, email, mobile, pin, is2Fa, onSuccess, onError ->
                viewModel.claimSuperAdminSlot(name, email, mobile, pin, is2Fa, onSuccess, onError)
            },
            onResetAdminSlot = {
                viewModel.resetSuperAdminSlot()
            },
            onResetPassword = { email, newPassword, onSuccess, onError ->
                viewModel.resetUserPassword(email, newPassword, onSuccess, onError)
            },
            onLogin = { email, role, name ->
                viewModel.login(email, role, name)
            },
            onAuthenticate = { identifier, password, role, onSuccess, onError ->
                viewModel.authenticateWithPassword(identifier, password, role, onSuccess, onError)
            },
            onRegister = { name, email, libName, phone ->
                viewModel.registerAndLogin(name, email, libName, phone)
            },
            onStudentQrSignup = { libId, fullName, mobile, email, exam, shift, plan ->
                viewModel.registerStudentViaQr(libId, fullName, mobile, email, exam, shift, plan)
            }
        )
        return
    }

    
    if (isOnboardingMode) {
        BackHandler {
            isOnboardingMode = false
        }
        OnboardingWizardScreen(
            onCompleteSetup = { name, owner, phone, email, addr, city, state, pin, upi, payee, hall, seatCount ->
                viewModel.createNewLibrary(name, owner, phone, email, addr, city, state, pin, upi, payee, hall, seatCount)
                isOnboardingMode = false
            },
            onLoadDemo = {
                viewModel.seedDemoData()
                isOnboardingMode = false
            },
            onCancel = { isOnboardingMode = false }
        )
    } else {

        val hasAnyOpenModal = viewingIdCardForStudent != null ||
                viewingReceiptForPayment != null ||
                showQrScannerModal ||
                showLibraryQrModal ||
                showSaaSOffersModal ||
                showRegisterStudentModal ||
                showAddPaymentModal ||
                showPostNoticeModal ||
                showNotificationsModal ||
                showUserProfileModal ||
                showProfileScreenModal ||
                showLogoutConfirmationDialog ||
                showInfraEditorModal ||
                editingStudentForProfile != null ||
                isSpeedDialFabExpanded

        val shouldInterceptBack = drawerState.isOpen ||
                hasAnyOpenModal ||
                showSettingsView ||
                (currentRole == "SUPER_ADMIN" && superAdminTab != 0) ||
                (currentRole == "MANAGER" && (managerDashboardSection != 0 || currentManagerTab != 0))

        BackHandler(enabled = shouldInterceptBack) {
            when {
                drawerState.isOpen -> {
                    coroutineScope.launch { drawerState.close() }
                }
                isSpeedDialFabExpanded -> {
                    isSpeedDialFabExpanded = false
                }
                showInfraEditorModal -> {
                    showInfraEditorModal = false
                }
                showSaaSOffersModal -> {
                    showSaaSOffersModal = false
                }
                showLogoutConfirmationDialog -> {
                    showLogoutConfirmationDialog = false
                }
                showProfileScreenModal -> {
                    showProfileScreenModal = false
                }
                showUserProfileModal -> {
                    showUserProfileModal = false
                }
                editingStudentForProfile != null -> {
                    editingStudentForProfile = null
                }
                viewingIdCardForStudent != null -> {
                    viewingIdCardForStudent = null
                }
                viewingReceiptForPayment != null -> {
                    viewingReceiptForPayment = null
                }
                showQrScannerModal -> {
                    showQrScannerModal = false
                }
                showLibraryQrModal -> {
                    showLibraryQrModal = false
                }

                showRegisterStudentModal -> {
                    showRegisterStudentModal = false
                }
                showAddPaymentModal -> {
                    showAddPaymentModal = false
                }
                showPostNoticeModal -> {
                    showPostNoticeModal = false
                }
                showNotificationsModal -> {
                    showNotificationsModal = false
                }
                showSettingsView -> {
                    showSettingsView = false
                }
                currentRole == "SUPER_ADMIN" && superAdminTab != 0 -> {
                    superAdminTab = 0
                }
                currentRole == "MANAGER" && managerDashboardSection != 0 -> {
                    managerDashboardSection = 0
                }
                currentRole == "MANAGER" && currentManagerTab != 0 -> {
                    currentManagerTab = 0
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                LibDeskDrawerContent(
                    library = library,
                    currentRole = currentRole,
                    activeStudent = activeStudent,
                    currentUserEmail = currentUserEmail,
                    currentUserName = currentUserName,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onSwitchRole = { viewModel.switchRole() },
                    onNavigateTab = { tabIndex, sectionIndex ->
                        if (currentRole == "SUPER_ADMIN") {
                            superAdminTab = tabIndex
                        } else {
                            if (currentRole != "MANAGER") {
                                viewModel.switchRole()
                            }
                            showSettingsView = false
                            currentManagerTab = tabIndex
                            managerDashboardSection = sectionIndex
                        }
                    },
                    onOpenSettings = {
                        if (currentRole != "MANAGER") {
                            viewModel.switchRole()
                        }
                        settingsInitialTab = 1
                        showSettingsView = true
                    },
                    onOpenNoticesAndHelp = {
                        if (currentRole != "MANAGER") {
                            viewModel.switchRole()
                        }
                        settingsInitialTab = 0
                        showSettingsView = true
                    },
                    onOpenAttendanceHistory = {
                        if (currentRole != "MANAGER") {
                            viewModel.switchRole()
                        }
                        showSettingsView = false
                        currentManagerTab = 0
                        managerDashboardSection = 3
                    },
onOpenSyncBackup = { showBackupScreen = true },
                    onOpenQrScanner = { showQrScannerModal = true },
                    onShowLibraryQr = { showLibraryQrModal = true },
                    onOpenSuperAdmin = {
                        viewModel.login("superadmin@libdesk.io", "SUPER_ADMIN", "Master Super Admin")
                    },
                    onOpenMySubscription = {
                        showSaaSOffersModal = true
                    },
                    isTrialActive = isTrialActive,
                    trialDaysRemaining = trialDaysRemaining,
                    isTrialExpired = isTrialExpired,
                    subscriptionPlanName = currentSubscription?.planName,
                    onOpenProfile = {
                        if (currentRole == LibDeskRoles.STUDENT) {
                            showProfileScreenModal = true
                        } else {
                            showUserProfileModal = true
                        }
                    },
                    onSeedDemo = { viewModel.seedDemoData() },
                    onLogout = { showLogoutConfirmationDialog = true },
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        ) {
            RoleGate(
                currentRole = currentRole,
                allowedRoles = setOf(LibDeskRoles.SUPER_ADMIN),
                fallback = {
                    Scaffold(
                topBar = {
                    LibDeskHeader(
                        library = library,
                        currentRole = currentRole,
                        userName = currentUserName,
                        notificationCount = activeAlertsCount,
                        isOnline = isOnline,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { viewModel.toggleDarkMode() },
                        onOpenNotifications = { showNotificationsModal = true },
                        onSwitchRole = { viewModel.switchRole() },
onOpenSyncBackup = { showBackupScreen = true },
                        onOpenMenu = {
                            coroutineScope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }
                    )
                },
                bottomBar = {
                    if (currentRole == "MANAGER" && !showSettingsView) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp
                        ) {
                            val navColors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            NavigationBarItem(
                                selected = currentManagerTab == 0,
                                onClick = { currentManagerTab = 0 },
                                icon = { Icon(if (currentManagerTab == 0) Icons.Filled.Dashboard else Icons.Outlined.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard", style = MaterialTheme.typography.labelSmall) },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentManagerTab == 1,
                                onClick = { currentManagerTab = 1 },
                                icon = { Icon(if (currentManagerTab == 1) Icons.Filled.Chair else Icons.Outlined.Chair, contentDescription = "Seats") },
                                label = { Text("Seats", style = MaterialTheme.typography.labelSmall) },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentManagerTab == 2,
                                onClick = { currentManagerTab = 2 },
                                icon = { Icon(if (currentManagerTab == 2) Icons.Filled.People else Icons.Outlined.People, contentDescription = "Students") },
                                label = { Text("Students", style = MaterialTheme.typography.labelSmall) },
                                colors = navColors
                            )
                            NavigationBarItem(
                                selected = currentManagerTab == 3,
                                onClick = { currentManagerTab = 3 },
                                icon = { Icon(if (currentManagerTab == 3) Icons.Filled.Payments else Icons.Outlined.Payments, contentDescription = "Finance") },
                                label = { Text("Finance", style = MaterialTheme.typography.labelSmall) },
                                colors = navColors
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (currentRole == "MANAGER" && !showSettingsView && currentManagerTab == 0) {
                        LibDeskSpeedDialFab(
                            actions = listOf(
                                SpeedDialAction(
                                    id = "new_admission",
                                    label = "New Admission",
                                    icon = Icons.Default.PersonAdd,
                                    containerColor = com.example.ui.theme.ButtonColor,
                                    onClick = { showRegisterStudentModal = true }
                                ),
                                SpeedDialAction(
                                    id = "record_fee",
                                    label = "Record Fee",
                                    icon = Icons.Default.Payments,
                                    containerColor = com.example.ui.theme.ButtonColor,
                                    onClick = { showAddPaymentModal = true }
                                ),
                                SpeedDialAction(
                                    id = "post_notice",
                                    label = "Post Notice",
                                    icon = Icons.Default.Campaign,
                                    containerColor = com.example.ui.theme.DangerRed,
                                    onClick = { showPostNoticeModal = true }
                                )
                            ),
                            isExpanded = isSpeedDialFabExpanded,
                            onToggle = { isSpeedDialFabExpanded = !isSpeedDialFabExpanded },
                            onDismiss = { isSpeedDialFabExpanded = false }
                        )
                    } else if (currentRole == "STUDENT") {
                        ExtendedFloatingActionButton(
                            onClick = { showQrScannerModal = true },
                            containerColor = com.example.ui.theme.ButtonColor,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Quick Check-in Scan",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Quick Check-in",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (normalizeUserRole(currentRole) == LibDeskRoles.MANAGER) {
                        RoleGate(
                            currentRole = currentRole,
                            requiredRole = LibDeskRoles.MANAGER,
                            onSignOut = { showLogoutConfirmationDialog = true },
                            onSwitchToAllowedView = {
                                viewModel.login(currentUserEmail, LibDeskRoles.STUDENT, currentUserName)
                            }
                        ) {
                        if (showSettingsView) {

                            var settingsSubTab by remember(settingsInitialTab) { mutableStateOf(settingsInitialTab) }
                            Column(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { showSettingsView = false }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to Dashboard")
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (settingsSubTab == 0) "Notices & Student Helpdesk" else "Library Configuration & Rules",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                TabRow(selectedTabIndex = settingsSubTab) {
                                    Tab(
                                        selected = settingsSubTab == 0,
                                        onClick = { settingsSubTab = 0 },
                                        text = { Text("Notices & Help (${notices.size})") }
                                    )
                                    Tab(
                                        selected = settingsSubTab == 1,
                                        onClick = { settingsSubTab = 1 },
                                        text = { Text("Library Settings") }
                                    )
                                }

                                if (settingsSubTab == 0) {
                                    ManagerNoticesAndFeedbackScreen(
                                        notices = notices,
                                        feedbackList = feedbackList,
                                        onPostNotice = { title, content, cat, prio ->
                                            viewModel.postNotice(title, content, cat, prio)
                                        },
                                        onReplyFeedback = { comp, reply, status ->
                                            viewModel.replyToComplaint(comp, reply, status)
                                        }
                                    )
                                } else {
                                    ManagerSettingsScreen(
                                        library = library,
                                        allLibraries = allLibraries,
                                        halls = halls,
                                        shifts = shifts,
                                        plans = plans,
                                        onSelectLibrary = { libId ->
                                            viewModel.selectLibrary(libId)
                                        },
                                        onCreateLibrary = {
                                            isOnboardingMode = true
                                        },
                                        onOpenSyncBackup = {

                                        },
                                        onEditProfile = {
                                            showUserProfileModal = true
                                        },
                                        onEditInfrastructure = {
                                            showInfraEditorModal = true
                                        }
                                    )
                                }
                            }
                        } else {
                            when (currentManagerTab) {
                                0 -> ManagerDashboardScreen(
                                    library = library,
                                    seats = seats,
                                    students = students,
                                    todayAttendance = todayAttendance,
                                    books = books,
                                    bookIssues = bookIssues,
                                    digitalMaterials = digitalMaterials,
                                    payments = payments,
                                    expenses = expenses,
                                    notices = notices,
                                    userName = currentUserName,
                                    onNavigateTab = { currentManagerTab = it },
                                    onOpenQrScanner = { showQrScannerModal = true },
                                    onOpenRegisterStudent = { showRegisterStudentModal = true },
                                    onOpenAddPayment = { showAddPaymentModal = true },
                                    onOpenPostNotice = { showPostNoticeModal = true },
                                    onOpenProfile = { showUserProfileModal = true },
                                    onAddBook = { title, author, isbn, cat, sub, rack, shelf, copies ->
                                        viewModel.addNewBook(title, author, isbn, cat, sub, rack, shelf, copies)
                                    },
                                    onIssueBook = { book, student ->
                                        viewModel.issueBook(book, student)
                                    },
                                    onReturnBook = { issue ->
                                        viewModel.returnBook(issue)
                                    },
                                    onAddMaterial = { title, desc, cat, sub, exam, fType, fSize, fUrl, policy ->
                                        viewModel.addDigitalMaterial(title, desc, cat, sub, exam, fType, fSize, fUrl, policy)
                                    },
                                    onAutoAddFreeMaterials = {
                                        viewModel.loadCuratedFreeStudyPdfs()
                                    },
                                    onToggleBookmark = { doc ->
                                        viewModel.toggleBookmark(doc)
                                    },
                                    viewModel = viewModel,
                                    initialSection = managerDashboardSection
                                )
                                1 -> SeatMatrixView(
                                    seats = seats,
                                    halls = halls,
                                    students = students,
                                    shifts = shifts,
                                    plans = plans,
                                    onAssignSeat = { seat, student, shift, plan, date ->
                                        viewModel.assignSeat(seat, student, shift, plan, date)
                                    },
                                    onTransferSeat = { oldSeat, newSeat, student ->
                                        viewModel.transferSeat(oldSeat, newSeat, student)
                                    },
                                    onReleaseSeat = { seat ->
                                        viewModel.releaseSeat(seat)
                                    },
                                    onUpdateStatus = { seat, status ->
                                        viewModel.updateSeatStatus(seat, status)
                                    },
                                    onToggleReservation = { seat ->
                                        viewModel.toggleSeatReservation(seat)
                                    },
                                    onBatchGenerate = { prefix, count, hallId, hallName, secId, secName, floor, type, fee, startNum ->
                                        viewModel.batchGenerateSeats(prefix, count, hallId, hallName, secId, secName, floor, type, fee, startNum)
                                    }
                                )
                                2 -> ManagerStudentsScreen(
                                    students = students,
                                    shifts = shifts,
                                    plans = plans,
                                    seats = seats,
                                    library = library,
                                    onSelectStudent = { student ->
                                        viewingIdCardForStudent = student
                                    },
                                    onViewIdCard = { student ->
                                        viewingIdCardForStudent = student
                                    },
                                    onRegisterStudent = { name, mob, email, gender, exam, course, addr, pName, pMob, joiningDate, shift, plan, fee, paid ->
                                        viewModel.registerStudent(name, mob, email, gender, exam, course, addr, pName, pMob, joiningDate, shift, plan, fee, paid)
                                    },
                                    onRecordFeePayment = { student, amt, mode, purpose, ref, discount, remarks, period, onReceipt ->
                                        viewModel.recordFeePayment(student, amt, mode, purpose, ref, discount, remarks, period, onReceipt)
                                    },
                                    onAssignSeat = { seat, student, shift, plan, date ->
                                        viewModel.assignSeat(seat, student, shift, plan, date)
                                    },
                                    onViewReceipt = { receipt ->
                                        viewingReceiptForPayment = receipt
                                    },
                                    onEditStudentProfile = { student ->
                                        editingStudentForProfile = student
                                    },
                                    onArchiveStudent = { student, reason ->
                                        viewModel.archiveStudent(student, reason)
                                    },
                                    onReactivateStudent = { student ->
                                        viewModel.reactivateStudent(student)
                                    }
                                )
                                3 -> ManagerFinanceScreen(
                                    library = library,
                                    payments = payments,
                                    expenses = expenses,
                                    students = students,
                                    onRecordPayment = { student, amt, mode, purpose, ref, discount, remarks, period, onReceipt ->
                                        viewModel.recordFeePayment(student, amt, mode, purpose, ref, discount, remarks, period, onReceipt)
                                    },
                                    onAddExpense = { cat, amt, desc, mode ->
                                        viewModel.addExpense(cat, amt, desc, mode)
                                    },
                                    onViewReceipt = { receipt ->
                                        viewingReceiptForPayment = receipt
                                    },
                                    onUpdatePayment = { pmt, amt, mode, purpose, ref, remarks, date ->
                                        viewModel.editPaymentRecord(pmt, amt, mode, purpose, ref, remarks, date)
                                    },
                                    onDeletePayment = { pmt ->
                                        viewModel.deletePaymentRecord(pmt)
                                    },
                                    onUpdateExpense = { exp, cat, amt, date, desc, mode ->
                                        viewModel.editExpenseRecord(exp, cat, amt, date, desc, mode)
                                    },
                                    onDeleteExpense = { exp ->
                                        viewModel.deleteExpenseRecord(exp)
                                    }
                                )
                            }
                        }
                        }
                    } else {

                        RoleGate(
                            currentRole = currentRole,
                            requiredRole = LibDeskRoles.STUDENT,
                            onSignOut = { showLogoutConfirmationDialog = true },
                            onSwitchToAllowedView = {
                                viewModel.login(currentUserEmail, LibDeskRoles.MANAGER, currentUserName)
                            }
                        ) {
                            StudentPortalScreen(
                                library = library,
                                students = students,
                                activeStudent = activeStudent,
                                attendanceList = allAttendance,
                                digitalMaterials = digitalMaterials,
                                books = books,
                                bookIssues = bookIssues,
                                notices = notices,
                                complaints = feedbackList,
                                payments = payments,
                                seats = seats,
                                shifts = shifts,
                                plans = plans,
                                halls = halls,
                                onSelectStudent = { stId ->
                                    viewModel.selectActiveStudent(stId)
                                },
                                onViewIdCard = { st ->
                                    viewingIdCardForStudent = st
                                },
                                onViewReceipt = { receipt ->
                                    viewingReceiptForPayment = receipt
                                },
                                onToggleBookmark = { doc ->
                                    viewModel.toggleBookmark(doc)
                                },
                                onSubmitComplaint = { subj, msg, type ->
                                    viewModel.submitStudentComplaint(subj, msg, type)
                                },
                                onOpenQrScanner = {
                                    showQrScannerModal = true
                                },
                                onOpenProfile = {
                                    showProfileScreenModal = true
                                },
                                onRequestLogout = {
                                    showLogoutConfirmationDialog = true
                                }
                            )
                        }
                    }
                }
            }
                }
            ) {
                SuperAdminScreen(
                    viewModel = viewModel,
                    onNavigateToLibrary = { libId ->
                        viewModel.selectLibrary(libId)
                        viewModel.login("admin@libdesk.io", "MANAGER", "Rajesh Verma")
                    },
                    onLogout = {
                        showLogoutConfirmationDialog = true
                    },
                    selectedTabFromDrawer = superAdminTab,
                    onTabChange = { superAdminTab = it },
                    onOpenMenu = {
                        coroutineScope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    }
                )
            }
        }
    }

    
    if (viewingIdCardForStudent != null) {
        DigitalIdCardView(
            library = library,
            student = viewingIdCardForStudent!!,
            onClose = { viewingIdCardForStudent = null }
        )
    }

    if (viewingReceiptForPayment != null) {
        val studentPhone = students.find { it.id == viewingReceiptForPayment!!.studentId }?.mobile
        DigitalReceiptView(
            library = library,
            payment = viewingReceiptForPayment!!,
            studentPhone = studentPhone,
            onClose = { viewingReceiptForPayment = null }
        )
    }

    if (showQrScannerModal) {
        val isStudent = currentRole == "STUDENT"
        val activeLib = library
        val libLat = if (activeLib != null && activeLib.latitude != 0.0) activeLib.latitude else 28.6139
        val libLng = if (activeLib != null && activeLib.longitude != 0.0) activeLib.longitude else 77.2090
        val libName = activeLib?.name ?: "Vanguard Study Hall"
        QrScannerModal(
            title = if (isStudent) "Student Attendance Punch (In / Out)" else "QR Seat & Member Check-in",
            subtitle = if (isStudent) "Scan Library Gate Poster or Assigned Seat QR" else "Scan student pass or assigned seat QR sticker",
            isStudentMode = isStudent,
            libraryLatitude = libLat,
            libraryLongitude = libLng,
            libraryName = libName,
            onScanCode = { code ->
                viewModel.scanQrAttendance(code)
            },
            onScanWithLocation = { code, locNote ->
                viewModel.scanQrAttendance(code, locationNote = locNote)
            },
            onClose = { showQrScannerModal = false }
        )
    }

    if (showLibraryQrModal) {
        LibraryEnrollmentQrModal(
            library = library,
            onClose = { showLibraryQrModal = false }
        )
    }



    if (showRegisterStudentModal) {
        RegisterStudentDialog(
            shifts = shifts,
            plans = plans,
            onClose = { showRegisterStudentModal = false },
            onConfirm = { name, mob, email, gender, exam, course, addr, pName, pMob, joiningDate, shift, plan, fee, paid ->
                viewModel.registerStudent(name, mob, email, gender, exam, course, addr, pName, pMob, joiningDate, shift, plan, fee, paid)
                showRegisterStudentModal = false
            }
        )
    }

    if (showAddPaymentModal) {
        RecordPaymentDialog(
            students = students,
            onClose = { showAddPaymentModal = false },
            onConfirm = { st, amt, mode, purpose, ref, discount, remarks, period ->
                viewModel.recordFeePayment(st, amt, mode, purpose, ref, discount, remarks, period) { receipt ->
                    viewingReceiptForPayment = receipt
                }
                showAddPaymentModal = false
            }
        )
    }

    if (showPostNoticeModal) {
        PostNoticeDialog(
            onClose = { showPostNoticeModal = false },
            onConfirm = { title, content, cat, prio ->
                viewModel.postNotice(title, content, cat, prio)
                showPostNoticeModal = false
            }
        )
    }

    if (showNotificationsModal) {
        NotificationsAlertsModal(
            students = students,
            notices = notices,
            currentRole = currentRole,
            onClose = { showNotificationsModal = false },
            onRecordPayment = { student ->
                showNotificationsModal = false
                showAddPaymentModal = true
            },
            onOpenPostNotice = {
                showNotificationsModal = false
                showPostNoticeModal = true
            }
        )
    }

    if (showLogoutConfirmationDialog) {
        LogoutConfirmationDialog(
            show = showLogoutConfirmationDialog,
            userName = currentUserName.ifBlank { "User" },
            onConfirmLogout = {
                showLogoutConfirmationDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutConfirmationDialog = false }
        )
    }

    if (showProfileScreenModal) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showProfileScreenModal = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            ProfileScreen(
                student = activeStudent ?: students.firstOrNull(),
                library = library,
                seats = seats,
                shifts = shifts,
                plans = plans,
                halls = halls,
                bookIssues = bookIssues,
                cachedUserBooking = cachedUserBooking,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onOpenEditProfile = {
                    showUserProfileModal = true
                },
                onViewFullIdCard = {
                    val st = activeStudent ?: students.firstOrNull()
                    if (st != null) {
                        viewingIdCardForStudent = st
                    }
                },
                onRenewMembership = {
                    showProfileScreenModal = false
                    showAddPaymentModal = true
                },
                onOpenQrScanner = {
                    showProfileScreenModal = false
                    showQrScannerModal = true
                },
                onPerformCheckIn = {
                    viewModel.performDirectSeatCheckIn()
                },
                onTriggerCheckInAnimation = {
                    viewModel.triggerTestCheckInAnimation()
                },
                onRequestLogout = {
                    showProfileScreenModal = false
                    showLogoutConfirmationDialog = true
                },
                onChangeStudent = {
                    // Handled inside student picker
                },
                onClose = { showProfileScreenModal = false }
            )
        }
    }

    if (showUserProfileModal) {
        UserProfileModal(
            currentRole = currentRole,
            library = library,
            student = activeStudent,
            userName = currentUserName,
            userEmail = currentUserEmail,
            seats = seats,
            shifts = shifts,
            plans = plans,
            onUpdateLibrary = { updatedLib ->
                viewModel.updateLibraryProfile(updatedLib)
            },
            onUpdateStudent = { updatedSt ->
                viewModel.updateStudentProfile(updatedSt)
            },
            onClose = { showUserProfileModal = false }
        )
    }

    if (showInfraEditorModal) {
        com.example.ui.manager.InfrastructureEditorModal(
            halls = halls,
            shifts = shifts,
            plans = plans,
            onAddHall = { name, floor, count -> viewModel.addHall(name, floor, count) },
            onAddShift = { name, start, end, fee -> viewModel.addShift(name, start, end, fee) },
            onAddPlan = { name, duration, fee -> viewModel.addMembershipPlan(name, duration, fee) },
            onClose = { showInfraEditorModal = false }
        )
    }
    if (showBackupScreen) {
        val backupViewModel: BackupViewModel = viewModel()
        val backupState by backupViewModel.backupState.collectAsStateWithLifecycle()
        val lastBackupTime by backupViewModel.lastBackupTime.collectAsStateWithLifecycle()
        
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showBackupScreen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
                BackupSettingsScreen(
                    onBack = { showBackupScreen = false },
                    onExportLocal = { uri -> 
                        backupViewModel.exportBackup(uri, "libdesk_secure") 
                    },
                    onImportLocal = { uri -> 
                        backupViewModel.restoreFromUri(uri, "libdesk_secure") { success ->
                            if (success) {
                                android.widget.Toast.makeText(context, "Restore completed. LibDesk data replaced.", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                android.widget.Toast.makeText(context, "Restore failed. Invalid backup or corrupted data.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onBackupNow = {
                        backupViewModel.createLocalBackup("libdesk_secure")
                    },
                    lastBackupTime = lastBackupTime,
                    backupState = backupState,
                    history = backupViewModel.history.collectAsStateWithLifecycle().value,
                    onDeleteHistory = { id -> backupViewModel.deleteHistory(id) },
                    // These were previously never wired up, so every Google Drive
                    // button in this screen silently called a no-op default lambda.
                    driveBackups = backupViewModel.driveBackups.collectAsStateWithLifecycle().value,
                    isDriveLoading = backupViewModel.isDriveLoading.collectAsStateWithLifecycle().value,
                    driveStatusMessage = backupViewModel.driveStatusMessage.collectAsStateWithLifecycle().value,
                    savedDriveToken = backupViewModel.getSavedDriveToken(),
                    onSaveDriveToken = { token -> backupViewModel.saveDriveToken(token) },
                    onUploadToGoogleDrive = { token, password, onResult ->
                        backupViewModel.uploadToGoogleDriveApi(token, password, onResult)
                    },
                    onRestoreFromGoogleDrive = { fileId, token, password, onResult ->
                        backupViewModel.restoreFromGoogleDriveApi(fileId, token, password, onResult)
                    },
                    onRefreshDriveFiles = { token -> backupViewModel.refreshDriveFiles(token) }
                )
            }
        }
    }


    if (editingStudentForProfile != null) {
        UserProfileModal(
            currentRole = "STUDENT",
            library = library,
            student = editingStudentForProfile,
            userName = editingStudentForProfile!!.fullName,
            userEmail = editingStudentForProfile!!.email,
            seats = seats,
            shifts = shifts,
            plans = plans,
            onUpdateStudent = { updatedSt ->
                viewModel.updateStudentProfile(updatedSt)
                editingStudentForProfile = null
            },
            onClose = { editingStudentForProfile = null }
        )
    }

    // Lottie-Style Seat Check-In Confirmation Dialog
    checkInConfirmation?.let { details ->
        SeatCheckInConfirmationDialog(
            details = details,
            onDismiss = { viewModel.dismissCheckInConfirmation() }
        )
    }

    // SaaS Plans, 15-Day Free Trial & Offers Modal (Accessible via Side Menu "My Subscriptions")
    if (showSaaSOffersModal) {
        SaaSPlansAndOffersModal(
            show = true,
            currentSubscription = currentSubscription,
            plans = saasPlans,
            trialDaysRemaining = trialDaysRemaining,
            onSelectPlan = { plan ->
                viewModel.requestSubscriptionUpgrade(plan)
                showSaaSOffersModal = false
            },
            onDismiss = { showSaaSOffersModal = false }
        )
    }
}
