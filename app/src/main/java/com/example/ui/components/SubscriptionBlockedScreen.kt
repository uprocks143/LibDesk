package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.payment.UpiPaymentHelper
import com.example.viewmodel.LiveSubscriptionCheck

/**
 * Fail-closed subscription gate. Shown for the Manager/Owner role whenever a
 * live (non-cached) check against Supabase does not confirm an Active
 * subscription — including when the check simply couldn't be verified at
 * all (no internet).
 */
@Composable
fun SubscriptionBlockedScreen(
    check: LiveSubscriptionCheck?,
    supportPhone: String? = null,
    onRetry: () -> Unit,
    onViewPlans: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val content = when (check) {
        null -> BlockedContent(
            Icons.Default.HourglassTop,
            "Checking your subscription…",
            "Verifying your library's subscription status. This needs an active internet connection.",
            "Retry" to onRetry
        )
        LiveSubscriptionCheck.NetworkError -> BlockedContent(
            Icons.Default.CloudOff,
            "Can't verify your subscription",
            "LibDesk needs to confirm your subscription is active before you can use the app. Please check your internet connection and try again.",
            "Retry" to onRetry
        )
        LiveSubscriptionCheck.Expired -> BlockedContent(
            Icons.Default.Warning,
            "Subscription Expired",
            "Your library's subscription has expired. Renew your plan to restore access for you and your students.",
            "Renew Now" to onViewPlans
        )
        LiveSubscriptionCheck.Inactive -> BlockedContent(
            Icons.Default.Block,
            "Subscription Inactive",
            "Your library's subscription is inactive in Supabase. Please activate your subscription or contact support to restore access.",
            "View Plans" to onViewPlans
        )
        LiveSubscriptionCheck.Suspended -> BlockedContent(
            Icons.Default.Block,
            "Account Suspended",
            "Your library's access has been suspended by the platform admin. Please clear any pending dues or contact support.",
            "View Subscription" to onViewPlans
        )
        LiveSubscriptionCheck.PendingVerification -> BlockedContent(
            Icons.Default.HourglassTop,
            "Payment Under Review",
            "We've received your payment submission. Access will unlock automatically once the admin verifies it — usually within a few hours.",
            "Retry" to onRetry
        )
        LiveSubscriptionCheck.NoSubscription -> BlockedContent(
            Icons.Default.Warning,
            "No Active Subscription",
            "Your library doesn't have an active subscription yet. Choose a plan to get started.",
            "View Plans" to onViewPlans
        )
        LiveSubscriptionCheck.Active -> return // shouldn't render, caller already gates on this
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(content.icon, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(34.dp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(content.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    content.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = content.primary.second,
                    modifier = Modifier.fillMaxWidth(0.85f).height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(content.primary.first, fontWeight = FontWeight.Bold)
                }

                if (!supportPhone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            UpiPaymentHelper.shareReceiptToWhatsApp(
                                context = context,
                                whatsappNumber = supportPhone,
                                planName = "Account Status Inquiry",
                                amount = 0.0,
                                utrNumber = "N/A",
                                libraryName = "Library",
                                ownerName = "Manager"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(0.85f).height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp_real),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Contact Super Admin Support", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onLogout) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private data class BlockedContent(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val message: String,
    val primary: Pair<String, () -> Unit>
)
