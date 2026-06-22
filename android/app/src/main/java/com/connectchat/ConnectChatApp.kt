package com.connectchat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.connectchat.data.service.ConnectChatFcmService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ConnectChatApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ConnectChatFcmService.CHANNEL_ID,
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New message notifications"
            enableLights(true)
            enableVibration(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
