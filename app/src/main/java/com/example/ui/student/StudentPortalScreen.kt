package com.example.ui.student

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.*
import com.example.ui.components.QrCodeView
import com.example.ui.components.StatusBadge
import com.example.ui.components.MembershipStatusCard
import com.example.ui.components.calculateMembershipExpiration
import com.example.ui.components.formatCurrency
import com.example.ui.manager.NoticeDetailDialog
import com.example.notification.StudentNotificationHelper
import com.example.ui.pdf.PdfViewerDialog
import com.example.ui.theme.*
import com.example.R
import com.example.util.ImageShareUtils
import androidx.compose.ui.res.painterResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudentPortalScreen(
    library: LibraryEntity?,
    activeStudent: StudentEntity?,
    attendanceList: List<AttendanceEntity>,
    digitalMaterials: List<DigitalMaterialEntity>,
    books: List<PhysicalBookEntity> = emptyList(),
    bookIssues: List<BookIssueEntity> = emptyList(),
    notices: List<NoticeEntity>,
    complaints: List<FeedbackComplaintEntity>,
    payments: List<PaymentEntity>,
    seats: List<SeatEntity> = emptyList(),
    shifts: List<ShiftEntity> = emptyList(),
    plans: List<MembershipPlanEntity> = emptyList(),
    halls: List<HallEntity> = emptyList(),
    onViewIdCard: (StudentEntity) -> Unit,
    onViewReceipt: (PaymentEntity) -> Unit,
    onToggleBookmark: (DigitalMaterialEntity) -> Unit,
    onSubmitComplaint: (String, String, String) -> Unit,
    onOpenQrScanner: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onRequestLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } 
    var showComplaintModal by remember { mutableStateOf(false) }
    var selectedNoticeForDetail by remember { mutableStateOf<NoticeEntity?>(null) }
    var bookCatalogQuery by remember { mutableStateOf("") }
    var selectedPdfToView by remember { mutableStateOf<DigitalMaterialEntity?>(null) }

    // SECURITY: this must NEVER fall back to `students.firstOrNull()`.
    // That fallback (plus the "Select Active Student" picker further below,
    // which is now removed) let any logged-in student browse and view every
    // other student's profile, fees, and attendance in the library. Only the
    // session's own bound student record may ever be shown here.
    val student = activeStudent

    if (student == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "We couldn't load your student profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onRequestLogout) {
                    Text("Sign out and try again")
                }
            }
        }
        return
    }

    val studentAttendance = remember(attendanceList, student) {
        if (student == null) emptyList()
        else attendanceList.filter { it.studentId == student.id || it.studentName == student.fullName }
    }

    val studentPayments = remember(payments, student) {
        if (student == null) emptyList()
        else payments.filter { it.studentId == student.id || it.studentName == student.fullName }
    }

    val studentComplaints = remember(complaints, student) {
        if (student == null) emptyList()
        else complaints.filter { it.studentId == student.id }
    }

    val studentBookIssues = remember(bookIssues, student) {
        if (student == null) emptyList()
        else bookIssues.filter { it.studentId == student.id || it.studentName.equals(student.fullName, ignoreCase = true) }
    }

    val filteredBooks = remember(books, bookCatalogQuery) {
        if (bookCatalogQuery.isBlank()) books
        else books.filter {
            it.title.contains(bookCatalogQuery, ignoreCase = true) ||
            it.author.contains(bookCatalogQuery, ignoreCase = true) ||
            it.category.contains(bookCatalogQuery, ignoreCase = true) ||
            it.subject.contains(bookCatalogQuery, ignoreCase = true)
        }
    }

    val daysRemaining = remember(student?.studentCode, student?.expiryDate, student?.status) {
        if (student == null) 30
        else calculateMembershipExpiration(student.joiningDate, student.expiryDate, student.status).daysRemaining
    }

    val currentShift = remember(shifts, student) {
        shifts.find { it.name.equals(student?.shiftName, ignoreCase = true) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(student?.id) {
        StudentNotificationHelper.initChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (student != null) {
            StudentNotificationHelper.scheduleSeatShiftReminderAlarm(context, student, currentShift)
            StudentNotificationHelper.sendMembershipExpiryReminder(
                context = context,
                student = student,
                libraryName = library?.name ?: "Your Library"
            )
        }
    }

    val shouldInterceptStudentBack = selectedNoticeForDetail != null ||
            showComplaintModal ||
            bookCatalogQuery.isNotEmpty() ||
            selectedTab != 0

    BackHandler(enabled = shouldInterceptStudentBack) {
        when {
            selectedNoticeForDetail != null -> selectedNoticeForDetail = null
            showComplaintModal -> showComplaintModal = false
            bookCatalogQuery.isNotEmpty() -> bookCatalogQuery = ""
            selectedTab != 0 -> selectedTab = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onOpenProfile() }
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (student?.fullName ?: "S").take(1).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = student?.fullName ?: "Select Student",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            text = "Seat: ${student?.seatNumber?.ifEmpty { "Unassigned" }} • ${student?.shiftName ?: "Full Day"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                if (student != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenProfile,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Button(
                            onClick = { onViewIdCard(student) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ID Pass", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            edgePadding = 12.dp
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Dashboard", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) })
            Tab(selected = selectedTab == 6, onClick = { selectedTab = 6 }, text = { Text("Seat Layout", fontWeight = if (selectedTab == 6) FontWeight.Bold else FontWeight.Normal) })
            Tab(selected = selectedTab == 5, onClick = { selectedTab = 5 }, text = { Text("Profile & Seat", fontWeight = if (selectedTab == 5) FontWeight.Bold else FontWeight.Normal) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("E-Resources", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("My Books", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Fee & Dues", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) })
            Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Help & Notices", fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) })
        }

        when (selectedTab) {
            0 -> {
                StudentMemberDashboard(
                    student = student,
                    library = library,
                    attendanceList = attendanceList,
                    seats = seats,
                    shifts = shifts,
                    plans = plans,
                    halls = halls,
                    onOpenQrScanner = onOpenQrScanner,
                    onViewIdCard = onViewIdCard,
                    onGoToFeeTab = { selectedTab = 3 },
                    onGoToProfileTab = { selectedTab = 5 },
                    onGoToSeatLayout = { selectedTab = 6 },
                    modifier = Modifier.fillMaxSize()
                )
            }
            1 -> {

                var searchQuery by remember { mutableStateOf("") }
                val filteredResources = remember(digitalMaterials, searchQuery) {
                    if (searchQuery.isBlank()) digitalMaterials
                    else digitalMaterials.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search notes, PDFs, PYQs...") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (filteredResources.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Study Materials Found", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Manager has not uploaded materials in this category yet.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        items(filteredResources, key = { it.id }) { item ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
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
                                        Row(
                                            modifier = Modifier.weight(1f).padding(end = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (item.fileType.equals("PDF", ignoreCase = true)) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (item.fileType.equals("PDF", ignoreCase = true)) Icons.Default.PictureAsPdf else Icons.Default.Article,
                                                    contentDescription = null,
                                                    tint = if (item.fileType.equals("PDF", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(item.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }

                                        IconButton(
                                            onClick = { onToggleBookmark(item) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (item.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                                contentDescription = null,
                                                tint = if (item.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                                Text(
                                                    item.category,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                                Text(
                                                    item.fileSize,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                selectedPdfToView = item
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Read", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("${studentBookIssues.count { it.status == "ISSUED" }} Books", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Active Loans", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (studentBookIssues.any { it.status == "OVERDUE" }) MaterialTheme.colorScheme.errorContainer else LibDeskColors.successSoft),
                                border = BorderStroke(1.dp, if (studentBookIssues.any { it.status == "OVERDUE" }) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else LibDeskColors.success.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = if (studentBookIssues.any { it.status == "OVERDUE" }) MaterialTheme.colorScheme.error else LibDeskColors.success, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val overdueCount = studentBookIssues.count { it.status == "OVERDUE" }
                                    Text(if (overdueCount > 0) "$overdueCount Overdue" else "No Dues", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (overdueCount > 0) MaterialTheme.colorScheme.error else LibDeskColors.success)
                                    Text("Fine: ₹5/day after due", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    item {
                        Text("My Borrowed Books:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    if (studentBookIssues.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(imageVector = Icons.Default.LibraryBooks, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No Books Currently Issued", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        items(studentBookIssues, key = { it.id }) { issue ->
                            val isOverdue = issue.status.equals("OVERDUE", ignoreCase = true)
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(issue.bookTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                        StatusBadge(status = issue.status)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Issued: ${issue.issueDate}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Due Date: ${issue.dueDate}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    if (issue.fineAmount > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Fine Accrued: ${formatCurrency(issue.fineAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Search Library Catalog:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = bookCatalogQuery,
                            onValueChange = { bookCatalogQuery = it },
                            placeholder = { Text("Search by title, author, subject...") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    items(filteredBooks, key = { it.id }) { book ->
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(book.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("By ${book.author} • ${book.category}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = if (book.availableCopies > 0) LibDeskColors.successSoft else MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            text = if (book.availableCopies > 0) "${book.availableCopies} Avail" else "All Issued",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (book.availableCopies > 0) LibDeskColors.success else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Location: ${book.rack} • ${book.shelf} • Acc: ${book.accessionNumber}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            3 -> {

                var paymentFilter by remember { mutableStateOf("ALL") }
                val filteredPayments = remember(studentPayments, paymentFilter) {
                    when (paymentFilter) {
                        "UPI" -> studentPayments.filter { it.paymentMode.equals("UPI", ignoreCase = true) }
                        "CASH" -> studentPayments.filter { it.paymentMode.equals("CASH", ignoreCase = true) }
                        else -> studentPayments
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Fee & Payment Health",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            "Plan: ${student?.planName ?: "Standard"}",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if ((student?.dueAmount ?: 0.0) <= 0) {
                                        Surface(
                                            color = LibDeskColors.successSoft,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, LibDeskColors.success.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = LibDeskColors.success,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Paid in Full ✓",
                                                    color = LibDeskColors.success,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Due: ${formatCurrency(student?.dueAmount ?: 0.0)}",
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val totalFee = student?.totalFee ?: 1000.0
                                val paid = student?.paidAmount ?: 0.0
                                val progress = if (totalFee > 0) (paid / totalFee).toFloat().coerceIn(0f, 1f) else 1f

                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = if (progress >= 1f) LibDeskColors.success else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Plan Fee:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatCurrency(student?.totalFee ?: 0.0), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Amount Paid:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatCurrency(student?.paidAmount ?: 0.0), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LibDeskColors.success)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Remaining Balance Due:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        formatCurrency(student?.dueAmount ?: 0.0),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if ((student?.dueAmount ?: 0.0) > 0) MaterialTheme.colorScheme.error else LibDeskColors.success
                                    )
                                }

                                if (!library?.upiId.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Pay Directly via UPI", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                Text(library?.upiId ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Past Payment Transactions",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${studentPayments.size} ${if (studentPayments.size == 1) "Record" else "Records"}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Verified transaction history, audit timestamps, reference IDs, and tax receipts.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (studentPayments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = paymentFilter == "ALL",
                                        onClick = { paymentFilter = "ALL" },
                                        label = { Text("All (${studentPayments.size})", fontSize = 14.sp) }
                                    )
                                    FilterChip(
                                        selected = paymentFilter == "UPI",
                                        onClick = { paymentFilter = "UPI" },
                                        label = { Text("UPI", fontSize = 14.sp) }
                                    )
                                    FilterChip(
                                        selected = paymentFilter == "CASH",
                                        onClick = { paymentFilter = "CASH" },
                                        label = { Text("Cash", fontSize = 14.sp) }
                                    )
                                }
                            }
                        }
                    }

                    if (filteredPayments.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "No Payment Transactions Found",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "When you pay subscription or seat fees at the library desk or via UPI, verified timestamped receipts will appear here.",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredPayments, key = { it.id }) { pay ->
                            StudentPaymentTransactionCard(
                                payment = pay,
                                library = library,
                                student = student,
                                onViewReceipt = { onViewReceipt(pay) }
                            )
                        }
                    }
                }
            }
            4 -> {

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Student Help Desk & Grievances", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { showComplaintModal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Submit Feedback / Grievance", fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    item {
                        Text("Notice Board:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    items(notices, key = { it.id }) { notice ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedNoticeForDetail = notice }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(notice.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    StatusBadge(status = notice.priority)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(notice.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Date: ${notice.date} • Category: ${notice.category}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Read more ↗", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
            5 -> {

                StudentProfileSection(
                    student = student,
                    library = library,
                    seats = seats,
                    shifts = shifts,
                    plans = plans,
                    halls = halls,
                    bookIssues = bookIssues,
                    daysRemaining = daysRemaining,
                    onOpenEditProfile = onOpenProfile,
                    onViewIdCard = { onViewIdCard(student) },
                    onGoToFeeTab = { selectedTab = 3 },
                    onOpenQrScanner = onOpenQrScanner,
                    onRequestLogout = onRequestLogout,
                    onGoToSeatLayout = { selectedTab = 6 }
                )
            }
            6 -> {
                StudentSeatLayoutView(
                    student = student,
                    seats = seats,
                    halls = halls,
                    library = library,
                    shifts = shifts,
                    onOpenQrScanner = onOpenQrScanner,
                    onViewIdCard = { if (student != null) onViewIdCard(student) },
                    onRequestSeatChange = { seat, msg ->
                        onSubmitComplaint(
                            "Seat Request: Desk #${seat.seatNumber}",
                            "Seat change/assignment request for Desk #${seat.seatNumber} (${seat.hallName}, ${seat.seatType}): $msg",
                            "SEAT"
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showComplaintModal) {
        StudentComplaintDialog(
            onClose = { showComplaintModal = false },
            onSubmit = { subj, msg, type ->
                onSubmitComplaint(subj, msg, type)
                showComplaintModal = false
            }
        )
    }

    selectedNoticeForDetail?.let { notice ->
        NoticeDetailDialog(
            notice = notice,
            onClose = { selectedNoticeForDetail = null }
        )
    }

    selectedPdfToView?.let { pdf ->
        PdfViewerDialog(
            title = pdf.title,
            category = pdf.category,
            subject = pdf.subject,
            fileSize = pdf.fileSize,
            fileUriOrUrl = pdf.fileUrl,
            description = pdf.description,
            onDismiss = { selectedPdfToView = null }
        )
    }
}

@Composable
fun StudentStatusDashboardCards(
    student: StudentEntity?,
    libraryName: String = "Your Library",
    studentShift: ShiftEntity? = null,
    daysRemaining: Int = 30,
    onGoToFeeTab: () -> Unit = {},
    onOpenQrScanner: () -> Unit = {}
) {
    val context = LocalContext.current
    if (student != null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MembershipStatusCard(
                student = student,
                onRenew = onGoToFeeTab,
                onPayDues = onGoToFeeTab,
                onSendReminder = {
                    val sent = StudentNotificationHelper.sendMembershipExpiryReminder(
                        context = context,
                        student = student,
                        libraryName = libraryName,
                        force = true
                    )
                    if (sent) {
                        // Toast.makeText(context, "Membership reminder notification delivered", Toast.LENGTH_SHORT).show()
                    } else {
                        // Toast.makeText(context, "Reminders are disabled or notification permission missing", Toast.LENGTH_SHORT).show()
                    }
                },
                showActions = true,
                modifier = Modifier.fillMaxWidth()
            )

            StudentNotificationRemindersCard(
                student = student,
                studentShift = studentShift,
                libraryName = libraryName,
                onOpenQrScanner = onOpenQrScanner
            )
        }
    }
}

@Composable
fun StudentNotificationRemindersCard(
    student: StudentEntity,
    studentShift: ShiftEntity?,
    libraryName: String,
    onOpenQrScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpiryEnabled by remember {
        mutableStateOf(StudentNotificationHelper.isExpiryReminderEnabled(context))
    }
    var isSeatEnabled by remember {
        mutableStateOf(StudentNotificationHelper.isSeatReminderEnabled(context))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Smart Notifications & Reminders",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Local alerts for expiration & reserved seats",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Toggle 1: Membership Expiration Reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Membership Expiry Reminders",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Alerts at 7 days, 3 days, and on expiration day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isExpiryEnabled,
                    onCheckedChange = { checked ->
                        isExpiryEnabled = checked
                        StudentNotificationHelper.setExpiryReminderEnabled(context, checked)
                        if (checked) {
                            StudentNotificationHelper.sendMembershipExpiryReminder(
                                context = context,
                                student = student,
                                libraryName = libraryName
                            )
                        }
                    }
                )
            }

            // Toggle 2: Seat Reservation / Shift Start Reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EventSeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        val seatDesc = if (student.seatNumber.isNotBlank()) "Seat ${student.seatNumber}" else "Assigned Seat"
                        Text(
                            text = "Seat Reservation Start Alerts ($seatDesc)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val shiftTime = studentShift?.let { "${it.startTime} - ${it.endTime}" } ?: student.shiftName.ifEmpty { "Daily Shift" }
                        Text(
                            text = "Triggers when $shiftTime begins with instant QR check-in",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isSeatEnabled,
                    onCheckedChange = { checked ->
                        isSeatEnabled = checked
                        StudentNotificationHelper.setSeatReminderEnabled(context, checked)
                        if (checked) {
                            StudentNotificationHelper.scheduleSeatShiftReminderAlarm(context, student, studentShift)
                        }
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Action Buttons Row: Test Reminders & Instant QR Scan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val (expSent, seatSent) = StudentNotificationHelper.sendTestReminders(
                            context = context,
                            student = student,
                            shift = studentShift,
                            libraryName = libraryName
                        )
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Test Reminders",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onOpenQrScanner,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scan Seat / Gate QR",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
fun StudentComplaintDialog(
    onClose: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("AC & Cooling") }

    val categories = listOf("AC & Cooling", "Wi-Fi Network", "Seat & Lighting", "Silence / Noise", "Restroom & Cleanliness", "Other")

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
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Submit Library Grievance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                Text("Category:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedType == cat,
                            onClick = { selectedType = cat },
                            label = { Text(cat, fontSize = 14.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Issue Subject *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Detailed Description *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (subject.isNotBlank() && message.isNotBlank()) {
                                onSubmit(subject, message, selectedType)
                            }
                        },
                        enabled = subject.isNotBlank() && message.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

@Composable
fun StudentPaymentTransactionCard(
    payment: PaymentEntity,
    library: LibraryEntity? = null,
    student: StudentEntity? = null,
    onViewReceipt: () -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = LibDeskColors.successSoft,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, LibDeskColors.success.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = LibDeskColors.success,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "COMPLETED",
                            color = LibDeskColors.success,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatPaymentDateTime(payment.createdAt, payment.date),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Receipt #${payment.receiptNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (payment.paymentMode.equals("UPI", ignoreCase = true)) "⚡ UPI" else "💵 Cash",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = payment.purpose.replace("_", " "),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            if (payment.referenceNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ref / UTR: ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = payment.referenceNumber,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (payment.notes.isNotBlank() && !payment.notes.contains("Payment via")) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Note: ${payment.notes}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(10.dp))

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Amount Paid",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "+${formatCurrency(payment.amount)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LibDeskColors.success
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Remaining Due",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(payment.dueBalance),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (payment.dueBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onViewReceipt,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("student_view_receipt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "View Receipt",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Receipt", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: WhatsApp (Image Format) & SMS (Text Format) with real distinct symbols
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        ImageShareUtils.shareReceiptViaWhatsApp(
                            context = context,
                            library = library,
                            payment = payment,
                            studentPhone = student?.mobile
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("student_whatsapp_receipt_button")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp_real),
                        contentDescription = "Share Receipt via WhatsApp in Image Format",
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WhatsApp",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        ImageShareUtils.triggerSmsReceiptIntent(
                            context = context,
                            library = library,
                            payment = payment,
                            studentPhone = student?.mobile
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0288D1),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("student_sms_receipt_button")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sms_real),
                        contentDescription = "Send Fee Record via Mobile SMS in Text Format",
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SMS",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatPaymentDateTime(createdAt: Long, dateStr: String): String {
    return try {
        if (createdAt > 0) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(createdAt))
        } else {
            val parseSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsed = parseSdf.parse(dateStr)
            if (parsed != null) {
                val outSdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                outSdf.format(parsed)
            } else {
                dateStr
            }
        }
    } catch (_: Exception) {
        dateStr
    }
}
