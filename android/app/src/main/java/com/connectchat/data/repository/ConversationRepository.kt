package com.connectchat.data.repository

import com.connectchat.data.api.ApiService
import com.connectchat.data.api.model.Conversation
import com.connectchat.data.api.model.ConversationType
import com.connectchat.data.api.model.CreateConversationRequest
import com.connectchat.data.local.ConversationDao
import com.connectchat.data.local.ConversationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val apiService: ApiService,
    private val conversationDao: ConversationDao
) {
    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    suspend fun refreshConversations() {
        runCatching {
            val response = apiService.getConversations()
            response.data?.let { list ->
                conversationDao.insertConversations(list.map { it.toEntity() })
            }
        }
    }

    suspend fun createConversation(participantId: String): Result<Conversation> = runCatching {
        val response = apiService.createConversation(CreateConversationRequest(participantId))
        val conv = response.data ?: throw Exception(response.message ?: "Failed to create conversation")
        conversationDao.insertConversation(conv.toEntity())
        conv
    }

    suspend fun getConversation(id: String): Result<Conversation> = runCatching {
        val response = apiService.getConversation(id)
        response.data ?: throw Exception(response.message ?: "Conversation not found")
    }

    suspend fun archiveConversation(id: String): Result<Unit> = runCatching {
        apiService.archiveConversation(id)
    }

    suspend fun pinConversation(id: String): Result<Unit> = runCatching {
        apiService.pinConversation(id)
    }

    suspend fun muteConversation(id: String): Result<Unit> = runCatching {
        apiService.muteConversation(id)
    }

    suspend fun clearUnread(id: String) {
        conversationDao.clearUnread(id)
    }

    suspend fun incrementUnread(id: String, lastMsg: String, time: String) {
        conversationDao.incrementUnread(id, lastMsg, time)
    }
}

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    type = type.name,
    otherUserId = otherUser?.id,
    otherUserName = otherUser?.displayName,
    otherUserAvatar = otherUser?.avatarUrl,
    otherUserOnline = otherUser?.isOnline ?: false,
    groupId = group?.id,
    groupName = group?.name,
    groupAvatar = group?.avatarUrl,
    lastMessageContent = lastMessage?.content,
    lastMessageType = lastMessage?.type?.name,
    lastMessageSenderId = lastMessage?.sender?.id,
    unreadCount = unreadCount,
    isArchived = isArchived,
    isPinned = isPinned,
    isMuted = isMuted,
    updatedAt = updatedAt
)

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    type = ConversationType.valueOf(type),
    otherUser = if (otherUserId != null) {
        com.connectchat.data.api.model.User(
            id = otherUserId,
            email = "",
            displayName = otherUserName ?: "",
            avatarUrl = otherUserAvatar,
            bio = null,
            isOnline = otherUserOnline,
            lastSeen = null,
            createdAt = ""
        )
    } else null,
    group = null,
    lastMessage = null,
    unreadCount = unreadCount,
    isArchived = isArchived,
    isPinned = isPinned,
    isMuted = isMuted,
    updatedAt = updatedAt
)
