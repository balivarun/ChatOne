package com.connectchat.data.websocket

import com.connectchat.data.api.model.Message
import com.connectchat.data.api.model.Notification

sealed class WebSocketEvent {
    data class NewMessage(val message: Message) : WebSocketEvent()
    data class MessageEdited(val message: Message) : WebSocketEvent()
    data class MessageDeleted(val messageId: String, val conversationId: String) : WebSocketEvent()
    data class ReadReceipt(val messageId: String, val userId: String) : WebSocketEvent()
    data class Typing(val conversationId: String, val userId: String, val isTyping: Boolean) : WebSocketEvent()
    data class UserOnlineStatus(val userId: String, val isOnline: Boolean) : WebSocketEvent()
    data class NewNotification(val notification: Notification) : WebSocketEvent()
}
