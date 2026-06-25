package com.connectchat.ui.screens.call

import android.Manifest
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoFrame
import org.webrtc.VideoSink
import org.webrtc.VideoTrack

private val NoOpVideoSink = VideoSink { _: VideoFrame? -> }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CallScreen(
    onCallEnded: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val callState by viewModel.callState.collectAsState()
    val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsState()
    val eglBase = remember { viewModel.getEglBase() }

    // Request camera + mic permissions up front
    val permissions = rememberMultiplePermissionsState(
        listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )
    LaunchedEffect(Unit) {
        if (!permissions.allPermissionsGranted) permissions.launchMultiplePermissionRequest()
    }

    LaunchedEffect(callState) {
        if (callState is CallState.Idle) onCallEnded()
    }

    when (val state = callState) {
        is CallState.Incoming -> IncomingCallView(
            state = state,
            viewModel = viewModel,
            eglBase = eglBase,
            permissionsGranted = permissions.allPermissionsGranted
        )
        is CallState.Outgoing -> OutgoingCallView(
            state = state,
            viewModel = viewModel,
            eglBase = eglBase,
            permissionsGranted = permissions.allPermissionsGranted
        )
        is CallState.Active -> ActiveCallView(
            callType = state.callType,
            viewModel = viewModel,
            remoteVideoTrack = remoteVideoTrack,
            eglBase = eglBase
        )
        is CallState.Idle -> Unit
    }
}

@Composable
private fun IncomingCallView(
    state: CallState.Incoming,
    viewModel: CallViewModel,
    eglBase: EglBase,
    permissionsGranted: Boolean
) {
    val isVideo = state.callType == "video"
    val localSinkRef = remember { mutableStateOf<VideoSink>(NoOpVideoSink) }
    val rendererReady = remember { mutableStateOf(!isVideo) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = if (isVideo) "Incoming video call" else "Incoming voice call",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
            UserAvatar(avatarUrl = state.callerAvatar, displayName = state.callerName, size = 100.dp)
            Text(state.callerName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                CallActionButton(icon = Icons.Default.CallEnd, label = "Decline", tint = Color.Red) {
                    viewModel.rejectCall()
                }
                CallActionButton(
                    icon = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                    label = "Accept",
                    tint = Color(0xFF00C853)
                ) {
                    if (rendererReady.value && permissionsGranted) {
                        viewModel.acceptCall(localSinkRef.value, isVideo)
                    }
                }
            }
        }

        // 1×1 invisible renderer to have an EGL surface ready for the video path
        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(1, 1)
                        init(eglBase.eglBaseContext, null)
                        localSinkRef.value = this
                        rendererReady.value = true
                    }
                },
                modifier = Modifier.size(1.dp)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { (localSinkRef.value as? SurfaceViewRenderer)?.release() }
    }
}

@Composable
private fun OutgoingCallView(
    state: CallState.Outgoing,
    viewModel: CallViewModel,
    eglBase: EglBase,
    permissionsGranted: Boolean
) {
    val isVideo = state.callType == "video"
    val mediaStarted = remember { mutableStateOf(false) }
    val localSinkRef = remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        if (isVideo && permissionsGranted) {
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
                modifier = Modifier.fillMaxSize()
            )
        }

        // Start media once permissions granted and (for video) renderer is ready
        LaunchedEffect(permissionsGranted, localSinkRef.value) {
            if (!permissionsGranted || mediaStarted.value) return@LaunchedEffect
            val readyForVideo = !isVideo || localSinkRef.value != null
            if (readyForVideo) {
                mediaStarted.value = true
                val sink: VideoSink = localSinkRef.value ?: NoOpVideoSink
                viewModel.startOutgoingMedia(sink, isVideo)
            }
        }

        // Calling overlay text
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UserAvatar(avatarUrl = state.targetAvatar, displayName = state.targetName, size = 80.dp)
            Text(state.targetName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
            CallActionButton(icon = Icons.Default.CallEnd, label = "End", tint = Color.Red) {
                viewModel.endCall()
            }
            if (isVideo) {
                CallControlButton(
                    icon = if (viewModel.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = if (viewModel.isCameraOn) "Camera off" else "Camera on",
                    onClick = { viewModel.toggleCamera() }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { localSinkRef.value?.release() }
    }
}

@Composable
private fun ActiveCallView(
    callType: String,
    viewModel: CallViewModel,
    remoteVideoTrack: VideoTrack?,
    eglBase: EglBase
) {
    val isVideo = callType == "video"
    val remoteSinkRef = remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    // Attach incoming remote track to renderer as soon as both are ready
    LaunchedEffect(remoteVideoTrack, remoteSinkRef.value) {
        val renderer = remoteSinkRef.value ?: return@LaunchedEffect
        remoteVideoTrack?.addSink(renderer)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isVideo) {
            // Full-screen remote video
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

            // Local camera picture-in-picture (top right)
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
                    Icon(
                        Icons.Default.Phone, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Call Connected", color = Color.White, fontSize = 20.sp)
                }
            }
        }

        // Control bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CallControlButton(
                icon = if (viewModel.isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                label = if (viewModel.isMicOn) "Mute" else "Unmute",
                onClick = { viewModel.toggleMic() }
            )
            if (isVideo) {
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
            CallActionButton(
                icon = Icons.Default.CallEnd, label = "End", tint = Color.Red,
                onClick = { viewModel.endCall() }
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val renderer = remoteSinkRef.value
            if (renderer != null) remoteVideoTrack?.removeSink(renderer)
            renderer?.release()
        }
    }
}

@Composable
private fun CallControlButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
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
            modifier = Modifier.size(64.dp).background(tint, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}
