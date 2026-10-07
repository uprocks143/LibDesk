package com.example.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.LibDeskViewModel

@Composable
fun StudentSeatShiftRequestModal(
    viewModel: LibDeskViewModel,
    onDismiss: () -> Unit
) {
    val shifts by viewModel.shifts.collectAsState()
    val availableSeats by viewModel.availableSeats.collectAsState()
    val currentStudent by viewModel.currentStudent.collectAsState()

    var requestType by remember { mutableStateOf("SEAT_CHANGE") } // "SEAT_CHANGE", "SHIFT_CHANGE", "SEAT_AND_SHIFT"
    var selectedSeatNumber by remember { mutableStateOf(availableSeats.firstOrNull()?.seatNumber ?: "") }
    var selectedShift by remember { mutableStateOf(shifts.firstOrNull()) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request Seat / Shift Change", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Assigned Details
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Current Assignment:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("🪑 Seat: ${currentStudent?.seatNumber ?: "None"} | ⏰ Shift: ${currentStudent?.shiftName ?: "Standard"}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Request Type Selector
                Text("What would you like to change?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = requestType == "SEAT_CHANGE",
                        onClick = { requestType = "SEAT_CHANGE" },
                        label = { Text("Seat Only", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = requestType == "SHIFT_CHANGE",
                        onClick = { requestType = "SHIFT_CHANGE" },
                        label = { Text("Shift Only", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = requestType == "SEAT_AND_SHIFT",
                        onClick = { requestType = "SEAT_AND_SHIFT" },
                        label = { Text("Both", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Desired Seat
                if (requestType == "SEAT_CHANGE" || requestType == "SEAT_AND_SHIFT") {
                    OutlinedTextField(
                        value = selectedSeatNumber,
                        onValueChange = { selectedSeatNumber = it },
                        label = { Text("Desired Seat Number (e.g. Seat 12)") },
                        placeholder = { Text("Enter preferred seat") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Desired Shift
                if (requestType == "SHIFT_CHANGE" || requestType == "SEAT_AND_SHIFT") {
                    Text("Select Desired Shift:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    shifts.forEach { shift ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedShift?.id == shift.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedShift = shift }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedShift?.id == shift.id,
                                    onClick = { selectedShift = shift }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(shift.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${shift.startTime} - ${shift.endTime}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Reason input
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Change (Required)") },
                    placeholder = { Text("e.g. Need AC row / College timing changed") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isNotBlank()) {
                        viewModel.submitSeatShiftRequest(
                            requestedSeatNumber = if (requestType != "SHIFT_CHANGE") selectedSeatNumber.trim() else "",
                            requestedShiftId = if (requestType != "SEAT_CHANGE") selectedShift?.id ?: "" else "",
                            requestedShiftName = if (requestType != "SEAT_CHANGE") selectedShift?.name ?: "" else "",
                            requestType = requestType,
                            reason = reason.trim()
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("submit_seat_shift_request_btn")
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
