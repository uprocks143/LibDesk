package com.example.data.remote

import android.util.Log
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.repository.LibDeskRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.util.UUID

/**
 * Supabase Realtime & Polling synchronization manager for direct student-librarian messaging,
 * inspired by Signal's fast, private, and resilient delivery architecture.
 */
object SupabaseRealtimeChatManager {
    private const val TAG = "SupabaseRealtimeChat"

    private val _isRealtimeConnected = MutableStateFlow(false)
    val isRealtimeConnected = _isRealtimeConnected.asStateFlow()

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Starts active real-time listening and polling for a library's chat messages.
     * When active, updates repository chat messages every 3 seconds if remote records exist.
     */
    fun startRealtimeSession(libraryId: String, repository: LibDeskRepository) {
        if (libraryId.isBlank()) return
        stopRealtimeSession()

        _isRealtimeConnected.value = SupabaseClient.isConfigured()

        pollingJob = scope.launch {
            // Immediate sync first
            syncMessagesFromSupabase(libraryId, repository)

            while (isActive) {
                delay(3500)
                if (SupabaseClient.isConfigured()) {
                    syncMessagesFromSupabase(libraryId, repository)
                }
            }
        }
    }

    /**
     * Stops the real-time polling session when the chat screen is closed to save battery and network.
     */
    fun stopRealtimeSession() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Synchronizes latest chat messages from Supabase REST/Realtime table into the repository.
     */
    suspend fun syncMessagesFromSupabase(libraryId: String, repository: LibDeskRepository) = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured() || libraryId.isBlank()) return@withContext

        try {
            val (success, array) = SupabaseClient.queryTable("chat_messages?libraryId=eq.$libraryId&order=timestamp.asc")
            if (success && array != null && array.length() > 0) {
                val list = mutableListOf<ChatMessageEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ChatMessageEntity(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            libraryId = obj.optString("libraryId", libraryId),
                            studentId = obj.optString("studentId", ""),
                            studentName = obj.optString("studentName", ""),
                            senderRole = obj.optString("senderRole", "STUDENT"),
                            senderName = obj.optString("senderName", ""),
                            message = obj.optString("message", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            timeFormatted = obj.optString("timeFormatted", ""),
                            dateFormatted = obj.optString("dateFormatted", ""),
                            status = obj.optString("status", "DELIVERED"),
                            isRead = obj.optBoolean("isRead", false)
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    // Merge with local list preserving any locally optimistic messages
                    val existing = repository.chatMessages.value
                    val mergedMap = existing.associateBy { it.id }.toMutableMap()
                    list.forEach { remoteMsg ->
                        mergedMap[remoteMsg.id] = remoteMsg
                    }
                    val sorted = mergedMap.values.sortedBy { it.timestamp }
                    val currentList = repository.chatMessages.value
                    if (sorted != currentList) {
                        repository.setChatMessages(sorted)
                    }
                }
                _isRealtimeConnected.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing chat messages via Supabase Realtime", e)
        }
    }
}
