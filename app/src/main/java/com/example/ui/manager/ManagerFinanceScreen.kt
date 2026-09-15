package com.example.ui.manager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.entities.ExpenseEntity
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.PaymentEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.ImageShareUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManagerFinanceScreen(
    library: LibraryEntity?,
    payments: List<PaymentEntity>,
    expenses: List<ExpenseEntity>,
    students: List<StudentEntity>,
    onRecordPayment: (StudentEntity, Double, String, String, String, Double, String, String, (PaymentEntity) -> Unit) -> Unit,
    onAddExpense: (String, Double, String, String) -> Unit,
    onViewReceipt: (PaymentEntity) -> Unit,
    onUpdatePayment: ((PaymentEntity, Double, String, String, String, String, String) -> Unit)? = null,
    onDeletePayment: ((PaymentEntity) -> Unit)? = null,
    onUpdateExpense: ((ExpenseEntity, String, Double, String, String, String) -> Unit)? = null,
    onDeleteExpense: ((ExpenseEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } 
    var showAddPaymentModal by remember { mutableStateOf(false) }
    var showAddExpenseModal by remember { mutableStateOf(false) }
    var initialStudentForPayment by remember { mutableStateOf<String?>(null) }

    var paymentToEdit by remember { mutableStateOf<PaymentEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<PaymentEntity?>(null) }
    var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }

    val totalIncome = payments.sumOf { it.amount }
    val totalExpense = expenses.sumOf { it.amount }
    val netProfit = totalIncome - totalExpense
    val totalPendingDues = students.sumOf { it.dueAmount }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                Button(
                    onClick = {
                        initialStudentForPayment = null
                        showAddPaymentModal = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Record Fee", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            } else if (selectedTab == 1) {
                Button(
                    onClick = { showAddExpenseModal = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Expense", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Uncolored Overview Metrics Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                        ) {
                            Text("Total Collected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(formatCurrency(totalIncome), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                        ) {
                            Text("Pending Dues", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                formatCurrency(totalPendingDues),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                        ) {
                            Text("Total Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(formatCurrency(totalExpense), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp)
                        ) {
                            Text("Net Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(formatCurrency(netProfit), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Payment History (${payments.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Expenses (${expenses.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("UPI QR Code", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) })
            }

            when (selectedTab) {
                0 -> {
                    PaymentHistorySection(
                        library = library,
                        payments = payments,
                        students = students,
                        onViewReceipt = onViewReceipt,
                        onEditPayment = { paymentToEdit = it },
                        onDeletePayment = { paymentToDelete = it },
                        onCollectFee = { student ->
                            initialStudentForPayment = student.id
                            showAddPaymentModal = true
                        }
                    )
                }
                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(expenses, key = { it.id }) { exp ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exp.category, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${exp.description} • ${exp.date}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Mode: ${exp.paymentMode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(formatCurrency(exp.amount), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { expenseToEdit = exp },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Expense", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(
                                            onClick = { expenseToDelete = exp },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Expense", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Library Payment QR",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Display at reception for students to pay via GPay, PhonePe, Paytm",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val upiString = "upi://pay?pa=${library?.upiId ?: "vanguard@icici"}&pn=${library?.upiPayeeName ?: "Vanguard Library"}&cu=INR"
                        QrCodeView(data = upiString, size = 200.dp)

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("UPI ID: ${library?.upiId ?: "vanguard@icici"}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text("Payee: ${library?.upiPayeeName ?: "Vanguard Library"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showAddPaymentModal) {
        RecordPaymentDialog(
            students = students,
            initialStudentId = initialStudentForPayment,
            onClose = {
                showAddPaymentModal = false
                initialStudentForPayment = null
            },
            onConfirm = { student, amt, mode, purpose, ref, discount, remarks, period ->
                onRecordPayment(student, amt, mode, purpose, ref, discount, remarks, period) { receipt ->
                    onViewReceipt(receipt)
                }
                showAddPaymentModal = false
                initialStudentForPayment = null
            }
        )
    }

    if (showAddExpenseModal) {
        AddExpenseDialog(
            onClose = { showAddExpenseModal = false },
            onConfirm = { cat, amt, desc, mode ->
                onAddExpense(cat, amt, desc, mode)
                showAddExpenseModal = false
            }
        )
    }

    // Edit Payment Entry Modal
    if (paymentToEdit != null) {
        EditPaymentModal(
            show = true,
            payment = paymentToEdit,
            onSave = { pmt, newAmount, newMode, newPurpose, newRef, newRemarks, newDate ->
                onUpdatePayment?.invoke(pmt, newAmount, newMode, newPurpose, newRef, newRemarks, newDate)
                paymentToEdit = null
            },
            onDelete = { pmt ->
                onDeletePayment?.invoke(pmt)
                paymentToEdit = null
            },
            onDismiss = { paymentToEdit = null }
        )
    }

    // Delete Payment Confirmation Dialog
    if (paymentToDelete != null) {
        val pmt = paymentToDelete!!
        ActionConfirmationDialog(
            show = true,
            title = "Delete Fee Receipt #${pmt.receiptNumber}?",
            message = "Are you sure you want to permanently delete this fee receipt of ${formatCurrency(pmt.amount)} for ${pmt.studentName}? This will alter revenue records.",
            confirmText = "Delete Receipt",
            isDestructive = true,
            targetName = "${pmt.studentName} (₹${pmt.amount.toInt()})",
            onConfirm = {
                onDeletePayment?.invoke(pmt)
                paymentToDelete = null
            },
            onDismiss = { paymentToDelete = null }
        )
    }

    // Edit Expense Entry Modal
    if (expenseToEdit != null) {
        EditExpenseModal(
            show = true,
            expense = expenseToEdit,
            onSave = { exp, newCat, newAmt, newDate, newDesc, newMode ->
                onUpdateExpense?.invoke(exp, newCat, newAmt, newDate, newDesc, newMode)
                expenseToEdit = null
            },
            onDelete = { exp ->
                onDeleteExpense?.invoke(exp)
                expenseToEdit = null
            },
            onDismiss = { expenseToEdit = null }
        )
    }

    // Delete Expense Confirmation Dialog
    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        ActionConfirmationDialog(
            show = true,
            title = "Delete Expense Record?",
            message = "Are you sure you want to permanently delete this expense record of ${formatCurrency(exp.amount)} under ${exp.category}?",
            confirmText = "Delete Expense",
            isDestructive = true,
            targetName = "${exp.category} (₹${exp.amount.toInt()})",
            onConfirm = {
                onDeleteExpense?.invoke(exp)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }
}

@Composable
fun RecordPaymentDialog(
    students: List<StudentEntity>,
    onClose: () -> Unit,
    initialStudentId: String? = null,
    onConfirm: (StudentEntity, Double, String, String, String, Double, String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStudentId by remember { mutableStateOf(initialStudentId ?: students.firstOrNull()?.id ?: "") }
    val selectedStudent = remember(selectedStudentId, students) {
        students.find { it.id == selectedStudentId } ?: students.firstOrNull()
    }

    var amountText by remember(selectedStudent) {
        mutableStateOf(if ((selectedStudent?.dueAmount ?: 0.0) > 0) selectedStudent!!.dueAmount.toInt().toString() else "1000")
    }
    var discountText by remember { mutableStateOf("0") }
    var mode by remember { mutableStateOf("UPI") }
    var purpose by remember { mutableStateOf("MONTHLY_SEAT_FEE") }
    val todayStr = remember { DateRangeUtils.getTodayString() }
    var fromDate by remember(selectedStudent) {
        mutableStateOf(
            if (selectedStudent != null && selectedStudent.expiryDate.isNotBlank() && selectedStudent.expiryDate >= todayStr) {
                selectedStudent.expiryDate
            } else {
                todayStr
            }
        )
    }
    var toDate by remember(fromDate) {
        mutableStateOf(DateRangeUtils.addDaysToDate(fromDate, 30))
    }
    var remarks by remember { mutableStateOf("") }
    var refNum by remember { mutableStateOf("TXN-${(100000..999999).random()}") }

    val filteredStudents = remember(searchQuery, students) {
        if (searchQuery.isBlank()) students
        else students.filter {
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.studentCode.contains(searchQuery, ignoreCase = true) ||
            it.mobile.contains(searchQuery)
        }
    }

    val currentDue = selectedStudent?.dueAmount ?: 0.0
    val enteredAmount = amountText.toDoubleOrNull() ?: 0.0
    val enteredDiscount = discountText.toDoubleOrNull() ?: 0.0
    val remainingBalance = (currentDue - enteredAmount - enteredDiscount).coerceAtLeast(0.0)

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Record Fee Payment",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        )
                        Text(
                            text = "Generate instant digital invoice & receipt",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    }
                }

                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Student (Name / Code / Phone)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        if (filteredStudents.isEmpty()) {
                            Text("No students found", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                        } else {
                            filteredStudents.take(4).forEach { st ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedStudentId == st.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable {
                                            selectedStudentId = st.id
                                            amountText = if (st.dueAmount > 0) st.dueAmount.toInt().toString() else "1000"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    RadioButton(
                                        selected = selectedStudentId == st.id,
                                        onClick = {
                                            selectedStudentId = st.id
                                            amountText = if (st.dueAmount > 0) st.dueAmount.toInt().toString() else "1000"
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(st.fullName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("${st.studentCode} • Due: ${formatCurrency(st.dueAmount)}", fontSize = 10.5.sp, color = if (st.dueAmount > 0) MaterialTheme.colorScheme.error else LibDeskColors.success)
                                    }
                                }
                            }
                        }
                    }
                }

                
                if (selectedStudent != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Fee: ${formatCurrency(selectedStudent.totalFee)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Already Paid: ${formatCurrency(selectedStudent.paidAmount)}", fontSize = 14.sp, color = LibDeskColors.success, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Current Due: ${formatCurrency(selectedStudent.dueAmount)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                Text("New Balance: ${formatCurrency(remainingBalance)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount Paid (₹) *") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Discount (₹)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.8f)
                    )
                }

                
                Text("Payment Method", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("UPI", "CASH", "CARD", "NET_BANKING").forEach { m ->
                        FilterChip(
                            selected = mode.equals(m, ignoreCase = true),
                            onClick = { mode = m },
                            label = { Text(m, fontSize = 10.5.sp) }
                        )
                    }
                }

                
                Text("Payment Purpose", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "MONTHLY_SEAT_FEE" to "Seat Fee",
                        "ADMISSION_FEE" to "Admission",
                        "SECURITY_DEPOSIT" to "Deposit",
                        "LOCKER_FEE" to "Locker"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = purpose == key,
                            onClick = { purpose = key },
                            label = { Text(label, fontSize = 10.5.sp) }
                        )
                    }
                }

                DepositPeriodDateRangeSelector(
                    fromDate = fromDate,
                    toDate = toDate,
                    onFromDateChange = { fromDate = it },
                    onToDateChange = { toDate = it }
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks / Notes (Optional)") },
                    placeholder = { Text("e.g. Paid in full for Oct-Nov, UPI verified") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = refNum,
                    onValueChange = { refNum = it },
                    label = { Text("Txn / Reference ID") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                         onClick = onClose,
                         border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                         shape = RoundedCornerShape(10.dp),
                         colors = ButtonDefaults.outlinedButtonColors(
                             containerColor = MaterialTheme.colorScheme.surface,
                             contentColor = MaterialTheme.colorScheme.onSurface
                         ),
                         modifier = Modifier.weight(1f)
                     ) {
                         Text("Cancel")
                     }
                     Button(
                         onClick = {
                             val st = selectedStudent ?: students.find { it.id == selectedStudentId }
                             val amt = amountText.toDoubleOrNull() ?: 0.0
                             if (st != null && amt > 0) {
                                 val periodRange = "$fromDate to $toDate"
                                 onConfirm(st, amt, mode, purpose, refNum, enteredDiscount, remarks.trim(), periodRange)
                             }
                         },
                         shape = RoundedCornerShape(10.dp),
                         colors = ButtonDefaults.buttonColors(
                             containerColor = MaterialTheme.colorScheme.primary,
                             contentColor = Color.White
                         ),
                         enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0 && selectedStudent != null,
                         modifier = Modifier.weight(1f)
                     ) {
                         Text("Generate Receipt", fontWeight = FontWeight.Bold)
                     }
                 }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    onClose: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit
) {
    var category by remember { mutableStateOf("Electricity / Power Bill") }
    var amountText by remember { mutableStateOf("2500") }
    var description by remember { mutableStateOf("Monthly AC & power consumption") }
    var mode by remember { mutableStateOf("UPI") }

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Log Library Expense",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        )
                        Text(
                            text = "Track utility, maintenance, and operating costs",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Expense Category *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onClose,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirm(category, amt, description, mode)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Expense", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistorySection(
    library: LibraryEntity?,
    payments: List<PaymentEntity>,
    students: List<StudentEntity>,
    onViewReceipt: (PaymentEntity) -> Unit,
    onEditPayment: ((PaymentEntity) -> Unit)? = null,
    onDeletePayment: ((PaymentEntity) -> Unit)? = null,
    onCollectFee: (StudentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "PAID", "PENDING"
    var datePreset by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "THIS_MONTH", "LAST_30", "CUSTOM"
    var customFromDate by remember { mutableStateOf(DateRangeUtils.getDaysAgo(30)) }
    var customToDate by remember { mutableStateOf(DateRangeUtils.getToday()) }
    var showSummaryPreviewDialog by remember { mutableStateOf(false) }

    val (effectiveFrom, effectiveTo, dateRangeLabel) = remember(datePreset, customFromDate, customToDate) {
        when (datePreset) {
            "TODAY" -> Triple(DateRangeUtils.getToday(), DateRangeUtils.getToday(), "Today (${DateRangeUtils.getToday()})")
            "THIS_MONTH" -> Triple(DateRangeUtils.getStartOfMonth(), DateRangeUtils.getToday(), "This Month")
            "LAST_30" -> Triple(DateRangeUtils.getDaysAgo(30), DateRangeUtils.getToday(), "Last 30 Days")
            "CUSTOM" -> Triple(customFromDate, customToDate, "$customFromDate to $customToDate")
            else -> Triple("", "", "All Time")
        }
    }

    val studentsWithDues = remember(students) { students.filter { it.dueAmount > 0 } }

    val filteredPayments = remember(payments, searchQuery, effectiveFrom, effectiveTo) {
        payments.filter { p ->
            val matchQuery = searchQuery.isBlank() ||
                p.studentName.contains(searchQuery, ignoreCase = true) ||
                p.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                p.referenceNumber.contains(searchQuery, ignoreCase = true) ||
                p.paymentMode.contains(searchQuery, ignoreCase = true) ||
                p.period.contains(searchQuery, ignoreCase = true)
            val matchDate = effectiveFrom.isBlank() || effectiveTo.isBlank() ||
                DateRangeUtils.isDateWithinRange(p.date, effectiveFrom, effectiveTo)
            matchQuery && matchDate
        }
    }

    val filteredPendingStudents = remember(studentsWithDues, searchQuery, effectiveFrom, effectiveTo) {
        studentsWithDues.filter { s ->
            val matchQuery = searchQuery.isBlank() ||
                s.fullName.contains(searchQuery, ignoreCase = true) ||
                s.studentCode.contains(searchQuery, ignoreCase = true) ||
                s.id.contains(searchQuery, ignoreCase = true) ||
                s.mobile.contains(searchQuery) ||
                s.email.contains(searchQuery, ignoreCase = true)
            val matchDate = effectiveFrom.isBlank() || effectiveTo.isBlank() ||
                (s.expiryDate.isNotBlank() && DateRangeUtils.isDateWithinRange(s.expiryDate, effectiveFrom, effectiveTo))
            matchQuery && matchDate
        }
    }

    val totalCollected = filteredPayments.sumOf { it.amount }
    val totalPending = filteredPendingStudents.sumOf { it.dueAmount }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Student, Receipt #, or Ref") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Status Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == "ALL",
                    onClick = { statusFilter = "ALL" },
                    label = { Text("All (${filteredPayments.size + filteredPendingStudents.size})") },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = statusFilter == "PAID",
                    onClick = { statusFilter = "PAID" },
                    label = { Text("Paid (${filteredPayments.size})") },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(8.dp)
                )
                FilterChip(
                    selected = statusFilter == "PENDING",
                    onClick = { statusFilter = "PENDING" },
                    label = { Text("Pending (${filteredPendingStudents.size})") },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Date Range Preset Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "All Time",
                    "TODAY" to "Today",
                    "THIS_MONTH" to "This Month",
                    "LAST_30" to "Last 30 Days",
                    "CUSTOM" to "Custom Range"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = datePreset == key,
                        onClick = { datePreset = key },
                        label = { Text(label) },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Custom Date Range Selector
        if (datePreset == "CUSTOM") {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Select Custom Date Range",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    DateRangeUtils.showNativeDatePicker(context, customFromDate) {
                                        customFromDate = it
                                    }
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("From Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(customFromDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }

                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))

                            OutlinedButton(
                                onClick = {
                                    DateRangeUtils.showNativeDatePicker(context, customToDate) {
                                        customToDate = it
                                    }
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("To Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(customToDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Admin Export Toolbar
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Admin Export & Summaries",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateRangeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Collected: ${formatCurrency(totalCollected)}  •  Pending: ${formatCurrency(totalPending)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val csvContent = buildString {
                                    appendLine("Receipt Number,Date,Student Name,Amount,Payment Mode,Deposit Period,Reference,Due Balance")
                                    filteredPayments.forEach { p ->
                                        appendLine("\"${p.receiptNumber}\",\"${p.date}\",\"${p.studentName}\",\"${p.amount}\",\"${p.paymentMode}\",\"${p.period}\",\"${p.referenceNumber}\",\"${p.dueBalance}\"")
                                    }
                                }
                                ImageShareUtils.exportPaymentSummaryCsv(
                                    context = context,
                                    library = library,
                                    dateRangeText = dateRangeLabel,
                                    csvContent = csvContent
                                )
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = { showSummaryPreviewDialog = true },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Image Summary", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val highlights = filteredPayments.take(8).map {
                                    "${it.receiptNumber} | ${it.studentName} | ₹${it.amount.toInt()} | ${it.paymentMode}"
                                }
                                val summaryBitmap = ImageShareUtils.createPaymentSummaryBitmap(
                                    library = library,
                                    dateRangeText = dateRangeLabel,
                                    totalPaid = totalCollected,
                                    totalPending = totalPending,
                                    totalRecords = filteredPayments.size + filteredPendingStudents.size,
                                    paidCount = filteredPayments.size,
                                    pendingCount = filteredPendingStudents.size,
                                    highlights = highlights
                                )
                                val summaryText = "📊 Fee Collection Summary — ${library?.name ?: "LibDesk"}\nPeriod: $dateRangeLabel\nTotal Paid: ₹${totalCollected.toInt()} (${filteredPayments.size} txns)\nPending Dues: ₹${totalPending.toInt()}"
                                ImageShareUtils.shareSummaryViaWhatsApp(
                                    context = context,
                                    library = library,
                                    summaryText = summaryText,
                                    bitmap = summaryBitmap
                                )
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val highlights = filteredPayments.take(8).map {
                                    "${it.receiptNumber} | ${it.studentName} | ₹${it.amount.toInt()} | ${it.paymentMode}"
                                }
                                val summaryBitmap = ImageShareUtils.createPaymentSummaryBitmap(
                                    library = library,
                                    dateRangeText = dateRangeLabel,
                                    totalPaid = totalCollected,
                                    totalPending = totalPending,
                                    totalRecords = filteredPayments.size + filteredPendingStudents.size,
                                    paidCount = filteredPayments.size,
                                    pendingCount = filteredPendingStudents.size,
                                    highlights = highlights
                                )
                                val summaryText = "Fee Collection Summary Report — ${library?.name ?: "LibDesk"}\nPeriod: $dateRangeLabel\nTotal Paid: ₹${totalCollected.toInt()}\nPending Dues: ₹${totalPending.toInt()}"
                                ImageShareUtils.shareSummaryViaEmail(
                                    context = context,
                                    library = library,
                                    summaryText = summaryText,
                                    bitmap = summaryBitmap
                                )
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Email", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Records list
        val showPaid = statusFilter == "ALL" || statusFilter == "PAID"
        val showPending = statusFilter == "ALL" || statusFilter == "PENDING"

        if (showPaid) {
            items(filteredPayments, key = { "paid_${it.id}" }) { payment ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = payment.studentName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = "PAID",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = formatCurrency(payment.amount),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Receipt: #${payment.receiptNumber}  •  ${payment.paymentMode}  •  ${payment.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (payment.period.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Deposit Period: ${payment.period}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fee Record Actions: Distinct WhatsApp & SMS Buttons + Receipt Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Branded WhatsApp Action Button (Image Format)
                            Surface(
                                onClick = {
                                    val studentPhone = students.find { it.id == payment.studentId }?.mobile
                                    ImageShareUtils.shareReceiptViaWhatsApp(
                                        context = context,
                                        library = library,
                                        payment = payment,
                                        studentPhone = studentPhone
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF25D366),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("fee_whatsapp_action_${payment.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_whatsapp_real),
                                        contentDescription = "Send WhatsApp Receipt",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "WhatsApp",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            // Branded Mobile SMS Action Button (Text Format)
                            Surface(
                                onClick = {
                                    val studentPhone = students.find { it.id == payment.studentId }?.mobile
                                    ImageShareUtils.triggerSmsReceiptIntent(
                                        context = context,
                                        library = library,
                                        payment = payment,
                                        studentPhone = studentPhone
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("fee_sms_action_${payment.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_sms_real),
                                        contentDescription = "Send SMS Receipt",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SMS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            // View Receipt Outlined Button
                            OutlinedButton(
                                onClick = { onViewReceipt(payment) },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("fee_view_receipt_${payment.id}")
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Receipt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Secondary Actions Row (Email, Save Locally, Edit, Delete)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val studentEmail = students.find { it.id == payment.studentId }?.email
                                    ImageShareUtils.shareReceiptViaEmail(
                                        context = context,
                                        library = library,
                                        payment = payment,
                                        studentEmail = studentEmail
                                    )
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = "Email Receipt", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = {
                                    val bmp = ImageShareUtils.generateReceiptImage(library, payment)
                                    ImageShareUtils.saveReceiptLocally(context, bmp, "receipt_${payment.receiptNumber}.png")
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Download Receipt Image", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }

                            if (onEditPayment != null) {
                                IconButton(
                                    onClick = { onEditPayment(payment) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Receipt", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (onDeletePayment != null) {
                                IconButton(
                                    onClick = { onDeletePayment(payment) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Receipt", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPending) {
            items(filteredPendingStudents, key = { "pending_${it.id}" }) { student ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.fullName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = "PENDING DUE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = formatCurrency(student.dueAmount),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Code: ${student.studentCode}  •  Phone: ${student.mobile}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (student.expiryDate.isNotBlank()) {
                            Text(
                                text = "Expiry Date: ${student.expiryDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onCollectFee(student) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Collect Fee", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val msg = "Dear ${student.fullName}, this is a gentle reminder from ${library?.name ?: "the library"} regarding your pending fee dues of ₹${student.dueAmount.toInt()}. Please deposit your fee to continue uninterrupted seat access. Thank you!"
                                    com.example.util.ImageShareUtils.sendTextToWhatsApp(context, student.mobile, msg)
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remind via WhatsApp", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        if (filteredPayments.isEmpty() && (statusFilter == "PAID" || filteredPendingStudents.isEmpty())) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No fee records match the selected filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showSummaryPreviewDialog) {
        PaymentSummaryPreviewDialog(
            library = library,
            payments = filteredPayments,
            totalCollected = totalCollected,
            totalPendingDues = totalPending,
            filterDescription = dateRangeLabel,
            onClose = { showSummaryPreviewDialog = false }
        )
    }
}

@Composable
fun PaymentSummaryPreviewDialog(
    library: LibraryEntity?,
    payments: List<PaymentEntity>,
    totalCollected: Double,
    totalPendingDues: Double,
    filterDescription: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val highlights = remember(payments) {
        payments.take(8).map {
            "${it.receiptNumber} | ${it.studentName} | ₹${it.amount.toInt()} | ${it.paymentMode}"
        }
    }
    val summaryBitmap = remember(library, payments, totalCollected, totalPendingDues, filterDescription) {
        ImageShareUtils.createPaymentSummaryBitmap(
            library = library,
            dateRangeText = filterDescription,
            totalPaid = totalCollected,
            totalPending = totalPendingDues,
            totalRecords = payments.size,
            paidCount = payments.size,
            pendingCount = 0,
            highlights = highlights
        )
    }
    val summaryText = "📊 Fee Summary Report — ${library?.name ?: "LibDesk"}\nPeriod: $filterDescription\nTotal Collected: ₹${totalCollected.toInt()} (${payments.size} txns)\nPending Dues: ₹${totalPendingDues.toInt()}"

    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Fee Summary Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = filterDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = summaryBitmap.asImageBitmap(),
                        contentDescription = "Fee Summary Preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val fileName = "summary_${System.currentTimeMillis()}.png"
                            ImageShareUtils.saveReceiptLocally(context, summaryBitmap, fileName)
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Locally")
                    }

                    OutlinedButton(
                        onClick = {
                            ImageShareUtils.shareSummaryViaWhatsApp(
                                context = context,
                                library = library,
                                summaryText = summaryText,
                                bitmap = summaryBitmap
                            )
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp")
                    }

                    OutlinedButton(
                        onClick = {
                            ImageShareUtils.shareSummaryViaEmail(
                                context = context,
                                library = library,
                                summaryText = summaryText,
                                bitmap = summaryBitmap
                            )
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Email")
                    }

                    OutlinedButton(
                        onClick = {
                            ImageShareUtils.shareImageGeneric(
                                context = context,
                                bitmap = summaryBitmap,
                                fileName = "summary_${System.currentTimeMillis()}.png",
                                title = "Library Fee Payment Summary",
                                captionText = summaryText
                            )
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}
