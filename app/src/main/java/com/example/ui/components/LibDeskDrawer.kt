package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.theme.*


@Composable
fun LibDeskDrawerContent(
    library: LibraryEntity?,
    currentRole: String,
    activeStudent: StudentEntity?,
    currentUserEmail: String = "",
    currentUserName: String = "",
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    onSwitchRole: () -> Unit,
    onNavigateTab: (Int, Int) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit,
    onOpenNoticesAndHelp: () -> Unit = {},
    onOpenAttendanceHistory: () -> Unit = {},
    onOpenSyncBackup: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onShowLibraryQr: () -> Unit = {},
    onOpenSuperAdmin: () -> Unit = {},
    onOpenMySubscription: () -> Unit = {},
    isTrialActive: Boolean = false,
    trialDaysRemaining: Int = 15,
    isTrialExpired: Boolean = false,
    subscriptionPlanName: String? = null,
    onOpenProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = modifier
            .widthIn(max = 340.dp)
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            val isDark = LocalIsDarkTheme.current
            val drawerHeaderBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFF87CEFA)
            val drawerHeaderText = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF0F172A)
            val drawerHeaderSubtext = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF334155)

            Surface(
                color = drawerHeaderBg,
                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFF6AB6EC)),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color(0xFF0F172A).copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = when (currentRole) {
                                    "SUPER_ADMIN" -> "SUPER ADMIN"
                                    "MANAGER" -> "MANAGER"
                                    else -> "STUDENT"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                letterSpacing = 0.5.sp
                            )
                        }

                        IconButton(
                            onClick = onCloseDrawer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Menu",
                                tint = drawerHeaderText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onOpenProfile()
                                onCloseDrawer()
                            }
                    ) {

                        val initials = when (currentRole) {
                            "SUPER_ADMIN" -> "SA"
                            "MANAGER" -> {
                                val name = currentUserName.ifBlank { library?.ownerName ?: "Manager" }
                                name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "MG" }
                            }
                            else -> {
                                val name = currentUserName.ifBlank { activeStudent?.fullName ?: "Student" }
                                name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase().ifBlank { "ST" }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(52.dp)
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
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val displayName = when (currentRole) {
                                "SUPER_ADMIN" -> "Super Admin (SaaS Manager)"
                                "MANAGER" -> currentUserName.ifBlank { library?.ownerName ?: "Library Manager" }
                                else -> currentUserName.ifBlank { activeStudent?.fullName ?: "Student Member" }
                            }
                            val displayEmail = when (currentRole) {
                                "SUPER_ADMIN" -> currentUserEmail.ifBlank { "superadmin@libdesk.cloud" }
                                "MANAGER" -> currentUserEmail.ifBlank { library?.email ?: "" }
                                else -> currentUserEmail.ifBlank { activeStudent?.email ?: "" }
                            }

                            Text(
                                text = displayName,
                                color = drawerHeaderText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                softWrap = true,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = displayEmail,
                                color = drawerHeaderSubtext,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (currentRole == "SUPER_ADMIN") "SaaS Subscriptions Hub" else (library?.name ?: "Your Library"),
                                color = drawerHeaderSubtext,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                onOpenProfile()
                                onCloseDrawer()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = drawerHeaderText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (currentRole) {
                                    "SUPER_ADMIN" -> Icons.Default.Shield
                                    "MANAGER" -> Icons.Default.AdminPanelSettings
                                    else -> Icons.Default.School
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (currentRole) {
                                    "SUPER_ADMIN" -> "Super Admin (SaaS Manager)"
                                    "MANAGER" -> "Manager"
                                    else -> "Student"
                                },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                softWrap = true,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            RoleGate(
                currentRole = currentRole,
                allowedRoles = setOf(LibDeskRoles.SUPER_ADMIN),
                fallback = {}
            ) {
                Column {
                    Text(
                        text = "SAAS MANAGEMENT MODULES",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Subscriptions,
                        title = "Subscriptions & Libraries",
                        subtitle = "Monitor active libraries, billing & expiry",
                        badge = "SaaS",
                        onClick = {
                            onNavigateTab(0, 0)
                            onCloseDrawer()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.PriceChange,
                        title = "SaaS Plans & Pricing",
                        subtitle = "Configure tiers, monthly & annual pricing",
                        onClick = {
                            onNavigateTab(1, 0)
                            onCloseDrawer()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.TrendingUp,
                        title = "Revenue Analytics & MRR",
                        subtitle = "View network health, revenue & library stats",
                        onClick = {
                            onNavigateTab(2, 0)
                            onCloseDrawer()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Security,
                        title = "2FA & Admin Security",
                        subtitle = "Two-factor authentication & admin access",
                        onClick = {
                            onNavigateTab(3, 0)
                            onCloseDrawer()
                        }
                    )
                }
            }

            RoleGate(
                currentRole = currentRole,
                allowedRoles = setOf(LibDeskRoles.MANAGER),
                fallback = {}
            ) {
                Column {
                    Text(
                        text = "MANAGEMENT UTILITIES",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    DrawerNavItem(
                        icon = Icons.Default.WorkspacePremium,
                        title = "My Subscriptions",
                        subtitle = if (isTrialActive) "15-Day Free Trial SaaS System • $trialDaysRemaining Days Left"
                                   else if (isTrialExpired) "15-Day Free Trial Expired • Tap to Upgrade"
                                   else (subscriptionPlanName ?: "15-Day Free Trial SaaS System & Special Plans"),
                        badge = if (isTrialActive) "15-Day Trial"
                                else if (isTrialExpired) "Trial Expired"
                                else "SaaS Pro",
                        badgeColor = if (isTrialExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        onClick = {
                            onOpenMySubscription()
                            onCloseDrawer()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.FactCheck,
                        title = "Attendance Register & History",
                        subtitle = "Multi-library logs, date filters & manual punches",
                        badge = "History",
                        onClick = {
                            onOpenAttendanceHistory()
                            onCloseDrawer()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Campaign,
                        title = "Notices & Student Helpdesk",
                        subtitle = "Broadcast notices & reply to student complaints",
                        onClick = {
                            onOpenNoticesAndHelp()
                            onCloseDrawer()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.QrCode,
                        title = "Gate & Enrollment QR Poster",
                        subtitle = "Printable reception QR for self-attendance & signup",
                        badge = "Poster",
                        onClick = {
                            onShowLibraryQr()
                            onCloseDrawer()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Settings,
                        title = "Library Configuration & Rules",
                        subtitle = "Configure shifts, fee plans, seats & Wi-Fi",
                        onClick = {
                            onOpenSettings()
                            onCloseDrawer()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Outlined.CloudSync,
                        title = "Backup & Restore",
                        subtitle = "Protect your LibDesk data locally and in Google Drive",
                        onClick = {
                            onOpenSyncBackup()
                            onCloseDrawer()
                        }
                    )
                }
            }

            RoleGate(
                currentRole = currentRole,
                allowedRoles = setOf(LibDeskRoles.STUDENT),
                fallback = {}
            ) {
                Column {
                    Text(
                        text = "STUDENT SERVICES",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Person,
                        title = "Student Profile & ID",
                        subtitle = "View registration, roll number & assigned seat",
                        onClick = {
                            onOpenProfile()
                            onCloseDrawer()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Badge,
                        title = "Digital ID Card & Pass",
                        subtitle = "View digital identity card & seat pass",
                        onClick = {
                            onNavigateTab(0, 0)
                            onCloseDrawer()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.QrCodeScanner,
                        title = "Scan Gate Attendance QR",
                        subtitle = "Scan reception QR to mark self check-in",
                        badge = "Scan",
                        onClick = {
                            onOpenQrScanner()
                            onCloseDrawer()
                        }
                    )
                    DrawerNavItem(
                        icon = Icons.Default.Info,
                        title = "Library Timings & Rules",
                        subtitle = "View library shifts, Wi-Fi & silent zone rules",
                        onClick = {
                            onOpenSettings()
                            onCloseDrawer()
                        }
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            
            Text(
                text = "ACCOUNT & APPEARANCE",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDarkMode() }
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = if (isDarkMode) LibDeskColors.warning else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (isDarkMode) "Oxford Midnight (Dark)" else "Classic Oxford (Light)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDarkMode) "Tap to switch to Light" else "Tap to switch to Dark",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LibDeskColors.warning,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            DrawerNavItem(
                icon = Icons.Default.AdminPanelSettings,
                title = "SaaS Admin Portal",
                subtitle = "Restricted management interface for platform owner",
                badge = "Owner",
                badgeColor = MaterialTheme.colorScheme.primary,
                onClick = {
                    onOpenSuperAdmin()
                    onCloseDrawer()
                }
            )

            DrawerNavItem(
                icon = Icons.Default.Logout,
                title = "Sign Out",
                subtitle = "Return to login screen",
                onClick = {
                    onLogout()
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "LibDesk Cloud ERP • v2.4.0",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Realtime Supabase Cloud Active",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        softWrap = true,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (badge != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = badge,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = true,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
