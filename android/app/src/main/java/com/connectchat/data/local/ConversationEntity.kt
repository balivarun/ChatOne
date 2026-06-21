package com.connectchat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val otherUserId: String?,
    val otherUserName: String?,
    val otherUserAvatar: String?,
    val otherUserOnline: Boolean,
    val groupId: String?,
    val groupName: String?,
    val groupAvatar: String?,
    val lastMessageContent: String?,
    val lastMessageSenderId: String?,
    val unreadCount: Int,
    val isArchived: Boolean,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val updatedAt: String
)
