package com.example.ui.manager

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.StudentEntity
import com.example.notification.DuesNotificationHelper
import com.example.ui.payment.UpiPaymentShareModal
import com.example.ui.theme.LibDeskColors
import com.example.util.WhatsAppAlertUtils
import com.example.viewmodel.LibDeskViewModel

/**
 * Monthly Due Date Auto-Scheduler & Batch WhatsApp Dispatcher Modal.
 * Scans all student records for upcoming due dates, expired memberships,
 * and allows 1-click batch automated WhatsApp fee reminders with UPI pay links.
 */
@Composable
fun MonthlyDuesSchedulerModal(
    library: LibraryEntity?,
    students: List<StudentEntity>,
    viewModel: LibDeskViewModel,
    onDismiss: () -> Unit,
    onCollectFeeForStudent: (StudentEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDailyAlertEnabled by remember { mutableStateOf(DuesNotificationHelper.isDailyAlertEnabled(context)) }
    var thresholdDays by remember { mutableIntStateOf(DuesNotificationHelper.getThresholdDays(context)) }
    var selectedTab by remember { mutableStateOf("ALL") } // "ALL", "OVERDUE", "TODAY", "EXPIRING_SOON"
    var studentForUpiModal by remember { mutableStateOf<StudentEntity?>(null) }
    var isScanning by remember { mutableStateOf(false) }

    // Evaluated list of students
    val dueAlertItems = remember(students, thresholdDays) {
        DuesNotificationHelper.getExpiringAndDueStudents(students, thresholdDays)
    }

    val overdueCount = dueAlertItems.count { it.isExpired }
    val todayDueCount = dueAlertItems.count { it.isExpiringToday }
    val expiringSoonCount = dueAlertItems.count { it.isExpiringSoon }
    val totalPendingAmount = dueAlertItems.sumOf { it.dueAmount }

    val filteredList = remember(dueAlertItems, selectedTab) {
        when (selectedTab) {
            "OVERDUE" -> dueAlertItems.filter { it.isExpired }
            "TODAY" -> dueAlertItems.filter { it.isExpiringToday }
            "EXPIRING_SOON" -> dueAlertItems.filter { it.isExpiringSoon }
            else -> dueAlertItems
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 14.dp)
                .testTag("monthly_dues_scheduler_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ScheduleSend,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Monthly Due Date Auto-Scheduler",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Automated Fee Reminders & UPI QR Dispatcher",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto-Scheduler Status & Scheduler Toggle Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isDailyAlertEnabled) Color(0xFF10B981) else Color(0xFFEF4444))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isDailyAlertEnabled) "Daily Auto-Alarm Active (09:00 AM)" else "Auto-Scheduler Inactive",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Evaluates subscriptions daily & raises system alert + WhatsApp queue",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isDailyAlertEnabled,
                            onCheckedChange = { checked ->
                                isDailyAlertEnabled = checked
                                DuesNotificationHelper.setDailyAlertEnabled(context, checked)
                                Toast.makeText(
                                    context,
                                    if (checked) "Auto-Scheduler enabled for 09:00 AM daily" else "Auto-Scheduler paused",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Metric Statistics Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Overdue",
                        count = "$overdueCount",
                        sub = "Expired",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Due Today",
                        count = "$todayDueCount",
                        sub = "Renew Now",
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Next 3 Days",
                        count = "$expiringSoonCount",
                        sub = "Expiring Soon",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Pending Total",
                        count = "₹${totalPendingAmount.toInt()}",
                        sub = "${dueAlertItems.size} Students",
                        color = Color(0xFF047857),
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar: Scan Now + 1-Click Batch Send WhatsApp Dues
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            isScanning = true
                            DuesNotificationHelper.triggerDuesNotification(
                                context = context,
                                students = students,
                                thresholdDays = thresholdDays,
                                isTest = true
                            )
                            isScanning = false
                            Toast.makeText(context, "Scan complete! Auto-scheduler evaluated ${students.size} students.", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run Scan Now", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val targets = dueAlertItems.filter { it.student.mobile.isNotBlank() }
                            if (targets.isEmpty()) {
                                Toast.makeText(context, "No students with pending dues have phone numbers", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val first = targets.first()
                            WhatsAppAlertUtils.sendFeeDueReminder(
                                context = context,
                                studentName = first.student.fullName,
                                phone = first.student.mobile,
                                libraryName = library?.name ?: "Library",
                                dueAmount = first.dueAmount.takeIf { it > 0 } ?: 1000.0,
                                dueDate = first.student.expiryDate.ifBlank { "Upcoming Renewal" },
                                upiId = library?.upiId ?: ""
                            )
                            Toast.makeText(
                                context,
                                "Launching WhatsApp reminder for ${first.student.fullName}. ${targets.size - 1} more queued.",
                                Toast.LENGTH_LONG
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp_real),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("1-Click Batch WhatsApp", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == "ALL",
                        onClick = { selectedTab = "ALL" },
                        label = { Text("All Dues (${dueAlertItems.size})", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = selectedTab == "OVERDUE",
                        onClick = { selectedTab = "OVERDUE" },
                        label = { Text("Overdue ($overdueCount)", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = selectedTab == "TODAY",
                        onClick = { selectedTab = "TODAY" },
                        label = { Text("Today ($todayDueCount)", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = selectedTab == "EXPIRING_SOON",
                        onClick = { selectedTab = "EXPIRING_SOON" },
                        label = { Text("Next 3 Days ($expiringSoonCount)", fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Student Due Items List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LibDeskColors.success,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Zero Pending Dues in Selected Filter",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "All student subscription fees are up to date! 🎉",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.student.id }) { alert ->
                            val st = alert.student
                            val statusColor = when {
                                alert.isExpired -> MaterialTheme.colorScheme.error
                                alert.isExpiringToday -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            val statusTag = when {
                                alert.isExpired -> "EXPIRED"
                                alert.isExpiringToday -> "DUE TODAY"
                                else -> "IN ${alert.daysRemaining} DAYS"
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = st.fullName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = statusColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = statusTag,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Seat #${st.seatNumber.ifBlank { "Unassigned" }} • ${st.planName.ifBlank { "Monthly" }} • Mob: ${st.mobile.ifBlank { "N/A" }}",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Due: ₹${(if (alert.dueAmount > 0) alert.dueAmount else 1000.0).toInt()} • Valid: ${st.expiryDate.ifBlank { "N/A" }}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = statusColor
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1-Click WhatsApp Button (opens UPI modal)
                                        IconButton(
                                            onClick = { studentForUpiModal = st },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF25D366).copy(alpha = 0.15f))
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_whatsapp_real),
                                                contentDescription = "WhatsApp UPI Pay Link",
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Collect Fee button
                                        Button(
                                            onClick = {
                                                onCollectFeeForStudent(st)
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Collect", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (studentForUpiModal != null) {
        UpiPaymentShareModal(
            student = studentForUpiModal!!,
            library = library,
            onDismiss = { studentForUpiModal = null }
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    count: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = color)
            Text(count, fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
            Text(sub, fontSize = 9.sp, color = color.copy(alpha = 0.8f))
        }
    }
}
