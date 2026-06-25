package com.connectchat.ui.screens.call

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.connectchat.data.call.CallManager
import com.connectchat.data.call.CallState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.VideoSink
import org.webrtc.VideoTrack
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callManager: CallManager
) : ViewModel() {

    val callState: StateFlow<CallState> = callManager.callState
    val remoteVideoTrack: StateFlow<VideoTrack?> = callManager.remoteVideoTrack
    val callStatus: StateFlow<String> = callManager.callStatus

    var isMicOn by mutableStateOf(true)
    var isCameraOn by mutableStateOf(true)

    fun startOutgoingMedia(localSink: VideoSink, isVideo: Boolean) {
        callManager.startOutgoingMedia(localSink, isVideo)
    }

    fun acceptCall(localSink: VideoSink, isVideo: Boolean) {
        callManager.acceptCall(localSink, isVideo)
    }

    fun rejectCall() {
        callManager.rejectCall()
    }

    fun endCall() {
        callManager.endCall()
    }

    fun toggleMic() {
        isMicOn = !isMicOn
        callManager.toggleMic(isMicOn)
    }

    fun toggleCamera() {
        isCameraOn = !isCameraOn
        callManager.toggleCamera(isCameraOn)
    }

    fun switchCamera() {
        callManager.switchCamera()
    }

    fun getEglBase() = callManager.getEglBase()
}
