package com.example.ui.manager

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

    var isFetchingLocation by remember { mutableStateOf(false) }

    fun fetchAndOpenCurrentLocation() {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                Toast.makeText(context, "Location service not available", Toast.LENGTH_SHORT).show()
                return
            }

            isFetchingLocation = true
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                try {
                    val l = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                        bestLocation = l
                    }
                } catch (e: SecurityException) {
                    // Ignore individual provider exception
                }
            }

            if (bestLocation != null) {
                val lat = String.format(java.util.Locale.US, "%.6f", bestLocation.latitude)
                val lng = String.format(java.util.Locale.US, "%.6f", bestLocation.longitude)
                try {
                    val label = library?.name ?: "Library Location"
                    val mapUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                    context.startActivity(mapIntent)
                    Toast.makeText(context, "Current GPS: $lat, $lng (Opened in Maps)", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                }
            } else {
                Toast.makeText(context, "Turn on GPS to capture current location", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not get location: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        } finally {
            isFetchingLocation = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchAndOpenCurrentLocation()
        } else {
            Toast.makeText(context, "Location permission is needed to fetch current GPS coordinates", Toast.LENGTH_SHORT).show()
        }
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Independent Library Entity", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(library?.name ?: "Your Library", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("Code: ${library?.code ?: "LIB-101"} • ${library?.city ?: "Autonomous"}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "Standalone",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This library operates as an independent institution. Multi-branch creation is disabled to preserve standalone operations, dedicated student admissions, and isolated finance records.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
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
                    SettingInfoRow(icon = Icons.Default.Phone, label = "Contact Phone", value = library?.ownerPhone ?: "")
                    SettingInfoRow(icon = Icons.Default.Email, label = "Email", value = library?.ownerEmail ?: "admin@libdesk.cloud")
                    val addressDisplay = listOfNotNull(library?.address?.takeIf { it.isNotBlank() }, library?.city?.takeIf { it.isNotBlank() }, library?.state?.takeIf { it.isNotBlank() }).joinToString(", ")
                    SettingInfoRow(icon = Icons.Default.LocationOn, label = "Address", value = addressDisplay.ifBlank { "Not set" })
                    val hasGeo = (library?.latitude ?: 0.0) != 0.0 && (library?.longitude ?: 0.0) != 0.0
                    if (hasGeo) {
                        SettingInfoRow(icon = Icons.Default.Place, label = "Geo Location", value = "${library?.latitude}, ${library?.longitude} (Maps Linked ✓)")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            enabled = !isFetchingLocation,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isFetchingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Capturing...", fontSize = 11.5.sp)
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Current Location", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val query = listOfNotNull(
                                        library?.name?.takeIf { it.isNotBlank() },
                                        library?.address?.takeIf { it.isNotBlank() },
                                        library?.city?.takeIf { it.isNotBlank() },
                                        library?.state?.takeIf { it.isNotBlank() }
                                    ).joinToString(", ")
                                    val mapUri = if (hasGeo) {
                                        val lat = library?.latitude ?: 0.0
                                        val lng = library?.longitude ?: 0.0
                                        val name = library?.name ?: "Library"
                                        Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(name)})")
                                    } else if (query.isNotBlank()) {
                                        Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                                    } else {
                                        Uri.parse("geo:0,0?q=Library")
                                    }
                                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                    context.startActivity(mapIntent)
                                    Toast.makeText(context, "Opening Map...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    val lat = library?.latitude ?: 0.0
                                    val lng = library?.longitude ?: 0.0
                                    val webUrl = if (hasGeo) "https://maps.google.com/?q=$lat,$lng" else "https://maps.google.com"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Select on Map", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    SettingInfoRow(icon = Icons.Default.QrCode, label = "UPI Billing ID", value = library?.upiId ?: "")
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

        // Student Membership Plans & Offers (Managed by Library)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Student Plans & Offers",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Managed independently by this Library",
                                fontSize = 12.sp,
                                color = LibDeskColors.success,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = onEditInfrastructure,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manage", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your library configures student membership durations, fees, and promotional discount offers directly.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (plans.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "No student plans configured yet. Tap 'Manage' to add your library's first membership plan or promotional offer.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            plans.forEach { plan ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = plan.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        "${plan.durationMonths} ${if (plan.durationMonths == 1) "Month" else "Months"}",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (plan.facilities.isNotBlank()) {
                                                Text(
                                                    text = plan.facilities,
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "₹${plan.baseFee}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.5.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Active Offer",
                                                fontSize = 10.sp,
                                                color = LibDeskColors.success,
                                                fontWeight = FontWeight.SemiBold
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
                                // Toast.makeText(context, "System notification dispatched to notification tray!", Toast.LENGTH_SHORT).show()
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
