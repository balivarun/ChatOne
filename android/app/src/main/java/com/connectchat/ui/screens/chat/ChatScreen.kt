package com.connectchat.ui.screens.chat

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.connectchat.ui.components.MessageBubble
import com.connectchat.ui.components.TypingIndicator
import com.connectchat.ui.components.UserAvatar
import com.connectchat.ui.theme.Accent
import com.connectchat.ui.theme.ChatBgLight
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    onStartCall: (callType: String) -> Unit = {},
    onGroupInfoClick: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val lazyPagingMessages = viewModel.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var messageInput by remember { mutableStateOf("") }

    // Camera photo URI
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Gallery / file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = uri.lastPathSegment ?: "file_${System.currentTimeMillis()}"
        viewModel.sendMessageWithAttachment(uri, fileName, mimeType)
    }

    // Camera capture — MIME type hardcoded to image/jpeg because FileProvider URIs
    // return null from ContentResolver.getType()
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraPhotoUri?.let { uri ->
                viewModel.sendMessageWithAttachment(uri, "photo_${System.currentTimeMillis()}.jpg", "image/jpeg")
            }
        }
    }

    // Camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val photoFile = File(context.cacheDir.also { File(it, "camera").mkdirs() }, "camera/photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val showScrollToBottom by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(lazyPagingMessages.itemCount) {
        if (listState.firstVisibleItemIndex <= 1 && lazyPagingMessages.itemCount > 0) {
            listState.animateScrollToItem(0)
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
    val lastSeen = conversation?.otherUser?.lastSeen
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
                            val subtitle = when {
                                viewModel.typingUsers.isNotEmpty() -> "typing..."
                                isOnline -> "Online"
                                lastSeen != null -> formatLastSeen(lastSeen)
                                groupId != null -> null
                                else -> null
                            }
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
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
                        IconButton(onClick = {
                            viewModel.startCall("voice")
                            onStartCall("voice")
                        }) {
                            Icon(Icons.Default.Phone, contentDescription = "Voice call", tint = Color.White)
                        }
                        IconButton(onClick = {
                            viewModel.startCall("video")
                            onStartCall("video")
                        }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video call", tint = Color.White)
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
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
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

                if (showScrollToBottom) {
                    FloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(0) } },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 8.dp)
                            .size(40.dp),
                        containerColor = Accent,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "Scroll to bottom",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (viewModel.typingUsers.isNotEmpty()) {
                TypingIndicator(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) }) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
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
                IconButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    enabled = !viewModel.isUploading
                ) {
                    if (viewModel.isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Accent)
                    } else {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "Attach",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
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
                                    scope.launch { listState.animateScrollToItem(0) }
                                }
                                messageInput = ""
                                viewModel.sendTyping(false)
                            }
                        },
                        enabled = !viewModel.isSending
                    ) {
                        if (viewModel.isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Accent
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Accent)
                        }
                    }
                }
            }
        }
    }
}

private fun formatLastSeen(isoString: String): String {
    return runCatching {
        val instant = Instant.parse(isoString)
        val now = Instant.now()
        val minutesAgo = ChronoUnit.MINUTES.between(instant, now)
        val hoursAgo = ChronoUnit.HOURS.between(instant, now)
        val daysAgo = ChronoUnit.DAYS.between(
            instant.atZone(ZoneId.systemDefault()).toLocalDate(),
            now.atZone(ZoneId.systemDefault()).toLocalDate()
        )
        when {
            minutesAgo < 1 -> "last seen just now"
            minutesAgo < 60 -> "last seen $minutesAgo min ago"
            hoursAgo < 24 -> "last seen ${DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(instant)}"
            daysAgo == 1L -> "last seen yesterday"
            else -> "last seen ${DateTimeFormatter.ofPattern("dd MMM").withZone(ZoneId.systemDefault()).format(instant)}"
        }
    }.getOrDefault("")
}
