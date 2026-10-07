package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.*
import com.example.data.remote.SessionManager
import com.example.data.remote.SupabaseAuthService
import com.example.data.remote.UserRole
import com.example.data.remote.UserSession
import com.example.data.repository.LibDeskRepository
import com.example.ui.components.SeatCheckInDetails
import com.example.ui.components.SnackbarController
import com.example.ui.components.NotificationType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
/**
 * Result of a live (non-cached) subscription check against Supabase — see
 * LibDeskViewModel.verifyLiveSubscriptionStatus(). Deliberately fail-closed:
 * only [Active] should ever unlock paid library features.
 */
sealed class LiveSubscriptionCheck {
    object Active : LiveSubscriptionCheck()
    object Inactive : LiveSubscriptionCheck()
    object Expired : LiveSubscriptionCheck()
    object Suspended : LiveSubscriptionCheck()
    object PendingVerification : LiveSubscriptionCheck()
    object NoSubscription : LiveSubscriptionCheck()
    object NetworkError : LiveSubscriptionCheck()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LibDeskViewModel(application: Application) : AndroidViewModel(application) {
    val repository = LibDeskRepository(application)
    val networkMonitor = com.example.util.NetworkConnectivityMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    val supabaseSyncManager = com.example.data.remote.SupabaseSyncManager(repository)

    private val _isSupabaseSyncing = MutableStateFlow(false)
    val isSupabaseSyncing: StateFlow<Boolean> = _isSupabaseSyncing.asStateFlow()

    private val _openQrScannerRequest = MutableStateFlow(false)
    val openQrScannerRequest: StateFlow<Boolean> = _openQrScannerRequest.asStateFlow()

    // Global Authentication Guard State for real-time subscription verification
    val authGuardStatus: StateFlow<LiveSubscriptionCheck?> = com.example.data.remote.AuthGuardService.guardState

    fun triggerOpenQrScanner() {
        _openQrScannerRequest.value = true
    }

    fun consumeOpenQrScanner() {
        _openQrScannerRequest.value = false
    }

    private val _openStudentPortalRequest = MutableStateFlow(false)
    val openStudentPortalRequest: StateFlow<Boolean> = _openStudentPortalRequest.asStateFlow()

    fun triggerOpenStudentPortal() {
        _currentRole.value = "STUDENT"
        _openStudentPortalRequest.value = true
    }

    fun consumeOpenStudentPortal() {
        _openStudentPortalRequest.value = false
    }

    private val _supabaseStatusMessage = MutableStateFlow<String?>("Connected: https://rfhqbdwctqulvwwjcsgt.supabase.co")
    val supabaseStatusMessage: StateFlow<String?> = _supabaseStatusMessage.asStateFlow()

    private val authPrefs = application.getSharedPreferences("libdesk_auth_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(authPrefs.getBoolean(KEY_DARK_MODE, false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(enabled: Boolean? = null) {
        val next = enabled ?: !_isDarkMode.value
        _isDarkMode.value = next
        authPrefs.edit().putBoolean(KEY_DARK_MODE, next).apply()
    }

    private val _isAmoledMode = MutableStateFlow(authPrefs.getBoolean(KEY_AMOLED_MODE, false))
    val isAmoledMode: StateFlow<Boolean> = _isAmoledMode.asStateFlow()

    fun toggleAmoledMode(enabled: Boolean? = null) {
        val next = enabled ?: !_isAmoledMode.value
        _isAmoledMode.value = next
        authPrefs.edit().putBoolean(KEY_AMOLED_MODE, next).apply()
        if (next && !_isDarkMode.value) {
            toggleDarkMode(true)
        }
    }

    private val _isBiometricEnabled = MutableStateFlow(authPrefs.getBoolean(KEY_BIOMETRIC, false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    fun toggleBiometric(enabled: Boolean? = null) {
        val next = enabled ?: !_isBiometricEnabled.value
        _isBiometricEnabled.value = next
        authPrefs.edit().putBoolean(KEY_BIOMETRIC, next).apply()
    }

    // Pomodoro Timer State
    private val _pomodoroSecondsLeft = MutableStateFlow(25 * 60)
    val pomodoroSecondsLeft: StateFlow<Int> = _pomodoroSecondsLeft.asStateFlow()

    private val _isPomodoroRunning = MutableStateFlow(false)
    val isPomodoroRunning: StateFlow<Boolean> = _isPomodoroRunning.asStateFlow()

    private val _isPomodoroBreak = MutableStateFlow(false)
    val isPomodoroBreak: StateFlow<Boolean> = _isPomodoroBreak.asStateFlow()

    private val _pomodoroSessionMinutes = MutableStateFlow(25)
    val pomodoroSessionMinutes: StateFlow<Int> = _pomodoroSessionMinutes.asStateFlow()

    private var pomodoroJob: kotlinx.coroutines.Job? = null

    private val _isAuthenticated = MutableStateFlow(authPrefs.getBoolean(KEY_IS_AUTHENTICATED, false))
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUserEmail = MutableStateFlow(authPrefs.getString(KEY_USER_EMAIL, "") ?: "")
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    private val _currentUserName = MutableStateFlow(authPrefs.getString(KEY_USER_NAME, "") ?: "")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _currentLibraryId = MutableStateFlow(authPrefs.getString(KEY_LIBRARY_ID, "") ?: "")
    val currentLibraryId: StateFlow<String> = _currentLibraryId.asStateFlow()

    private val _currentRole = MutableStateFlow(authPrefs.getString(KEY_ROLE, "OWNER")?.let { if (it == "MANAGER" || it == "ADMIN") "OWNER" else it } ?: "OWNER") 
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    private val _activeStudentId = MutableStateFlow(authPrefs.getString(KEY_ACTIVE_STUDENT_ID, "") ?: "")
    val activeStudentId: StateFlow<String> = _activeStudentId.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val allLibraries: StateFlow<List<LibraryEntity>> = repository.getAllLibraries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLibrary: StateFlow<LibraryEntity?> = combine(
        _currentLibraryId,
        repository.getAllLibraries(),
        _currentUserEmail
    ) { id, allLibs, email ->
        val found = when {
            id.isNotBlank() -> allLibs.find { it.id == id }
                ?: allLibs.find { it.ownerEmail.equals(email, ignoreCase = true) || it.email.equals(email, ignoreCase = true) }
                ?: if (_currentRole.value == "OWNER") allLibs.firstOrNull() else null
            email.isNotBlank() -> allLibs.find { it.ownerEmail.equals(email, ignoreCase = true) || it.email.equals(email, ignoreCase = true) }
                ?: if (_currentRole.value == "OWNER") allLibs.firstOrNull() else null
            else -> if (_currentRole.value == "OWNER") allLibs.firstOrNull() else null
        }
        if (found != null && _currentLibraryId.value != found.id) {
            _currentLibraryId.value = found.id
        }
        found
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val halls: StateFlow<List<HallEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getHalls(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cabins: StateFlow<List<CabinEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getCabins(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sections: StateFlow<List<SectionEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getSections(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shifts: StateFlow<List<ShiftEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getShifts(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plans: StateFlow<List<MembershipPlanEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getPlans(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seats: StateFlow<List<SeatEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getSeats(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val students: StateFlow<List<StudentEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getStudents(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStudent: StateFlow<StudentEntity?> = _activeStudentId
        .flatMapLatest { stId -> repository.getStudentById(stId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentStudent: StateFlow<StudentEntity?> = activeStudent

    val availableSeats: StateFlow<List<SeatEntity>> = seats.map { list ->
        list.filter { it.status == "AVAILABLE" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _checkInConfirmation = MutableStateFlow<SeatCheckInDetails?>(null)
    val checkInConfirmation: StateFlow<SeatCheckInDetails?> = _checkInConfirmation.asStateFlow()

    val todayAttendance: StateFlow<List<AttendanceEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getTodayAttendance(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceLogs: StateFlow<List<AttendanceEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getAttendance(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val books: StateFlow<List<PhysicalBookEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getBooks(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookIssues: StateFlow<List<BookIssueEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getBookIssues(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val digitalMaterials: StateFlow<List<DigitalMaterialEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getDigitalMaterials(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<PaymentEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getPayments(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getExpenses(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fines: StateFlow<List<FineEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getFines(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notices: StateFlow<List<NoticeEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getAllNotices(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBroadcastNotices: StateFlow<List<NoticeEntity>> = repository.getAllBroadcastNotices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feedbackList: StateFlow<List<FeedbackComplaintEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getFeedback(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getAuditLogs(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadStudentChatCount: StateFlow<Int> = repository.chatMessages.map { list ->
        list.count { it.senderRole == "STUDENT" && !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // NOTE: saasPlans/SaaSSubscriptionPlanEntity below was a completely
    // separate, disconnected plan catalog — the Super Admin dashboard could
    // create/edit plans here, but PlansAndOffersScreen (what library owners
    // actually browse and pay from) reads from the SubscriptionPlans table
    // instead, so none of those edits were ever visible to anyone. The Super
    // Admin's Plans tab now manages allSubscriptionPlansAdmin (the real,
    // user-facing table) instead. saasPlans/getAllSaasPlans() is kept only so
    // existing Room data/migrations aren't broken, but nothing should read
    // from it going forward.
    val saasPlans: StateFlow<List<SaaSSubscriptionPlanEntity>> = repository.getAllSaasPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptionPlans: StateFlow<List<SubscriptionPlans>> = repository.getActiveSubscriptionPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubscriptionPlansAdmin: StateFlow<List<SubscriptionPlans>> = repository.getAllSubscriptionPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveSubscriptionPlanAdmin(plan: SubscriptionPlans) {
        viewModelScope.launch { repository.saveSubscriptionPlan(plan) }
    }

    fun deleteSubscriptionPlanAdmin(plan: SubscriptionPlans) {
        viewModelScope.launch { repository.deleteSubscriptionPlan(plan) }
    }

    val userSubscription: StateFlow<UserSubscription?> = _currentLibraryId
        .flatMapLatest { id -> repository.getUserSubscription(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSubscriptions: StateFlow<List<LibrarySubscriptionEntity>> = repository.getAllLibrarySubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentSubscription: StateFlow<LibrarySubscriptionEntity?> = _currentLibraryId
        .flatMapLatest { id -> repository.getSubscriptionForLibrary(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trialDaysRemaining: StateFlow<Int> = currentSubscription.map { sub ->
        if (sub == null) return@map 15
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiry = sdf.parse(sub.expiryDate)
            val today = Calendar.getInstance().time
            if (expiry != null) {
                val diff = (expiry.time - today.time) / (1000 * 60 * 60 * 24)
                diff.toInt().coerceAtLeast(0)
            } else 15
        } catch (e: Exception) { 15 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    val isTrialActive: StateFlow<Boolean> = currentSubscription.map { sub ->
        sub == null || sub.planId == "PLAN-TRIAL" || sub.status == "TRIAL"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isTrialExpired: StateFlow<Boolean> = combine(isTrialActive, trialDaysRemaining, currentSubscription) { isTrial, days, sub ->
        (isTrial && days <= 0) || sub?.status == "EXPIRED" || sub?.status == "SUSPENDED"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val superAdminProfile: StateFlow<SuperAdminUserEntity?> = repository.getSuperAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lockers: StateFlow<List<LockerEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getLockersByLibrary(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visitorPasses: StateFlow<List<VisitorPassEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getVisitorPassesByLibrary(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val seatShiftRequests: StateFlow<List<SeatShiftRequestEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getSeatShiftRequestsByLibrary(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentStudentSeatShiftRequests: StateFlow<List<SeatShiftRequestEntity>> = _activeStudentId
        .flatMapLatest { id -> repository.getSeatShiftRequestsByStudent(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val floorElements: StateFlow<List<FloorElementEntity>> = _currentLibraryId
        .flatMapLatest { id -> repository.getFloorElementsByLibrary(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentStudentStreak: StateFlow<StudyStreakEntity?> = _activeStudentId
        .flatMapLatest { id -> repository.getStudyStreakByStudent(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    
    private val _isAwaiting2Fa = MutableStateFlow(false)
    val isAwaiting2Fa: StateFlow<Boolean> = _isAwaiting2Fa.asStateFlow()

    private val _twoFaTargetEmail = MutableStateFlow("")
    val twoFaTargetEmail: StateFlow<String> = _twoFaTargetEmail.asStateFlow()

    private val _otpTimerSeconds = MutableStateFlow(60)
    val otpTimerSeconds: StateFlow<Int> = _otpTimerSeconds.asStateFlow()

    // Temporary holding slot for platform owner claim until Email OTP verification completes
    private var pendingSuperAdminClaim: SuperAdminUserEntity? = null

    init {

        SessionManager.init(application)
        val restored = SessionManager.restoreSessionFromPrefs()
        if (restored != null) {
            _isAuthenticated.value = true
            _currentUserEmail.value = restored.email
            _currentUserName.value = restored.name
            _currentRole.value = when (restored.role) {
                UserRole.SUPER_ADMIN -> "SUPER_ADMIN"
                UserRole.OWNER -> "OWNER"
                UserRole.STUDENT -> "STUDENT"
            }
            if (restored.libraryId.isNotBlank()) {
                _currentLibraryId.value = restored.libraryId
            }
            if (restored.studentId.isNotBlank()) {
                _activeStudentId.value = restored.studentId
            }
        }

        // Standalone Local Mode Initialization
        _supabaseStatusMessage.value = if (com.example.data.remote.SupabaseClient.isConfigured()) {
            "● Cloud Sync Active"
        } else {
            "● Local Offline Mode (Standalone Active)"
        }

        viewModelScope.launch {
            repository.ensureDefaultSubscriptionPlansSeeded()
        }
    }

    fun submitManualSubscriptionPayment(
        plan: SubscriptionPlans,
        utrNumber: String,
        billingCycle: String = "MONTHLY",
        receiptImageUri: Uri? = null,
        ownerName: String = "",
        ownerMobile: String = "",
        ownerEmail: String = "",
        notes: String = ""
    ) {
        val libId = _currentLibraryId.value
        if (libId.isBlank()) {
            _userMessage.value = "No library selected. Please sign in again before submitting a payment."
            return
        }
        val lib = currentLibrary.value
        val libName = lib?.name ?: "Unknown Library"
        val resolvedOwner = ownerName.ifBlank { _currentUserName.value.ifBlank { lib?.ownerName ?: "" } }
        val resolvedEmail = ownerEmail.ifBlank { _currentUserEmail.value.ifBlank { lib?.ownerEmail ?: "" } }
        val resolvedMobile = ownerMobile.ifBlank { lib?.ownerPhone ?: "" }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val startDate = sdf.format(cal.time)
        val monthsToAdd = when (billingCycle.uppercase()) {
            "ANNUAL" -> 12
            "QUARTERLY" -> 3
            else -> plan.durationMonths.coerceAtLeast(1)
        }
        cal.add(Calendar.MONTH, monthsToAdd)
        val expiryDate = sdf.format(cal.time)

        val payableAmount = when (billingCycle.uppercase()) {
            "ANNUAL" -> (plan.price * 12) * (1.0 - (plan.discountPercentage.coerceAtLeast(20.0) / 100.0))
            "QUARTERLY" -> (plan.price * 3) * 0.90
            else -> plan.price
        }

        val userSub = UserSubscription(
            id = "SUB-${UUID.randomUUID().toString().take(8).uppercase()}",
            libraryId = libId,
            userId = resolvedEmail,
            ownerName = resolvedOwner,
            ownerMobile = resolvedMobile,
            ownerEmail = resolvedEmail,
            libraryName = libName,
            planId = plan.id,
            planName = "${plan.name} ($billingCycle)",
            amountPaid = payableAmount,
            billingCycle = billingCycle,
            status = "PENDING_VERIFICATION",
            startDate = startDate,
            expiryDate = expiryDate,
            paymentMethod = "UPI_MANUAL",
            paymentReferenceId = utrNumber.trim(),
            receiptImageUrl = receiptImageUri?.toString() ?: "",
            isVerifiedByAdmin = false,
            notes = notes.ifBlank { "Manual UPI payment submitted with UTR: ${utrNumber.trim()}" },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.saveUserSubscription(userSub)
            repository.logAudit(
                libId,
                resolvedOwner,
                "SUBMIT_SUBSCRIPTION_PAYMENT",
                "UserSubscription",
                userSub.id,
                "Submitted manual UPI payment of ₹${String.format(Locale.US, "%.2f", payableAmount)} for ${plan.name}. UTR: ${utrNumber.trim()}"
            )
            _userMessage.value = "Payment details submitted! Opening WhatsApp to share receipt..."
        }
    }

    /**
     * Refreshes the Supabase access token if it's expired, BEFORE any Supabase
     * call is made. Without this, a Supabase-backed session (Manager/Owner
     * signed in via email+password or magic link) would keep showing as
     * "authenticated" with full role permissions locally while the backend
     * silently rejected every request once the ~1hr token expired — the core
     * login/permission conflict. Only applies to real Supabase sessions; local
     * Super Admin PIN login / offline demo Manager login never had a Supabase
     * token to begin with, so this is a no-op for them.
     */
    private fun ensureSupabaseSessionFreshness() {
        if (SessionManager.sessionState.value == null) return
        if (!SessionManager.isAccessTokenExpired()) return

        viewModelScope.launch {
            val result = SupabaseAuthService.refreshAccessToken(getApplication())
            result.onFailure {
                
                logout()
                _userMessage.value = "Your session has expired. Please sign in again."
            }
        }
    }

    fun handleDeepLink(uri: Uri) {
        SessionManager.handleAuthCallback(getApplication(), uri) { success, session, error ->
            if (success && session != null) {
                _isAuthenticated.value = true
                _currentUserEmail.value = session.email
                _currentUserName.value = session.name
                _currentRole.value = when (session.role) {
                    UserRole.SUPER_ADMIN -> "SUPER_ADMIN"
                    UserRole.OWNER -> "OWNER"
                    UserRole.STUDENT -> "STUDENT"
                }
                if (session.libraryId.isNotBlank()) {
                    _currentLibraryId.value = session.libraryId
                }
                if (session.studentId.isNotBlank()) {
                    _activeStudentId.value = session.studentId
                }
                _userMessage.value = "Authenticated via magic link as ${session.name} (${session.role.title})"
            } else if (error != null) {
                _userMessage.value = "Sign-in link failed: $error"
            }
        }
    }

    
    fun claimSuperAdminSlot(
        name: String,
        email: String,
        mobile: String,
        accessCode: String,
        is2Fa: Boolean = true, // Default to true: Email OTP is mandatory for Platform Owner account
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        forceReclaim: Boolean = false
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            val cleanPassword = accessCode.trim()

            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                onError("Please provide a valid Super Admin Email address.")
                return@launch
            }
            if (cleanPassword.length < 6) {
                onError("Super Admin password must be at least 6 characters.")
                return@launch
            }

            val current = repository.getSuperAdmin().firstOrNull()
            val isSeedAccount = current == null ||
                    current.email.isBlank() ||
                    current.email.contains("CHANGE-THIS-EMAIL", ignoreCase = true) ||
                    !current.isClaimed

            if (current != null && current.isClaimed && !isSeedAccount &&
                !current.email.equals(cleanEmail, ignoreCase = true) && !forceReclaim
            ) {
                onError("Admin Slot Already Claimed: An account is already registered (${com.example.util.EmailOtpService.maskEmail(current.email)}). Please sign in with your registered credentials.")
                return@launch
            }

            if (!com.example.data.remote.SupabaseClient.isConfigured()) {
                val localAdmin = SuperAdminUserEntity(
                    id = "SUPER-ADMIN-MASTER",
                    email = cleanEmail,
                    name = name.trim().ifBlank { "SaaS Master Administrator" },
                    mobile = mobile.trim(),
                    role = "SUPER_ADMIN",
                    accessCode = cleanPassword,
                    is2FaEnabled = false,
                    isClaimed = true,
                    createdAt = System.currentTimeMillis()
                )
                repository.saveSuperAdmin(localAdmin)
                _isAuthenticated.value = true
                _currentRole.value = "SUPER_ADMIN"
                _currentUserEmail.value = localAdmin.email
                _currentUserName.value = localAdmin.name
                _userMessage.value = "🎉 Super Admin Account successfully claimed and activated!"
                persistAuthSession(
                    authenticated = true,
                    role = "SUPER_ADMIN",
                    email = localAdmin.email,
                    name = localAdmin.name,
                    libraryId = ""
                )
                onSuccess("Super Admin successfully activated!")
                return@launch
            }

            // Register or verify in Supabase Auth
            val signUpResult = SupabaseAuthService.signUp(
                context = getApplication(),
                email = cleanEmail,
                password = cleanPassword,
                name = name.ifBlank { "SaaS Master Administrator" },
                role = UserRole.SUPER_ADMIN,
                libraryId = "",
                phone = mobile.trim()
            )

            var authUserId = "SUPER-ADMIN-MASTER"
            var isUserAlreadyRegistered = false

            if (signUpResult.isFailure) {
                val errMsg = signUpResult.exceptionOrNull()?.message ?: ""
                isUserAlreadyRegistered = errMsg.contains("already registered", ignoreCase = true) ||
                        errMsg.contains("already exists", ignoreCase = true) ||
                        errMsg.contains("user_already_exists", ignoreCase = true)

                if (isUserAlreadyRegistered) {
                    val checkPass = SupabaseAuthService.signInWithPassword(
                        context = getApplication(),
                        email = cleanEmail,
                        password = cleanPassword,
                        tenantCode = null,
                        expectedRole = null,
                        persistSession = false
                    )
                    if (checkPass.isFailure) {
                        onError("This email is already registered in Supabase. Please enter the correct password to claim the Super Admin role.")
                        return@launch
                    }
                    authUserId = checkPass.getOrNull()?.userId?.takeIf { it.isNotBlank() } ?: "SUPER-ADMIN-MASTER"
                } else if (errMsg.isNotBlank()) {
                    onError("Supabase Registration Error: $errMsg")
                    return@launch
                }
            } else {
                authUserId = signUpResult.getOrNull()?.userId?.takeIf { it.isNotBlank() } ?: "SUPER-ADMIN-MASTER"
            }

            val pendingAdmin = SuperAdminUserEntity(
                id = authUserId,
                email = cleanEmail,
                name = name.trim().ifBlank { "SaaS Master Administrator" },
                mobile = mobile.trim(),
                role = "SUPER_ADMIN",
                accessCode = cleanPassword,
                is2FaEnabled = is2Fa,
                isClaimed = true,
                createdAt = System.currentTimeMillis()
            )

            if (is2Fa) {
                pendingSuperAdminClaim = pendingAdmin
                _twoFaTargetEmail.value = cleanEmail

                if (isUserAlreadyRegistered) {
                    val otpResult = SupabaseAuthService.signInWithOtp(cleanEmail, shouldCreateUser = false)
                    if (otpResult.isFailure) {
                        val otpErr = otpResult.exceptionOrNull()?.message ?: "Failed to dispatch verification OTP."
                        onError(otpErr)
                        return@launch
                    }
                    _userMessage.value = "Security 2FA OTP dispatched to ${com.example.util.EmailOtpService.maskEmail(cleanEmail)} via Supabase."
                } else {
                    _userMessage.value = "Verification code dispatched to ${com.example.util.EmailOtpService.maskEmail(cleanEmail)} by Supabase. Please enter the 6-digit code to complete registration."
                }

                _isAwaiting2Fa.value = true
                _otpTimerSeconds.value = 60
                onSuccess("")

                viewModelScope.launch {
                    for (sec in 60 downTo 0) {
                        _otpTimerSeconds.value = sec
                        kotlinx.coroutines.delay(1000L)
                        if (!_isAwaiting2Fa.value) break
                    }
                }
            } else {
                repository.saveSuperAdmin(pendingAdmin.copy(is2FaEnabled = false))
                _isAuthenticated.value = true
                _currentRole.value = "SUPER_ADMIN"
                _currentUserEmail.value = pendingAdmin.email
                _currentUserName.value = pendingAdmin.name
                _userMessage.value = "🎉 Super Admin Account successfully registered and activated!"
                persistAuthSession(
                    authenticated = true,
                    email = pendingAdmin.email,
                    name = pendingAdmin.name,
                    role = "SUPER_ADMIN",
                    libraryId = "",
                    studentId = ""
                )
                onSuccess("")
            }
        }
    }

    fun resetSuperAdminSlot(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            // SECURITY GATE: Only an already authenticated SUPER_ADMIN can ever reset or reopen the master slot!
            if (_currentRole.value != "SUPER_ADMIN" && SessionManager.currentRole.value != com.example.data.remote.UserRole.SUPER_ADMIN) {
                _userMessage.value = "Security Error: Only an authenticated Platform Owner can reset the master slot."
                return@launch
            }
            val resetAdmin = SuperAdminUserEntity(
                id = "SUPER-ADMIN-MASTER",
                email = "",
                name = "",
                mobile = "",
                role = "SUPER_ADMIN",
                accessCode = "",
                is2FaEnabled = true,
                isClaimed = false
            )
            repository.saveSuperAdmin(resetAdmin)
            _userMessage.value = "Super Admin slot reopened! New admin can now register."
            onDone()
        }
    }

    fun requestSuperAdmin2FaOtp(
        email: String,
        accessCode: String = "",
        onOtpDispatched: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedPassword = accessCode.trim()

        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            onError("Please enter Super Admin Email / User ID")
            return
        }

        viewModelScope.launch {
            val adminProfile = repository.getSuperAdmin().firstOrNull()
            val isUnclaimed = adminProfile == null || !adminProfile.isClaimed ||
                    adminProfile.email.isBlank() ||
                    adminProfile.email.contains("CHANGE-THIS-EMAIL", ignoreCase = true)

            if (isUnclaimed) {
                onError("SaaS Admin account has not been claimed or created yet. Please tap 'Claim Slot' above to create the account first.")
                return@launch
            }

            val isMatchingAdmin = trimmedEmail.equals(adminProfile?.email, ignoreCase = true) ||
                    (adminProfile?.mobile?.isNotBlank() == true && trimmedEmail == adminProfile.mobile)

            if (!isMatchingAdmin) {
                onError("The entered credentials do not match the registered SaaS Super Admin.")
                return@launch
            }

            val targetEmail = adminProfile?.email ?: trimmedEmail

            // If password was supplied, verify with Supabase Auth
            if (trimmedPassword.isNotBlank()) {
                val authResult = SupabaseAuthService.signInWithPassword(
                    context = getApplication(),
                    email = targetEmail,
                    password = trimmedPassword,
                    expectedRole = "SUPER_ADMIN"
                )
                if (authResult.isFailure) {
                    onError(authResult.exceptionOrNull()?.message ?: "Invalid Super Admin credentials.")
                    return@launch
                }
            }

            val otpResult = SupabaseAuthService.signInWithOtp(targetEmail, shouldCreateUser = false)
            if (otpResult.isSuccess) {
                _twoFaTargetEmail.value = targetEmail
                _isAwaiting2Fa.value = true
                _otpTimerSeconds.value = 60
                _userMessage.value = "Security 2FA OTP dispatched to ${com.example.util.EmailOtpService.maskEmail(targetEmail)} via Supabase."
                onOtpDispatched("")

                viewModelScope.launch {
                    for (sec in 60 downTo 0) {
                        _otpTimerSeconds.value = sec
                        kotlinx.coroutines.delay(1000L)
                        if (!_isAwaiting2Fa.value) break
                    }
                }
            } else {
                val err = otpResult.exceptionOrNull()?.message ?: "Failed to dispatch 2FA OTP."
                onError(err)
            }
        }
    }

    fun sendSuperAdminMagicLink(
        email: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            onError("Please enter a valid Super Admin email address.")
            return
        }

        viewModelScope.launch {
            val adminProfile = repository.getSuperAdmin().firstOrNull()
            if (adminProfile != null && adminProfile.isClaimed && !cleanEmail.equals(adminProfile.email, ignoreCase = true)) {
                onError("The entered email does not match the registered Super Admin account.")
                return@launch
            }

            val result = SupabaseAuthService.signInWithOtp(cleanEmail, shouldCreateUser = false)
            if (result.isSuccess) {
                _twoFaTargetEmail.value = cleanEmail
                _isAwaiting2Fa.value = true
                _otpTimerSeconds.value = 60
                _userMessage.value = "Magic Link / OTP dispatched to ${com.example.util.EmailOtpService.maskEmail(cleanEmail)}. Enter the code to authenticate."

                viewModelScope.launch {
                    for (sec in 60 downTo 0) {
                        _otpTimerSeconds.value = sec
                        kotlinx.coroutines.delay(1000L)
                        if (!_isAwaiting2Fa.value) break
                    }
                }
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to send magic link.")
            }
        }
    }

    fun verifySuperAdminOtp(
        enteredOtp: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val targetEmail = _twoFaTargetEmail.value ?: ""
        if (targetEmail.isBlank()) {
            onError("2FA session expired. Please log in again.")
            return
        }

        val cleanOtp = enteredOtp.trim()
        if (cleanOtp.length != 6) {
            onError("Verification code must be 6 digits.")
            return
        }

        viewModelScope.launch {
            val adminProfile = repository.getSuperAdmin().firstOrNull()
            val isClaimFlow = pendingSuperAdminClaim != null

            val purpose = if (isClaimFlow) {
                com.example.util.OtpPurpose.SUPER_ADMIN_CLAIM
            } else {
                com.example.util.OtpPurpose.SUPER_ADMIN_2FA
            }

            com.example.util.EmailOtpService.verifyOtp(
                context = getApplication(),
                email = targetEmail,
                enteredOtp = cleanOtp,
                purpose = purpose,
                scope = viewModelScope
            ) { isValid, errorMsg ->
                if (isValid) {
                    viewModelScope.launch {
                        _isAwaiting2Fa.value = false

                        val pending = pendingSuperAdminClaim
                        val finalAdmin = (pending ?: adminProfile ?: SuperAdminUserEntity()).copy(
                            email = targetEmail,
                            role = "SUPER_ADMIN",
                            isClaimed = true,
                            is2FaEnabled = true
                        )

                        // 1. Update Room DB
                        repository.saveSuperAdmin(finalAdmin)
                        pendingSuperAdminClaim = null

                        // 2. Update Supabase Cloud super_admin_users table
                        val arr = org.json.JSONArray().apply {
                            put(org.json.JSONObject().apply {
                                put("id", finalAdmin.id.ifBlank { "SUPER-ADMIN-MASTER" })
                                put("name", finalAdmin.name)
                                put("email", finalAdmin.email)
                                put("mobile", finalAdmin.mobile)
                                put("phone", finalAdmin.mobile)
                                put("role", "SUPER_ADMIN")
                                put("accessCode", finalAdmin.accessCode)
                                put("is2FaEnabled", true)
                                put("isClaimed", true)
                                put("upiId", finalAdmin.upiId)
                                put("upiPayeeName", finalAdmin.upiPayeeName)
                                put("supportWhatsApp", finalAdmin.mobile)
                                put("updatedAt", System.currentTimeMillis())
                            })
                        }
                        com.example.data.remote.SupabaseClient.upsertRecords("super_admin_users", arr)

                        // 3. Update public.users table in Supabase Cloud
                        val userArr = org.json.JSONArray().apply {
                            put(org.json.JSONObject().apply {
                                put("id", finalAdmin.id.ifBlank { "SUPER-ADMIN-MASTER" })
                                put("email", targetEmail)
                                put("name", finalAdmin.name.ifBlank { "Super Administrator" })
                                put("role", "SUPER_ADMIN")
                                put("libraryId", "")
                                put("phone", finalAdmin.mobile)
                                put("isActive", true)
                            })
                        }
                        com.example.data.remote.SupabaseClient.upsertRecords("users", userArr)

                        val adminUserAccount = UserAccountEntity(
                            id = finalAdmin.id.ifBlank { "SUPER-ADMIN-MASTER" },
                            email = targetEmail,
                            password = finalAdmin.accessCode,
                            name = finalAdmin.name.ifBlank { "Super Administrator" },
                            role = "SUPER_ADMIN",
                            libraryId = "",
                            phone = finalAdmin.mobile,
                            isActive = true,
                            createdAt = System.currentTimeMillis()
                        )
                        repository.saveUser(adminUserAccount)

                        _userMessage.value = "🎉 2FA Verified! Welcome to SaaS Super Admin Portal."
                        _isAuthenticated.value = true
                        _currentRole.value = "SUPER_ADMIN"
                        _currentUserEmail.value = targetEmail
                        _currentUserName.value = finalAdmin.name.ifBlank { "Super Administrator" }

                        persistAuthSession(
                            authenticated = true,
                            email = _currentUserEmail.value,
                            name = _currentUserName.value,
                            role = "SUPER_ADMIN",
                            libraryId = "",
                            studentId = ""
                        )
                        onSuccess()
                    }
                } else {
                    onError(errorMsg ?: "Invalid or expired 2FA OTP code. Please check your email and try again.")
                }
            }
        }
    }

    fun resendSuperAdminOtp(onOtpDispatched: (String) -> Unit) {
        val targetEmail = _twoFaTargetEmail.value ?: return
        _otpTimerSeconds.value = 60
        viewModelScope.launch {
            val result = SupabaseAuthService.signInWithOtp(targetEmail, shouldCreateUser = false)
            if (result.isSuccess) {
                _userMessage.value = "A new 2FA security OTP was dispatched to ${com.example.util.EmailOtpService.maskEmail(targetEmail)} via Supabase."
                onOtpDispatched("")
            } else {
                _userMessage.value = result.exceptionOrNull()?.message ?: "Failed to resend OTP."
            }
            viewModelScope.launch {
                for (sec in 60 downTo 0) {
                    _otpTimerSeconds.value = sec
                    kotlinx.coroutines.delay(1000L)
                    if (!_isAwaiting2Fa.value) break
                }
            }
        }
    }

    fun cancel2Fa() {
        _isAwaiting2Fa.value = false
        pendingSuperAdminClaim = null
    }

    fun resetUserPassword(
        email: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isBlank()) {
                onError("Please enter a valid email address.")
                return@launch
            }
            
            val res = SupabaseAuthService.sendPasswordResetEmail(trimmedEmail)
            if (res.isSuccess) {
                _userMessage.value = "Password reset instructions sent to $trimmedEmail via Supabase."
                onSuccess()
            } else {
                onError(res.exceptionOrNull()?.message ?: "Failed to send reset email.")
            }
        }
    }

    private fun persistAuthSession(
        authenticated: Boolean,
        email: String = _currentUserEmail.value,
        name: String = _currentUserName.value,
        role: String = _currentRole.value,
        libraryId: String = _currentLibraryId.value,
        studentId: String = _activeStudentId.value
    ) {
        authPrefs.edit()
            .putBoolean(KEY_IS_AUTHENTICATED, authenticated)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_ROLE, role)
            .putString(KEY_LIBRARY_ID, libraryId)
            .putString(KEY_ACTIVE_STUDENT_ID, studentId)
            .apply()
    }

    // NOTE: The old `login(email, role, name)` function was removed here.
    // It let ANY caller set `_currentRole` (SUPER_ADMIN/MANAGER/STUDENT) for
    // the current session with zero backend authorization check — a genuine
    // privilege-escalation hole (e.g. a RoleGate fallback button could
    // silently promote a STUDENT session to MANAGER). Role changes must now
    // always go through authenticateWithPassword() / claimSuperAdminSlot() /
    // the real Supabase session, which do check credentials.

    fun authenticateWithPassword(
        identifier: String,
        passwordInput: String,
        role: String,
        tenantCode: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val trimmedIdentifier = identifier.trim()
        val trimmedPassword = passwordInput.trim()
        val trimmedTenantCode = tenantCode.trim()

        if (trimmedIdentifier.isBlank()) {
            onError("Please enter your registered Email address.")
            return
        }
        if (!trimmedIdentifier.contains("@") || !trimmedIdentifier.contains(".")) {
            onError("Login is strictly restricted to Email address. Please enter your valid registered email (e.g. user@example.com).")
            return
        }
        if (trimmedPassword.isBlank()) {
            onError("Please enter your Password.")
            return
        }

        viewModelScope.launch {
            val adminProfile = repository.getSuperAdmin().firstOrNull()
            val isMatchingAdminCredentials = adminProfile != null && adminProfile.isClaimed &&
                    (trimmedIdentifier.equals(adminProfile.email, ignoreCase = true) ||
                            (adminProfile.mobile.isNotBlank() && trimmedIdentifier == adminProfile.mobile))

            // 1. Super Admin authentication branch
            if (role.equals("SUPER_ADMIN", ignoreCase = true) || isMatchingAdminCredentials) {
                val targetEmail = if (isMatchingAdminCredentials && adminProfile != null) {
                    adminProfile.email
                } else {
                    trimmedIdentifier
                }

                if (!com.example.data.remote.SupabaseClient.isConfigured()) {
                    val admin = adminProfile
                    if (admin != null && admin.isClaimed && admin.email.isNotBlank()) {
                        if (admin.email.equals(targetEmail, ignoreCase = true) &&
                            (admin.accessCode.isBlank() || admin.accessCode == trimmedPassword)
                        ) {
                            _isAuthenticated.value = true
                            _currentRole.value = "SUPER_ADMIN"
                            _currentUserEmail.value = admin.email
                            _currentUserName.value = admin.name
                            _userMessage.value = "Welcome back, ${admin.name}!"
                            persistAuthSession(
                                authenticated = true,
                                email = admin.email,
                                name = admin.name,
                                role = "SUPER_ADMIN",
                                libraryId = "",
                                studentId = ""
                            )
                            onSuccess()
                            return@launch
                        } else {
                            onError("Invalid Super Admin email or password.")
                            return@launch
                        }
                    } else {
                        // Unclaimed slot in offline mode - allow claim or auto-claim
                        val localAdmin = SuperAdminUserEntity(
                            id = "SUPER-ADMIN-MASTER",
                            email = targetEmail,
                            name = "Super Administrator",
                            role = "SUPER_ADMIN",
                            accessCode = trimmedPassword,
                            isClaimed = true
                        )
                        repository.saveSuperAdmin(localAdmin)
                        _isAuthenticated.value = true
                        _currentRole.value = "SUPER_ADMIN"
                        _currentUserEmail.value = localAdmin.email
                        _currentUserName.value = localAdmin.name
                        persistAuthSession(
                            authenticated = true,
                            email = localAdmin.email,
                            name = localAdmin.name,
                            role = "SUPER_ADMIN",
                            libraryId = "",
                            studentId = ""
                        )
                        onSuccess()
                        return@launch
                    }
                }

                // Strictly authenticate credentials against Supabase Auth
                val supabaseResult = SupabaseAuthService.signInWithPassword(
                    context = getApplication(),
                    email = targetEmail,
                    password = trimmedPassword,
                    tenantCode = null,
                    expectedRole = "SUPER_ADMIN"
                )

                if (supabaseResult.isFailure) {
                    val errMsg = supabaseResult.exceptionOrNull()?.message ?: "Invalid Super Admin credentials."
                    onError(errMsg)
                    return@launch
                }

                val session = supabaseResult.getOrNull()
                val finalEmail = session?.email?.ifBlank { targetEmail } ?: targetEmail
                val finalName = session?.name?.ifBlank { adminProfile?.name ?: "Super Administrator" } ?: "Super Administrator"

                // Synchronize super admin entity
                val updatedAdmin = (adminProfile ?: SuperAdminUserEntity()).copy(
                    email = finalEmail,
                    name = finalName,
                    role = "SUPER_ADMIN",
                    isClaimed = true
                )
                repository.saveSuperAdmin(updatedAdmin)

                val is2Fa = adminProfile?.is2FaEnabled == true && adminProfile?.email.equals(finalEmail, ignoreCase = true)

                if (is2Fa) {
                    _twoFaTargetEmail.value = finalEmail
                    _isAwaiting2Fa.value = true
                    _otpTimerSeconds.value = 60

                    val otpResult = SupabaseAuthService.signInWithOtp(finalEmail, shouldCreateUser = false)
                    if (otpResult.isSuccess) {
                        _userMessage.value = "Security 2FA OTP dispatched to ${com.example.util.EmailOtpService.maskEmail(finalEmail)} via Supabase."
                    } else {
                        _userMessage.value = "Failed to dispatch 2FA OTP: ${otpResult.exceptionOrNull()?.message}"
                    }

                    viewModelScope.launch {
                        for (sec in 60 downTo 0) {
                            _otpTimerSeconds.value = sec
                            kotlinx.coroutines.delay(1000L)
                            if (!_isAwaiting2Fa.value) break
                        }
                    }
                    return@launch
                } else {
                    _isAuthenticated.value = true
                    _currentRole.value = "SUPER_ADMIN"
                    _currentUserEmail.value = finalEmail
                    _currentUserName.value = finalName
                    _userMessage.value = "Welcome back, $finalName!"
                    persistAuthSession(
                        authenticated = true,
                        email = finalEmail,
                        name = finalName,
                        role = "SUPER_ADMIN",
                        libraryId = "",
                        studentId = ""
                    )
                    onSuccess()
                    return@launch
                }
            }

            // 2. Resolve identifier if it is mobile or student code instead of raw email
            val (resolvedEmail, resolvedTenantFromId) = SupabaseAuthService.resolveLoginEmail(trimmedIdentifier, role)
            val effectiveTenantCode = if (trimmedTenantCode.isNotBlank()) trimmedTenantCode else (resolvedTenantFromId ?: "")

            val supabaseResult = SupabaseAuthService.signInWithPassword(
                context = getApplication(),
                email = resolvedEmail,
                password = trimmedPassword,
                tenantCode = effectiveTenantCode.takeIf { it.isNotBlank() },
                expectedRole = role
            )

            if (supabaseResult.isSuccess) {
                val session = supabaseResult.getOrNull()
                if (session != null) {
                    _isAuthenticated.value = true
                    _currentUserEmail.value = session.email
                    _currentUserName.value = session.name
                    _currentRole.value = when (session.role) {
                        UserRole.SUPER_ADMIN -> "SUPER_ADMIN"
                        UserRole.OWNER -> "OWNER"
                        UserRole.STUDENT -> "STUDENT"
                    }
                    if (session.libraryId.isNotBlank()) {
                        _currentLibraryId.value = session.libraryId
                    }
                    if (session.studentId.isNotBlank()) {
                        _activeStudentId.value = session.studentId
                    }
                    _userMessage.value = "Welcome back, ${session.name}!"
                    persistAuthSession(
                        authenticated = true,
                        email = session.email,
                        name = session.name,
                        role = _currentRole.value,
                        libraryId = _currentLibraryId.value,
                        studentId = _activeStudentId.value
                    )
                    
                    if (_currentLibraryId.value.isNotBlank()) {
                        supabaseSyncManager.pullSupabaseToLocal(_currentLibraryId.value)
                        // If student session and studentId was blank, try to resolve from repository
                        if (_currentRole.value == "STUDENT" && _activeStudentId.value.isBlank()) {
                            val localStudent = repository.findStudentByIdentifier(session.email)
                                ?: repository.findStudentByIdentifier(trimmedIdentifier)
                            if (localStudent != null) {
                                _activeStudentId.value = localStudent.id
                                persistAuthSession(
                                    authenticated = true,
                                    studentId = localStudent.id
                                )
                            }
                        }
                    }

                    onSuccess()
                    return@launch
                }
            }

            // 3. Local database fallback for Manager and Student if offline
            val localUser = repository.getUserByIdentifier(trimmedIdentifier)
                ?: repository.getUserByEmail(resolvedEmail)
            if (localUser != null && localUser.password == trimmedPassword) {
                _isAuthenticated.value = true
                _currentUserEmail.value = localUser.email
                _currentUserName.value = localUser.name
                _currentRole.value = localUser.role
                if (localUser.libraryId.isNotBlank()) {
                    _currentLibraryId.value = localUser.libraryId
                }
                if (!localUser.studentIdRef.isNullOrBlank()) {
                    _activeStudentId.value = localUser.studentIdRef
                }
                _userMessage.value = "Welcome back, ${localUser.name}!"
                persistAuthSession(
                    authenticated = true,
                    email = localUser.email,
                    name = localUser.name,
                    role = localUser.role,
                    libraryId = localUser.libraryId,
                    studentId = localUser.studentIdRef ?: ""
                )
                onSuccess()
                return@launch
            }
            
            val errorMsg = supabaseResult.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                ?: "गलत ईमेल या पासवर्ड (Invalid email or password). कृपया दोबारा जांचें।"
            onError(errorMsg)
        }
    }

    
    fun resetSupabasePassword(email: String) {
        viewModelScope.launch {
            val res = SupabaseAuthService.sendPasswordResetEmail(email)
            if (res.isSuccess) {
                com.example.ui.components.SnackbarController.showSuccess(res.getOrNull() ?: "Password reset instructions sent.")
            } else {
                com.example.ui.components.SnackbarController.showError(res.exceptionOrNull()?.message ?: "Failed to send reset email.")
            }
        }
    }

    fun toggleSeatStatusById(seatId: String, newStatus: String) {
        viewModelScope.launch {
            val currentSeats = seats.value
            val seat = currentSeats.find { it.id == seatId }
            if (seat != null) {
                repository.updateSeatStatus(_currentLibraryId.value, seat, newStatus)
                _userMessage.value = "Seat ${seat.seatNumber} marked as $newStatus"
                com.example.ui.components.SnackbarController.showSuccess("Seat ${seat.seatNumber} is now $newStatus")
            }
        }
    }

    fun checkLibraryTrialEligibility(
        email: String,
        phone: String,
        onResult: (isEligible: Boolean, errorMsg: String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.checkLibraryTrialEligibility(email, phone)
            onResult(res.first, res.second)
        }
    }

    fun requestOwnerSignupOtp(
        email: String,
        name: String,
        phone: String,
        onOtpSent: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val (isEligible, trialErrMsg) = repository.checkLibraryTrialEligibility(email, phone)
            if (!isEligible) {
                val err = trialErrMsg ?: "इस Email या Mobile Number पर पहले से एक Library रजिस्टर्ड है। 15 दिनों का Free Trial केवल एक बार ही मिलता है।"
                onError(err)
                return@launch
            }

            com.example.util.EmailOtpService.dispatchEmailOtp(
                email = email.trim().lowercase(),
                recipientName = name.trim(),
                purpose = com.example.util.OtpPurpose.SIGNUP_VERIFICATION,
                scope = viewModelScope
            ) { result ->
                if (result.isSuccess) {
                    onOtpSent(result.message)
                } else {
                    onError(result.message)
                }
            }
        }
    }

    fun verifyOwnerSignupOtp(
        email: String,
        enteredOtp: String,
        onVerified: (Boolean, String?) -> Unit
    ) {
        com.example.util.EmailOtpService.verifyOtp(
            context = getApplication(),
            email = email.trim().lowercase(),
            enteredOtp = enteredOtp.trim(),
            purpose = com.example.util.OtpPurpose.SIGNUP_VERIFICATION,
            scope = viewModelScope,
            onVerified = onVerified
        )
    }

    fun registerAndLogin(
        name: String,
        email: String,
        libraryName: String,
        phone: String,
        password: String,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            // Strict duplicate check: 15-day trial is only permitted ONCE per email and phone number
            val (isEligible, trialErrMsg) = repository.checkLibraryTrialEligibility(email, phone)
            if (!isEligible) {
                val err = trialErrMsg ?: "इस Email या Mobile Number पर पहले से एक Library रजिस्टर्ड है। 15 दिनों का Free Trial केवल एक बार ही मिलता है।"
                com.example.ui.components.SnackbarController.showError(err)
                _userMessage.value = err
                onError?.invoke(err)
                return@launch
            }

            val libId = "LIB-${UUID.randomUUID().toString().take(6).uppercase()}"
            
            val signUpResult = SupabaseAuthService.signUp(
                context = getApplication(),
                email = email,
                password = password,
                name = name,
                role = UserRole.OWNER,
                libraryId = libId,
                phone = phone.trim()
            )
            
            if (signUpResult.isFailure) {
                com.example.ui.components.SnackbarController.showError(signUpResult.exceptionOrNull()?.message ?: "Registration failed")
                return@launch
            }

            val authUserId = signUpResult.getOrNull()?.userId?.takeIf { it.isNotBlank() } ?: "USER-${UUID.randomUUID().toString().take(6)}"

            val newLib = LibraryEntity(
                id = libId,
                name = libraryName.ifBlank { "My Library" },
                code = "LIB-${(1000..9999).random()}",
                ownerName = name,
                ownerPhone = phone,
                ownerEmail = email,
                address = "",
                city = "",
                state = "",
                pincode = "",
                upiId = "",
                upiPayeeName = libraryName.ifBlank { "My Library" }
            )
            repository.saveLibrary(newLib)

            val localUserAccount = UserAccountEntity(
                id = authUserId,
                email = email,
                password = password.trim(),
                name = name,
                role = "OWNER",
                libraryId = libId,
                phone = phone.trim()
            )
            repository.saveUser(localUserAccount)

            val hallId = "HALL-MAIN"
            repository.saveHall(
                HallEntity(
                    id = hallId,
                    libraryId = libId,
                    name = "Main Silent Hall",
                    type = "AC Study Hall",
                    floor = "Ground Floor",
                    seatCount = 40
                )
            )
            repository.generateBatchSeats(
                libraryId = libId,
                prefix = "A",
                count = 40,
                hallId = hallId,
                hallName = "Main Silent Hall",
                sectionId = "SEC-A",
                sectionName = "Silent Zone",
                floor = "Ground Floor",
                seatType = "Standard",
                fee = 1200.0
            )

            // Setup 15-Day Free Trial
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 15)
            val expiryDate = sdf.format(cal.time)
            val trialSub = LibrarySubscriptionEntity(
                id = "SUB-$libId",
                libraryId = libId,
                libraryName = newLib.name,
                planId = "PLAN-TRIAL",
                planName = "15-Day Free Trial",
                status = "TRIAL",
                startDate = startDate,
                expiryDate = expiryDate,
                price = 0.0,
                discount = 0.0,
                maxSeats = 50,
                autoRenew = false,
                notes = "Automatic 15-day free trial on library creation"
            )
            repository.saveLibrarySubscription(trialSub)

            _currentLibraryId.value = libId
            _isAuthenticated.value = true
            _currentRole.value = "OWNER"
            _currentUserEmail.value = email
            _currentUserName.value = name
            _userMessage.value = "Account & Library created successfully!"

            persistAuthSession(
                authenticated = true,
                email = email,
                name = name,
                role = "OWNER",
                libraryId = libId,
                studentId = _activeStudentId.value
            )
            
            supabaseSyncManager.syncLocalToSupabase(libId)
        }
    }

    fun logout() {
        SessionManager.logout(getApplication())
        com.example.data.remote.AuthGuardService.reset()
        _isAuthenticated.value = false
        _userMessage.value = "You have been logged out."
        persistAuthSession(
            authenticated = false,
            email = _currentUserEmail.value,
            name = _currentUserName.value,
            role = _currentRole.value,
            libraryId = _currentLibraryId.value,
            studentId = _activeStudentId.value
        )
    }

    fun switchRole() {

        _userMessage.value = "Active role is locked to your authenticated login. Sign out to switch account."
    }

    fun setRole(role: String) {
        _currentRole.value = role
        persistAuthSession(
            authenticated = _isAuthenticated.value,
            role = role
        )
    }

    fun selectLibrary(libraryId: String) {
        _currentLibraryId.value = libraryId
        persistAuthSession(
            authenticated = _isAuthenticated.value,
            libraryId = libraryId
        )
        viewModelScope.launch {
            repository.pullFromCloud(libraryId)
        }
    }

    fun refreshAllData(forceCloud: Boolean = true) {
        viewModelScope.launch {
            _isSupabaseSyncing.value = true
            val libId = _currentLibraryId.value
            if (libId.isNotBlank()) {
                repository.pullFromCloud(libId)
            }
            repository.pullAllLibrariesFromCloud()
            repository.pullAllSubscriptionPlansFromCloud()
            repository.pullSuperAdminFromCloud()
            _isSupabaseSyncing.value = false
            _userMessage.value = "Data refreshed live from Supabase cloud"
        }
    }

    fun selectActiveStudent(studentId: String) {
        _activeStudentId.value = studentId
        persistAuthSession(
            authenticated = _isAuthenticated.value,
            studentId = studentId
        )
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    // NOTE: seedDemoData() was removed here — it silently populated a fake
    // "Vanguard" library with hardcoded people (Rajesh Verma, Aditya Sharma)
    // and a fake paid subscription. A production multi-tenant SaaS app
    // should never ship a built-in "inject fake accounts" capability.

    
    fun createNewLibrary(
        name: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        address: String,
        city: String,
        state: String,
        pincode: String,
        upiId: String,
        upiPayeeName: String,
        initialHallName: String,
        initialSeatCount: Int
    ) {
        viewModelScope.launch {
            val randomSuffix = (1000..9999).random()
            val codePrefix = name.take(3).uppercase()
            val libraryCode = "$codePrefix-$randomSuffix"
            val libId = "LIB-${UUID.randomUUID().toString().take(8).uppercase()}"

            val newLib = LibraryEntity(
                id = libId,
                name = name,
                code = libraryCode,
                ownerName = ownerName,
                ownerPhone = ownerPhone,
                ownerWhatsApp = ownerPhone,
                ownerEmail = ownerEmail,
                address = address,
                city = city,
                state = state,
                pincode = pincode,
                upiId = upiId,
                upiPayeeName = upiPayeeName
            )
            repository.saveLibrary(newLib)

            
            val hallId = "HALL-${UUID.randomUUID().toString().take(6)}"
            val hall = HallEntity(
                id = hallId,
                libraryId = libId,
                name = initialHallName.ifEmpty { "Main Reading Hall" },
                type = "AC Study Hall",
                floor = "Ground Floor",
                seatCount = initialSeatCount
            )
            repository.saveHall(hall)

            
            repository.saveShift(ShiftEntity(UUID.randomUUID().toString(), libId, "Morning Batch", "06:00 AM", "01:30 PM", 900.0))
            repository.saveShift(ShiftEntity(UUID.randomUUID().toString(), libId, "Evening Batch", "01:30 PM", "08:30 PM", 900.0))
            repository.saveShift(ShiftEntity(UUID.randomUUID().toString(), libId, "Full Day (24x7)", "06:00 AM", "11:00 PM", 1500.0))

            
            repository.savePlan(
                MembershipPlanEntity(
                    id = UUID.randomUUID().toString(),
                    libraryId = libId,
                    name = "Monthly Regular",
                    durationMonths = 1,
                    durationDays = 30,
                    durationType = "MONTHS",
                    baseFee = 1000.0,
                    maintenanceFee = 100.0,
                    securityDeposit = 500.0,
                    discount = 0.0
                )
            )

            
            if (initialSeatCount > 0) {
                repository.generateBatchSeats(
                    libId,
                    "A",
                    initialSeatCount.coerceIn(1, 500),
                    hallId,
                    hall.name,
                    "",
                    "",
                    "Ground Floor",
                    "Standard",
                    1000.0,
                    startNumber = 1
                )
            }

            // Setup 15-Day Free Trial
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = sdf.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 15)
            val expiryDate = sdf.format(cal.time)
            val trialSub = LibrarySubscriptionEntity(
                id = "SUB-$libId",
                libraryId = libId,
                libraryName = newLib.name,
                planId = "PLAN-TRIAL",
                planName = "15-Day Free Trial",
                status = "TRIAL",
                startDate = startDate,
                expiryDate = expiryDate,
                price = 0.0,
                discount = 0.0,
                maxSeats = 50,
                autoRenew = false,
                notes = "Automatic 15-day free trial on onboarding"
            )
            repository.saveLibrarySubscription(trialSub)

            _currentLibraryId.value = libId
            _currentRole.value = "OWNER"
            _userMessage.value = "Welcome to LibDesk! Library created with code $libraryCode"
        }
    }

    
    fun assignSeat(seat: SeatEntity, student: StudentEntity, shift: ShiftEntity?, plan: MembershipPlanEntity?, date: String) {
        viewModelScope.launch {
            repository.assignSeat(_currentLibraryId.value, seat, student, shift, plan, date)
            _userMessage.value = "Seat ${seat.seatNumber} assigned to ${student.fullName}"
        }
    }

    fun transferSeat(oldSeat: SeatEntity, newSeat: SeatEntity, student: StudentEntity) {
        viewModelScope.launch {
            repository.transferSeat(_currentLibraryId.value, oldSeat, newSeat, student)
            _userMessage.value = "Transferred ${student.fullName} from ${oldSeat.seatNumber} to ${newSeat.seatNumber}"
        }
    }

    fun releaseSeat(seat: SeatEntity) {
        viewModelScope.launch {
            repository.releaseSeat(_currentLibraryId.value, seat)
            _userMessage.value = "Released seat ${seat.seatNumber}"
        }
    }

    fun updateSeatStatus(seat: SeatEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateSeatStatus(_currentLibraryId.value, seat, newStatus)
            _userMessage.value = "Seat ${seat.seatNumber} marked as $newStatus"
        }
    }

    fun toggleSeatReservation(seat: SeatEntity) {
        viewModelScope.launch {
            val newStatus = repository.toggleSeatReservation(_currentLibraryId.value, seat)
            _userMessage.value = "Seat ${seat.seatNumber} is now $newStatus"
        }
    }

    fun batchGenerateSeats(
        prefix: String,
        count: Int,
        hallId: String,
        hallName: String,
        secId: String,
        secName: String,
        floor: String,
        type: String,
        fee: Double,
        startNumber: Int? = null
    ) {
        viewModelScope.launch {
            repository.generateBatchSeats(_currentLibraryId.value, prefix, count, hallId, hallName, secId, secName, floor, type, fee, startNumber)
            _userMessage.value = "Generated $count new seats with prefix $prefix"
        }
    }

    
    fun registerStudent(
        fullName: String,
        mobile: String,
        email: String,
        gender: String,
        targetExam: String,
        courseClass: String,
        address: String,
        parentName: String,
        parentMobile: String,
        joiningDate: String,
        shift: ShiftEntity?,
        plan: MembershipPlanEntity?,
        fee: Double,
        paid: Double
    ) {
        viewModelScope.launch {
            val studentCode = "STU-${(1000..9999).random()}"
            val studentId = UUID.randomUUID().toString()

            // IMPORTANT: student starts with paidAmount = 0 and dueAmount = full fee.
            // The initial "paid" amount (if any) is applied ONLY via recordPayment()
            // below, which is the single source of truth for paid/due/totalFee math.
            // Previously this set paidAmount/dueAmount here AND ALSO called
            // recordPayment() with the same amount, double-counting the payment
            // (paidAmount got doubled and totalFee auto-inflated to match).
            val computedExpiry = if (plan != null) {
                val cal = Calendar.getInstance()
                if (plan.durationType.equals("DAYS", ignoreCase = true) || plan.durationDays > 0) {
                    cal.add(Calendar.DAY_OF_YEAR, if (plan.durationDays > 0) plan.durationDays else 30)
                } else {
                    cal.add(Calendar.MONTH, if (plan.durationMonths > 0) plan.durationMonths else 1)
                }
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            } else ""

            val student = StudentEntity(
                id = studentId,
                libraryId = _currentLibraryId.value,
                studentCode = studentCode,
                fullName = fullName,
                mobile = mobile,
                email = email,
                gender = gender,
                targetExam = targetExam,
                courseClass = courseClass,
                address = address,
                parentName = parentName,
                parentMobile = parentMobile,
                joiningDate = joiningDate,
                shiftId = shift?.id ?: "",
                shiftName = shift?.name ?: "",
                planId = plan?.id ?: "",
                planName = plan?.name ?: "",
                expiryDate = computedExpiry,
                totalFee = fee,
                paidAmount = 0.0,
                dueAmount = fee,
                rfidQrCode = "QR-$studentCode"
            )
            repository.saveStudent(student)

            if (paid > 0) {
                repository.recordPayment(
                    _currentLibraryId.value,
                    student,
                    paid,
                    "UPI",
                    "MEMBERSHIP_JOINING_FEE",
                    "JOIN-${System.currentTimeMillis().toString().takeLast(6)}"
                )
            }

            

            if (_currentRole.value == "STUDENT") {
                _activeStudentId.value = student.id
            }

            // Ensure student user account exists in users table and syncs to Supabase
            val studentEmail = email.trim().ifBlank { "${mobile.filter { it.isDigit() }}@student.libdesk" }
            val studentUserAccount = UserAccountEntity(
                id = student.userId.ifBlank { student.id },
                email = studentEmail,
                password = mobile.takeLast(6),
                name = fullName.trim().ifBlank { "Student" },
                role = "STUDENT",
                libraryId = _currentLibraryId.value,
                phone = mobile.trim(),
                studentIdRef = student.id,
                isActive = true
            )
            repository.saveUser(studentUserAccount)

            val currentToken = SessionManager.sessionState.value?.accessToken ?: ""
            if (email.isNotBlank() && email.contains("@")) {
                SupabaseAuthService.createStudentUserViaEdgeFunction(
                    ownerAccessToken = currentToken,
                    email = email.trim(),
                    fullName = fullName.trim(),
                    mobile = mobile.trim(),
                    libraryId = _currentLibraryId.value,
                    studentCode = studentCode
                )
            }

            _userMessage.value = "Student registered: $fullName ($studentCode)"
        }
    }

    
    fun registerStudentViaQr(
        libraryId: String,
        fullName: String,
        mobile: String,
        email: String,
        targetExam: String,
        shift: ShiftEntity?,
        plan: MembershipPlanEntity?,
        password: String
    ) {
        viewModelScope.launch {
            val targetLib = repository.getLibraryById(libraryId).firstOrNull() ?: repository.getAllLibraries().firstOrNull()?.firstOrNull()
            val effectiveLibId = targetLib?.id ?: if (libraryId.isNotBlank()) libraryId else _currentLibraryId.value
            val libName = targetLib?.name ?: "Library Center"

            val studentCode = "STU-${(1000..9999).random()}"
            val studentId = UUID.randomUUID().toString()
            val fee = plan?.baseFee ?: 1200.0

            val resolvedEmail = email.ifBlank { "${mobile.filter { it.isDigit() }}@student.libdesk" }
            
            val signUpResult = SupabaseAuthService.signUp(
                context = getApplication(),
                email = resolvedEmail,
                password = password,
                name = fullName.ifBlank { "New Student" },
                role = UserRole.STUDENT,
                libraryId = effectiveLibId,
                phone = mobile.trim()
            )
            
            if (signUpResult.isFailure) {
                com.example.ui.components.SnackbarController.showError(signUpResult.exceptionOrNull()?.message ?: "Registration failed")
                return@launch
            }

            val authUserId = signUpResult.getOrNull()?.userId?.takeIf { it.isNotBlank() } ?: studentId

            val student = StudentEntity(
                id = studentId,
                libraryId = effectiveLibId,
                userId = authUserId,
                studentCode = studentCode,
                fullName = fullName.trim(),
                mobile = mobile.trim(),
                email = resolvedEmail,
                gender = "Other",
                targetExam = targetExam.ifBlank { "Competitive Exams" },
                courseClass = "Self Study",
                address = "Self-enrolled via Library QR Code",
                parentName = "",
                parentMobile = "",
                shiftId = shift?.id ?: "SHIFT-MORN",
                shiftName = shift?.name ?: "Morning Shift (6 AM - 2 PM)",
                planId = plan?.id ?: "PLAN-STD",
                planName = plan?.name ?: "Monthly Standard Access",
                totalFee = fee,
                paidAmount = 0.0,
                dueAmount = fee,
                status = "ACTIVE",
                rfidQrCode = "QR-$studentCode",
                password = password.trim()
            )
            repository.saveStudent(student)

            val studentUserAccount = UserAccountEntity(
                id = authUserId,
                email = resolvedEmail,
                password = password.trim(),
                name = fullName.trim(),
                role = "STUDENT",
                libraryId = effectiveLibId,
                phone = mobile.trim(),
                studentIdRef = studentId,
                isActive = true
            )
            repository.saveUser(studentUserAccount)

            _currentLibraryId.value = effectiveLibId
            _activeStudentId.value = student.id
            _currentRole.value = "STUDENT"
            _currentUserEmail.value = student.email
            _currentUserName.value = student.fullName
            _isAuthenticated.value = true
            _userMessage.value = "🎉 Welcome to $libName! Enrolled as ${student.fullName} ($studentCode)"

            persistAuthSession(
                authenticated = true,
                email = student.email,
                name = student.fullName,
                role = "STUDENT",
                libraryId = effectiveLibId,
                studentId = student.id
            )
            
            supabaseSyncManager.pushStudent(student)
        }
    }

    
    fun dismissCheckInConfirmation() {
        _checkInConfirmation.value = null
    }

    fun triggerTestCheckInAnimation(
        studentName: String = "Rahul Sharma",
        seatNumber: String = "A-14",
        hallName: String = "Main AC Study Hall",
        shiftName: String = "Morning Shift (07:00 AM – 01:00 PM)"
    ) {
        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        _checkInConfirmation.value = SeatCheckInDetails(
            studentName = studentName,
            studentCode = "STU-001",
            seatNumber = seatNumber,
            hallName = hallName,
            shiftName = shiftName,
            checkInTime = nowTime,
            mode = "Turnstile Gate Verified"
        )
    }

    fun performDirectSeatCheckIn(studentId: String? = null, seatNumber: String? = null) {
        viewModelScope.launch {
            val targetStudentId = studentId ?: _activeStudentId.value
            val student = repository.getStudentById(targetStudentId).firstOrNull()
                ?: students.value.find { it.id == targetStudentId }
                ?: students.value.firstOrNull()

            val studentName = student?.fullName ?: "Library Scholar"
            val targetSeat = seatNumber ?: student?.seatNumber?.ifBlank { "01" } ?: "01"
            val hall = student?.hallName?.ifBlank { "Main Study Hall" } ?: "Main Study Hall"
            val shift = student?.shiftName?.ifBlank { "Full Day Shift" } ?: "Full Day Shift"
            val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val att = AttendanceEntity(
                id = "ATT-AUTO-${UUID.randomUUID().toString().take(8)}",
                libraryId = _currentLibraryId.value,
                studentId = student?.id ?: targetStudentId,
                studentName = studentName,
                seatNumber = targetSeat,
                hallName = hall,
                shiftName = shift,
                date = today,
                checkInTime = nowTime,
                checkOutTime = "",
                durationMinutes = 0,
                status = "CHECKED_IN",
                mode = "QUICK_CHECKIN",
                notes = "Direct Seat Punch via App"
            )
            repository.manualAttendance(att)
            _checkInConfirmation.value = SeatCheckInDetails(
                studentName = studentName,
                studentCode = student?.studentCode ?: "STU-001",
                seatNumber = targetSeat,
                hallName = hall,
                shiftName = shift,
                checkInTime = nowTime,
                mode = "Instant Seat Punch"
            )
            // SnackbarController.showSuccess("Checked in to Desk #$targetSeat successfully!")
        }
    }

    fun scanQrAttendance(
        code: String,
        studentIdContext: String? = null,
        locationNote: String? = null,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val effectiveStudentId = studentIdContext ?: _activeStudentId.value
            val (success, message) = repository.processQrAttendance(_currentLibraryId.value, code, effectiveStudentId, locationNote)
            _userMessage.value = message
            onResult?.invoke(success, message)
            if (success) {
                if (message.contains("Checked IN", ignoreCase = true)) {
                    val student = repository.getStudentById(effectiveStudentId).firstOrNull()
                        ?: students.value.find { it.id == effectiveStudentId }
                    val seatNumber = student?.seatNumber?.ifBlank { "General Desk" } ?: "General Desk"
                    _checkInConfirmation.value = SeatCheckInDetails(
                        studentName = student?.fullName ?: "Library Scholar",
                        studentCode = student?.studentCode ?: effectiveStudentId,
                        seatNumber = seatNumber,
                        hallName = student?.hallName?.ifBlank { "Main Study Hall" } ?: "Main Study Hall",
                        shiftName = student?.shiftName?.ifBlank { "Full Day Shift" } ?: "Full Day Shift",
                        checkInTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                        mode = if (code.startsWith("GATE") || code.startsWith("LIBDESK_GATE")) "Turnstile Gate Scan" else "Allocated Seat QR Verified"
                    )
                }
            } else {
                SnackbarController.showError(message)
            }
        }
    }

    fun recordManualAttendance(
        studentId: String,
        studentName: String,
        seatNumber: String,
        hallName: String,
        shiftName: String,
        date: String,
        checkInTime: String,
        checkOutTime: String = "",
        durationMinutes: Int = 0,
        status: String = "CHECKED_IN",
        mode: String = "MANUAL",
        notes: String = "Manual Desk Entry"
    ) {
        viewModelScope.launch {
            val att = AttendanceEntity(
                id = "ATT-MAN-${UUID.randomUUID().toString().take(8)}",
                libraryId = _currentLibraryId.value,
                studentId = studentId,
                studentName = studentName,
                seatNumber = seatNumber,
                hallName = hallName,
                shiftName = shiftName,
                date = date,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                durationMinutes = durationMinutes,
                status = status,
                mode = mode,
                notes = notes
            )
            repository.manualAttendance(att)
            // SnackbarController.showSuccess("Attendance recorded for $studentName")
        }
    }

    fun markAttendanceCheckOut(attendanceId: String, checkOutTimeStr: String? = null) {
        viewModelScope.launch {
            val currentLogs = attendanceLogs.value
            val record = currentLogs.find { it.id == attendanceId }
            if (record != null) {
                val nowTime = checkOutTimeStr ?: SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                val duration = try {
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val t1 = sdf.parse(record.checkInTime)
                    val t2 = sdf.parse(nowTime)
                    if (t1 != null && t2 != null) {
                        val diff = (t2.time - t1.time) / (60 * 1000)
                        if (diff > 0) diff.toInt() else 60
                    } else 60
                } catch (e: Exception) { 60 }
                val updated = record.copy(
                    checkOutTime = nowTime,
                    durationMinutes = duration,
                    status = "CHECKED_OUT"
                )
                repository.updateAttendance(updated)
                // SnackbarController.showSuccess("Checked out ${record.studentName} at $nowTime (${duration / 60}h ${duration % 60}m)")
            }
        }
    }

    fun deleteAttendanceRecord(attendanceId: String) {
        viewModelScope.launch {
            repository.deleteAttendance(attendanceId)
            // SnackbarController.showSuccess("Attendance entry removed successfully")
        }
    }

    fun editAttendanceRecord(
        recordId: String,
        newDate: String,
        newCheckIn: String,
        newCheckOut: String,
        newStatus: String,
        newNotes: String
    ) {
        viewModelScope.launch {
            val allRecords = attendanceLogs.value
            val record = allRecords.find { it.id == recordId } ?: return@launch
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val duration = try {
                if (newCheckIn.isNotBlank() && newCheckOut.isNotBlank()) {
                    val t1 = sdf.parse(newCheckIn)
                    val t2 = sdf.parse(newCheckOut)
                    if (t1 != null && t2 != null) {
                        val diff = (t2.time - t1.time) / (60 * 1000)
                        if (diff > 0) diff.toInt() else record.durationMinutes
                    } else record.durationMinutes
                } else record.durationMinutes
            } catch (e: Exception) { record.durationMinutes }

            val updated = record.copy(
                date = newDate.ifBlank { record.date },
                checkInTime = newCheckIn.ifBlank { record.checkInTime },
                checkOutTime = newCheckOut,
                status = newStatus,
                notes = newNotes,
                durationMinutes = duration
            )
            repository.updateAttendance(updated)
            // SnackbarController.showSuccess("Attendance entry corrected successfully")
        }
    }

    fun editPaymentRecord(
        payment: PaymentEntity,
        newAmount: Double,
        newMode: String,
        newPurpose: String,
        newRef: String,
        newRemarks: String,
        newDate: String
    ) {
        viewModelScope.launch {
            val updated = payment.copy(
                amount = newAmount,
                paymentMode = newMode,
                purpose = newPurpose,
                referenceNumber = newRef,
                remarks = newRemarks,
                date = newDate
            )
            repository.updatePayment(updated)
            // SnackbarController.showSuccess("Payment receipt #${payment.receiptNumber} updated")
        }
    }

    fun deletePaymentRecord(payment: PaymentEntity) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            // SnackbarController.showSuccess("Payment receipt #${payment.receiptNumber} deleted")
        }
    }

    fun editExpenseRecord(
        expense: ExpenseEntity,
        newCategory: String,
        newAmount: Double,
        newDate: String,
        newDesc: String,
        newMode: String
    ) {
        viewModelScope.launch {
            val updated = expense.copy(
                category = newCategory,
                amount = newAmount,
                date = newDate,
                description = newDesc,
                paymentMode = newMode
            )
            repository.updateExpense(updated)
            // SnackbarController.showSuccess("Expense record updated successfully")
        }
    }

    fun deleteExpenseRecord(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            // SnackbarController.showSuccess("Expense record deleted")
        }
    }

    fun extendTrial(libraryId: String, additionalDays: Int = 15) {
        viewModelScope.launch {
            val currentSub = repository.getSubscriptionDirect(libraryId)
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentExpiry = try {
                if (currentSub != null && currentSub.expiryDate.isNotBlank()) sdf.parse(currentSub.expiryDate) else null
            } catch (e: Exception) { null }

            if (currentExpiry != null && currentExpiry.after(cal.time)) {
                cal.time = currentExpiry
            }
            cal.add(Calendar.DAY_OF_YEAR, additionalDays)
            val newExpiry = sdf.format(cal.time)

            if (currentSub != null) {
                repository.saveLibrarySubscription(
                    currentSub.copy(
                        expiryDate = newExpiry,
                        status = if (currentSub.status == "EXPIRED") "TRIAL" else currentSub.status,
                        notes = "Trial extended by $additionalDays days by SuperAdmin",
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                repository.saveLibrarySubscription(
                    LibrarySubscriptionEntity(
                        id = "SUB-$libraryId",
                        libraryId = libraryId,
                        libraryName = "Library",
                        planId = "PLAN-TRIAL",
                        planName = "15-Day Free Trial",
                        status = "TRIAL",
                        startDate = sdf.format(Date()),
                        expiryDate = newExpiry,
                        price = 0.0,
                        discount = 0.0,
                        maxSeats = 50,
                        notes = "Initial 15-day free trial"
                    )
                )
            }
            _userMessage.value = "15-day trial extended by $additionalDays days until $newExpiry!"
        }
    }

    fun requestSubscriptionUpgrade(plan: SaaSSubscriptionPlanEntity, libraryId: String = _currentLibraryId.value) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = sdf.format(cal.time)
            cal.add(Calendar.MONTH, if (plan.durationMonths > 0) plan.durationMonths else 1)
            val expiryDate = sdf.format(cal.time)

            val libName = currentLibrary.value?.name ?: "Your Library"
            val newSub = LibrarySubscriptionEntity(
                id = "SUB-$libraryId",
                libraryId = libraryId,
                libraryName = libName,
                planId = plan.id,
                planName = plan.name,
                status = "ACTIVE",
                startDate = startDate,
                expiryDate = expiryDate,
                price = plan.price,
                discount = 0.0,
                maxSeats = plan.maxSeats,
                autoRenew = true,
                notes = "Subscribed to ${plan.name} from app",
                updatedAt = System.currentTimeMillis()
            )
            repository.saveLibrarySubscription(newSub)
            _userMessage.value = "Upgraded to ${plan.name}! All premium features are active."
        }
    }

    
    fun addHall(name: String, floor: String, seatCount: Int) {
        val libId = currentLibraryId.value
        viewModelScope.launch {
            val hall = HallEntity(
                id = "HAL-${UUID.randomUUID().toString().take(8)}",
                libraryId = libId,
                name = name,
                type = "READING_HALL",
                floor = floor,
                seatCount = seatCount
            )
            repository.insertHall(hall)
            _userMessage.value = "Hall '$name' added successfully"
        }
    }

    fun updateHall(hall: HallEntity) {
        viewModelScope.launch {
            repository.updateHall(hall)
            _userMessage.value = "Hall '${hall.name}' updated successfully"
        }
    }

    fun deleteHall(hall: HallEntity) {
        viewModelScope.launch {
            repository.deleteHall(hall)
            _userMessage.value = "Hall '${hall.name}' deleted"
        }
    }

    fun addShift(name: String, startTime: String, endTime: String, fee: Double) {
        val libId = currentLibraryId.value
        viewModelScope.launch {
            val shift = ShiftEntity(
                id = "SHF-${UUID.randomUUID().toString().take(8)}",
                libraryId = libId,
                name = name,
                startTime = startTime,
                endTime = endTime,
                fee = fee
            )
            repository.insertShift(shift)
            _userMessage.value = "Shift '$name' added successfully"
        }
    }

    fun updateShift(shift: ShiftEntity) {
        viewModelScope.launch {
            repository.updateShift(shift)
            _userMessage.value = "Shift '${shift.name}' updated successfully"
        }
    }

    fun deleteShift(shift: ShiftEntity) {
        viewModelScope.launch {
            repository.deleteShift(shift)
            _userMessage.value = "Shift '${shift.name}' deleted"
        }
    }

    fun addMembershipPlan(
        name: String,
        duration: Int,
        fee: Double,
        durationType: String = "MONTHS",
        discount: Double = 0.0,
        facilities: String = "High-Speed Wi-Fi, RO Water, Charging Socket, Silent AC",
        shiftId: String = ""
    ) {
        val libId = currentLibraryId.value
        viewModelScope.launch {
            val (months, days) = if (durationType.equals("DAYS", ignoreCase = true)) {
                Pair((duration / 30).coerceAtLeast(1), duration)
            } else {
                Pair(duration, duration * 30)
            }
            val plan = MembershipPlanEntity(
                id = "PLN-${UUID.randomUUID().toString().take(8)}",
                libraryId = libId,
                name = name,
                durationMonths = months,
                durationDays = days,
                durationType = durationType,
                baseFee = fee,
                discount = discount,
                facilities = facilities,
                shiftId = shiftId
            )
            repository.insertMembershipPlan(plan)
            _userMessage.value = "Plan '$name' added successfully"
        }
    }

    fun updateMembershipPlan(plan: MembershipPlanEntity) {
        viewModelScope.launch {
            repository.updateMembershipPlan(plan)
            _userMessage.value = "Plan '${plan.name}' updated successfully"
        }
    }

    fun deleteMembershipPlan(plan: MembershipPlanEntity) {
        viewModelScope.launch {
            repository.deletePlan(plan)
            _userMessage.value = "Plan '${plan.name}' deleted"
        }
    }

    fun addNewBook(
        title: String,
        author: String,
        isbn: String,
        category: String,
        subject: String,
        rack: String,
        shelf: String,
        copies: Int
    ) {
        viewModelScope.launch {
            val book = PhysicalBookEntity(
                id = UUID.randomUUID().toString(),
                libraryId = _currentLibraryId.value,
                title = title,
                author = author,
                isbn = isbn,
                category = category,
                subject = subject,
                rack = rack,
                shelf = shelf,
                accessionNumber = "ACC-${(100..999).random()}",
                totalCopies = copies,
                availableCopies = copies,
                issuedCopies = 0
            )
            repository.saveBook(book)
            _userMessage.value = "Book added to catalog: $title"
        }
    }

    fun issueBook(book: PhysicalBookEntity, student: StudentEntity) {
        viewModelScope.launch {
            val (success, msg) = repository.issueBook(_currentLibraryId.value, book, student)
            _userMessage.value = msg
        }
    }

    fun returnBook(issue: BookIssueEntity) {
        viewModelScope.launch {
            val (success, msg) = repository.returnBook(_currentLibraryId.value, issue)
            _userMessage.value = msg
        }
    }

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    
    fun addDigitalMaterial(
        title: String,
        description: String,
        category: String,
        subject: String,
        exam: String,
        fileType: String,
        fileSize: String,
        fileUrl: String,
        accessPolicy: String
    ) {
        viewModelScope.launch {
            val doc = DigitalMaterialEntity(
                id = "DM-" + UUID.randomUUID().toString().take(8).uppercase(),
                libraryId = _currentLibraryId.value,
                title = title.trim(),
                description = description.trim().ifBlank { "High-yield study material and revision notes." },
                category = category.trim().ifBlank { "General Studies" },
                subject = subject.trim().ifBlank { "Exam Preparation" },
                exam = exam.trim().ifBlank { "All Competitive Exams" },
                fileType = fileType.ifBlank { "PDF" },
                fileSize = fileSize.ifBlank { "5.2 MB" },
                fileUrl = fileUrl.ifBlank { "https://libdesk.cloud/materials/study-notes.pdf" },
                accessPolicy = accessPolicy.ifBlank { "ALL_STUDENTS" },
                uploadDate = getTodayDateString(),
                downloadCount = 1
            )
            repository.saveDigitalMaterial(doc)
            _userMessage.value = "✅ Digital study resource published: $title"
        }
    }

    val isFetchingNcertBooks = kotlinx.coroutines.flow.MutableStateFlow(false)

    fun loadCuratedFreeStudyPdfs(onlyClass1To10: Boolean = false) {
        viewModelScope.launch {
            val libId = _currentLibraryId.value
            isFetchingNcertBooks.value = true
            try {
                val catalog = if (onlyClass1To10) {
                    com.example.data.remote.NcertCatalogService.fetchClass1To10Catalog()
                } else {
                    com.example.data.remote.NcertCatalogService.fetchNcertCatalog()
                }
                val materials = catalog.mapIndexed { index, metadata ->
                    DigitalMaterialEntity(
                        id = "DM-NCERT-${index + 1}-${libId.take(4)}",
                        libraryId = libId,
                        title = metadata.title,
                        description = metadata.description.ifBlank { "Official NCERT textbook for ${metadata.title} (${metadata.category})" },
                        category = metadata.category,
                        subject = metadata.subject,
                        exam = metadata.exam,
                        fileType = "PDF",
                        fileSize = "Digital PDF",
                        fileUrl = metadata.fileUrl,
                        accessPolicy = "ALL_STUDENTS",
                        uploadDate = getTodayDateString(),
                        downloadCount = 1,
                        isBookmarked = false
                    )
                }
                materials.forEach { repository.saveDigitalMaterial(it) }
                val label = if (onlyClass1To10) "Class 1 to 10th" else "Class 1-12"
                _userMessage.value = "📚 All NCERT $label Textbooks (${materials.size} Books) successfully fetched & added to Study Materials!"
            } catch (e: Exception) {
                _userMessage.value = "Could not load NCERT books: ${e.localizedMessage}"
            } finally {
                isFetchingNcertBooks.value = false
            }
        }
    }

    fun toggleBookmark(doc: DigitalMaterialEntity) {
        viewModelScope.launch {
            repository.toggleBookmarkMaterial(doc)
        }
    }

    
    fun recordFeePayment(
        student: StudentEntity,
        amount: Double,
        mode: String,
        purpose: String,
        refNum: String,
        discount: Double = 0.0,
        remarks: String = "",
        period: String = "",
        onReceiptGenerated: (PaymentEntity) -> Unit
    ) {
        viewModelScope.launch {
            val payment = repository.recordPayment(
                _currentLibraryId.value,
                student,
                amount,
                mode,
                purpose,
                refNum,
                discount = discount,
                remarks = remarks,
                period = period
            )
            _userMessage.value = "Payment recorded: ₹$amount for ${student.fullName}"
            onReceiptGenerated(payment)
        }
    }

    fun addExpense(category: String, amount: Double, description: String, mode: String) {
        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = UUID.randomUUID().toString(),
                libraryId = _currentLibraryId.value,
                category = category,
                amount = amount,
                date = getTodayDateString(),
                description = description,
                paymentMode = mode,
                status = "PAID"
            )
            repository.saveExpense(expense)
            _userMessage.value = "Expense logged: ₹$amount for $category"
        }
    }

    
    fun postNotice(title: String, content: String, category: String, priority: String) {
        viewModelScope.launch {
            val notice = NoticeEntity(
                id = UUID.randomUUID().toString(),
                libraryId = _currentLibraryId.value,
                title = title,
                content = content,
                category = category,
                priority = priority,
                date = getTodayDateString()
            )
            repository.saveNotice(notice)
            _userMessage.value = "Notice published to student notice boards!"
        }
    }

    fun submitStudentComplaint(subject: String, message: String, type: String) {
        viewModelScope.launch {
            val student = activeStudent.value
            val complaint = FeedbackComplaintEntity(
                id = UUID.randomUUID().toString(),
                libraryId = _currentLibraryId.value,
                studentId = student?.id ?: "ANONYMOUS",
                studentName = student?.fullName ?: "Library Student",
                seatNumber = student?.seatNumber ?: "",
                type = type,
                subject = subject,
                message = message,
                status = "PENDING",
                date = getTodayDateString()
            )
            repository.submitFeedback(complaint)
            _userMessage.value = "Feedback submitted to Library Manager!"
        }
    }

    fun replyToComplaint(complaint: FeedbackComplaintEntity, reply: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateFeedbackStatus(complaint, newStatus, reply)
            _userMessage.value = "Complaint updated to $newStatus"
        }
    }

    fun updateLibraryProfile(updatedLibrary: LibraryEntity, onError: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            if (_currentLibraryId.value.isBlank() || _currentLibraryId.value != updatedLibrary.id) {
                _currentLibraryId.value = updatedLibrary.id
            }
            repository.saveLibrary(updatedLibrary)
            if (updatedLibrary.ownerName.isNotBlank()) {
                _currentUserName.value = updatedLibrary.ownerName.trim()
            }
            if (updatedLibrary.ownerEmail.isNotBlank()) {
                _currentUserEmail.value = updatedLibrary.ownerEmail.trim().lowercase()
            } else if (updatedLibrary.email.isNotBlank()) {
                _currentUserEmail.value = updatedLibrary.email.trim().lowercase()
            }
            persistAuthSession(
                authenticated = _isAuthenticated.value,
                email = _currentUserEmail.value,
                name = _currentUserName.value,
                role = _currentRole.value,
                libraryId = updatedLibrary.id,
                studentId = _activeStudentId.value
            )
            val currentSession = SessionManager.sessionState.value
            if (currentSession != null) {
                val updatedSession = currentSession.copy(
                    name = _currentUserName.value.ifBlank { currentSession.name },
                    email = _currentUserEmail.value.ifBlank { currentSession.email },
                    libraryId = updatedLibrary.id
                )
                SessionManager.saveSession(getApplication(), updatedSession)
            }
            supabaseSyncManager.syncLocalToSupabase(updatedLibrary.id)
            _userMessage.value = "Library & Manager profile updated successfully!"
        }
    }

    fun updateStudentProfile(updatedStudent: StudentEntity) {
        viewModelScope.launch {
            repository.saveStudent(updatedStudent)
            if (_activeStudentId.value == updatedStudent.id) {
                _currentUserName.value = updatedStudent.fullName
                if (updatedStudent.email.isNotBlank()) {
                    _currentUserEmail.value = updatedStudent.email
                }
                persistAuthSession(
                    authenticated = _isAuthenticated.value,
                    email = updatedStudent.email.ifBlank { _currentUserEmail.value },
                    name = updatedStudent.fullName,
                    role = _currentRole.value,
                    libraryId = _currentLibraryId.value,
                    studentId = updatedStudent.id
                )
            }
            _userMessage.value = "Student profile updated successfully!"
        }
    }

    fun archiveStudent(student: StudentEntity, reason: String = "Left Library") {
        viewModelScope.launch {
            repository.archiveStudent(_currentLibraryId.value, student, reason)
            _userMessage.value = "Student ${student.fullName} moved to Archived Data. Details retained for future advertisement & records."
        }
    }

    fun reactivateStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.reactivateStudent(_currentLibraryId.value, student)
            _userMessage.value = "Student ${student.fullName} reactivated and restored to Active Members."
        }
    }

    fun archiveLibrary(libraryId: String, reason: String = "Deactivated") {
        viewModelScope.launch {
            repository.archiveLibrary(libraryId, reason)
            _userMessage.value = "Library moved to Archive. Details retained for future outreach & campaigns."
        }
    }

    fun reactivateLibrary(libraryId: String) {
        viewModelScope.launch {
            repository.reactivateLibrary(libraryId)
            _userMessage.value = "Library subscription reactivated successfully."
        }
    }

    fun closeOrArchiveCurrentAccount(reason: String = "User requested account closure") {
        viewModelScope.launch {
            if (_currentRole.value == "STUDENT") {
                val studentId = _activeStudentId.value
                val student = if (studentId.isNotBlank()) repository.getStudentById(studentId).firstOrNull() else null
                if (student != null) {
                    repository.archiveStudent(student.libraryId, student, reason)
                }
                logout()
                _userMessage.value = "Your account is closed. Your past academic records remain safely archived."
            } else {
                val libId = _currentLibraryId.value
                if (libId.isNotBlank()) {
                    repository.archiveLibrary(libId, reason)
                }
                logout()
                _userMessage.value = "Library account archived. All historical records are securely preserved."
            }
        }
    }

    
    fun testSupabaseConnection(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val (success, msg) = com.example.data.remote.SupabaseClient.testConnection()
            _supabaseStatusMessage.value = msg
            _userMessage.value = msg
            onResult(success, msg)
        }
    }

    fun syncLocalToSupabaseCloud() {
        viewModelScope.launch {
            _isSupabaseSyncing.value = true
            _supabaseStatusMessage.value = "Pushing local records to Supabase Cloud..."
            val (success, msg) = supabaseSyncManager.syncLocalToSupabase(_currentLibraryId.value)
            _isSupabaseSyncing.value = false
            _supabaseStatusMessage.value = msg
            _userMessage.value = msg
        }
    }

    fun pullFromSupabaseCloud() {
        viewModelScope.launch {
            _isSupabaseSyncing.value = true
            _supabaseStatusMessage.value = "Pulling records from Supabase Cloud..."
            val (success, msg) = supabaseSyncManager.pullSupabaseToLocal(_currentLibraryId.value)
            _isSupabaseSyncing.value = false
            _supabaseStatusMessage.value = msg
            _userMessage.value = msg
        }
    }

    
    fun saveSaasPlan(plan: SaaSSubscriptionPlanEntity) {
        viewModelScope.launch {
            repository.saveSaasPlan(plan)
            _userMessage.value = "SaaS Plan updated: ${plan.name} (₹${plan.price})"
        }
    }

    fun deleteSaasPlan(plan: SaaSSubscriptionPlanEntity) {
        viewModelScope.launch {
            repository.deleteSaasPlan(plan)
            _userMessage.value = "SaaS Plan deleted: ${plan.name}"
        }
    }

    fun saveLibrarySubscription(sub: LibrarySubscriptionEntity) {
        viewModelScope.launch {
            repository.saveLibrarySubscription(sub)
            _userMessage.value = "Subscription updated for ${sub.libraryName}"
        }
    }

    fun extendOrUpdateLibrarySubscription(
        libraryId: String,
        libraryName: String,
        plan: SubscriptionPlans,
        durationValue: Int,
        durationUnit: String = "MONTHS", // "MONTHS" or "DAYS"
        customPrice: Double,
        customDiscount: Double,
        status: String,
        maxSeats: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = sdf.format(cal.time)

            val (months, days) = if (durationUnit.equals("DAYS", ignoreCase = true)) {
                cal.add(Calendar.DAY_OF_YEAR, if (durationValue > 0) durationValue else 30)
                Pair((durationValue / 30).coerceAtLeast(1), durationValue)
            } else {
                val m = if (durationValue > 0) durationValue else plan.durationMonths
                cal.add(Calendar.MONTH, m)
                Pair(m, m * 30)
            }
            val expiryDate = sdf.format(cal.time)
            val durationLabel = if (durationUnit.equals("DAYS", ignoreCase = true)) "$durationValue Days" else "$months Mo"

            val updatedSub = LibrarySubscriptionEntity(
                id = "SUB-$libraryId",
                libraryId = libraryId,
                libraryName = libraryName,
                planId = plan.id,
                planName = "${plan.name} ($durationLabel)",
                status = status,
                startDate = startDate,
                expiryDate = expiryDate,
                durationDays = days,
                durationUnit = durationUnit,
                price = if (customPrice > 0) customPrice else plan.price,
                discount = customDiscount,
                maxSeats = if (maxSeats > 0) maxSeats else plan.maxSeats,
                notes = notes,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveLibrarySubscription(updatedSub)
            pushSubscriptionToSupabase(updatedSub)
            _userMessage.value = "Subscription activated for $libraryName until $expiryDate!"
        }
    }

    /**
     * Pushes the authoritative subscription record to Supabase so that a
     * live/realtime check (verifyLiveSubscriptionStatus, called from every
     * device on app open) can see it — Room alone is just a local cache and
     * was previously the ONLY source ever checked, which meant a library
     * could keep full access forever by simply never going online again
     * after an admin suspended/expired them.
     */
    private fun pushSubscriptionToSupabase(sub: LibrarySubscriptionEntity) {
        viewModelScope.launch {
            try {
                val json = org.json.JSONObject().apply {
                    put("id", sub.id)
                    put("libraryId", sub.libraryId)
                    put("libraryName", sub.libraryName)
                    put("planId", sub.planId)
                    put("planName", sub.planName)
                    put("status", sub.status)
                    put("startDate", sub.startDate)
                    put("expiryDate", sub.expiryDate)
                    put("price", sub.price)
                    put("discount", sub.discount)
                    put("maxSeats", sub.maxSeats)
                    put("notes", sub.notes)
                    put("updatedAt", sub.updatedAt)
                    put("subscription_active", sub.status.equals("ACTIVE", ignoreCase = true) || sub.status.equals("TRIAL", ignoreCase = true))
                }
                val array = org.json.JSONArray().put(json)
                com.example.data.remote.SupabaseClient.upsertRecords("library_subscriptions", array)
            } catch (e: Exception) {
                // Best-effort push; verifyLiveSubscriptionStatus fails closed
                // if this never lands, so we don't silently pretend it worked.
            }
        }
    }

    /**
     * Fresh, non-cached subscription check straight from Supabase via AuthGuardService.
     * This is the actual enforcement point: it verifies the 'subscription_active' flag
     * directly against the remote Supabase tables ('libraries' and 'library_subscriptions').
     * Fail-closed: only [Active] grants access to library features.
     */
    suspend fun verifyLiveSubscriptionStatus(libraryId: String): LiveSubscriptionCheck {
        return com.example.data.remote.AuthGuardService.verifyLibrarySubscription(libraryId)
    }

    fun toggleLibrarySuspension(libraryId: String, libraryName: String, isSuspended: Boolean) {
        viewModelScope.launch {
            val currentSub = repository.getSubscriptionDirect(libraryId)
            val newStatus = if (isSuspended) "SUSPENDED" else "ACTIVE"
            if (currentSub != null) {
                val updated = currentSub.copy(status = newStatus, updatedAt = System.currentTimeMillis())
                repository.saveLibrarySubscription(updated)
                pushSubscriptionToSupabase(updated)
            } else {
                // If not cached locally, attempt to update remote subscription directly in Supabase
                try {
                    val (success, records) = com.example.data.remote.SupabaseClient.fetchRecords("library_subscriptions", libraryId)
                    if (success && records != null && records.length() > 0) {
                        val subObj = records.getJSONObject(0)
                        subObj.put("status", newStatus)
                        subObj.put("subscription_active", !isSuspended)
                        subObj.put("updatedAt", System.currentTimeMillis())
                        com.example.data.remote.SupabaseClient.upsertRecords("library_subscriptions", org.json.JSONArray().put(subObj))
                    }
                } catch (e: Exception) {
                    // Best-effort remote update
                }
            }

            // Sync 'subscription_active' flag directly to Supabase libraries organization table
            try {
                val libUpdate = org.json.JSONObject().apply {
                    put("id", libraryId)
                    put("subscription_active", !isSuspended)
                }
                com.example.data.remote.SupabaseClient.upsertRecords("libraries", org.json.JSONArray().put(libUpdate))
            } catch (e: Exception) {
                // Best-effort push
            }

            _userMessage.value = if (isSuspended) "Library $libraryName has been SUSPENDED" else "Library $libraryName reactivated!"
        }
    }

    fun updateSuperAdminProfile(
        email: String,
        name: String,
        accessCode: String = "",
        is2Fa: Boolean = true,
        upiId: String = "",
        upiPayeeName: String = "",
        mobile: String = ""
    ) {
        viewModelScope.launch {
            val current = superAdminProfile.value
            val effectiveUpiId = if (upiId.isNotBlank()) upiId.trim() else current?.upiId ?: ""
            val effectivePayee = if (upiPayeeName.isNotBlank()) upiPayeeName.trim() else current?.upiPayeeName ?: (if (name.isNotBlank()) name.trim() else "")
            val effectiveMobile = if (mobile.isNotBlank()) mobile.trim() else current?.mobile ?: ""
            
            val updated = SuperAdminUserEntity(
                id = current?.id?.ifBlank { "SUPER-ADMIN-MASTER" } ?: "SUPER-ADMIN-MASTER",
                email = email.trim().lowercase(),
                name = name.trim(),
                mobile = effectiveMobile,
                accessCode = current?.accessCode ?: "",
                is2FaEnabled = is2Fa,
                isClaimed = true,
                upiId = effectiveUpiId,
                upiPayeeName = effectivePayee
            )
            repository.saveSuperAdmin(updated)
            // Synchronize UPI ID and Support WhatsApp number across all subscription plans in database
            repository.updateAllPlansUpi(effectiveUpiId, effectivePayee, effectiveMobile)
            if (name.isNotBlank()) _currentUserName.value = name.trim()
            if (email.isNotBlank()) _currentUserEmail.value = email.trim().lowercase()
            persistAuthSession(
                authenticated = _isAuthenticated.value,
                email = _currentUserEmail.value,
                name = _currentUserName.value,
                role = "SUPER_ADMIN",
                libraryId = "",
                studentId = ""
            )
            val currentSession = SessionManager.sessionState.value
            if (currentSession != null) {
                val updatedSession = currentSession.copy(
                    name = _currentUserName.value.ifBlank { currentSession.name },
                    email = _currentUserEmail.value.ifBlank { currentSession.email }
                )
                SessionManager.saveSession(getApplication(), updatedSession)
            }
            _userMessage.value = "Super Admin settings & UPI billing details updated across all plans!"
        }
    }

    fun updateSuperAdminUpi(upiId: String, upiPayeeName: String) {
        val trimmedUpi = upiId.trim()
        val trimmedPayee = upiPayeeName.trim().ifBlank { "LibDesk Subscriptions" }
        if (trimmedUpi.isBlank()) return
        viewModelScope.launch {
            val current = superAdminProfile.value
            val updated = (current ?: SuperAdminUserEntity()).copy(
                upiId = trimmedUpi,
                upiPayeeName = trimmedPayee
            )
            repository.saveSuperAdmin(updated)
            repository.updateAllPlansUpi(trimmedUpi, trimmedPayee)
            _userMessage.value = "Super Admin UPI ID updated to $trimmedUpi for all libraries!"
        }
    }

    fun broadcastSuperAdminMessage(
        title: String,
        content: String,
        targetAudience: String = "ALL", // "ALL", "LIBRARIES", "STUDENTS"
        priority: String = "NORMAL",
        targetLibraryId: String? = null,
        senderName: String = "LibDesk Super Admin"
    ) {
        viewModelScope.launch {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val notice = NoticeEntity(
                id = "NOTIF-${UUID.randomUUID().toString().take(8)}",
                libraryId = targetLibraryId ?: "GLOBAL",
                title = title.trim(),
                content = content.trim(),
                category = "BROADCAST",
                priority = priority,
                date = sdf.format(Date()),
                targetAudience = targetAudience,
                senderName = senderName,
                isActive = true
            )
            repository.saveNotice(notice)
            _userMessage.value = "Broadcast sent successfully to $targetAudience"
        }
    }

    fun deleteBroadcastNotice(notice: NoticeEntity) {
        viewModelScope.launch {
            repository.deleteNotice(notice)
            _userMessage.value = "Notification removed"
        }
    }

    // ==========================================
    // POMODORO FOCUS TIMER
    // ==========================================
    fun startPomodoro() {
        if (_isPomodoroRunning.value) return
        _isPomodoroRunning.value = true
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            while (_isPomodoroRunning.value && _pomodoroSecondsLeft.value > 0) {
                kotlinx.coroutines.delay(1000L)
                _pomodoroSecondsLeft.value = (_pomodoroSecondsLeft.value - 1).coerceAtLeast(0)
            }
            if (_pomodoroSecondsLeft.value <= 0) {
                _isPomodoroRunning.value = false
                val minutesSpent = _pomodoroSessionMinutes.value
                if (!_isPomodoroBreak.value) {
                    // Log study session for current student
                    val stId = _activeStudentId.value
                    val libId = _currentLibraryId.value
                    val stName = _currentUserName.value
                    if (stId.isNotBlank()) {
                        repository.logStudyMinutes(stId, libId, stName, minutesSpent)
                    }
                    _userMessage.value = "🎉 Great focus session! Time for a short 5-minute break."
                    switchPomodoroMode(isBreak = true)
                } else {
                    _userMessage.value = "⚡ Break finished! Ready for another focused study block?"
                    switchPomodoroMode(isBreak = false)
                }
            }
        }
    }

    fun pausePomodoro() {
        _isPomodoroRunning.value = false
        pomodoroJob?.cancel()
    }

    fun resetPomodoro() {
        pausePomodoro()
        _pomodoroSecondsLeft.value = if (_isPomodoroBreak.value) 5 * 60 else _pomodoroSessionMinutes.value * 60
    }

    fun switchPomodoroMode(isBreak: Boolean) {
        pausePomodoro()
        _isPomodoroBreak.value = isBreak
        _pomodoroSecondsLeft.value = if (isBreak) 5 * 60 else _pomodoroSessionMinutes.value * 60
    }

    fun setPomodoroDuration(minutes: Int) {
        pausePomodoro()
        _pomodoroSessionMinutes.value = minutes
        if (!_isPomodoroBreak.value) {
            _pomodoroSecondsLeft.value = minutes * 60
        }
    }

    // ==========================================
    // LOCKER ACTIONS
    // ==========================================
    fun saveLocker(locker: LockerEntity) {
        viewModelScope.launch {
            repository.saveLocker(locker)
            _userMessage.value = "Locker ${locker.lockerNumber} saved successfully"
        }
    }

    fun deleteLocker(lockerId: String) {
        viewModelScope.launch {
            repository.deleteLocker(lockerId)
            _userMessage.value = "Locker removed"
        }
    }

    fun allocateLocker(
        lockerId: String,
        studentId: String,
        studentName: String,
        studentPhone: String,
        expiryDate: String,
        keyNumber: String = ""
    ) {
        viewModelScope.launch {
            repository.allocateLocker(lockerId, studentId, studentName, studentPhone, expiryDate, keyNumber)
            _userMessage.value = "Locker assigned to $studentName"
        }
    }

    fun releaseLocker(lockerId: String) {
        viewModelScope.launch {
            repository.releaseLocker(lockerId)
            _userMessage.value = "Locker released & marked Available"
        }
    }

    // ==========================================
    // VISITOR / 1-DAY TRIAL PASS ACTIONS
    // ==========================================
    fun saveVisitorPass(pass: VisitorPassEntity) {
        viewModelScope.launch {
            repository.saveVisitorPass(pass)
            _userMessage.value = "1-Day Pass issued to ${pass.visitorName}"
        }
    }

    fun checkoutVisitorPass(passId: String) {
        viewModelScope.launch {
            repository.checkoutVisitorPass(passId)
            _userMessage.value = "Visitor checkout logged"
        }
    }

    // ==========================================
    // SEAT & SHIFT CHANGE REQUEST ACTIONS
    // ==========================================
    fun submitSeatShiftRequest(
        requestedSeatNumber: String = "",
        requestedShiftId: String = "",
        requestedShiftName: String = "",
        requestType: String = "SEAT_AND_SHIFT",
        reason: String
    ) {
        viewModelScope.launch {
            val student = currentStudent.value
            val req = SeatShiftRequestEntity(
                id = "REQ-${UUID.randomUUID().toString().take(8).uppercase()}",
                libraryId = _currentLibraryId.value,
                studentId = _activeStudentId.value,
                studentName = student?.fullName ?: _currentUserName.value,
                studentPhone = student?.mobile ?: "",
                currentSeatNumber = student?.seatNumber ?: "Unassigned",
                currentShiftName = student?.shiftName ?: "Standard Shift",
                requestedSeatNumber = requestedSeatNumber,
                requestedShiftId = requestedShiftId,
                requestedShiftName = requestedShiftName,
                requestType = requestType,
                reason = reason.trim(),
                status = "PENDING"
            )
            repository.submitSeatShiftRequest(req)
            _userMessage.value = "Seat/Shift change request submitted to library manager"
        }
    }

    fun approveSeatShiftRequest(requestId: String, remarks: String = "") {
        viewModelScope.launch {
            repository.resolveSeatShiftRequest(requestId, isApproved = true, adminRemarks = remarks)
            _userMessage.value = "Seat/Shift change request Approved & updated!"
        }
    }

    fun rejectSeatShiftRequest(requestId: String, remarks: String = "") {
        viewModelScope.launch {
            repository.resolveSeatShiftRequest(requestId, isApproved = false, adminRemarks = remarks)
            _userMessage.value = "Request Rejected"
        }
    }

    // ==========================================
    // INTERACTIVE FLOOR PLAN ACTIONS
    // ==========================================
    fun saveFloorElements(elements: List<FloorElementEntity>) {
        viewModelScope.launch {
            repository.saveFloorElements(elements)
            _userMessage.value = "Floor plan layout saved!"
        }
    }

    fun addFloorElement(element: FloorElementEntity) {
        viewModelScope.launch {
            repository.addFloorElement(element)
            _userMessage.value = "Added ${element.label} to floor layout"
        }
    }

    fun removeFloorElement(elementId: String) {
        viewModelScope.launch {
            repository.removeFloorElement(elementId)
            _userMessage.value = "Element removed from floor plan"
        }
    }

    // ==========================================
    // WI-FI & STUDENT TARGET EXAM ACTIONS
    // ==========================================
    fun rotateWifiPassword(newSsid: String, newPassword: String) {
        viewModelScope.launch {
            val lib = currentLibrary.value ?: return@launch
            val updated = lib.copy(
                wifiSsid = newSsid.trim(),
                wifiPassword = newPassword.trim(),
                wifiLastRotated = System.currentTimeMillis()
            )
            repository.saveLibrary(updated)
            _userMessage.value = "Library Wi-Fi credentials updated!"
        }
    }

    fun updateStudentTargetExam(studentId: String, targetExam: String, targetExamDate: String) {
        viewModelScope.launch {
            val currentSt = students.value.find { it.id == studentId } ?: activeStudent.value ?: return@launch
            val updated = currentSt.copy(
                targetExam = targetExam.trim(),
                targetExamDate = targetExamDate.trim()
            )
            repository.saveStudent(updated)
            _userMessage.value = "Target exam updated to $targetExam"
        }
    }

    // ==========================================
    // SUPABASE REALTIME & SIGNAL MESSAGING
    // ==========================================

    fun sendChatMessage(
        studentId: String,
        studentName: String,
        senderRole: String,
        senderName: String,
        messageText: String
    ) {
        if (messageText.isBlank()) return
        val libId = _currentLibraryId.value.ifBlank { "LIB-DEFAULT" }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now))
            val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(now))
            val msg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                libraryId = libId,
                studentId = studentId,
                studentName = studentName,
                senderRole = senderRole,
                senderName = senderName,
                message = messageText.trim(),
                timestamp = now,
                timeFormatted = timeFmt,
                dateFormatted = dateFmt,
                status = "SENT",
                isRead = false
            )
            repository.sendChatMessage(msg)
        }
    }

    fun markChatAsRead(studentId: String, readerRole: String) {
        viewModelScope.launch {
            repository.markChatMessagesAsRead(studentId, readerRole)
        }
    }

    fun startRealtimeChat() {
        val libId = _currentLibraryId.value
        if (libId.isNotBlank()) {
            com.example.data.remote.SupabaseRealtimeChatManager.startRealtimeSession(libId, repository)
        }
    }

    fun stopRealtimeChat() {
        com.example.data.remote.SupabaseRealtimeChatManager.stopRealtimeSession()
    }

    fun refreshChatMessages() {
        val libId = _currentLibraryId.value
        if (libId.isNotBlank()) {
            viewModelScope.launch {
                com.example.data.remote.SupabaseRealtimeChatManager.syncMessagesFromSupabase(libId, repository)
            }
        }
    }

    companion object {
        private const val KEY_IS_AUTHENTICATED = "key_is_authenticated"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_LIBRARY_ID = "key_library_id"
        private const val KEY_ROLE = "key_role"
        private const val KEY_ACTIVE_STUDENT_ID = "key_active_student_id"
        private const val KEY_DARK_MODE = "key_dark_mode"
        private const val KEY_AMOLED_MODE = "key_amoled_mode"
        private const val KEY_BIOMETRIC = "key_biometric"
    }
}

