package com.example.ui.student

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LibDeskColors
import com.example.viewmodel.LibDeskViewModel

@Composable
fun StudentStudyTrackerScreen(
    viewModel: LibDeskViewModel,
    modifier: Modifier = Modifier
) {
    val streak by viewModel.currentStudentStreak.collectAsState()
    val secondsLeft by viewModel.pomodoroSecondsLeft.collectAsState()
    val isRunning by viewModel.isPomodoroRunning.collectAsState()
    val isBreak by viewModel.isPomodoroBreak.collectAsState()
    val sessionDuration by viewModel.pomodoroSessionMinutes.collectAsState()

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val totalSeconds = if (isBreak) 5 * 60 else sessionDuration * 60
    val progress = 1f - (secondsLeft.toFloat() / totalSeconds.toFloat())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Study Streak Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "🔥 ${streak?.streakDays ?: 1} Days",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "ACTIVE STREAK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Total Study Time: ${(streak?.totalStudyMinutesAllTime ?: 0) / 60} hrs ${(streak?.totalStudyMinutesAllTime ?: 0) % 60} mins",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Pomodoro Focus Timer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isBreak) "☕ Short Break Mode" else "🎯 Deep Focus Study",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isBreak) LibDeskColors.success else MaterialTheme.colorScheme.primary
                    )

                    // Switch 25 min / 45 min presets
                    if (!isRunning) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(25, 45, 60).forEach { dur ->
                                FilterChip(
                                    selected = sessionDuration == dur && !isBreak,
                                    onClick = { viewModel.setPomodoroDuration(dur) },
                                    label = { Text("${dur}m", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Circular Progress Box
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(190.dp),
                        strokeWidth = 10.dp,
                        color = if (isBreak) LibDeskColors.success else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            timeFormatted,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (isRunning) "KEEP STUDYING 📚" else "PAUSED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Timer Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.resetPomodoro() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }

                    Button(
                        onClick = {
                            if (isRunning) viewModel.pausePomodoro() else viewModel.startPomodoro()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pomodoro_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isRunning) "Pause Session" else "Start Focus Timer",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Milestones & Badges Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Study Badges & Achievements", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                val earnedBadges = streak?.unlockedBadges?.split(",") ?: listOf("ROOKIE_SCHOLAR")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StudyBadgeItem(
                        icon = "🥉",
                        title = "Rookie",
                        desc = "Day 1 Goal",
                        unlocked = earnedBadges.contains("ROOKIE_SCHOLAR"),
                        modifier = Modifier.weight(1f)
                    )
                    StudyBadgeItem(
                        icon = "🔥",
                        title = "3-Day Fire",
                        desc = "Consistency",
                        unlocked = earnedBadges.contains("STREAK_3") || (streak?.streakDays ?: 0) >= 3,
                        modifier = Modifier.weight(1f)
                    )
                    StudyBadgeItem(
                        icon = "⚡",
                        title = "7-Day Pro",
                        desc = "1-Week Habit",
                        unlocked = earnedBadges.contains("STREAK_7_FIRE") || (streak?.streakDays ?: 0) >= 7,
                        modifier = Modifier.weight(1f)
                    )
                    StudyBadgeItem(
                        icon = "👑",
                        title = "50h Club",
                        desc = "Scholar",
                        unlocked = earnedBadges.contains("HOURS_50_CENTURY") || ((streak?.totalStudyMinutesAllTime ?: 0) >= 3000),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StudyBadgeItem(
    icon: String,
    title: String,
    desc: String,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = if (unlocked) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.5.dp, if (unlocked) Color(0xFFF59E0B) else Color.Transparent),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (unlocked) icon else "🔒",
                    fontSize = 20.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            desc,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
