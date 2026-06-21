package com.connectchat.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.connectchat.ui.components.MessageBubble
import com.connectchat.ui.components.TypingIndicator
import com.connectchat.ui.components.UserAvatar
import com.connectchat.ui.theme.Accent
import com.connectchat.ui.theme.ChatBgLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    onGroupInfoClick: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val lazyPagingMessages = viewModel.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var messageInput by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val conversation = viewModel.conversation
    val otherUserName = when {
        conversation?.otherUser != null -> conversation.otherUser.displayName
        conversation?.group != null -> conversation.group.name
        else -> "Chat"
    }
    val otherUserAvatar = when {
        conversation?.otherUser != null -> conversation.otherUser.avatarUrl
        conversation?.group != null -> conversation.group.avatarUrl
        else -> null
    }
    val isOnline = conversation?.otherUser?.isOnline ?: false
    val groupId = conversation?.group?.id

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            avatarUrl = otherUserAvatar,
                            displayName = otherUserName,
                            size = 36.dp,
                            showOnlineDot = isOnline
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(otherUserName, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            if (isOnline) {
                                Text("Online", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                            } else if (viewModel.typingUsers.isNotEmpty()) {
                                Text("typing...", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Accent),
                actions = {
                    if (groupId != null) {
                        IconButton(onClick = { onGroupInfoClick(groupId) }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Group Info", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ChatBgLight)
                .imePadding()
        ) {
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(
                    count = lazyPagingMessages.itemCount,
                    key = lazyPagingMessages.itemKey { it.id }
                ) { index ->
                    val message = lazyPagingMessages[index]
                    if (message != null) {
                        MessageBubble(
                            message = message,
                            isOwn = message.senderId == viewModel.currentUserId,
                            onReply = {
                                val msg = com.connectchat.data.api.model.Message(
                                    id = message.id,
                                    conversationId = message.conversationId,
                                    sender = com.connectchat.data.api.model.User(
                                        id = message.senderId,
                                        email = "",
                                        displayName = message.senderName,
                                        avatarUrl = message.senderAvatar,
                                        bio = null,
                                        isOnline = false,
                                        lastSeen = null,
                                        createdAt = ""
                                    ),
                                    content = message.content,
                                    type = runCatching {
                                        com.connectchat.data.api.model.MessageType.valueOf(message.type)
                                    }.getOrDefault(com.connectchat.data.api.model.MessageType.TEXT),
                                    replyTo = null,
                                    isEdited = message.isEdited,
                                    isDeleted = message.isDeleted,
                                    attachments = emptyList(),
                                    readBy = emptyList(),
                                    createdAt = message.createdAt,
                                    updatedAt = message.updatedAt
                                )
                                viewModel.setReplyTo(msg)
                            },
                            onEdit = {
                                val msg = com.connectchat.data.api.model.Message(
                                    id = message.id,
                                    conversationId = message.conversationId,
                                    sender = com.connectchat.data.api.model.User(
                                        id = message.senderId,
                                        email = "",
                                        displayName = message.senderName,
                                        avatarUrl = message.senderAvatar,
                                        bio = null,
                                        isOnline = false,
                                        lastSeen = null,
                                        createdAt = ""
                                    ),
                                    content = message.content,
                                    type = runCatching {
                                        com.connectchat.data.api.model.MessageType.valueOf(message.type)
                                    }.getOrDefault(com.connectchat.data.api.model.MessageType.TEXT),
                                    replyTo = null,
                                    isEdited = message.isEdited,
                                    isDeleted = message.isDeleted,
                                    attachments = emptyList(),
                                    readBy = emptyList(),
                                    createdAt = message.createdAt,
                                    updatedAt = message.updatedAt
                                )
                                viewModel.setEditing(msg)
                                messageInput = message.content ?: ""
                            },
                            onDelete = { viewModel.deleteMessage(message.id) },
                            onCopy = { }
                        )
                    }
                }
            }

            // Typing indicator
            if (viewModel.typingUsers.isNotEmpty()) {
                TypingIndicator(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            // Reply preview bar
            viewModel.replyToMessage?.let { replyMsg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Reply, contentDescription = null, tint = Accent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = replyMsg.sender.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = Accent
                        )
                        Text(
                            text = replyMsg.content ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = { viewModel.setReplyTo(null) }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Reply")
                    }
                }
                Divider()
            }

            // Edit mode indicator
            viewModel.editingMessage?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editing message",
                        style = MaterialTheme.typography.labelMedium,
                        color = Accent,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        viewModel.setEditing(null)
                        messageInput = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Edit")
                    }
                }
                Divider()
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { text ->
                        messageInput = text
                        viewModel.sendTyping(text.isNotEmpty())
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    maxLines = 5
                )
                IconButton(onClick = { }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                if (messageInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val text = messageInput.trim()
                            if (text.isNotEmpty()) {
                                if (viewModel.editingMessage != null) {
                                    viewModel.editMessage(text)
                                } else {
                                    viewModel.sendMessage(text)
                                }
                                messageInput = ""
                                viewModel.sendTyping(false)
                            }
                        },
                        enabled = !viewModel.isSending
                    ) {
                        if (viewModel.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Accent)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Accent)
                        }
                    }
                }
            }
        }
    }
}
