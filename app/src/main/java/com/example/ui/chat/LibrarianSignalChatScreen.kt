package com.example.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.remote.SupabaseRealtimeChatManager
import com.example.util.WhatsAppAlertUtils
import com.example.viewmodel.LibDeskViewModel

/**
 * Signal-inspired Real-time Messaging Interface for Librarians / Managers
 * to communicate directly with students with Supabase Realtime synchronization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarianSignalChatScreen(
    library: LibraryEntity?,
    students: List<StudentEntity>,
    viewModel: LibDeskViewModel,
    initialStudentId: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedStudentId by remember { mutableStateOf(initialStudentId) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "UNREAD"

    val allMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isRealtimeLive by SupabaseRealtimeChatManager.isRealtimeConnected.collectAsStateWithLifecycle()

    // Start Realtime session
    DisposableEffect(Unit) {
        viewModel.startRealtimeChat()
        onDispose {
            viewModel.stopRealtimeChat()
        }
    }

    // Intercept back to close open thread first
    BackHandler(enabled = selectedStudentId != null) {
        selectedStudentId = null
    }

    val activeSelectedStudent = remember(selectedStudentId, students) {
        students.find { it.id == selectedStudentId }
    }

    if (activeSelectedStudent != null) {
        // Individual Conversation Thread
        LibrarianStudentThreadView(
            student = activeSelectedStudent,
            library = library,
            viewModel = viewModel,
            allMessages = allMessages,
            isRealtimeLive = isRealtimeLive,
            onBack = { selectedStudentId = null },
            modifier = modifier
        )
    } else {
        // Conversation Threads List
        LibrarianChatThreadsListView(
            students = students,
            allMessages = allMessages,
            isRealtimeLive = isRealtimeLive,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedFilter = selectedFilter,
            onSelectFilter = { selectedFilter = it },
            onSelectStudent = { studentId ->
                selectedStudentId = studentId
            },
            onRefresh = { viewModel.refreshChatMessages() },
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
    }
}

/**
 * List of student chat conversations with unread badges, last message previews, and search.
 */
@Composable
private fun LibrarianChatThreadsListView(
    students: List<StudentEntity>,
    allMessages: List<ChatMessageEntity>,
    isRealtimeLive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onSelectStudent: (String) -> Unit,
    onRefresh: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    // Group messages by studentId
    val studentThreads = remember(students, allMessages, searchQuery, selectedFilter) {
        students.map { student ->
            val msgs = allMessages.filter { it.studentId == student.id }.sortedBy { it.timestamp }
            val lastMsg = msgs.lastOrNull()
            val unreadCount = msgs.count { it.senderRole == "STUDENT" && !it.isRead }
            StudentChatThreadSummary(
                student = student,
                lastMessage = lastMsg,
                unreadCount = unreadCount,
                lastTimestamp = lastMsg?.timestamp ?: student.createdAt
            )
        }
        .filter { thread ->
            if (searchQuery.isBlank()) true
            else {
                val q = searchQuery.trim().lowercase()
                thread.student.fullName.lowercase().contains(q) ||
                thread.student.seatNumber.lowercase().contains(q) ||
                thread.student.mobile.contains(q)
            }
        }
        .filter { thread ->
            when (selectedFilter) {
                "UNREAD" -> thread.unreadCount > 0
                else -> true
            }
        }
        .sortedWith(compareByDescending<StudentChatThreadSummary> { it.unreadCount > 0 }
            .thenByDescending { it.lastTimestamp })
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Student Desk Chat",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isRealtimeLive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isRealtimeLive) Color(0xFF10B981) else Color(0xFFF59E0B))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isRealtimeLive) "Supabase Live" else "Sync",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRealtimeLive) Color(0xFF10B981) else Color(0xFFF59E0B)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Signal-inspired direct communication channel with enrolled students",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = onRefresh) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search by student name, seat #, or mobile...", fontSize = 13.5.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_search_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filters: All vs Unread
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { onSelectFilter("ALL") },
                            label = { Text("All Students (${students.size})") }
                        )
                        val totalUnread = allMessages.count { it.senderRole == "STUDENT" && !it.isRead }
                        FilterChip(
                            selected = selectedFilter == "UNREAD",
                            onClick = { onSelectFilter("UNREAD") },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Unread")
                                    if (totalUnread > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text(totalUnread.toString())
                                        }
                                    }
                                }
                            }
                        )
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            SignalSecurityBanner(isRealtimeLive = isRealtimeLive)

            if (studentThreads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Conversations Found", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("No matching students or messages in this filter.", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(studentThreads, key = { it.student.id }) { thread ->
                        val student = thread.student
                        val lastMsg = thread.lastMessage

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable { onSelectStudent(student.id) }
                                .testTag("chat_thread_${student.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar with initial
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (thread.unreadCount > 0) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = student.fullName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = if (thread.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = student.fullName,
                                            fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = lastMsg?.timeFormatted ?: "",
                                            fontSize = 11.5.sp,
                                            color = if (thread.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (student.seatNumber.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    text = "Seat #${student.seatNumber}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        Text(
                                            text = lastMsg?.message ?: "Tap to start direct library chat",
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (thread.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (thread.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (thread.unreadCount > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(thread.unreadCount.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1-on-1 thread view between Librarian and a specific student with canned replies and input bar.
 */
@Composable
private fun LibrarianStudentThreadView(
    student: StudentEntity,
    library: LibraryEntity?,
    viewModel: LibDeskViewModel,
    allMessages: List<ChatMessageEntity>,
    isRealtimeLive: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    val studentMessages = remember(allMessages, student.id) {
        allMessages.filter { it.studentId == student.id }
            .sortedBy { it.timestamp }
    }

    // Mark as read when librarian opens the view
    LaunchedEffect(student.id, studentMessages.size) {
        viewModel.markChatAsRead(student.id, readerRole = "LIBRARIAN")
        if (studentMessages.isNotEmpty()) {
            listState.animateScrollToItem(studentMessages.size - 1)
        }
    }

    val cannedReplies = remember {
        listOf(
            "✅ Noted, resolving this right away.",
            "📶 Wi-Fi: ${library?.wifiSsid ?: "LibDesk_5G"} / Pass: ${library?.wifiPassword ?: "StudyQuiet@2026"}",
            "🤫 Please maintain absolute silence in the study hall.",
            "💳 Your payment receipt has been verified!",
            "📚 Your book issue/renewal is approved."
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.fullName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Seat #${student.seatNumber.ifBlank { "Unassigned" }} • ${student.batch.ifBlank { "Regular" }}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Direct WhatsApp alert fallback for student
                    if (student.mobile.isNotBlank()) {
                        IconButton(
                            onClick = {
                                WhatsAppAlertUtils.openWhatsApp(
                                    context,
                                    student.mobile,
                                    "Hello ${student.fullName}, this is Library Management regarding your study desk inquiry."
                                )
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp_real),
                                contentDescription = "WhatsApp Student",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            SignalChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    val sending = inputText
                    inputText = ""
                    viewModel.sendChatMessage(
                        studentId = student.id,
                        studentName = student.fullName,
                        senderRole = "LIBRARIAN",
                        senderName = library?.name?.ifBlank { "Library Desk" } ?: "Library Desk",
                        messageText = sending
                    )
                },
                quickSuggestions = cannedReplies,
                onQuickSuggestionClick = { reply ->
                    inputText = reply
                },
                onAttachmentClick = {
                    inputText = "Wi-Fi: ${library?.wifiSsid ?: "LibDesk_5G"} / Pass: ${library?.wifiPassword ?: "StudyQuiet@2026"}"
                },
                modifier = Modifier.testTag("librarian_signal_input_bar")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            SignalSecurityBanner(isRealtimeLive = isRealtimeLive)

            if (studentMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Messages Yet with ${student.fullName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Send a message or select a quick canned reply below.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(studentMessages, key = { it.id }) { msg ->
                        val isOutgoing = msg.senderRole == "LIBRARIAN"
                        SignalMessageBubble(
                            message = msg,
                            isOutgoing = isOutgoing
                        )
                    }
                }
            }
        }
    }
}

private data class StudentChatThreadSummary(
    val student: StudentEntity,
    val lastMessage: ChatMessageEntity?,
    val unreadCount: Int,
    val lastTimestamp: Long
)
