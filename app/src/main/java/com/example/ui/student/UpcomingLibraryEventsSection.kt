package com.example.ui.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.LibraryEventEntity
import com.example.data.local.entities.StudentEntity

/**
 * Modern, interactive Upcoming Library Events & Workshops Section.
 *
 * Displays:
 * 1. Category filtering (All, Mock Tests, Workshops, Silent Marathons, Book Clubs).
 * 2. Event cards with date, time, venue, speaker/mentor, and attendee count.
 * 3. 1-Click RSVP / Registration button with instant state update.
 * 4. Rich Event Details modal dialog.
 */
@Composable
fun UpcomingLibraryEventsSection(
    events: List<LibraryEventEntity>,
    currentStudentId: String,
    onRsvpEvent: (eventId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var selectedEventForDetail by remember { mutableStateOf<LibraryEventEntity?>(null) }

    val categories = remember {
        listOf(
            "ALL" to "All Events",
            "MOCK_TEST" to "Mock Tests",
            "WORKSHOP" to "Workshops",
            "COMMUNITY" to "Study Marathons",
            "BOOK_CLUB" to "Book Club",
            "MAINTENANCE" to "Maintenance"
        )
    }

    val filteredEvents = remember(events, selectedCategoryFilter) {
        if (selectedCategoryFilter == "ALL") events
        else events.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upcoming_library_events_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "UPCOMING LIBRARY EVENTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Workshops & Academic Meets",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${events.size} Scheduled",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { (catKey, catLabel) ->
                val isSelected = selectedCategoryFilter == catKey
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryFilter = catKey },
                    label = {
                        Text(
                            text = catLabel,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Events List Cards
        if (filteredEvents.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.EventBusy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No events in this category currently.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredEvents.forEach { event ->
                    LibraryEventCard(
                        event = event,
                        currentStudentId = currentStudentId,
                        onRsvp = { onRsvpEvent(event.id) },
                        onOpenDetail = { selectedEventForDetail = event }
                    )
                }
            }
        }
    }

    // Detail Dialog
    if (selectedEventForDetail != null) {
        LibraryEventDetailDialog(
            event = selectedEventForDetail!!,
            currentStudentId = currentStudentId,
            onRsvp = {
                onRsvpEvent(selectedEventForDetail!!.id)
                // keep modal state updated
                selectedEventForDetail = selectedEventForDetail?.let { ev ->
                    val isReg = ev.registeredStudentIds.contains(currentStudentId)
                    val newIds = if (isReg) ev.registeredStudentIds - currentStudentId else ev.registeredStudentIds + currentStudentId
                    ev.copy(registeredStudentIds = newIds)
                }
            },
            onDismiss = { selectedEventForDetail = null }
        )
    }
}

/**
 * Individual Event Card with category pill, speaker, venue, time, attendee counter, and RSVP.
 */
@Composable
private fun LibraryEventCard(
    event: LibraryEventEntity,
    currentStudentId: String,
    onRsvp: () -> Unit,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRegistered = event.registeredStudentIds.contains(currentStudentId)
    val maxSeats = event.maxParticipants
    val regCount = event.registeredStudentIds.size
    val isFull = maxSeats > 0 && regCount >= maxSeats && !isRegistered

    val categoryColor = when (event.category.uppercase()) {
        "MOCK_TEST" -> Color(0xFF6366F1)
        "WORKSHOP" -> Color(0xFF0D9488)
        "COMMUNITY" -> Color(0xFFF59E0B)
        "BOOK_CLUB" -> Color(0xFF8B5CF6)
        "MAINTENANCE" -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.2.dp,
            if (isRegistered) Color(0xFF10B981).copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Tag + Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.bannerEmoji,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = categoryColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = event.category.replace("_", " "),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.5.sp,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = event.date,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Title & Snippet
            Column {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Time & Venue Meta Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.time,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.venue,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Speaker Row
            if (event.speakerOrOrganizer.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.speakerOrOrganizer,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Footer Row: Attendance Count & RSVP Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spots / Registered Counter
                if (maxSeats > 0) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$regCount / $maxSeats",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFull) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Registered",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (maxSeats > regCount && !isRegistered) {
                            Text(
                                text = "${maxSeats - regCount} spots left",
                                fontSize = 10.5.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Open to All Scholars",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // RSVP / Register Action Button
                if (event.category.equals("MAINTENANCE", ignoreCase = true)) {
                    FilledTonalButton(
                        onClick = onOpenDetail,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("View Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onRsvp,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRegistered) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isRegistered) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isRegistered) "Registered ✓" else if (isFull) "Waitlist" else "RSVP Now",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rich Event Details Dialog.
 */
@Composable
fun LibraryEventDetailDialog(
    event: LibraryEventEntity,
    currentStudentId: String,
    onRsvp: () -> Unit,
    onDismiss: () -> Unit
) {
    val isRegistered = event.registeredStudentIds.contains(currentStudentId)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = event.bannerEmoji, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = event.category.replace("_", " "),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Schedule and Venue Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventDetailInfoRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Date & Schedule",
                        value = "${event.date} • ${event.time}"
                    )
                    EventDetailInfoRow(
                        icon = Icons.Default.LocationOn,
                        label = "Venue / Room",
                        value = event.venue
                    )
                    if (event.speakerOrOrganizer.isNotBlank()) {
                        EventDetailInfoRow(
                            icon = Icons.Default.Person,
                            label = "Host / Speaker",
                            value = event.speakerOrOrganizer
                        )
                    }
                    if (event.maxParticipants > 0) {
                        EventDetailInfoRow(
                            icon = Icons.Default.Group,
                            label = "Capacity & Attendance",
                            value = "${event.registeredStudentIds.size} / ${event.maxParticipants} registered scholars"
                        )
                    }
                }

                // Full Description
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "EVENT OVERVIEW & AGENDA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }

                    if (!event.category.equals("MAINTENANCE", ignoreCase = true)) {
                        Button(
                            onClick = onRsvp,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRegistered) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isRegistered) "Registered ✓" else "RSVP / Register",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDetailInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
