package com.connectchat.data.api.model

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val isOnline: Boolean,
    val lastSeen: String?,
    val createdAt: String
)
