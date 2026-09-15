package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID


enum class NotificationType {
    ERROR,
    WARNING,
    SUCCESS,
    INFO,
    NETWORK_OFFLINE,
    NETWORK_ONLINE
}


data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: NotificationType = NotificationType.INFO,
    val title: String? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val durationMs: Long = 4000L
)


object SnackbarController {
    private val _notifications = MutableSharedFlow<AppNotification>(extraBufferCapacity = 10)
    val notifications: SharedFlow<AppNotification> = _notifications.asSharedFlow()

    fun sendNotification(notification: AppNotification) {
        _notifications.tryEmit(notification)
    }

    fun showError(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        sendNotification(
            AppNotification(
                message = message,
                type = NotificationType.ERROR,
                title = "Error",
                actionLabel = actionLabel,
                onAction = onAction,
                durationMs = 5000L
            )
        )
    }

    fun showWarning(message: String) {
        sendNotification(
            AppNotification(
                message = message,
                type = NotificationType.WARNING,
                title = "Notice",
                durationMs = 3500L
            )
        )
    }

    fun showSuccess(message: String) {
        sendNotification(
            AppNotification(
                message = message,
                type = NotificationType.SUCCESS,
                title = "Success",
                durationMs = 3000L
            )
        )
    }

    fun showInfo(message: String) {
        sendNotification(
            AppNotification(
                message = message,
                type = NotificationType.INFO,
                durationMs = 3000L
            )
        )
    }

    fun showNetworkStatus(isOnline: Boolean) {
        if (isOnline) {
            sendNotification(
                AppNotification(
                    message = "Internet connection restored. Cloud sync active.",
                    type = NotificationType.NETWORK_ONLINE,
                    title = "Back Online",
                    durationMs = 2500L
                )
            )
        } else {
            sendNotification(
                AppNotification(
                    message = "No internet connection. LibDesk running in offline Room DB mode.",
                    type = NotificationType.NETWORK_OFFLINE,
                    title = "Offline Mode",
                    durationMs = 4500L
                )
            )
        }
    }
}


@Composable
fun GlobalErrorBoundary(
    hasError: Boolean = false,
    errorMessage: String? = null,
    errorDetails: String? = null,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var caughtException by remember { mutableStateOf<Throwable?>(null) }
    var showDetails by remember { mutableStateOf(false) }

    val isErrorActive = hasError || caughtException != null
    val displayMessage = errorMessage ?: caughtException?.localizedMessage ?: "An unexpected error occurred while loading this view."
    val displayDetails = errorDetails ?: caughtException?.stackTraceToString()

    if (isErrorActive) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReportProblem,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Something went wrong",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = displayMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )

                    
                    if (!displayDetails.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showDetails = !showDetails },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Technical Diagnostics",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Icon(
                                        imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                AnimatedVisibility(visible = showDetails) {
                                    Text(
                                        text = displayDetails.take(500),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                caughtException = null
                                onRetry()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Action")
                        }

                        Button(
                            onClick = {
                                caughtException = null
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    } else {
        content()
    }
}


@Composable
fun GlobalSnackbarHost(
    modifier: Modifier = Modifier,
    isOnline: Boolean = true
) {
    var activeNotification by remember { mutableStateOf<AppNotification?>(null) }

    LaunchedEffect(Unit) {
        SnackbarController.notifications.collect { notification ->
            activeNotification = notification
            delay(notification.durationMs)
            if (activeNotification?.id == notification.id) {
                activeNotification = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = activeNotification != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            activeNotification?.let { notif ->
                NotificationBannerItem(
                    notification = notif,
                    onDismiss = { activeNotification = null }
                )
            }
        }
    }
}

@Composable
private fun NotificationBannerItem(
    notification: AppNotification,
    onDismiss: () -> Unit
) {
    val (bgColor, contentColor, icon) = when (notification.type) {
        NotificationType.ERROR -> Triple(MaterialTheme.colorScheme.error, Color.White, Icons.Default.ErrorOutline)
        NotificationType.WARNING -> Triple(LibDeskColors.warning, Color.White, Icons.Default.LibDeskColors.warning)
        NotificationType.SUCCESS -> Triple(LibDeskColors.success, Color.White, Icons.Default.CheckCircleOutline)
        NotificationType.NETWORK_OFFLINE -> Triple(MaterialTheme.colorScheme.error, Color.White, Icons.Default.WifiOff)
        NotificationType.NETWORK_ONLINE -> Triple(LibDeskColors.success, Color.White, Icons.Default.Wifi)
        NotificationType.INFO -> Triple(MaterialTheme.colorScheme.onPrimaryContainer, Color.White, Icons.Default.Info)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                if (!notification.title.isNullOrBlank()) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentColor.copy(alpha = 0.9f)
                        )
                    )
                }
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = contentColor,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!notification.actionLabel.isNullOrBlank()) {
                TextButton(
                    onClick = {
                        notification.onAction?.invoke()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                ) {
                    Text(
                        text = notification.actionLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
