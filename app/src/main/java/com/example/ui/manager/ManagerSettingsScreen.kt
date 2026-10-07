package com.example.ui.manager

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.*
import com.example.notification.DuesNotificationHelper
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerSettingsScreen(
    library: LibraryEntity?,
    allLibraries: List<LibraryEntity> = emptyList(),
    halls: List<HallEntity> = emptyList(),
    shifts: List<ShiftEntity> = emptyList(),
    plans: List<MembershipPlanEntity> = emptyList(),
    students: List<StudentEntity> = emptyList(),
    superAdminProfile: SuperAdminUserEntity? = null,
    initialTab: Int = 0,
    isSupabaseSyncing: Boolean = false,
    supabaseStatusMessage: String? = null,
    onSelectLibrary: (String) -> Unit = {},
    onCreateLibrary: () -> Unit = {},
    onUpdateLibrary: (LibraryEntity) -> Unit = {},
    onAddHall: (name: String, floor: String, seats: Int) -> Unit = { _, _, _ -> },
    onEditHall: (HallEntity) -> Unit = {},
    onDeleteHall: (HallEntity) -> Unit = {},
    onAddShift: (name: String, start: String, end: String, fee: Double) -> Unit = { _, _, _, _ -> },
    onEditShift: (ShiftEntity) -> Unit = {},
    onDeleteShift: (ShiftEntity) -> Unit = {},
    onAddPlan: (name: String, duration: Int, fee: Double, durationType: String, discount: Double, facilities: String, shiftId: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onEditPlan: (MembershipPlanEntity) -> Unit = {},
    onDeletePlan: (MembershipPlanEntity) -> Unit = {},
    onManualCloudSync: () -> Unit = {},
    viewModel: com.example.viewmodel.LibDeskViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(initialTab) }

    // ==========================================
    // TAB 0: PROFILE & GENERAL SETTINGS STATE
    // ==========================================
    var libName by remember(library) { mutableStateOf(library?.name ?: "") }
    var libCode by remember(library) { mutableStateOf(library?.code ?: "") }
    var ownerName by remember(library) { mutableStateOf(library?.ownerName ?: "") }
    var ownerPhone by remember(library) { mutableStateOf(library?.ownerPhone ?: "") }
    var ownerEmail by remember(library) { mutableStateOf(library?.ownerEmail ?: "") }
    var ownerWhatsApp by remember(library) { mutableStateOf(library?.ownerWhatsApp ?: "") }
    var address by remember(library) { mutableStateOf(library?.address ?: "") }
    var landmark by remember(library) { mutableStateOf(library?.landmark ?: "") }
    var city by remember(library) { mutableStateOf(library?.city ?: "") }
    var state by remember(library) { mutableStateOf(library?.state ?: "") }
    var pincode by remember(library) { mutableStateOf(library?.pincode ?: "") }
    var receiptPrefix by remember(library) { mutableStateOf(library?.receiptPrefix ?: "REC") }
    var defaultFinePerDay by remember(library) { mutableStateOf(library?.defaultFinePerDay?.toString() ?: "5.0") }
    var upiId by remember(library) { mutableStateOf(library?.upiId ?: "") }
    var upiPayeeName by remember(library) { mutableStateOf(library?.upiPayeeName ?: "") }

    var isFetchingLocation by remember { mutableStateOf(false) }

    // ==========================================
    // TAB 1: HALLS & INFRASTRUCTURE STATE
    // ==========================================
    var showAddHallDialog by remember { mutableStateOf(false) }
    var hallNameInput by remember { mutableStateOf("") }
    var hallFloorInput by remember { mutableStateOf("Ground Floor") }
    var hallSeatsInput by remember { mutableStateOf("30") }
    var editingHall by remember { mutableStateOf<HallEntity?>(null) }

    // ==========================================
    // TAB 2: SHIFTS & PRICING PLANS STATE
    // ==========================================
    var showAddShiftDialog by remember { mutableStateOf(false) }
    var shiftNameInput by remember { mutableStateOf("") }
    var shiftStartInput by remember { mutableStateOf("08:00 AM") }
    var shiftEndInput by remember { mutableStateOf("02:00 PM") }
    var shiftFeeInput by remember { mutableStateOf("800") }
    var editingShift by remember { mutableStateOf<ShiftEntity?>(null) }

    var showAddPlanDialog by remember { mutableStateOf(false) }
    var planNameInput by remember { mutableStateOf("") }
    var planDurationInput by remember { mutableStateOf("1") }
    var planFeeInput by remember { mutableStateOf("1000") }
    var planDiscountInput by remember { mutableStateOf("0") }
    var planFacilitiesInput by remember { mutableStateOf("Wi-Fi, Silent AC, RO Water, Charging Socket") }
    var planShiftIdInput by remember { mutableStateOf("") }

    // ==========================================
    // TAB 3: DUES & NOTIFICATIONS STATE
    // ==========================================
    var isDailyAlertsActive by remember {
        mutableStateOf(DuesNotificationHelper.isDailyAlertEnabled(context))
    }
    var alertThresholdDays by remember {
        mutableStateOf(DuesNotificationHelper.getThresholdDays(context))
    }

    // Helper: Time Picker
    fun openTimePicker(initialTime: String, onSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        try {
            val parser = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val d = parser.parse(initialTime)
            if (d != null) cal.time = d
        } catch (_: Exception) {}

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val c = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                onSelected(formatter.format(c.time))
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    // Helper: Location GPS Capture
    fun fetchAndSaveGpsLocation() {
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
                } catch (_: SecurityException) {}
            }

            if (bestLocation != null) {
                val lat = bestLocation.latitude
                val lng = bestLocation.longitude

                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val geocoder = android.location.Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            address = addr.getAddressLine(0) ?: address
                            city = addr.locality ?: addr.subAdminArea ?: city
                            state = addr.adminArea ?: state
                            pincode = addr.postalCode ?: pincode
                        }
                    } catch (_: Exception) {}

                    val updated = (library ?: LibraryEntity(id = "", name = libName, code = libCode)).copy(
                        latitude = lat,
                        longitude = lng,
                        address = address,
                        city = city,
                        state = state,
                        pincode = pincode,
                        updatedAt = System.currentTimeMillis()
                    )
                    onUpdateLibrary(updated)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isFetchingLocation = false
                        Toast.makeText(context, "GPS Location captured and saved ($city, $pincode)!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                isFetchingLocation = false
                Toast.makeText(context, "Please turn on GPS/Location on your device.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            isFetchingLocation = false
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchAndSaveGpsLocation()
        } else {
            Toast.makeText(context, "Location permission required to fetch GPS coordinates", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // DIALOGS: ADD / EDIT HALL
    // ==========================================
    if (showAddHallDialog || editingHall != null) {
        val isEditing = editingHall != null
        AlertDialog(
            onDismissRequest = {
                showAddHallDialog = false
                editingHall = null
            },
            modifier = Modifier.imePadding(),
            title = { Text(if (isEditing) "Edit Hall / Cabin" else "Add Study Hall") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = hallNameInput,
                        onValueChange = { hallNameInput = it },
                        label = { Text("Hall Name * (e.g. AC Study Hall A)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hallFloorInput,
                        onValueChange = { hallFloorInput = it },
                        label = { Text("Floor (e.g. Ground Floor, 1st Floor)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hallSeatsInput,
                        onValueChange = { hallSeatsInput = it },
                        label = { Text("Seat Capacity *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val seats = hallSeatsInput.toIntOrNull() ?: 30
                        if (hallNameInput.isBlank()) {
                            Toast.makeText(context, "Please enter hall name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isEditing) {
                            onEditHall(
                                editingHall!!.copy(
                                    name = hallNameInput.trim(),
                                    floor = hallFloorInput.trim(),
                                    seatCount = seats
                                )
                            )
                        } else {
                            onAddHall(hallNameInput.trim(), hallFloorInput.trim(), seats)
                        }
                        showAddHallDialog = false
                        editingHall = null
                        hallNameInput = ""
                        hallSeatsInput = "30"
                    }
                ) {
                    Text(if (isEditing) "Save Hall" else "Add Hall")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddHallDialog = false
                    editingHall = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DIALOGS: ADD / EDIT SHIFT
    // ==========================================
    if (showAddShiftDialog || editingShift != null) {
        val isEditing = editingShift != null
        AlertDialog(
            onDismissRequest = {
                showAddShiftDialog = false
                editingShift = null
            },
            modifier = Modifier.imePadding(),
            title = { Text(if (isEditing) "Edit Shift" else "Add Library Shift") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = shiftNameInput,
                        onValueChange = { shiftNameInput = it },
                        label = { Text("Shift Name * (e.g. Morning Shift)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = shiftStartInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Start Time") },
                            trailingIcon = {
                                IconButton(onClick = { openTimePicker(shiftStartInput) { shiftStartInput = it } }) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Pick Start Time")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = shiftEndInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("End Time") },
                            trailingIcon = {
                                IconButton(onClick = { openTimePicker(shiftEndInput) { shiftEndInput = it } }) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Pick End Time")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = shiftFeeInput,
                        onValueChange = { shiftFeeInput = it },
                        label = { Text("Base Monthly Fee (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fee = shiftFeeInput.toDoubleOrNull() ?: 800.0
                        if (shiftNameInput.isBlank()) {
                            Toast.makeText(context, "Please enter shift name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isEditing) {
                            onEditShift(
                                editingShift!!.copy(
                                    name = shiftNameInput.trim(),
                                    startTime = shiftStartInput,
                                    endTime = shiftEndInput,
                                    fee = fee
                                )
                            )
                        } else {
                            onAddShift(shiftNameInput.trim(), shiftStartInput, shiftEndInput, fee)
                        }
                        showAddShiftDialog = false
                        editingShift = null
                        shiftNameInput = ""
                        shiftFeeInput = "800"
                    }
                ) {
                    Text(if (isEditing) "Save Shift" else "Add Shift")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddShiftDialog = false
                    editingShift = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DIALOGS: ADD / EDIT PLAN
    // ==========================================
    if (showAddPlanDialog) {
        AlertDialog(
            onDismissRequest = { showAddPlanDialog = false },
            modifier = Modifier.imePadding(),
            title = { Text("Add Membership Plan / Offer") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = planNameInput,
                        onValueChange = { planNameInput = it },
                        label = { Text("Plan Title * (e.g. 3 Months Combo)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = planDurationInput,
                            onValueChange = { planDurationInput = it },
                            label = { Text("Duration (Months)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = planFeeInput,
                            onValueChange = { planFeeInput = it },
                            label = { Text("Total Fee (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = planFacilitiesInput,
                        onValueChange = { planFacilitiesInput = it },
                        label = { Text("Included Amenities") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dur = planDurationInput.toIntOrNull() ?: 1
                        val fee = planFeeInput.toDoubleOrNull() ?: 1000.0
                        val disc = planDiscountInput.toDoubleOrNull() ?: 0.0
                        if (planNameInput.isBlank()) {
                            Toast.makeText(context, "Please enter plan title", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onAddPlan(planNameInput.trim(), dur, fee, "MONTHS", disc, planFacilitiesInput.trim(), planShiftIdInput)
                        showAddPlanDialog = false
                        planNameInput = ""
                        planFeeInput = "1000"
                    }
                ) {
                    Text("Add Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab Selector Row
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                "🏛️ Profile & Rules",
                "🪑 Halls & Cabins",
                "⏱️ Shifts & Plans",
                "🔔 Dues & Alerts",
                "☁️ Cloud Sync & DB",
                "🔒 Security & Theme"
            ).forEachIndexed { idx, title ->
                Tab(
                    selected = activeTab == idx,
                    onClick = { activeTab = idx },
                    text = { Text(title, fontWeight = if (activeTab == idx) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            when (activeTab) {
                // ==========================================
                // 1. PROFILE & GENERAL SETTINGS TAB
                // ==========================================
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🏛️ Library Identity & Location", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                                OutlinedTextField(
                                    value = libName,
                                    onValueChange = { libName = it },
                                    label = { Text("Library / Study Center Name *") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = libCode,
                                        onValueChange = { libCode = it },
                                        label = { Text("Library Code") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = city,
                                        onValueChange = { city = it },
                                        label = { Text("City") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = address,
                                    onValueChange = { address = it },
                                    label = { Text("Full Street Address") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = state,
                                        onValueChange = { state = it },
                                        label = { Text("State") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = pincode,
                                        onValueChange = { pincode = it },
                                        label = { Text("Pincode") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isFetchingLocation
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isFetchingLocation) "Fetching GPS Location..." else "Auto-Capture GPS Coordinates")
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("📞 Owner Contacts & Billing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                                OutlinedTextField(
                                    value = ownerName,
                                    onValueChange = { ownerName = it },
                                    label = { Text("Owner Full Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = ownerPhone,
                                    onValueChange = { 
                                        ownerPhone = it
                                        ownerWhatsApp = it
                                    },
                                    label = { Text("Mobile & WhatsApp Number") },
                                    placeholder = { Text("e.g. 9876543210 (Calls & WhatsApp)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    },
                                    supportingText = {
                                        Text("Used for student calls, fee slips, UPI & WhatsApp reminders")
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = ownerEmail,
                                    onValueChange = { ownerEmail = it },
                                    label = { Text("Owner Email Address") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = upiId,
                                        onValueChange = { upiId = it },
                                        label = { Text("UPI ID (for Student QR)") },
                                        placeholder = { Text("library@upi") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = upiPayeeName,
                                        onValueChange = { upiPayeeName = it },
                                        label = { Text("UPI Payee Name") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = receiptPrefix,
                                        onValueChange = { receiptPrefix = it },
                                        label = { Text("Receipt Prefix") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = defaultFinePerDay,
                                        onValueChange = { defaultFinePerDay = it },
                                        label = { Text("Daily Late Fine (₹)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (libName.isBlank()) {
                                    Toast.makeText(context, "Library name cannot be empty", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val updated = (library ?: LibraryEntity(id = "", name = libName, code = libCode)).copy(
                                    name = libName.trim(),
                                    code = libCode.trim(),
                                    ownerName = ownerName.trim(),
                                    ownerPhone = ownerPhone.trim(),
                                    ownerEmail = ownerEmail.trim(),
                                    ownerWhatsApp = ownerPhone.trim(),
                                    address = address.trim(),
                                    city = city.trim(),
                                    state = state.trim(),
                                    pincode = pincode.trim(),
                                    receiptPrefix = receiptPrefix.trim(),
                                    defaultFinePerDay = defaultFinePerDay.toDoubleOrNull() ?: 5.0,
                                    upiId = upiId.trim(),
                                    upiPayeeName = upiPayeeName.trim(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                onUpdateLibrary(updated)
                                Toast.makeText(context, "Library settings saved successfully!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save All Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ==========================================
                // 2. HALLS & INFRASTRUCTURE TAB
                // ==========================================
                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("🪑 Halls & Cabins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${halls.size} Active Study Halls configured", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    editingHall = null
                                    hallNameInput = ""
                                    hallSeatsInput = "30"
                                    showAddHallDialog = true
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Hall")
                            }
                        }

                        if (halls.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Halls Configured", fontWeight = FontWeight.Bold)
                                    Text("Click '+ Add Hall' to configure your first study room and seat capacity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            halls.forEach { hall ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(hall.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("${hall.floor} • ${hall.seatCount} Seats • ${if (hall.isAc) "AC" else "Non-AC"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Row {
                                            IconButton(onClick = {
                                                editingHall = hall
                                                hallNameInput = hall.name
                                                hallFloorInput = hall.floor
                                                hallSeatsInput = hall.seatCount.toString()
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                                            }
                                            IconButton(onClick = { onDeleteHall(hall) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 3. SHIFTS & PRICING PLANS TAB
                // ==========================================
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Shifts Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("⏱️ Operational Shifts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${shifts.size} Shift timings configured", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    editingShift = null
                                    shiftNameInput = ""
                                    shiftFeeInput = "800"
                                    showAddShiftDialog = true
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Shift")
                            }
                        }

                        shifts.forEach { shift ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(shift.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${shift.startTime} to ${shift.endTime} • ₹${shift.fee.toInt()}/mo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            editingShift = shift
                                            shiftNameInput = shift.name
                                            shiftStartInput = shift.startTime
                                            shiftEndInput = shift.endTime
                                            shiftFeeInput = shift.fee.toString()
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = { onDeleteShift(shift) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Plans Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("💳 Membership Packages & Offers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${plans.size} Active Packages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    planNameInput = ""
                                    planFeeInput = "1000"
                                    showAddPlanDialog = true
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Package")
                            }
                        }

                        plans.forEach { plan ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${plan.durationDays / 30} Months • ₹${plan.baseFee.toInt()} • ${plan.facilities}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { onDeletePlan(plan) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 4. DUES & NOTIFICATIONS TAB
                // ==========================================
                3 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("🔔 Automated Daily Dues Alarm", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "When active, LibDesk automatically checks upcoming fee expiries every morning at 09:00 AM and notifies you with 1-click WhatsApp reminders.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Daily Morning Expiry Alerts", fontWeight = FontWeight.SemiBold)
                                    Switch(
                                        checked = isDailyAlertsActive,
                                        onCheckedChange = {
                                            isDailyAlertsActive = it
                                            DuesNotificationHelper.setDailyAlertEnabled(context, it)
                                            if (it) {
                                                DuesNotificationHelper.scheduleDailyDuesAlarm(context)
                                                Toast.makeText(context, "Morning 09:00 AM Dues Alarm scheduled!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                DuesNotificationHelper.cancelDailyDuesAlarm(context)
                                                Toast.makeText(context, "Daily alarm cancelled.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Alert Threshold (Days before expiry):", style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(1, 3, 5, 7).forEach { days ->
                                            FilterChip(
                                                selected = alertThresholdDays == days,
                                                onClick = {
                                                    alertThresholdDays = days
                                                    DuesNotificationHelper.setThresholdDays(context, days)
                                                },
                                                label = { Text("${days}d") }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                DuesNotificationHelper.triggerDuesNotification(context, students, isTest = true)
                                Toast.makeText(context, "Checked dues! Check notifications panel.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Run Daily Dues Notification Now")
                        }
                    }
                }

                // ==========================================
                // 5. CLOUD SYNC & DATABASE BACKUP TAB
                // ==========================================
                4 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSupabaseSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Supabase Cloud Sync Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                Text(
                                    text = supabaseStatusMessage ?: "Cloud connected. Real-time PostgreSQL database synchronization active.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = onManualCloudSync,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSupabaseSyncing,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isSupabaseSyncing) "Syncing with Cloud..." else "Force Push All Records to Supabase")
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📊 Local Database Statistics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("• ${students.size} Total Registered Students", style = MaterialTheme.typography.bodySmall)
                                Text("• ${halls.sumOf { it.seatCount }} Total Study Seats across ${halls.size} Halls", style = MaterialTheme.typography.bodySmall)
                                Text("• ${shifts.size} Shift Timings & ${plans.size} Membership Plans", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // ==========================================
                // 6. SECURITY & THEME TAB
                // ==========================================
                5 -> {
                    val isDarkMode by (viewModel?.isDarkMode?.collectAsState() ?: remember { mutableStateOf(false) })
                    val isAmoledMode by (viewModel?.isAmoledMode?.collectAsState() ?: remember { mutableStateOf(false) })
                    val isBiometricEnabled by (viewModel?.isBiometricEnabled?.collectAsState() ?: remember { mutableStateOf(false) })

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Display & Theme Modes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Dark Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("Standard modern dark slate UI", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = isDarkMode,
                                        onCheckedChange = { viewModel?.toggleDarkMode(it) }
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("AMOLED Pure Black Theme", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF38BDF8).copy(alpha = 0.2f)
                                            ) {
                                                Text("STUDY NIGHT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                        Text("Deep pitch black #000000 with glowing text for late-night study halls", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = isAmoledMode,
                                        onCheckedChange = { viewModel?.toggleAmoledMode(it) }
                                    )
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Biometric Security Protection", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Fingerprint / Face Unlock", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("Require biometric authentication to access manager & finance controls", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = isBiometricEnabled,
                                        onCheckedChange = {
                                            viewModel?.toggleBiometric(it)
                                            Toast.makeText(context, if (it) "Biometric Lock Enabled" else "Biometric Lock Disabled", Toast.LENGTH_SHORT).show()
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
}
