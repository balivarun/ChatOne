package com.connectchat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.connectchat.data.api.model.MessageType
import com.connectchat.data.local.MessageEntity
import com.connectchat.ui.theme.Accent
import com.connectchat.ui.theme.MessageInLight
import com.connectchat.ui.theme.MessageOutLight
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    isOwn: Boolean,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showContextMenu by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val maxBubbleWidth = (configuration.screenWidthDp * 0.75).dp

    if (message.isDeleted) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (isOwn) MessageOutLight.copy(alpha = 0.5f) else MessageInLight.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "This message was deleted",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic
                )
            }
        }
        return
    }

    if (showContextMenu) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = { Text("Message Actions") },
            text = {
                Column {
                    TextButton(
                        onClick = { showContextMenu = false; onReply() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reply", modifier = Modifier.weight(1f))
                    }
                    if (isOwn) {
                        TextButton(
                            onClick = { showContextMenu = false; onEdit() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Edit", modifier = Modifier.weight(1f))
                        }
                        Divider()
                        TextButton(
                            onClick = { showContextMenu = false; onDelete() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text("Delete", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (!message.content.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                showContextMenu = false
                                clipboardManager.setText(AnnotatedString(message.content))
                                onCopy()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Copy", modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextMenu = false }) { Text("Cancel") }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwn) {
            UserAvatar(
                avatarUrl = message.senderAvatar,
                displayName = message.senderName,
                size = 32.dp,
                modifier = Modifier.padding(end = 4.dp, top = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .background(
                    color = if (isOwn) MessageOutLight else MessageInLight,
                    shape = RoundedCornerShape(
                        topStart = if (isOwn) 12.dp else 4.dp,
                        topEnd = if (isOwn) 4.dp else 12.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    )
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showContextMenu = true }
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Sender name for group chats
            if (!isOwn) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Reply preview
            if (message.replyToId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                        .padding(start = 6.dp, top = 4.dp, end = 6.dp, bottom = 4.dp)
                ) {
                    Column {
                        Text(
                            text = message.replyToSenderName ?: "Unknown",
                            style = MaterialTheme.typography.labelSmall,
                            color = Accent,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = message.replyToContent ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Message content
            val msgType = runCatching { MessageType.valueOf(message.type) }.getOrDefault(MessageType.TEXT)
            when (msgType) {
                MessageType.IMAGE -> {
                    AsyncImage(
                        model = message.content,
                        contentDescription = "Image",
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                MessageType.FILE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Forward, contentDescription = "File", tint = Accent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message.content ?: "File", style = MaterialTheme.typography.bodyMedium, color = Accent)
                    }
                }
                else -> {
                    if (!message.content.isNullOrBlank()) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }
                }
            }

            // Timestamp row
            Row(
                modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (message.isEdited) {
                    Text(
                        text = "edited",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.5f),
                        fontStyle = FontStyle.Italic,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = formatMessageTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
                if (isOwn) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Read",
                        modifier = Modifier.size(14.dp),
                        tint = Accent
                    )
                }
            }
        }
    }
}

private fun formatMessageTime(isoString: String): String {
    return runCatching {
        val instant = Instant.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    }.getOrDefault("--:--")
}
