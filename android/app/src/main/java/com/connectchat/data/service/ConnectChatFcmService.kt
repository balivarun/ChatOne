package com.connectchat.data.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.connectchat.MainActivity
import com.connectchat.R
import com.connectchat.data.call.CallManager
import com.connectchat.data.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ConnectChatFcmService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var callManager: CallManager

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            authRepository.uploadFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"]

        if (type == "CALL_OFFER") {
            handleCallOffer(message.data)
            return
        }

        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        val conversationId = message.data["referenceId"]
        val senderName = message.data["senderName"] ?: title
        showMessageNotification(title, body, conversationId, senderName)
    }

    private fun handleCallOffer(data: Map<String, String>) {
        val callerEmail = data["callerEmail"] ?: return
        val callerName = data["callerName"] ?: "Unknown"
        val callerAvatar = data["callerAvatar"] ?: ""
        val conversationId = data["conversationId"] ?: ""
        val callType = data["callType"] ?: "video"
        val sdp = data["sdp"] ?: ""

        // Set CallManager state so MainActivity navigates to CallScreen on launch
        callManager.onIncomingOffer(
            callerId = callerEmail,
            callerName = callerName,
            callerAvatar = callerAvatar,
            callerEmail = callerEmail,
            convId = conversationId,
            callType = callType,
            sdp = sdp
        )

        showIncomingCallNotification(callerName, callerEmail, callType)
    }

    private fun showIncomingCallNotification(
        callerName: String,
        callerEmail: String,
        callType: String
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Full-screen intent — opens MainActivity which auto-navigates to CallScreen
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = ACTION_INCOMING_CALL
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, CALL_REQUEST_CODE, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline button — sends broadcast to dismiss the call
        val declineIntent = Intent(this, CallDeclineReceiver::class.java)
        val declinePendingIntent = PendingIntent.getBroadcast(
            this, CALL_REQUEST_CODE + 1, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callTypeLabel = if (callType == "video") "Incoming video call" else "Incoming voice call"

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName)
            .setContentText(callTypeLabel)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(60_000)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .setVibrate(longArrayOf(0, 500, 500, 500, 500, 500))
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Decline", declinePendingIntent)
            .addAction(R.mipmap.ic_launcher, "Answer", fullScreenPendingIntent)
            .build()

        manager.notify(CALL_NOTIFICATION_ID, notification)
    }

    private fun showMessageNotification(
        title: String,
        body: String,
        conversationId: String?,
        senderName: String
    ) {
        val convId = conversationId ?: "default"
        val notifId = convId.hashCode()

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            conversationId?.let { putExtra("conversationId", it) }
        }
        val tapPendingIntent = PendingIntent.getActivity(
            this, notifId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val replyRemoteInput = RemoteInput.Builder(KEY_REPLY).setLabel("Reply").build()
        val replyIntent = Intent(this, NotificationReplyReceiver::class.java).apply {
            putExtra("conversationId", convId)
            putExtra("notifId", notifId)
            putExtra("title", title)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            this, notifId, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val replyAction = NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher, "Reply", replyPendingIntent
        ).addRemoteInput(replyRemoteInput).build()

        val sender = Person.Builder().setName(senderName).build()
        val me = Person.Builder().setName("You").build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val existingStyle = getExistingMessagingStyle(manager, notifId)
        val style = (existingStyle ?: NotificationCompat.MessagingStyle(me))
            .addMessage(body, System.currentTimeMillis(), sender)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(style)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(replyAction)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(convId)
            .setGroupSummary(false)
            .build()

        manager.notify(notifId, notification)
    }

    private fun getExistingMessagingStyle(
        manager: NotificationManager,
        notifId: Int
    ): NotificationCompat.MessagingStyle? {
        val existing = manager.activeNotifications.find { it.id == notifId }
            ?.notification ?: return null
        return NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(existing)
    }

    companion object {
        const val CHANNEL_ID = "connectchat_messages"
        const val CALL_CHANNEL_ID = "connectchat_calls"
        const val KEY_REPLY = "key_reply"
        const val CALL_NOTIFICATION_ID = 9001
        const val CALL_REQUEST_CODE = 9002
        const val ACTION_INCOMING_CALL = "com.connectchat.ACTION_INCOMING_CALL"
    }
}
