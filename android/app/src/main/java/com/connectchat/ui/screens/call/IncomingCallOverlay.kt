package com.connectchat.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.connectchat.data.call.CallState
import com.connectchat.ui.components.UserAvatar

@Composable
fun IncomingCallOverlay(
    callState: CallState.Incoming,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color(0xFF1A1A2E), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (callState.callType == "video") "Incoming video call" else "Incoming voice call",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
                UserAvatar(
                    avatarUrl = callState.callerAvatar,
                    displayName = callState.callerName,
                    size = 72.dp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = callState.callerName,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Decline", color = Color.White, fontSize = 12.sp)
                    }

                    // Accept
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFF00C853), androidx.compose.foundation.shape.CircleShape)
                        ) {
                            val icon = if (callState.callType == "video") Icons.Default.Videocam else Icons.Default.Call
                            Icon(icon, contentDescription = "Accept", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Accept", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
