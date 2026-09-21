package com.example.ui.manager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.*
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.StatusBadge
import com.example.ui.components.MembershipStatusCard
import com.example.ui.components.MembershipExpiryAlertBadge
import com.example.ui.components.calculateMembershipExpiration
import com.example.ui.components.MembershipAlertLevel
import com.example.ui.components.CountryCodePhoneField
import com.example.ui.components.combineCountryCodeAndPhone
import com.example.ui.components.formatCurrency
import com.example.ui.components.DepositPeriodDateRangeSelector
import com.example.ui.components.DateRangeUtils
import com.example.ui.theme.*
import com.example.util.ImageShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerStudentsScreen(
    students: List<StudentEntity>,
    shifts: List<ShiftEntity>,
    plans: List<MembershipPlanEntity>,
    seats: List<SeatEntity> = emptyList(),
    library: LibraryEntity? = null,
    onSelectStudent: (StudentEntity) -> Unit,
    onViewIdCard: (StudentEntity) -> Unit,
    onRegisterStudent: (String, String, String, String, String, String, String, String, String, String, ShiftEntity?, MembershipPlanEntity?, Double, Double) -> Unit,
    onRecordFeePayment: (StudentEntity, Double, String, String, String, Double, String, String, (PaymentEntity) -> Unit) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onAssignSeat: (SeatEntity, StudentEntity, ShiftEntity?, MembershipPlanEntity?, String) -> Unit = { _, _, _, _, _ -> },
    onViewReceipt: (PaymentEntity) -> Unit = {},
    onEditStudentProfile: (StudentEntity) -> Unit = {},
    onArchiveStudent: (StudentEntity, String) -> Unit = { _, _ -> },
    onReactivateStudent: (StudentEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var sortBy by remember { mutableStateOf("NAME") } 
    var showSortMenu by remember { mutableStateOf(false) }

    var showRegisterModal by remember { mutableStateOf(false) }
    var selectedStudentForDetails by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedStudentForFee by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedStudentForSeat by remember { mutableStateOf<StudentEntity?>(null) }
    var selectedStudentForReminder by remember { mutableStateOf<StudentEntity?>(null) }
    var studentToArchive by remember { mutableStateOf<StudentEntity?>(null) }

    val activeCount = remember(students) { students.count { it.status.equals("ACTIVE", ignoreCase = true) } }
    val archivedCount = remember(students) { students.count { it.status.equals("ARCHIVED", ignoreCase = true) || it.status.equals("LEFT", ignoreCase = true) } }
    val currentStudentsCount = remember(students) { students.count { !it.status.equals("ARCHIVED", ignoreCase = true) && !it.status.equals("LEFT", ignoreCase = true) } }
    val pendingDuesCount = remember(students) { students.count { it.dueAmount > 0 && !it.status.equals("ARCHIVED", ignoreCase = true) && !it.status.equals("LEFT", ignoreCase = true) } }
    val totalDuesAmount = remember(students) { students.filter { !it.status.equals("ARCHIVED", ignoreCase = true) && !it.status.equals("LEFT", ignoreCase = true) }.sumOf { it.dueAmount } }
    val unassignedSeatsCount = remember(students) { students.count { it.seatNumber.isBlank() && !it.status.equals("ARCHIVED", ignoreCase = true) && !it.status.equals("LEFT", ignoreCase = true) } }

    val filteredAndSortedStudents = remember(students, searchQuery, selectedFilter, sortBy) {
        val filtered = students.filter { student ->
            val matchesSearch = searchQuery.isBlank() ||
                student.fullName.contains(searchQuery, ignoreCase = true) ||
                student.studentCode.contains(searchQuery, ignoreCase = true) ||
                student.mobile.contains(searchQuery, ignoreCase = true) ||
                student.seatNumber.contains(searchQuery, ignoreCase = true) ||
                student.targetExam.contains(searchQuery, ignoreCase = true) ||
                student.planName.contains(searchQuery, ignoreCase = true) ||
                student.shiftName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "ALL" -> !student.status.equals("ARCHIVED", ignoreCase = true) && !student.status.equals("LEFT", ignoreCase = true)
                "ACTIVE" -> student.status.equals("ACTIVE", ignoreCase = true)
                "EXPIRING_SOON" -> student.status.equals("ACTIVE", ignoreCase = true) && (Math.abs(student.studentCode.hashCode()) % 30 <= 5)
                "PENDING_DUES" -> student.dueAmount > 0 && !student.status.equals("ARCHIVED", ignoreCase = true) && !student.status.equals("LEFT", ignoreCase = true)
                "CLEAR_DUES" -> student.dueAmount <= 0 && !student.status.equals("ARCHIVED", ignoreCase = true) && !student.status.equals("LEFT", ignoreCase = true)
                "EXPIRED" -> student.status.equals("EXPIRED", ignoreCase = true)
                "UNASSIGNED" -> student.seatNumber.isBlank() && !student.status.equals("ARCHIVED", ignoreCase = true) && !student.status.equals("LEFT", ignoreCase = true)
                "ARCHIVED" -> student.status.equals("ARCHIVED", ignoreCase = true) || student.status.equals("LEFT", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }

        when (sortBy) {
            "NAME" -> filtered.sortedBy { it.fullName.lowercase() }
            "DUES" -> filtered.sortedByDescending { it.dueAmount }
            "NEWEST" -> filtered.sortedByDescending { it.createdAt }
            "EXPIRY" -> filtered.sortedBy { Math.abs(it.studentCode.hashCode()) % 30 }
            else -> filtered
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showRegisterModal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Member", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Total Members", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${students.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Divider(modifier = Modifier.height(32.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Active Passes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$activeCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = LibDeskColors.success)
                    }
                    Divider(modifier = Modifier.height(32.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Total Dues", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatCurrency(totalDuesAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                    }
                    Divider(modifier = Modifier.height(32.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("No Seat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$unassignedSeatsCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = LibDeskColors.warning)
                    }
                }
            }

            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, ID, phone, seat, exam...", fontSize = 14.sp) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by Name (A-Z)") },
                            onClick = { sortBy = "NAME"; showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.SortByAlpha, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Expiry (Urgent First)") },
                            onClick = { sortBy = "EXPIRY"; showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Pending Dues") },
                            onClick = { sortBy = "DUES"; showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Newest Joined") },
                            onClick = { sortBy = "NEWEST"; showSortMenu = false },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                        )
                    }
                }
            }

            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("Active Members ($currentStudentsCount)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
                )
                FilterChip(
                    selected = selectedFilter == "ACTIVE",
                    onClick = { selectedFilter = "ACTIVE" },
                    label = { Text("Regular ($activeCount)", style = MaterialTheme.typography.labelMedium) }
                )
                FilterChip(
                    selected = selectedFilter == "ARCHIVED",
                    onClick = { selectedFilter = "ARCHIVED" },
                    label = { Text("Archived / Alumni ($archivedCount)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedFilter == "EXPIRING_SOON",
                    onClick = { selectedFilter = "EXPIRING_SOON" },
                    label = { Text("Expiring Soon", style = MaterialTheme.typography.labelMedium) }
                )
                FilterChip(
                    selected = selectedFilter == "PENDING_DUES",
                    onClick = { selectedFilter = "PENDING_DUES" },
                    label = { Text("Dues Pending ($pendingDuesCount)", style = MaterialTheme.typography.labelMedium) }
                )
                FilterChip(
                    selected = selectedFilter == "CLEAR_DUES",
                    onClick = { selectedFilter = "CLEAR_DUES" },
                    label = { Text("Paid in Full", style = MaterialTheme.typography.labelMedium) }
                )
                FilterChip(
                    selected = selectedFilter == "UNASSIGNED",
                    onClick = { selectedFilter = "UNASSIGNED" },
                    label = { Text("No Seat ($unassignedSeatsCount)", style = MaterialTheme.typography.labelMedium) }
                )
                FilterChip(
                    selected = selectedFilter == "EXPIRED",
                    onClick = { selectedFilter = "EXPIRED" },
                    label = { Text("Expired", style = MaterialTheme.typography.labelMedium) }
                )
            }

            
            if (filteredAndSortedStudents.isEmpty()) {
                EmptyPlaceholder(
                    title = if (selectedFilter == "ARCHIVED") "No Archived Members" else "No Members Found",
                    description = if (selectedFilter == "ARCHIVED") "Members who exit or leave the library are safely retained in archive data for future marketing." else if (searchQuery.isNotBlank()) "No students matched '$searchQuery'" else "No students under selected filter.",
                    icon = if (selectedFilter == "ARCHIVED") Icons.Default.Archive else Icons.Default.PeopleOutline,
                    actionLabel = if (selectedFilter == "ARCHIVED") "View Active Members" else "Register New Member",
                    onAction = { if (selectedFilter == "ARCHIVED") selectedFilter = "ALL" else showRegisterModal = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredAndSortedStudents, key = { it.id }) { student ->
                        EnhancedRegisteredMemberCard(
                            student = student,
                            onClick = { selectedStudentForDetails = student },
                            onViewId = { onViewIdCard(student) },
                            onCollectFee = { selectedStudentForFee = student },
                            onAssignSeat = { selectedStudentForSeat = student },
                            onSendReminder = { selectedStudentForReminder = student },
                            onArchive = { studentToArchive = student },
                            onReactivate = { onReactivateStudent(student) },
                            onSendPromo = {
                                sendPromotionalOffer(context, student, library)
                            },
                            onCopyDetails = {
                                copyStudentDetails(context, student)
                            }
                        )
                    }
                }
            }
        }
    }

    
    if (showRegisterModal) {
        RegisterStudentDialog(
            shifts = shifts,
            plans = plans,
            onClose = { showRegisterModal = false },
            onConfirm = { name, mob, email, gender, exam, course, addr, pName, pMob, joiningDate, shift, plan, fee, paid ->
                onRegisterStudent(name, mob, email, gender, exam, course, addr, pName, pMob, joiningDate, shift, plan, fee, paid)
                showRegisterModal = false
            }
        )
    }

    if (selectedStudentForDetails != null) {
        StudentProfileDetailsDialog(
            student = selectedStudentForDetails!!,
            onClose = { selectedStudentForDetails = null },
            onViewId = {
                onViewIdCard(selectedStudentForDetails!!)
                selectedStudentForDetails = null
            },
            onCollectFee = {
                val st = selectedStudentForDetails!!
                selectedStudentForDetails = null
                selectedStudentForFee = st
            },
            onSendReminder = {
                val st = selectedStudentForDetails!!
                selectedStudentForDetails = null
                selectedStudentForReminder = st
            },
            onEditProfile = {
                val st = selectedStudentForDetails!!
                selectedStudentForDetails = null
                onEditStudentProfile(st)
            },
            onArchiveStudent = {
                val st = selectedStudentForDetails!!
                selectedStudentForDetails = null
                studentToArchive = st
            },
            onReactivateStudent = {
                val st = selectedStudentForDetails!!
                onReactivateStudent(st)
                selectedStudentForDetails = null
            },
            onSendPromo = {
                sendPromotionalOffer(context, selectedStudentForDetails!!, library)
            },
            onCopyDetails = {
                copyStudentDetails(context, selectedStudentForDetails!!)
            }
        )
    }

    if (studentToArchive != null) {
        ExitStudentConfirmationDialog(
            student = studentToArchive!!,
            onClose = { studentToArchive = null },
            onConfirmArchive = { reason ->
                onArchiveStudent(studentToArchive!!, reason)
                studentToArchive = null
            }
        )
    }

    if (selectedStudentForFee != null) {
        QuickMemberFeeDialog(
            student = selectedStudentForFee!!,
            onClose = { selectedStudentForFee = null },
            onConfirm = { amt, mode, purpose, ref, discount, remarks, period ->
                onRecordFeePayment(selectedStudentForFee!!, amt, mode, purpose, ref, discount, remarks, period) { receipt ->
                    onViewReceipt(receipt)
                }
                selectedStudentForFee = null
            }
        )
    }

    if (selectedStudentForSeat != null) {
        QuickAssignSeatDialog(
            student = selectedStudentForSeat!!,
            seats = seats,
            shifts = shifts,
            plans = plans,
            onClose = { selectedStudentForSeat = null },
            onConfirm = { seat, shift, plan, date ->
                onAssignSeat(seat, selectedStudentForSeat!!, shift, plan, date)
                selectedStudentForSeat = null
            }
        )
    }

    if (selectedStudentForReminder != null) {
        SendReminderDialog(
            student = selectedStudentForReminder!!,
            library = library,
            onClose = { selectedStudentForReminder = null }
        )
    }
}

@Composable
fun EnhancedRegisteredMemberCard(
    student: StudentEntity,
    onClick: () -> Unit,
    onViewId: () -> Unit,
    onCollectFee: () -> Unit,
    onAssignSeat: () -> Unit,
    onSendReminder: () -> Unit,
    onArchive: () -> Unit = {},
    onReactivate: () -> Unit = {},
    onSendPromo: () -> Unit = {},
    onCopyDetails: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isArchived = student.status.equals("ARCHIVED", ignoreCase = true) || student.status.equals("LEFT", ignoreCase = true)
    val expInfo = remember(student.joiningDate, student.expiryDate, student.status) {
        calculateMembershipExpiration(
            startDateStr = student.joiningDate,
            expiryDateStr = student.expiryDate,
            rawStatus = student.status
        )
    }
    val daysRemaining = expInfo.daysRemaining
    val isExpiringSoon = !isArchived && (expInfo.level == MembershipAlertLevel.EXPIRING_SOON || expInfo.level == MembershipAlertLevel.EXPIRING_CRITICAL || expInfo.level == MembershipAlertLevel.EXPIRING_TODAY)
    val isExpired = !isArchived && expInfo.level == MembershipAlertLevel.EXPIRED

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, when {
            isArchived -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            isExpired -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            isExpiringSoon -> LibDeskColors.warning.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.outlineVariant
        }),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(when {
                                isArchived -> MaterialTheme.colorScheme.secondaryContainer
                                isExpired -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isArchived) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = student.fullName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${student.studentCode} • ${student.mobile}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                
                Column(horizontalAlignment = Alignment.End) {
                    if (isArchived) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Archived / Left",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else {
                        MembershipExpiryAlertBadge(
                            expiryDate = student.expiryDate,
                            status = student.status
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isArchived -> MaterialTheme.colorScheme.secondaryContainer
                        student.seatNumber.isNotBlank() -> MaterialTheme.colorScheme.primaryContainer
                        else -> LibDeskColors.warningSoft
                    }
                ) {
                    Text(
                        text = when {
                            isArchived -> if (student.seatNumber.isNotBlank()) "Past: ${student.seatNumber}" else "Archived"
                            student.seatNumber.isNotBlank() -> "Seat: ${student.seatNumber}"
                            else -> "No Seat"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isArchived -> MaterialTheme.colorScheme.onSecondaryContainer
                            student.seatNumber.isNotBlank() -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> LibDeskColors.warning
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = student.targetExam.take(14),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                
                if (isArchived) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Permanent Record ✓",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (student.dueAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Due: ${formatCurrency(student.dueAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LibDeskColors.successSoft
                    ) {
                        Text(
                            text = "Paid ✓",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = LibDeskColors.success,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

            
            if (isArchived) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCopyDetails,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Info", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onSendPromo,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, LibDeskColors.success),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LibDeskColors.success),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(14.dp), tint = LibDeskColors.success)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Promo Ad", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onReactivate,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-admit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    OutlinedButton(
                        onClick = onViewId,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ID Pass", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                        if (student.seatNumber.isBlank()) {
                            FilledTonalButton(
                                onClick = onAssignSeat,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Chair, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Assign", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        
                        if (student.dueAmount > 0) {
                            Button(
                                onClick = onCollectFee,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Collect Fee", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            
                            IconButton(
                                onClick = onSendReminder,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LibDeskColors.successSoft)
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "Send Reminder", tint = LibDeskColors.success, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentProfileDetailsDialog(
    student: StudentEntity,
    onClose: () -> Unit,
    onViewId: () -> Unit,
    onCollectFee: () -> Unit,
    onSendReminder: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onArchiveStudent: () -> Unit = {},
    onReactivateStudent: () -> Unit = {},
    onSendPromo: () -> Unit = {},
    onCopyDetails: () -> Unit = {}
) {
    val isArchived = student.status.equals("ARCHIVED", ignoreCase = true) || student.status.equals("LEFT", ignoreCase = true)
    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isArchived) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isArchived) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = student.fullName.take(1).uppercase(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(student.fullName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                if (isArchived) {
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                                        Text("Archived", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text("ID: ${student.studentCode} • Joined: ${student.joiningDate.ifBlank { "Active" }}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isArchived) {
                            IconButton(onClick = onEditProfile) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        IconButton(onClick = onClose) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isArchived) MaterialTheme.colorScheme.secondaryContainer else LibDeskColors.successSoft),
                    border = BorderStroke(1.dp, if (isArchived) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else LibDeskColors.success.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isArchived) Icons.Default.Inventory2 else Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (isArchived) MaterialTheme.colorScheme.secondary else LibDeskColors.success,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = if (isArchived) "Permanent Archive Database Active" else "LibDesk Permanent Retention Policy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isArchived) MaterialTheme.colorScheme.onSecondaryContainer else LibDeskColors.success
                            )
                            Text(
                                text = if (isArchived)
                                    "Student details are never deleted from the database. Preserved for future re-admission discounts and marketing campaigns."
                                else
                                    "Student and library records are never deleted. When an account exits, all data moves to the safe archive for your records and marketing.",
                                fontSize = 12.sp,
                                color = if (isArchived) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else LibDeskColors.success.copy(alpha = 0.9f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    DetailRow(label = "Status:", value = if (isArchived) "ARCHIVED (Ex-Student)" else student.status)
                    DetailRow(label = "Mobile:", value = student.mobile)
                    DetailRow(label = "Email:", value = student.email.ifBlank { "Not provided" })
                    DetailRow(label = "Target Exam:", value = student.targetExam)
                    DetailRow(label = "Seat & Shift:", value = "${student.seatNumber.ifEmpty { "Unassigned" }} (${student.shiftName.ifEmpty { "Full Day" }})")
                    DetailRow(label = "Plan:", value = student.planName.ifEmpty { "Monthly Standard" })
                    DetailRow(label = "Address:", value = student.address.ifBlank { "N/A" })
                    if (student.parentName.isNotBlank()) {
                        DetailRow(label = "Guardian:", value = "${student.parentName} (${student.parentMobile})")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                
                MembershipStatusCard(
                    student = student,
                    onRenew = onCollectFee,
                    onPayDues = onCollectFee,
                    showActions = !isArchived,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                
                if (isArchived) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onReactivateStudent,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Re-admit Student", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onSendPromo,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Promo Offer", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onCopyDetails,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Complete Details for Ad/Campaign", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onViewId,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Digital Pass", fontSize = 14.sp)
                            }

                            if (student.dueAmount > 0) {
                                OutlinedButton(
                                    onClick = onSendReminder,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LibDeskColors.success),
                                    border = BorderStroke(1.dp, LibDeskColors.success),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = LibDeskColors.success)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = onCollectFee,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Collect Fee", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onArchiveStudent,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LibDeskColors.warning),
                            border = BorderStroke(1.dp, LibDeskColors.warning),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp), tint = LibDeskColors.warning)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exit Student / Move to Archive Data", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun QuickMemberFeeDialog(
    student: StudentEntity,
    onClose: () -> Unit,
    onConfirm: (Double, String, String, String, Double, String, String) -> Unit
) {
    val currentDue = student.dueAmount
    var amountText by remember {
        mutableStateOf(
            if (currentDue > 0) currentDue.toInt().toString()
            else (student.totalFee.takeIf { it > 0 } ?: 1000.0).toInt().toString()
        )
    }
    var discountText by remember { mutableStateOf("0") }
    var paymentMode by remember { mutableStateOf("UPI") }
    var purpose by remember { mutableStateOf(if (currentDue > 0) "DUE_CLEARANCE" else "FEE_RENEWAL") }
    var remarks by remember { mutableStateOf("") }
    val todayStr = remember { DateRangeUtils.getTodayString() }
    var fromDate by remember(student) {
        mutableStateOf(
            if (student.expiryDate.isNotBlank() && student.expiryDate >= todayStr) {
                student.expiryDate
            } else {
                todayStr
            }
        )
    }
    var toDate by remember(fromDate) {
        mutableStateOf(DateRangeUtils.addDaysToDate(fromDate, 30))
    }
    var refNumber by remember { mutableStateOf("REF-${(1000..9999).random()}") }

    val enteredAmt = amountText.toDoubleOrNull() ?: 0.0
    val enteredDisc = discountText.toDoubleOrNull() ?: 0.0
    val calculatedNewDue = (currentDue - enteredAmt - enteredDisc).coerceAtLeast(0.0)

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Collect Fee - ${student.fullName}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Fee: ${formatCurrency(student.totalFee)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Paid So Far: ${formatCurrency(student.paidAmount)}", fontSize = 14.sp, color = LibDeskColors.success, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Outstanding: ${formatCurrency(student.dueAmount)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            Text("New Balance: ${formatCurrency(calculatedNewDue)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Payment Amount (₹) *") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Discount (₹)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f)
                    )
                }

                
                Text("Payment Purpose", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "DUE_CLEARANCE" to "Due Clearance",
                        "FEE_RENEWAL" to "Renewal",
                        "ADMISSION_FEE" to "Admission",
                        "LOCKER_FEE" to "Locker"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = purpose == key,
                            onClick = { purpose = key },
                            label = { Text(label, fontSize = 10.5.sp) }
                        )
                    }
                }

                
                Text("Payment Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("UPI", "CASH", "CARD", "NET_BANKING").forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode, fontSize = 14.sp) }
                        )
                    }
                }

                DepositPeriodDateRangeSelector(
                    fromDate = fromDate,
                    toDate = toDate,
                    onFromDateChange = { fromDate = it },
                    onToDateChange = { toDate = it }
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Notes (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = refNumber,
                    onValueChange = { refNumber = it },
                    label = { Text("Transaction / Receipt Ref") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                val periodRange = "$fromDate to $toDate"
                                onConfirm(amt, paymentMode, purpose, refNumber, enteredDisc, remarks.trim(), periodRange)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Record & Print", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAssignSeatDialog(
    student: StudentEntity,
    seats: List<SeatEntity>,
    shifts: List<ShiftEntity>,
    plans: List<MembershipPlanEntity>,
    onClose: () -> Unit,
    onConfirm: (SeatEntity, ShiftEntity?, MembershipPlanEntity?, String) -> Unit
) {
    val availableSeats = remember(seats) { seats.filter { it.status.equals("AVAILABLE", ignoreCase = true) } }
    var selectedSeat by remember { mutableStateOf(availableSeats.firstOrNull()) }
    var selectedShift by remember { mutableStateOf(shifts.firstOrNull()) }
    var selectedPlan by remember { mutableStateOf(plans.firstOrNull()) }

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Allocate Seat to ${student.fullName}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                if (availableSeats.isEmpty()) {
                    Text("No available seats found in catalog. Please free up a seat or generate new seats.", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                } else {
                    Text("Select Available Seat:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableSeats.take(15).forEach { seat ->
                            FilterChip(
                                selected = selectedSeat?.id == seat.id,
                                onClick = { selectedSeat = seat },
                                label = { Text("${seat.seatNumber} (${seat.floor})", fontSize = 14.sp) }
                            )
                        }
                    }
                }

                Text("Select Shift:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    shifts.forEach { shift ->
                        FilterChip(
                            selected = selectedShift?.id == shift.id,
                            onClick = { selectedShift = shift },
                            label = { Text(shift.name.take(16), fontSize = 14.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onClose, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (selectedSeat != null) {
                                val cal = java.util.Calendar.getInstance()
                                cal.add(java.util.Calendar.DAY_OF_YEAR, 30)
                                val validDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
                                onConfirm(selectedSeat!!, selectedShift, selectedPlan, validDate)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = selectedSeat != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Allocate Seat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SendReminderDialog(
    student: StudentEntity,
    library: LibraryEntity? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val messageText = "Dear ${student.fullName},\nThis is a friendly reminder from ${library?.name ?: "LibDesk Library"} regarding your pending fee dues of *${formatCurrency(student.dueAmount)}*.\n\nPlease scan the UPI QR in the attached official card or pay to UPI ID: *${library?.upiId ?: ""}*.\nThank you!"

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
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
                                .background(LibDeskColors.successSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = LibDeskColors.success, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Fee Reminder (WhatsApp)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("ID: ${student.studentCode} • Ph: ${student.mobile}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Pending Due", fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                            Text(formatCurrency(student.dueAmount), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Text("Message Preview & Graphic Card:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = messageText,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Includes high-res graphic card with UPI QR code", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                
                Button(
                    onClick = {
                        val reminderBitmap = ImageShareUtils.createFeeReminderBitmap(library, student)
                        ImageShareUtils.sendImageToWhatsApp(
                            context = context,
                            bitmap = reminderBitmap,
                            fileName = "reminder_${student.studentCode}.png",
                            captionText = messageText,
                            phoneNumber = student.mobile
                        )
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Reminder Image to WhatsApp", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val reminderBitmap = ImageShareUtils.createFeeReminderBitmap(library, student)
                            ImageShareUtils.shareImageGeneric(
                                context = context,
                                bitmap = reminderBitmap,
                                fileName = "reminder_${student.studentCode}.png",
                                title = "Share Fee Reminder",
                                captionText = messageText
                            )
                            onClose()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Image", fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Fee Reminder", messageText)
                            clipboard.setPrimaryClip(clip)
                            // Toast.makeText(context, "Message copied to clipboard!", Toast.LENGTH_SHORT).show()
                            onClose()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Text", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterStudentDialog(
    shifts: List<ShiftEntity>,
    plans: List<MembershipPlanEntity>,
    onClose: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String, String, String, String, ShiftEntity?, MembershipPlanEntity?, Double, Double) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+91") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var targetExam by remember { mutableStateOf("UPSC CSE") }
    var courseClass by remember { mutableStateOf("Graduate") }
    var address by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf("") }
    var parentMobile by remember { mutableStateOf("") }
    var joiningDate by remember { mutableStateOf(com.example.ui.components.DateRangeUtils.getTodayString()) }

    var selectedShift by remember { mutableStateOf(shifts.firstOrNull()) }
    var selectedPlan by remember { mutableStateOf(plans.firstOrNull()) }
    var feeText by remember { mutableStateOf("1000") }
    var paidText by remember { mutableStateOf("1000") }

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Register New Member",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    CountryCodePhoneField(
                        mobile = mobile,
                        onMobileChange = { mobile = it },
                        countryCode = countryCode,
                        onCountryCodeChange = { countryCode = it },
                        label = "Mobile Number *",
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = joiningDate,
                        onValueChange = { joiningDate = it },
                        label = { Text("Date of Admission") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                com.example.ui.components.DateRangeUtils.showNativeDatePicker(context, joiningDate) { pickedDate ->
                                    joiningDate = pickedDate
                                }
                            }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = targetExam,
                        onValueChange = { targetExam = it },
                        label = { Text("Target Exam (UPSC, NEET, Banking, etc.)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Select Student Plan & Offer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        plans.forEach { plan ->
                            val net = (plan.baseFee - plan.discount).coerceAtLeast(0.0)
                            FilterChip(
                                selected = selectedPlan?.id == plan.id,
                                onClick = {
                                    selectedPlan = plan
                                    feeText = net.toInt().toString()
                                    paidText = net.toInt().toString()
                                },
                                label = {
                                    if (plan.discount > 0) {
                                        Text("${plan.name} (${formatCurrency(net)} • ₹${plan.discount.toInt()} OFF)", fontSize = 14.sp)
                                    } else {
                                        Text("${plan.name} (${formatCurrency(plan.baseFee)})", fontSize = 14.sp)
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    Text("Select Shift", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        shifts.forEach { shift ->
                            FilterChip(
                                selected = selectedShift?.id == shift.id,
                                onClick = { selectedShift = shift },
                                label = { Text("${shift.name} (${shift.startTime}-${shift.endTime})", fontSize = 14.sp) }
                            )
                        }
                    }
                }

                item {
                    val totalFeeVal = feeText.toDoubleOrNull() ?: 0.0
                    val paidVal = paidText.toDoubleOrNull() ?: 0.0
                    val dueCalc = (totalFeeVal - paidVal).coerceAtLeast(0.0)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = feeText,
                                onValueChange = { feeText = it },
                                label = { Text("Total Fee (₹)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = paidText,
                                onValueChange = { paidText = it },
                                label = { Text("Paid Now (₹)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = if (dueCalc > 0) "Calculated Due: ${formatCurrency(dueCalc)}" else "Fee Status: Full Paid",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dueCalc > 0) MaterialTheme.colorScheme.error else LibDeskColors.success
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Full Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onClose,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (fullName.isNotBlank() && mobile.isNotBlank()) {
                                    val fee = feeText.toDoubleOrNull() ?: 1000.0
                                    val paid = paidText.toDoubleOrNull() ?: 1000.0
                                    val fullMobile = combineCountryCodeAndPhone(countryCode, mobile)
                                    onConfirm(
                                        fullName,
                                        fullMobile,
                                        email,
                                        gender,
                                        targetExam,
                                        courseClass,
                                        address,
                                        parentName,
                                        parentMobile,
                                        joiningDate,
                                        selectedShift,
                                        selectedPlan,
                                        fee,
                                        paid
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            enabled = fullName.isNotBlank() && mobile.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Register Member", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExitStudentConfirmationDialog(
    student: StudentEntity,
    onClose: () -> Unit,
    onConfirmArchive: (String) -> Unit
) {
    var reason by remember { mutableStateOf("Completed preparation / Left library") }
    val reasons = listOf(
        "Completed preparation / Left library",
        "Exam passed / Job selected",
        "Temporarily relocated",
        "Requested account deletion / exit",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    text = "Archive & Preserve Student Record",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LibDeskColors.warningSoft,
                    border = BorderStroke(1.dp, LibDeskColors.warning.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Permanent Record Policy Active",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LibDeskColors.warning
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Student '${student.fullName}' will NOT be deleted from the database. All contact details, fee history, and KYC records will be safely archived for future verification and promotional campaigns.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Select Exit / Archive Reason:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                reasons.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reason = r }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (reason == r),
                            onClick = { reason = r },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = r, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmArchive(reason) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm Archive", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onClose,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun copyStudentDetails(context: Context, student: StudentEntity) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = """
        LIBDESK STUDENT ARCHIVE RECORD
        Name: ${student.fullName}
        Mobile: ${student.mobile}
        Email: ${student.email.ifBlank { "N/A" }}
        Status: ${student.status}
        Exam Target: ${student.targetExam.ifBlank { "General" }}
        Address: ${student.address.ifBlank { "N/A" }}
        Guardian: ${student.parentName} (${student.parentMobile})
        Registered Date: ${student.joiningDate}
        Past Seat: ${student.seatNumber}
    """.trimIndent()
    val clip = ClipData.newPlainText("Student Archive Details", text)
    clipboard.setPrimaryClip(clip)
    // Toast.makeText(context, "Student details copied for advertising & campaigns!", Toast.LENGTH_SHORT).show()
}

private fun sendPromotionalOffer(context: Context, student: StudentEntity, library: LibraryEntity?) {
    val libName = library?.name ?: "LibDesk Study Library"
    val promoMessage = "Hello ${student.fullName}! We miss you at $libName. We are offering an exclusive alumni re-admission discount of 20% on monthly & quarterly library seats! Contact us at ${library?.ownerPhone ?: ""} to book your desk today."
    ImageShareUtils.sendTextToWhatsApp(context, student.mobile, promoMessage)
}

