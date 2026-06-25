package com.connectchat.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.connectchat.data.api.ApiService
import com.connectchat.data.api.model.EditMessageRequest
import com.connectchat.data.api.model.ForwardMessageRequest
import com.connectchat.data.api.model.MarkReadRequest
import com.connectchat.data.api.model.Message
import com.connectchat.data.api.model.MessageType
import com.connectchat.data.api.model.SendMessageRequest
import com.connectchat.data.local.AppDatabase
import com.connectchat.data.local.MessageDao
import com.connectchat.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val apiService: ApiService,
    private val messageDao: MessageDao,
    private val appDatabase: AppDatabase
) {
    fun getMessagesPaged(conversationId: String): Flow<PagingData<MessageEntity>> =
        Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
            messageDao.getMessagesPaged(conversationId)
        }.flow

    suspend fun loadMessages(conversationId: String, page: Int = 0) {
        runCatching {
            val response = apiService.getMessages(conversationId, page)
            response.data?.content?.let { messages ->
                messageDao.insertMessages(messages.map { it.toEntity() })
            }
        }
    }

    suspend fun sendMessage(request: SendMessageRequest): Result<Message> = runCatching {
        val response = apiService.sendMessage(request)
        val msg = response.data ?: throw Exception(response.message ?: "Send failed")
        messageDao.insertMessage(msg.toEntity())
        msg
    }

    suspend fun editMessage(id: String, content: String): Result<Message> = runCatching {
        val response = apiService.editMessage(id, EditMessageRequest(content))
        val msg = response.data ?: throw Exception(response.message ?: "Edit failed")
        messageDao.updateContent(id, content)
        msg
    }

    suspend fun deleteMessage(id: String): Result<Unit> = runCatching {
        apiService.deleteMessage(id)
        messageDao.markDeleted(id)
    }

    suspend fun forwardMessage(messageId: String, conversationId: String): Result<Message> = runCatching {
        val response = apiService.forwardMessage(messageId, ForwardMessageRequest(conversationId))
        response.data ?: throw Exception(response.message ?: "Forward failed")
    }

    suspend fun markRead(messageIds: List<String>): Result<Unit> = runCatching {
        apiService.markRead(MarkReadRequest(messageIds))
    }

    suspend fun uploadFile(bytes: ByteArray, fileName: String, mimeType: String): Result<Triple<String, String, Long>> = runCatching {
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
        val response = apiService.uploadFile(part)
        @Suppress("UNCHECKED_CAST")
        val data = response.data as? Map<String, Any> ?: throw Exception("Upload failed")
        val url = data["url"] as? String ?: throw Exception("No URL returned")
        val size = (data["fileSize"] as? Double)?.toLong() ?: bytes.size.toLong()
        Triple(url, mimeType, size)
    }

    suspend fun insertMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }
}

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    senderId = sender.id,
    senderName = sender.displayName,
    senderAvatar = sender.avatarUrl,
    content = content,
    type = type.name,
    attachmentUrl = attachments.firstOrNull()?.url,
    attachmentFileName = attachments.firstOrNull()?.fileName,
    replyToId = replyTo?.id,
    replyToContent = replyTo?.content,
    replyToSenderName = replyTo?.sender?.displayName,
    isEdited = isEdited,
    isDeleted = isDeleted,
    isRead = readBy.isNotEmpty(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    sender = com.connectchat.data.api.model.User(
        id = senderId,
        email = "",
        displayName = senderName,
        avatarUrl = senderAvatar,
        bio = null,
        isOnline = false,
        lastSeen = null,
        createdAt = ""
    ),
    content = content,
    type = runCatching { MessageType.valueOf(type) }.getOrDefault(MessageType.TEXT),
    replyTo = null,
    isEdited = isEdited,
    isDeleted = isDeleted,
    attachments = if (attachmentUrl != null) listOf(
        com.connectchat.data.api.model.Attachment(
            id = id,
            url = attachmentUrl,
            fileName = attachmentFileName,
            fileType = null,
            fileSize = null
        )
    ) else emptyList(),
    readBy = emptyList(),
    createdAt = createdAt,
    updatedAt = updatedAt
)
