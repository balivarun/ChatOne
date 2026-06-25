package com.connectchat.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import com.connectchat.data.local.ConversationEntity
import com.connectchat.ui.theme.Accent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: ConversationEntity,
    currentUserId: String,
    onClick: () -> Unit,
    onArchive: () -> Unit = {},
    onPin: () -> Unit = {},
    onMute: () -> Unit = {}
) {
    var showContextMenu by remember { mutableStateOf(false) }
    val displayName = when {
        conversation.type == "DIRECT" -> conversation.otherUserName ?: "Unknown"
        else -> conversation.groupName ?: "Unknown Group"
    }
    val avatarUrl = when {
        conversation.type == "DIRECT" -> conversation.otherUserAvatar
        else -> conversation.groupAvatar
    }
    val isOnline = conversation.type == "DIRECT" && conversation.otherUserOnline

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showContextMenu = true }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                avatarUrl = avatarUrl,
                displayName = displayName,
                size = 52.dp,
                showOnlineDot = isOnline
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (conversation.isPinned) {
                            Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(12.dp), tint = Accent)
                            Spacer(Modifier.width(4.dp))
                        }
                        if (conversation.isMuted) {
                            Icon(Icons.Default.VolumeOff, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        text = formatConversationTime(conversation.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (conversation.unreadCount > 0) Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isOwnMessage = conversation.lastMessageSenderId == currentUserId
                    val snippet = when (conversation.lastMessageType) {
                        "IMAGE" -> "📷 Photo"
                        "FILE"  -> "📎 File"
                        "VOICE" -> "🎤 Voice message"
                        "VIDEO" -> "🎥 Video"
                        else -> conversation.lastMessageContent ?: "No messages yet"
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOwnMessage && conversation.lastMessageType != null) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp).padding(end = 2.dp),
                                tint = Accent.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = snippet,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                    if (conversation.unreadCount > 0) {
                        Surface(
                            color = Accent,
                            shape = CircleShape
                        ) {
                            Text(
                                text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (conversation.isArchived) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }

        DropdownMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }) {
            DropdownMenuItem(
                text = { Text(if (conversation.isPinned) "Unpin" else "Pin") },
                leadingIcon = { Icon(Icons.Default.PushPin, null) },
                onClick = { showContextMenu = false; onPin() }
            )
            DropdownMenuItem(
                text = { Text(if (conversation.isMuted) "Unmute" else "Mute") },
                leadingIcon = { Icon(Icons.Default.VolumeOff, null) },
                onClick = { showContextMenu = false; onMute() }
            )
            DropdownMenuItem(
                text = { Text(if (conversation.isArchived) "Unarchive" else "Archive") },
                leadingIcon = { Icon(Icons.Default.Archive, null) },
                onClick = { showContextMenu = false; onArchive() }
            )
        }
    }
}

private fun formatConversationTime(isoString: String): String {
    return runCatching {
        val instant = Instant.parse(isoString)
        val now = Instant.now()
        val daysBetween = ChronoUnit.DAYS.between(instant.atZone(ZoneId.systemDefault()).toLocalDate(), now.atZone(ZoneId.systemDefault()).toLocalDate())
        when {
            daysBetween == 0L -> DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(instant)
            daysBetween == 1L -> "Yesterday"
            daysBetween < 7L -> DateTimeFormatter.ofPattern("EEE").withZone(ZoneId.systemDefault()).format(instant)
            else -> DateTimeFormatter.ofPattern("dd/MM/yy").withZone(ZoneId.systemDefault()).format(instant)
        }
    }.getOrDefault("--")
}
