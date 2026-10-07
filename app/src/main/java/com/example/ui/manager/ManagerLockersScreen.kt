package com.example.ui.manager

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.LockerEntity
import com.example.ui.theme.LibDeskColors
import com.example.util.WhatsAppAlertUtils
import com.example.viewmodel.LibDeskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerLockersScreen(
    viewModel: LibDeskViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lockers by viewModel.lockers.collectAsState()
    val students by viewModel.students.collectAsState()
    val currentLib by viewModel.currentLibrary.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "AVAILABLE", "OCCUPIED", "MAINTENANCE"
    var showAddLockerDialog by remember { mutableStateOf(false) }
    var selectedLockerForAction by remember { mutableStateOf<LockerEntity?>(null) }
    var showAllocateDialog by remember { mutableStateOf(false) }

    val filteredLockers = remember(lockers, searchQuery, selectedFilter) {
        lockers.filter { locker ->
            val matchSearch = locker.lockerNumber.contains(searchQuery, ignoreCase = true) ||
                    locker.assignedStudentName.contains(searchQuery, ignoreCase = true) ||
                    locker.floor.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (selectedFilter) {
                "AVAILABLE" -> locker.status == "AVAILABLE"
                "OCCUPIED" -> locker.status == "OCCUPIED"
                "MAINTENANCE" -> locker.status == "MAINTENANCE"
                else -> true
            }
            matchSearch && matchFilter
        }
    }

    val totalLockers = lockers.size
    val availableCount = lockers.count { it.status == "AVAILABLE" }
    val occupiedCount = lockers.count { it.status == "OCCUPIED" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Stat Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LockerStatCard(
                title = "Total Lockers",
                count = "$totalLockers",
                icon = Icons.Default.Lock,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            LockerStatCard(
                title = "Available",
                count = "$availableCount",
                icon = Icons.Default.CheckCircle,
                color = LibDeskColors.success,
                modifier = Modifier.weight(1f)
            )
            LockerStatCard(
                title = "Occupied",
                count = "$occupiedCount",
                icon = Icons.Default.Person,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search and Add Locker Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("locker_search_input"),
                placeholder = { Text("Search Locker No / Student", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { showAddLockerDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_locker_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Locker", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All ($totalLockers)", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = selectedFilter == "AVAILABLE",
                onClick = { selectedFilter = "AVAILABLE" },
                label = { Text("Available ($availableCount)", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = selectedFilter == "OCCUPIED",
                onClick = { selectedFilter = "OCCUPIED" },
                label = { Text("Occupied ($occupiedCount)", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredLockers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No lockers found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredLockers, key = { it.id }) { locker ->
                    LockerGridCard(
                        locker = locker,
                        onAllocateClick = {
                            selectedLockerForAction = locker
                            showAllocateDialog = true
                        },
                        onReleaseClick = {
                            viewModel.releaseLocker(locker.id)
                        },
                        onWhatsAppClick = {
                            if (locker.assignedStudentPhone.isNotBlank()) {
                                WhatsAppAlertUtils.sendLockerAllocationAlert(
                                    context = context,
                                    studentName = locker.assignedStudentName,
                                    phone = locker.assignedStudentPhone,
                                    libraryName = currentLib?.name ?: "LibDesk",
                                    lockerNumber = locker.lockerNumber,
                                    keyNumber = locker.keyNumber,
                                    monthlyRent = locker.monthlyRent,
                                    expiryDate = locker.expiryDate
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // Add New Locker Dialog
    if (showAddLockerDialog) {
        var lockerNumber by remember { mutableStateOf("") }
        var floor by remember { mutableStateOf("Ground Floor") }
        var rent by remember { mutableStateOf("250") }
        var deposit by remember { mutableStateOf("300") }
        var size by remember { mutableStateOf("Standard") }

        AlertDialog(
            onDismissRequest = { showAddLockerDialog = false },
            modifier = Modifier.imePadding(),
            title = { Text("Add New Locker", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = lockerNumber,
                        onValueChange = { lockerNumber = it },
                        label = { Text("Locker Number (e.g. L-101)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = floor,
                        onValueChange = { floor = it },
                        label = { Text("Floor / Area") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rent,
                            onValueChange = { rent = it },
                            label = { Text("Monthly Rent (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = deposit,
                            onValueChange = { deposit = it },
                            label = { Text("Deposit (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (lockerNumber.isNotBlank()) {
                            val newLocker = LockerEntity(
                                id = "LCK-${UUID.randomUUID().toString().take(8).uppercase()}",
                                libraryId = currentLib?.id ?: "LIB-DEMO",
                                lockerNumber = lockerNumber.trim(),
                                floor = floor.trim(),
                                monthlyRent = rent.toDoubleOrNull() ?: 200.0,
                                depositAmount = deposit.toDoubleOrNull() ?: 300.0,
                                size = size,
                                status = "AVAILABLE"
                            )
                            viewModel.saveLocker(newLocker)
                            showAddLockerDialog = false
                        }
                    }
                ) {
                    Text("Save Locker")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLockerDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Allocate Locker Dialog
    if (showAllocateDialog && selectedLockerForAction != null) {
        val locker = selectedLockerForAction!!
        var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
        var keyNumber by remember { mutableStateOf(locker.keyNumber.ifBlank { "KEY-${locker.lockerNumber}" }) }
        var expiryDays by remember { mutableStateOf("30") }

        AlertDialog(
            onDismissRequest = { showAllocateDialog = false },
            modifier = Modifier.imePadding(),
            title = { Text("Assign Locker ${locker.lockerNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select student to allocate this locker:", fontSize = 13.sp)

                    students.take(10).forEach { st ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStudent = st },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedStudent?.id == st.id)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedStudent?.id == st.id,
                                    onClick = { selectedStudent = st }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(st.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Seat: ${st.seatNumber} | Phone: ${st.mobile}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = keyNumber,
                        onValueChange = { keyNumber = it },
                        label = { Text("Key Number / Tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = expiryDays,
                        onValueChange = { expiryDays = it },
                        label = { Text("Duration (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val st = selectedStudent
                        if (st != null) {
                            val days = expiryDays.toIntOrNull() ?: 30
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
                            val expDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                            
                            viewModel.allocateLocker(
                                lockerId = locker.id,
                                studentId = st.id,
                                studentName = st.fullName,
                                studentPhone = st.mobile,
                                expiryDate = expDate,
                                keyNumber = keyNumber.trim()
                            )

                            // 1-Tap WhatsApp Alert option
                            WhatsAppAlertUtils.sendLockerAllocationAlert(
                                context = context,
                                studentName = st.fullName,
                                phone = st.mobile,
                                libraryName = currentLib?.name ?: "LibDesk",
                                lockerNumber = locker.lockerNumber,
                                keyNumber = keyNumber.trim(),
                                monthlyRent = locker.monthlyRent,
                                expiryDate = expDate
                            )

                            showAllocateDialog = false
                        }
                    }
                ) {
                    Text("Confirm & WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAllocateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LockerStatCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(count, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LockerGridCard(
    locker: LockerEntity,
    onAllocateClick: () -> Unit,
    onReleaseClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    val isOccupied = locker.status == "OCCUPIED"
    val isMaintenance = locker.status == "MAINTENANCE"
    val statusColor = when {
        isOccupied -> Color(0xFFEF4444)
        isMaintenance -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    locker.lockerNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        locker.status,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Floor: ${locker.floor}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Rent: ₹${locker.monthlyRent.toInt()}/mo",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            if (isOccupied) {
                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Student: ${locker.assignedStudentName}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (locker.expiryDate.isNotBlank()) {
                    Text(
                        "Till: ${locker.expiryDate}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            if (isOccupied) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onWhatsAppClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF25D366).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Button(
                        onClick = onReleaseClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                    ) {
                        Text("Release", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onAllocateClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                ) {
                    Text("Assign", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
