package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.*
import com.example.ui.theme.*


@Composable
fun LibraryFloorMapView(
    seats: List<SeatEntity>,
    halls: List<HallEntity>,
    students: List<StudentEntity>,
    shifts: List<ShiftEntity>,
    plans: List<MembershipPlanEntity>,
    onToggleReservation: (SeatEntity) -> Unit,
    onAssignSeat: (SeatEntity, StudentEntity, ShiftEntity?, MembershipPlanEntity?, String) -> Unit,
    onTransferSeat: (SeatEntity, SeatEntity, StudentEntity) -> Unit,
    onReleaseSeat: (SeatEntity) -> Unit,
    onUpdateStatus: (SeatEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedHallId by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var isQuickToggleMode by remember { mutableStateOf(true) }
    var selectedSeatForDialog by remember { mutableStateOf<SeatEntity?>(null) }

    val filteredSeats = remember(seats, selectedHallId, selectedStatusFilter) {
        seats.filter { seat ->
            (selectedHallId == null || seat.hallId == selectedHallId) &&
            (selectedStatusFilter == null || seat.status.equals(selectedStatusFilter, ignoreCase = true))
        }
    }

    val availableCount = seats.count { it.status.equals("AVAILABLE", ignoreCase = true) }
    val reservedCount = seats.count { it.status.equals("RESERVED", ignoreCase = true) }
    val occupiedCount = seats.count { it.status.equals("OCCUPIED", ignoreCase = true) }
    val maintenanceCount = seats.count { it.status.equals("MAINTENANCE", ignoreCase = true) }
    val totalCount = seats.size

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Library Floor Map",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                        }
                        Text(
                            text = "$totalCount Desks Configured • Tap to Toggle",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    
                    Switch(
                        checked = isQuickToggleMode,
                        onCheckedChange = { isQuickToggleMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloorLegendChip(
                        label = "Available",
                        count = availableCount,
                        color = SeatAvailable,
                        isSelected = selectedStatusFilter == "AVAILABLE",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedStatusFilter = if (selectedStatusFilter == "AVAILABLE") null else "AVAILABLE"
                    }

                    FloorLegendChip(
                        label = "Occupied",
                        count = occupiedCount,
                        color = SeatOccupied,
                        isSelected = selectedStatusFilter == "OCCUPIED",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedStatusFilter = if (selectedStatusFilter == "OCCUPIED") null else "OCCUPIED"
                    }

                    FloorLegendChip(
                        label = "Reserved",
                        count = reservedCount,
                        color = SeatReserved,
                        isSelected = selectedStatusFilter == "RESERVED",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedStatusFilter = if (selectedStatusFilter == "RESERVED") null else "RESERVED"
                    }

                    FloorLegendChip(
                        label = "Maint",
                        count = maintenanceCount,
                        color = SeatMaintenance,
                        isSelected = selectedStatusFilter == "MAINTENANCE",
                        modifier = Modifier.weight(1f)
                    ) {
                        selectedStatusFilter = if (selectedStatusFilter == "MAINTENANCE") null else "MAINTENANCE"
                    }
                }
            }
        }

        
        if (halls.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedHallId == null,
                    onClick = { selectedHallId = null },
                    label = { Text("All Halls & Floors (${seats.size})", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                )
                halls.forEach { hall ->
                    val hallSeatCount = seats.count { it.hallId == hall.id }
                    FilterChip(
                        selected = selectedHallId == hall.id,
                        onClick = { selectedHallId = hall.id },
                        label = { Text("${hall.name} ($hallSeatCount)", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    )
                }
            }
        }

        
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredSeats.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventSeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Seats Matching Filters",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (seats.isEmpty()) "Add seats for your coaching/library using the matrix controls." else "Try clearing the hall or status filters above.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {

                        val displayedHalls = if (selectedHallId != null) {
                            halls.filter { it.id == selectedHallId }
                        } else {
                            halls
                        }

                        val hallColors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primary)
                        val handledSeatIds = mutableSetOf<String>()

                        displayedHalls.forEachIndexed { index, hall ->
                            val hallSeats = filteredSeats.filter {
                                it.hallId == hall.id || (it.hallId.isBlank() && it.hallName.equals(hall.name, ignoreCase = true))
                            }
                            if (hallSeats.isNotEmpty()) {
                                hallSeats.forEach { handledSeatIds.add(it.id) }
                                val color = hallColors[index % hallColors.size]
                                val floorSuffix = if (hall.floor.isNotBlank()) " • ${hall.floor.uppercase()}" else ""
                                FloorZoneSection(
                                    zoneTitle = "${hall.name.uppercase()}$floorSuffix",
                                    zoneIcon = when {
                                        hall.name.contains("Silent", ignoreCase = true) -> Icons.Default.VolumeOff
                                        hall.name.contains("Group", ignoreCase = true) || hall.name.contains("Discussion", ignoreCase = true) -> Icons.Default.Group
                                        hall.name.contains("AC", ignoreCase = true) -> Icons.Default.AcUnit
                                        else -> Icons.Default.MeetingRoom
                                    },
                                    zoneColor = color,
                                    seats = hallSeats,
                                    onSeatClick = { seat ->
                                        if (isQuickToggleMode) {
                                            onToggleReservation(seat)
                                        } else {
                                            selectedSeatForDialog = seat
                                        }
                                    },
                                    onSeatLongClick = { seat ->
                                        selectedSeatForDialog = seat
                                    }
                                )

                                if (index < displayedHalls.size - 1) {
                                    CorridorAisle(
                                        title = "CORRIDOR • POWER OUTLETS & HIGH SPEED FIBER WI-FI",
                                        icon = Icons.Default.Wifi
                                    )
                                }
                            }
                        }

                        
                        val remainingSeats = filteredSeats.filter { it.id !in handledSeatIds }
                        if (remainingSeats.isNotEmpty()) {

                            val remainingGroups = remainingSeats.groupBy { it.hallName.ifBlank { "Main Study Hall" } }
                            remainingGroups.entries.forEachIndexed { index, (groupHallName, groupSeats) ->
                                FloorZoneSection(
                                    zoneTitle = groupHallName.uppercase(),
                                    zoneIcon = Icons.Default.Chair,
                                    zoneColor = hallColors[(displayedHalls.size + index) % hallColors.size],
                                    seats = groupSeats,
                                    onSeatClick = { seat ->
                                        if (isQuickToggleMode) {
                                            onToggleReservation(seat)
                                        } else {
                                            selectedSeatForDialog = seat
                                        }
                                    },
                                    onSeatLongClick = { seat ->
                                        selectedSeatForDialog = seat
                                    }
                                )
                            }
                        }

                        
                    }
                }
            }
        }
    }

    
    if (selectedSeatForDialog != null) {
        val currentSeat = seats.find { it.id == selectedSeatForDialog!!.id } ?: selectedSeatForDialog!!
        SeatDetailDialog(
            seat = currentSeat,
            allSeats = seats,
            students = students,
            shifts = shifts,
            plans = plans,
            onClose = { selectedSeatForDialog = null },
            onAssign = { student, shift, plan, date ->
                onAssignSeat(currentSeat, student, shift, plan, date)
                selectedSeatForDialog = null
            },
            onTransfer = { targetSeat, student ->
                onTransferSeat(currentSeat, targetSeat, student)
                selectedSeatForDialog = null
            },
            onRelease = {
                onReleaseSeat(currentSeat)
                selectedSeatForDialog = null
            },
            onUpdateStatus = { newStatus ->
                onUpdateStatus(currentSeat, newStatus)
                selectedSeatForDialog = null
            }
        )
    }
}

@Composable
private fun FloorZoneSection(
    zoneTitle: String,
    zoneIcon: androidx.compose.ui.graphics.vector.ImageVector,
    zoneColor: Color,
    seats: List<SeatEntity>,
    onSeatClick: (SeatEntity) -> Unit,
    onSeatLongClick: (SeatEntity) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(zoneColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = zoneIcon, contentDescription = null, tint = zoneColor, modifier = Modifier.size(13.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = zoneTitle,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = zoneColor,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${seats.size} Desks",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse Zone" else "Expand Zone",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    if (seats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No seats matching current filter in this zone", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {

                        val chunkedSeats = seats.chunked(4)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            chunkedSeats.forEachIndexed { rowIndex, rowSeats ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    rowSeats.forEach { seat ->
                                        FloorMapSeatItem(
                                            seat = seat,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onSeatClick(seat) }
                                        )
                                    }

                                    if (rowSeats.size < 4) {
                                        repeat(4 - rowSeats.size) {
                                            Spacer(modifier = Modifier.weight(1f))
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
}


@Composable
fun FloorMapSeatItem(
    seat: SeatEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = seat.status.uppercase()
    val (statusColor, bgAlpha, border, statusLabel) = when (status) {
        "AVAILABLE" -> Quadruple(SeatAvailable, 0.12f, SeatAvailable, "AVAILABLE")
        "RESERVED" -> Quadruple(SeatReserved, 0.20f, SeatReserved, "RESERVED")
        "OCCUPIED" -> Quadruple(SeatOccupied, 0.15f, SeatOccupied, "OCCUPIED")
        "MAINTENANCE" -> Quadruple(SeatMaintenance, 0.25f, SeatMaintenance, "MAINT")
        else -> Quadruple(SeatDisabled, 0.10f, SeatDisabled, "DISABLED")
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = bgAlpha)
        ),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .aspectRatio(1.0f)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
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
                    fontSize = 6.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
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
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = seat.seatNumber,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            
            if (seat.assignedStudentName.isNotEmpty()) {
                Text(
                    text = seat.assignedStudentName.split(" ").firstOrNull() ?: "",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = SeatOccupied,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = if (status == "RESERVED") "Hold" else if (status == "AVAILABLE") "Open" else seat.seatType.take(4),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Medium,
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
private fun CorridorAisle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun FloorLegendChip(
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                fontSize = 8.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
