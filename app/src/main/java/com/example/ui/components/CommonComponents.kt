package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.LibraryEntity
import com.example.ui.theme.*

@Composable
fun libDeskHeaderBrush(): Brush {
    val isDark = LocalIsDarkTheme.current
    return if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF0B0F19),
                Color(0xFF1E293B)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFF87CEFA), // User requested #00FFFF Cyan
                Color(0xFF87CEFA)
            )
        )
    }
}

@Composable
fun LibDeskHeader(
    library: LibraryEntity?,
    currentRole: String,
    userName: String = "",
    appName: String = "LibDesk",
    notificationCount: Int = 0,
    isOnline: Boolean = true,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onSwitchRole: () -> Unit = {},
    onOpenSyncBackup: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val headerBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFF87CEFA)
    val headerText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
    val primaryTint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF0F172A)
    val circularBg = if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.06f)
    val syncText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
    val headerBorderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFF6AB6EC)

    Surface(
        color = headerBg,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = headerBorderColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(circularBg)
                    .clickable { onOpenMenu() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Side Menu",
                    tint = primaryTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .clickable { onOpenMenu() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalLibrary,
                        contentDescription = null,
                        tint = primaryTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = headerText
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFF0F172A).copy(alpha = 0.15f)),
                    modifier = Modifier.clickable { onOpenSyncBackup() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) (if (isDark) LibDeskColors.warning else MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = if (isOnline) "SYNC" else "ROOM",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = syncText,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(circularBg)
                        .clickable { onOpenNotifications() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (notificationCount > 0) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                        contentDescription = "Notifications & Alerts",
                        tint = if (notificationCount > 0) primaryTint else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )

                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(LibDeskColors.warning),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (notificationCount > 9) "9+" else "$notificationCount",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                            )
                        }
                    }
                }

                val initials = remember(userName, currentRole) {
                    if (userName.isNotBlank()) {
                        userName.trim().split(" ")
                            .filter { it.isNotEmpty() }
                            .take(2)
                            .map { it.first().uppercaseChar() }
                            .joinToString("")
                    } else if (currentRole == "MANAGER") "MG" else "ST"
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(circularBg)
                        .padding(2.dp)
                        .clickable { onOpenMenu() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF2563EB),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricKpiCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "AVAILABLE" -> Triple(LibDeskColors.successSoft, LibDeskColors.success, "Available")
        "OCCUPIED" -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "Occupied")
        "RESERVED" -> Triple(LibDeskColors.warningSoft, LibDeskColors.warning, "Reserved")
        "MAINTENANCE" -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Maintenance")
        "ACTIVE" -> Triple(LibDeskColors.successSoft, LibDeskColors.success, "Active")
        "EXPIRED" -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, "Expired")
        "CHECKED_IN" -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "In Hall")
        "CHECKED_OUT" -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Checked Out")
        "RESOLVED" -> Triple(LibDeskColors.successSoft, LibDeskColors.success, "Resolved")
        "PENDING" -> Triple(LibDeskColors.warningSoft, LibDeskColors.warning, "Pending")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, status)
    }

    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = 0.2.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EmptyPlaceholder(
    title: String,
    description: String,
    icon: ImageVector,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape
            ) {
                Text(text = actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


fun generateQrBitmap(
    content: String,
    size: Int = 300,
    foregroundColor: Int = AndroidColor.BLACK,
    backgroundColor: Int = AndroidColor.WHITE
): Bitmap {
    if (content.isBlank()) {
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            eraseColor(backgroundColor)
        }
    }
    return try {
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = 1
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (e: Exception) {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val hash = content.hashCode()
        val pattern = BooleanArray(25 * 25)
        val random = java.util.Random(hash.toLong())
        for (i in pattern.indices) {
            pattern[i] = random.nextBoolean()
        }
        for (y in 0..6) {
            for (x in 0..6) {
                val isBorder = x == 0 || x == 6 || y == 0 || y == 6
                val isCenter = x in 2..4 && y in 2..4
                val value = isBorder || isCenter
                pattern[y * 25 + x] = value
                pattern[y * 25 + (24 - x)] = value
                pattern[(24 - y) * 25 + x] = value
            }
        }
        val scale = (size / 25).coerceAtLeast(1)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val px = (x / scale).coerceIn(0, 24)
                val py = (y / scale).coerceIn(0, 24)
                val isBlack = pattern[py * 25 + px]
                bitmap.setPixel(x, y, if (isBlack) foregroundColor else backgroundColor)
            }
        }
        bitmap
    }
}

@Composable
fun QrCodeView(
    data: String,
    size: Dp = 160.dp,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(data) { generateQrBitmap(data, 300) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier.padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code: $data",
                modifier = Modifier.size(size)
            )
        }
    }
}

data class SpeedDialAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val containerColor: Color = Color.Unspecified,
    val contentColor: Color = Color.White,
    val onClick: () -> Unit
)

@Composable
fun LibDeskSpeedDialFab(
    actions: List<SpeedDialAction>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        label = "fab_rotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    actions.forEach { action ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    onDismiss()
                                    action.onClick()
                                }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 3.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = action.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            SmallFloatingActionButton(
                                onClick = {
                                    onDismiss()
                                    action.onClick()
                                },
                                containerColor = if (action.containerColor != Color.Unspecified) action.containerColor else MaterialTheme.colorScheme.primary,
                                contentColor = action.contentColor,
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onToggle,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isExpanded) "Close Actions Menu" else "Quick Actions Menu",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}


fun formatCurrency(amount: Double): String {
    return if (amount % 1.0 == 0.0) "₹${amount.toInt()}" else "₹%.2f".format(java.util.Locale.US, amount)
}

fun splitCountryCodeAndPhone(raw: String, defaultCode: String = "+91"): Pair<String, String> {
    val trimmed = raw.trim()
    if (trimmed.startsWith("+")) {
        val parts = trimmed.split(" ", limit = 2)
        if (parts.size == 2) {
            return Pair(parts[0], parts[1].trim())
        }
        if (trimmed.startsWith("+91") && trimmed.length > 3) {
            return Pair("+91", trimmed.substring(3).trim())
        }
        val match = Regex("^(\\+[0-9]{1,4})(.*)$").find(trimmed)
        if (match != null) {
            return Pair(match.groupValues[1], match.groupValues[2].trim())
        }
    }
    return Pair(defaultCode, trimmed)
}

fun combineCountryCodeAndPhone(code: String, number: String): String {
    val cleanNum = number.trim()
    val cleanCode = code.trim()
    if (cleanNum.isBlank()) return ""
    if (cleanNum.startsWith("+")) return cleanNum
    val effectiveCode = if (cleanCode.isBlank()) "+91" else if (!cleanCode.startsWith("+")) "+$cleanCode" else cleanCode
    return "$effectiveCode $cleanNum"
}

@Composable
fun CountryCodePhoneField(
    mobile: String,
    onMobileChange: (String) -> Unit,
    countryCode: String = "+91",
    onCountryCodeChange: (String) -> Unit = {},
    label: String = "Mobile Number",
    placeholder: String = "98765 43210",
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    singleLine: Boolean = true,
    enabled: Boolean = true,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = countryCode,
            onValueChange = { newCode ->
                val filtered = newCode.filter { it == '+' || it.isDigit() }
                if (filtered.length <= 5) {
                    onCountryCodeChange(if (!filtered.startsWith("+") && filtered.isNotBlank()) "+$filtered" else filtered)
                }
            },
            label = { Text("Code", fontSize = 11.sp) },
            singleLine = true,
            enabled = enabled,
            shape = shape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = colors,
            modifier = Modifier.width(86.dp)
        )

        OutlinedTextField(
            value = mobile,
            onValueChange = onMobileChange,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = singleLine,
            enabled = enabled,
            shape = shape,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = colors,
            modifier = Modifier.weight(1f)
        )
    }
}



