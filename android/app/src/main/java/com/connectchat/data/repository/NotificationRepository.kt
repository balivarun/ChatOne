package com.connectchat.data.repository

import com.connectchat.data.api.ApiService
import com.connectchat.data.api.model.Notification
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getNotifications(page: Int = 0, size: Int = 20): Result<List<Notification>> = runCatching {
        val response = apiService.getNotifications(page, size)
        response.data?.content ?: emptyList()
    }

    suspend fun getUnreadCount(): Result<Long> = runCatching {
        val response = apiService.getUnreadCount()
        response.data?.get("count") ?: 0L
    }

    suspend fun markAllRead(): Result<Unit> = runCatching {
        apiService.markAllNotificationsRead()
    }
}
