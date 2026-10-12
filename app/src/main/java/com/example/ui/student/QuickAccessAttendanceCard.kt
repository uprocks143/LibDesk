package com.example.ui.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.theme.LibDeskColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * High-craft Quick Access Check-In / Check-Out Attendance Control Card.
 *
 * Provides:
 * 1. 1-Tap Quick Check-In button with immediate desk confirmation.
 * 2. 1-Tap Quick Check-Out button with active session duration computation.
 * 3. Quick Scan QR button for gate turnstile or desk barcode verification.
 * 4. Real-time session elapsed time and daily study goal indicator.
 */
@Composable
fun QuickAccessAttendanceCard(
    student: StudentEntity,
    todayAttendance: AttendanceEntity?,
    isCurrentlyCheckedIn: Boolean,
    onQuickCheckIn: () -> Unit,
    onQuickCheckOut: () -> Unit,
    onOpenQrScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val checkInTime = todayAttendance?.checkInTime.orEmpty()
    val checkOutTime = todayAttendance?.checkOutTime.orEmpty()

    var showCheckOutConfirmDialog by remember { mutableStateOf(false) }
    var showCheckInConfirmDialog by remember { mutableStateOf(false) }

    // Live session elapsed calculation
    var elapsedMinutes by remember { mutableStateOf(0) }
    LaunchedEffect(isCurrentlyCheckedIn, checkInTime) {
        if (isCurrentlyCheckedIn && checkInTime.isNotBlank()) {
            while (true) {
                try {
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val parsed = sdf.parse(checkInTime)
                    val now = sdf.parse(sdf.format(Date()))
                    if (parsed != null && now != null) {
                        val diff = (now.time - parsed.time) / (60 * 1000)
                        elapsedMinutes = if (diff > 0) diff.toInt() else 0
                    }
                } catch (e: Exception) {
                    elapsedMinutes = 0
                }
                kotlinx.coroutines.delay(30000L) // update every 30 seconds
            }
        } else {
            elapsedMinutes = 0
        }
    }

    val elapsedHours = elapsedMinutes / 60
    val elapsedRemMins = elapsedMinutes % 60

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.5.dp,
            if (isCurrentlyCheckedIn) Color(0xFF10B981).copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("quick_access_attendance_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Section Label + Status Ticker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrentlyCheckedIn) Color(0xFF10B981).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isCurrentlyCheckedIn) Icons.Default.PunchClock else Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = if (isCurrentlyCheckedIn) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "QUICK ATTENDANCE ACCESS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (isCurrentlyCheckedIn) "Active Study Session" else "Ready for Check-In",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                        )
                    }
                }

                // Live Indicator Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrentlyCheckedIn) Color(0xFF10B981).copy(alpha = 0.15f)
                    else Color(0xFF3B82F6).copy(alpha = 0.15f),
                    border = BorderStroke(
                        1.dp,
                        if (isCurrentlyCheckedIn) Color(0xFF10B981).copy(alpha = 0.4f)
                        else Color(0xFF3B82F6).copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isCurrentlyCheckedIn) Color(0xFF10B981) else Color(0xFF3B82F6))
                        )
                        Text(
                            text = if (isCurrentlyCheckedIn) "PUNCHED IN" else "IDLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = if (isCurrentlyCheckedIn) Color(0xFF047857) else Color(0xFF1D4ED8)
                            )
                        )
                    }
                }
            }

            // Real-Time Session Status Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isCurrentlyCheckedIn) Color(0xFF10B981).copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(
                    1.dp,
                    if (isCurrentlyCheckedIn) Color(0xFF10B981).copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCurrentlyCheckedIn) "In-Desk Duration Today" else "Today's Status",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                        if (isCurrentlyCheckedIn) {
                            Text(
                                text = if (elapsedHours > 0) "${elapsedHours}h ${elapsedRemMins}m studied" else "${elapsedRemMins} mins studied",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857)
                                )
                            )
                        } else {
                            Text(
                                text = if (checkOutTime.isNotBlank()) "Checked Out at $checkOutTime" else "Not punched in yet",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    if (checkInTime.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Punch-In Time",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = checkInTime,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Daily Study Goal Progress (6 Hours Target)
            val dailyTargetMinutes = 360f // 6 Hours
            val progressFraction = (elapsedMinutes / dailyTargetMinutes).coerceIn(0f, 1f)
            val progressPercent = (progressFraction * 100).toInt()

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Daily Goal: 6h Target",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        Text(
                            text = "$progressPercent% Complete",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (progressPercent >= 100) Color(0xFF059669) else MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (progressPercent >= 100) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }

            // Quick Access Buttons: Check-In & Check-Out Dual Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Check-In Button
                Button(
                    onClick = {
                        if (!isCurrentlyCheckedIn) {
                            showCheckInConfirmDialog = true
                        }
                    },
                    enabled = !isCurrentlyCheckedIn,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF059669).copy(alpha = 0.25f),
                        disabledContentColor = Color(0xFF059669).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("quick_checkin_button")
                ) {
                    Icon(
                        imageVector = if (isCurrentlyCheckedIn) Icons.Default.Check else Icons.Default.Login,
                        contentDescription = "Quick Check In",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = if (isCurrentlyCheckedIn) "Checked In ✓" else "Check-In",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Quick Check-Out Button
                Button(
                    onClick = {
                        if (isCurrentlyCheckedIn) {
                            showCheckOutConfirmDialog = true
                        }
                    },
                    enabled = isCurrentlyCheckedIn,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE11D48),
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("quick_checkout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Quick Check Out",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Check-Out",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick QR Scanner Camera Button
            OutlinedButton(
                onClick = onOpenQrScanner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("quick_open_qr_scanner_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scan Turnstile / Desk QR Code",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Check-In Confirmation Dialog
    if (showCheckInConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCheckInConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Instant Check-In",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Mark attendance check-in for Desk #${student.seatNumber.ifBlank { "Assigned Desk" }} (${student.shiftName.ifBlank { "Full Day" }}) right now?",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCheckInConfirmDialog = false
                        onQuickCheckIn()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Yes, Check In Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckInConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Check-Out Confirmation Dialog
    if (showCheckOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCheckOutConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Check-Out",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Are you leaving your study desk? This will record your punch-out timestamp and log your session duration.",
                        textAlign = TextAlign.Center
                    )
                    if (elapsedMinutes > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Today's Study Session: ${elapsedHours}h ${elapsedRemMins}m",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCheckOutConfirmDialog = false
                        onQuickCheckOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("Yes, Check Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckOutConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
