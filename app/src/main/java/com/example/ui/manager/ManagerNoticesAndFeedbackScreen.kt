package com.example.ui.manager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.FeedbackComplaintEntity
import com.example.data.local.entities.NoticeEntity
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun ManagerNoticesAndFeedbackScreen(
    notices: List<NoticeEntity>,
    feedbackList: List<FeedbackComplaintEntity>,
    onPostNotice: (String, String, String, String) -> Unit,
    onReplyFeedback: (FeedbackComplaintEntity, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } 
    var showPostNoticeModal by remember { mutableStateOf(false) }
    var activeComplaintToReply by remember { mutableStateOf<FeedbackComplaintEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showPostNoticeModal = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Campaign, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Post Notice", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Notices (${notices.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Grievances (${feedbackList.size})") })
            }

            if (selectedTab == 0) {

                if (notices.isEmpty()) {
                    EmptyPlaceholder(
                        title = "No Active Notices",
                        description = "Broadcast announcements, holiday schedules, and rule updates to students.",
                        icon = Icons.Default.Campaign,
                        actionLabel = "Post Announcement",
                        onAction = { showPostNoticeModal = true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(notices, key = { it.id }) { notice ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(notice.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (notice.priority == "URGENT") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = notice.priority,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (notice.priority == "URGENT") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(notice.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(notice.category, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                                        Text(notice.date, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {

                if (feedbackList.isEmpty()) {
                    EmptyPlaceholder(
                        title = "No Student Complaints",
                        description = "All library facilities (AC, WiFi, silence) are functioning smoothly.",
                        icon = Icons.Default.ThumbUp
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(feedbackList, key = { it.id }) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeComplaintToReply = item }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(item.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                text = "By ${item.studentName} (Seat: ${item.seatNumber.ifEmpty { "N/A" }}) • ${item.type}",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        StatusBadge(status = item.status)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(item.message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    if (item.reply.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Manager Response:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Text(item.reply, fontSize = 14.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Text("Tap to Reply / Update Status →", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPostNoticeModal) {
        PostNoticeDialog(
            onClose = { showPostNoticeModal = false },
            onConfirm = { title, content, cat, prio ->
                onPostNotice(title, content, cat, prio)
                showPostNoticeModal = false
            }
        )
    }

    if (activeComplaintToReply != null) {
        ReplyComplaintDialog(
            complaint = activeComplaintToReply!!,
            onClose = { activeComplaintToReply = null },
            onConfirm = { reply, newStatus ->
                onReplyFeedback(activeComplaintToReply!!, reply, newStatus)
                activeComplaintToReply = null
            }
        )
    }
}

@Composable
fun PostNoticeDialog(
    onClose: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Facility Update") }
    var priority by remember { mutableStateOf("NORMAL") }

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Broadcast Notice", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Notice Content *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priority,
                        onValueChange = { priority = it },
                        label = { Text("Priority (NORMAL/URGENT)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onClose, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onConfirm(title, content, category, priority)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Publish")
                    }
                }
            }
        }
    }
}

@Composable
fun ReplyComplaintDialog(
    complaint: FeedbackComplaintEntity,
    onClose: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var replyText by remember { mutableStateOf(complaint.reply) }
    var selectedStatus by remember { mutableStateOf(complaint.status) }

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Respond to Grievance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("${complaint.studentName}: ${complaint.subject}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Response to Student") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Update Status:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { selectedStatus = "RESOLVED" },
                        label = { Text("Mark Resolved") },
                        leadingIcon = if (selectedStatus == "RESOLVED") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    AssistChip(
                        onClick = { selectedStatus = "IN_PROGRESS" },
                        label = { Text("In Progress") },
                        leadingIcon = if (selectedStatus == "IN_PROGRESS") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onClose, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(replyText, selectedStatus) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}
