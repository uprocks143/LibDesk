package com.example.ui.student

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.*
import com.example.ui.theme.*

/**
 * View mode for the visual seat layout.
 */
enum class StudentSeatViewMode {
    FLOOR_MAP,
    GRID_MATRIX
}

/**
 * A student-facing visual seat layout component allowing students to explore
 * available, occupied, and reserved seats, locate their own allocated seat,
 * inspect seat amenities (power socket, silent zone, AC), and inquire about seats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSeatLayoutView(
    student: StudentEntity?,
    seats: List<SeatEntity>,
    halls: List<HallEntity> = emptyList(),
    library: LibraryEntity? = null,
    shifts: List<ShiftEntity> = emptyList(),
    onOpenQrScanner: () -> Unit = {},
    onViewIdCard: () -> Unit = {},
    onRequestSeatChange: (seat: SeatEntity, message: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // Ensure we always have realistic seat data even if the branch hasn't generated seats yet
    val effectiveSeats = remember(seats, student) {
        if (seats.isNotEmpty()) seats else generateDefaultBranchSeats(student)
    }

    var selectedHallId by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(StudentSeatViewMode.FLOOR_MAP) }
    var inspectingSeat by remember { mutableStateOf<SeatEntity?>(null) }
    var seatForRequestModal by remember { mutableStateOf<SeatEntity?>(null) }
    var requestConfirmationNote by remember { mutableStateOf<String?>(null) }

    val mySeatNumber = student?.seatNumber?.trim().orEmpty()
    val mySeatId = student?.seatId.orEmpty()

    // Distinct halls from entity list or seat metadata
    val availableHalls = remember(effectiveSeats, halls) {
        val hallNamesFromSeats = effectiveSeats.mapNotNull { it.hallName.ifBlank { null } }.distinct()
        if (halls.isNotEmpty()) {
            halls
        } else {
            hallNamesFromSeats.mapIndexed { idx, name ->
                HallEntity(
                    id = "hall-$idx",
                    libraryId = library?.id ?: "lib-001",
                    name = name,
                    type = "General Study",
                    floor = "Ground Floor",
                    isAc = true,
                    seatCount = effectiveSeats.count { it.hallName == name }
                )
            }
        }
    }

    // Filtered seats based on user controls
    val filteredSeats = remember(effectiveSeats, selectedHallId, selectedStatusFilter, searchQuery, mySeatNumber, mySeatId) {
        effectiveSeats.filter { seat ->
            val matchesHall = selectedHallId == null ||
                    seat.hallId == selectedHallId ||
                    (seat.hallName.isNotBlank() && availableHalls.find { it.id == selectedHallId }?.name.equals(seat.hallName, ignoreCase = true))

            val isMySeat = (mySeatNumber.isNotBlank() && seat.seatNumber.equals(mySeatNumber, ignoreCase = true)) ||
                    (mySeatId.isNotBlank() && seat.id == mySeatId)

            val matchesStatus = when (selectedStatusFilter) {
                null -> true
                "MY_SEAT" -> isMySeat
                else -> seat.status.equals(selectedStatusFilter, ignoreCase = true)
            }

            val matchesSearch = searchQuery.isBlank() ||
                    seat.seatNumber.contains(searchQuery.trim(), ignoreCase = true) ||
                    seat.seatType.contains(searchQuery.trim(), ignoreCase = true) ||
                    seat.sectionName.contains(searchQuery.trim(), ignoreCase = true)

            matchesHall && matchesStatus && matchesSearch
        }
    }

    // Counts for occupancy summary
    val totalCount = effectiveSeats.size
    val availableCount = effectiveSeats.count { it.status.equals("AVAILABLE", ignoreCase = true) }
    val occupiedCount = effectiveSeats.count { it.status.equals("OCCUPIED", ignoreCase = true) }
    val reservedCount = effectiveSeats.count { it.status.equals("RESERVED", ignoreCase = true) }
    val maintenanceCount = effectiveSeats.count { it.status.equals("MAINTENANCE", ignoreCase = true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("student_seat_layout_view"),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Confirmation Toast / Banner if a seat change request was sent
        AnimatedVisibility(visible = requestConfirmationNote != null) {
            Surface(
                color = LibDeskColors.successSoft,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, LibDeskColors.success.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LibDeskColors.success,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = requestConfirmationNote.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = LibDeskColors.success,
                                fontSize = 12.sp
                            )
                        )
                    }
                    IconButton(
                        onClick = { requestConfirmationNote = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = LibDeskColors.success,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Branch Capacity & Real-Time Occupancy Summary Card
            item {
                BranchCapacitySummaryCard(
                    libraryName = library?.name ?: "Central Campus Branch",
                    totalSeats = totalCount,
                    availableSeats = availableCount,
                    occupiedSeats = occupiedCount,
                    reservedSeats = reservedCount,
                    maintenanceSeats = maintenanceCount,
                    mySeatNumber = mySeatNumber
                )
            }

            // 2. Search, Hall Selection & Filter Toolbar
            item {
                SeatFilterControlBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    halls = availableHalls,
                    selectedHallId = selectedHallId,
                    onSelectHall = { selectedHallId = it },
                    selectedStatusFilter = selectedStatusFilter,
                    onSelectStatus = { selectedStatusFilter = it },
                    hasMySeat = mySeatNumber.isNotBlank(),
                    viewMode = viewMode,
                    onToggleViewMode = { viewMode = it }
                )
            }

            // 3. Main Visual Layout (Floor Map vs Grid Matrix)
            item {
                if (filteredSeats.isEmpty()) {
                    SeatEmptyFilterCard(onResetFilters = {
                        selectedHallId = null
                        selectedStatusFilter = null
                        searchQuery = ""
                    })
                } else {
                    when (viewMode) {
                        StudentSeatViewMode.FLOOR_MAP -> {
                            StudentSpatialFloorMap(
                                seats = filteredSeats,
                                mySeatNumber = mySeatNumber,
                                mySeatId = mySeatId,
                                onSelectSeat = { inspectingSeat = it }
                            )
                        }
                        StudentSeatViewMode.GRID_MATRIX -> {
                            StudentGridSeatMatrix(
                                seats = filteredSeats,
                                mySeatNumber = mySeatNumber,
                                mySeatId = mySeatId,
                                onSelectSeat = { inspectingSeat = it }
                            )
                        }
                    }
                }
            }

            // 4. Interactive Legend Bar
            item {
                SeatMapLegendCard()
            }
        }
    }

    // Seat Detail Inspection Bottom Sheet
    inspectingSeat?.let { seat ->
        val isMySeat = (mySeatNumber.isNotBlank() && seat.seatNumber.equals(mySeatNumber, ignoreCase = true)) ||
                (mySeatId.isNotBlank() && seat.id == mySeatId)

        StudentSeatDetailModal(
            seat = seat,
            isMySeat = isMySeat,
            student = student,
            onDismiss = { inspectingSeat = null },
            onOpenQrScanner = {
                inspectingSeat = null
                onOpenQrScanner()
            },
            onViewIdCard = {
                inspectingSeat = null
                onViewIdCard()
            },
            onRequestSeat = {
                inspectingSeat = null
                seatForRequestModal = seat
            }
        )
    }

    // Seat Change Request Dialog
    seatForRequestModal?.let { seat ->
        StudentSeatRequestDialog(
            seat = seat,
            currentSeat = mySeatNumber.ifBlank { "Unassigned" },
            onDismiss = { seatForRequestModal = null },
            onSubmit = { reason ->
                onRequestSeatChange(seat, reason)
                requestConfirmationNote = "Request for Desk #${seat.seatNumber} submitted to library manager!"
                seatForRequestModal = null
            }
        )
    }
}

/**
 * Branch Real-Time Capacity & Occupancy Overview Card.
 */
@Composable
private fun BranchCapacitySummaryCard(
    libraryName: String,
    totalSeats: Int,
    availableSeats: Int,
    occupiedSeats: Int,
    reservedSeats: Int,
    maintenanceSeats: Int,
    mySeatNumber: String,
    modifier: Modifier = Modifier
) {
    val occupancyPercent = if (totalSeats > 0) {
        (((occupiedSeats + reservedSeats).toFloat() / totalSeats) * 100).toInt()
    } else 0

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Library Title & Occupancy Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "BRANCH SEAT STATUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = libraryName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (occupancyPercent >= 90) DangerRedContainer
                    else if (occupancyPercent >= 70) WarningAmberContainer
                    else SuccessGreenContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (occupancyPercent >= 90) DangerRed
                                    else if (occupancyPercent >= 70) WarningAmber
                                    else SuccessGreen
                                )
                        )
                        Text(
                            text = "$occupancyPercent% Full",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (occupancyPercent >= 90) DangerRed
                                else if (occupancyPercent >= 70) Color(0xFF92400E)
                                else Color(0xFF065F46),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Segmented Progress Bar of Branch Capacity
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$availableSeats seats free right now",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SeatAvailable
                        )
                    )
                    Text(
                        text = "$totalSeats total branch capacity",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Visual Capacity Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        val occWeight = if (totalSeats > 0) occupiedSeats.toFloat() / totalSeats else 0f
                        val resWeight = if (totalSeats > 0) reservedSeats.toFloat() / totalSeats else 0f
                        val avWeight = if (totalSeats > 0) availableSeats.toFloat() / totalSeats else 0f

                        if (occWeight > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(occWeight)
                                    .background(SeatOccupied)
                            )
                        }
                        if (resWeight > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(resWeight)
                                    .background(SeatReserved)
                            )
                        }
                        if (avWeight > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(avWeight)
                                    .background(SeatAvailable)
                            )
                        }
                    }
                }
            }

            // Metric Counters Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCounterChip(
                    label = "Available",
                    count = availableSeats,
                    color = SeatAvailable,
                    modifier = Modifier.weight(1f)
                )
                MetricCounterChip(
                    label = "In Use",
                    count = occupiedSeats,
                    color = SeatOccupied,
                    modifier = Modifier.weight(1f)
                )
                MetricCounterChip(
                    label = "Reserved",
                    count = reservedSeats,
                    color = SeatReserved,
                    modifier = Modifier.weight(1f)
                )
                if (mySeatNumber.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Your Desk",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "#$mySeatNumber",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Metric chip inside capacity summary.
 */
@Composable
private fun MetricCounterChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 14.sp
                )
            )
        }
    }
}

/**
 * Filter and search control bar for seats.
 */
@Composable
private fun SeatFilterControlBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    halls: List<HallEntity>,
    selectedHallId: String?,
    onSelectHall: (String?) -> Unit,
    selectedStatusFilter: String?,
    onSelectStatus: (String?) -> Unit,
    hasMySeat: Boolean,
    viewMode: StudentSeatViewMode,
    onToggleViewMode: (StudentSeatViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search & View Mode Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search desk (e.g. A-04)", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("seat_search_input")
            )

            // Floor Map / Grid Toggle
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.height(50.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onToggleViewMode(StudentSeatViewMode.FLOOR_MAP) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (viewMode == StudentSeatViewMode.FLOOR_MAP)
                                    MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Floor Map View",
                            tint = if (viewMode == StudentSeatViewMode.FLOOR_MAP)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onToggleViewMode(StudentSeatViewMode.GRID_MATRIX) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (viewMode == StudentSeatViewMode.GRID_MATRIX)
                                    MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Grid View",
                            tint = if (viewMode == StudentSeatViewMode.GRID_MATRIX)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Hall Selection Chips
        if (halls.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedHallId == null,
                        onClick = { onSelectHall(null) },
                        label = { Text("All Halls", fontSize = 12.5.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                items(halls) { hall ->
                    FilterChip(
                        selected = selectedHallId == hall.id,
                        onClick = { onSelectHall(hall.id) },
                        label = {
                            Text(
                                text = hall.name,
                                fontSize = 12.5.sp,
                                maxLines = 1
                            )
                        },
                        leadingIcon = if (hall.isAc) {
                            {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Status Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                StatusFilterChip(
                    label = "All Statuses",
                    isSelected = selectedStatusFilter == null,
                    onClick = { onSelectStatus(null) }
                )
            }
            item {
                StatusFilterChip(
                    label = "Available Only",
                    color = SeatAvailable,
                    isSelected = selectedStatusFilter == "AVAILABLE",
                    onClick = { onSelectStatus(if (selectedStatusFilter == "AVAILABLE") null else "AVAILABLE") }
                )
            }
            item {
                StatusFilterChip(
                    label = "In Use",
                    color = SeatOccupied,
                    isSelected = selectedStatusFilter == "OCCUPIED",
                    onClick = { onSelectStatus(if (selectedStatusFilter == "OCCUPIED") null else "OCCUPIED") }
                )
            }
            item {
                StatusFilterChip(
                    label = "Reserved",
                    color = SeatReserved,
                    isSelected = selectedStatusFilter == "RESERVED",
                    onClick = { onSelectStatus(if (selectedStatusFilter == "RESERVED") null else "RESERVED") }
                )
            }
            if (hasMySeat) {
                item {
                    StatusFilterChip(
                        label = "My Seat",
                        color = MaterialTheme.colorScheme.primary,
                        leadingIcon = Icons.Default.Star,
                        isSelected = selectedStatusFilter == "MY_SEAT",
                        onClick = { onSelectStatus(if (selectedStatusFilter == "MY_SEAT") null else "MY_SEAT") }
                    )
                }
            }
        }
    }
}

/**
 * Filter chip with color indicator.
 */
@Composable
private fun StatusFilterChip(
    label: String,
    color: Color? = null,
    leadingIcon: ImageVector? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) {
            color?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (isSelected) (color ?: MaterialTheme.colorScheme.primary)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isSelected) (color ?: MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
            } else if (color != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) (color ?: MaterialTheme.colorScheme.onPrimaryContainer)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
        }
    }
}

/**
 * Spatial Floor Map representing the real hall architecture:
 * Windows at top, main aisle in the center, study pods on sides, and entrance gate at the bottom.
 */
@Composable
private fun StudentSpatialFloorMap(
    seats: List<SeatEntity>,
    mySeatNumber: String,
    mySeatId: String,
    onSelectSeat: (SeatEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Landmark: North Window & Quiet Zone (Top)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = InfoBlueContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, InfoBlue.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🪟 Natural Daylight Windows • Silent Study Zone",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = InfoBlue,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            // Hall Floor Plan Area
            // Partition seats into Left Wing, Center Corridor, Right Wing
            val leftWingSeats = remember(seats) {
                seats.filterIndexed { index, _ -> index % 2 == 0 }
            }
            val rightWingSeats = remember(seats) {
                seats.filterIndexed { index, _ -> index % 2 == 1 }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left Wing Cluster
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "WEST WING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    leftWingSeats.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { seat ->
                                val isMySeat = (mySeatNumber.isNotBlank() && seat.seatNumber.equals(mySeatNumber, ignoreCase = true)) ||
                                        (mySeatId.isNotBlank() && seat.id == mySeatId)

                                StudentSeatDeskItem(
                                    seat = seat,
                                    isMySeat = isMySeat,
                                    onClick = { onSelectSeat(seat) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Filler if odd
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Center Walking Aisle / Amenities Pillar
                Column(
                    modifier = Modifier
                        .width(44.dp)
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Wi-Fi 6",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "A\nI\nS\nL\nE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Water Station",
                            tint = InfoBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Right Wing Cluster
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "EAST WING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    rightWingSeats.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { seat ->
                                val isMySeat = (mySeatNumber.isNotBlank() && seat.seatNumber.equals(mySeatNumber, ignoreCase = true)) ||
                                        (mySeatId.isNotBlank() && seat.id == mySeatId)

                                StudentSeatDeskItem(
                                    seat = seat,
                                    isMySeat = isMySeat,
                                    onClick = { onSelectSeat(seat) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Landmark: Main Entrance & RFID Gate (Bottom)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🚪 Main Entrance • Smart RFID Gate & Attendance Hub",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Individual Study Desk representation inside the Floor Map.
 */
@Composable
private fun StudentSeatDeskItem(
    seat: SeatEntity,
    isMySeat: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = seat.status.uppercase()
    val isAvailable = status == "AVAILABLE"
    val isOccupied = status == "OCCUPIED"
    val isReserved = status == "RESERVED"

    // Theme colors
    val themeColor = when {
        isMySeat -> MaterialTheme.colorScheme.primary
        isAvailable -> SeatAvailable
        isOccupied -> SeatOccupied
        isReserved -> SeatReserved
        else -> SeatMaintenance
    }

    val containerColor = when {
        isMySeat -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        isAvailable -> SeatAvailable.copy(alpha = 0.10f)
        isOccupied -> SeatOccupied.copy(alpha = 0.08f)
        isReserved -> SeatReserved.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val borderColor = when {
        isMySeat -> MaterialTheme.colorScheme.primary
        isAvailable -> SeatAvailable.copy(alpha = 0.45f)
        isOccupied -> SeatOccupied.copy(alpha = 0.3f)
        isReserved -> SeatReserved.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (isMySeat) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("student_seat_${seat.seatNumber}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // "YOUR SEAT" Ribbon / Star Badge
            if (isMySeat) {
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(9.dp)
                        )
                        Text(
                            text = "YOU",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 8.5.sp
                            )
                        )
                    }
                }
            }

            // Power outlet icon indicator at top right
            Icon(
                imageVector = Icons.Default.Power,
                contentDescription = "AC Power",
                tint = if (isAvailable) SeatAvailable.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(11.dp)
            )

            // Center Content: Chair Icon & Seat Number
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chair,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = seat.seatNumber,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isMySeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = when {
                        isMySeat -> "Allocated"
                        isAvailable -> "Available"
                        isOccupied -> "Occupied"
                        isReserved -> "Reserved"
                        else -> "Service"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = themeColor,
                        fontSize = 9.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Grid Matrix representation grouping seats by Rows or Sections.
 */
@Composable
private fun StudentGridSeatMatrix(
    seats: List<SeatEntity>,
    mySeatNumber: String,
    mySeatId: String,
    onSelectSeat: (SeatEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "BRANCH DESK MATRIX",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp
                )
            )

            // Group seats by letter prefix (e.g. A-01, A-02 belongs to Row A)
            val groupedByRow = remember(seats) {
                seats.groupBy { seat ->
                    val clean = seat.seatNumber.trim()
                    if (clean.contains("-")) clean.substringBefore("-")
                    else if (clean.isNotEmpty()) clean.first().toString()
                    else "General"
                }
            }

            groupedByRow.forEach { (rowKey, rowSeats) ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Row $rowKey",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "${rowSeats.count { it.status.equals("AVAILABLE", true) }} free of ${rowSeats.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // 4-Column Grid for Desks
                    val chunks = rowSeats.chunked(3)
                    chunks.forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunk.forEach { seat ->
                                val isMySeat = (mySeatNumber.isNotBlank() && seat.seatNumber.equals(mySeatNumber, ignoreCase = true)) ||
                                        (mySeatId.isNotBlank() && seat.id == mySeatId)

                                StudentSeatGridItem(
                                    seat = seat,
                                    isMySeat = isMySeat,
                                    onClick = { onSelectSeat(seat) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (chunk.size < 3) {
                                repeat(3 - chunk.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Single Grid Item inside the Matrix View.
 */
@Composable
private fun StudentSeatGridItem(
    seat: SeatEntity,
    isMySeat: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = seat.status.uppercase()
    val isAvailable = status == "AVAILABLE"
    val isOccupied = status == "OCCUPIED"
    val isReserved = status == "RESERVED"

    val statusColor = when {
        isMySeat -> MaterialTheme.colorScheme.primary
        isAvailable -> SeatAvailable
        isOccupied -> SeatOccupied
        isReserved -> SeatReserved
        else -> SeatMaintenance
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isMySeat) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            if (isMySeat) 1.5.dp else 1.dp,
            if (isMySeat) MaterialTheme.colorScheme.primary else statusColor.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                if (isMySeat) {
                    Text(
                        text = "YOU",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 8.sp
                        )
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Power,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            Text(
                text = seat.seatNumber,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
            )

            Text(
                text = seat.seatType.ifBlank { "Standard" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Visual Legend explaining color codes.
 */
@Composable
private fun SeatMapLegendCard(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "SEAT COLOR CODING",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = SeatAvailable, label = "Available")
                LegendItem(color = SeatOccupied, label = "In Use")
                LegendItem(color = SeatReserved, label = "Reserved")
                LegendItem(color = MaterialTheme.colorScheme.primary, label = "Your Desk", isStar = true)
            }
        }
    }
}

/**
 * Single Legend key item.
 */
@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isStar: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (isStar) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

/**
 * Empty filter state card.
 */
@Composable
private fun SeatEmptyFilterCard(
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "No Desks Match Filters",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = "Try adjusting your search keyword or selected status filters to view other branch seats.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            )

            OutlinedButton(
                onClick = onResetFilters,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text("Reset All Filters")
            }
        }
    }
}

/**
 * Interactive Seat Inspection Bottom Sheet / Modal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentSeatDetailModal(
    seat: SeatEntity,
    isMySeat: Boolean,
    student: StudentEntity?,
    onDismiss: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onViewIdCard: () -> Unit,
    onRequestSeat: () -> Unit
) {
    val status = seat.status.uppercase()
    val isAvailable = status == "AVAILABLE"
    val isOccupied = status == "OCCUPIED"
    val isReserved = status == "RESERVED"

    val statusColor = when {
        isMySeat -> MaterialTheme.colorScheme.primary
        isAvailable -> SeatAvailable
        isOccupied -> SeatOccupied
        isReserved -> SeatReserved
        else -> SeatMaintenance
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("seat_detail_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Seat Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chair,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Desk #${seat.seatNumber}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = seat.seatType.ifBlank { "Dedicated Study Pod" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isMySeat) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = if (isMySeat) "YOUR DESK" else status,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Location & Plan Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SeatDetailInfoTile(
                    label = "HALL / ZONE",
                    value = seat.hallName.ifBlank { "Main Study Hall" },
                    icon = Icons.Default.MeetingRoom,
                    modifier = Modifier.weight(1f)
                )
                SeatDetailInfoTile(
                    label = "FLOOR LEVEL",
                    value = seat.floor.ifBlank { "Ground Floor" },
                    icon = Icons.Default.Layers,
                    modifier = Modifier.weight(1f)
                )
            }

            // Pricing & Shift info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SeatDetailInfoTile(
                    label = "MONTHLY RATE",
                    value = "₹${seat.monthlyFee.toInt()} / mo",
                    icon = Icons.Default.Payments,
                    modifier = Modifier.weight(1f)
                )
                SeatDetailInfoTile(
                    label = "SHIFT COMPATIBILITY",
                    value = if (seat.assignedShiftName.isNotBlank()) seat.assignedShiftName else "All Shifts (Full/Flexible)",
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )
            }

            // Seat Amenities Checklist
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DESK AMENITIES & FEATURES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SeatAmenityBadge(icon = Icons.Default.Power, label = "230V Universal Socket", modifier = Modifier.weight(1f))
                    SeatAmenityBadge(icon = Icons.Default.AcUnit, label = "Inverter AC 24°C", modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SeatAmenityBadge(icon = Icons.Default.Wifi, label = "Gigabit 5G Wi-Fi", modifier = Modifier.weight(1f))
                    SeatAmenityBadge(icon = Icons.Default.Lightbulb, label = "LED Reading Lamp", modifier = Modifier.weight(1f))
                }
            }

            // Action Buttons based on status
            if (isMySeat) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenQrScanner,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("seat_qr_checkin_btn")
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check-In at Desk (Scan QR)", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onViewIdCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Digital ID Pass", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (isAvailable) {
                Button(
                    onClick = onRequestSeat,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SeatAvailable),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("request_seat_btn")
                ) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request Seat Assignment / Change", fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isOccupied) "This desk is currently occupied by a registered member. Check other halls or inquire for next slot availability."
                            else "This desk is reserved for an upcoming member or maintenance check.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Seat detail info tile.
 */
@Composable
private fun SeatDetailInfoTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Badge for amenities with icon and text.
 */
@Composable
private fun SeatAmenityBadge(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Seat change / inquiry dialog.
 */
@Composable
private fun StudentSeatRequestDialog(
    seat: SeatEntity,
    currentSeat: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String) -> Unit
) {
    var reasonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Request Desk #${seat.seatNumber}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "You are requesting to switch or assign to Desk #${seat.seatNumber} in ${seat.hallName.ifBlank { "Main Study Hall" }}.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Current Desk: #$currentSeat",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("Reason or shift note (optional)") },
                    placeholder = { Text("e.g., Prefer window seat / morning study") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(reasonText) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Helper to generate realistic sample seats for a branch if none are configured in database.
 */
private fun generateDefaultBranchSeats(student: StudentEntity?): List<SeatEntity> {
    val mySeat = student?.seatNumber?.trim().orEmpty()
    val list = mutableListOf<SeatEntity>()

    val prefixes = listOf("A", "B", "C", "D")
    prefixes.forEachIndexed { rowIndex, prefix ->
        for (col in 1..8) {
            val seatNum = "$prefix-${String.format("%02d", col)}"
            val isStudentSeat = mySeat.isNotBlank() && seatNum.equals(mySeat, ignoreCase = true)

            val status = when {
                isStudentSeat -> "OCCUPIED"
                (rowIndex * 8 + col) % 4 == 0 -> "OCCUPIED"
                (rowIndex * 8 + col) % 6 == 0 -> "RESERVED"
                else -> "AVAILABLE"
            }

            val hallName = if (rowIndex < 2) "Main Quiet Reading Hall" else "AC Cubicles & Pods"
            val seatType = when {
                col in 1..2 -> "Window View Desk"
                col in 7..8 -> "Silent Pod Desk"
                else -> "Standard AC Desk"
            }

            list.add(
                SeatEntity(
                    id = "sample-seat-$prefix-$col",
                    libraryId = student?.libraryId ?: "lib-001",
                    seatNumber = seatNum,
                    hallId = if (rowIndex < 2) "hall-1" else "hall-2",
                    hallName = hallName,
                    sectionName = "Row $prefix",
                    floor = if (rowIndex < 2) "Ground Floor" else "1st Floor",
                    seatType = seatType,
                    monthlyFee = 1200.0,
                    status = status,
                    assignedStudentId = if (isStudentSeat) student?.id.orEmpty() else "",
                    assignedStudentName = if (isStudentSeat) student?.fullName.orEmpty() else "",
                    gridRow = rowIndex + 1,
                    gridCol = col
                )
            )
        }
    }
    return list
}
