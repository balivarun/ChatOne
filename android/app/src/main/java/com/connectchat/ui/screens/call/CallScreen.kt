package com.connectchat.ui.screens.call

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.connectchat.data.call.CallState
import com.connectchat.ui.components.UserAvatar
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink

private val NoOpVideoSink = VideoSink { _: VideoFrame? -> }

@Composable
fun CallScreen(
    onCallEnded: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val callState by viewModel.callState.collectAsState()
    val remoteStream by viewModel.remoteStream.collectAsState()
    val eglBase = remember { viewModel.getEglBase() }

    LaunchedEffect(callState) {
        if (callState is CallState.Idle) onCallEnded()
    }

    when (val state = callState) {
        is CallState.Incoming -> IncomingCallView(
            callerName = state.callerName,
            callerAvatar = state.callerAvatar,
            callType = state.callType,
            viewModel = viewModel,
            eglBase = eglBase,
            onReject = { viewModel.rejectCall() }
        )
        is CallState.Outgoing -> OutgoingCallView(
            targetName = state.targetName,
            targetAvatar = state.targetAvatar,
            callType = state.callType,
            viewModel = viewModel,
            eglBase = eglBase,
            onEnd = { viewModel.endCall() }
        )
        is CallState.Active -> ActiveCallView(
            callType = state.callType,
            viewModel = viewModel,
            remoteStream = remoteStream,
            eglBase = eglBase,
            onEnd = { viewModel.endCall() }
        )
        is CallState.Idle -> Unit
    }
}

@Composable
private fun IncomingCallView(
    callerName: String,
    callerAvatar: String,
    callType: String,
    viewModel: CallViewModel,
    eglBase: EglBase,
    onReject: () -> Unit
) {
    val localSinkRef = remember { mutableStateOf<VideoSink>(NoOpVideoSink) }
    var rendererReady by remember { mutableStateOf(callType != "video") }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = if (callType == "video") "Incoming video call" else "Incoming voice call",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
            UserAvatar(avatarUrl = callerAvatar, displayName = callerName, size = 100.dp)
            Text(callerName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                CallActionButton(icon = Icons.Default.CallEnd, label = "Decline", tint = Color.Red) {
                    onReject()
                }
                CallActionButton(icon = Icons.Default.Call, label = "Accept", tint = Color(0xFF00C853)) {
                    if (rendererReady) viewModel.acceptCall(localSinkRef.value)
                }
            }
        }

        // Tiny hidden renderer to initialize EGL for video calls
        if (callType == "video") {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(1, 1)
                        init(eglBase.eglBaseContext, null)
                        localSinkRef.value = this
                        rendererReady = true
                    }
                },
                modifier = Modifier.size(1.dp)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            (localSinkRef.value as? SurfaceViewRenderer)?.release()
        }
    }
}

@Composable
private fun OutgoingCallView(
    targetName: String,
    targetAvatar: String?,
    callType: String,
    viewModel: CallViewModel,
    eglBase: EglBase,
    onEnd: () -> Unit
) {
    val localSinkRef = remember { mutableStateOf<VideoSink?>(null) }
    val mediaStarted = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        if (callType == "video") {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        init(eglBase.eglBaseContext, null)
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        setMirror(true)
                        localSinkRef.value = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { renderer ->
                    if (!mediaStarted.value && localSinkRef.value != null) {
                        mediaStarted.value = true
                        viewModel.startOutgoingMedia(renderer)
                    }
                }
            )
        } else {
            // Voice call — start media immediately with no-op sink
            LaunchedEffect(Unit) {
                if (!mediaStarted.value) {
                    mediaStarted.value = true
                    viewModel.startOutgoingMedia(NoOpVideoSink)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserAvatar(avatarUrl = targetAvatar, displayName = targetName, size = 80.dp)
            Text(targetName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Calling...", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            CallControlButton(
                icon = if (viewModel.isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                label = if (viewModel.isMicOn) "Mute" else "Unmute",
                onClick = { viewModel.toggleMic() }
            )
            CallActionButton(icon = Icons.Default.CallEnd, label = "End", tint = Color.Red) { onEnd() }
            if (callType == "video") {
                CallControlButton(
                    icon = if (viewModel.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = if (viewModel.isCameraOn) "Camera off" else "Camera on",
                    onClick = { viewModel.toggleCamera() }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { (localSinkRef.value as? SurfaceViewRenderer)?.release() }
    }
}

@Composable
private fun ActiveCallView(
    callType: String,
    viewModel: CallViewModel,
    remoteStream: org.webrtc.MediaStream?,
    eglBase: EglBase,
    onEnd: () -> Unit
) {
    val remoteSinkRef = remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    LaunchedEffect(remoteStream, remoteSinkRef.value) {
        val renderer = remoteSinkRef.value ?: return@LaunchedEffect
        remoteStream?.videoTracks?.firstOrNull()?.addSink(renderer)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (callType == "video") {
            // Remote full-screen
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        init(eglBase.eglBaseContext, null)
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                        remoteSinkRef.value = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Local preview (top-right pip)
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        init(eglBase.eglBaseContext, null)
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        setMirror(true)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(width = 100.dp, height = 140.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Call Connected", color = Color.White, fontSize = 20.sp)
                }
            }
        }

        // Controls bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallControlButton(
                icon = if (viewModel.isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                label = if (viewModel.isMicOn) "Mute" else "Unmute",
                onClick = { viewModel.toggleMic() }
            )
            if (callType == "video") {
                CallControlButton(
                    icon = if (viewModel.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = if (viewModel.isCameraOn) "Cam off" else "Cam on",
                    onClick = { viewModel.toggleCamera() }
                )
                CallControlButton(
                    icon = Icons.Default.Cameraswitch,
                    label = "Flip",
                    onClick = { viewModel.switchCamera() }
                )
            }
            CallControlButton(
                icon = Icons.Default.VolumeUp,
                label = "Speaker",
                onClick = { }
            )
            CallActionButton(icon = Icons.Default.CallEnd, label = "End", tint = Color.Red) { onEnd() }
        }
    }

    DisposableEffect(Unit) {
        onDispose { remoteSinkRef.value?.release() }
    }
}

@Composable
private fun CallControlButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}

@Composable
private fun CallActionButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .background(tint, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}
