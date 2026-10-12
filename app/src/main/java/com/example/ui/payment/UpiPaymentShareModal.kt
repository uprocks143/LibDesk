package com.example.ui.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.theme.LibDeskColors
import com.example.util.WhatsAppAlertUtils
import com.example.util.payment.UpiPaymentHelper
import java.util.Locale

/**
 * 1-Click UPI Payment Link & Branded QR Code Modal.
 * Allows library managers to send instant WhatsApp dues reminders with clickable UPI pay links
 * and shareable branded QR invoice cards.
 */
@Composable
fun UpiPaymentShareModal(
    student: StudentEntity,
    library: LibraryEntity?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val libName = library?.name?.ifBlank { "LibDesk Study Library" } ?: "LibDesk Study Library"
    val upiId = library?.upiId?.takeIf { it.isNotBlank() }
        ?: library?.ownerPhone?.takeIf { it.length == 10 }?.let { "$it@upi" }
        ?: "library.desk@upi"
    val payeeName = library?.upiPayeeName?.takeIf { it.isNotBlank() } ?: libName
    val dueAmount = if (student.dueAmount > 0) student.dueAmount else 1000.0

    // Construct NPCI standard UPI Deep Link
    val upiDeepLink = remember(upiId, payeeName, dueAmount, student.fullName) {
        UpiPaymentHelper.buildUpiDeepLink(
            upiId = upiId,
            payeeName = payeeName,
            amount = dueAmount,
            note = "LibDesk Fee ${student.studentCode.ifBlank { student.fullName }}",
            transactionRef = "FEE_${student.id.take(8)}_${System.currentTimeMillis() % 100000}"
        )
    }

    // Generate QR code bitmap
    val qrBitmap: Bitmap? = remember(upiDeepLink) {
        try {
            UpiPaymentHelper.generateUpiQrBitmap(upiDeepLink, size = 520)
        } catch (_: Exception) {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 480.dp)
                .padding(vertical = 20.dp)
                .testTag("upi_payment_share_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
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
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "1-Click UPI Payment & QR",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = student.fullName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount & Due Summary Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PENDING FEE DUE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format(Locale.US, "%.0f", dueAmount)}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Seat #${student.seatNumber.ifBlank { "Desk" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "•",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Due: ${student.expiryDate.ifBlank { "Upcoming" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (student.dueAmount > 0) MaterialTheme.colorScheme.error else LibDeskColors.success
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Generated UPI QR Code Preview
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .size(220.dp)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "UPI Payment QR Code",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            )
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payee UPI ID display with Copy Action
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Library UPI ID",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = upiId,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("UPI ID", upiId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "UPI ID copied: $upiId", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy UPI ID", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Action 1: 1-Click WhatsApp Reminder with Clickable UPI Deep Link
                Button(
                    onClick = {
                        val studentPhone = student.mobile.takeIf { it.isNotBlank() } ?: ""
                        if (studentPhone.isBlank()) {
                            Toast.makeText(context, "Student mobile number is missing", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        WhatsAppAlertUtils.sendFeeDueReminder(
                            context = context,
                            studentName = student.fullName,
                            phone = studentPhone,
                            libraryName = libName,
                            dueAmount = dueAmount,
                            dueDate = student.expiryDate.ifBlank { "Upcoming Renewal" },
                            upiId = upiId
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("whatsapp_1click_upi_btn")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_whatsapp_real),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Send 1-Click WhatsApp Pay Link",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Action 2: Share Branded QR Invoice Card via WhatsApp/Share sheet
                OutlinedButton(
                    onClick = {
                        if (qrBitmap != null) {
                            val cardBitmap = try {
                                UpiPaymentHelper.generateBrandedUpiPaymentCard(
                                    context = context,
                                    upiDeepLink = upiDeepLink,
                                    planName = student.planName.ifBlank { "Monthly Membership Fee" },
                                    amount = dueAmount,
                                    upiId = upiId,
                                    payeeName = payeeName,
                                    libraryName = libName
                                )
                            } catch (_: Exception) {
                                qrBitmap
                            }
                            UpiPaymentHelper.shareUpiPaymentCard(
                                context = context,
                                bitmap = cardBitmap,
                                planName = "${student.fullName} Fee Due",
                                amount = dueAmount,
                                upiId = upiId,
                                upiDeepLink = upiDeepLink
                            )
                        } else {
                            Toast.makeText(context, "QR code generating...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("share_branded_qr_card_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share Branded QR Invoice Card",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 3: Open in External UPI App directly (GPay, PhonePe, Paytm)
                FilledTonalButton(
                    onClick = {
                        val opened = UpiPaymentHelper.openUpiPayIntent(context, upiDeepLink)
                        if (!opened) {
                            Toast.makeText(context, "No UPI payment apps found on this device", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pay Directly via UPI Apps",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
