package com.connectchat.data.api.model

enum class NotificationType { NEW_MESSAGE, GROUP_INVITE, GROUP_MESSAGE, MEMBER_ADDED, MEMBER_REMOVED }

data class Notification(
    val id: String,
    val sender: User?,
    val type: NotificationType,
    val title: String?,
    val body: String?,
    val isRead: Boolean,
    val referenceId: String?,
    val createdAt: String
)
