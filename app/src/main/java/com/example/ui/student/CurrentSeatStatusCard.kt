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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.*
import com.example.ui.theme.LibDeskColors

/**
 * Modern, high-craft Current Seat Status Card for Student Member Dashboard.
 *
 * Displays:
 * 1. User's currently allocated seat number and hall location.
 * 2. Live occupancy status (Occupied/Checked-In vs Reserved/Vacant) with session timer.
 * 3. Shift schedule and timing boundaries.
 * 4. Seat amenities (Power socket, Ergonomic chair, 5G Wi-Fi, Reading lamp, AC).
 * 5. Assigned locker info (if allocated).
 * 6. Quick navigation to Seat Matrix layout and Seat/Shift change request.
 */
@Composable
fun CurrentSeatStatusCard(
    student: StudentEntity,
    seat: SeatEntity?,
    hall: HallEntity?,
    shift: ShiftEntity?,
    assignedLocker: LockerEntity? = null,
    isCurrentlyCheckedIn: Boolean,
    checkInTime: String = "",
    onGoToSeatLayout: () -> Unit = {},
    onGoToSeatRequest: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val seatNumber = student.seatNumber.ifBlank { seat?.seatNumber ?: "Desk #01" }
    val hallName = student.hallName.ifBlank { hall?.name ?: seat?.hallName ?: "Main Study Hall" }
    val floorName = seat?.floor ?: hall?.floor ?: "Ground Floor"
    val shiftName = student.shiftName.ifBlank { shift?.name ?: "Full Day Shift" }
    val shiftTime = if (shift != null && shift.startTime.isNotBlank()) {
        "${shift.startTime} – ${shift.endTime}"
    } else "08:00 AM – 08:00 PM"

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.5.dp,
            if (isCurrentlyCheckedIn) Color(0xFF10B981).copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("current_seat_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Section Label + Live Seat Status Chip
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
                                imageVector = if (isCurrentlyCheckedIn) Icons.Default.EventSeat else Icons.Default.Chair,
                                contentDescription = null,
                                tint = if (isCurrentlyCheckedIn) Color(0xFF059669) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CURRENT SEAT STATUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "Desk #$seatNumber",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        )
                    }
                }

                // Live Status Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCurrentlyCheckedIn) Color(0xFF10B981).copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (isCurrentlyCheckedIn) Color(0xFF10B981).copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isCurrentlyCheckedIn) Color(0xFF10B981) else MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = if (isCurrentlyCheckedIn) "CHECKED IN • OCCUPIED" else "RESERVED • VACANT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = if (isCurrentlyCheckedIn) Color(0xFF059669) else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Session Note if checked in
            if (isCurrentlyCheckedIn && checkInTime.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Active session started at $checkInTime today. Your desk is secured.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Seat Location & Shift Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Zone / Hall Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hall / Zone",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = hallName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = floorName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Shift Schedule Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Shift Timings",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = shiftName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = shiftTime,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Optional Allocated Locker row if student has a locker
            if (assignedLocker != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Allocated Locker: ${assignedLocker.lockerNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFB45309)
                                )
                                Text(
                                    text = "Key #${assignedLocker.keyNumber.ifBlank { "K-${assignedLocker.lockerNumber}" }} • ${assignedLocker.floor}",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Desk Amenities Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DESK AMENITIES & FEATURES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AmenityChip(
                        icon = Icons.Default.Power,
                        label = "Power Socket",
                        modifier = Modifier.weight(1f)
                    )
                    AmenityChip(
                        icon = Icons.Default.Wifi,
                        label = "5G Wi-Fi",
                        modifier = Modifier.weight(1f)
                    )
                    AmenityChip(
                        icon = Icons.Default.AcUnit,
                        label = "Silent AC",
                        modifier = Modifier.weight(1f)
                    )
                    AmenityChip(
                        icon = Icons.Default.Lightbulb,
                        label = "Desk LED",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Access Action Buttons inside Seat Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onGoToSeatLayout,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View on Floor Map",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FilledTonalButton(
                    onClick = onGoToSeatRequest,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shift / Desk Change",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AmenityChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
