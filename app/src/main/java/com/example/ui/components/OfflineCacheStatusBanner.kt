package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.UserBookingCacheEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Offline Cache Status Banner for Profile Screen.
 * Visualizes local Room Database persistence status and guarantees offline accessibility.
 */
@Composable
fun OfflineCacheStatusBanner(
    cachedBooking: UserBookingCacheEntity?,
    isOfflineMode: Boolean = false,
    onTriggerCheckInAnimation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (cachedBooking == null && !isOfflineMode) return

    val formattedCacheDate = remember(cachedBooking?.cachedTimestamp) {
        if (cachedBooking != null && cachedBooking.cachedTimestamp > 0) {
            SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(cachedBooking.cachedTimestamp))
        } else {
            "Synchronized"
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOfflineMode) LibDeskColors.warningSoft.copy(alpha = 0.5f) else LibDeskColors.successSoft.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            1.dp,
            if (isOfflineMode) LibDeskColors.warning.copy(alpha = 0.4f) else LibDeskColors.success.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("offline_cache_status_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            .background(if (isOfflineMode) LibDeskColors.warning else LibDeskColors.success),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOfflineMode) Icons.Default.WifiOff else Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isOfflineMode) "Offline Mode • Room DB Active" else "Room DB Offline Cache Active",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOfflineMode) LibDeskColors.warning else LibDeskColors.success
                        )
                        Text(
                            text = "Profile & Entrance Pass stored locally on device",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = (if (isOfflineMode) LibDeskColors.warning else LibDeskColors.success).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "100% Offline",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfflineMode) LibDeskColors.warning else LibDeskColors.success,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (cachedBooking != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("CACHED DESK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Desk #${cachedBooking.seatNumber.ifBlank { "Open" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("DAILY SHIFT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = cachedBooking.shiftName.ifBlank { "Full Day" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("SYNCED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formattedCacheDate,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
