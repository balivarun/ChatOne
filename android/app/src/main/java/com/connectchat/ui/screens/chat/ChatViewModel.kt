package com.connectchat.ui.screens.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.connectchat.BuildConfig
import com.connectchat.data.api.model.Conversation
import com.connectchat.data.api.model.Message
import com.connectchat.data.api.model.SendMessageRequest
import com.connectchat.data.local.MessageEntity
import com.connectchat.data.preferences.UserPreferences
import com.connectchat.data.repository.ConversationRepository
import com.connectchat.data.repository.MessageRepository
import com.connectchat.data.repository.toEntity
import com.connectchat.data.websocket.StompClient
import com.connectchat.data.websocket.StompFrame
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val userPreferences: UserPreferences,
    private val stompClient: StompClient,
    private val gson: Gson,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    val messages: Flow<PagingData<MessageEntity>> = messageRepository
        .getMessagesPaged(conversationId)
        .cachedIn(viewModelScope)

    var conversation by mutableStateOf<Conversation?>(null)
    var replyToMessage by mutableStateOf<Message?>(null)
    var editingMessage by mutableStateOf<Message?>(null)
    var typingUsers by mutableStateOf<Set<String>>(emptySet())
    var isSending by mutableStateOf(false)
    var isUploading by mutableStateOf(false)
    var currentUserId by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)

    private var typingJob: Job? = null

    init {
        viewModelScope.launch {
            currentUserId = userPreferences.userId.first() ?: ""
            messageRepository.loadMessages(conversationId)
            conversationRepository.getConversation(conversationId).onSuccess {
                conversation = it
            }
            val token = userPreferences.accessToken.first()
            if (token != null && !stompClient.isConnected()) {
                stompClient.connect(BuildConfig.BASE_URL, token)
            }
            stompClient.subscribe("/topic/conversation/$conversationId")
            stompClient.subscribe("/topic/conversation/$conversationId/events")
            stompClient.subscribe("/user/queue/messages")
            stompClient.subscribe("/user/queue/typing")
            observeFrames()
        }
    }

    private fun observeFrames() {
        viewModelScope.launch {
            stompClient.frames.collect { frame ->
                when (frame) {
                    is StompFrame.Message -> handleIncomingFrame(frame)
                    else -> Unit
                }
            }
        }
    }

    private suspend fun handleIncomingFrame(frame: StompFrame.Message) {
        when {
            frame.destination == "/topic/conversation/$conversationId" ||
            frame.destination.contains("/queue/messages") -> {
                runCatching {
                    val message = gson.fromJson(frame.body, Message::class.java)
                    if (message.conversationId == conversationId) {
                        messageRepository.insertMessage(message.toEntity())
                        conversationRepository.clearUnread(conversationId)
                    }
                }
            }
            frame.destination == "/topic/conversation/$conversationId/events" -> {
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    val event = gson.fromJson(frame.body, Map::class.java) as Map<String, Any>
                    when (event["type"] as? String) {
                        "MESSAGE_READ" -> {
                            @Suppress("UNCHECKED_CAST")
                            val data = event["data"] as? Map<String, Any> ?: return@runCatching
                            val messageId = data["messageId"]?.toString() ?: return@runCatching
                            messageRepository.markMessageRead(messageId)
                        }
                        "MESSAGE_EDITED" -> {
                            @Suppress("UNCHECKED_CAST")
                            val data = event["data"] as? Map<String, Any> ?: return@runCatching
                            val id = data["id"]?.toString() ?: return@runCatching
                            val content = data["content"] as? String ?: return@runCatching
                            messageRepository.updateMessage(id, content)
                        }
                        "MESSAGE_DELETED" -> {
                            @Suppress("UNCHECKED_CAST")
                            val data = event["data"] as? Map<String, Any> ?: return@runCatching
                            val messageId = data["messageId"]?.toString() ?: return@runCatching
                            messageRepository.deleteMessageLocally(messageId)
                        }
                    }
                }
            }
            frame.destination.contains("/queue/typing") -> {
                runCatching {
                    val typingData = gson.fromJson(frame.body, Map::class.java)
                    val userId = typingData["userId"] as? String ?: return@runCatching
                    val convId = typingData["conversationId"] as? String ?: return@runCatching
                    val isTyping = typingData["isTyping"] as? Boolean ?: false
                    if (convId == conversationId && userId != currentUserId) {
                        typingUsers = if (isTyping) {
                            typingUsers + userId
                        } else {
                            typingUsers - userId
                        }
                    }
                }
            }
        }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            isSending = true
            messageRepository.sendMessage(
                SendMessageRequest(
                    conversationId = conversationId,
                    content = content,
                    replyToId = replyToMessage?.id
                )
            ).onFailure {
                errorMessage = it.message ?: "Failed to send message"
            }
            replyToMessage = null
            isSending = false
        }
    }

    fun editMessage(newContent: String) {
        val editing = editingMessage ?: return
        viewModelScope.launch {
            messageRepository.editMessage(editing.id, newContent).onFailure {
                errorMessage = it.message ?: "Failed to edit message"
            }
            editingMessage = null
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId).onFailure {
                errorMessage = it.message ?: "Failed to delete message"
            }
        }
    }

    fun forwardMessage(messageId: String, targetConversationId: String) {
        viewModelScope.launch {
            messageRepository.forwardMessage(messageId, targetConversationId).onFailure {
                errorMessage = it.message ?: "Failed to forward message"
            }
        }
    }

    fun sendMessageWithAttachment(uri: Uri, fileName: String, mimeType: String) {
        viewModelScope.launch {
            isUploading = true
            val bytes = withContext(Dispatchers.IO) {
                val raw = appContext.contentResolver.openInputStream(uri)?.readBytes() ?: ByteArray(0)
                if (mimeType.startsWith("image/") && raw.isNotEmpty()) {
                    runCatching {
                        val bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.size)
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
                        out.toByteArray()
                    }.getOrDefault(raw)
                } else raw
            }
            if (bytes.isEmpty()) {
                errorMessage = "Failed to read file"
                isUploading = false
                return@launch
            }
            messageRepository.uploadFile(bytes, fileName, mimeType).onSuccess { (url, type, size) ->
                val msgType = if (mimeType.startsWith("image/")) "IMAGE" else "FILE"
                messageRepository.sendMessage(
                    SendMessageRequest(
                        conversationId = conversationId,
                        content = null,
                        type = msgType,
                        replyToId = replyToMessage?.id,
                        attachmentUrl = url,
                        attachmentFileName = fileName,
                        attachmentFileType = type,
                        attachmentFileSize = size
                    )
                ).onFailure { errorMessage = it.message ?: "Failed to send" }
            }.onFailure { errorMessage = it.message ?: "Failed to upload" }
            replyToMessage = null
            isUploading = false
        }
    }

    fun sendTyping(isTyping: Boolean) {
        typingJob?.cancel()
        stompClient.send(
            "/app/chat.typing",
            gson.toJson(mapOf("conversationId" to conversationId, "isTyping" to isTyping))
        )
        if (isTyping) {
            typingJob = viewModelScope.launch {
                delay(3000)
                stompClient.send(
                    "/app/chat.typing",
                    gson.toJson(mapOf("conversationId" to conversationId, "isTyping" to false))
                )
            }
        }
    }

    fun markRead(messageIds: List<String>) {
        viewModelScope.launch {
            messageRepository.markRead(messageIds)
            conversationRepository.clearUnread(conversationId)
        }
    }

    fun setReplyTo(message: Message?) {
        replyToMessage = message
        editingMessage = null
    }

    fun setEditing(message: Message?) {
        editingMessage = message
        replyToMessage = null
    }

    fun clearError() {
        errorMessage = null
    }
}
