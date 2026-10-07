package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LibDeskColors

/**
 * Comprehensive Privacy Policy, Data Protection & Library Terms of Service Modal.
 * Covers Seat Allocation, Locker Safekeeping, Student Data Management (DPDP Act 2023),
 * Biometrics, CCTV, Fee Refund & 1-Day Visitor guidelines.
 */
@Composable
fun LibraryPrivacyAndTermsModal(
    libraryName: String = "LibDesk Smart Library",
    onDismiss: () -> Unit
) {
    var selectedSection by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("privacy_policy_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Privacy Policy & Terms",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$libraryName • Legal & Data Safety",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_privacy_policy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                val tabs = listOf(
                    "All Overview",
                    "Seat & Hall",
                    "Locker Safety",
                    "Data Privacy",
                    "Biometrics & CCTV",
                    "Fees & Refunds"
                )

                ScrollableTabRow(
                    selectedTabIndex = selectedSection,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    edgePadding = 8.dp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedSection == index,
                            onClick = { selectedSection = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selectedSection == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedSection) {
                        0 -> {
                            PolicyOverviewCard(libraryName)
                            SeatPolicySection()
                            LockerPolicySection()
                            DataPrivacySection()
                            BiometricSecuritySection()
                            FeeAndRefundSection()
                        }
                        1 -> SeatPolicySection()
                        2 -> LockerPolicySection()
                        3 -> DataPrivacySection()
                        4 -> BiometricSecuritySection()
                        5 -> FeeAndRefundSection()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("accept_privacy_policy_button")
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I Understand & Acknowledge", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PolicyOverviewCard(libraryName: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Commitment to Student Safety & Data Integrity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "This policy governs the terms of use, privacy protection, and operational standards for all members, visitors, and management at $libraryName.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun SeatPolicySection() {
    PolicyCard(
        title = "1. Seat Allocation & Reading Hall Discipline",
        icon = Icons.Default.Chair,
        accentColor = LibDeskColors.info
    ) {
        PolicyPoint(
            number = "1.1",
            heading = "Seat Allocation & Reservation",
            content = "Reserved seats are allocated strictly to registered members for their designated shift hours. Flexi seats operate on a first-come, first-served basis within the registered shift timing."
        )
        PolicyPoint(
            number = "1.2",
            heading = "Attendance & QR Check-In",
            content = "Students must punch in using the dynamic QR code scanner at the entrance upon arrival and punch out when leaving. Multiple missed punch-ins may trigger attendance notifications to parents/students."
        )
        PolicyPoint(
            number = "1.3",
            heading = "Silent Zone Protocol",
            content = "Strict silence must be maintained in reading halls. Phone calls, discussions, and video playback on speaker are prohibited. Headphones must be used at moderate volume."
        )
        PolicyPoint(
            number = "1.4",
            heading = "Seat & Shift Change Requests",
            content = "Students can submit seat/shift change requests via their student portal. Requests are reviewed and processed within 24-48 hours subject to seat availability."
        )
    }
}

@Composable
private fun LockerPolicySection() {
    PolicyCard(
        title = "2. Locker Safekeeping & Key Liability",
        icon = Icons.Default.Lock,
        accentColor = LibDeskColors.warning
    ) {
        PolicyPoint(
            number = "2.1",
            heading = "Locker Allotment & Security Deposit",
            content = "Personal study lockers are allotted on a monthly or quarterly subscription basis with a refundable security deposit. One key is provided to the member."
        )
        PolicyPoint(
            number = "2.2",
            heading = "Key Loss & Duplication",
            content = "Students are solely responsible for the safety of their locker key. In case of key loss, a replacement fee of ₹150 applies for lock replacement."
        )
        PolicyPoint(
            number = "2.3",
            heading = "Prohibited Articles",
            content = "Storage of perishable food items, inflammable substances, weapons, or illegal contraband is strictly forbidden. Management reserves the right to conduct safety audits with notice."
        )
        PolicyPoint(
            number = "2.4",
            heading = "Vacating & Deposit Refund",
            content = "Upon subscription expiry or seat surrender, lockers must be emptied and key returned. Security deposit will be refunded within 3 working days after clearance."
        )
    }
}

@Composable
private fun DataPrivacySection() {
    PolicyCard(
        title = "3. Student Personal Data Protection (DPDP Act 2023)",
        icon = Icons.Default.Security,
        accentColor = LibDeskColors.success
    ) {
        PolicyPoint(
            number = "3.1",
            heading = "Data Collected",
            content = "We collect only essential details: Full name, contact number, email, guardian phone, course/exam target, emergency contact, and timestamped attendance records."
        )
        PolicyPoint(
            number = "3.2",
            heading = "Zero Commercial Sharing",
            content = "Your personal information is NEVER sold, rented, or shared with third-party coaching institutes, marketing aggregators, or advertisers."
        )
        PolicyPoint(
            number = "3.3",
            heading = "Encryption & Local-First Storage",
            content = "All student databases and financial records are stored using AES encrypted local database with TLS 1.3 secure cloud backups."
        )
        PolicyPoint(
            number = "3.4",
            heading = "Right to Erasure & Rectification",
            content = "Members have the right to request deletion or correction of their personal data upon completion of their library tenure."
        )
    }
}

@Composable
private fun BiometricSecuritySection() {
    PolicyCard(
        title = "4. Biometric Authentication & CCTV Surveillance",
        icon = Icons.Default.Fingerprint,
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        PolicyPoint(
            number = "4.1",
            heading = "On-Device Biometric Lock",
            content = "Biometric authentication (fingerprint / face unlock) operates entirely on-device via Android Keystore. Raw biometric data never leaves your smartphone."
        )
        PolicyPoint(
            number = "4.2",
            heading = "CCTV Monitoring & Safety",
            content = "Library common areas, entrance gate, and corridors are under 24/7 CCTV surveillance to ensure physical security and prevent theft of personal belongings."
        )
        PolicyPoint(
            number = "4.3",
            heading = "Footage Access & Retention",
            content = "CCTV recordings are retained for 30 days and accessed solely for investigating reported grievances, security breaches, or by authorized law enforcement."
        )
    }
}

@Composable
private fun FeeAndRefundSection() {
    PolicyCard(
        title = "5. Fee Structure, Invoicing & Refund Policy",
        icon = Icons.Default.ReceiptLong,
        accentColor = Color(0xFF8B5CF6)
    ) {
        PolicyPoint(
            number = "5.1",
            heading = "Advance Fee Payment Cycle",
            content = "Library membership fees are payable in advance on or before the due date. A 3-day grace period is provided before seat deallocation."
        )
        PolicyPoint(
            number = "5.2",
            heading = "Digital Receipts & UPI Invoicing",
            content = "Automated digital receipts with unique transaction IDs are generated instantly upon payment confirmation and accessible in the student portal."
        )
        PolicyPoint(
            number = "5.3",
            heading = "Non-Transferability & Cancellation",
            content = "Membership passes and shift slots are non-transferable to other individuals. Fee refunds for mid-month voluntary cancellations are calculated pro-rata with 7 days notice."
        )
        PolicyPoint(
            number = "5.4",
            heading = "1-Day Visitor Trial Passes",
            content = "Visitor trial passes are valid for the purchased date and shift only. Pass fees are non-refundable but can be adjusted against full admission fees."
        )
    }
}

@Composable
private fun PolicyCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            content()
        }
    }
}

@Composable
private fun PolicyPoint(
    number: String,
    heading: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = number,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = heading,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = content,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
