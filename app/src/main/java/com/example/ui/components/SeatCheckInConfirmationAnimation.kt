package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data payload describing a confirmed seat check-in.
 */
data class SeatCheckInDetails(
    val studentName: String,
    val studentCode: String = "",
    val seatNumber: String,
    val hallName: String = "Main Study Hall",
    val shiftName: String = "Full Day Shift",
    val checkInTime: String = "",
    val mode: String = "Gate / Seat QR Verified",
    val deskAmenities: String = "AC • Power Socket • Lamp"
)

/**
 * Lottie-style vector confirmation animation when a user successfully checks into a seat.
 * Features:
 * 1. Spring-physics scale pop-in with energetic overshoot bounce
 * 2. Multi-layer concentric radiant pulse waves expanding outward
 * 3. Real-time animated vector Path drawing of the checkmark stroke
 * 4. 360-degree festive confetti starburst particles with varied angular velocities & shapes
 * 5. Staggered card cascade with seat, shift, and timestamp confirmation details
 * 6. Auto-dismiss timer progress bar with haptic feedback
 */
@Composable
fun SeatCheckInConfirmationDialog(
    details: SeatCheckInDetails,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            SeatCheckInConfirmationCard(
                details = details,
                onDismiss = onDismiss,
                modifier = modifier
                    .padding(horizontal = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Prevent dialog dismiss when clicking card body
                    )
            )
        }
    }
}

@Composable
fun SeatCheckInConfirmationCard(
    details: SeatCheckInDetails,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss countdown (8 seconds) with linear progress bar
    var progress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        val totalMs = 8000L
        val stepMs = 50L
        val steps = (totalMs / stepMs).toInt()
        for (i in steps downTo 0) {
            progress = i.toFloat() / steps
            delay(stepMs)
        }
        onDismiss()
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("seat_checkin_confirmation_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lottie-style vector animation stage
            LottieStyleCheckmarkAnimation(
                modifier = Modifier
                    .size(160.dp)
                    .testTag("lottie_checkmark_animation")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Celebratory Header
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LibDeskColors.successSoft,
                border = androidx.compose.foundation.BorderStroke(1.dp, LibDeskColors.success.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = LibDeskColors.success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "CHECK-IN SUCCESSFUL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LibDeskColors.success,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Welcome, ${details.studentName.split(" ").firstOrNull() ?: details.studentName}!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your library study session is now active.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Desk & Booking Details Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Seat Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EventSeat,
                                    contentDescription = null,
                                    tint = LibDeskColors.warningSoft,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "ASSIGNED SEAT",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (details.seatNumber.isNotBlank()) "Desk #${details.seatNumber}" else "General Study Hall Desk",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LibDeskColors.successSoft
                        ) {
                            Text(
                                text = "OCCUPIED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LibDeskColors.success,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 0.8.dp
                    )

                    // Timings & Hall Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "STUDY HALL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = details.hallName.ifBlank { "Main Study Hall" },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SHIFT & TIMING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = details.shiftName.ifBlank { "Full Day Shift" },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Check-in Time & Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Punch In: ${details.checkInTime.ifEmpty { "Just Now" }}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = details.mode,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Confirm Button
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("checkin_confirm_done_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = LibDeskColors.warningSoft
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Awesome, Go to My Desk",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auto-dismiss countdown bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Auto-closing in ${(progress * 8).toInt() + 1}s",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Pure Jetpack Compose Lottie-Style Animated Vector Checkmark with:
 * - Concentric expanding pulse wave rings
 * - Dynamic spring pop-in of center disk
 * - Real-time vector PathMeasure stroke drawing of checkmark
 * - 360-degree radiant confetti particle burst
 */
@Composable
fun LottieStyleCheckmarkAnimation(
    modifier: Modifier = Modifier
) {
    // 1. Spring scale animation for central emblem
    val scaleAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // 2. Continuous pulse ripples expanding behind the emblem
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")
    val pulseProgress1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_1"
    )
    val pulseProgress2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_2"
    )

    // 3. Checkmark drawing path progress (0f to 1f)
    val checkmarkProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(250) // Wait for emblem bounce to start
        checkmarkProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    // 4. Confetti burst animation (0f to 1f)
    val confettiProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(350)
        confettiProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    // Confetti particle specs
    val confettiParticles = remember {
        val colors = listOf(
            Color(0xFF10B981), // Emerald
            Color(0xFFD4AF37), // Burnished Gold
            Color(0xFF1E40AF), // Oxford Blue
            Color(0xFF38BDF8), // Sky
            Color(0xFFF59E0B), // Amber
            Color(0xFFEC4899), // Pink
            Color(0xFF8B5CF6)  // Purple
        )
        List(20) { index ->
            val angleRad = (index.toDouble() / 20.0) * (2 * Math.PI)
            val distanceMax = 65f + (index % 5) * 10f
            val size = 5f + (index % 3) * 2.5f
            val shapeType = index % 3 // 0: circle, 1: diamond, 2: rect
            Triple(angleRad, distanceMax, Pair(colors[index % colors.size], Pair(size, shapeType)))
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2

            // Draw Concentric Pulse Rings
            val r1 = 44f + pulseProgress1 * (maxRadius - 44f)
            val a1 = (1f - pulseProgress1).coerceIn(0f, 1f) * 0.45f
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = a1),
                radius = r1,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            val r2 = 44f + pulseProgress2 * (maxRadius - 44f)
            val a2 = (1f - pulseProgress2).coerceIn(0f, 1f) * 0.35f
            drawCircle(
                color = Color(0xFFD4AF37).copy(alpha = a2),
                radius = r2,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Draw Confetti Particles bursting outward
            val cProg = confettiProgress.value
            if (cProg > 0f) {
                confettiParticles.forEach { (angleRad, maxDist, config) ->
                    val color = config.first
                    val particleSize = config.second.first
                    val shape = config.second.second

                    // Eased distance
                    val dist = maxDist * (1f - (1f - cProg) * (1f - cProg))
                    val particleAlpha = (1f - cProg).coerceIn(0f, 1f)
                    val px = center.x + (cos(angleRad) * dist).toFloat()
                    val py = center.y + (sin(angleRad) * dist).toFloat()

                    when (shape) {
                        0 -> {
                            drawCircle(
                                color = color.copy(alpha = particleAlpha),
                                radius = particleSize,
                                center = Offset(px, py)
                            )
                        }
                        1 -> {
                            // Diamond
                            val path = Path().apply {
                                moveTo(px, py - particleSize)
                                lineTo(px + particleSize, py)
                                lineTo(px, py + particleSize)
                                lineTo(px - particleSize, py)
                                close()
                            }
                            drawPath(path, color = color.copy(alpha = particleAlpha))
                        }
                        else -> {
                            // Rotating tiny rectangle
                            rotate(degrees = cProg * 180f, pivot = Offset(px, py)) {
                                drawRect(
                                    color = color.copy(alpha = particleAlpha),
                                    topLeft = Offset(px - particleSize, py - particleSize / 2),
                                    size = androidx.compose.ui.geometry.Size(particleSize * 2, particleSize)
                                )
                            }
                        }
                    }
                }
            }

            // Draw Core Emblem Shadow & Background Circle
            val emblemRadius = 42.dp.toPx() * scaleAnim.value
            if (emblemRadius > 0) {
                // Soft glow behind emblem
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF10B981).copy(alpha = 0.4f), Color.Transparent),
                        center = center,
                        radius = emblemRadius * 1.3f
                    ),
                    radius = emblemRadius * 1.3f,
                    center = center
                )

                // Outer Gold Ring
                drawCircle(
                    color = Color(0xFFD4AF37),
                    radius = emblemRadius + 3.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Vibrant Gradient Fill
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)),
                        start = Offset(center.x - emblemRadius, center.y - emblemRadius),
                        end = Offset(center.x + emblemRadius, center.y + emblemRadius)
                    ),
                    radius = emblemRadius,
                    center = center
                )

                // Animated Path Checkmark Drawing
                val cp = checkmarkProgress.value
                if (cp > 0f) {
                    val p1 = Offset(center.x - emblemRadius * 0.42f, center.y - emblemRadius * 0.02f)
                    val p2 = Offset(center.x - emblemRadius * 0.12f, center.y + emblemRadius * 0.32f)
                    val p3 = Offset(center.x + emblemRadius * 0.42f, center.y - emblemRadius * 0.32f)

                    val fullPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                    }

                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(fullPath, false)
                    val totalLength = pathMeasure.length
                    val subPath = Path()
                    pathMeasure.getSegment(0f, totalLength * cp, subPath, true)

                    drawPath(
                        path = subPath,
                        color = Color.White,
                        style = Stroke(
                            width = 6.dp.toPx() * scaleAnim.value,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
