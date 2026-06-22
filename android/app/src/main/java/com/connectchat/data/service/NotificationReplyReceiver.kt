package com.connectchat.data.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.connectchat.data.api.model.SendMessageRequest
import com.connectchat.data.repository.MessageRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReplyReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationReplyEntryPoint {
        fun messageRepository(): MessageRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId = intent.getStringExtra("conversationId") ?: return
        val notifId = intent.getIntExtra("notifId", 0)

        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(ConnectChatFcmService.KEY_REPLY)
            ?.toString()?.trim() ?: return

        val messageRepository = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationReplyEntryPoint::class.java
        ).messageRepository()

        CoroutineScope(Dispatchers.IO).launch {
            messageRepository.sendMessage(
                SendMessageRequest(conversationId = conversationId, content = replyText)
            )
        }

        val me = Person.Builder().setName("You").build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.activeNotifications.find { it.id == notifId }?.notification
        val style = (existing?.let {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it)
        } ?: NotificationCompat.MessagingStyle(me))
            .addMessage(replyText, System.currentTimeMillis(), me)

        val updated = NotificationCompat.Builder(context, ConnectChatFcmService.CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setStyle(style)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        manager.notify(notifId, updated)
    }
}
