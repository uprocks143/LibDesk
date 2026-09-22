package com.example.ui.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.*
import com.example.ui.components.MembershipStatusCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.calculateMembershipExpiration
import com.example.ui.components.formatCurrency
import com.example.ui.theme.LibDeskColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern, high-craft Dashboard for Student Members.
 *
 * Displays:
 * 1. Quick-action hero button for QR code check-in & turnstile entry.
 * 2. Active memberships card with validity progress, fee breakdown, and renewal options.
 * 3. Upcoming seat reservations with hall, shift timings, amenities, and real-time status.
 * 4. At-a-glance study statistics and today's attendance log.
 */
@Composable
fun StudentMemberDashboard(
    student: StudentEntity,
    library: LibraryEntity?,
    attendanceList: List<AttendanceEntity>,
    seats: List<SeatEntity> = emptyList(),
    shifts: List<ShiftEntity> = emptyList(),
    plans: List<MembershipPlanEntity> = emptyList(),
    halls: List<HallEntity> = emptyList(),
    onOpenQrScanner: () -> Unit,
    onViewIdCard: (StudentEntity) -> Unit,
    onGoToFeeTab: () -> Unit,
    onGoToProfileTab: () -> Unit,
    onGoToSeatLayout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    val todayFormatted = remember {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    // Determine today's attendance status
    val todayAttendance = remember(attendanceList, todayDateStr, student.id) {
        attendanceList.find {
            it.studentId == student.id && (it.date == todayDateStr || it.date.startsWith(todayDateStr))
        }
    }
    val isCurrentlyCheckedIn = todayAttendance != null &&
            todayAttendance.status.equals("CHECKED_IN", ignoreCase = true) &&
            todayAttendance.checkOutTime.isBlank()

    // Match assigned seat, hall, shift, and plan
    val assignedSeat = remember(seats, student) {
        if (student.seatId.isNotBlank()) {
            seats.find { it.id == student.seatId }
        } else if (student.seatNumber.isNotBlank()) {
            seats.find { it.seatNumber.equals(student.seatNumber, ignoreCase = true) }
        } else null
    }

    val assignedHall = remember(halls, assignedSeat, student) {
        if (assignedSeat != null && assignedSeat.hallId.isNotBlank()) {
            halls.find { it.id == assignedSeat.hallId }
        } else if (student.hallName.isNotBlank()) {
            halls.find { it.name.equals(student.hallName, ignoreCase = true) }
        } else halls.firstOrNull()
    }

    val assignedShift = remember(shifts, student) {
        if (student.shiftId.isNotBlank()) {
            shifts.find { it.id == student.shiftId }
        } else if (student.shiftName.isNotBlank()) {
            shifts.find { it.name.equals(student.shiftName, ignoreCase = true) }
        } else shifts.firstOrNull()
    }

    val assignedPlan = remember(plans, student) {
        if (student.planId.isNotBlank()) {
            plans.find { it.id == student.planId }
        } else if (student.planName.isNotBlank()) {
            plans.find { it.name.equals(student.planName, ignoreCase = true) }
        } else plans.firstOrNull()
    }

    val expirationInfo = remember(student.joiningDate, student.expiryDate, student.status) {
        calculateMembershipExpiration(student.joiningDate, student.expiryDate, student.status)
    }

    // Monthly attendance count
    val monthAttendanceCount = remember(attendanceList, student.id) {
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        attendanceList.count {
            it.studentId == student.id && it.date.startsWith(currentMonthPrefix)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("student_member_dashboard"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Today's Briefing & Status Overview
        item {
            StudentTodayBriefingCard(
                student = student,
                todayFormatted = todayFormatted,
                seatNumber = student.seatNumber.ifBlank { assignedSeat?.seatNumber ?: "Allocated Desk" },
                daysRemaining = expirationInfo.daysRemaining,
                isCurrentlyCheckedIn = isCurrentlyCheckedIn
            )
        }

        // 2. Quick-Action QR Code Check-In Hub (Hero Element)
        item {
            StudentQuickActionQrCard(
                seatNumber = student.seatNumber.ifBlank { assignedSeat?.seatNumber ?: "Allocated Desk" },
                shiftName = student.shiftName.ifBlank { assignedShift?.name ?: "Regular Shift" },
                isCurrentlyCheckedIn = isCurrentlyCheckedIn,
                checkInTime = todayAttendance?.checkInTime.orEmpty(),
                onOpenQrScanner = onOpenQrScanner,
                onViewIdCard = { onViewIdCard(student) }
            )
        }

        // 3. Upcoming Seat Reservation Card
        item {
            StudentUpcomingSeatReservationCard(
                student = student,
                seat = assignedSeat,
                hall = assignedHall,
                shift = assignedShift,
                isCurrentlyCheckedIn = isCurrentlyCheckedIn,
                onOpenQrScanner = onOpenQrScanner,
                onGoToSeatLayout = onGoToSeatLayout
            )
        }

        // 4. Active Membership Card with Validity Progress & Fee Status
        item {
            StudentActiveMembershipCard(
                student = student,
                plan = assignedPlan,
                expirationInfo = expirationInfo,
                onRenewOrPay = onGoToFeeTab
            )
        }

        // 5. At-a-Glance Attendance & Study Activity
        item {
            StudentStudyActivityCard(
                todayAttendance = todayAttendance,
                monthAttendanceCount = monthAttendanceCount,
                recentAttendance = attendanceList.filter { it.studentId == student.id }.take(3)
            )
        }
    }
}

/**
 * Top Today Briefing Card with calendar date, presence status, and 3 quick stat indicators.
 */
@Composable
private fun StudentTodayBriefingCard(
    student: StudentEntity,
    todayFormatted: String,
    seatNumber: String,
    daysRemaining: Int,
    isCurrentlyCheckedIn: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = todayFormatted,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isCurrentlyCheckedIn) LibDeskColors.successSoft else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isCurrentlyCheckedIn) "In Library" else "Out of Library",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isCurrentlyCheckedIn) LibDeskColors.success else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick chip 1: Desk
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Assigned Desk",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "Desk #$seatNumber",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 1
                        )
                    }
                }

                // Quick chip 2: Membership Validity
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Membership",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = if (daysRemaining > 0) "$daysRemaining Days Left" else "Expired",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (daysRemaining > 5) LibDeskColors.success else MaterialTheme.colorScheme.error
                            ),
                            maxLines = 1
                        )
                    }
                }

                // Quick chip 3: Attendance status
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Today's Status",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = if (isCurrentlyCheckedIn) "Present" else "Not Marked",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentlyCheckedIn) LibDeskColors.success else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Prominent Quick-Action QR Code Check-In Hero Card.
 * Allows student to scan QR code directly for desk check-in or gate entrance.
 */
@Composable
private fun StudentQuickActionQrCard(
    seatNumber: String,
    shiftName: String,
    isCurrentlyCheckedIn: Boolean,
    checkInTime: String,
    onOpenQrScanner: () -> Unit,
    onViewIdCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.5.dp,
            if (isCurrentlyCheckedIn) LibDeskColors.success.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row with Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isCurrentlyCheckedIn) LibDeskColors.successSoft
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyCheckedIn) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = if (isCurrentlyCheckedIn) LibDeskColors.success else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SEAT & GATE ACCESS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (isCurrentlyCheckedIn) "Checked In at Desk #$seatNumber" else "Quick QR Check-In",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Status Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrentlyCheckedIn) LibDeskColors.successSoft else LibDeskColors.infoSoft,
                    border = BorderStroke(
                        1.dp,
                        if (isCurrentlyCheckedIn) LibDeskColors.success.copy(alpha = 0.4f)
                        else LibDeskColors.info.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isCurrentlyCheckedIn) LibDeskColors.success else LibDeskColors.info)
                        )
                        Text(
                            text = if (isCurrentlyCheckedIn) "ACTIVE SESSION" else "READY TO SCAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = if (isCurrentlyCheckedIn) LibDeskColors.success else LibDeskColors.info
                            )
                        )
                    }
                }
            }

            // Information block
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Allocated Desk",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = "Desk #$seatNumber",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column {
                        Text(
                            text = "Shift Schedule",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = shiftName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isCurrentlyCheckedIn && checkInTime.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        Column {
                            Text(
                                text = "Entry Time",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = checkInTime,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = LibDeskColors.success
                                )
                            )
                        }
                    }
                }
            }

            // Big Action Button for QR Scanner
            Button(
                onClick = onOpenQrScanner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentlyCheckedIn) LibDeskColors.success
                    else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("student_quick_qr_checkin_button")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isCurrentlyCheckedIn) "Scan QR to Check Out / Switch Desk" else "Scan QR Code to Check In",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick secondary hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Point camera at your desk QR code or turnstile scanner",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                )
            }
        }
    }
}

/**
 * Upcoming Seat Reservation Card displaying allocated seat, hall name, shift timings,
 * amenities (AC, Wi-Fi, Charging Point), and live reservation status.
 */
@Composable
private fun StudentUpcomingSeatReservationCard(
    student: StudentEntity,
    seat: SeatEntity?,
    hall: HallEntity?,
    shift: ShiftEntity?,
    isCurrentlyCheckedIn: Boolean,
    onOpenQrScanner: () -> Unit,
    onGoToSeatLayout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val seatNumber = student.seatNumber.ifBlank { seat?.seatNumber ?: "A-14" }
    val hallName = student.hallName.ifBlank { hall?.name ?: seat?.hallName ?: "Main Study Hall" }
    val floor = seat?.floor ?: hall?.floor ?: "Ground Floor"
    val shiftName = student.shiftName.ifBlank { shift?.name ?: "Full Day Shift" }
    val shiftTime = if (shift != null && shift.startTime.isNotBlank()) {
        "${shift.startTime} – ${shift.endTime}"
    } else "08:00 AM – 08:00 PM"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chair,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "UPCOMING SEAT RESERVATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = "Reserved Desk #$seatNumber",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LibDeskColors.successSoft,
                    border = BorderStroke(1.dp, LibDeskColors.success.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (isCurrentlyCheckedIn) "OCCUPIED" else "CONFIRMED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = LibDeskColors.success,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Hall & Shift Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Location info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Hall / Zone",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = hallName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = floor,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Shift timing info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Shift Slot",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = shiftName,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = shiftTime,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Amenities Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AmenityChip(icon = Icons.Default.AcUnit, label = "Silent AC")
                AmenityChip(icon = Icons.Default.Power, label = "Power Socket")
                AmenityChip(icon = Icons.Default.Wifi, label = "5G Wi-Fi")
                AmenityChip(icon = Icons.Default.Lightbulb, label = "Desk Lamp")
            }

            // Action to view branch floor map
            OutlinedButton(
                onClick = onGoToSeatLayout,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_view_seat_layout_btn"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Branch Floor Layout & Available Seats",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

/**
 * Clean chip for seat amenities.
 */
@Composable
private fun AmenityChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

/**
 * Active Membership Card displaying current membership plan, validity countdown,
 * progress bar, fees status (paid vs dues), and renewal button.
 */
@Composable
private fun StudentActiveMembershipCard(
    student: StudentEntity,
    plan: MembershipPlanEntity?,
    expirationInfo: com.example.ui.components.MembershipExpirationInfo,
    onRenewOrPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val planTitle = student.planName.ifBlank { plan?.name ?: "Standard Monthly Membership" }
    val daysRemaining = expirationInfo.daysRemaining
    val progressPercent = expirationInfo.progressPercent
    val isDuesPending = student.dueAmount > 0.0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ACTIVE MEMBERSHIP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = planTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                StatusBadge(status = student.status)
            }

            // Validity Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = expirationInfo.primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (daysRemaining > 0) "$daysRemaining Days Remaining" else "Membership Expired",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = expirationInfo.primaryColor
                            )
                        )
                    }

                    Text(
                        text = "Valid: ${student.joiningDate.ifBlank { "N/A" }} to ${student.expiryDate.ifBlank { "N/A" }}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }

                LinearProgressIndicator(
                    progress = { (1f - (progressPercent / 100f)).coerceIn(0.05f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = expirationInfo.primaryColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Fee Summary Row
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Plan Fee",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = formatCurrency(student.totalFee),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column {
                        Text(
                            text = "Amount Paid",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = formatCurrency(student.paidAmount),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = LibDeskColors.success
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column {
                        Text(
                            text = "Pending Dues",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                        Text(
                            text = if (isDuesPending) formatCurrency(student.dueAmount) else "Nil (Paid)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDuesPending) MaterialTheme.colorScheme.error else LibDeskColors.success
                            )
                        )
                    }
                }
            }

            // Action Button for Renewal / Dues
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onRenewOrPay,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isDuesPending) "Pay Pending Dues" else "Renew / Upgrade Plan",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * At-a-glance study statistics and today's attendance log.
 */
@Composable
private fun StudentStudyActivityCard(
    todayAttendance: AttendanceEntity?,
    monthAttendanceCount: Int,
    recentAttendance: List<AttendanceEntity>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "STUDY ATTENDANCE LOG",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = "Monthly Streak: $monthAttendanceCount Days",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "$monthAttendanceCount sessions",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (recentAttendance.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No recent sessions logged yet.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentAttendance.forEach { att ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = LibDeskColors.success,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Date: ${att.date}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "In: ${att.checkInTime} ${if (att.checkOutTime.isNotBlank()) "• Out: ${att.checkOutTime}" else "• Active"}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (att.checkOutTime.isBlank() && att.status == "CHECKED_IN") LibDeskColors.successSoft
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (att.checkOutTime.isBlank() && att.status == "CHECKED_IN") "Active" else "Completed",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (att.checkOutTime.isBlank() && att.status == "CHECKED_IN") LibDeskColors.success
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
