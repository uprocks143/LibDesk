package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ManagerSpeedDialFab(
    onQuickStudent: () -> Unit,
    onCollectFee: () -> Unit,
    onVisitorPass: () -> Unit,
    onLockerAssign: () -> Unit,
    onManualPunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 45f else 0f, label = "fab_rotation")

    Column(
        modifier = modifier.padding(bottom = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpeedDialItem(
                    label = "Quick Admission",
                    icon = Icons.Default.PersonAdd,
                    color = Color(0xFF10B981),
                    onClick = {
                        isExpanded = false
                        onQuickStudent()
                    }
                )
                SpeedDialItem(
                    label = "Collect Fee",
                    icon = Icons.Default.ReceiptLong,
                    color = Color(0xFF1E90FF),
                    onClick = {
                        isExpanded = false
                        onCollectFee()
                    }
                )
                SpeedDialItem(
                    label = "1-Day Visitor Pass",
                    icon = Icons.Default.ConfirmationNumber,
                    color = Color(0xFFF59E0B),
                    onClick = {
                        isExpanded = false
                        onVisitorPass()
                    }
                )
                SpeedDialItem(
                    label = "Locker Management",
                    icon = Icons.Default.Lock,
                    color = Color(0xFF8B5CF6),
                    onClick = {
                        isExpanded = false
                        onLockerAssign()
                    }
                )
                SpeedDialItem(
                    label = "Manual Punch",
                    icon = Icons.Default.HowToReg,
                    color = Color(0xFF06B6D4),
                    onClick = {
                        isExpanded = false
                        onManualPunch()
                    }
                )
            }
        }

        // Main Floating Action Button
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .size(56.dp)
                .testTag("manager_speed_dial_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Quick Actions",
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation)
            )
        }
    }
}

@Composable
fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(42.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}
