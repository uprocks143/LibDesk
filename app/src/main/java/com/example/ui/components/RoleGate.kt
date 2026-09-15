package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.UserRole
import com.example.ui.theme.*


object LibDeskRoles {
    const val SUPER_ADMIN = "SUPER_ADMIN"
    const val MANAGER = "MANAGER"
    const val STUDENT = "STUDENT"

    val ADMIN_ROLES = setOf(SUPER_ADMIN, MANAGER)
    val ALL_ROLES = setOf(SUPER_ADMIN, MANAGER, STUDENT)
}


fun normalizeUserRole(role: String?): String {
    return when (role?.trim()?.uppercase()) {
        "SUPER_ADMIN", "OWNER", "MASTER_ADMIN", "PLATFORM_ADMIN" -> LibDeskRoles.SUPER_ADMIN
        "MANAGER", "ADMIN", "STAFF", "OPERATOR" -> LibDeskRoles.MANAGER
        "STUDENT", "MEMBER", "USER" -> LibDeskRoles.STUDENT
        else -> LibDeskRoles.STUDENT
    }
}


@Composable
fun RoleGate(
    currentRole: String,
    modifier: Modifier = Modifier,
    requiredRole: String? = null,
    allowedRoles: Set<String> = remember(requiredRole) {
        if (requiredRole != null) setOf(requiredRole) else LibDeskRoles.ADMIN_ROLES
    },
    fallback: (@Composable () -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    onSwitchToAllowedView: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val normalizedRole = remember(currentRole) { normalizeUserRole(currentRole) }
    val normalizedAllowedRoles = remember(allowedRoles) { allowedRoles.map { normalizeUserRole(it) }.toSet() }
    val isAuthorized = normalizedRole in normalizedAllowedRoles

    if (isAuthorized) {
        Box(modifier = modifier) {
            content()
        }
    } else {
        if (fallback != null) {
            fallback()
        } else {
            AccessDeniedView(
                currentRole = currentRole,
                requiredRoles = normalizedAllowedRoles,
                onSignOut = onSignOut,
                onSwitchToAllowedView = onSwitchToAllowedView,
                modifier = modifier
            )
        }
    }
}


@Composable
fun RequireAdminRole(
    currentRole: String,
    modifier: Modifier = Modifier,
    allowSuperAdmin: Boolean = true,
    onSignOut: (() -> Unit)? = null,
    onSwitchToAllowedView: (() -> Unit)? = null,
    fallback: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val allowed = remember(allowSuperAdmin) {
        if (allowSuperAdmin) setOf(LibDeskRoles.MANAGER, LibDeskRoles.SUPER_ADMIN)
        else setOf(LibDeskRoles.MANAGER)
    }

    RoleGate(
        currentRole = currentRole,
        allowedRoles = allowed,
        fallback = fallback,
        onSignOut = onSignOut,
        onSwitchToAllowedView = onSwitchToAllowedView,
        modifier = modifier,
        content = content
    )
}


@Composable
fun RequireSuperAdminRole(
    currentRole: String,
    modifier: Modifier = Modifier,
    onSignOut: (() -> Unit)? = null,
    onSwitchToAllowedView: (() -> Unit)? = null,
    fallback: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    RoleGate(
        currentRole = currentRole,
        allowedRoles = setOf(LibDeskRoles.SUPER_ADMIN),
        fallback = fallback,
        onSignOut = onSignOut,
        onSwitchToAllowedView = onSwitchToAllowedView,
        modifier = modifier,
        content = content
    )
}


@Composable
fun AccessDeniedView(
    currentRole: String,
    requiredRoles: Set<String>,
    modifier: Modifier = Modifier,
    onSignOut: (() -> Unit)? = null,
    onSwitchToAllowedView: (() -> Unit)? = null
) {
    val normalizedRole = normalizeUserRole(currentRole)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Access Restricted",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Access Restricted",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This section contains administrative library management features. Your current Supabase account role does not have permission to view or manage this area.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Active Role:",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (normalizedRole == LibDeskRoles.STUDENT) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = normalizedRole,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (normalizedRole == LibDeskRoles.STUDENT) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Required Role:",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = requiredRoles.joinToString(" or "),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                
                if (onSwitchToAllowedView != null) {
                    Button(
                        onClick = onSwitchToAllowedView,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (normalizedRole == LibDeskRoles.STUDENT) "Go to Student Portal" else "Return to Safe Screen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                
                if (onSignOut != null) {
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign Out & Switch Account",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
