package com.example.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ChatMessageEntity
import com.example.ui.theme.*

// Signal-inspired modern messaging colors
private val SignalBubbleOutgoing = Color(0xFF1E90FF) // Signal signature vibrant blue
private val SignalBubbleIncomingLight = Color(0xFFF1F5F9) // Slate 100
private val SignalBubbleIncomingDark = Color(0xFF1E293B)  // Slate 800
private val SignalCyanReadTick = Color(0xFF38BDF8)       // Electric Cyan Read Receipt
private val SignalLockBadgeBg = Color(0xFFEFF6FF)
private val SignalLockBadgeBorder = Color(0xFFBFDBFE)

/**
 * Signal-style security & encryption header banner.
 */
@Composable
fun SignalSecurityBanner(
    isRealtimeLive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("signal_security_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Signal Direct Library Channel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isRealtimeLive) Color(0xFF10B981) else Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRealtimeLive) "Realtime" else "Local Sync",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isRealtimeLive) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                }
                Text(
                    text = "End-to-end direct communication between student and librarian desk.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Signal-style message bubble with sharp tail corner, sender tag, timestamp, and read ticks.
 */
@Composable
fun SignalMessageBubble(
    message: ChatMessageEntity,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier
) {
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    val bubbleBg = if (isOutgoing) {
        SignalBubbleOutgoing
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val metaColor = if (isOutgoing) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp),
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.widthIn(max = 310.dp),
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            if (!isOutgoing && message.senderName.isNotBlank()) {
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                )
            }

            Surface(
                shape = bubbleShape,
                color = bubbleBg,
                shadowElevation = if (isOutgoing) 1.5.dp else 0.5.dp,
                modifier = Modifier.testTag("signal_bubble_${message.id}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = message.message,
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.timeFormatted.ifBlank { "Just now" },
                            fontSize = 10.5.sp,
                            color = metaColor
                        )

                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            SignalDeliveryTicks(
                                status = message.status,
                                isRead = message.isRead
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Signal-style ticks status icon: single tick for sent, double ticks (DoneAll) for delivered and read receipts.
 */
@Composable
fun SignalDeliveryTicks(
    status: String,
    isRead: Boolean,
    modifier: Modifier = Modifier
) {
    val tickColor = if (isRead || status == "READ") {
        SignalCyanReadTick
    } else {
        Color.White.copy(alpha = 0.85f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        when {
            isRead || status == "READ" -> {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Read",
                    tint = tickColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            status == "DELIVERED" -> {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Delivered",
                    tint = tickColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Sent",
                    tint = tickColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Signal-inspired bottom input composer with attachment drawer, quick chips, and animated send button.
 */
@Composable
fun SignalChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    quickSuggestions: List<String> = emptyList(),
    onQuickSuggestionClick: (String) -> Unit = {},
    onAttachmentClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        // Quick suggestion chips
        if (quickSuggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickSuggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onQuickSuggestionClick(suggestion) },
                        label = { Text(suggestion, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAttachmentClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .testTag("signal_attachment_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = {
                        Text(
                            "Signal message...",
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signal_input_field")
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend()
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .testTag("signal_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
