package com.connectchat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String?,
    val content: String?,
    val type: String,
    val replyToId: String?,
    val replyToContent: String?,
    val replyToSenderName: String?,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val createdAt: String,
    val updatedAt: String
)
