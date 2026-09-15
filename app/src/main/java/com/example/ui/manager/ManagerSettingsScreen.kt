package com.example.ui.manager

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.*
import com.example.notification.DuesNotificationHelper
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ManagerSettingsScreen(
    library: LibraryEntity?,
    allLibraries: List<LibraryEntity>,
    halls: List<HallEntity>,
    shifts: List<ShiftEntity>,
    plans: List<MembershipPlanEntity>,
    students: List<StudentEntity> = emptyList(),
    onSelectLibrary: (String) -> Unit,
    onCreateLibrary: () -> Unit,
    onOpenSyncBackup: () -> Unit,
    onEditProfile: () -> Unit = {},
    onEditInfrastructure: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showTenantSwitchDialog by remember { mutableStateOf(false) }

    var isDailyAlertsActive by remember {
        mutableStateOf(DuesNotificationHelper.isDailyAlertEnabled(context))
    }
    var alertThresholdDays by remember {
        mutableStateOf(DuesNotificationHelper.getThresholdDays(context))
    }
    val alertTime = remember {
        DuesNotificationHelper.getAlertTime(context)
    }

    val pendingAlertCount = remember(students, alertThresholdDays) {
        DuesNotificationHelper.getExpiringAndDueStudents(students, alertThresholdDays).size
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Active Library Tenant", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(library?.name ?: "Vanguard Library", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Code: ${library?.code ?: "VAN-101"}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(
                            onClick = { showTenantSwitchDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SwapCalls, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Switch / Add", fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Library Profile & Billing", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        TextButton(
                            onClick = onEditProfile,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Profile", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingInfoRow(icon = Icons.Default.Person, label = "Owner / Incharge", value = library?.ownerName ?: "Library Manager")
                    SettingInfoRow(icon = Icons.Default.Phone, label = "Contact Phone", value = library?.ownerPhone ?: "+91 98765 43210")
                    SettingInfoRow(icon = Icons.Default.Email, label = "Email", value = library?.ownerEmail ?: "admin@libdesk.cloud")
                    SettingInfoRow(icon = Icons.Default.LocationOn, label = "Address", value = "${library?.address ?: ""}, ${library?.city ?: ""}")
                    SettingInfoRow(icon = Icons.Default.QrCode, label = "UPI Billing ID", value = library?.upiId ?: "vanguard@icici")
                }
            }
        }

        
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Infrastructure & Batches", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        TextButton(onClick = onEditInfrastructure) {
                            Text("Edit")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Halls / Reading Areas (${halls.size}):", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    halls.forEach { hall ->
                        Text("• ${hall.name} (${hall.floor}) — ${hall.seatCount} seats", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Active Shifts & Batches (${shifts.size}):", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    shifts.forEach { shift ->
                        Text("• ${shift.name} (${shift.startTime} - ${shift.endTime}) — ₹${shift.fee}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Membership Plans (${plans.size}):", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    plans.forEach { plan ->
                        Text("• ${plan.name} (${plan.durationMonths} Mo) — ₹${plan.baseFee}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        
        
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Backup Center",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Backup & Restore",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Keep your LibDesk data safe",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onOpenSyncBackup,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Backup Center", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isDailyAlertsActive) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .background(if (isDailyAlertsActive) MaterialTheme.colorScheme.primaryContainer else Color.LightGray.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDailyAlertsActive) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (isDailyAlertsActive) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Daily Dues & Expiry Alerts",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isDailyAlertsActive) "Scheduled daily at ${String.format("%02d:%02d AM", alertTime.first, alertTime.second)}" else "Alerts disabled",
                                    fontSize = 14.sp,
                                    color = if (isDailyAlertsActive) LibDeskColors.success else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isDailyAlertsActive,
                            onCheckedChange = { checked ->
                                isDailyAlertsActive = checked
                                DuesNotificationHelper.setDailyAlertEnabled(context, checked)
                                Toast.makeText(
                                    context,
                                    if (checked) "Daily dues alert scheduled for 9:00 AM" else "Daily dues alerts disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Triggers local Android system alerts for library admins to follow up with students whose memberships are expiring or have pending balance dues.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    if (isDailyAlertsActive) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Alert Window (Days Before Expiry):",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 3, 5, 7).forEach { days ->
                                val isSelected = alertThresholdDays == days
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        alertThresholdDays = days
                                        DuesNotificationHelper.setThresholdDays(context, days)
                                    },
                                    label = {
                                        Text(
                                            text = "$days ${if (days == 1) "Day" else "Days"}",
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (pendingAlertCount > 0) LibDeskColors.warning.copy(alpha = 0.15f) else LibDeskColors.successSoft,
                            border = BorderStroke(1.dp, if (pendingAlertCount > 0) LibDeskColors.warning.copy(alpha = 0.5f) else LibDeskColors.success.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (pendingAlertCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (pendingAlertCount > 0) LibDeskColors.warning else LibDeskColors.success,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (pendingAlertCount > 0) "$pendingAlertCount student(s) currently match your $alertThresholdDays-day follow-up criteria." else "All students are currently clear of dues and expiration.",
                                    fontSize = 14.sp,
                                    color = if (pendingAlertCount > 0) LibDeskColors.warning else LibDeskColors.success,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                DuesNotificationHelper.triggerDuesNotification(
                                    context = context,
                                    students = students,
                                    thresholdDays = alertThresholdDays,
                                    isTest = true
                                )
                                Toast.makeText(context, "System notification dispatched to notification tray!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test System Alert Notification Now", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showTenantSwitchDialog) {
        BackHandler { showTenantSwitchDialog = false }
        Dialog(
            onDismissRequest = { showTenantSwitchDialog = false },
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
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Switch Library Branch / Tenant", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(12.dp))

                    allLibraries.forEach { lib ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = if (lib.id == library?.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectLibrary(lib.id)
                                    showTenantSwitchDialog = false
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(lib.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Code: ${lib.code} • ${lib.city}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            showTenantSwitchDialog = false
                            onCreateLibrary()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.AddBusiness, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Register New Library / Branch")
                    }
                }
            }
        }
    }


}

@Composable
private fun SettingInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
