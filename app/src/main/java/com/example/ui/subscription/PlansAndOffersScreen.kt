package com.example.ui.subscription

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.entities.SubscriptionPlans
import com.example.data.local.entities.UserSubscription
import com.example.util.payment.UpiPaymentHelper
import com.example.viewmodel.LibDeskViewModel
import java.util.Locale

/**
 * Modern Compose Screen displaying available Subscription Plans & Offers
 * fetched directly from the Room Database, with UPI QR code generation and
 * WhatsApp receipt sharing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansAndOffersScreen(
    viewModel: LibDeskViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val plans by viewModel.subscriptionPlans.collectAsState()
    val activeUserSub by viewModel.userSubscription.collectAsState()
    val currentLibrary by viewModel.currentLibrary.collectAsState()
    val trialDaysRemaining by viewModel.trialDaysRemaining.collectAsState()
    val isTrialActive by viewModel.isTrialActive.collectAsState()

    var selectedBillingCycle by remember { mutableStateOf("MONTHLY") } // MONTHLY, QUARTERLY, ANNUAL
    var selectedPlanForPayment by remember { mutableStateOf<SubscriptionPlans?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Plans & Offers",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Supercharge your library management",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("plans_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to dashboard"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Previously this always sent to a hardcoded fake
                            // number ("") regardless of any real
                            // configuration — every "General Inquiry" from every
                            // library owner went to a random, unrelated phone
                            // number instead of real LibDesk support.
                            val supportPhone = plans.firstOrNull { it.supportWhatsApp.isNotBlank() }?.supportWhatsApp
                            if (supportPhone != null) {
                                UpiPaymentHelper.shareReceiptToWhatsApp(
                                    context = context,
                                    whatsappNumber = supportPhone,
                                    planName = "General Inquiry",
                                    amount = 0.0,
                                    utrNumber = "N/A",
                                    libraryName = currentLibrary?.name ?: "",
                                    ownerName = viewModel.currentUserName.value
                                )
                            } else {
                                com.example.ui.components.SnackbarController.showError(
                                    "No support contact number is configured yet."
                                )
                            }
                        },
                        modifier = Modifier.testTag("plans_whatsapp_support_button")
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp_real),
                            contentDescription = "Contact WhatsApp Support",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.testTag("plans_and_offers_screen")
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Current Active Membership Banner
            item {
                ActiveMembershipCard(
                    userSub = activeUserSub,
                    isTrial = isTrialActive,
                    trialDaysRemaining = trialDaysRemaining,
                    libraryName = currentLibrary?.name ?: "Your Library"
                )
            }

            // Promotional Offers Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("special_offers_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LIMITED PERIOD OFFER",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "35% OFF",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Get up to 35% discount & 2 months free on Annual subscriptions! Instant UPI activation available.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Billing Cycle Selector Tabs
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose Billing Cycle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BillingCycleChip(
                            title = "Monthly",
                            discountBadge = null,
                            isSelected = selectedBillingCycle == "MONTHLY",
                            onClick = { selectedBillingCycle = "MONTHLY" },
                            modifier = Modifier.weight(1f)
                        )
                        BillingCycleChip(
                            title = "Quarterly",
                            discountBadge = "10% OFF",
                            isSelected = selectedBillingCycle == "QUARTERLY",
                            onClick = { selectedBillingCycle = "QUARTERLY" },
                            modifier = Modifier.weight(1f)
                        )
                        BillingCycleChip(
                            title = "Annual",
                            discountBadge = "35% OFF",
                            isSelected = selectedBillingCycle == "ANNUAL",
                            onClick = { selectedBillingCycle = "ANNUAL" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Subscription Plans Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Plans (${plans.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Manual UPI & QR Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Subscription Plan Cards
            if (plans.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            } else {
                items(plans, key = { it.id }) { plan ->
                    SubscriptionPlanCard(
                        plan = plan,
                        billingCycle = selectedBillingCycle,
                        onPayClick = {
                            selectedPlanForPayment = plan
                        }
                    )
                }
            }
        }
    }

    // Modal for Manual UPI Payment & WhatsApp Receipt Sharing
    selectedPlanForPayment?.let { plan ->
        ManualUpiPaymentDialog(
            plan = plan,
            billingCycle = selectedBillingCycle,
            libraryName = currentLibrary?.name ?: "",
            ownerName = viewModel.currentUserName.value.ifBlank { currentLibrary?.ownerName ?: "" },
            ownerMobile = currentLibrary?.ownerPhone ?: "",
            onDismiss = { selectedPlanForPayment = null },
            onSubmitPayment = { utr, receiptUri ->
                viewModel.submitManualSubscriptionPayment(
                    plan = plan,
                    utrNumber = utr,
                    billingCycle = selectedBillingCycle,
                    receiptImageUri = receiptUri,
                    ownerName = viewModel.currentUserName.value,
                    ownerMobile = currentLibrary?.ownerPhone ?: "",
                    ownerEmail = viewModel.currentUserEmail.value
                )

                // Only attempt WhatsApp handoff when a real, admin-configured
                // support number exists. Previously a blank number here was
                // silently replaced with a made-up "" — sending
                // the owner's real UTR/payment receipt screenshot to whoever
                // actually owns that number, not to LibDesk support.
                if (plan.supportWhatsApp.isNotBlank()) {
                    UpiPaymentHelper.shareReceiptToWhatsApp(
                        context = context,
                        whatsappNumber = plan.supportWhatsApp,
                        planName = plan.name,
                        amount = calculateCyclePrice(plan, selectedBillingCycle),
                        utrNumber = utr,
                        libraryName = currentLibrary?.name ?: "",
                        ownerName = viewModel.currentUserName.value,
                        billingCycle = selectedBillingCycle,
                        receiptImageUri = receiptUri
                    )
                } else {
                    com.example.ui.components.SnackbarController.showError(
                        "Payment submitted for review. No support WhatsApp number is configured, so the receipt wasn't auto-shared — the Super Admin will verify it from the Payments tab."
                    )
                }

                selectedPlanForPayment = null
                com.example.ui.components.SnackbarController.showSuccess("Payment submitted! It will be verified shortly.")
            }
        )
    }
}

/**
 * Displays active library membership details or free trial status.
 */
@Composable
private fun ActiveMembershipCard(
    userSub: UserSubscription?,
    isTrial: Boolean,
    trialDaysRemaining: Int,
    libraryName: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_membership_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Current Membership",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val status = userSub?.status ?: if (isTrial) "TRIAL" else "ACTIVE"
                val (statusColor, statusBg) = when (status) {
                    "ACTIVE" -> Color(0xFF059669) to Color(0xFFD1FAE5)
                    "PENDING_VERIFICATION" -> Color(0xFFD97706) to Color(0xFFFEF3C7)
                    else -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.primaryContainer
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (status == "PENDING_VERIFICATION") "UNDER REVIEW" else status,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val planTitle = userSub?.planName ?: if (isTrial) "15-Day Free Trial" else "Growth Pro"
            Text(
                text = planTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Library: $libraryName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VALIDITY / EXPIRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val validityStr = userSub?.expiryDate ?: if (isTrial) "$trialDaysRemaining Days Left" else "Active (Auto-renew)"
                    Text(
                        text = validityStr,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (trialDaysRemaining <= 3 && isTrial) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (userSub?.status == "PENDING_VERIFICATION") {
                    Text(
                        text = "Ref: ${userSub.paymentReferenceId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cloud Synced",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chip for switching between Monthly, Quarterly, and Annual billing cycles.
 */
@Composable
private fun BillingCycleChip(
    title: String,
    discountBadge: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = onClick)
            .testTag("cycle_${title.lowercase()}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                discountBadge?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Card rendering an individual Subscription Plan with features and UPI payment action.
 */
@Composable
private fun SubscriptionPlanCard(
    plan: SubscriptionPlans,
    billingCycle: String,
    onPayClick: () -> Unit
) {
    val payableAmount = calculateCyclePrice(plan, billingCycle)
    val originalAmount = calculateOriginalPrice(plan, billingCycle)
    val isFeatured = plan.badge.isNotBlank() && (plan.badge.contains("Popular", ignoreCase = true) || plan.badge.contains("Best", ignoreCase = true))

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (isFeatured) 2.dp else 1.dp,
            if (isFeatured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("plan_card_${plan.id}")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row: Plan Name + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (plan.description.isNotBlank()) {
                        Text(
                            text = plan.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (plan.badge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isFeatured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = plan.badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isFeatured) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pricing Row
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "₹${String.format(Locale.US, "%.0f", payableAmount)}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (originalAmount > payableAmount) {
                    Text(
                        text = "₹${String.format(Locale.US, "%.0f", originalAmount)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = when (billingCycle) {
                        "ANNUAL" -> "/ year"
                        "QUARTERLY" -> "/ 3 months"
                        else -> "/ month"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Max Seats Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = if (plan.maxSeats >= 9999) "Unlimited Seats" else "${plan.maxSeats} Seats",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Features List
            val featureItems = plan.features.split("\n", ",").map { it.trim() }.filter { it.isNotBlank() }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                featureItems.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Pay via UPI Action Button
            Button(
                onClick = onPayClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFeatured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isFeatured) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pay_button_${plan.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pay via UPI • ₹${String.format(Locale.US, "%.0f", payableAmount)}",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Modal dialog displaying the dynamically generated UPI QR code, deep link trigger,
 * and WhatsApp payment receipt sharing form.
 */
@Composable
private fun ManualUpiPaymentDialog(
    plan: SubscriptionPlans,
    billingCycle: String,
    libraryName: String,
    ownerName: String,
    ownerMobile: String,
    onDismiss: () -> Unit,
    onSubmitPayment: (utrNumber: String, receiptUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    val payableAmount = calculateCyclePrice(plan, billingCycle)
    val upiId = plan.upiId.ifBlank { "libdesk.billing@upi" }
    val payeeName = plan.upiPayeeName.ifBlank { "LibDesk Subscriptions" }

    // Standard NPCI UPI URI
    val upiDeepLink = remember(plan, billingCycle, payableAmount) {
        UpiPaymentHelper.buildUpiDeepLink(
            upiId = upiId,
            payeeName = payeeName,
            amount = payableAmount,
            note = "${plan.name} ($billingCycle) - $libraryName"
        )
    }

    // Dynamic QR Bitmap generated via ZXing library
    val qrBitmap = remember(upiDeepLink) {
        UpiPaymentHelper.generateUpiQrBitmap(upiDeepLink, size = 480)
    }

    var utrNumber by remember { mutableStateOf("") }
    var selectedReceiptUri by remember { mutableStateOf<Uri?>(null) }
    var utrError by remember { mutableStateOf(false) }

    // Android Photo Picker for receipt selection
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedReceiptUri = uri
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("upi_payment_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "UPI Manual Payment",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${plan.name} ($billingCycle) • ₹${String.format(Locale.US, "%.2f", payableAmount)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_payment_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Generated UPI QR Code Container
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .size(240.dp)
                            .testTag("upi_qr_code_image")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().padding(12.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "UPI Payment QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // UPI ID Badge with Copy button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "PAY TO UPI ID",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = upiId,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(
                                onClick = {
                                    UpiPaymentHelper.copyToClipboard(context, "UPI ID", upiId)
                                },
                                modifier = Modifier.testTag("copy_upi_id_button")
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy")
                            }
                        }
                    }

                    // Action buttons: Launch UPI App & Share QR Image
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                val opened = UpiPaymentHelper.openUpiPayIntent(context, upiDeepLink)
                                if (!opened) {
                                    // Toast.makeText(context, "Could not open UPI app. Please scan the QR code.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("open_upi_app_button")
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open UPI App", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                val cardBitmap = UpiPaymentHelper.generateBrandedUpiPaymentCard(
                                    context = context,
                                    upiDeepLink = upiDeepLink,
                                    planName = plan.name,
                                    amount = payableAmount,
                                    upiId = upiId,
                                    payeeName = payeeName,
                                    libraryName = libraryName,
                                    billingCycle = billingCycle
                                )
                                UpiPaymentHelper.shareUpiPaymentCard(
                                    context = context,
                                    bitmap = cardBitmap,
                                    planName = plan.name,
                                    amount = payableAmount,
                                    upiId = upiId,
                                    upiDeepLink = upiDeepLink
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("share_qr_image_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share QR", maxLines = 1)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Step 2: Verification Details Form
                    Text(
                        text = "Verify Payment & Send Receipt",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = utrNumber,
                        onValueChange = {
                            utrNumber = it
                            utrError = false
                        },
                        label = { Text("12-Digit UTR / Transaction Ref *") },
                        placeholder = { Text("e.g. 429182739485") },
                        isError = utrError,
                        supportingText = {
                            if (utrError) {
                                Text("Please enter the 12-digit UTR from your UPI app receipt", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Found on the payment confirmation screen in GPay/PhonePe/Paytm")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("utr_input_field")
                    )

                    // Optional Payment Screenshot Attachment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedReceiptUri != null) "Screenshot Attached" else "Attach Payment Screenshot (Optional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("attach_screenshot_button")
                        ) {
                            Icon(imageVector = Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (selectedReceiptUri != null) "Change" else "Attach")
                        }
                    }

                    if (selectedReceiptUri != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Receipt Image Selected",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                                IconButton(
                                    onClick = { selectedReceiptUri = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove attachment", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Action Button: WhatsApp Intent Trigger
                Button(
                    onClick = {
                        if (utrNumber.trim().length < 6) {
                            utrError = true
                            return@Button
                        }
                        onSubmitPayment(utrNumber.trim(), selectedReceiptUri)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("share_receipt_whatsapp_button")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp_real),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Share Receipt on WhatsApp",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * Calculates payable price for selected billing cycle with discounts applied.
 */
private fun calculateCyclePrice(plan: SubscriptionPlans, cycle: String): Double {
    return when (cycle.uppercase()) {
        "ANNUAL" -> {
            val annualOriginal = plan.price * 12
            val discount = plan.discountPercentage.coerceAtLeast(35.0)
            annualOriginal * (1.0 - (discount / 100.0))
        }
        "QUARTERLY" -> {
            (plan.price * 3) * 0.90 // 10% discount on quarterly
        }
        else -> plan.price
    }
}

/**
 * Calculates original price without promotional discounts for strikethrough display.
 */
private fun calculateOriginalPrice(plan: SubscriptionPlans, cycle: String): Double {
    return when (cycle.uppercase()) {
        "ANNUAL" -> plan.price * 12
        "QUARTERLY" -> plan.price * 3
        else -> plan.price
    }
}
