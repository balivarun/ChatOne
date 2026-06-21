package com.connectchat.ui.screens.chatlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connectchat.BuildConfig
import com.connectchat.data.api.model.Group
import com.connectchat.data.api.model.Message
import com.connectchat.data.local.ConversationEntity
import com.connectchat.data.preferences.UserPreferences
import com.connectchat.data.repository.ConversationRepository
import com.connectchat.data.repository.GroupRepository
import com.connectchat.data.websocket.StompClient
import com.connectchat.data.websocket.StompFrame
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val groupRepository: GroupRepository,
    private val userPreferences: UserPreferences,
    private val stompClient: StompClient,
    private val gson: Gson
) : ViewModel() {

    val conversations: StateFlow<List<ConversationEntity>> = conversationRepository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var groups by mutableStateOf<List<Group>>(emptyList())
    var isLoading by mutableStateOf(false)
    var currentUserId by mutableStateOf("")

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            isLoading = true
            currentUserId = userPreferences.userId.first() ?: ""
            val token = userPreferences.accessToken.first()
            if (token != null) {
                stompClient.connect(BuildConfig.BASE_URL, token)
                stompClient.subscribe("/user/queue/messages")
                stompClient.subscribe("/user/queue/notifications")
            }
            conversationRepository.refreshConversations()
            groupRepository.getMyGroups().onSuccess { groups = it }
            isLoading = false
            observeWebSocket()
        }
    }

    private fun observeWebSocket() {
        viewModelScope.launch {
            stompClient.frames.collect { frame ->
                if (frame is StompFrame.Message) {
                    handleIncomingFrame(frame)
                }
            }
        }
    }

    private suspend fun handleIncomingFrame(frame: StompFrame.Message) {
        when {
            frame.destination.contains("/queue/messages") || frame.destination.contains("/topic/conversation") -> {
                runCatching {
                    val message = gson.fromJson(frame.body, Message::class.java)
                    conversationRepository.incrementUnread(
                        message.conversationId,
                        message.content ?: "",
                        message.createdAt
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading = true
            conversationRepository.refreshConversations()
            groupRepository.getMyGroups().onSuccess { groups = it }
            isLoading = false
        }
    }

    fun archiveConversation(id: String) {
        viewModelScope.launch { conversationRepository.archiveConversation(id) }
    }

    fun pinConversation(id: String) {
        viewModelScope.launch { conversationRepository.pinConversation(id) }
    }

    fun muteConversation(id: String) {
        viewModelScope.launch { conversationRepository.muteConversation(id) }
    }

    override fun onCleared() {
        super.onCleared()
        stompClient.disconnect()
    }
}
