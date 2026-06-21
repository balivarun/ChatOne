package com.connectchat.data.api.model

data class MobileAuthRequest(val idToken: String)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val user: User
)

data class RefreshTokenRequest(val refreshToken: String)

data class SendMessageRequest(
    val conversationId: String,
    val content: String?,
    val type: String = "TEXT",
    val replyToId: String? = null
)

data class EditMessageRequest(val content: String)

data class ForwardMessageRequest(val conversationId: String)

data class MarkReadRequest(val messageIds: List<String>)

data class CreateConversationRequest(val participantId: String)

data class CreateGroupRequest(
    val name: String,
    val description: String?,
    val memberIds: List<String>
)

data class UpdateGroupRequest(val name: String?, val description: String?)

data class AddMembersRequest(val userIds: List<String>)

data class UpdateProfileRequest(val displayName: String?, val bio: String?)
