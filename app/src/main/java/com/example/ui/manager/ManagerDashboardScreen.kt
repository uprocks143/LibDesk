package com.example.ui.manager

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.local.entities.*
import com.example.domain.model.DashboardKpiState
import com.example.domain.model.KpiCalculationLogic
import com.example.viewmodel.LibDeskViewModel
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.MetricKpiCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.MembershipStatusCard
import com.example.ui.components.calculateMembershipExpiration
import com.example.ui.components.SaaSPlansAndOffersModal
import com.example.ui.components.MembershipAlertLevel
import com.example.ui.components.libDeskHeaderBrush
import com.example.ui.theme.*

@Composable
fun ManagerDashboardScreen(
    library: LibraryEntity?,
    seats: List<SeatEntity>,
    students: List<StudentEntity>,
    todayAttendance: List<AttendanceEntity>,
    books: List<PhysicalBookEntity>,
    bookIssues: List<BookIssueEntity>,
    digitalMaterials: List<DigitalMaterialEntity> = emptyList(),
    payments: List<PaymentEntity>,
    expenses: List<ExpenseEntity>,
    notices: List<NoticeEntity> = emptyList(),
    userName: String = "",
    onNavigateTab: (Int) -> Unit,
    onOpenQrScanner: () -> Unit,
    onOpenRegisterStudent: () -> Unit,
    onOpenAddPayment: () -> Unit,
    onOpenPostNotice: () -> Unit,
    onOpenNoticesAndHelp: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onAddBook: (title: String, author: String, isbn: String, category: String, subject: String, rack: String, shelf: String, copies: Int) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onIssueBook: (PhysicalBookEntity, StudentEntity) -> Unit = { _, _ -> },
    onReturnBook: (BookIssueEntity) -> Unit = {},
    onAddMaterial: (title: String, desc: String, category: String, subject: String, exam: String, fileType: String, fileSize: String, fileUrl: String, accessPolicy: String) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onToggleBookmark: (DigitalMaterialEntity) -> Unit = {},
    onAutoAddFreeMaterials: () -> Unit = {},
    viewModel: LibDeskViewModel? = null,
    initialSection: Int = 0,
    modifier: Modifier = Modifier
) {
    var dashboardSection by remember(initialSection) { mutableStateOf(initialSection) } 

    val isTrialActive by (viewModel?.isTrialActive?.collectAsState() ?: remember { mutableStateOf(false) })
    val trialDaysRemaining by (viewModel?.trialDaysRemaining?.collectAsState() ?: remember { mutableIntStateOf(15) })
    val isTrialExpired by (viewModel?.isTrialExpired?.collectAsState() ?: remember { mutableStateOf(false) })
    val currentSubscription by (viewModel?.currentSubscription?.collectAsState() ?: remember { mutableStateOf(null) })
    val saasPlans by (viewModel?.saasPlans?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val isSupabaseSyncing by (viewModel?.isSupabaseSyncing?.collectAsState() ?: remember { mutableStateOf(false) })
    val supabaseStatusMessage by (viewModel?.supabaseStatusMessage?.collectAsState() ?: remember { mutableStateOf<String?>(null) })
    var showSaaSOffersModal by remember { mutableStateOf(false) }

    
    var showAddBookModal by remember { mutableStateOf(false) }
    var showAddMaterialModal by remember { mutableStateOf(false) }
    var selectedBookToIssue by remember { mutableStateOf<PhysicalBookEntity?>(null) }
    var selectedNoticeForDetail by remember { mutableStateOf<NoticeEntity?>(null) }
    var catalogSearchQuery by remember { mutableStateOf("") }
    var selectedBookCategory by remember { mutableStateOf("ALL") }

    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val kpiState = remember(seats, students, payments, expenses, todayAttendance, bookIssues, books, todayDateStr) {
        KpiCalculationLogic.calculate(
            seats = seats,
            students = students,
            payments = payments,
            expenses = expenses,
            attendance = todayAttendance,
            bookIssues = bookIssues,
            books = books,
            todayDateStr = todayDateStr
        )
    }

    val totalSeats = kpiState.totalSeats
    val occupiedSeats = kpiState.occupiedSeats
    val availableSeats = kpiState.availableSeats
    val occupancyPercent = kpiState.occupancyPercent
    val activeStudentsCount = kpiState.activeStudentsCount
    val expiringCount = kpiState.expiringCount
    val pendingDuesTotal = kpiState.pendingDuesTotal
    val todayCollection = kpiState.todayCollection
    val totalIncome = kpiState.totalIncome
    val totalExpense = kpiState.totalExpense
    val netRevenue = kpiState.netRevenue
    val issuedBooksCount = kpiState.issuedBooksCount
    val liveInHallCount = kpiState.liveInHallCount

    val expiringMemberships = remember(students) {
        students.filter { student ->
            !student.status.equals("ARCHIVED", ignoreCase = true) &&
            !student.status.equals("LEFT", ignoreCase = true) &&
            (student.status.equals("EXPIRED", ignoreCase = true) ||
             calculateMembershipExpiration(student.joiningDate, student.expiryDate, student.status).level != MembershipAlertLevel.ACTIVE)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            ScrollableTabRow(
                selectedTabIndex = dashboardSection,
                edgePadding = 16.dp,
                divider = {}
            ) {
                Tab(
                    selected = dashboardSection == 0,
                    onClick = { dashboardSection = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Overview & KPIs",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (dashboardSection == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                )
                Tab(
                    selected = dashboardSection == 1,
                    onClick = { dashboardSection = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Physical Books (${books.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (dashboardSection == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                )
                Tab(
                    selected = dashboardSection == 2,
                    onClick = { dashboardSection = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Digital Library (${digitalMaterials.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (dashboardSection == 2) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                )
                Tab(
                    selected = dashboardSection == 3,
                    onClick = { dashboardSection = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Attendance Register",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (dashboardSection == 3) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                )
            }
        }

        
        when (dashboardSection) {
            0 -> {

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {

                    
                    item {
                        val heroDisplayName = remember(library?.name, userName) {
                            val lib = library?.name?.trim()
                            when {
                                !lib.isNullOrEmpty() -> lib
                                else -> "Your Library"
                            }
                        }
                        val isDark = LocalIsDarkTheme.current
                        val heroCardBg = if (isDark) Color(0xFF1E293B) else Color(0xFF87CEFA)
                        val heroCardText = if (isDark) Color.White else Color(0xFF0F172A)
                        val heroCardSubtext = if (isDark) Color(0xFF94A3B8) else Color(0xFF334155)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        ) {

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                shadowElevation = 2.dp,
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(heroCardBg)
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFF6AB6EC),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 18.dp, vertical = 16.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = Color.White.copy(alpha = 0.9f),
                                                border = BorderStroke(1.dp, Color(0xFF0F172A).copy(alpha = 0.15f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(7.dp)
                                                            .clip(CircleShape)
                                                            .background(LibDeskColors.success)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = "LIVE DESK SYSTEM",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = Color(0xFF0F172A),
                                                            fontWeight = FontWeight.ExtraBold,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White.copy(alpha = 0.85f),
                                                border = BorderStroke(1.dp, Color(0xFF0F172A).copy(alpha = 0.12f))
                                            ) {
                                                Text(
                                                    text = library?.code ?: "LIB-01",
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        color = Color(0xFF0F172A),
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = heroDisplayName,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = (-0.5).sp
                                            ),
                                            color = heroCardText,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        val mgrName = library?.ownerName?.takeIf { it.isNotBlank() } ?: userName.ifBlank { "Library Manager" }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .clickable { onOpenProfile() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AdminPanelSettings,
                                                contentDescription = null,
                                                tint = Color(0xFF0F172A),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "Manager: $mgrName • ${library?.city ?: "Main Library"}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = heroCardSubtext,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = Color(0xFF0F172A),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Comprehensive Library Details & Real-Time Supabase Capacity Dashboard
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    // Top Header: Library Details & Supabase Cloud Status
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Business,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = library?.name?.ifBlank { "Library Dashboard" } ?: "Library Dashboard",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Code: ${library?.code ?: "LIB-01"} • ${library?.city ?: "Main Branch"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Supabase Cloud Fetch / Sync Button
                                        OutlinedButton(
                                            onClick = { viewModel?.pullFromSupabaseCloud() },
                                            enabled = !isSupabaseSyncing,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            if (isSupabaseSyncing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Syncing...",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.CloudSync,
                                                    contentDescription = "Sync Supabase",
                                                    modifier = Modifier.size(15.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "Fetch Supabase",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Occupancy Status & Capacity Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.EventSeat,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Real-Time Occupancy & Capacity",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        val occupancyStatusColor = when {
                                            occupancyPercent >= 95 -> MaterialTheme.colorScheme.error
                                            occupancyPercent >= 75 -> Color(0xFFF59E0B)
                                            else -> LibDeskColors.success
                                        }
                                        val occupancyStatusText = when {
                                            occupancyPercent >= 95 -> "At Capacity ($occupancyPercent%)"
                                            occupancyPercent >= 75 -> "High Demand ($occupancyPercent%)"
                                            else -> "Available ($occupancyPercent%)"
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = occupancyStatusColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, occupancyStatusColor.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = occupancyStatusText,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = occupancyStatusColor,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Capacity Progress Bar
                                    val progressFraction = if (totalSeats > 0) (occupiedSeats.toFloat() / totalSeats.toFloat()).coerceIn(0f, 1f) else 0f
                                    LinearProgressIndicator(
                                        progress = { progressFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = when {
                                            occupancyPercent >= 90 -> MaterialTheme.colorScheme.error
                                            occupancyPercent >= 75 -> Color(0xFFF59E0B)
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Capacity Metrics Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(horizontalAlignment = Alignment.Start) {
                                            Text(
                                                text = "Total Capacity",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$totalSeats Seats",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Occupied",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$occupiedSeats Assigned",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Vacant",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$availableSeats Free",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = LibDeskColors.success
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Live In-Hall",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$liveInHallCount Present",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF0284C7)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. OCCUPANCY (Vibrant Soft Sky Blue)
                                item {
                                    val occBg = if (isDark) Color(0xFF0C2444) else Color(0xFFE0F2FE)
                                    val occBorder = if (isDark) Color(0xFF0284C7) else Color(0xFF7DD3FC)
                                    val occPrimary = if (isDark) Color(0xFFBAE6FD) else Color(0xFF0369A1)
                                    val occTitleColor = if (isDark) Color.White else Color(0xFF0C4A6E)

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = occBg),
                                        border = BorderStroke(1.dp, occBorder),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier
                                            .width(158.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onNavigateTab(1) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(7.dp))
                                                            .background(Color(0xFF0284C7)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.EventSeat,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Occupancy",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            color = occPrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (occupancyPercent > 85) MaterialTheme.colorScheme.error else Color(0xFF0284C7)
                                                ) {
                                                    Text(
                                                        text = "$occupancyPercent%",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = Color.White,
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                Text(
                                                    text = "$occupiedSeats / $totalSeats",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = occTitleColor
                                                    )
                                                )
                                                Text(
                                                    text = if (availableSeats > 0) "$availableSeats left" else "Full",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (availableSeats > 0) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // 2. IN HALL LIVE (Vibrant Soft Mint Green)
                                item {
                                    val hallBg = if (isDark) Color(0xFF063327) else Color(0xFFD1FAE5)
                                    val hallBorder = if (isDark) Color(0xFF059669) else Color(0xFF6EE7B7)
                                    val hallPrimary = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)
                                    val hallTitleColor = if (isDark) Color.White else Color(0xFF064E3B)

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = hallBg),
                                        border = BorderStroke(1.dp, hallBorder),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier
                                            .width(158.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onOpenQrScanner() }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(7.dp))
                                                            .background(Color(0xFF10B981)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.MeetingRoom,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "In Hall",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            color = hallPrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF10B981)
                                                ) {
                                                    Text(
                                                        text = "LIVE",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = Color.White,
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                Text(
                                                    text = "$liveInHallCount In Hall",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = hallTitleColor
                                                    )
                                                )
                                                Text(
                                                    text = "QR Verified",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF059669),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. TODAY'S FEE (Vibrant Soft Golden Amber)
                                item {
                                    val feeBg = if (isDark) Color(0xFF332105) else Color(0xFFFEF3C7)
                                    val feeBorder = if (isDark) Color(0xFFB45309) else Color(0xFFFCD34D)
                                    val feePrimary = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                    val feeTitleColor = if (isDark) Color.White else Color(0xFF78350F)

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = feeBg),
                                        border = BorderStroke(1.dp, feeBorder),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier
                                            .width(158.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onNavigateTab(3) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(7.dp))
                                                            .background(Color(0xFFF59E0B)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Payments,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Today's Fee",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            color = feePrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFF59E0B)
                                                ) {
                                                    Text(
                                                        text = "FEE",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = Color.White,
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                Text(
                                                    text = "₹${todayCollection.toInt()}",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = feeTitleColor
                                                    )
                                                )
                                                Text(
                                                    text = "Instant",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = feePrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // 4. PENDING DUES (Vibrant Soft Coral Rose)
                                item {
                                    val dueBg = if (isDark) Color(0xFF3B1214) else Color(0xFFFEE2E2)
                                    val dueBorder = if (isDark) Color(0xFFDC2626) else Color(0xFFFCA5A5)
                                    val duePrimary = if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B)
                                    val dueTitleColor = if (pendingDuesTotal > 0) MaterialTheme.colorScheme.error else (if (isDark) Color.White else Color(0xFF7F1D1D))

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = dueBg),
                                        border = BorderStroke(1.dp, dueBorder),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier
                                            .width(158.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onNavigateTab(3) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(7.dp))
                                                            .background(Color(0xFFEF4444)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Warning,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Pending Dues",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            color = duePrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (pendingDuesTotal > 0) MaterialTheme.colorScheme.error else LibDeskColors.success
                                                ) {
                                                    Text(
                                                        text = if (pendingDuesTotal > 0) "DUE" else "OK",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = Color.White,
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                Text(
                                                    text = "₹${pendingDuesTotal.toInt()}",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = dueTitleColor
                                                    )
                                                )
                                                Text(
                                                    text = if (pendingDuesTotal > 0) "Remind" else "All clear",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (pendingDuesTotal > 0) MaterialTheme.colorScheme.error else Color(0xFF059669),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // 5. MEMBERS (Vibrant Soft Lavender Amethyst)
                                item {
                                    val memBg = if (isDark) Color(0xFF281347) else Color(0xFFEDE9FE)
                                    val memBorder = if (isDark) Color(0xFF7C3AED) else Color(0xFFC4B5FD)
                                    val memPrimary = if (isDark) Color(0xFFDDD6FE) else Color(0xFF5B21B6)
                                    val memTitleColor = if (isDark) Color.White else Color(0xFF4C1D95)

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = memBg),
                                        border = BorderStroke(1.dp, memBorder),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier
                                            .width(158.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onNavigateTab(2) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(7.dp))
                                                            .background(Color(0xFF8B5CF6)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.School,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Members",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            color = memPrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF8B5CF6)
                                                ) {
                                                    Text(
                                                        text = "$expiringCount EXP",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = Color.White,
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                Text(
                                                    text = "$activeStudentsCount Active",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = memTitleColor
                                                    )
                                                )
                                                Text(
                                                    text = "$expiringCount exp",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF7C3AED),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // 6. BOOK LOANS (Vibrant Soft Teal)
                                item {
                                    val bkBg = if (isDark) Color(0xFF042F2E) else Color(0xFFCCFBF1)
                                    val bkBorder = if (isDark) Color(0xFF0D9488) else Color(0xFF5EEAD4)
                                    val bkPrimary = if (isDark) Color(0xFF99F6E4) else Color(0xFF115E59)
                                    val bkTitleColor = if (isDark) Color.White else Color(0xFF134E4A)

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = bkBg),
                                        border = BorderStroke(1.dp, bkBorder),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        modifier = Modifier
                                            .width(158.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { dashboardSection = 1 }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(7.dp))
                                                            .background(Color(0xFF0D9488)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.MenuBook,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Book Loans",
                                                        style = MaterialTheme.typography.labelMedium.copy(
                                                            color = bkPrimary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFF0D9488)
                                                ) {
                                                    Text(
                                                        text = "${books.size} BK",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = Color.White,
                                                            fontWeight = FontWeight.ExtraBold
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                Text(
                                                    text = "$issuedBooksCount Issued",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        color = bkTitleColor
                                                    )
                                                )
                                                Text(
                                                    text = "${digitalMaterials.size} notes",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF0F766E),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Quick Actions",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.3).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenRegisterStudent() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("New Admission", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.secondary))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenAddPayment() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Payments, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Record Fee", style = MaterialTheme.typography.labelLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenQrScanner() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.QrCodeScanner, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Scan Pass", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onNavigateTab(1) }
                                 ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.EventSeat, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Seat Matrix", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenPostNotice() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Campaign, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("+Post", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.error))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LibDeskColors.successSoft,
                                    border = BorderStroke(1.dp, LibDeskColors.success.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showAddBookModal = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, null, tint = LibDeskColors.success, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Book", style = MaterialTheme.typography.labelLarge.copy(color = LibDeskColors.success))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LibDeskColors.warningSoft,
                                    border = BorderStroke(1.dp, LibDeskColors.warning.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showAddMaterialModal = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CloudUpload, null, tint = LibDeskColors.warning, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add PDF/Notes", style = MaterialTheme.typography.labelLarge.copy(color = LibDeskColors.warning))
                                    }
                                }
                            }
                        }
                    }

                    
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
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
                                            .background(LibDeskColors.warningSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = LibDeskColors.warning,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Library Announcements",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.3).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    if (notices.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = if (notices.any { it.priority.equals("URGENT", ignoreCase = true) }) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "${notices.size} ${if (notices.size == 1) "Notice" else "Notices"}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (notices.any { it.priority.equals("URGENT", ignoreCase = true) }) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (notices.isNotEmpty()) {
                                        TextButton(
                                            onClick = onOpenNoticesAndHelp,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("View All", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    TextButton(
                                        onClick = onOpenPostNotice,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("+Post", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (notices.isEmpty()) {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(LibDeskColors.warningSoft),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Campaign,
                                                    contentDescription = null,
                                                    tint = LibDeskColors.warning,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "No Active Announcements",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Post exam updates, holiday hours, and fee reminders.",
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = onOpenPostNotice,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("+Post", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                val sortedNotices = remember(notices) {
                                    notices.sortedWith(
                                        compareByDescending<NoticeEntity> { it.priority.equals("URGENT", ignoreCase = true) }
                                            .thenByDescending { it.priority.equals("HIGH", ignoreCase = true) }
                                    )
                                }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(sortedNotices, key = { it.id }) { notice ->
                                        val isUrgent = notice.priority.equals("URGENT", ignoreCase = true)
                                        val isHigh = notice.priority.equals("HIGH", ignoreCase = true)
                                        Card(
                                            shape = RoundedCornerShape(20.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = when {
                                                    isUrgent -> MaterialTheme.colorScheme.errorContainer
                                                    isHigh -> LibDeskColors.warningSoft
                                                    else -> MaterialTheme.colorScheme.surface
                                                }
                                            ),
                                            border = BorderStroke(
                                                1.dp,
                                                when {
                                                    isUrgent -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                                    isHigh -> LibDeskColors.warning.copy(alpha = 0.5f)
                                                    else -> MaterialTheme.colorScheme.outline
                                                }
                                            ),
                                            modifier = Modifier
                                                .widthIn(min = 220.dp, max = 280.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .clickable { selectedNoticeForDetail = notice }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = when {
                                                            isUrgent -> MaterialTheme.colorScheme.error
                                                            isHigh -> LibDeskColors.warning
                                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        }
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isUrgent) Icons.Default.Warning else Icons.Default.Campaign,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = notice.priority.uppercase(),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = notice.date,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = notice.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = notice.content,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 16.sp
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                                    ) {
                                                        Text(
                                                            text = notice.category.replace("_", " "),
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }

                                                    Text(
                                                        text = "Tap to read ↗",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text(
                                text = "Library Overview",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MetricKpiCard(
                                        title = "Seat Occupancy",
                                        value = "$occupancyPercent%",
                                        subtitle = "$occupiedSeats occupied / $availableSeats free",
                                        icon = Icons.Default.EventSeat,
                                        accentColor = MaterialTheme.colorScheme.primary,
                                        onClick = { onNavigateTab(1) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricKpiCard(
                                        title = "Live in Hall",
                                        value = "$liveInHallCount",
                                        subtitle = "Students inside study halls",
                                        icon = Icons.Default.MeetingRoom,
                                        accentColor = LibDeskColors.success,
                                        onClick = onOpenQrScanner,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MetricKpiCard(
                                        title = "Active Students",
                                        value = "$activeStudentsCount",
                                        subtitle = "$expiringCount memberships expired",
                                        icon = Icons.Default.Group,
                                        accentColor = MaterialTheme.colorScheme.tertiary,
                                        onClick = { onNavigateTab(2) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricKpiCard(
                                        title = "Pending Dues",
                                        value = "₹$pendingDuesTotal",
                                        subtitle = "Uncollected student fees",
                                        icon = Icons.Default.Warning,
                                        accentColor = MaterialTheme.colorScheme.error,
                                        onClick = { onNavigateTab(3) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    MetricKpiCard(
                                        title = "Net Revenue",
                                        value = "₹$netRevenue",
                                        subtitle = "Income ₹$totalIncome • Exp ₹$totalExpense",
                                        icon = Icons.Default.AccountBalanceWallet,
                                        accentColor = Color(0xFF059669),
                                        onClick = { onNavigateTab(3) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetricKpiCard(
                                        title = "Books In Loan",
                                        value = "$issuedBooksCount",
                                        subtitle = "${books.size} physical titles in catalog",
                                        icon = Icons.Default.MenuBook,
                                        accentColor = MaterialTheme.colorScheme.secondary,
                                        onClick = { dashboardSection = 1 },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    if (expiringMemberships.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(LibDeskColors.warningSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = LibDeskColors.warning,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = "Expiring Subscriptions (${expiringMemberships.size})",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = (-0.2).sp
                                            )
                                        )
                                    }
                                    TextButton(
                                        onClick = { onNavigateTab(2) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "View All",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(expiringMemberships) { expiringStudent ->
                                        MembershipStatusCard(
                                            student = expiringStudent,
                                            onRenew = { onOpenAddPayment() },
                                            onPayDues = { onOpenAddPayment() },
                                            onViewDetails = { onNavigateTab(2) },
                                            showActions = true,
                                            modifier = Modifier.width(320.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Live Attendance",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.3).sp
                                        )
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)),
                                            modifier = Modifier.clickable { dashboardSection = 3 }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FactCheck,
                                                    contentDescription = "History",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Register",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier.clickable { onOpenQrScanner() }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.QrCodeScanner,
                                                    contentDescription = "Scan",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Scan QR",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val checkedInToday = todayAttendance.filter { it.status == "CHECKED_IN" }
                                if (checkedInToday.isEmpty()) {
                                    Text(
                                        text = "No students currently checked in. Tap 'Scan QR' to log attendance.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    checkedInToday.take(4).forEachIndexed { index, att ->
                                        if (index > 0) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.padding(vertical = 10.dp)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = att.studentName.take(2).uppercase(),
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = att.studentName,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                    Text(
                                                        text = "Seat ${att.seatNumber.ifEmpty { "Unassigned" }} • ${att.shiftName}",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = LibDeskColors.successSoft
                                            ) {
                                                Text(
                                                    text = "In: ${att.checkInTime}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = LibDeskColors.success
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { dashboardSection = 3 },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "View All Attendance History & Logs",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {

                val filteredBooks = remember(books, catalogSearchQuery, selectedBookCategory) {
                    books.filter { b ->
                        (selectedBookCategory == "ALL" || b.category.equals(selectedBookCategory, ignoreCase = true)) &&
                        (catalogSearchQuery.isBlank() ||
                                b.title.contains(catalogSearchQuery, ignoreCase = true) ||
                                b.author.contains(catalogSearchQuery, ignoreCase = true) ||
                                b.subject.contains(catalogSearchQuery, ignoreCase = true) ||
                                b.rack.contains(catalogSearchQuery, ignoreCase = true))
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = catalogSearchQuery,
                                    onValueChange = { catalogSearchQuery = it },
                                    placeholder = { Text("Search by title, author, rack...") },
                                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = {
                                        if (catalogSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { catalogSearchQuery = "" }) {
                                                Icon(Icons.Default.Clear, null)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = { showAddBookModal = true },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Book", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }

                        
                        item {
                            val categories = listOf("ALL", "General", "Competitive", "Engineering", "Medical", "Law", "Literature")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categories) { cat ->
                                    FilterChip(
                                        selected = selectedBookCategory == cat,
                                        onClick = { selectedBookCategory = cat },
                                        label = { Text(cat, fontSize = 14.sp) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${filteredBooks.size} Titles Listed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${books.sumOf { it.availableCopies }} Copies Available • $issuedBooksCount On Loan",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (filteredBooks.isEmpty()) {
                            item {
                                EmptyPlaceholder(
                                    title = "No Books Found",
                                    description = "",
                                    icon = Icons.Default.MenuBook
                                )
                            }
                        } else {
                            items(filteredBooks, key = { it.id }) { book ->
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Row(modifier = Modifier.weight(1f)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoStories,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = book.title,
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "By ${book.author} • ${book.subject}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "Rack: ${book.rack} • Shelf: ${book.shelf} • ISBN: ${book.isbn}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            Surface(
                                                shape = CircleShape,
                                                color = if (book.availableCopies > 0) LibDeskColors.successSoft else MaterialTheme.colorScheme.errorContainer
                                            ) {
                                                Text(
                                                    text = "${book.availableCopies}/${book.totalCopies} Available",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (book.availableCopies > 0) LibDeskColors.success else MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = { selectedBookToIssue = book },
                                                enabled = book.availableCopies > 0,
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, if (book.availableCopies > 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.AssignmentReturn, null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Issue to Student", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {

                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudUpload,
                                                contentDescription = "Upload Study Material Icon",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Upload Study Material",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                            )
                                            Text(
                                                text = "PDF notes, test series, syllabus & question banks",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { showAddMaterialModal = true },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Upload PDF", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { onAutoAddFreeMaterials() },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Auto-Add Free PDFs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }

                        if (digitalMaterials.isEmpty()) {
                            item {
                                EmptyPlaceholder(
                                    title = "No Digital Resources",
                                    description = "",
                                    icon = Icons.Default.CloudDownload
                                )
                            }
                        } else {
                            items(digitalMaterials, key = { it.id }) { doc ->
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(
                                                        if (doc.fileType.equals("PDF", ignoreCase = true)) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (doc.fileType.equals("PDF", ignoreCase = true)) Icons.Default.PictureAsPdf else Icons.Default.Description,
                                                    contentDescription = null,
                                                    tint = if (doc.fileType.equals("PDF", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column {
                                                Text(
                                                    text = doc.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${doc.category} • ${doc.subject} • ${doc.fileSize}",
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = doc.description,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        IconButton(onClick = { onToggleBookmark(doc) }) {
                                            Icon(
                                                imageVector = if (doc.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                                contentDescription = "Bookmark",
                                                tint = if (doc.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                if (viewModel != null) {
                    ManagerAttendanceHistoryScreen(
                        viewModel = viewModel,
                        onOpenQrScanner = onOpenQrScanner
                    )
                } else {
                    EmptyPlaceholder(
                        title = "Attendance Register",
                        description = "Attendance logs will appear here",
                        icon = Icons.Default.FactCheck
                    )
                }
            }
        }
    }

    
    if (showAddBookModal) {
        var newTitle by remember { mutableStateOf("") }
        var newAuthor by remember { mutableStateOf("") }
        var newIsbn by remember { mutableStateOf("978-") }
        var newCategory by remember { mutableStateOf("Competitive") }
        var newSubject by remember { mutableStateOf("General Studies") }
        var newRack by remember { mutableStateOf("Rack A-1") }
        var newShelf by remember { mutableStateOf("Shelf 2") }
        var newCopies by remember { mutableStateOf("3") }

        BackHandler { showAddBookModal = false }

        Dialog(
            onDismissRequest = { showAddBookModal = false },
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Add Book to Catalog", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Book Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newAuthor,
                        onValueChange = { newAuthor = it },
                        label = { Text("Author / Publication") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newRack,
                            onValueChange = { newRack = it },
                            label = { Text("Rack") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newShelf,
                            onValueChange = { newShelf = it },
                            label = { Text("Shelf") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCopies,
                            onValueChange = { newCopies = it },
                            label = { Text("Total Copies") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newCategory,
                            onValueChange = { newCategory = it },
                            label = { Text("Category") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddBookModal = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank()) {
                                    val count = newCopies.toIntOrNull() ?: 1
                                    onAddBook(newTitle, newAuthor, newIsbn, newCategory, newSubject, newRack, newShelf, count)
                                    showAddBookModal = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Book")
                        }
                    }
                }
            }
        }
    }

    
    if (selectedBookToIssue != null) {
        var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }

        BackHandler { selectedBookToIssue = null }

        Dialog(
            onDismissRequest = { selectedBookToIssue = null },
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Issue Book Loan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = "Book: ${selectedBookToIssue!!.title} (${selectedBookToIssue!!.availableCopies} copies left)",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("Select Enrolled Student:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(students) { st ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedStudentId == st.id) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                border = BorderStroke(1.dp, if (selectedStudentId == st.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStudentId = st.id }
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedStudentId == st.id,
                                        onClick = { selectedStudentId = st.id }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(st.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${st.mobile} • Seat ${st.seatNumber}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedBookToIssue = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val targetStudent = students.find { it.id == selectedStudentId }
                                if (targetStudent != null) {
                                    onIssueBook(selectedBookToIssue!!, targetStudent)
                                    selectedBookToIssue = null
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Confirm Issue")
                        }
                    }
                }
            }
        }
    }

    
    if (showAddMaterialModal) {
        var matTitle by remember { mutableStateOf("") }
        var matDesc by remember { mutableStateOf("") }
        var matCategory by remember { mutableStateOf("Competitive Notes") }
        var matSubject by remember { mutableStateOf("History & Polity") }
        var matExam by remember { mutableStateOf("UPSC / State PCS") }
        var matType by remember { mutableStateOf("PDF") }
        var matSize by remember { mutableStateOf("4.2 MB") }

        BackHandler { showAddMaterialModal = false }

        Dialog(
            onDismissRequest = { showAddMaterialModal = false },
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Upload Study Material", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    OutlinedTextField(
                        value = matTitle,
                        onValueChange = { matTitle = it },
                        label = { Text("Resource Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = matDesc,
                        onValueChange = { matDesc = it },
                        label = { Text("Short Description") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = matCategory,
                            onValueChange = { matCategory = it },
                            label = { Text("Category") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = matExam,
                            onValueChange = { matExam = it },
                            label = { Text("Target Exam") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddMaterialModal = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (matTitle.isNotBlank()) {
                                    onAddMaterial(
                                        matTitle,
                                        matDesc.ifBlank { "Comprehensive study notes" },
                                        matCategory,
                                        matSubject,
                                        matExam,
                                        matType,
                                        matSize,
                                        "https://example.com/file.pdf",
                                        "ACTIVE_MEMBERS"
                                    )
                                    showAddMaterialModal = false
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Publish Resource")
                        }
                    }
                }
            }
        }
    }

    selectedNoticeForDetail?.let { notice ->
        NoticeDetailDialog(
            notice = notice,
            onClose = { selectedNoticeForDetail = null }
        )
    }

    if (showSaaSOffersModal) {
        if (viewModel != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showSaaSOffersModal = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                com.example.ui.subscription.PlansAndOffersScreen(
                    viewModel = viewModel,
                    onNavigateBack = { showSaaSOffersModal = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            SaaSPlansAndOffersModal(
                show = true,
                currentSubscription = currentSubscription,
                plans = saasPlans,
                trialDaysRemaining = trialDaysRemaining,
                onSelectPlan = { plan ->
                    showSaaSOffersModal = false
                },
                onDismiss = { showSaaSOffersModal = false }
            )
        }
    }
}

@Composable
fun NoticeDetailDialog(
    notice: NoticeEntity,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val isUrgent = notice.priority.equals("URGENT", ignoreCase = true)
    val isHigh = notice.priority.equals("HIGH", ignoreCase = true)

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
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isUrgent -> MaterialTheme.colorScheme.errorContainer
                            isHigh -> LibDeskColors.warningSoft
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isUrgent) Icons.Default.Warning else Icons.Default.Campaign,
                                contentDescription = null,
                                tint = when {
                                    isUrgent -> MaterialTheme.colorScheme.error
                                    isHigh -> LibDeskColors.warning
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${notice.priority.uppercase()} NOTICE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    isUrgent -> MaterialTheme.colorScheme.error
                                    isHigh -> LibDeskColors.warning
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = notice.category.replace("_", " "),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "• ${notice.date}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Target: ${notice.targetAudience}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = notice.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Notice", "${notice.title}\n\n${notice.content}")
                            clipboard?.setPrimaryClip(clip)
                            // android.widget.Toast.makeText(context, "Announcement copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Notice", style = MaterialTheme.typography.labelMedium)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onClose,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text("Dismiss", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
private fun SleekQuickAction(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}
