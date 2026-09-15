package com.example.ui.superadmin

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.LibrarySubscriptionEntity
import com.example.data.local.entities.SaaSSubscriptionPlanEntity
import com.example.data.local.entities.SuperAdminUserEntity
import com.example.ui.components.LogoutConfirmationDialog
import com.example.ui.theme.*
import com.example.viewmodel.LibDeskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(
    viewModel: LibDeskViewModel,
    onNavigateToLibrary: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTabFromDrawer: Int = 0,
    onTabChange: (Int) -> Unit = {},
    onOpenMenu: () -> Unit = {}
) {
    val context = LocalContext.current
    val libraries by viewModel.allLibraries.collectAsState()
    val saasPlans by viewModel.saasPlans.collectAsState()
    val allSubscriptions by viewModel.allSubscriptions.collectAsState()
    val superAdminProfile by viewModel.superAdminProfile.collectAsState()

    var selectedTab by remember { mutableIntStateOf(selectedTabFromDrawer) }
    LaunchedEffect(selectedTabFromDrawer) {
        selectedTab = selectedTabFromDrawer
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } 

    
    var showEditSubscriptionDialog by remember { mutableStateOf(false) }
    var selectedLibForEdit by remember { mutableStateOf<LibraryEntity?>(null) }
    var selectedSubForEdit by remember { mutableStateOf<LibrarySubscriptionEntity?>(null) }

    var showCreatePlanDialog by remember { mutableStateOf(false) }
    var planToEdit by remember { mutableStateOf<SaaSSubscriptionPlanEntity?>(null) }

    var showSecuritySettingsDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmationDialog by remember { mutableStateOf(false) }

    LogoutConfirmationDialog(
        show = showLogoutConfirmationDialog,
        userName = "Super Admin",
        onConfirmLogout = {
            showLogoutConfirmationDialog = false
            viewModel.logout()
            onLogout()
        },
        onDismiss = { showLogoutConfirmationDialog = false }
    )

    
    val subscriptionMap = remember(allSubscriptions) {
        allSubscriptions.associateBy { it.libraryId }
    }

    
    val totalLibrariesCount = libraries.size
    val activeSubscriptionsCount = libraries.count { lib ->
        val sub = subscriptionMap[lib.id]
        sub?.status == "ACTIVE" || sub == null 
    }
    val suspendedCount = allSubscriptions.count { it.status == "SUSPENDED" }
    val estimatedMrr = allSubscriptions.filter { it.status == "ACTIVE" }.sumOf {
        val months = if (it.planName.contains("12 Mo") || it.planName.contains("Annual")) 12
        else if (it.planName.contains("6 Mo")) 6
        else if (it.planName.contains("3 Mo")) 3
        else 1
        it.price / months
    }.coerceAtLeast(4890.0)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primaryContainer)))
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.85f))
                                    .clickable { onOpenMenu() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Open Side Menu",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Super Admin (SaaS Manager)",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            letterSpacing = (-0.2).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        softWrap = true,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "Subscriptions & SaaS Platform Hub",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    softWrap = true,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.85f))
                                    .clickable { showSecuritySettingsDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = "2FA Settings",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.85f))
                                        .clickable {
                                            showLogoutConfirmationDialog = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Logout",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        edgePadding = 8.dp,
                        divider = {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        },
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    height = 3.dp
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                onTabChange(0)
                            },
                            text = { Text("Subscriptions (${libraries.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp) },
                            icon = { Icon(Icons.Default.Subscriptions, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                onTabChange(1)
                            },
                            text = { Text("SaaS Plans (${saasPlans.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp) },
                            icon = { Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = {
                                selectedTab = 2
                                onTabChange(2)
                            },
                            text = { Text("Revenue & MRR", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp) },
                            icon = { Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = {
                                selectedTab = 3
                                onTabChange(3)
                            },
                            text = { Text("2FA & Security", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp) },
                            icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> {

                    SubscriptionsManagementTab(
                        libraries = libraries,
                        subscriptionMap = subscriptionMap,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedStatusFilter = selectedStatusFilter,
                        onFilterChange = { selectedStatusFilter = it },
                        onEditSubscription = { lib, sub ->
                            selectedLibForEdit = lib
                            selectedSubForEdit = sub
                            showEditSubscriptionDialog = true
                        },
                        onToggleSuspension = { lib, isSuspended ->
                            viewModel.toggleLibrarySuspension(lib.id, lib.name, isSuspended)
                        },
                        onOpenLibrary = { lib ->
                            viewModel.selectLibrary(lib.id)
                            viewModel.login(lib.ownerEmail, "MANAGER", lib.ownerName)
                            onNavigateToLibrary(lib.id)
                        },
                        onSendReminder = { lib, sub ->
                            val defaultFutureDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 60L * 86400000L))
                            val daysLeft = calculateDaysRemaining(sub?.expiryDate ?: defaultFutureDate)
                            val message = "Dear ${lib.ownerName}, your SaaS subscription for ${lib.name} on LibDesk is due for renewal (Expires in $daysLeft days). Please renew at UPI ID: admin@libdesk.io to continue uninterrupted service."
                            com.example.util.ImageShareUtils.sendTextToWhatsApp(context, lib.ownerPhone, message)
                        }
                    )
                }

                1 -> {

                    SaasPlansConfigTab(
                        plans = saasPlans,
                        onAddNewPlan = {
                            planToEdit = null
                            showCreatePlanDialog = true
                        },
                        onEditPlan = { plan ->
                            planToEdit = plan
                            showCreatePlanDialog = true
                        },
                        onDeletePlan = { plan ->
                            viewModel.deleteSaasPlan(plan)
                        }
                    )
                }

                2 -> {

                    SaasRevenueAnalyticsTab(
                        totalLibraries = totalLibrariesCount,
                        activeSubs = activeSubscriptionsCount,
                        suspendedSubs = suspendedCount,
                        estimatedMrr = estimatedMrr,
                        subscriptions = allSubscriptions,
                        plans = saasPlans
                    )
                }

                3 -> {

                    SuperAdminSecurityTab(
                        superAdmin = superAdminProfile ?: SuperAdminUserEntity(),
                        onUpdateSecurity = { email, name, code, is2Fa ->
                            viewModel.updateSuperAdminProfile(email, name, code, is2Fa)
                        }
                    )
                }
            }
        }
    }

    
    if (showEditSubscriptionDialog && selectedLibForEdit != null) {
        val lib = selectedLibForEdit!!
        val currentSub = selectedSubForEdit

        EditSubscriptionModal(
            library = lib,
            currentSubscription = currentSub,
            availablePlans = saasPlans,
            onDismiss = {
                showEditSubscriptionDialog = false
                selectedLibForEdit = null
                selectedSubForEdit = null
            },
            onSave = { plan, additionalMonths, customPrice, customDiscount, status, maxSeats, notes ->
                viewModel.extendOrUpdateLibrarySubscription(
                    libraryId = lib.id,
                    libraryName = lib.name,
                    plan = plan,
                    additionalMonths = additionalMonths,
                    customPrice = customPrice,
                    customDiscount = customDiscount,
                    status = status,
                    maxSeats = maxSeats,
                    notes = notes
                )
                showEditSubscriptionDialog = false
                selectedLibForEdit = null
                selectedSubForEdit = null
            }
        )
    }

    
    if (showCreatePlanDialog) {
        CreateOrEditPlanModal(
            planToEdit = planToEdit,
            onDismiss = {
                showCreatePlanDialog = false
                planToEdit = null
            },
            onSave = { plan ->
                viewModel.saveSaasPlan(plan)
                showCreatePlanDialog = false
                planToEdit = null
            }
        )
    }

    
    if (showSecuritySettingsDialog) {
        val currentAdmin = superAdminProfile ?: SuperAdminUserEntity()
        var adminEmail by remember { mutableStateOf(currentAdmin.email) }
        var adminName by remember { mutableStateOf(currentAdmin.name) }
        var accessCode by remember { mutableStateOf(currentAdmin.accessCode) }
        var showAccessCode by remember { mutableStateOf(false) }
        var enable2Fa by remember { mutableStateOf(currentAdmin.is2FaEnabled) }

        BackHandler { showSecuritySettingsDialog = false }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSecuritySettingsDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Super Admin & 2FA Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { showSecuritySettingsDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Manage Super Admin credentials and Email OTP Two-Factor Authentication policy.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                    OutlinedTextField(
                        value = adminEmail,
                        onValueChange = { adminEmail = it },
                        label = { Text("Super Admin Email (for 2FA OTP)") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = adminName,
                        onValueChange = { adminName = it },
                        label = { Text("Super Administrator Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accessCode,
                        onValueChange = { accessCode = it },
                        label = { Text("Access PIN / Password") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showAccessCode = !showAccessCode }) {
                                Icon(
                                    imageVector = if (showAccessCode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showAccessCode) "Hide Password" else "Show Password"
                                )
                            }
                        },
                        visualTransformation = if (showAccessCode) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Require 2FA Email OTP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Enforce 6-digit OTP sent to email on every Super Admin login", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = enable2Fa,
                                onCheckedChange = { enable2Fa = it }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showSecuritySettingsDialog = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateSuperAdminProfile(adminEmail, adminName, accessCode, enable2Fa)
                            showSecuritySettingsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
} 




@Composable
fun SubscriptionsManagementTab(
    libraries: List<LibraryEntity>,
    subscriptionMap: Map<String, LibrarySubscriptionEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedStatusFilter: String,
    onFilterChange: (String) -> Unit,
    onEditSubscription: (LibraryEntity, LibrarySubscriptionEntity?) -> Unit,
    onToggleSuspension: (LibraryEntity, Boolean) -> Unit,
    onOpenLibrary: (LibraryEntity) -> Unit,
    onSendReminder: (LibraryEntity, LibrarySubscriptionEntity?) -> Unit
) {
    val defaultExpDate = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 60L * 86400000L))
    }
    val filteredLibraries = remember(libraries, subscriptionMap, searchQuery, selectedStatusFilter, defaultExpDate) {
        libraries.filter { lib ->
            val sub = subscriptionMap[lib.id]
            val status = sub?.status ?: "ACTIVE"

            val matchesSearch = lib.name.contains(searchQuery, ignoreCase = true) ||
                    lib.code.contains(searchQuery, ignoreCase = true) ||
                    lib.city.contains(searchQuery, ignoreCase = true) ||
                    lib.ownerName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedStatusFilter) {
                "ALL" -> true
                "ACTIVE" -> status == "ACTIVE"
                "EXPIRING" -> status == "EXPIRING_SOON" || calculateDaysRemaining(sub?.expiryDate ?: defaultExpDate) <= 14
                "TRIAL" -> status == "TRIAL" || (sub?.planId == "PLAN-TRIAL")
                "SUSPENDED" -> status == "SUSPENDED"
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search by Library Name, Code, City, Owner...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All Libraries (${libraries.size})",
                        "ACTIVE" to "Active Subscriptions",
                        "EXPIRING" to "Expiring Soon",
                        "TRIAL" to "Free Trials",
                        "SUSPENDED" to "Suspended"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedStatusFilter == key,
                            onClick = { onFilterChange(key) },
                            label = { Text(label, fontSize = 14.sp, fontWeight = if (selectedStatusFilter == key) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        if (filteredLibraries.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No matching libraries found", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Try adjusting your search query or status filter.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filteredLibraries, key = { it.id }) { lib ->
                val sub = subscriptionMap[lib.id]
                LibrarySubscriptionCard(
                    library = lib,
                    subscription = sub,
                    onEditSubscription = { onEditSubscription(lib, sub) },
                    onToggleSuspension = { isSuspended -> onToggleSuspension(lib, isSuspended) },
                    onOpenLibrary = { onOpenLibrary(lib) },
                    onSendReminder = { onSendReminder(lib, sub) }
                )
            }
        }
    }
}

@Composable
fun LibrarySubscriptionCard(
    library: LibraryEntity,
    subscription: LibrarySubscriptionEntity?,
    onEditSubscription: () -> Unit,
    onToggleSuspension: (Boolean) -> Unit,
    onOpenLibrary: () -> Unit,
    onSendReminder: () -> Unit
) {
    val status = subscription?.status ?: "ACTIVE"
    val isSuspended = status == "SUSPENDED"
    val planName = subscription?.planName ?: "Growth Pro (3 Months)"
    val defaultSubExpDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() + 60L * 86400000L)) }
    val expiryDate = subscription?.expiryDate ?: defaultSubExpDate
    val daysRemaining = calculateDaysRemaining(expiryDate)
    val pricePaid = subscription?.price ?: 1899.0
    val maxSeats = subscription?.maxSeats ?: 150

    val (statusBg, statusFg, statusLabel) = when {
        isSuspended -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "SUSPENDED")
        daysRemaining <= 0 -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "EXPIRED")
        daysRemaining <= 14 -> Triple(LibDeskColors.warningSoft, LibDeskColors.warning, "EXPIRING IN $daysRemaining D")
        subscription?.planId == "PLAN-TRIAL" -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, "TRIAL")
        else -> Triple(LibDeskColors.successSoft, LibDeskColors.success, "ACTIVE")
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isSuspended) 1.5.dp else 1.dp,
            color = if (isSuspended) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSuspended) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSuspended) Icons.Default.Block else Icons.Default.LocalLibrary,
                            contentDescription = null,
                            tint = if (isSuspended) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = library.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = library.code,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(" • ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${library.city.ifBlank { "New Delhi" }}, ${library.state.ifBlank { "Delhi" }}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        color = statusFg,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CURRENT PLAN", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = planName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        softWrap = true,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("EXPIRY DATE", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = expiryDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (daysRemaining <= 14) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        softWrap = true,
                        maxLines = 1
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("PRICE / SEATS", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "₹${pricePaid.toInt()} • $maxSeats Seats",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = LibDeskColors.success,
                        softWrap = true,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${library.ownerName.ifBlank { "Owner" }} (${library.ownerPhone.ifBlank { "+91 98765 43210" }})",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        softWrap = true,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onEditSubscription,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Manage", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onToggleSuspension(!isSuspended) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isSuspended) LibDeskColors.success else MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, if (isSuspended) LibDeskColors.success else MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isSuspended) Icons.Default.CheckCircle else Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (isSuspended) "Activate" else "Suspend", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onSendReminder,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LibDeskColors.successSoft)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "WhatsApp Reminder", tint = LibDeskColors.success, modifier = Modifier.size(17.dp))
                }

                IconButton(
                    onClick = onOpenLibrary,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Library", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}




@Composable
fun SaasPlansConfigTab(
    plans: List<SaaSSubscriptionPlanEntity>,
    onAddNewPlan: () -> Unit,
    onEditPlan: (SaaSSubscriptionPlanEntity) -> Unit,
    onDeletePlan: (SaaSSubscriptionPlanEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SaaS Subscription Tiers", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Configure plan pricing, durations, seat capacities, and feature sets.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = onAddNewPlan,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        items(plans, key = { it.id }) { plan ->
            SaasPlanCard(
                plan = plan,
                onEdit = { onEditPlan(plan) },
                onDelete = { onDeletePlan(plan) }
            )
        }
    }
}

@Composable
fun SaasPlanCard(
    plan: SaaSSubscriptionPlanEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPopular = plan.badge.isNotBlank()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isPopular) 1.5.dp else 1.dp,
            color = if (isPopular) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (plan.badge.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = plan.badge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = plan.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${plan.price.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "for ${plan.durationMonths} ${if (plan.durationMonths == 1) "Month" else "Months"}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EventSeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (plan.maxSeats >= 9000) "Unlimited Seats" else "Up to ${plan.maxSeats} Seats",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Plan ID: ${plan.id}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = plan.features,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Plan", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}




@Composable
fun SaasRevenueAnalyticsTab(
    totalLibraries: Int,
    activeSubs: Int,
    suspendedSubs: Int,
    estimatedMrr: Double,
    subscriptions: List<LibrarySubscriptionEntity>,
    plans: List<SaaSSubscriptionPlanEntity>
) {
    val arr = estimatedMrr * 12.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SaaS Platform Performance", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Monthly Recurring (MRR)", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹${estimatedMrr.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("+18.4% this month", fontSize = 14.sp, color = LibDeskColors.success)
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Annual Run Rate (ARR)", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("₹${(arr / 100000.0).let { String.format("%.2f L", it) }}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Projected Annual", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricMiniBox("Total Libraries", totalLibraries.toString(), Icons.Default.Domain, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MetricMiniBox("Active Subs", activeSubs.toString(), Icons.Default.CheckCircle, LibDeskColors.success, Modifier.weight(1f))
                MetricMiniBox("Suspended", suspendedSubs.toString(), Icons.Default.Block, MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
        }

        
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Subscription Distribution by Tier", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    plans.forEach { plan ->
                        val count = subscriptions.count { it.planId == plan.id }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(plan.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("$count Libraries (₹${plan.price.toInt()})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            LinearProgressIndicator(
                                progress = { if (subscriptions.isNotEmpty()) count.toFloat() / subscriptions.size else 0.25f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricMiniBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}




@Composable
fun SuperAdminSecurityTab(
    superAdmin: SuperAdminUserEntity,
    onUpdateSecurity: (email: String, name: String, accessCode: String, is2Fa: Boolean) -> Unit
) {
    var email by remember { mutableStateOf(superAdmin.email) }
    var name by remember { mutableStateOf(superAdmin.name) }
    var accessCode by remember { mutableStateOf(superAdmin.accessCode) }
    var showAccessCode by remember { mutableStateOf(false) }
    var is2FaEnabled by remember { mutableStateOf(superAdmin.is2FaEnabled) }
    var isSaved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Two-Factor Authentication (2FA)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Email OTP verification protecting Super Admin access", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Super Admin Primary Email") },
                        supportingText = { Text("6-Digit 2FA OTP codes are dispatched to this email address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Super Admin Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accessCode,
                        onValueChange = { accessCode = it },
                        label = { Text("Access PIN / Password") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            IconButton(onClick = { showAccessCode = !showAccessCode }) {
                                Icon(
                                    imageVector = if (showAccessCode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (showAccessCode) "Hide Password" else "Show Password"
                                )
                            }
                        },
                        visualTransformation = if (showAccessCode) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (is2FaEnabled) LibDeskColors.successSoft else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (is2FaEnabled) LibDeskColors.success else MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (is2FaEnabled) Icons.Default.Shield else Icons.Default.GppMaybe,
                                        contentDescription = null,
                                        tint = if (is2FaEnabled) LibDeskColors.success else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Enforce 2FA Email OTP", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Text("Requires email OTP verification on login", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = is2FaEnabled,
                                onCheckedChange = { is2FaEnabled = it }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onUpdateSecurity(email, name, accessCode, is2FaEnabled)
                            isSaved = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Security Configuration", fontWeight = FontWeight.Bold)
                    }

                    if (isSaved) {
                        Text(
                            text = "Security credentials updated successfully!",
                            color = LibDeskColors.success,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubscriptionModal(
    library: LibraryEntity,
    currentSubscription: LibrarySubscriptionEntity?,
    availablePlans: List<SaaSSubscriptionPlanEntity>,
    onDismiss: () -> Unit,
    onSave: (
        plan: SaaSSubscriptionPlanEntity,
        additionalMonths: Int,
        customPrice: Double,
        customDiscount: Double,
        status: String,
        maxSeats: Int,
        notes: String
    ) -> Unit
) {
    var selectedPlan by remember {
        mutableStateOf(
            availablePlans.find { it.id == currentSubscription?.planId } ?: availablePlans.firstOrNull() ?: SaaSSubscriptionPlanEntity(
                id = "PLAN-PRO",
                name = "Growth Pro",
                durationMonths = 3,
                price = 1899.0,
                maxSeats = 150,
                features = "All Features"
            )
        )
    }

    var additionalMonths by remember { mutableStateOf(selectedPlan.durationMonths) }
    var customPriceStr by remember { mutableStateOf(currentSubscription?.price?.toInt()?.toString() ?: selectedPlan.price.toInt().toString()) }
    var customDiscountStr by remember { mutableStateOf(currentSubscription?.discount?.toInt()?.toString() ?: "0") }
    var status by remember { mutableStateOf(currentSubscription?.status ?: "ACTIVE") }
    var maxSeatsStr by remember { mutableStateOf((currentSubscription?.maxSeats ?: selectedPlan.maxSeats).toString()) }
    var notes by remember { mutableStateOf(currentSubscription?.notes ?: "") }

    BackHandler { onDismiss() }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Manage Subscription", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(library.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                Text("Select SaaS Tier Plan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availablePlans.forEach { plan ->
                        val isSelected = selectedPlan.id == plan.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPlan = plan
                                    additionalMonths = plan.durationMonths
                                    customPriceStr = plan.price.toInt().toString()
                                    maxSeatsStr = plan.maxSeats.toString()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    Text("${plan.durationMonths} Months • ${plan.maxSeats} Seats", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("₹${plan.price.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }

                
                Text("Validity Duration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1 to "1 Mo", 3 to "3 Mo", 6 to "6 Mo", 12 to "1 Year").forEach { (months, label) ->
                        FilterChip(
                            selected = additionalMonths == months,
                            onClick = { additionalMonths = months },
                            label = { Text(label, fontSize = 14.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customPriceStr,
                        onValueChange = { customPriceStr = it },
                        label = { Text("Price (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = customDiscountStr,
                        onValueChange = { customDiscountStr = it },
                        label = { Text("Discount (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                
                OutlinedTextField(
                    value = maxSeatsStr,
                    onValueChange = { maxSeatsStr = it },
                    label = { Text("Seat Capacity Limit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                
                Text("Subscription Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ACTIVE", "TRIAL", "EXPIRING_SOON", "EXPIRED", "SUSPENDED").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st, fontSize = 14.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (st == "SUSPENDED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Super Admin Notes / Payment Reference") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                } 

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = customPriceStr.toDoubleOrNull() ?: selectedPlan.price
                            val discount = customDiscountStr.toDoubleOrNull() ?: 0.0
                            val seats = maxSeatsStr.toIntOrNull() ?: selectedPlan.maxSeats
                            onSave(selectedPlan, additionalMonths, price, discount, status, seats, notes)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save & Apply")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateOrEditPlanModal(
    planToEdit: SaaSSubscriptionPlanEntity?,
    onDismiss: () -> Unit,
    onSave: (SaaSSubscriptionPlanEntity) -> Unit
) {
    var name by remember { mutableStateOf(planToEdit?.name ?: "") }
    var durationMonthsStr by remember { mutableStateOf((planToEdit?.durationMonths ?: 1).toString()) }
    var priceStr by remember { mutableStateOf(planToEdit?.price?.toInt()?.toString() ?: "999") }
    var maxSeatsStr by remember { mutableStateOf(planToEdit?.maxSeats?.toString() ?: "100") }
    var features by remember { mutableStateOf(planToEdit?.features ?: "QR Attendance, Auto WhatsApp Reminders, Cloud Backup") }
    var badge by remember { mutableStateOf(planToEdit?.badge ?: "") }

    BackHandler { onDismiss() }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (planToEdit != null) "Edit SaaS Plan" else "Create New SaaS Plan", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                    label = { Text("Plan Name (e.g. Starter, Pro, Enterprise)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = durationMonthsStr,
                        onValueChange = { durationMonthsStr = it },
                        label = { Text("Duration (Months)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (₹)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = maxSeatsStr,
                    onValueChange = { maxSeatsStr = it },
                    label = { Text("Max Seat Capacity (e.g. 50, 150, 9999)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = features,
                    onValueChange = { features = it },
                    label = { Text("Features (Comma-separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = badge,
                    onValueChange = { badge = it },
                    label = { Text("Badge Label (Optional, e.g. Most Popular)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                } 

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val id = planToEdit?.id ?: "PLAN-${UUID.randomUUID().toString().take(6).uppercase()}"
                            val dur = durationMonthsStr.toIntOrNull() ?: 1
                            val pr = priceStr.toDoubleOrNull() ?: 999.0
                            val seats = maxSeatsStr.toIntOrNull() ?: 100
                            onSave(
                                SaaSSubscriptionPlanEntity(
                                    id = id,
                                    name = name.ifBlank { "Custom Plan" },
                                    durationMonths = dur,
                                    price = pr,
                                    maxSeats = seats,
                                    features = features,
                                    badge = badge,
                                    isActive = true
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Plan")
                    }
                }
            }
        }
    }
}

fun calculateDaysRemaining(expiryDateStr: String): Int {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val expiryDate = sdf.parse(expiryDateStr) ?: return 30
        val today = Date()
        val diffMs = expiryDate.time - today.time
        (diffMs / (1000 * 60 * 60 * 24)).toInt()
    } catch (e: Exception) {
        30
    }
}
