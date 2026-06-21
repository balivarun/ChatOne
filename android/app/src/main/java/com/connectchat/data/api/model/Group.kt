package com.connectchat.data.api.model

enum class GroupRole { ADMIN, MEMBER }

data class Group(
    val id: String,
    val name: String,
    val description: String?,
    val avatarUrl: String?,
    val createdBy: User,
    val memberCount: Int,
    val role: GroupRole,
    val createdAt: String
)
