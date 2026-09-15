package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.ExpenseEntity
import com.example.data.local.entities.PaymentEntity
import com.example.ui.theme.MaterialTheme.colorScheme.outlineVariant
import com.example.ui.theme.MaterialTheme.colorScheme.primary
import com.example.ui.theme.MaterialTheme.colorScheme.error

/**
 * Modal to edit or correct a wrong Attendance entry in the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAttendanceModal(
    show: Boolean,
    attendance: AttendanceEntity?,
    onSave: (recordId: String, newDate: String, newCheckIn: String, newCheckOut: String, newStatus: String, newNotes: String) -> Unit,
    onDelete: (AttendanceEntity) -> Unit,
    onDismiss: () -> Unit
) {
    if (!show || attendance == null) return

    var date by remember(attendance) { mutableStateOf(attendance.date) }
    var checkInTime by remember(attendance) { mutableStateOf(attendance.checkInTime) }
    var checkOutTime by remember(attendance) { mutableStateOf(attendance.checkOutTime) }
    var status by remember(attendance) { mutableStateOf(attendance.status) }
    var notes by remember(attendance) { mutableStateOf(attendance.notes) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        ActionConfirmationDialog(
            show = showDeleteConfirm,
            title = "Delete Attendance Entry?",
            message = "Are you sure you want to permanently delete this attendance record for ${attendance.studentName}?",
            confirmText = "Delete Record",
            isDestructive = true,
            targetName = "${attendance.studentName} (${attendance.date})",
            onConfirm = {
                onDelete(attendance)
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("edit_attendance_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Correct Attendance Entry",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Student: ${attendance.studentName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Check-in & Check-out time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = checkInTime,
                        onValueChange = { checkInTime = it },
                        label = { Text("Check-In Time") },
                        placeholder = { Text("08:30 AM") },
                        leadingIcon = { Icon(Icons.Default.Login, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = checkOutTime,
                        onValueChange = { checkOutTime = it },
                        label = { Text("Check-Out Time") },
                        placeholder = { Text("02:00 PM") },
                        leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Status Selector
                Text(
                    text = "Attendance Status",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("CHECKED_IN", "CHECKED_OUT", "ABSENT").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = {
                                Text(
                                    text = when (st) {
                                        "CHECKED_IN" -> "In Library"
                                        "CHECKED_OUT" -> "Completed"
                                        else -> "Absent"
                                    },
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Notes / Correction remarks
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reason for Correction / Notes") },
                    placeholder = { Text("e.g. Corrected manual punch error") },
                    leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onSave(attendance.id, date, checkInTime, checkOutTime, status, notes)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Modal to edit or correct a wrong Payment entry in the app.
 */
@Composable
fun EditPaymentModal(
    show: Boolean,
    payment: PaymentEntity?,
    onSave: (payment: PaymentEntity, newAmount: Double, newMode: String, newPurpose: String, newRef: String, newRemarks: String, newDate: String) -> Unit,
    onDelete: (PaymentEntity) -> Unit,
    onDismiss: () -> Unit
) {
    if (!show || payment == null) return

    var amountText by remember(payment) { mutableStateOf(payment.amount.toInt().toString()) }
    var paymentMode by remember(payment) { mutableStateOf(payment.paymentMode) }
    var purpose by remember(payment) { mutableStateOf(payment.purpose) }
    var refNum by remember(payment) { mutableStateOf(payment.referenceNumber) }
    var remarks by remember(payment) { mutableStateOf(payment.remarks) }
    var date by remember(payment) { mutableStateOf(payment.date) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        ActionConfirmationDialog(
            show = showDeleteConfirm,
            title = "Delete Payment Receipt?",
            message = "Are you sure you want to permanently delete Receipt #${payment.receiptNumber}?",
            confirmText = "Delete Receipt",
            isDestructive = true,
            amount = payment.amount,
            targetName = "${payment.studentName} (Receipt #${payment.receiptNumber})",
            onConfirm = {
                onDelete(payment)
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("edit_payment_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Correct Payment Entry",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Receipt #${payment.receiptNumber} • ${payment.studentName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Correct Amount (₹)") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment Mode
                Text(
                    text = "Payment Mode",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("UPI", "CASH", "BANK_TRANSFER", "CARD").forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Reference number / Txn ID
                OutlinedTextField(
                    value = refNum,
                    onValueChange = { refNum = it },
                    label = { Text("UPI / Transaction Ref ID") },
                    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Remarks
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Correction Note") },
                    leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: payment.amount
                            onSave(payment, amt, paymentMode, purpose, refNum, remarks, date)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Update Receipt", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Modal to edit or correct a wrong Expense entry in the app.
 */
@Composable
fun EditExpenseModal(
    show: Boolean,
    expense: ExpenseEntity?,
    onSave: (expense: ExpenseEntity, newCategory: String, newAmount: Double, newDate: String, newDesc: String, newMode: String) -> Unit,
    onDelete: (ExpenseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    if (!show || expense == null) return

    var category by remember(expense) { mutableStateOf(expense.category) }
    var amountText by remember(expense) { mutableStateOf(expense.amount.toInt().toString()) }
    var date by remember(expense) { mutableStateOf(expense.date) }
    var description by remember(expense) { mutableStateOf(expense.description) }
    var paymentMode by remember(expense) { mutableStateOf(expense.paymentMode) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        ActionConfirmationDialog(
            show = showDeleteConfirm,
            title = "Delete Expense Entry?",
            message = "Are you sure you want to permanently delete this expense of ₹${expense.amount} for ${expense.category}?",
            confirmText = "Delete Expense",
            isDestructive = true,
            amount = expense.amount,
            targetName = expense.category,
            onConfirm = {
                onDelete(expense)
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("edit_expense_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Correct Expense Entry",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    placeholder = { Text("Electricity, Rent, Internet...") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Amount (₹)") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("UPI", "CASH", "BANK_TRANSFER").forEach { mode ->
                        FilterChip(
                            selected = paymentMode == mode,
                            onClick = { paymentMode = mode },
                            label = { Text(mode, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: expense.amount
                            onSave(expense, category, amt, date, description, paymentMode)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
