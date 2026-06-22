package com.connectchat.data.api.model

import com.google.gson.annotations.SerializedName

enum class ConversationType { DIRECT, GROUP }

data class Conversation(
    val id: String,
    val type: ConversationType,
    val otherUser: User?,
    val group: Group?,
    val lastMessage: Message?,
    val unreadCount: Int,
    @SerializedName("archived") val isArchived: Boolean,
    @SerializedName("pinned") val isPinned: Boolean,
    @SerializedName("muted") val isMuted: Boolean,
    val updatedAt: String
)
