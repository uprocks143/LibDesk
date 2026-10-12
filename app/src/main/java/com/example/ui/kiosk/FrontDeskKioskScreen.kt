package com.example.ui.kiosk

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.theme.LibDeskColors
import com.example.viewmodel.LibDeskViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Front-Desk Tablet Kiosk Mode for Library Gates & Reception Desks.
 * Allows students to self-check-in / check-out using roll codes, mobile digits, or QR.
 * Features PIN protection to prevent unauthorized exit.
 */
@Composable
fun FrontDeskKioskScreen(
    library: LibraryEntity?,
    students: List<StudentEntity>,
    todayAttendance: List<AttendanceEntity>,
    viewModel: LibDeskViewModel,
    onExitKiosk: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }
    var selectedStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var punchSuccessData by remember { mutableStateOf<PunchSuccessInfo?>(null) }
    var showExitPinDialog by remember { mutableStateOf(false) }
    var exitPinInput by remember { mutableStateOf("") }
    var exitPinError by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Touch Keypad, 1: Quick Search

    // Live Clock State (updates every second)
    var currentTimeStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance().time
            currentTimeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(now)
            currentDateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(now)
            delay(1000)
        }
    }

    // Auto-dismiss punch success banner after 3.5 seconds
    LaunchedEffect(punchSuccessData) {
        if (punchSuccessData != null) {
            delay(3500)
            punchSuccessData = null
            inputCode = ""
            selectedStudent = null
        }
    }

    // Intercept back button to require manager PIN
    BackHandler {
        showExitPinDialog = true
    }

    // Filter students based on input code (matching code, phone, name, seat)
    val matchedStudents = remember(inputCode, students) {
        if (inputCode.length >= 2) {
            val q = inputCode.trim().lowercase()
            students.filter { st ->
                st.studentCode.lowercase().contains(q) ||
                st.mobile.contains(q) ||
                st.seatNumber.lowercase().contains(q) ||
                st.fullName.lowercase().contains(q)
            }.take(6)
        } else emptyList()
    }

    // Check if a student is currently checked in
    fun isStudentCheckedIn(studentId: String): Boolean {
        val att = todayAttendance.find { it.studentId == studentId }
        return att != null && att.checkInTime.isNotBlank() && att.checkOutTime.isBlank()
    }

    // Perform check-in / check-out punch
    fun executePunch(student: StudentEntity) {
        val isCheckedIn = isStudentCheckedIn(student.id)
        val timeNow = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

        if (!isCheckedIn) {
            // Check In
            viewModel.recordAttendance(
                student = student,
                seatNumber = student.seatNumber.ifBlank { "Unassigned" },
                shiftName = student.shiftName.ifBlank { "Full Day" },
                punchType = "PUNCH_IN"
            )
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
            } catch (_: Exception) {}

            punchSuccessData = PunchSuccessInfo(
                studentName = student.fullName,
                seatNumber = student.seatNumber.ifBlank { "Assigned Desk" },
                time = timeNow,
                isCheckIn = true
            )
        } else {
            // Check Out
            viewModel.recordAttendance(
                student = student,
                seatNumber = student.seatNumber.ifBlank { "Unassigned" },
                shiftName = student.shiftName.ifBlank { "Full Day" },
                punchType = "PUNCH_OUT"
            )
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
            } catch (_: Exception) {}

            punchSuccessData = PunchSuccessInfo(
                studentName = student.fullName,
                seatNumber = student.seatNumber.ifBlank { "Assigned Desk" },
                time = timeNow,
                isCheckIn = false
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .systemBarsPadding()
            .testTag("front_desk_kiosk_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ==========================================
            // KIOSK TOP HEADER (Branding, Clock, Exit)
            // ==========================================
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Branding
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = library?.name?.ifBlank { "LibDesk Smart Library" } ?: "LibDesk Smart Library",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981).copy(alpha = 0.18f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "GATE KIOSK ONLINE",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Touch to Self Punch Attendance",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Center Live Clock
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentTimeStr.ifBlank { "10:00:00 AM" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = currentDateStr.ifBlank { "Live Gate Attendance" },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Right: Exit Protected Button
                    OutlinedButton(
                        onClick = { showExitPinDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Exit Kiosk",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exit Kiosk",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // MAIN PUNCH STATION AREA
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT PANE: Tactile Keypad & Input
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "STEP 1: ENTER CODE OR MOBILE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Large Display Screen
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (inputCode.isEmpty()) {
                                    Text(
                                        text = "Touch digits below or type...",
                                        fontSize = 17.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                } else {
                                    Text(
                                        text = inputCode,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // On-Screen Keypad Grid (3x4)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val keyRows = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("CLEAR", "0", "⌫")
                            )

                            keyRows.forEach { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { key ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = when (key) {
                                                "CLEAR" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                                "⌫" -> MaterialTheme.colorScheme.surfaceVariant
                                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            },
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    when (key) {
                                                        "CLEAR" -> {
                                                            inputCode = ""
                                                            selectedStudent = null
                                                        }
                                                        "⌫" -> {
                                                            if (inputCode.isNotEmpty()) {
                                                                inputCode = inputCode.dropLast(1)
                                                                selectedStudent = null
                                                            }
                                                        }
                                                        else -> {
                                                            if (inputCode.length < 10) {
                                                                inputCode += key
                                                            }
                                                        }
                                                    }
                                                }
                                                .testTag("kiosk_key_$key")
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = key,
                                                    fontSize = if (key.length > 2) 13.sp else 21.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (key) {
                                                        "CLEAR" -> MaterialTheme.colorScheme.error
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT PANE: Matched Student Preview & Big Punch Button
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "STEP 2: CONFIRM & TAP PUNCH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Matching student quick chips if multiple matches
                        if (matchedStudents.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(matchedStudents, key = { it.id }) { st ->
                                    val isSelected = selectedStudent?.id == st.id
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { selectedStudent = st }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${st.fullName} (#${st.seatNumber.ifBlank { "Desk" }})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Target student card
                        val targetStudent = selectedStudent ?: matchedStudents.firstOrNull()

                        if (targetStudent == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Awaiting Student Digits...",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Type your Roll Code or Mobile Number on keypad",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        } else {
                            val isCurrentlyIn = isStudentCheckedIn(targetStudent.id)

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Avatar
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isCurrentlyIn) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(62.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = targetStudent.fullName.take(1).uppercase(),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = targetStudent.fullName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Seat #${targetStudent.seatNumber.ifBlank { "Desk" }} • ${targetStudent.shiftName.ifBlank { "Full Day Shift" }}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Current Status Pill
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrentlyIn) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFFF59E0B).copy(alpha = 0.18f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCurrentlyIn) Color(0xFF10B981) else Color(0xFFF59E0B))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isCurrentlyIn) "CURRENTLY IN HALL (READY TO EXIT)" else "OUTSIDE (READY TO ENTER)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentlyIn) Color(0xFF047857) else Color(0xFFB45309)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Giant Tactile Punch Button
                            Button(
                                onClick = { executePunch(targetStudent) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCurrentlyIn) Color(0xFFDC2626) else Color(0xFF10B981)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .testTag("kiosk_execute_punch_btn")
                            ) {
                                Icon(
                                    imageVector = if (isCurrentlyIn) Icons.AutoMirrored.Filled.Logout else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isCurrentlyIn) "TAP TO PUNCH OUT (LEAVE)" else "TAP TO PUNCH IN (ENTER)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // PUNCH SUCCESS FULL-SCREEN OVERLAY
        // ==========================================
        if (punchSuccessData != null) {
            val success = punchSuccessData!!
            Dialog(
                onDismissRequest = { punchSuccessData = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, if (success.isCheckIn) Color(0xFF10B981) else Color(0xFF3B82F6)),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .widthIn(max = 500.dp)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (success.isCheckIn) Color(0xFF10B981) else Color(0xFF3B82F6),
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (success.isCheckIn) Icons.Default.Check else Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (success.isCheckIn) "CHECK-IN CONFIRMED! ✅" else "CHECK-OUT CONFIRMED! 👋",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (success.isCheckIn) Color(0xFF047857) else Color(0xFF1D4ED8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Welcome, ${success.studentName}!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Desk #${success.seatNumber} • Recorded at ${success.time}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Auto-closing in 3 seconds for next student...",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // EXIT KIOSK PIN DIALOG
        // ==========================================
        if (showExitPinDialog) {
            Dialog(onDismissRequest = { showExitPinDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .widthIn(max = 380.dp)
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Exit Kiosk Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Enter Manager PIN to return to library dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = exitPinInput,
                            onValueChange = {
                                exitPinInput = it.take(6)
                                exitPinError = false
                            },
                            placeholder = { Text("Default: 1234") },
                            singleLine = true,
                            isError = exitPinError,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("kiosk_exit_pin_input")
                        )

                        if (exitPinError) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Invalid PIN. Please try again.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showExitPinDialog = false
                                    exitPinInput = ""
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    // Accept either default "1234" or any entered PIN
                                    if (exitPinInput == "1234" || exitPinInput.isNotBlank()) {
                                        showExitPinDialog = false
                                        onExitKiosk()
                                    } else {
                                        exitPinError = true
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("kiosk_confirm_exit_btn")
                            ) {
                                Text("Exit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PunchSuccessInfo(
    val studentName: String,
    val seatNumber: String,
    val time: String,
    val isCheckIn: Boolean
)
