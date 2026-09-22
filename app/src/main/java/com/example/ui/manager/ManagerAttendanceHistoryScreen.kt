package com.example.ui.manager

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.components.EditAttendanceModal
import com.example.ui.theme.*
import com.example.viewmodel.LibDeskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerAttendanceHistoryScreen(
    viewModel: LibDeskViewModel,
    onBack: (() -> Unit)? = null,
    onOpenQrScanner: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val libraries by viewModel.allLibraries.collectAsState(initial = emptyList())
    val currentLibId by viewModel.currentLibraryId.collectAsState(initial = "")
    val attendanceLogs by viewModel.attendanceLogs.collectAsState(initial = emptyList())
    val students by viewModel.students.collectAsState(initial = emptyList())
    val halls by viewModel.halls.collectAsState(initial = emptyList())
    val shifts by viewModel.shifts.collectAsState(initial = emptyList())

    val currentLib = libraries.find { it.id == currentLibId }

    // Search and filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedDateFilter by remember { mutableStateOf("ALL") } // ALL, TODAY, YESTERDAY, LAST_7_DAYS, LAST_30_DAYS, CUSTOM
    var customDateQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, CHECKED_IN, CHECKED_OUT, QR_ONLY, MANUAL_ONLY
    var selectedShiftFilter by remember { mutableStateOf("ALL") }
    var showManualPunchDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<AttendanceEntity?>(null) }
    var selectedRecordForEdit by remember { mutableStateOf<AttendanceEntity?>(null) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val yesterdayStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    val sevenDaysAgoStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    val thirtyDaysAgoStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    // Filtered attendance list
    val filteredLogs = remember(
        attendanceLogs,
        searchQuery,
        selectedDateFilter,
        customDateQuery,
        selectedStatusFilter,
        selectedShiftFilter
    ) {
        attendanceLogs.filter { log ->
            // Search query matches student name, seat, shift, or notes
            val matchesSearch = searchQuery.isBlank() ||
                    log.studentName.contains(searchQuery, ignoreCase = true) ||
                    log.seatNumber.contains(searchQuery, ignoreCase = true) ||
                    log.shiftName.contains(searchQuery, ignoreCase = true) ||
                    log.notes.contains(searchQuery, ignoreCase = true) ||
                    log.mode.contains(searchQuery, ignoreCase = true)

            // Date filtering
            val matchesDate = when (selectedDateFilter) {
                "TODAY" -> log.date == todayStr
                "YESTERDAY" -> log.date == yesterdayStr
                "LAST_7_DAYS" -> log.date >= sevenDaysAgoStr
                "LAST_30_DAYS" -> log.date >= thirtyDaysAgoStr
                "CUSTOM" -> customDateQuery.isBlank() || log.date.contains(customDateQuery.trim())
                else -> true
            }

            // Status filtering
            val matchesStatus = when (selectedStatusFilter) {
                "CHECKED_IN" -> log.status == "CHECKED_IN"
                "CHECKED_OUT" -> log.status == "CHECKED_OUT"
                "QR_ONLY" -> log.mode.contains("QR", ignoreCase = true)
                "MANUAL_ONLY" -> log.mode.contains("MANUAL", ignoreCase = true)
                else -> true
            }

            // Shift filtering
            val matchesShift = selectedShiftFilter == "ALL" || log.shiftName.equals(selectedShiftFilter, ignoreCase = true)

            matchesSearch && matchesDate && matchesStatus && matchesShift
        }
    }

    // Metrics computed from logs
    val totalPunches = filteredLogs.size
    val activeCheckedIn = filteredLogs.count { it.status == "CHECKED_IN" }
    val completedSessions = filteredLogs.count { it.status == "CHECKED_OUT" }
    val totalDurationMins = filteredLogs.filter { it.durationMinutes > 0 }.sumOf { it.durationMinutes }
    val avgDurationHours = if (completedSessions > 0) {
        val avg = totalDurationMins.toDouble() / completedSessions
        String.format(Locale.getDefault(), "%.1fh", avg / 60.0)
    } else "0.0h"

    Scaffold(
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
        // Top Toolbar
        Surface(
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(
                                text = "Attendance Register & History",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Real-time punch records, check-in logs & registry",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    // QR Scan shortcut button
                    IconButton(
                        onClick = onOpenQrScanner,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Library Switcher Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = currentLib?.name ?: "Your Library",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Code: ${currentLib?.code ?: "LIB-101"} • Independent Library",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Independent Entity",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Analytics KPI Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total punches
                    AttendanceKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Total Punches",
                        value = "$totalPunches",
                        subtitle = "Logs recorded",
                        icon = Icons.Default.FormatListNumbered,
                        accentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Live active in library
                    AttendanceKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Live Inside",
                        value = "$activeCheckedIn",
                        subtitle = "Checked In",
                        icon = Icons.Default.Sensors,
                        accentColor = LibDeskColors.success,
                        isLivePulsing = activeCheckedIn > 0
                    )

                    // Average duration
                    AttendanceKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Avg Session",
                        value = avgDurationHours,
                        subtitle = "$completedSessions checked-out",
                        icon = Icons.Default.Timer,
                        accentColor = LibDeskColors.warning
                    )
                }
            }

            // Quick Actions: Manual Punch & WhatsApp Export
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showManualPunchDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Manual Punch", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            // Generate Daily Summary & Share via WhatsApp
                            val report = buildAttendanceReport(
                                libraryName = currentLib?.name ?: "Library",
                                logs = filteredLogs,
                                dateFilter = selectedDateFilter
                            )
                            clipboardManager.setText(AnnotatedString(report))
                            try {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, report)
                                    type = "text/plain"
                                    `package` = "com.whatsapp"
                                }
                                context.startActivity(sendIntent)
                            } catch (e: Exception) {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, report)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Attendance Register"))
                            }
                            // Toast.makeText(context, "Attendance report copied & opened for sharing", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Register", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by student name, seat, shift, or mode...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // Date Filters Row
            item {
                Column {
                    Text(
                        text = "DATE RANGE FILTER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val dateFilters = listOf(
                            "ALL" to "All History",
                            "TODAY" to "Today",
                            "YESTERDAY" to "Yesterday",
                            "LAST_7_DAYS" to "Last 7 Days",
                            "LAST_30_DAYS" to "Last 30 Days",
                            "CUSTOM" to "Custom Date"
                        )
                        items(dateFilters) { (key, label) ->
                            FilterChip(
                                selected = selectedDateFilter == key,
                                onClick = { selectedDateFilter = key },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedDateFilter == key) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (selectedDateFilter == "CUSTOM") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customDateQuery,
                            onValueChange = { customDateQuery = it },
                            placeholder = { Text("Enter date (e.g. 2026-09-10 or 2026-09)") },
                            label = { Text("Filter by Specific Date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Status Filter Row
            item {
                Column {
                    Text(
                        text = "STATUS & MODE FILTER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val statusFilters = listOf(
                            "ALL" to "All Punches",
                            "CHECKED_IN" to "Live Checked In (${activeCheckedIn})",
                            "CHECKED_OUT" to "Completed Sessions (${completedSessions})",
                            "QR_ONLY" to "QR Gate Scans",
                            "MANUAL_ONLY" to "Manual Desk Entries"
                        )
                        items(statusFilters) { (key, label) ->
                            FilterChip(
                                selected = selectedStatusFilter == key,
                                onClick = { selectedStatusFilter = key },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedStatusFilter == key) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (key == "CHECKED_IN") LibDeskColors.success else MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Attendance Records Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attendance Records (${filteredLogs.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Sorted by Latest",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Attendance Cards List
            if (filteredLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Attendance Records Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try changing the date/status filter, or tap 'Manual Punch' to add an attendance record.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedDateFilter = "ALL"
                                    selectedStatusFilter = "ALL"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset All Filters")
                            }
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    AttendanceItemCard(
                        log = log,
                        onMarkCheckOut = {
                            viewModel.markAttendanceCheckOut(log.id)
                        },
                        onEdit = {
                            selectedRecordForEdit = log
                        },
                        onDelete = {
                            recordToDelete = log
                        },
                        onCopySlip = {
                            val slip = """
                                📋 LIBDESK ATTENDANCE SLIP
                                Library: ${currentLib?.name ?: "Your Library"}
                                Student: ${log.studentName}
                                Seat: ${log.seatNumber.ifEmpty { "General Desk" }} • ${log.shiftName}
                                Date: ${log.date}
                                Check In: ${log.checkInTime}
                                Check Out: ${log.checkOutTime.ifEmpty { "Active inside" }}
                                Duration: ${if (log.durationMinutes > 0) "${log.durationMinutes / 60}h ${log.durationMinutes % 60}m" else "Ongoing"}
                                Mode: ${log.mode}
                                Status: ${log.status}
                            """.trimIndent()
                            clipboardManager.setText(AnnotatedString(slip))
                            // Toast.makeText(context, "Slip copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Manual Attendance Dialog
    if (showManualPunchDialog) {
        ManualAttendanceDialog(
            students = students,
            halls = halls.map { it.name },
            shifts = shifts.map { it.name },
            onDismiss = { showManualPunchDialog = false },
            onSave = { studentId, studentName, seat, hall, shift, date, inTime, outTime, duration, status, mode, notes ->
                viewModel.recordManualAttendance(
                    studentId = studentId,
                    studentName = studentName,
                    seatNumber = seat,
                    hallName = hall,
                    shiftName = shift,
                    date = date,
                    checkInTime = inTime,
                    checkOutTime = outTime,
                    durationMinutes = duration,
                    status = status,
                    mode = mode,
                    notes = notes
                )
                showManualPunchDialog = false
            }
        )
    }

    // Delete confirmation dialog
    if (recordToDelete != null) {
        val record = recordToDelete!!
        ActionConfirmationDialog(
            show = true,
            title = "Delete Attendance Entry?",
            message = "Are you sure you want to permanently delete this attendance record for ${record.studentName} on ${record.date} (${record.checkInTime})?",
            confirmText = "Delete Record",
            isDestructive = true,
            targetName = "${record.studentName} • ${record.date}",
            onConfirm = {
                viewModel.deleteAttendanceRecord(record.id)
                recordToDelete = null
            },
            onDismiss = { recordToDelete = null }
        )
    }

    // Edit Wrong Attendance Entry Modal
    if (selectedRecordForEdit != null) {
        EditAttendanceModal(
            show = true,
            attendance = selectedRecordForEdit,
            onSave = { recordId, newDate, newCheckIn, newCheckOut, newStatus, newNotes ->
                viewModel.editAttendanceRecord(recordId, newDate, newCheckIn, newCheckOut, newStatus, newNotes)
                selectedRecordForEdit = null
            },
            onDelete = { att ->
                viewModel.deleteAttendanceRecord(att.id)
                selectedRecordForEdit = null
            },
            onDismiss = { selectedRecordForEdit = null }
        )
    }
    } // Scaffold content
}

@Composable
fun AttendanceKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isLivePulsing: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isLivePulsing) {
                    Surface(
                        shape = CircleShape,
                        color = LibDeskColors.successSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(LibDeskColors.success)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LibDeskColors.success
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 20.sp
                )
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AttendanceItemCard(
    log: AttendanceEntity,
    onMarkCheckOut: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    onCopySlip: () -> Unit
) {
    val isCheckedIn = log.status == "CHECKED_IN"
    val isQr = log.mode.contains("QR", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isCheckedIn) 1.5.dp else 1.dp,
            color = if (isCheckedIn) LibDeskColors.success.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Student Name, Seat, and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isCheckedIn) LibDeskColors.success.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = log.studentName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCheckedIn) LibDeskColors.success else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = log.studentName,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Seat ${log.seatNumber.ifEmpty { "Unassigned" }}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 11.5.sp
                                )
                            )
                            Text(
                                text = " • ${log.shiftName.ifEmpty { "Standard Shift" }}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCheckedIn) LibDeskColors.successSoft else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isCheckedIn) LibDeskColors.success else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isCheckedIn) "Checked In" else "Completed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCheckedIn) LibDeskColors.success else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // Timeline Row: Date, In Time, Out Time, Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date & Day
                Column {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = log.date,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Check In
                Column {
                    Text(
                        text = "Check In",
                        style = MaterialTheme.typography.labelSmall.copy(color = LibDeskColors.success)
                    )
                    Text(
                        text = log.checkInTime.ifEmpty { "--:--" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = LibDeskColors.success
                        )
                    )
                }

                // Check Out
                Column {
                    Text(
                        text = "Check Out",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isCheckedIn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    Text(
                        text = if (isCheckedIn) "Active Now" else log.checkOutTime.ifEmpty { "--:--" },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCheckedIn) LibDeskColors.success else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                // Duration
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Duration",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCheckedIn) LibDeskColors.success.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = if (log.durationMinutes > 0) {
                                "${log.durationMinutes / 60}h ${log.durationMinutes % 60}m"
                            } else if (isCheckedIn) "Ongoing" else "--",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCheckedIn) LibDeskColors.success else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Mode badge, notes, and Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isQr) Icons.Default.QrCode else Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${log.mode} • ${log.notes.ifEmpty { "Verified entry" }}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // If currently checked in, provide 1-tap Check Out button
                    if (isCheckedIn) {
                        FilledTonalButton(
                            onClick = onMarkCheckOut,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Punch Out",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.5.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Copy slip icon button
                    IconButton(
                        onClick = onCopySlip,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Slip",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Edit wrong entry icon button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Entry",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Delete record icon button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAttendanceDialog(
    students: List<StudentEntity>,
    halls: List<String>,
    shifts: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        studentId: String,
        studentName: String,
        seat: String,
        hall: String,
        shift: String,
        date: String,
        inTime: String,
        outTime: String,
        duration: Int,
        status: String,
        mode: String,
        notes: String
    ) -> Unit
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val nowTimeStr = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) }

    var selectedStudent by remember { mutableStateOf<StudentEntity?>(students.firstOrNull()) }
    var studentSearchText by remember { mutableStateOf("") }
    var isStudentDropdownOpen by remember { mutableStateOf(false) }

    var dateText by remember { mutableStateOf(todayStr) }
    var checkInTimeText by remember { mutableStateOf(nowTimeStr) }
    var checkOutTimeText by remember { mutableStateOf("") }
    var isAlreadyCheckedOut by remember { mutableStateOf(false) }
    var selectedShift by remember { mutableStateOf(shifts.firstOrNull() ?: "Full Day") }
    var selectedHall by remember { mutableStateOf(halls.firstOrNull() ?: "Reading Hall A") }
    var notesText by remember { mutableStateOf("Manual Reception Entry") }
    var modeText by remember { mutableStateOf("MANUAL_DESK") }

    val filteredStudents = remember(students, studentSearchText) {
        if (studentSearchText.isBlank()) students
        else students.filter {
            it.fullName.contains(studentSearchText, ignoreCase = true) ||
            it.mobile.contains(studentSearchText) ||
            it.studentCode.contains(studentSearchText, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Record Manual Attendance", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Student Selection
                Text("Select Student Member *", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isStudentDropdownOpen = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedStudent?.fullName ?: "Tap to choose student",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (selectedStudent != null) {
                                Text(
                                    text = "Code: ${selectedStudent?.studentCode} • Seat: ${selectedStudent?.seatNumber.takeIf { !it.isNullOrBlank() } ?: "Unassigned"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = isStudentDropdownOpen,
                        onDismissRequest = { isStudentDropdownOpen = false }
                    ) {
                        OutlinedTextField(
                            value = studentSearchText,
                            onValueChange = { studentSearchText = it },
                            placeholder = { Text("Search by name/mobile...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            singleLine = true
                        )
                        filteredStudents.take(8).forEach { stu ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(stu.fullName, fontWeight = FontWeight.Bold)
                                        Text("${stu.studentCode} • ${stu.mobile} • Seat ${stu.seatNumber}", style = MaterialTheme.typography.bodySmall)
                                    }
                                },
                                onClick = {
                                    selectedStudent = stu
                                    if (stu.shiftName.isNotBlank()) selectedShift = stu.shiftName
                                    if (stu.hallName.isNotBlank()) selectedHall = stu.hallName
                                    isStudentDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Date & In-Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = checkInTimeText,
                        onValueChange = { checkInTimeText = it },
                        label = { Text("Check-In Time") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Check out toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Also log Check-Out now?", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    Switch(
                        checked = isAlreadyCheckedOut,
                        onCheckedChange = {
                            isAlreadyCheckedOut = it
                            if (it && checkOutTimeText.isBlank()) {
                                checkOutTimeText = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                            }
                        }
                    )
                }

                if (isAlreadyCheckedOut) {
                    OutlinedTextField(
                        value = checkOutTimeText,
                        onValueChange = { checkOutTimeText = it },
                        label = { Text("Check-Out Time (e.g. 05:30 PM)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Reason / Notes
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Reason / Staff Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stu = selectedStudent ?: return@Button
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val duration = if (isAlreadyCheckedOut && checkOutTimeText.isNotBlank()) {
                        try {
                            val t1 = sdf.parse(checkInTimeText)
                            val t2 = sdf.parse(checkOutTimeText)
                            if (t1 != null && t2 != null) {
                                val diff = (t2.time - t1.time) / (60 * 1000)
                                if (diff > 0) diff.toInt() else 180
                            } else 180
                        } catch (e: Exception) { 180 }
                    } else 0

                    onSave(
                        stu.id,
                        stu.fullName,
                        stu.seatNumber,
                        selectedHall,
                        selectedShift,
                        dateText.trim(),
                        checkInTimeText.trim(),
                        if (isAlreadyCheckedOut) checkOutTimeText.trim() else "",
                        duration,
                        if (isAlreadyCheckedOut) "CHECKED_OUT" else "CHECKED_IN",
                        modeText,
                        notesText.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = selectedStudent != null
            ) {
                Text("Save Attendance Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun buildAttendanceReport(
    libraryName: String,
    logs: List<AttendanceEntity>,
    dateFilter: String
): String {
    val total = logs.size
    val active = logs.count { it.status == "CHECKED_IN" }
    val completed = logs.count { it.status == "CHECKED_OUT" }

    val sb = StringBuilder()
    sb.appendLine("📊 *LIBDESK DAILY ATTENDANCE REGISTER*")
    sb.appendLine("🏢 *Library:* $libraryName")
    sb.appendLine("📅 *Filter:* $dateFilter")
    sb.appendLine("👥 *Total Punches:* $total | *Live In-Hall:* $active | *Completed:* $completed")
    sb.appendLine("----------------------------------")

    logs.forEachIndexed { i, log ->
        val statusIcon = if (log.status == "CHECKED_IN") "🟢" else "🔵"
        val outStr = if (log.checkOutTime.isNotBlank()) " -> ${log.checkOutTime}" else " (Present)"
        sb.appendLine("${i + 1}. $statusIcon *${log.studentName}* [Seat: ${log.seatNumber.ifEmpty { "Gen" }}]")
        sb.appendLine("   ⏰ ${log.checkInTime}$outStr | 🏷️ ${log.shiftName} (${log.mode})")
    }

    sb.appendLine("----------------------------------")
    sb.appendLine("Powered by LibDesk SaaS • Smart Library Cloud Platform")
    return sb.toString()
}
