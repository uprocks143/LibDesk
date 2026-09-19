package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun SeatMatrixView(
    seats: List<SeatEntity>,
    halls: List<HallEntity>,
    students: List<StudentEntity>,
    shifts: List<ShiftEntity>,
    plans: List<MembershipPlanEntity>,
    onAssignSeat: (SeatEntity, StudentEntity, ShiftEntity?, MembershipPlanEntity?, String) -> Unit,
    onTransferSeat: (SeatEntity, SeatEntity, StudentEntity) -> Unit,
    onReleaseSeat: (SeatEntity) -> Unit,
    onUpdateStatus: (SeatEntity, String) -> Unit,
    onBatchGenerate: (String, Int, String, String, String, String, String, String, Double, Int?) -> Unit,
    onToggleReservation: (SeatEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(0) } 
    var selectedHallId by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var activeSeatDialog by remember { mutableStateOf<SeatEntity?>(null) }
    var showBatchGenerateModal by remember { mutableStateOf(false) }

    val filteredSeats = remember(seats, selectedHallId, selectedStatusFilter) {
        seats.filter { seat ->
            (selectedHallId == null || seat.hallId == selectedHallId) &&
            (selectedStatusFilter == null || seat.status.equals(selectedStatusFilter, ignoreCase = true))
        }
    }

    val totalCount = seats.size
    val occupiedCount = seats.count { it.status == "OCCUPIED" }
    val availableCount = seats.count { it.status == "AVAILABLE" }
    val reservedCount = seats.count { it.status == "RESERVED" }
    val maintenanceCount = seats.count { it.status == "MAINTENANCE" }
    val occupancyPercent = if (totalCount > 0) (occupiedCount * 100) / totalCount else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (viewMode == 0) MaterialTheme.colorScheme.onPrimaryContainer else Color.Transparent,
                            modifier = Modifier.clickable { viewMode = 0 }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = if (viewMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Floor Map",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (viewMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (viewMode == 1) MaterialTheme.colorScheme.onPrimaryContainer else Color.Transparent,
                            modifier = Modifier.clickable { viewMode = 1 }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = if (viewMode == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Grid Matrix",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (viewMode == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (viewMode == 2) MaterialTheme.colorScheme.onPrimaryContainer else Color.Transparent,
                            modifier = Modifier.clickable { viewMode = 2 }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = if (viewMode == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Visualizer",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (viewMode == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        when (viewMode) {
            0 -> {
                LibraryFloorMapView(
                    seats = seats,
                    halls = halls,
                    students = students,
                    shifts = shifts,
                    plans = plans,
                    onToggleReservation = onToggleReservation,
                    onAssignSeat = onAssignSeat,
                    onTransferSeat = onTransferSeat,
                    onReleaseSeat = onReleaseSeat,
                    onUpdateStatus = onUpdateStatus,
                    modifier = Modifier.weight(1f)
                )
            }
            1 -> {
                Column(modifier = Modifier.weight(1f)) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Grid Layout (${filteredSeats.size} Seats)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                            Text(
                                text = "Occupancy: $occupancyPercent%",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (occupancyPercent > 80) MaterialTheme.colorScheme.error else LibDeskColors.success,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        
                        FourSeatIndicatorsRow(
                            availableCount = availableCount,
                            occupiedCount = occupiedCount,
                            reservedCount = reservedCount,
                            maintenanceCount = maintenanceCount,
                            selectedStatusFilter = selectedStatusFilter,
                            onSelectFilter = { selectedStatusFilter = it }
                        )
                    }
                }

                
                if (halls.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedHallId == null,
                            onClick = { selectedHallId = null },
                            label = { Text("All Halls (${seats.size})", fontSize = 14.sp) }
                        )
                        halls.forEach { hall ->
                            val hallSeats = seats.count { it.hallId == hall.id }
                            FilterChip(
                                selected = selectedHallId == hall.id,
                                onClick = { selectedHallId = hall.id },
                                label = { Text("${hall.name} ($hallSeats)", fontSize = 14.sp) }
                            )
                        }
                    }
                }

                
                if (filteredSeats.isEmpty()) {
                    EmptyPlaceholder(
                        title = "No Seats Found",
                        description = "Configure seats for your library halls using the Add Seats button above.",
                        icon = Icons.Default.Chair,
                        actionLabel = "Generate Seats",
                        onAction = { showBatchGenerateModal = true },
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredSeats, key = { it.id }) { seat ->
                            SeatGridItem(
                                seat = seat,
                                onClick = { activeSeatDialog = seat }
                            )
                        }
                    }
                }
            }
            }
            else -> {

                SeatLayoutVisualizer(
                    seats = seats,
                    halls = halls,
                    students = students,
                    onToggleSeatStatus = { seatId, newStatus ->
                        val target = seats.find { it.id == seatId }
                        if (target != null) {
                            onUpdateStatus(target, newStatus)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    
    if (activeSeatDialog != null) {
        val currentSeat = seats.find { it.id == activeSeatDialog!!.id } ?: activeSeatDialog!!
        SeatDetailDialog(
            seat = currentSeat,
            allSeats = seats,
            students = students,
            shifts = shifts,
            plans = plans,
            onClose = { activeSeatDialog = null },
            onToggleReservation = {
                onToggleReservation(currentSeat)
                activeSeatDialog = null
            },
            onAssign = { student, shift, plan, date ->
                onAssignSeat(currentSeat, student, shift, plan, date)
                activeSeatDialog = null
            },
            onTransfer = { targetSeat, student ->
                onTransferSeat(currentSeat, targetSeat, student)
                activeSeatDialog = null
            },
            onRelease = {
                onReleaseSeat(currentSeat)
                activeSeatDialog = null
            },
            onUpdateStatus = { newStatus ->
                onUpdateStatus(currentSeat, newStatus)
                activeSeatDialog = null
            }
        )
    }

    
    if (showBatchGenerateModal) {
        BatchGenerateSeatsDialog(
            halls = halls,
            existingSeats = seats,
            onClose = { showBatchGenerateModal = false },
            onGenerate = { prefix, count, hallId, hallName, secId, secName, floor, type, fee, startNum ->
                onBatchGenerate(prefix, count, hallId, hallName, secId, secName, floor, type, fee, startNum)
                showBatchGenerateModal = false
            }
        )
    }
}

@Composable
fun FourSeatIndicatorsRow(
    availableCount: Int,
    occupiedCount: Int,
    reservedCount: Int,
    maintenanceCount: Int,
    selectedStatusFilter: String?,
    onSelectFilter: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SeatIndicatorBox(
            label = "Available",
            count = availableCount,
            color = SeatAvailable,
            isSelected = selectedStatusFilter == "AVAILABLE",
            onClick = { onSelectFilter(if (selectedStatusFilter == "AVAILABLE") null else "AVAILABLE") },
            modifier = Modifier.weight(1f)
        )
        SeatIndicatorBox(
            label = "Occupied",
            count = occupiedCount,
            color = SeatOccupied,
            isSelected = selectedStatusFilter == "OCCUPIED",
            onClick = { onSelectFilter(if (selectedStatusFilter == "OCCUPIED") null else "OCCUPIED") },
            modifier = Modifier.weight(1f)
        )
        SeatIndicatorBox(
            label = "Reserved",
            count = reservedCount,
            color = SeatReserved,
            isSelected = selectedStatusFilter == "RESERVED",
            onClick = { onSelectFilter(if (selectedStatusFilter == "RESERVED") null else "RESERVED") },
            modifier = Modifier.weight(1f)
        )
        SeatIndicatorBox(
            label = "Maint",
            count = maintenanceCount,
            color = SeatMaintenance,
            isSelected = selectedStatusFilter == "MAINTENANCE",
            onClick = { onSelectFilter(if (selectedStatusFilter == "MAINTENANCE") null else "MAINTENANCE") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SeatIndicatorBox(
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.18f) else color.copy(alpha = 0.08f),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) color else color.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when (label.uppercase()) {
                        "RESERVED", "RES" -> Icons.Default.BookmarkAdded
                        "OCCUPIED", "OCC" -> Icons.Default.EventSeat
                        "MAINT", "MAINTENANCE" -> Icons.Default.Build
                        else -> Icons.Default.Chair
                    },
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.5.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun SeatGridItem(
    seat: SeatEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = seat.status.uppercase()
    val (statusColor, bgAlpha, border) = when (status) {
        "AVAILABLE" -> Triple(SeatAvailable, 0.12f, SeatAvailable)
        "OCCUPIED" -> Triple(SeatOccupied, 0.15f, SeatOccupied)
        "RESERVED" -> Triple(SeatReserved, 0.20f, SeatReserved)
        "MAINTENANCE" -> Triple(SeatMaintenance, 0.25f, SeatMaintenance)
        else -> Triple(SeatDisabled, 0.10f, SeatDisabled)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = bgAlpha)),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .aspectRatio(1.02f)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = when (status) {
                        "RESERVED" -> "RES"
                        "AVAILABLE" -> "FREE"
                        "OCCUPIED" -> "OCC"
                        else -> "OFF"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when (status) {
                        "RESERVED" -> Icons.Default.BookmarkAdded
                        "OCCUPIED" -> Icons.Default.EventSeat
                        "MAINTENANCE" -> Icons.Default.Build
                        else -> Icons.Default.Chair
                    },
                    contentDescription = "Seat ${seat.seatNumber}",
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = seat.seatNumber,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            if (seat.assignedStudentName.isNotEmpty()) {
                Text(
                    text = seat.assignedStudentName.split(" ").firstOrNull() ?: "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = SeatOccupied,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = if (status == "RESERVED") "Hold" else seat.seatType.take(5),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (status == "RESERVED") SeatReserved else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SeatDetailDialog(
    seat: SeatEntity,
    allSeats: List<SeatEntity>,
    students: List<StudentEntity>,
    shifts: List<ShiftEntity>,
    plans: List<MembershipPlanEntity>,
    onClose: () -> Unit,
    onAssign: (StudentEntity, ShiftEntity?, MembershipPlanEntity?, String) -> Unit,
    onTransfer: (SeatEntity, StudentEntity) -> Unit,
    onRelease: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onToggleReservation: () -> Unit = {}
) {
    var isAssigning by remember { mutableStateOf(false) }
    var isTransferring by remember { mutableStateOf(false) }

    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
    var selectedShiftId by remember { mutableStateOf(shifts.firstOrNull()?.id ?: "") }
    var selectedPlanId by remember { mutableStateOf(plans.firstOrNull()?.id ?: "") }
    val defaultValidUntil = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    var validUntilDate by remember { mutableStateOf(defaultValidUntil) }

    var selectedTargetSeatId by remember { mutableStateOf(allSeats.firstOrNull { it.status == "AVAILABLE" }?.id ?: "") }

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Seat ${seat.seatNumber}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${seat.hallName} • ${seat.floor} • ${seat.seatType}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    StatusBadge(status = seat.status)
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                if (!isAssigning && !isTransferring) {

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (seat.status.equals("RESERVED", ignoreCase = true)) SeatReserved.copy(alpha = 0.15f) else SeatAvailable.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (seat.status.equals("RESERVED", ignoreCase = true)) SeatReserved else SeatAvailable),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleReservation() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (seat.status.equals("RESERVED", ignoreCase = true)) Icons.Default.CheckCircle else Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (seat.status.equals("RESERVED", ignoreCase = true)) SeatReserved else SeatAvailable,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (seat.status.equals("RESERVED", ignoreCase = true)) "Seat is Reserved" else "Seat is Available",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (seat.status.equals("RESERVED", ignoreCase = true)) SeatReserved else SeatAvailable
                                    )
                                    Text(
                                        text = "Tap to switch Available ↔ Reserved",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (seat.status.equals("RESERVED", ignoreCase = true)) SeatReserved else SeatAvailable
                            ) {
                                Text(
                                    text = if (seat.status.equals("RESERVED", ignoreCase = true)) "Make Free" else "Reserve",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    
                    if (seat.assignedStudentName.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("CURRENT OCCUPANT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(seat.assignedStudentName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (seat.assignedShiftName.isNotEmpty()) {
                                    Text("Shift: ${seat.assignedShiftName}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (seat.validUntil.isNotEmpty()) {
                                    Text("Validity: ${seat.validUntil}", fontSize = 14.sp, color = LibDeskColors.success, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { isTransferring = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Transfer Seat", fontSize = 14.sp)
                            }
                            OutlinedButton(
                                onClick = onRelease,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Release Seat", fontSize = 14.sp)
                            }
                        }
                    } else {
                        Text(
                            text = "This seat is currently unassigned. Monthly rate: ₹${seat.monthlyFee}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { isAssigning = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Assign to Student")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    
                    Text(text = "Change Seat Status:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = { onUpdateStatus("AVAILABLE") },
                            label = { Text("Available", fontSize = 14.sp) },
                            leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(SeatAvailable)) }
                        )
                        AssistChip(
                            onClick = { onUpdateStatus("RESERVED") },
                            label = { Text("Reserve", fontSize = 14.sp) },
                            leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(SeatReserved)) }
                        )
                        AssistChip(
                            onClick = { onUpdateStatus("MAINTENANCE") },
                            label = { Text("Maint.", fontSize = 14.sp) },
                            leadingIcon = { Box(Modifier.size(8.dp).clip(CircleShape).background(SeatMaintenance)) }
                        )
                    }
                } else if (isAssigning) {

                    Text(text = "Assign Student to Seat ${seat.seatNumber}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select Student:", fontSize = 14.sp)
                    students.forEach { st ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStudentId = st.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedStudentId == st.id,
                                onClick = { selectedStudentId = st.id }
                            )
                            Text("${st.fullName} (${st.studentCode})", fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isAssigning = false },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val student = students.find { it.id == selectedStudentId }
                                val shift = shifts.find { it.id == selectedShiftId }
                                val plan = plans.find { it.id == selectedPlanId }
                                if (student != null) {
                                    onAssign(student, shift, plan, validUntilDate)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirm")
                        }
                    }
                } else if (isTransferring) {

                    Text(text = "Transfer Student from Seat ${seat.seatNumber}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    val availableTargetSeats = allSeats.filter { it.status == "AVAILABLE" && it.id != seat.id }
                    if (availableTargetSeats.isEmpty()) {
                        Text("No other seats currently available for transfer.", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    } else {
                        Text("Select New Available Seat:", fontSize = 14.sp)
                        availableTargetSeats.take(5).forEach { target ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTargetSeatId = target.id }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedTargetSeatId == target.id,
                                    onClick = { selectedTargetSeatId = target.id }
                                )
                                Text("Seat ${target.seatNumber} (${target.hallName})", fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isTransferring = false },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val targetSeat = allSeats.find { it.id == selectedTargetSeatId }
                                val student = students.find { it.id == seat.assignedStudentId }
                                if (targetSeat != null && student != null) {
                                    onTransfer(targetSeat, student)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            enabled = availableTargetSeats.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Execute Transfer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatchGenerateSeatsDialog(
    halls: List<HallEntity>,
    existingSeats: List<SeatEntity> = emptyList(),
    onClose: () -> Unit,
    onGenerate: (String, Int, String, String, String, String, String, String, Double, Int?) -> Unit
) {
    var prefix by remember { mutableStateOf("A") }
    var countText by remember { mutableStateOf("20") }
    var customHallName by remember { mutableStateOf(halls.firstOrNull()?.name ?: "Main Study Hall") }
    var selectedHall by remember { mutableStateOf(halls.firstOrNull()) }
    var floor by remember { mutableStateOf("Ground Floor") }
    var seatType by remember { mutableStateOf("Standard") }
    var feeText by remember { mutableStateOf("1000") }

    
    val calculatedStart = remember(prefix, existingSeats) {
        val maxExisting = existingSeats
            .filter { it.seatNumber.startsWith(prefix.trim(), ignoreCase = true) }
            .mapNotNull {
                it.seatNumber.removePrefix(prefix.trim()).removePrefix("-").trim().toIntOrNull()
            }
            .maxOrNull() ?: 0
        maxExisting + 1
    }
    var startNumText by remember(calculatedStart) { mutableStateOf(calculatedStart.toString()) }

    val startNumber = startNumText.toIntOrNull() ?: calculatedStart
    val count = countText.toIntOrNull() ?: 1
    val previewEnd = (startNumber + count - 1).coerceAtLeast(startNumber)

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add Seats to Coaching / Library",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Generate multiple numbered seats at once for your floor layout.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it.uppercase() },
                        label = { Text("Prefix (A, B, C...)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it },
                        label = { Text("Seat Count") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startNumText,
                        onValueChange = { startNumText = it },
                        label = { Text("Start Number") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it },
                        label = { Text("Monthly Fee (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Seat Range Preview:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${prefix.trim().ifBlank { "S" }}-${String.format("%02d", startNumber)}  to  ${prefix.trim().ifBlank { "S" }}-${String.format("%02d", previewEnd)}  ($count seats)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Hall / Classroom / Reading Area:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))

                if (halls.isNotEmpty()) {
                    halls.forEach { hall ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedHall = hall
                                    customHallName = hall.name
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = selectedHall?.id == hall.id,
                                onClick = {
                                    selectedHall = hall
                                    customHallName = hall.name
                                }
                            )
                            Text(hall.name + if (hall.floor.isNotBlank()) " (${hall.floor})" else "", fontSize = 14.sp)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = customHallName,
                        onValueChange = { customHallName = it },
                        label = { Text("Hall / Room Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val fee = feeText.toDoubleOrNull() ?: 1000.0
                            val hall = selectedHall ?: halls.firstOrNull()
                            val hallId = hall?.id ?: ""
                            val hallName = hall?.name ?: customHallName.ifBlank { "Main Study Hall" }
                            val actualFloor = hall?.floor ?: floor
                            onGenerate(
                                prefix.trim().ifBlank { "A" },
                                count,
                                hallId,
                                hallName,
                                "",
                                "",
                                actualFloor,
                                seatType,
                                fee,
                                startNumber
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Generate $count Seats")
                    }
                }
            }
        }
    }
}
