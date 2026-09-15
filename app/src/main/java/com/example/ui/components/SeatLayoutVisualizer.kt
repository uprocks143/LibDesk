package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.HallEntity
import com.example.data.local.entities.SeatEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.theme.*


val SeatStatusAvailable = SeatAvailable
val SeatStatusReserved = SeatReserved
val SeatStatusOccupied = SeatOccupied
val SeatStatusMaintenance = SeatMaintenance 


@Composable
fun SeatLayoutVisualizer(
    seats: List<SeatEntity>,
    halls: List<HallEntity> = emptyList(),
    students: List<StudentEntity> = emptyList(),
    onToggleSeatStatus: (seatId: String, newStatus: String) -> Unit,
    modifier: Modifier = Modifier,
    canToggleDirectly: Boolean = true
) {
    var selectedHallId by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isQuickToggleModeActive by remember { mutableStateOf(false) }
    var inspectingSeat by remember { mutableStateOf<SeatEntity?>(null) }

    val filteredSeats = remember(seats, selectedHallId, selectedStatusFilter, searchQuery) {
        seats.filter { seat ->
            (selectedHallId == null || seat.hallId == selectedHallId) &&
            (selectedStatusFilter == null || seat.status.equals(selectedStatusFilter, ignoreCase = true)) &&
            (searchQuery.isBlank() || seat.seatNumber.contains(searchQuery.trim(), ignoreCase = true))
        }
    }

    val totalCount = seats.size
    val availableCount = seats.count { it.status.equals("AVAILABLE", ignoreCase = true) }
    val reservedCount = seats.count { it.status.equals("RESERVED", ignoreCase = true) }
    val occupiedCount = seats.count { it.status.equals("OCCUPIED", ignoreCase = true) }
    val maintenanceCount = seats.count { it.status.equals("MAINTENANCE", ignoreCase = true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Seat Layout Visualizer",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = if (isQuickToggleModeActive) "Tap any seat button to cycle availability" else "Tap seat button to inspect & update status",
                    fontSize = 14.sp,
                    color = if (isQuickToggleModeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (canToggleDirectly) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isQuickToggleModeActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isQuickToggleModeActive = !isQuickToggleModeActive }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isQuickToggleModeActive) "Quick Toggle ON" else "Quick Toggle",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isQuickToggleModeActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusLegendCard(
                label = "Available",
                count = availableCount,
                color = SeatStatusAvailable,
                icon = Icons.Default.CheckCircle,
                isSelected = selectedStatusFilter == "AVAILABLE",
                onClick = {
                    selectedStatusFilter = if (selectedStatusFilter == "AVAILABLE") null else "AVAILABLE"
                },
                modifier = Modifier.weight(1f)
            )
            StatusLegendCard(
                label = "Reserved",
                count = reservedCount,
                color = SeatStatusReserved,
                icon = Icons.Default.Bookmark,
                isSelected = selectedStatusFilter == "RESERVED",
                onClick = {
                    selectedStatusFilter = if (selectedStatusFilter == "RESERVED") null else "RESERVED"
                },
                modifier = Modifier.weight(1f)
            )
            StatusLegendCard(
                label = "Occupied",
                count = occupiedCount,
                color = SeatStatusOccupied,
                icon = Icons.Default.Person,
                isSelected = selectedStatusFilter == "OCCUPIED",
                onClick = {
                    selectedStatusFilter = if (selectedStatusFilter == "OCCUPIED") null else "OCCUPIED"
                },
                modifier = Modifier.weight(1f)
            )
            StatusLegendCard(
                label = "Maint",
                count = maintenanceCount,
                color = SeatStatusMaintenance,
                icon = Icons.Default.Build,
                isSelected = selectedStatusFilter == "MAINTENANCE",
                onClick = {
                    selectedStatusFilter = if (selectedStatusFilter == "MAINTENANCE") null else "MAINTENANCE"
                },
                modifier = Modifier.weight(1f)
            )
        }

        
        if (halls.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedHallId == null,
                    onClick = { selectedHallId = null },
                    label = { Text("All Halls ($totalCount)") }
                )
                halls.forEach { hall ->
                    val hallSeats = seats.count { it.hallId == hall.id }
                    FilterChip(
                        selected = selectedHallId == hall.id,
                        onClick = { selectedHallId = hall.id },
                        label = { Text("${hall.name} ($hallSeats)") }
                    )
                }
            }
        }

        
        if (filteredSeats.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventSeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "No seats match this filter",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Clear your search or status filter to see all library seats.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 68.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                items(filteredSeats, key = { it.id }) { seat ->
                    SeatVisualizerButtonItem(
                        seat = seat,
                        isQuickToggleMode = isQuickToggleModeActive,
                        onButtonClick = {
                            if (isQuickToggleModeActive) {

                                val nextStatus = when (seat.status.uppercase()) {
                                    "AVAILABLE" -> "RESERVED"
                                    "RESERVED" -> "OCCUPIED"
                                    "OCCUPIED" -> "AVAILABLE"
                                    else -> "AVAILABLE"
                                }
                                onToggleSeatStatus(seat.id, nextStatus)
                                SnackbarController.showInfo("Seat ${seat.seatNumber} set to $nextStatus")
                            } else {
                                inspectingSeat = seat
                            }
                        }
                    )
                }
            }
        }
    }

    
    inspectingSeat?.let { currentSeat ->
        val assignedStudent = students.find { it.id == currentSeat.assignedStudentId }
            ?: students.find { it.seatId == currentSeat.id }

        SeatStatusToggleDialog(
            seat = currentSeat,
            student = assignedStudent,
            onClose = { inspectingSeat = null },
            onSelectNewStatus = { newStatus ->
                onToggleSeatStatus(currentSeat.id, newStatus)
                inspectingSeat = null
                SnackbarController.showSuccess("Seat ${currentSeat.seatNumber} is now $newStatus")
            }
        )
    }
}


@Composable
fun SeatVisualizerButtonItem(
    seat: SeatEntity,
    isQuickToggleMode: Boolean,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = seat.status.uppercase()
    val statusColor = when (status) {
        "AVAILABLE" -> SeatStatusAvailable
        "RESERVED" -> SeatStatusReserved
        "OCCUPIED" -> SeatStatusOccupied
        "MAINTENANCE" -> SeatStatusMaintenance
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val animatedBgColor by animateColorAsState(
        targetValue = statusColor.copy(alpha = 0.14f),
        animationSpec = tween(250),
        label = "seat_bg"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(250),
        label = "seat_border"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = animatedBgColor,
        border = BorderStroke(1.5.dp, animatedBorderColor),
        shadowElevation = 1.dp,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onButtonClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
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
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )

                Text(
                    text = when (status) {
                        "AVAILABLE" -> "FREE"
                        "RESERVED" -> "RES"
                        "OCCUPIED" -> "OCC"
                        else -> "MAINT"
                    },
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
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
                        "OCCUPIED" -> Icons.Default.Person
                        "MAINTENANCE" -> Icons.Default.Build
                        else -> Icons.Default.Chair
                    },
                    contentDescription = "Seat ${seat.seatNumber}",
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = seat.seatNumber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            
            Text(
                text = if (isQuickToggleMode) "Tap to Cycle" else seat.seatType.take(8),
                fontSize = 7.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun StatusLegendCard(
    label: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) color.copy(alpha = 0.20f) else color.copy(alpha = 0.08f),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) color else color.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "$count",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}


@Composable
fun SeatStatusToggleDialog(
    seat: SeatEntity,
    student: StudentEntity?,
    onClose: () -> Unit,
    onSelectNewStatus: (String) -> Unit
) {
    val currentStatus = seat.status.uppercase()

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Seat ${seat.seatNumber}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = "${seat.hallName.ifBlank { "Main Hall" }} • ${seat.floor}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (currentStatus) {
                        "AVAILABLE" -> SeatStatusAvailable.copy(alpha = 0.12f)
                        "RESERVED" -> SeatStatusReserved.copy(alpha = 0.12f)
                        "OCCUPIED" -> SeatStatusOccupied.copy(alpha = 0.12f)
                        else -> SeatStatusMaintenance.copy(alpha = 0.12f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when (currentStatus) {
                            "AVAILABLE" -> SeatStatusAvailable
                            "RESERVED" -> SeatStatusReserved
                            "OCCUPIED" -> SeatStatusOccupied
                            else -> SeatStatusMaintenance
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (currentStatus) {
                                "RESERVED" -> Icons.Default.BookmarkAdded
                                "OCCUPIED" -> Icons.Default.Person
                                "MAINTENANCE" -> Icons.Default.Build
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = when (currentStatus) {
                                "AVAILABLE" -> SeatStatusAvailable
                                "RESERVED" -> SeatStatusReserved
                                "OCCUPIED" -> SeatStatusOccupied
                                else -> SeatStatusMaintenance
                            }
                        )
                        Column {
                            Text(
                                text = "Current Status: $currentStatus",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (student != null) {
                                Text(
                                    text = "Assigned to: ${student.fullName} (${student.mobile})",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Toggle Availability Status:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSelectNewStatus("AVAILABLE") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStatus == "AVAILABLE") SeatStatusAvailable else SeatStatusAvailable.copy(alpha = 0.15f),
                            contentColor = if (currentStatus == "AVAILABLE") Color.White else SeatStatusAvailable
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as AVAILABLE (Free)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSelectNewStatus("RESERVED") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStatus == "RESERVED") SeatStatusReserved else SeatStatusReserved.copy(alpha = 0.15f),
                            contentColor = if (currentStatus == "RESERVED") Color.White else SeatStatusReserved
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as RESERVED", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSelectNewStatus("OCCUPIED") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentStatus == "OCCUPIED") SeatStatusOccupied else SeatStatusOccupied.copy(alpha = 0.15f),
                            contentColor = if (currentStatus == "OCCUPIED") Color.White else SeatStatusOccupied
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.EventSeat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as OCCUPIED (Checked In)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
