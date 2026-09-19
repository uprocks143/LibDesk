package com.example.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.*
import com.example.ui.components.DynamicEntranceQrCard
import com.example.ui.components.LogoutConfirmationDialog
import com.example.ui.components.MembershipStatusCard
import com.example.ui.components.OfflineCacheStatusBanner
import com.example.ui.components.QrCodeView
import com.example.ui.components.calculateMembershipExpiration
import com.example.ui.theme.*

/**
 * High-fidelity Profile Screen displaying:
 * 1. User details & verification status
 * 2. Real-time Membership Status & validity countdown
 * 3. Active Bookings (Seat reservation, study shift schedule & library book checkouts)
 * 4. Digital ID Card View for Turnstile / Gate Access Control
 * 5. Secure Session Management with Logout Confirmation Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    student: StudentEntity?,
    library: LibraryEntity?,
    seats: List<SeatEntity> = emptyList(),
    shifts: List<ShiftEntity> = emptyList(),
    plans: List<MembershipPlanEntity> = emptyList(),
    halls: List<HallEntity> = emptyList(),
    bookIssues: List<BookIssueEntity> = emptyList(),
    cachedUserBooking: UserBookingCacheEntity? = null,
    isDarkMode: Boolean = false,
    onToggleDarkMode: (() -> Unit)? = null,
    onOpenEditProfile: () -> Unit = {},
    onViewFullIdCard: () -> Unit = {},
    onRenewMembership: () -> Unit = {},
    onOpenQrScanner: () -> Unit = {},
    onPerformCheckIn: () -> Unit = {},
    onTriggerCheckInAnimation: () -> Unit = {},
    onRequestLogout: () -> Unit = {},
    onChangeStudent: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var showExpandedGatePassModal by remember { mutableStateOf(false) }
    var selectedSectionFilter by remember { mutableStateOf(0) } // 0: All, 1: Membership, 2: Bookings, 3: ID Pass

    // Offline First: If live student is null but Room DB cache is available, construct effective student
    val effectiveStudent = student ?: cachedUserBooking?.let { cache ->
        StudentEntity(
            id = cache.studentId,
            libraryId = cache.libraryId,
            studentCode = cache.studentCode,
            fullName = cache.fullName,
            email = cache.email,
            mobile = cache.phone,
            address = cache.libraryAddress,
            gender = "Student Member",
            dob = "",
            joiningDate = cache.startDate,
            seatId = cache.seatId,
            seatNumber = cache.seatNumber,
            hallName = cache.hallName,
            shiftId = cache.shiftId,
            shiftName = cache.shiftName,
            planId = cache.planId,
            planName = cache.planName,
            status = cache.membershipStatus,
            expiryDate = cache.expiryDate,
            rfidQrCode = cache.rfidQrCode,
            totalFee = 0.0,
            paidAmount = 0.0,
            dueAmount = 0.0,
            emergencyContact = "",
            courseClass = "Library Scholar",
            targetExam = "Study Hub Member"
        )
    }

    if (onClose != null) {
        BackHandler { onClose() }
    }

    // Confirmation dialog before logging out to prevent accidental session termination
    LogoutConfirmationDialog(
        show = showLogoutConfirmation,
        userName = effectiveStudent?.fullName ?: "Student Member",
        onConfirmLogout = {
            showLogoutConfirmation = false
            onRequestLogout()
        },
        onDismiss = { showLogoutConfirmation = false }
    )

    if (showExpandedGatePassModal && effectiveStudent != null) {
        ExpandedAccessPassModal(
            student = effectiveStudent,
            library = library,
            seats = seats,
            shifts = shifts,
            onPerformCheckIn = onPerformCheckIn,
            onClose = { showExpandedGatePassModal = false }
        )
    }

    if (effectiveStudent == null) {
        EmptyProfilePlaceholder(
            onRequestLogout = onRequestLogout,
            onClose = onClose,
            modifier = modifier
        )
        return
    }

    val assignedSeat = remember(seats, effectiveStudent) {
        seats.find { it.id == effectiveStudent.seatId }
            ?: seats.find { it.seatNumber.equals(effectiveStudent.seatNumber, ignoreCase = true) }
            ?: seats.find { it.assignedStudentId == effectiveStudent.id }
    }

    val assignedShift = remember(shifts, effectiveStudent) {
        shifts.find { it.id == effectiveStudent.shiftId }
            ?: shifts.find { it.name.equals(effectiveStudent.shiftName, ignoreCase = true) }
    }

    val assignedPlan = remember(plans, effectiveStudent) {
        plans.find { it.id == effectiveStudent.planId }
            ?: plans.find { it.name.equals(effectiveStudent.planName, ignoreCase = true) }
    }

    val assignedHall = remember(halls, assignedSeat) {
        halls.find { it.id == assignedSeat?.hallId }
            ?: halls.find { it.name.equals(assignedSeat?.hallName, ignoreCase = true) }
    }

    val activeBookIssues = remember(bookIssues, effectiveStudent) {
        bookIssues.filter {
            (it.studentId == effectiveStudent.id || it.studentName.equals(effectiveStudent.fullName, ignoreCase = true)) &&
                    (it.status.equals("ISSUED", ignoreCase = true) || it.status.equals("ACTIVE", ignoreCase = true))
        }
    }

    val membershipCalc = remember(effectiveStudent.joiningDate, effectiveStudent.expiryDate, effectiveStudent.status) {
        calculateMembershipExpiration(effectiveStudent.joiningDate, effectiveStudent.expiryDate, effectiveStudent.status)
    }

    val isMembershipActive = effectiveStudent.status.equals("ACTIVE", ignoreCase = true)
    val hasDues = effectiveStudent.dueAmount > 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Member Profile",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = library?.name ?: "LibDesk Study Network",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("profile_screen_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenEditProfile,
                        modifier = Modifier.testTag("profile_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = { showLogoutConfirmation = true },
                        modifier = Modifier.testTag("profile_top_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Identity & KYC Card
            item {
                ProfileHeroCard(
                    student = effectiveStudent,
                    isMembershipActive = isMembershipActive,
                    onOpenEditProfile = onOpenEditProfile,
                    onViewFullIdCard = onViewFullIdCard,
                    onChangeStudent = onChangeStudent
                )
            }

            // Offline Room Database Local Cache Status Banner
            item {
                OfflineCacheStatusBanner(
                    cachedBooking = cachedUserBooking,
                    isOfflineMode = student == null && cachedUserBooking != null,
                    onTriggerCheckInAnimation = onTriggerCheckInAnimation
                )
            }

            // Quick Category Filter Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("All Overview", "Membership", "Active Bookings", "Dynamic QR Pass")
                    tabs.forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedSectionFilter == index,
                            onClick = { selectedSectionFilter = index },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedSectionFilter == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 2. User Membership Status Section
            if (selectedSectionFilter == 0 || selectedSectionFilter == 1) {
                item {
                    SectionHeader(
                        title = "MEMBERSHIP STATUS",
                        subtitle = "Active subscription & billing details",
                        icon = Icons.Default.VerifiedUser
                    )
                }

                item {
                    MembershipStatusCard(
                        student = effectiveStudent,
                        onRenew = onRenewMembership,
                        onPayDues = onRenewMembership,
                        showActions = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    MembershipDetailsCard(
                        student = effectiveStudent,
                        assignedPlan = assignedPlan,
                        hasDues = hasDues,
                        onRenewMembership = onRenewMembership
                    )
                }
            }

            // 3. Active Bookings Section (Seat, Shift & Book Issue reservations)
            if (selectedSectionFilter == 0 || selectedSectionFilter == 2) {
                item {
                    SectionHeader(
                        title = "ACTIVE BOOKINGS & RESERVATIONS",
                        subtitle = "Assigned study desk, shift timings & library checkouts",
                        icon = Icons.Default.EventSeat
                    )
                }

                item {
                    ActiveSeatBookingCard(
                        student = effectiveStudent,
                        assignedSeat = assignedSeat,
                        assignedShift = assignedShift,
                        assignedHall = assignedHall,
                        cachedUserBooking = cachedUserBooking,
                        onPerformCheckIn = onPerformCheckIn,
                        onTriggerCheckInAnimation = onTriggerCheckInAnimation,
                        onOpenQrScanner = onOpenQrScanner
                    )
                }

                item {
                    ActiveBookLoansCard(
                        bookIssues = activeBookIssues
                    )
                }
            }

            // 4. Dynamic Entrance QR Pass for Turnstile Access Control
            if (selectedSectionFilter == 0 || selectedSectionFilter == 3) {
                item {
                    SectionHeader(
                        title = "DYNAMIC ENTRANCE QR PASS",
                        subtitle = "Dynamic rolling pass for turnstile entrance & seat verification",
                        icon = Icons.Default.QrCode2
                    )
                }

                item {
                    DynamicEntranceQrCard(
                        student = effectiveStudent,
                        library = library,
                        assignedSeat = assignedSeat,
                        assignedShift = assignedShift,
                        cachedUserBooking = cachedUserBooking,
                        onPresentPass = { showExpandedGatePassModal = true },
                        onPerformCheckIn = onPerformCheckIn,
                        onTriggerCheckInAnimation = onTriggerCheckInAnimation,
                        onShareId = {
                            val shareText = """
                                LIBDESK ENTRANCE GATE PASS
                                Library: ${library?.name ?: "LibDesk"}
                                Member: ${effectiveStudent.fullName}
                                Member ID: ${effectiveStudent.studentCode}
                                Assigned Seat: ${effectiveStudent.seatNumber.ifBlank { "Desk Pass" }}
                                Shift: ${effectiveStudent.shiftName}
                                Valid Until: ${effectiveStudent.expiryDate.ifBlank { "Active" }}
                            """.trimIndent()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Dynamic Access Pass"))
                        }
                    )
                }
            }

            // 5. Account Security & Logout Section
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Account Security & Session",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Manage your login session on this mobile device",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        if (onToggleDarkMode != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onToggleDarkMode() }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                            contentDescription = null,
                                            tint = if (isDarkMode) LibDeskColors.warning else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isDarkMode) "Oxford Midnight (Dark)" else "Classic Oxford Navy (Light)",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isDarkMode) "Tap to switch to Oxford Light" else "Tap to switch to Oxford Dark",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isDarkMode,
                                    onCheckedChange = { onToggleDarkMode() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = LibDeskColors.warning,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }

                        Button(
                            onClick = { showLogoutConfirmation = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign Out of LibDesk",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "LibDesk Access Control v2.4 • Offline Encrypted Session",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Profile Hero Card with avatar, verification status, and quick shortcuts
 */
@Composable
private fun ProfileHeroCard(
    student: StudentEntity,
    isMembershipActive: Boolean,
    onOpenEditProfile: () -> Unit,
    onViewFullIdCard: () -> Unit,
    onChangeStudent: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dual-ring Avatar with initials
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primary))),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = student.fullName.trim().split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .joinToString("")
                        .ifEmpty { "ST" }
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.4).sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMembershipActive) LibDeskColors.successSoft else MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(
                                1.dp,
                                if (isMembershipActive) LibDeskColors.success.copy(alpha = 0.4f) else MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = student.status.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isMembershipActive) LibDeskColors.success else MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "CODE: ${student.studentCode}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = "• ${student.courseClass.ifBlank { "Registered Student" }}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (student.targetExam.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Target: ${student.targetExam}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Contact details snippet row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoPill(
                    icon = Icons.Default.Phone,
                    text = student.mobile.ifBlank { "No Mobile" },
                    modifier = Modifier.weight(1f)
                )
                InfoPill(
                    icon = Icons.Default.Email,
                    text = student.email.ifBlank { "No Email" },
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenEditProfile,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Info", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onViewFullIdCard,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Digital Pass", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Member ID", student.studentCode)
                        clipboard.setPrimaryClip(clip)
                        // Toast.makeText(context, "Student ID copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy ID",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Membership details card breaking down fees, expiration, and admission date
 */
@Composable
private fun MembershipDetailsCard(
    student: StudentEntity,
    assignedPlan: MembershipPlanEntity?,
    hasDues: Boolean,
    onRenewMembership: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MEMBERSHIP PLAN & DURATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = student.planName.ifBlank { assignedPlan?.name ?: "Standard Desk Plan" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Breakdown grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricItem(
                    label = "DATE OF ADMISSION",
                    value = student.joiningDate.ifBlank { "Not Recorded" },
                    icon = Icons.Default.CalendarToday,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "EXPIRES ON",
                    value = student.expiryDate.ifBlank { "Active / Ongoing" },
                    icon = Icons.Default.Event,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricItem(
                    label = "TOTAL FEE",
                    value = "₹${student.totalFee.toInt()}",
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "PAID AMOUNT",
                    value = "₹${student.paidAmount.toInt()}",
                    valueColor = LibDeskColors.success,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "DUE BALANCE",
                    value = "₹${student.dueAmount.toInt()}",
                    valueColor = if (hasDues) MaterialTheme.colorScheme.error else LibDeskColors.success,
                    icon = Icons.Default.ReceiptLong,
                    modifier = Modifier.weight(1f)
                )
            }

            if (hasDues) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pending Fee Balance: ₹${student.dueAmount.toInt()}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Clear fee dues to guarantee uninterrupted seat access",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onRenewMembership,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Deposit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Active Seat Booking Card detailing reserved desk, study hall, and daily shift hours
 */
@Composable
private fun ActiveSeatBookingCard(
    student: StudentEntity,
    assignedSeat: SeatEntity?,
    assignedShift: ShiftEntity?,
    assignedHall: HallEntity?,
    cachedUserBooking: UserBookingCacheEntity? = null,
    onPerformCheckIn: () -> Unit = {},
    onTriggerCheckInAnimation: () -> Unit = {},
    onOpenQrScanner: () -> Unit
) {
    val isSeatConfirmed = student.seatNumber.isNotBlank() || assignedSeat != null
    val isCheckedInToday = cachedUserBooking?.isCheckedIn == true

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventSeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "RESERVED STUDY DESK",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCheckedInToday) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LibDeskColors.successSoft
                        ) {
                            Text(
                                text = "CHECKED IN",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LibDeskColors.success,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSeatConfirmed) LibDeskColors.successSoft else LibDeskColors.warningSoft
                    ) {
                        Text(
                            text = if (isSeatConfirmed) "CONFIRMED" else "UNASSIGNED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSeatConfirmed) LibDeskColors.success else LibDeskColors.warning,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Prominent Desk Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chair,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (student.seatNumber.isNotBlank()) "Desk #${student.seatNumber}" else (assignedSeat?.seatNumber?.let { "Desk #$it" } ?: "Seat Not Assigned"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = assignedSeat?.seatType ?: "Reserved AC Study Desk",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Live Access Pulse Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isSeatConfirmed) LibDeskColors.success else Color.Gray)
                        )
                        Text(
                            text = if (isSeatConfirmed) "Active" else "Pending",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSeatConfirmed) LibDeskColors.success else Color.Gray
                        )
                    }
                }
            }

            // Hall and Shift Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricItem(
                    label = "STUDY HALL",
                    value = assignedSeat?.hallName ?: assignedHall?.name ?: "Main Study Hall",
                    icon = Icons.Default.MeetingRoom,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "FLOOR LEVEL",
                    value = assignedSeat?.floor ?: assignedHall?.floor ?: "Ground Floor",
                    icon = Icons.Default.Layers,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricItem(
                    label = "SHIFT NAME",
                    value = student.shiftName.ifBlank { assignedShift?.name ?: "Full Day Shift" },
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "DAILY TIMINGS",
                    value = "${assignedShift?.startTime ?: "07:00 AM"} – ${assignedShift?.endTime ?: "11:00 PM"}",
                    icon = Icons.Default.AccessTime,
                    modifier = Modifier.weight(1f)
                )
            }

            // Desk Check-In Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPerformCheckIn,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("instant_seat_check_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Check In to Desk", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }

                OutlinedButton(
                    onClick = onOpenQrScanner,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("scan_gate_attendance_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scanner", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

/**
 * Active Library Book Loans / Checkouts
 */
@Composable
private fun ActiveBookLoansCard(
    bookIssues: List<BookIssueEntity>
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "LIBRARY BOOK RESERVATIONS",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = 0.4.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${bookIssues.size} Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (bookIssues.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "No books currently checked out. You can reserve reading material from the library catalog tab.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bookIssues.forEach { issue ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = issue.bookTitle,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Issued: ${issue.issueDate} • Due: ${issue.dueDate}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LibDeskColors.warningSoft
                                ) {
                                    Text(
                                        text = "Due ${issue.dueDate}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LibDeskColors.warning,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Digital ID Card View for Turnstile / Gate Access Control
 */
@Composable
private fun DigitalAccessPassCard(
    student: StudentEntity,
    library: LibraryEntity?,
    assignedSeat: SeatEntity?,
    assignedShift: ShiftEntity?,
    onPresentPass: () -> Unit,
    onShareId: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("digital_id_card_view")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Ribbon: Library Branding
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primary)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = library?.name ?: "LIBDESK STUDY SUITE",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "SECURE ACCESS CONTROL PASS",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RFID ACTIVE",
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ID Pass Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Student Name and Roll code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Member ID: ${student.studentCode} • Roll: ${student.id.takeLast(6).uppercase()}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Grid of Access Parameters
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PassField(
                                label = "ASSIGNED SEAT",
                                value = if (student.seatNumber.isNotBlank()) "Seat ${student.seatNumber}" else "Open Access",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            PassField(
                                label = "DAILY SHIFT",
                                value = student.shiftName.ifBlank { "Full Day" },
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PassField(
                                label = "HALL / SECTION",
                                value = assignedSeat?.hallName ?: "Main Study Hall",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            PassField(
                                label = "VALID UNTIL",
                                value = student.expiryDate.ifBlank { "Active" },
                                color = LibDeskColors.success
                            )
                        }
                    }
                }

                // Turnstile QR Code for scanning
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QrCodeView(
                        data = student.rfidQrCode.ifEmpty { student.studentCode },
                        size = 135.dp
                    )
                    Text(
                        text = "HOLD NEAR TURNSTILE OR GATE SCANNER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = 0.6.sp
                    )
                }

                // Card actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPresentPass,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("present_gate_pass_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Present to Gate", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onShareId,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Pass", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen / Expanded Pass Modal for high-contrast presentation at gate sensors
 */
@Composable
private fun ExpandedAccessPassModal(
    student: StudentEntity,
    library: LibraryEntity?,
    seats: List<SeatEntity>,
    shifts: List<ShiftEntity>,
    onPerformCheckIn: () -> Unit = {},
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            color = MaterialTheme.colorScheme.primary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top close bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = library?.name ?: "LIBDESK",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "TURNSTILE GATE PASS",
                            color = LibDeskColors.warningSoft,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Giant Scannable Gate Pass Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = student.fullName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "STUDENT ID: ${student.studentCode}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Giant QR
                        QrCodeView(
                            data = student.rfidQrCode.ifEmpty { student.studentCode },
                            size = 180.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("ASSIGNED SEAT", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (student.seatNumber.isNotBlank()) "Desk #${student.seatNumber}" else "Open Access",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SHIFT TIMINGS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = student.shiftName.ifBlank { "Regular Shift" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Instruction footer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = LibDeskColors.warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Hold screen within 5cm of turnstile reader",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            onPerformCheckIn()
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LibDeskColors.success),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Punch Entrance Check-in", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done Scanning", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Reusable Section Header with icon and descriptive subtitle
 */
@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                letterSpacing = 0.5.sp
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoPill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    icon: ImageVector? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PassField(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 0.4.sp
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyProfilePlaceholder(
    onRequestLogout: () -> Unit,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "No Student Profile Linked",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "Your account isn't linked to a student record yet. Please contact your library's front desk to get this fixed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onRequestLogout,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }

                if (onClose != null) {
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
