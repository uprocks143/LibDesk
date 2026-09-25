package com.example.ui.manager

import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.HallEntity
import com.example.data.local.entities.MembershipPlanEntity
import com.example.data.local.entities.ShiftEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun InfrastructureEditorModal(
    halls: List<HallEntity>,
    shifts: List<ShiftEntity>,
    plans: List<MembershipPlanEntity>,
    onAddHall: (name: String, floor: String, seats: Int) -> Unit,
    onEditHall: (HallEntity) -> Unit = {},
    onDeleteHall: (HallEntity) -> Unit = {},
    onAddShift: (name: String, start: String, end: String, fee: Double) -> Unit,
    onEditShift: (ShiftEntity) -> Unit = {},
    onDeleteShift: (ShiftEntity) -> Unit = {},
    onAddPlan: (name: String, duration: Int, fee: Double, durationType: String, discount: Double, facilities: String, shiftId: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onEditPlan: (MembershipPlanEntity) -> Unit = {},
    onDeletePlan: (MembershipPlanEntity) -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    // Hall inputs
    var newHallName by remember { mutableStateOf("") }
    var newHallFloor by remember { mutableStateOf("") }
    var newHallSeats by remember { mutableStateOf("") }
    var editingHall by remember { mutableStateOf<HallEntity?>(null) }
    var deletingHall by remember { mutableStateOf<HallEntity?>(null) }

    // Shift inputs
    var newShiftName by remember { mutableStateOf("") }
    var newShiftStart by remember { mutableStateOf("08:00 AM") }
    var newShiftEnd by remember { mutableStateOf("02:00 PM") }
    var newShiftFee by remember { mutableStateOf("800") }
    var editingShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var deletingShift by remember { mutableStateOf<ShiftEntity?>(null) }

    // Plan inputs (Student Plans & Offers linked to Shifts)
    var newPlanShiftId by remember { mutableStateOf("") } // "" = All Shifts
    var newPlanName by remember { mutableStateOf("") }
    var newPlanDurationUnit by remember { mutableStateOf("MONTHS") } // "MONTHS" or "DAYS"
    var newPlanDuration by remember { mutableStateOf("1") }
    var newPlanFee by remember { mutableStateOf("1000") }
    var newPlanDiscount by remember { mutableStateOf("0") }
    var newPlanFacilities by remember { mutableStateOf("High-Speed Wi-Fi, RO Water, Charging Socket, Silent AC") }
    var editingPlan by remember { mutableStateOf<MembershipPlanEntity?>(null) }
    var deletingPlan by remember { mutableStateOf<MembershipPlanEntity?>(null) }

    // Time picker dialog helper
    fun showTimePicker(initialTimeStr: String, onTimeSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        try {
            val parser = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val date = parser.parse(initialTimeStr)
            if (date != null) cal.time = date
        } catch (_: Exception) {}

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
                onTimeSelected(formatter.format(selectedCal.time))
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false // 12-hour view with AM / PM selector
        ).show()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Infrastructure & Plans",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Manage study halls, shift timings, and membership plans",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Halls (${halls.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Shifts (${shifts.size})") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.CardMembership, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Plans & Offers (${plans.size})") }
                    )
                }

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        // -------------------------------------------------------------
                        // TAB 0: HALLS
                        // -------------------------------------------------------------
                        0 -> {
                            Text(
                                "Existing Reading Halls",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (halls.isEmpty()) {
                                Text(
                                    "No halls added yet. Use the form below to create your first hall.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            } else {
                                halls.forEach { hall ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(hall.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    "Floor: ${hall.floor.ifBlank { "Ground" }} • Capacity: ${hall.seatCount} seats",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { editingHall = hall },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Hall", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = { deletingHall = hall },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Hall", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider()

                            Text(
                                "Add New Reading Hall",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = newHallName,
                                onValueChange = { newHallName = it },
                                label = { Text("Hall Name (e.g. Main Hall A, Silent Room)") },
                                leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newHallFloor,
                                onValueChange = { newHallFloor = it },
                                label = { Text("Floor (e.g. Ground Floor, 1st Floor)") },
                                leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newHallSeats,
                                onValueChange = { newHallSeats = it.filter { c -> c.isDigit() } },
                                label = { Text("Total Seats Capacity") },
                                leadingIcon = { Icon(Icons.Default.Chair, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val seats = newHallSeats.toIntOrNull() ?: 0
                                    if (newHallName.isNotBlank() && seats > 0) {
                                        onAddHall(newHallName.trim(), newHallFloor.trim(), seats)
                                        newHallName = ""; newHallFloor = ""; newHallSeats = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = newHallName.isNotBlank() && (newHallSeats.toIntOrNull() ?: 0) > 0
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Reading Hall")
                            }
                        }

                        // -------------------------------------------------------------
                        // TAB 1: SHIFTS (WITH TIME PICKER + AM/PM)
                        // -------------------------------------------------------------
                        1 -> {
                            Text(
                                "Existing Shifts / Batches",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (shifts.isEmpty()) {
                                Text(
                                    "No shifts added yet. Create shifts to allow flexible hourly slot booking.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            } else {
                                shifts.forEach { shift ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(shift.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    "Timing: ${shift.startTime} – ${shift.endTime} • Fee: ₹${shift.fee}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { editingShift = shift },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Shift", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = { deletingShift = shift },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Shift", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider()

                            Text(
                                "Add New Shift",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = newShiftName,
                                onValueChange = { newShiftName = it },
                                label = { Text("Shift Name (e.g. Morning Batch, Full Day, Night Owl)") },
                                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Start Time with interactive TimePicker
                                OutlinedTextField(
                                    value = newShiftStart,
                                    onValueChange = { newShiftStart = it },
                                    readOnly = true,
                                    label = { Text("Start Time") },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            showTimePicker(newShiftStart) { newShiftStart = it }
                                        }) {
                                            Icon(Icons.Default.AccessTime, contentDescription = "Select Start Time", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showTimePicker(newShiftStart) { newShiftStart = it }
                                        }
                                )

                                // End Time with interactive TimePicker
                                OutlinedTextField(
                                    value = newShiftEnd,
                                    onValueChange = { newShiftEnd = it },
                                    readOnly = true,
                                    label = { Text("End Time") },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            showTimePicker(newShiftEnd) { newShiftEnd = it }
                                        }) {
                                            Icon(Icons.Default.AccessTime, contentDescription = "Select End Time", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showTimePicker(newShiftEnd) { newShiftEnd = it }
                                        }
                                )
                            }

                            OutlinedTextField(
                                value = newShiftFee,
                                onValueChange = { newShiftFee = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Shift Fee (₹)") },
                                leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val fee = newShiftFee.toDoubleOrNull() ?: 0.0
                                    if (newShiftName.isNotBlank() && newShiftStart.isNotBlank()) {
                                        onAddShift(newShiftName.trim(), newShiftStart, newShiftEnd, fee)
                                        newShiftName = ""
                                        newShiftStart = "08:00 AM"
                                        newShiftEnd = "02:00 PM"
                                        newShiftFee = "800"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = newShiftName.isNotBlank() && newShiftStart.isNotBlank() && newShiftEnd.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Shift")
                            }
                        }

                        // -------------------------------------------------------------
                        // TAB 2: STUDENT PLANS & OFFERS (MANAGED BY LIBRARY)
                        // -------------------------------------------------------------
                        2 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Student Plans & Offers",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Autonomous pricing & student discount management",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "Library Managed",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (plans.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "No student plans created yet. Use the form below to configure your library's admission plans and promotional offers.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.5.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    plans.forEach { plan ->
                                        val isDays = plan.durationType.equals("DAYS", ignoreCase = true) || plan.durationDays > 0 && plan.durationMonths <= 1
                                        val durationLabel = if (isDays) "${plan.durationDays} Days" else "${plan.durationMonths} ${if (plan.durationMonths == 1) "Month" else "Months"}"
                                        val netPayable = (plan.baseFee - plan.discount).coerceAtLeast(0.0)
                                        val linkedShift = shifts.firstOrNull { it.id == plan.shiftId }

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.padding(bottom = 2.dp)
                                                    ) {
                                                        Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                        Surface(
                                                            color = if (isDays) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                durationLabel,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDays) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        }
                                                        if (plan.discount > 0.0) {
                                                            Surface(
                                                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                                shape = RoundedCornerShape(4.dp)
                                                            ) {
                                                                Text(
                                                                    "₹${plan.discount.toInt()} OFF",
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    fontSize = 10.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFF047857)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // Linked Shift Tag
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (linkedShift != null) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            text = if (linkedShift != null) "Shift: ${linkedShift.name} (${linkedShift.startTime} - ${linkedShift.endTime})" else "Shift: All Shifts (Global)",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (linkedShift != null) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (plan.discount > 0.0) {
                                                            Text(
                                                                "₹${plan.baseFee.toInt()}",
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                "Net: ₹${netPayable.toInt()}",
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        } else {
                                                            Text(
                                                                "Fee: ₹${plan.baseFee.toInt()}",
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        Text(
                                                            " • ${plan.seatType}",
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    if (plan.facilities.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            plan.facilities,
                                                            fontSize = 11.5.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    IconButton(
                                                        onClick = { editingPlan = plan },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Plan", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { deletingPlan = plan },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Plan", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider()

                            Text(
                                "Create Shift Plan / Special Offer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // 1. Shift Association Selector
                            Text("1. Select Target Shift / Batch", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = newPlanShiftId.isBlank(),
                                    onClick = { newPlanShiftId = "" },
                                    label = { Text("All Shifts (Global)", fontSize = 11.5.sp) }
                                )
                                shifts.take(3).forEach { shift ->
                                    FilterChip(
                                        selected = newPlanShiftId == shift.id,
                                        onClick = {
                                            newPlanShiftId = shift.id
                                            if (newPlanName.isBlank() || newPlanName.contains("Shift", ignoreCase = true)) {
                                                newPlanName = "${shift.name} Regular Plan"
                                            }
                                            newPlanFee = (shift.fee * (newPlanDuration.toIntOrNull() ?: 1)).toInt().toString()
                                        },
                                        label = { Text(shift.name, fontSize = 11.5.sp) }
                                    )
                                }
                            }

                            // 2. Offer Templates / Presets
                            Text("2. Quick Plan & Offer Templates", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SuggestionChip(
                                    onClick = {
                                        val shiftObj = shifts.firstOrNull { it.id == newPlanShiftId }
                                        val shiftName = shiftObj?.name ?: "Shift"
                                        newPlanName = "$shiftName Regular Plan"
                                        newPlanDurationUnit = "MONTHS"
                                        newPlanDuration = "1"
                                        val base = shiftObj?.fee ?: 500.0
                                        newPlanFee = base.toInt().toString()
                                        newPlanDiscount = "0"
                                    },
                                    label = { Text("Regular Plan", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        val shiftObj = shifts.firstOrNull { it.id == newPlanShiftId }
                                        val shiftName = shiftObj?.name ?: "Shift"
                                        newPlanName = "$shiftName 3-Month Plan (20% Off)"
                                        newPlanDurationUnit = "MONTHS"
                                        newPlanDuration = "3"
                                        val perMonth = shiftObj?.fee ?: 500.0
                                        val totalBase = perMonth * 3.0
                                        val discount = totalBase * 0.20
                                        newPlanFee = totalBase.toInt().toString()
                                        newPlanDiscount = discount.toInt().toString()
                                    },
                                    label = { Text("3 Mo (20% Off)", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        val shiftObj = shifts.firstOrNull { it.id == newPlanShiftId }
                                        val shiftName = shiftObj?.name ?: "Library"
                                        newPlanName = "Diwali Festive Offer (Flat 20% Off)"
                                        newPlanDurationUnit = "MONTHS"
                                        newPlanDuration = "1"
                                        val base = shiftObj?.fee ?: 600.0
                                        val discount = base * 0.20
                                        newPlanFee = base.toInt().toString()
                                        newPlanDiscount = discount.toInt().toString()
                                    },
                                    label = { Text("Diwali 20% Off", fontSize = 11.sp) }
                                )
                            }

                            OutlinedTextField(
                                value = newPlanName,
                                onValueChange = { newPlanName = it },
                                label = { Text("Plan / Offer Name (e.g. Morning Shift Regular, Diwali Special)") },
                                leadingIcon = { Icon(Icons.Default.CardMembership, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Granular Duration Unit Selector: Days vs Months
                            Text("Duration Unit", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = newPlanDurationUnit == "DAYS",
                                    onClick = {
                                        newPlanDurationUnit = "DAYS"
                                        if (newPlanDuration == "1") newPlanDuration = "15"
                                    },
                                    label = { Text("Days (Granular)") },
                                    leadingIcon = {
                                        if (newPlanDurationUnit == "DAYS") {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = newPlanDurationUnit == "MONTHS",
                                    onClick = {
                                        newPlanDurationUnit = "MONTHS"
                                        if (newPlanDuration == "15") newPlanDuration = "1"
                                    },
                                    label = { Text("Months (Monthly)") },
                                    leadingIcon = {
                                        if (newPlanDurationUnit == "MONTHS") {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Quick duration suggestion chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (newPlanDurationUnit == "DAYS") {
                                    listOf("7", "15", "30", "45", "60").forEach { d ->
                                        SuggestionChip(
                                            onClick = { newPlanDuration = d },
                                            label = { Text("$d Days", fontSize = 11.sp) }
                                        )
                                    }
                                } else {
                                    listOf("1", "2", "3", "6", "12").forEach { m ->
                                        SuggestionChip(
                                            onClick = {
                                                newPlanDuration = m
                                                val shiftObj = shifts.firstOrNull { it.id == newPlanShiftId }
                                                if (shiftObj != null) {
                                                    val mInt = m.toIntOrNull() ?: 1
                                                    newPlanFee = (shiftObj.fee * mInt).toInt().toString()
                                                }
                                            },
                                            label = { Text("$m Mo", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newPlanDuration,
                                onValueChange = { newPlanDuration = it.filter { c -> c.isDigit() } },
                                label = { Text(if (newPlanDurationUnit == "DAYS") "Duration (Days)" else "Duration (Months)") },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = newPlanFee,
                                    onValueChange = { newPlanFee = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("Base Fee (₹)") },
                                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = newPlanDiscount,
                                    onValueChange = { newPlanDiscount = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("Offer Discount (₹)") },
                                    leadingIcon = { Icon(Icons.Default.LocalOffer, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = newPlanFacilities,
                                onValueChange = { newPlanFacilities = it },
                                label = { Text("Facilities & Perks Included") },
                                leadingIcon = { Icon(Icons.Default.DoneAll, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            val feeNum = newPlanFee.toDoubleOrNull() ?: 0.0
                            val discNum = newPlanDiscount.toDoubleOrNull() ?: 0.0
                            val netFeeCalc = (feeNum - discNum).coerceAtLeast(0.0)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Student Net Payable:", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                                    Text("₹${netFeeCalc.toInt()} (Base ₹${feeNum.toInt()} - ₹${discNum.toInt()} OFF)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Button(
                                onClick = {
                                    val durationVal = newPlanDuration.toIntOrNull() ?: if (newPlanDurationUnit == "DAYS") 30 else 1
                                    val fee = newPlanFee.toDoubleOrNull() ?: 0.0
                                    val discountVal = newPlanDiscount.toDoubleOrNull() ?: 0.0
                                    if (newPlanName.isNotBlank() && durationVal > 0) {
                                        onAddPlan(
                                            newPlanName.trim(),
                                            durationVal,
                                            fee,
                                            newPlanDurationUnit,
                                            discountVal,
                                            newPlanFacilities.trim(),
                                            newPlanShiftId
                                        )
                                        newPlanName = ""
                                        newPlanDuration = if (newPlanDurationUnit == "DAYS") "15" else "1"
                                        newPlanFee = "1000"
                                        newPlanDiscount = "0"
                                        newPlanShiftId = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = newPlanName.isNotBlank() && (newPlanDuration.toIntOrNull() ?: 0) > 0
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Student Plan & Offer")
                            }
                        }
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(onClick = onClose) {
                        Text("Done")
                    }
                }
            }
        }
    }

    // =========================================================================
    // EDIT HALL DIALOG
    // =========================================================================
    editingHall?.let { hall ->
        var editName by remember { mutableStateOf(hall.name) }
        var editFloor by remember { mutableStateOf(hall.floor) }
        var editSeats by remember { mutableStateOf(hall.seatCount.toString()) }

        AlertDialog(
            onDismissRequest = { editingHall = null },
            title = { Text("Edit Reading Hall", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Hall Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editFloor,
                        onValueChange = { editFloor = it },
                        label = { Text("Floor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSeats,
                        onValueChange = { editSeats = it.filter { c -> c.isDigit() } },
                        label = { Text("Total Seats") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = editName.isNotBlank() && (editSeats.toIntOrNull() ?: 0) > 0,
                    onClick = {
                        val updated = hall.copy(
                            name = editName.trim(),
                            floor = editFloor.trim(),
                            seatCount = editSeats.toIntOrNull() ?: hall.seatCount
                        )
                        onEditHall(updated)
                        editingHall = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingHall = null }) { Text("Cancel") }
            }
        )
    }

    // =========================================================================
    // DELETE HALL CONFIRMATION DIALOG
    // =========================================================================
    deletingHall?.let { hall ->
        AlertDialog(
            onDismissRequest = { deletingHall = null },
            title = { Text("Delete Hall?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${hall.name}' (${hall.floor})? Seats inside this hall will remain in database.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDeleteHall(hall)
                        deletingHall = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingHall = null }) { Text("Cancel") }
            }
        )
    }

    // =========================================================================
    // EDIT SHIFT DIALOG
    // =========================================================================
    editingShift?.let { shift ->
        var editName by remember { mutableStateOf(shift.name) }
        var editStart by remember { mutableStateOf(shift.startTime) }
        var editEnd by remember { mutableStateOf(shift.endTime) }
        var editFee by remember { mutableStateOf(shift.fee.toString()) }

        AlertDialog(
            onDismissRequest = { editingShift = null },
            title = { Text("Edit Shift / Batch", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Shift Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editStart,
                            onValueChange = { editStart = it },
                            readOnly = true,
                            label = { Text("Start Time") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    showTimePicker(editStart) { editStart = it }
                                }) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Select Start Time", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showTimePicker(editStart) { editStart = it }
                                }
                        )
                        OutlinedTextField(
                            value = editEnd,
                            onValueChange = { editEnd = it },
                            readOnly = true,
                            label = { Text("End Time") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    showTimePicker(editEnd) { editEnd = it }
                                }) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Select End Time", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showTimePicker(editEnd) { editEnd = it }
                                }
                        )
                    }
                    OutlinedTextField(
                        value = editFee,
                        onValueChange = { editFee = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Shift Fee (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = editName.isNotBlank() && editStart.isNotBlank(),
                    onClick = {
                        val updated = shift.copy(
                            name = editName.trim(),
                            startTime = editStart,
                            endTime = editEnd,
                            fee = editFee.toDoubleOrNull() ?: shift.fee
                        )
                        onEditShift(updated)
                        editingShift = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingShift = null }) { Text("Cancel") }
            }
        )
    }

    // =========================================================================
    // DELETE SHIFT CONFIRMATION DIALOG
    // =========================================================================
    deletingShift?.let { shift ->
        AlertDialog(
            onDismissRequest = { deletingShift = null },
            title = { Text("Delete Shift?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete shift '${shift.name}' (${shift.startTime} - ${shift.endTime})?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDeleteShift(shift)
                        deletingShift = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingShift = null }) { Text("Cancel") }
            }
        )
    }

    // =========================================================================
    // EDIT PLAN DIALOG (WITH DAYS vs MONTHS TOGGLE)
    // =========================================================================
    editingPlan?.let { plan ->
        val initialIsDays = plan.durationType.equals("DAYS", ignoreCase = true) || (plan.durationDays > 0 && plan.durationMonths <= 1)
        var editName by remember { mutableStateOf(plan.name) }
        var editShiftId by remember { mutableStateOf(plan.shiftId) }
        var editUnit by remember { mutableStateOf(if (initialIsDays) "DAYS" else "MONTHS") }
        var editDuration by remember {
            mutableStateOf(if (initialIsDays) plan.durationDays.toString() else plan.durationMonths.toString())
        }
        var editFee by remember { mutableStateOf(plan.baseFee.toString()) }
        var editDiscount by remember { mutableStateOf(plan.discount.toString()) }
        var editFacilities by remember { mutableStateOf(plan.facilities) }

        AlertDialog(
            onDismissRequest = { editingPlan = null },
            title = { Text("Edit Student Plan & Offer", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Plan / Offer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Target Shift
                    Text("Target Shift / Batch", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = editShiftId.isBlank(),
                            onClick = { editShiftId = "" },
                            label = { Text("All Shifts", fontSize = 11.sp) }
                        )
                        shifts.forEach { s ->
                            FilterChip(
                                selected = editShiftId == s.id,
                                onClick = { editShiftId = s.id },
                                label = { Text(s.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = editUnit == "DAYS",
                            onClick = { editUnit = "DAYS" },
                            label = { Text("Days") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = editUnit == "MONTHS",
                            onClick = { editUnit = "MONTHS" },
                            label = { Text("Months") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editDuration,
                        onValueChange = { editDuration = it.filter { c -> c.isDigit() } },
                        label = { Text(if (editUnit == "DAYS") "Duration (Days)" else "Duration (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editFee,
                            onValueChange = { editFee = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Base Fee (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editDiscount,
                            onValueChange = { editDiscount = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Offer (₹ OFF)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = editFacilities,
                        onValueChange = { editFacilities = it },
                        label = { Text("Facilities & Perks") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = editName.isNotBlank() && (editDuration.toIntOrNull() ?: 0) > 0,
                    onClick = {
                        val durationNum = editDuration.toIntOrNull() ?: 1
                        val (m, d) = if (editUnit == "DAYS") {
                            Pair((durationNum / 30).coerceAtLeast(1), durationNum)
                        } else {
                            Pair(durationNum, durationNum * 30)
                        }
                        val updated = plan.copy(
                            name = editName.trim(),
                            shiftId = editShiftId,
                            durationMonths = m,
                            durationDays = d,
                            durationType = editUnit,
                            baseFee = editFee.toDoubleOrNull() ?: plan.baseFee,
                            discount = editDiscount.toDoubleOrNull() ?: 0.0,
                            facilities = editFacilities.trim()
                        )
                        onEditPlan(updated)
                        editingPlan = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingPlan = null }) { Text("Cancel") }
            }
        )
    }

    // =========================================================================
    // DELETE PLAN CONFIRMATION DIALOG
    // =========================================================================
    deletingPlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { deletingPlan = null },
            title = { Text("Delete Plan?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete membership plan '${plan.name}'?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onDeletePlan(plan)
                        deletingPlan = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlan = null }) { Text("Cancel") }
            }
        )
    }
}
