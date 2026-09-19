package com.example.ui.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.HallEntity
import com.example.data.local.entities.MembershipPlanEntity
import com.example.data.local.entities.ShiftEntity

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun InfrastructureEditorModal(
    halls: List<HallEntity>,
    shifts: List<ShiftEntity>,
    plans: List<MembershipPlanEntity>,
    onAddHall: (name: String, floor: String, seats: Int) -> Unit,
    onAddShift: (name: String, start: String, end: String, fee: Double) -> Unit,
    onAddPlan: (name: String, duration: Int, fee: Double) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    
    var newHallName by remember { mutableStateOf("") }
    var newHallFloor by remember { mutableStateOf("") }
    var newHallSeats by remember { mutableStateOf("") }

    
    var newShiftName by remember { mutableStateOf("") }
    var newShiftStart by remember { mutableStateOf("") }
    var newShiftEnd by remember { mutableStateOf("") }
    var newShiftFee by remember { mutableStateOf("") }

    
    var newPlanName by remember { mutableStateOf("") }
    var newPlanDuration by remember { mutableStateOf("") }
    var newPlanFee by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onClose, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    "Edit Infrastructure & Batches", 
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(16.dp)
                )
                
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Halls", modifier = Modifier.padding(12.dp)) }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Shifts", modifier = Modifier.padding(12.dp)) }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text("Plans", modifier = Modifier.padding(12.dp)) }
                }
                
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.weight(1f).padding(16.dp).verticalScroll(scrollState)) {
                    when (selectedTab) {
                        0 -> {
                            Text("Existing Halls", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            halls.forEach { hall ->
                                Text("• ${hall.name} (${hall.floor}) — ${hall.seatCount} seats", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Add New Hall", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(value = newHallName, onValueChange = { newHallName = it }, label = { Text("Hall Name") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            OutlinedTextField(value = newHallFloor, onValueChange = { newHallFloor = it }, label = { Text("Floor (e.g. Ground Floor)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            OutlinedTextField(value = newHallSeats, onValueChange = { newHallSeats = it }, label = { Text("Total Seats") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                            Button(onClick = { 
                                val seats = newHallSeats.toIntOrNull() ?: 0
                                if (newHallName.isNotBlank() && seats > 0) {
                                    onAddHall(newHallName, newHallFloor, seats)
                                    newHallName = ""; newHallFloor = ""; newHallSeats = ""
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Add Hall") }
                        }
                        1 -> {
                            Text("Existing Shifts", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            shifts.forEach { shift ->
                                Text("• ${shift.name} (${shift.startTime} - ${shift.endTime}) — ₹${shift.fee}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Add New Shift", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(value = newShiftName, onValueChange = { newShiftName = it }, label = { Text("Shift Name") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = newShiftStart, onValueChange = { newShiftStart = it }, label = { Text("Start Time") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = newShiftEnd, onValueChange = { newShiftEnd = it }, label = { Text("End Time") }, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(value = newShiftFee, onValueChange = { newShiftFee = it }, label = { Text("Shift Fee (₹)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                            Button(onClick = { 
                                val fee = newShiftFee.toDoubleOrNull() ?: 0.0
                                if (newShiftName.isNotBlank() && newShiftStart.isNotBlank()) {
                                    onAddShift(newShiftName, newShiftStart, newShiftEnd, fee)
                                    newShiftName = ""; newShiftStart = ""; newShiftEnd = ""; newShiftFee = ""
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Add Shift") }
                        }
                        2 -> {
                            Text("Existing Plans", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            plans.forEach { plan ->
                                Text("• ${plan.name} (${plan.durationMonths} Months) — ₹${plan.baseFee}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Add New Plan", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            OutlinedTextField(value = newPlanName, onValueChange = { newPlanName = it }, label = { Text("Plan Name") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            OutlinedTextField(value = newPlanDuration, onValueChange = { newPlanDuration = it }, label = { Text("Duration (Months)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                            OutlinedTextField(value = newPlanFee, onValueChange = { newPlanFee = it }, label = { Text("Base Fee (₹)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                            Button(onClick = { 
                                val duration = newPlanDuration.toIntOrNull() ?: 1
                                val fee = newPlanFee.toDoubleOrNull() ?: 0.0
                                if (newPlanName.isNotBlank()) {
                                    onAddPlan(newPlanName, duration, fee)
                                    newPlanName = ""; newPlanDuration = ""; newPlanFee = ""
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Add Plan") }
                        }
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) {
                        Text("Close", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
