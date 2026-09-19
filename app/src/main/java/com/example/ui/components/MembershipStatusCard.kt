package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.StudentEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Membership alert severity levels for color-coded status styling.
 */
enum class MembershipAlertLevel {
    ACTIVE,
    EXPIRING_SOON,       // 4 to 7 days
    EXPIRING_CRITICAL,   // 1 to 3 days
    EXPIRING_TODAY,      // 0 days
    EXPIRED              // < 0 days or EXPIRED status
}

data class MembershipExpirationInfo(
    val level: MembershipAlertLevel,
    val daysRemaining: Int,
    val formattedExpiryDate: String,
    val formattedStartDate: String,
    val progressPercent: Float,
    val badgeLabel: String,
    val alertHeadline: String,
    val alertMessage: String,
    val primaryColor: Color,
    val containerColor: Color,
    val borderColor: Color,
    val icon: ImageVector,
    val isUrgent: Boolean
)

/**
 * Helper to calculate days remaining, expiry date formatting, and color-coded alert attributes.
 */
fun calculateMembershipExpiration(
    startDateStr: String,
    expiryDateStr: String,
    rawStatus: String = "ACTIVE"
): MembershipExpirationInfo {
    val isExplicitlyExpired = rawStatus.equals("EXPIRED", ignoreCase = true) ||
            rawStatus.equals("ARCHIVED", ignoreCase = true) ||
            rawStatus.equals("LEFT", ignoreCase = true)

    val todayCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // If expiryDateStr is blank, it means membership hasn't started yet (no seat assigned / fees not paid)
    if (expiryDateStr.isBlank()) {
        return MembershipExpirationInfo(
            level = MembershipAlertLevel.EXPIRED,
            daysRemaining = 0,
            alertHeadline = "Membership Not Started",
            alertMessage = "Please assign a seat and collect fee to start membership.",
            badgeLabel = "PENDING",
            isUrgent = false,
            primaryColor = EmeraldSecondaryText,
            containerColor = EmeraldSurfaceSoft,
            borderColor = EmeraldDivider,
            icon = Icons.Default.PendingActions,
            formattedStartDate = startDateStr.ifBlank { "N/A" },
            formattedExpiryDate = "Pending",
            progressPercent = 0.0f
        )
    }

    val formats = arrayOf("yyyy-MM-dd", "dd-MM-yyyy", "yyyy/MM/dd", "dd/MM/yyyy")
    var targetDate: Date? = null
    var parsedExpiryCal: Calendar? = null

    if (expiryDateStr.isNotBlank()) {
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                val parsed = sdf.parse(expiryDateStr.trim())
                if (parsed != null) {
                    targetDate = parsed
                    parsedExpiryCal = Calendar.getInstance().apply {
                        time = parsed
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    break
                }
            } catch (_: Exception) { }
        }
    }

    // Default to +30 days if blank or unparseable (Should not hit this often anymore)
    val expiryCal = parsedExpiryCal ?: Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 30)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val displayDateSdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedExpiry = if (targetDate != null) displayDateSdf.format(targetDate)
    else if (expiryDateStr.isNotBlank()) expiryDateStr
    else displayDateSdf.format(expiryCal.time)

    // Calculate days diff
    val diffMs = expiryCal.timeInMillis - todayCal.timeInMillis
    var daysRemaining = (diffMs / (1000 * 60 * 60 * 24)).toInt()

    if (isExplicitlyExpired && daysRemaining > 0) {
        daysRemaining = -1
    }

    // Start date parsing for progress bar calculation
    var startCal: Calendar? = null
    if (startDateStr.isNotBlank()) {
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                val parsed = sdf.parse(startDateStr.trim())
                if (parsed != null) {
                    startCal = Calendar.getInstance().apply {
                        time = parsed
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    break
                }
            } catch (_: Exception) { }
        }
    }

    val effectiveStartCal = startCal ?: Calendar.getInstance().apply {
        timeInMillis = expiryCal.timeInMillis - (30L * 24 * 60 * 60 * 1000)
    }
    val formattedStart = displayDateSdf.format(effectiveStartCal.time)

    // Progress calculation: fraction of validity period already consumed
    val totalPeriodMs = (expiryCal.timeInMillis - effectiveStartCal.timeInMillis).coerceAtLeast(1L)
    val elapsedMs = (todayCal.timeInMillis - effectiveStartCal.timeInMillis).coerceAtLeast(0L)
    val progressPercent = (elapsedMs.toFloat() / totalPeriodMs.toFloat()).coerceIn(0f, 1f)

    return when {
        isExplicitlyExpired || daysRemaining < 0 -> {
            val daysAgo = abs(daysRemaining)
            MembershipExpirationInfo(
                level = MembershipAlertLevel.EXPIRED,
                daysRemaining = daysRemaining,
                formattedExpiryDate = formattedExpiry,
                formattedStartDate = formattedStart,
                progressPercent = 1f,
                badgeLabel = if (daysAgo <= 1) "EXPIRED YESTERDAY" else "EXPIRED ${daysAgo}d AGO",
                alertHeadline = "Subscription Expired",
                alertMessage = "Your membership ended on $formattedExpiry. Seat reservation and library entry are temporarily suspended.",
                primaryColor = EmeraldError,
                containerColor = EmeraldErrorSoft,
                borderColor = EmeraldError.copy(alpha = 0.5f),
                icon = Icons.Default.Cancel,
                isUrgent = true
            )
        }
        daysRemaining == 0 -> {
            MembershipExpirationInfo(
                level = MembershipAlertLevel.EXPIRING_TODAY,
                daysRemaining = 0,
                formattedExpiryDate = formattedExpiry,
                formattedStartDate = formattedStart,
                progressPercent = 1f,
                badgeLabel = "EXPIRES TODAY",
                alertHeadline = "Last Day of Membership",
                alertMessage = "Your plan expires tonight at 11:59 PM. Renew now to avoid losing your reserved seat.",
                primaryColor = EmeraldError,
                containerColor = EmeraldErrorSoft,
                borderColor = EmeraldError.copy(alpha = 0.5f),
                icon = Icons.Default.Warning,
                isUrgent = true
            )
        }
        daysRemaining in 1..3 -> {
            MembershipExpirationInfo(
                level = MembershipAlertLevel.EXPIRING_CRITICAL,
                daysRemaining = daysRemaining,
                formattedExpiryDate = formattedExpiry,
                formattedStartDate = formattedStart,
                progressPercent = progressPercent,
                badgeLabel = if (daysRemaining == 1) "1 DAY LEFT" else "$daysRemaining DAYS LEFT",
                alertHeadline = "Expiring in $daysRemaining ${if (daysRemaining == 1) "Day" else "Days"}",
                alertMessage = "Critical alert: Only $daysRemaining day(s) left until $formattedExpiry. Renew early to retain your slot.",
                primaryColor = EmeraldWarning,
                containerColor = EmeraldWarningSoft,
                borderColor = EmeraldWarning.copy(alpha = 0.6f),
                icon = Icons.Default.HourglassTop,
                isUrgent = true
            )
        }
        daysRemaining in 4..7 -> {
            MembershipExpirationInfo(
                level = MembershipAlertLevel.EXPIRING_SOON,
                daysRemaining = daysRemaining,
                formattedExpiryDate = formattedExpiry,
                formattedStartDate = formattedStart,
                progressPercent = progressPercent,
                badgeLabel = "$daysRemaining DAYS REMAINING",
                alertHeadline = "Renewal Approaching",
                alertMessage = "Your plan is valid until $formattedExpiry ($daysRemaining days remaining). Avoid last-minute rush by renewing early.",
                primaryColor = EmeraldWarning,
                containerColor = EmeraldWarningSoft,
                borderColor = EmeraldWarning.copy(alpha = 0.5f),
                icon = Icons.Default.AccessTime,
                isUrgent = false
            )
        }
        else -> {
            MembershipExpirationInfo(
                level = MembershipAlertLevel.ACTIVE,
                daysRemaining = daysRemaining,
                formattedExpiryDate = formattedExpiry,
                formattedStartDate = formattedStart,
                progressPercent = progressPercent,
                badgeLabel = "ACTIVE • ${daysRemaining}d LEFT",
                alertHeadline = "Active Membership",
                alertMessage = "Valid until $formattedExpiry. You have $daysRemaining days of active library access.",
                primaryColor = EmeraldSuccess,
                containerColor = EmeraldSuccessSoft,
                borderColor = EmeraldSuccess.copy(alpha = 0.35f),
                icon = Icons.Default.CheckCircle,
                isUrgent = false
            )
        }
    }
}

/**
 * Dedicated Membership Status Card.
 * Prominently showcases plan name, expiration date, countdown of remaining days,
 * visual period progress, color-coded status alert banner, dues summary, and action CTAs.
 */
@Composable
fun MembershipStatusCard(
    planName: String,
    status: String,
    startDate: String,
    expiryDate: String,
    seatNumber: String? = null,
    shiftName: String? = null,
    hallName: String? = null,
    dueAmount: Double = 0.0,
    totalFee: Double = 0.0,
    studentName: String? = null,
    studentCode: String? = null,
    onRenew: (() -> Unit)? = null,
    onPayDues: (() -> Unit)? = null,
    onViewDetails: (() -> Unit)? = null,
    onSendReminder: (() -> Unit)? = null,
    showActions: Boolean = true,
    modifier: Modifier = Modifier
) {
    val expInfo = remember(startDate, expiryDate, status) {
        calculateMembershipExpiration(
            startDateStr = startDate,
            expiryDateStr = expiryDate,
            rawStatus = status
        )
    }

    val animatedProgress by animateFloatAsState(
        targetValue = expInfo.progressPercent,
        label = "membership_progress"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (expInfo.level) {
                MembershipAlertLevel.EXPIRED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                MembershipAlertLevel.EXPIRING_TODAY -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                MembershipAlertLevel.EXPIRING_CRITICAL -> LibDeskColors.warningSoft.copy(alpha = 0.3f)
                MembershipAlertLevel.EXPIRING_SOON -> LibDeskColors.warningSoft.copy(alpha = 0.2f)
                MembershipAlertLevel.ACTIVE -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(1.5.dp, expInfo.borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("membership_status_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Plan Name, Seat info & Status Alert Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = expInfo.primaryColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CardMembership,
                                    contentDescription = null,
                                    tint = expInfo.primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "MEMBERSHIP PLAN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                color = expInfo.primaryColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = planName.ifBlank { "Regular Study Plan" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val metaItems = listOfNotNull(
                        seatNumber?.takeIf { it.isNotBlank() }?.let { "Seat $it" },
                        shiftName?.takeIf { it.isNotBlank() },
                        hallName?.takeIf { it.isNotBlank() }
                    )
                    if (metaItems.isNotEmpty()) {
                        Text(
                            text = metaItems.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Alert Badge Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = expInfo.containerColor,
                    border = BorderStroke(1.dp, expInfo.borderColor),
                    modifier = Modifier.testTag("membership_alert_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = expInfo.icon,
                            contentDescription = null,
                            tint = expInfo.primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = expInfo.badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                color = expInfo.primaryColor
                            )
                        )
                    }
                }
            }

            // Expiration & Remaining Days Spotlight Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Expiration Date Details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "EXPIRATION DATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        Text(
                            text = expInfo.formattedExpiryDate,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )

                        if (startDate.isNotBlank()) {
                            Text(
                                text = "Started on ${expInfo.formattedStartDate}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    )

                    // Right Column: Remaining Days Highlight
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .padding(start = 14.dp)
                            .testTag("membership_days_counter")
                    ) {
                        Text(
                            text = when {
                                expInfo.daysRemaining < 0 -> "${abs(expInfo.daysRemaining)}"
                                expInfo.daysRemaining == 0 -> "0"
                                else -> "${expInfo.daysRemaining}"
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = expInfo.primaryColor,
                                lineHeight = 30.sp
                            )
                        )
                        Text(
                            text = when {
                                expInfo.daysRemaining < 0 -> "Days Overdue"
                                expInfo.daysRemaining == 0 -> "Days (Ends Today)"
                                expInfo.daysRemaining == 1 -> "Day Remaining"
                                else -> "Days Remaining"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = expInfo.primaryColor
                            )
                        )
                    }
                }
            }

            // Validity Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Validity Cycle",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = "${(animatedProgress * 100).toInt()}% elapsed",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = expInfo.primaryColor
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        expInfo.primaryColor.copy(alpha = 0.7f),
                                        expInfo.primaryColor
                                    )
                                )
                            )
                    )
                }
            }

            // Dedicated Color-Coded Alert Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = expInfo.containerColor.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, expInfo.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = expInfo.icon,
                        contentDescription = null,
                        tint = expInfo.primaryColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = expInfo.alertHeadline,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = expInfo.primaryColor
                            )
                        )
                        Text(
                            text = expInfo.alertMessage,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            // Due Payment Warning (if fee due exists)
            if (dueAmount > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LibDeskColors.warningSoft.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, LibDeskColors.warning.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = null,
                                tint = LibDeskColors.warning,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Pending Due: ₹${dueAmount.toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = LibDeskColors.warning
                                )
                            )
                        }

                        if (onPayDues != null) {
                            TextButton(
                                onClick = onPayDues,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(
                                    text = "Pay Now",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = LibDeskColors.warning
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            if (showActions && (onRenew != null || onViewDetails != null || onSendReminder != null)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRenew != null) {
                        Button(
                            onClick = onRenew,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (expInfo.isUrgent) expInfo.primaryColor else MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("renew_membership_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (expInfo.level == MembershipAlertLevel.EXPIRED) "Renew Now"
                                else "Extend Plan",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (onSendReminder != null) {
                        OutlinedButton(
                            onClick = onSendReminder,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, expInfo.borderColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = expInfo.primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remind",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = expInfo.primaryColor
                                )
                            )
                        }
                    }

                    if (onViewDetails != null) {
                        OutlinedButton(
                            onClick = onViewDetails,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(
                                text = "Details",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Convenience overload that accepts a StudentEntity directly.
 */
@Composable
fun MembershipStatusCard(
    student: StudentEntity,
    onRenew: (() -> Unit)? = null,
    onPayDues: (() -> Unit)? = null,
    onViewDetails: (() -> Unit)? = null,
    onSendReminder: (() -> Unit)? = null,
    showActions: Boolean = true,
    modifier: Modifier = Modifier
) {
    MembershipStatusCard(
        planName = student.planName,
        status = student.status,
        startDate = student.joiningDate,
        expiryDate = student.expiryDate,
        seatNumber = student.seatNumber,
        shiftName = student.shiftName,
        hallName = student.hallName,
        dueAmount = student.dueAmount,
        totalFee = student.totalFee,
        studentName = student.fullName,
        studentCode = student.studentCode,
        onRenew = onRenew,
        onPayDues = onPayDues,
        onViewDetails = onViewDetails,
        onSendReminder = onSendReminder,
        showActions = showActions,
        modifier = modifier
    )
}

/**
 * Compact color-coded alert badge for use in student lists, tables, and overview cards.
 */
@Composable
fun MembershipExpiryAlertBadge(
    expiryDate: String,
    status: String = "ACTIVE",
    modifier: Modifier = Modifier
) {
    val expInfo = remember(expiryDate, status) {
        calculateMembershipExpiration(
            startDateStr = "",
            expiryDateStr = expiryDate,
            rawStatus = status
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = expInfo.containerColor,
        border = BorderStroke(1.dp, expInfo.borderColor),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = expInfo.icon,
                contentDescription = null,
                tint = expInfo.primaryColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = expInfo.badgeLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = expInfo.primaryColor
                )
            )
        }
    }
}
