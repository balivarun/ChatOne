package com.connectchat.data.api.model

enum class MessageType { TEXT, IMAGE, FILE, VOICE, SYSTEM }

data class Attachment(
    val id: String,
    val url: String,
    val fileName: String?,
    val fileType: String?,
    val fileSize: Long?
)

data class Message(
    val id: String,
    val conversationId: String,
    val sender: User,
    val content: String?,
    val type: MessageType,
    val replyTo: Message?,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val attachments: List<Attachment>,
    val readBy: List<User>,
    val createdAt: String,
    val updatedAt: String
)
