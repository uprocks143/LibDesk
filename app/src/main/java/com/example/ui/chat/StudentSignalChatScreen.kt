package com.example.ui.chat

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
 * Signal-inspired Real-time Messaging Interface for Students to communicate directly with the Librarian.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSignalChatScreen(
    student: StudentEntity,
    library: LibraryEntity?,
    viewModel: LibDeskViewModel,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    val allMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isRealtimeLive by SupabaseRealtimeChatManager.isRealtimeConnected.collectAsStateWithLifecycle()

    // Filter messages for this specific student
    val studentMessages = remember(allMessages, student.id) {
        allMessages.filter { it.studentId == student.id }
            .sortedBy { it.timestamp }
    }

    // Auto-start Supabase Realtime session and mark unread librarian messages as read
    DisposableEffect(student.id) {
        viewModel.startRealtimeChat()
        viewModel.markChatAsRead(student.id, readerRole = "STUDENT")
        onDispose {
            viewModel.stopRealtimeChat()
        }
    }

    LaunchedEffect(studentMessages.size) {
        if (studentMessages.isNotEmpty()) {
            listState.animateScrollToItem(studentMessages.size - 1)
            viewModel.markChatAsRead(student.id, readerRole = "STUDENT")
        }
    }

    val quickQuestions = remember {
        listOf(
            "📶 What is the Wi-Fi password?",
            "🤫 Please request silence in Hall 1",
            "❄️ Could you please adjust AC temp?",
            "📚 Can I renew my issued book?",
            "🕒 Can I extend my shift today?",
            "💳 Please check my payment receipt"
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
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = library?.name?.ifBlank { "Librarian Helpdesk" } ?: "Librarian Helpdesk",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Desk Live • Instant Response",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF10B981),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    // Direct WhatsApp Fallback Action
                    val librarianPhone = library?.ownerWhatsApp?.takeIf { it.isNotBlank() }
                        ?: library?.phone?.takeIf { it.isNotBlank() }
                        ?: ""

                    if (librarianPhone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                WhatsAppAlertUtils.openWhatsApp(
                                    context,
                                    librarianPhone,
                                    "Hello Librarian, this is ${student.fullName} (Seat #${student.seatNumber.ifBlank { "Unassigned" }}). I am messaging via LibDesk App."
                                )
                            },
                            modifier = Modifier.testTag("whatsapp_fallback_button")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp_real),
                                contentDescription = "WhatsApp Fallback",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.refreshChatMessages() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Realtime",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                        senderRole = "STUDENT",
                        senderName = student.fullName,
                        messageText = sending
                    )
                },
                quickSuggestions = quickQuestions,
                onQuickSuggestionClick = { prompt ->
                    inputText = prompt
                },
                onAttachmentClick = { showAttachmentSheet = true },
                modifier = Modifier.testTag("student_signal_input_bar")
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Direct Line with Librarian Desk",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ask for Wi-Fi credentials, AC comfort, quiet zone alerts, book renewals, or any help. Your messages are synced in real time via Supabase.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
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
                        val isOutgoing = msg.senderRole == "STUDENT"
                        SignalMessageBubble(
                            message = msg,
                            isOutgoing = isOutgoing
                        )
                    }
                }
            }
        }
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Quick Desk Actions & Inquiries",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(14.dp))

                val quickActions = listOf(
                    Triple("📶 Wi-Fi Password Request", "Request latest study hall Wi-Fi SSID and passkey", Icons.Default.Wifi),
                    Triple("🤫 Silent Zone Noise Alert", "Notify librarian about noise disturbance nearby", Icons.Default.VolumeMute),
                    Triple("❄️ AC / Temperature Request", "Request temperature adjustment in study zone", Icons.Default.AcUnit),
                    Triple("🕒 Shift Timing Inquiry", "Ask regarding timing or extension of your batch", Icons.Default.Schedule),
                    Triple("📚 Book Renewal Inquiry", "Ask librarian to extend or return current physical book", Icons.Default.MenuBook)
                )

                quickActions.forEach { (title, subtitle, icon) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                showAttachmentSheet = false
                                inputText = title
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
