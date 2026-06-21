package com.connectchat.data.api.model

enum class ConversationType { DIRECT, GROUP }

data class Conversation(
    val id: String,
    val type: ConversationType,
    val otherUser: User?,
    val group: Group?,
    val lastMessage: Message?,
    val unreadCount: Int,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val updatedAt: String
)
