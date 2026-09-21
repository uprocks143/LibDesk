package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.NoticeEntity
import com.example.data.local.entities.StudentEntity
import com.example.notification.DuesNotificationHelper
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun NotificationsAlertsModal(
    students: List<StudentEntity>,
    notices: List<NoticeEntity>,
    currentRole: String = "MANAGER",
    initialTab: Int = 0,
    onClose: () -> Unit,
    onOpenPostNotice: (() -> Unit)? = null,
    onRecordPayment: ((StudentEntity) -> Unit)? = null,
    onSelectStudent: ((StudentEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableStateOf(initialTab) } 
    var dismissedAlertIds by remember { mutableStateOf(setOf<String>()) }

    
    val renewalAlerts = remember(students) {
        students.filter { student ->
            student.status.equals("EXPIRED", ignoreCase = true) ||
            student.dueAmount > 0 ||
            student.expiryDate.isNotBlank()
        }.sortedWith(
            compareByDescending<StudentEntity> { it.status.equals("EXPIRED", ignoreCase = true) }
                .thenByDescending { it.dueAmount }
        )
    }

    val activeNotices = remember(notices) {
        notices.sortedWith(
            compareByDescending<NoticeEntity> { it.priority == "URGENT" }
                .thenByDescending { it.priority == "HIGH" }
        )
    }

    val totalAlertsCount = (renewalAlerts.size + activeNotices.size) - dismissedAlertIds.size

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                Surface(
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Alerts & Notifications",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = Color.White
                                        )
                                    )
                                    if (totalAlertsCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.error
                                        ) {
                                            Text(
                                                text = "$totalAlertsCount new",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Membership renewals & library announcements",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "All (${renewalAlerts.size + activeNotices.size})",
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Renewals (${renewalAlerts.size})",
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                text = "Notices (${activeNotices.size})",
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }

                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {

                        if (selectedTab == 0 || selectedTab == 1) {
                            if (renewalAlerts.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "UPCOMING RENEWALS & DUES",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "${renewalAlerts.size} Alerts",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                items(renewalAlerts, key = { "renewal_${it.id}" }) { student ->
                                    val isExpired = student.status.equals("EXPIRED", ignoreCase = true)
                                    val hasDues = student.dueAmount > 0
                                    val isDismissed = dismissedAlertIds.contains("renewal_${student.id}")

                                    if (!isDismissed) {
                                        RenewalAlertCard(
                                            student = student,
                                            isExpired = isExpired,
                                            hasDues = hasDues,
                                            onSendReminder = {
                                                val msg = "Dear ${student.fullName}, your library seat membership (${student.seatNumber.ifBlank { "Seat" }}) at Vanguard Study Hall is due for renewal. Expiry: ${student.expiryDate}. Due: ₹${student.dueAmount.toInt()}. Kindly renew to secure your seat."
                                                clipboardManager.setText(AnnotatedString(msg))
                                                // Toast.makeText(context, "Reminder copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            onPayOrRenew = {
                                                onRecordPayment?.invoke(student)
                                                onClose()
                                            },
                                            onDismiss = {
                                                dismissedAlertIds = dismissedAlertIds + "renewal_${student.id}"
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        
                        if (selectedTab == 0 || selectedTab == 2) {
                            if (activeNotices.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "LIBRARY ANNOUNCEMENTS",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            letterSpacing = 0.5.sp
                                        )
                                        if (currentRole == "MANAGER" && onOpenPostNotice != null) {
                                            TextButton(
                                                onClick = {
                                                    onClose()
                                                    onOpenPostNotice()
                                                },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Post Notice", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                items(activeNotices, key = { "notice_${it.id}" }) { notice ->
                                    val isDismissed = dismissedAlertIds.contains("notice_${notice.id}")
                                    if (!isDismissed) {
                                        AnnouncementAlertCard(
                                            notice = notice,
                                            onDismiss = {
                                                dismissedAlertIds = dismissedAlertIds + "notice_${notice.id}"
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        
                        if ((selectedTab == 1 && renewalAlerts.isEmpty()) ||
                            (selectedTab == 2 && activeNotices.isEmpty()) ||
                            (selectedTab == 0 && renewalAlerts.isEmpty() && activeNotices.isEmpty()) ||
                            (totalAlertsCount <= 0)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(LibDeskColors.successSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = LibDeskColors.success,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "All Caught Up!",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "No pending membership renewals or urgent announcements.",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val allIds = renewalAlerts.map { "renewal_${it.id}" } + activeNotices.map { "notice_${it.id}" }
                                dismissedAlertIds = allIds.toSet()
                                // Toast.makeText(context, "All alerts marked as reviewed", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mark All Read", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenewalAlertCard(
    student: StudentEntity,
    isExpired: Boolean,
    hasDues: Boolean,
    onSendReminder: () -> Unit,
    onPayOrRenew: () -> Unit,
    onDismiss: () -> Unit
) {
    val borderColor = if (isExpired) MaterialTheme.colorScheme.error else if (hasDues) LibDeskColors.warning else MaterialTheme.colorScheme.onPrimaryContainer
    val containerColor = if (isExpired) MaterialTheme.colorScheme.errorContainer else if (hasDues) LibDeskColors.warningSoft else MaterialTheme.colorScheme.primaryContainer

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isExpired) MaterialTheme.colorScheme.errorContainer else LibDeskColors.warningSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpired) Icons.Default.Cancel else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (isExpired) MaterialTheme.colorScheme.error else LibDeskColors.warning,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Seat: ${student.seatNumber.ifBlank { "Unassigned" }} • ${student.planName.ifBlank { "Standard" }}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isExpired) MaterialTheme.colorScheme.error else LibDeskColors.warning
                ) {
                    Text(
                        text = if (isExpired) "EXPIRED" else "RENEWAL DUE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Expiry Date", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurface)
                        val fallbackDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
                        Text(student.expiryDate.ifBlank { fallbackDate }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    if (student.dueAmount > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Pending Due", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("₹${student.dueAmount.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSendReminder,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remind", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onPayOrRenew,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Renew / Pay", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AnnouncementAlertCard(
    notice: NoticeEntity,
    onDismiss: () -> Unit
) {
    val isUrgent = notice.priority == "URGENT"
    val isHigh = notice.priority == "HIGH"
    val accentColor = if (isUrgent) MaterialTheme.colorScheme.error else if (isHigh) LibDeskColors.warning else MaterialTheme.colorScheme.onPrimaryContainer
    val containerBg = if (isUrgent) MaterialTheme.colorScheme.errorContainer else if (isHigh) LibDeskColors.warningSoft else MaterialTheme.colorScheme.primaryContainer

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUrgent) Icons.Default.Campaign else Icons.Default.Announcement,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notice.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${notice.category} • ${notice.date}",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor
                ) {
                    Text(
                        text = notice.priority,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notice.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )
        }
    }
}
