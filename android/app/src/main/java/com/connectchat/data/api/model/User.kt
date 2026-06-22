package com.connectchat.data.api.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    @SerializedName("online") val isOnline: Boolean,
    val lastSeen: String?,
    val createdAt: String
)
