package com.example.ui.manager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.VisitorPassEntity
import com.example.ui.theme.LibDeskColors
import com.example.util.WhatsAppAlertUtils
import com.example.viewmodel.LibDeskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManagerVisitorPassModal(
    viewModel: LibDeskViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val passes by viewModel.visitorPasses.collectAsState()
    val currentLib by viewModel.currentLibrary.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f)
                .imePadding(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ConfirmationNumber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("1-Day Trial & Visitor Passes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${passes.count { it.status == "ACTIVE" }} Active Walk-Ins Today", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("issue_visitor_pass_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Issue New 1-Day Trial Pass", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (passes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No visitor passes issued today", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(passes, key = { it.id }) { pass ->
                            VisitorPassCard(
                                pass = pass,
                                onCheckoutClick = {
                                    viewModel.checkoutVisitorPass(pass.id)
                                },
                                onWhatsAppClick = {
                                    WhatsAppAlertUtils.sendVisitorPassAlert(
                                        context = context,
                                        visitorName = pass.visitorName,
                                        phone = pass.mobile,
                                        libraryName = currentLib?.name ?: "LibDesk",
                                        passNumber = pass.passNumber,
                                        timeSlot = pass.timeSlot,
                                        assignedSeat = pass.assignedSeatNumber,
                                        feeAmount = pass.feeAmount
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var visitorName by remember { mutableStateOf("") }
        var mobile by remember { mutableStateOf("") }
        var purpose by remember { mutableStateOf("1-Day Trial (UPSC Prep)") }
        var assignedSeat by remember { mutableStateOf("Temp Desk 01") }
        var timeSlot by remember { mutableStateOf("Full Day (8 AM - 8 PM)") }
        var fee by remember { mutableStateOf("100") }
        var paymentMode by remember { mutableStateOf("UPI") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            modifier = Modifier.imePadding(),
            title = { Text("Issue 1-Day Study Pass", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = visitorName,
                        onValueChange = { visitorName = it },
                        label = { Text("Visitor / Student Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { if (it.length <= 10) mobile = it },
                        label = { Text("Mobile & WhatsApp Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Text("+91 ", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = purpose,
                        onValueChange = { purpose = it },
                        label = { Text("Purpose / Target Exam") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = assignedSeat,
                            onValueChange = { assignedSeat = it },
                            label = { Text("Temp Seat") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fee,
                            onValueChange = { fee = it },
                            label = { Text("Pass Fee (₹)") },
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
                        if (visitorName.isNotBlank() && mobile.isNotBlank()) {
                            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            val now = Date()
                            val passNum = "VP-${(1000..9999).random()}"
                            
                            val pass = VisitorPassEntity(
                                id = "VP-${UUID.randomUUID().toString().take(8).uppercase()}",
                                libraryId = currentLib?.id ?: "LIB-DEMO",
                                passNumber = passNum,
                                visitorName = visitorName.trim(),
                                mobile = mobile.trim(),
                                purpose = purpose.trim(),
                                visitDate = sdfDate.format(now),
                                timeSlot = timeSlot,
                                assignedSeatNumber = assignedSeat.trim(),
                                feeAmount = fee.toDoubleOrNull() ?: 100.0,
                                paymentMode = paymentMode,
                                status = "ACTIVE",
                                checkInTime = sdfTime.format(now)
                            )
                            viewModel.saveVisitorPass(pass)

                            // 1-Tap WhatsApp
                            WhatsAppAlertUtils.sendVisitorPassAlert(
                                context = context,
                                visitorName = visitorName.trim(),
                                phone = mobile.trim(),
                                libraryName = currentLib?.name ?: "LibDesk",
                                passNumber = passNum,
                                timeSlot = timeSlot,
                                assignedSeat = assignedSeat.trim(),
                                feeAmount = fee.toDoubleOrNull() ?: 100.0
                            )

                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Issue & Share on WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun VisitorPassCard(
    pass: VisitorPassEntity,
    onCheckoutClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    val isActive = pass.status == "ACTIVE"

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, if (isActive) LibDeskColors.success.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(pass.visitorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("📞 +91 ${pass.mobile}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) LibDeskColors.success.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant
                ) {
                    Text(
                        if (isActive) "ACTIVE TODAY" else "CHECKED OUT",
                        color = if (isActive) LibDeskColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Desk: ${pass.assignedSeatNumber}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Text("In: ${pass.checkInTime}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Fee: ₹${pass.feeAmount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF25D366).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                }
                if (isActive) {
                    Button(
                        onClick = onCheckoutClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Log Punch-Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
