package com.example.ui.superadmin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.LibrarySubscriptionEntity
import com.example.data.local.entities.NoticeEntity
import com.example.data.local.entities.SubscriptionPlans
import com.example.data.local.entities.SuperAdminUserEntity
import com.example.ui.theme.LibDeskColors
import com.example.viewmodel.LibDeskViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class SubStatus { ACTIVE, EXPIRED, SUSPENDED, PENDING, NO_PLAN }

private fun effectiveStatus(sub: LibrarySubscriptionEntity?): SubStatus {
    if (sub == null) return SubStatus.NO_PLAN
    return when (sub.status) {
        "SUSPENDED" -> SubStatus.SUSPENDED
        "PENDING_VERIFICATION" -> SubStatus.PENDING
        "REJECTED" -> SubStatus.NO_PLAN
        else -> {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (sub.expiryDate.isNotBlank() && sub.expiryDate < today) SubStatus.EXPIRED else SubStatus.ACTIVE
        }
    }
}

@Composable
private fun statusColor(status: SubStatus): Color = when (status) {
    SubStatus.ACTIVE -> LibDeskColors.success
    SubStatus.PENDING -> LibDeskColors.warning
    SubStatus.EXPIRED -> MaterialTheme.colorScheme.error
    SubStatus.SUSPENDED -> MaterialTheme.colorScheme.error
    SubStatus.NO_PLAN -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun statusLabel(status: SubStatus): String = when (status) {
    SubStatus.ACTIVE -> "Active"
    SubStatus.PENDING -> "Pending Review"
    SubStatus.EXPIRED -> "Expired"
    SubStatus.SUSPENDED -> "Suspended"
    SubStatus.NO_PLAN -> "No Subscription"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(
    viewModel: LibDeskViewModel,
    selectedTabFromDrawer: Int = 0,
    onTabChange: (Int) -> Unit = {},
    onOpenMenu: () -> Unit = {},
    onNavigateToLibrary: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val superAdminProfile by viewModel.superAdminProfile.collectAsState()
    val libraries by viewModel.allLibraries.collectAsState()
    val saasPlans by viewModel.allSubscriptionPlansAdmin.collectAsState()
    val allSubscriptions by viewModel.allSubscriptions.collectAsState()

    var selectedTab by remember { mutableIntStateOf(selectedTabFromDrawer) }
    val pendingCount = remember(allSubscriptions) { allSubscriptions.count { it.status == "PENDING_VERIFICATION" } }

    val tabs = listOf("Overview", "Libraries", "Payments", "Plans", "Broadcast", "Settings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SaaS Admin", fontWeight = FontWeight.Bold)
                        Text(
                            "Subscriptions across every library",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index; onTabChange(index) },
                        text = {
                            if (index == 2 && pendingCount > 0) {
                                BadgedBox(badge = { Badge { Text("$pendingCount") } }) {
                                    Text(label)
                                }
                            } else {
                                Text(label)
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> OverviewTab(superAdminProfile, libraries, allSubscriptions, saasPlans) { selectedTab = it }
                1 -> LibrariesTab(libraries, allSubscriptions, saasPlans, viewModel, onNavigateToLibrary)
                2 -> PaymentsTab(allSubscriptions, libraries, saasPlans, viewModel)
                3 -> PlansTab(saasPlans, viewModel)
                4 -> BroadcastTab(libraries, viewModel)
                5 -> SettingsTab(superAdminProfile, viewModel, onLogout)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// OVERVIEW
// ---------------------------------------------------------------------------

@Composable
private fun OverviewTab(
    profile: SuperAdminUserEntity?,
    libraries: List<LibraryEntity>,
    subs: List<LibrarySubscriptionEntity>,
    plans: List<SubscriptionPlans>,
    onJumpToTab: (Int) -> Unit
) {
    val statuses = remember(libraries, subs) {
        libraries.associate { it.id to effectiveStatus(subs.firstOrNull { s -> s.libraryId == it.id }) }
    }
    val active = statuses.values.count { it == SubStatus.ACTIVE }
    val pending = statuses.values.count { it == SubStatus.PENDING }
    val expiredOrSuspended = statuses.values.count { it == SubStatus.EXPIRED || it == SubStatus.SUSPENDED }
    val mrr = subs.filter { effectiveStatus(it) == SubStatus.ACTIVE }.sumOf { it.price - it.discount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome, ${profile?.name ?: "Super Admin"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Libraries", "${libraries.size}", Icons.Default.Store, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            StatCard("Active Plans", "$active", Icons.Default.CheckCircle, LibDeskColors.success, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                "Needs Review", "$pending", Icons.Default.HourglassTop, LibDeskColors.warning,
                Modifier.weight(1f), onClick = { onJumpToTab(2) }
            )
            StatCard(
                "Expired/Suspended", "$expiredOrSuspended", Icons.Default.Warning, MaterialTheme.colorScheme.error,
                Modifier.weight(1f), onClick = { onJumpToTab(1) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("SaaS Plans", "${plans.size}", Icons.Default.CardMembership, MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
            StatCard("Est. Revenue/mo", "\u20B9${"%.0f".format(mrr)}", Icons.Default.CurrencyRupee, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }

        if (pending > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LibDeskColors.warningSoft),
                modifier = Modifier.fillMaxWidth().clickable { onJumpToTab(2) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("$pending payment${if (pending > 1) "s" else ""} awaiting verification", fontWeight = FontWeight.Bold)
                        Text("Review manual subscription payments", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.let { if (onClick != null) it.clickable(onClick = onClick) else it },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------------------
// LIBRARIES
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrariesTab(
    libraries: List<LibraryEntity>,
    subs: List<LibrarySubscriptionEntity>,
    plans: List<SubscriptionPlans>,
    viewModel: LibDeskViewModel,
    onNavigateToLibrary: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var detailLibrary by remember { mutableStateOf<LibraryEntity?>(null) }

    val filtered = remember(libraries, query) {
        if (query.isBlank()) libraries
        else libraries.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.ownerEmail.contains(query, ignoreCase = true) ||
                it.city.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 0.dp),
            placeholder = { Text("Search by name, owner email, or city") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShapeDefault()
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No libraries found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { lib ->
                    val sub = subs.firstOrNull { it.libraryId == lib.id }
                    val status = effectiveStatus(sub)
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { detailLibrary = lib },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(lib.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        lib.ownerEmail.ifBlank { "No owner email set" },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (lib.city.isNotBlank()) {
                                        Text(lib.city, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Surface(
                                    color = statusColor(status).copy(alpha = 0.15f),
                                    shape = RoundedCornerShapeDefault()
                                ) {
                                    Text(
                                        statusLabel(status),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = statusColor(status),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            if (sub != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "${sub.planName} \u2022 expires ${sub.expiryDate}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    detailLibrary?.let { lib ->
        LibraryDetailDialog(
            library = lib,
            subscription = subs.firstOrNull { it.libraryId == lib.id },
            plans = plans,
            onDismiss = { detailLibrary = null },
            onExtend = { plan, durationVal, durationUnit, price, discount, notes ->
                viewModel.extendOrUpdateLibrarySubscription(
                    libraryId = lib.id,
                    libraryName = lib.name,
                    plan = plan,
                    durationValue = durationVal,
                    durationUnit = durationUnit,
                    customPrice = price,
                    customDiscount = discount,
                    status = "ACTIVE",
                    maxSeats = plan.maxSeats,
                    notes = notes
                )
                detailLibrary = null
            },
            onToggleSuspend = { suspend ->
                viewModel.toggleLibrarySuspension(lib.id, lib.name, suspend)
                detailLibrary = null
            },
            onManage = {
                onNavigateToLibrary(lib.id)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryDetailDialog(
    library: LibraryEntity,
    subscription: LibrarySubscriptionEntity?,
    plans: List<SubscriptionPlans>,
    onDismiss: () -> Unit,
    onExtend: (SubscriptionPlans, durationVal: Int, durationUnit: String, price: Double, discount: Double, notes: String) -> Unit,
    onToggleSuspend: (Boolean) -> Unit,
    onManage: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf(plans.firstOrNull { it.id == subscription?.planId } ?: plans.firstOrNull()) }
    var durationUnit by remember { mutableStateOf(if (subscription?.durationUnit.equals("DAYS", ignoreCase = true)) "DAYS" else "MONTHS") }
    var durationText by remember { mutableStateOf(if (subscription?.durationUnit.equals("DAYS", ignoreCase = true) && (subscription?.durationDays ?: 0) > 0) subscription!!.durationDays.toString() else "") }
    var priceText by remember { mutableStateOf(subscription?.price?.let { if (it > 0) it.toString() else "" } ?: "") }
    var discountText by remember { mutableStateOf(subscription?.discount?.takeIf { it > 0 }?.toString() ?: "") }
    var notes by remember { mutableStateOf(subscription?.notes ?: "") }
    var planMenuExpanded by remember { mutableStateOf(false) }
    val status = effectiveStatus(subscription)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(library.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = statusColor(status).copy(alpha = 0.15f), shape = RoundedCornerShapeDefault()) {
                        Text(
                            statusLabel(status),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = statusColor(status), fontWeight = FontWeight.Bold, fontSize = 11.sp
                        )
                    }
                }
                Text("Owner: ${library.ownerName.ifBlank { "—" }} \u2022 ${library.ownerPhone.ifBlank { "—" }}", fontSize = 12.sp)
                Text("Email: ${library.ownerEmail.ifBlank { "—" }}", fontSize = 12.sp)
                if (subscription != null) {
                    Text(
                        "Current plan: ${subscription.planName} (\u20B9${subscription.price}, expires ${subscription.expiryDate})",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider()
                Text("Extend / change subscription", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                ExposedDropdownMenuBox(expanded = planMenuExpanded, onExpandedChange = { planMenuExpanded = it }) {
                    OutlinedTextField(
                        value = selectedPlan?.let {
                            val dur = if (it.durationType == "DAYS") "${it.durationDays}d" else "${it.durationMonths}mo"
                            "${it.name} (\u20B9${it.price}/$dur)"
                        } ?: "Select a plan",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Plan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = planMenuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = planMenuExpanded, onDismissRequest = { planMenuExpanded = false }) {
                        plans.forEach { plan ->
                            val dur = if (plan.durationType == "DAYS") "${plan.durationDays}d" else "${plan.durationMonths}mo"
                            DropdownMenuItem(
                                text = { Text("${plan.name} \u2013 \u20B9${plan.price} / $dur") },
                                onClick = {
                                    selectedPlan = plan
                                    planMenuExpanded = false
                                    if (plan.durationType == "DAYS") {
                                        durationUnit = "DAYS"
                                        durationText = plan.durationDays.toString()
                                    }
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = durationUnit == "MONTHS",
                        onClick = { durationUnit = "MONTHS" },
                        label = { Text("Months") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = durationUnit == "DAYS",
                        onClick = { durationUnit = "DAYS" },
                        label = { Text("Days") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { c -> c.isDigit() } },
                    label = { Text(if (durationUnit == "DAYS") "Duration in Days (e.g. 7, 15, 30)" else "Duration in Months (e.g. 1, 3, 6)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Price override (\u20B9)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Discount (\u20B9)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (internal)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                HorizontalDivider()
                OutlinedButton(
                    onClick = { onToggleSuspend(status != SubStatus.SUSPENDED) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(if (status == SubStatus.SUSPENDED) Icons.Default.PlayArrow else Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (status == SubStatus.SUSPENDED) "Reactivate Library" else "Suspend Library")
                }
                TextButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                    Text("Open library details")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedPlan != null,
                onClick = {
                    selectedPlan?.let {
                        onExtend(
                            it,
                            durationText.toIntOrNull() ?: 0,
                            durationUnit,
                            priceText.toDoubleOrNull() ?: 0.0,
                            discountText.toDoubleOrNull() ?: 0.0,
                            notes
                        )
                    }
                }
            ) { Text("Save Subscription") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

// ---------------------------------------------------------------------------
// PAYMENTS (manual subscription payments awaiting verification)
// ---------------------------------------------------------------------------

@Composable
private fun PaymentsTab(
    subs: List<LibrarySubscriptionEntity>,
    libraries: List<LibraryEntity>,
    plans: List<SubscriptionPlans>,
    viewModel: LibDeskViewModel
) {
    val pending = remember(subs) { subs.filter { it.status == "PENDING_VERIFICATION" } }

    if (pending.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LibDeskColors.success, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No payments waiting for review", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(pending, key = { it.id }) { sub ->
            val lib = libraries.firstOrNull { it.id == sub.libraryId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, LibDeskColors.warning.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(sub.libraryName, fontWeight = FontWeight.Bold)
                    Text("Plan: ${sub.planName} \u2022 \u20B9${sub.price}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (sub.notes.isNotBlank()) {
                        Text("Note: ${sub.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = LibDeskColors.success),
                            onClick = {
                                val plan = plans.firstOrNull { it.id == sub.planId }
                                    ?: SubscriptionPlans(
                                        id = sub.planId, name = sub.planName, durationMonths = 1,
                                        price = sub.price, maxSeats = sub.maxSeats, features = ""
                                    )
                                viewModel.extendOrUpdateLibrarySubscription(
                                    libraryId = sub.libraryId,
                                    libraryName = sub.libraryName,
                                    plan = plan,
                                    durationValue = if (sub.durationUnit.equals("DAYS", ignoreCase = true)) sub.durationDays else 0,
                                    durationUnit = if (sub.durationUnit.isNotBlank()) sub.durationUnit else if (plan.durationType == "DAYS") "DAYS" else "MONTHS",
                                    customPrice = sub.price,
                                    customDiscount = sub.discount,
                                    status = "ACTIVE",
                                    maxSeats = sub.maxSeats,
                                    notes = sub.notes
                                )
                            }
                        ) { Text("Approve") }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            onClick = {
                                viewModel.saveLibrarySubscription(sub.copy(status = "REJECTED", updatedAt = System.currentTimeMillis()))
                            }
                        ) { Text("Reject") }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PLANS
// ---------------------------------------------------------------------------

@Composable
private fun PlansTab(plans: List<SubscriptionPlans>, viewModel: LibDeskViewModel) {
    var editingPlan by remember { mutableStateOf<SubscriptionPlans?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var deletingPlan by remember { mutableStateOf<SubscriptionPlans?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (plans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No SaaS plans yet. Tap + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(plans, key = { it.id }) { plan ->
                    val isDays = plan.durationType.equals("DAYS", ignoreCase = true) || (plan.durationDays > 0 && plan.durationMonths <= 1)
                    val durLabel = if (isDays) "${plan.durationDays} Days" else "${plan.durationMonths} Month${if (plan.durationMonths > 1) "s" else ""}"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(plan.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Surface(
                                            color = if (isDays) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                durLabel,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDays) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Text(
                                        "₹${plan.price} / $durLabel • up to ${plan.maxSeats} seats",
                                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!plan.isActive) {
                                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShapeDefault()) {
                                        Text("Inactive", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp)
                                    }
                                }
                            }
                            if (plan.features.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(plan.features, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { editingPlan = plan }, modifier = Modifier.weight(1f)) {
                                    Text("Edit")
                                }
                                OutlinedButton(
                                    onClick = { deletingPlan = plan },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreate = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Default.Add, contentDescription = "New plan") }
    }

    if (showCreate) {
        PlanEditDialog(
            plan = null,
            onDismiss = { showCreate = false },
            onSave = { viewModel.saveSubscriptionPlanAdmin(it); showCreate = false }
        )
    }
    editingPlan?.let { plan ->
        PlanEditDialog(
            plan = plan,
            onDismiss = { editingPlan = null },
            onSave = { viewModel.saveSubscriptionPlanAdmin(it); editingPlan = null }
        )
    }
    deletingPlan?.let { plan ->
        AlertDialog(
            onDismissRequest = { deletingPlan = null },
            title = { Text("Delete \"${plan.name}\"?") },
            text = { Text("Libraries already subscribed to this plan keep their current subscription; this only removes it from the plan list.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = { viewModel.deleteSubscriptionPlanAdmin(plan); deletingPlan = null }
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deletingPlan = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PlanEditDialog(
    plan: SubscriptionPlans?,
    onDismiss: () -> Unit,
    onSave: (SubscriptionPlans) -> Unit
) {
    val initialIsDays = plan?.durationType.equals("DAYS", ignoreCase = true) || ((plan?.durationDays ?: 0) > 0 && (plan?.durationMonths ?: 1) <= 1)
    var name by remember { mutableStateOf(plan?.name ?: "") }
    var durationUnit by remember { mutableStateOf(if (initialIsDays) "DAYS" else "MONTHS") }
    var duration by remember {
        mutableStateOf(
            if (initialIsDays && (plan?.durationDays ?: 0) > 0) plan!!.durationDays.toString()
            else plan?.durationMonths?.toString() ?: "1"
        )
    }
    var price by remember { mutableStateOf(plan?.price?.toString() ?: "") }
    var maxSeats by remember { mutableStateOf(plan?.maxSeats?.toString() ?: "50") }
    var features by remember { mutableStateOf(plan?.features ?: "") }
    var badge by remember { mutableStateOf(plan?.badge ?: "") }
    var isActive by remember { mutableStateOf(plan?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (plan == null) "New SaaS Plan" else "Edit Plan") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Plan name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = durationUnit == "MONTHS",
                        onClick = {
                            durationUnit = "MONTHS"
                            if (duration == "15" || duration == "30") duration = "1"
                        },
                        label = { Text("Months") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = durationUnit == "DAYS",
                        onClick = {
                            durationUnit = "DAYS"
                            if (duration == "1") duration = "15"
                        },
                        label = { Text("Days") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = duration, onValueChange = { duration = it.filter { c -> c.isDigit() } },
                        label = { Text(if (durationUnit == "DAYS") "Duration (days)" else "Duration (months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = price, onValueChange = { price = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Price (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                OutlinedTextField(
                    value = maxSeats, onValueChange = { maxSeats = it.filter { c -> c.isDigit() } },
                    label = { Text("Max seats") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value = features, onValueChange = { features = it }, label = { Text("Features (shown to library owners)") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
                OutlinedTextField(value = badge, onValueChange = { badge = it }, label = { Text("Badge (e.g. \"Most Popular\", optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Active (visible to libraries)")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && price.toDoubleOrNull() != null && (duration.toIntOrNull() ?: 0) > 0,
                onClick = {
                    val durNum = duration.toIntOrNull() ?: 1
                    val (m, d) = if (durationUnit == "DAYS") {
                        Pair((durNum / 30).coerceAtLeast(1), durNum)
                    } else {
                        Pair(durNum, durNum * 30)
                    }
                    onSave(
                        SubscriptionPlans(
                            id = plan?.id ?: "PLAN-${System.currentTimeMillis()}",
                            name = name.trim(),
                            durationMonths = m,
                            durationDays = d,
                            durationType = durationUnit,
                            price = price.toDoubleOrNull() ?: 0.0,
                            maxSeats = maxSeats.toIntOrNull() ?: 50,
                            features = features.trim(),
                            isActive = isActive,
                            badge = badge.trim(),
                            upiId = plan?.upiId ?: "libdesk.billing@upi",
                            upiPayeeName = plan?.upiPayeeName ?: "LibDesk Subscriptions"
                        )
                    )
                }
            ) { Text(if (plan == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------------------
// BROADCAST (IN-APP PUSH MESSAGING & NOTIFICATIONS)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BroadcastTab(libraries: List<LibraryEntity>, viewModel: LibDeskViewModel) {
    val broadcastNotices by viewModel.allBroadcastNotices.collectAsState()
    var targetAudience by remember { mutableStateOf("ALL") } // "ALL", "LIBRARIES", "STUDENTS", "SPECIFIC"
    var selectedTargetLibrary by remember { mutableStateOf<LibraryEntity?>(null) }
    var libraryDropdownExpanded by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf("NORMAL") } // "NORMAL", "URGENT", "ANNOUNCEMENT"
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var deletingNotice by remember { mutableStateOf<NoticeEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("In-App Push Messaging & Broadcast", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Send instant in-app announcements, push notifications, and alerts directly to registered libraries and library students.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Compose Push Broadcast", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Text("Target Audience", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetAudience == "ALL",
                        onClick = { targetAudience = "ALL"; selectedTargetLibrary = null },
                        label = { Text("All", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = targetAudience == "LIBRARIES",
                        onClick = { targetAudience = "LIBRARIES"; selectedTargetLibrary = null },
                        label = { Text("Libraries", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = targetAudience == "STUDENTS",
                        onClick = { targetAudience = "STUDENTS"; selectedTargetLibrary = null },
                        label = { Text("Students", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = targetAudience == "SPECIFIC",
                        onClick = { targetAudience = "SPECIFIC" },
                        label = { Text("Specific", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (targetAudience == "SPECIFIC") {
                    ExposedDropdownMenuBox(
                        expanded = libraryDropdownExpanded,
                        onExpandedChange = { libraryDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTargetLibrary?.name ?: "Select Target Library",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Target Library") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = libraryDropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = libraryDropdownExpanded,
                            onDismissRequest = { libraryDropdownExpanded = false }
                        ) {
                            libraries.forEach { lib ->
                                DropdownMenuItem(
                                    text = { Text("${lib.name} (${lib.city.ifBlank { "Main" }})") },
                                    onClick = {
                                        selectedTargetLibrary = lib
                                        libraryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text("Priority", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NORMAL", "ANNOUNCEMENT", "URGENT").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (p == "URGENT") LibDeskColors.warningSoft else MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notification Title (e.g. System Maintenance, New Offer)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null) }
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Message Body / Announcement Details") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Button(
                    onClick = {
                        val targetLibId = if (targetAudience == "SPECIFIC") selectedTargetLibrary?.id else null
                        viewModel.broadcastSuperAdminMessage(
                            title = title,
                            content = content,
                            targetAudience = targetAudience,
                            priority = priority,
                            targetLibraryId = targetLibId
                        )
                        title = ""
                        content = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank() && content.isNotBlank() && (targetAudience != "SPECIFIC" || selectedTargetLibrary != null)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send In-App Notification")
                }
            }
        }

        Text("Broadcast History (${broadcastNotices.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (broadcastNotices.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No notifications broadcasted yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            broadcastNotices.forEach { notice ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(notice.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (notice.priority == "URGENT") LibDeskColors.warningSoft else MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        notice.priority,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (notice.priority == "URGENT") LibDeskColors.warning else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                IconButton(onClick = { deletingNotice = notice }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Text(notice.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Audience: ${notice.targetAudience} • By: ${notice.senderName.ifBlank { "Super Admin" }}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(notice.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    deletingNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { deletingNotice = null },
            title = { Text("Delete Broadcast?") },
            text = { Text("Are you sure you want to remove notification '${notice.title}'? It will disappear from all libraries and student devices.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteBroadcastNotice(notice)
                        deletingNotice = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingNotice = null }) { Text("Cancel") }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// SETTINGS
// ---------------------------------------------------------------------------

@Composable
private fun SettingsTab(profile: SuperAdminUserEntity?, viewModel: LibDeskViewModel, onLogout: () -> Unit) {
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var email by remember(profile) { mutableStateOf(profile?.email ?: "") }
    var mobile by remember(profile) { mutableStateOf(profile?.mobile ?: "") }
    var upiId by remember(profile) { mutableStateOf(profile?.upiId ?: "libdesk.billing@upi") }
    var upiPayeeName by remember(profile) { mutableStateOf(profile?.upiPayeeName ?: "LibDesk Subscriptions") }
    var showResetConfirm by remember { mutableStateOf(false) }
    var resetConfirmText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Account & Support Contact", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Your contact number is automatically mapped across the app for student & library helpline, WhatsApp support, and billing queries.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Super Admin Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it.filter { c -> c.isDigit() || c == '+' } },
                    label = { Text("Official Contact & WhatsApp Support Phone") },
                    placeholder = { Text("e.g. +91 9876543210") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                )
                Button(
                    onClick = {
                        viewModel.updateSuperAdminProfile(
                            email = email.trim(),
                            name = name.trim(),
                            mobile = mobile.trim(),
                            accessCode = "",
                            is2Fa = true,
                            upiId = upiId.trim(),
                            upiPayeeName = upiPayeeName.trim()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && email.isNotBlank()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes & Sync Helpline")
                }
            }
        }

        Text("UPI Billing & Payment Gateway", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "All registered libraries send their subscription fees directly to this UPI ID. Updating this synchronizes it across all subscription plans immediately.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("Super Admin UPI ID (VPA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Payment, contentDescription = null) }
                )
                OutlinedTextField(
                    value = upiPayeeName,
                    onValueChange = { upiPayeeName = it },
                    label = { Text("Payee Business Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
                )
                Button(
                    onClick = {
                        viewModel.updateSuperAdminUpi(upiId.trim(), upiPayeeName.trim())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = upiId.isNotBlank()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync UPI ID Across All Plans")
                }
            }
        }

        Text("Security", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("2-Factor Email OTP", fontWeight = FontWeight.Bold)
                    Text("Mandatory for every Super Admin login. Cannot be disabled.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = LibDeskColors.successSoft, shape = RoundedCornerShapeDefault()) {
                    Text("ENABLED", modifier = Modifier.padding(10.dp, 6.dp), fontWeight = FontWeight.Bold, color = LibDeskColors.success, fontSize = 10.sp)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Single Admin Account", fontWeight = FontWeight.Bold)
                    Text("LibDesk allows exactly one Super Admin account at a time.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShapeDefault()) {
                    Text("ENFORCED", modifier = Modifier.padding(10.dp, 6.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        OutlinedButton(
            onClick = { showResetConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset Super Admin Slot")
        }

        Button(
            onClick = { viewModel.logout(); onLogout() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Logout")
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false; resetConfirmText = "" },
            title = { Text("Reset Super Admin Slot?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This signs out the current Super Admin permanently and allows a brand new account to claim the single Super Admin slot. This cannot be undone.")
                    Text("Type RESET to confirm.", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = resetConfirmText,
                        onValueChange = { resetConfirmText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = resetConfirmText.trim().equals("RESET", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.resetSuperAdminSlot { onLogout() }
                        showResetConfirm = false
                        resetConfirmText = ""
                    }
                ) { Text("Confirm Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false; resetConfirmText = "" }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RoundedCornerShapeDefault() = MaterialTheme.shapes.small
