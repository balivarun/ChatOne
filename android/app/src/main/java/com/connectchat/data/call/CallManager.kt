package com.connectchat.data.call

import com.connectchat.data.webrtc.WebRtcManager
import com.connectchat.data.websocket.StompClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.VideoSink
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton

sealed class CallState {
    object Idle : CallState()
    data class Incoming(
        val callerId: String,
        val callerName: String,
        val callerAvatar: String,
        val callerEmail: String,
        val conversationId: String,
        val callType: String
    ) : CallState()
    data class Outgoing(
        val targetEmail: String,
        val targetName: String,
        val targetAvatar: String?,
        val conversationId: String,
        val callType: String
    ) : CallState()
    data class Active(val callType: String) : CallState()
}

@Singleton
class CallManager @Inject constructor(
    private val stompClient: StompClient,
    private val webRtcManager: WebRtcManager,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    val localVideoTrack: VideoTrack? get() = webRtcManager.localVideoTrack

    /** Human-readable status shown in the calling screen (e.g. "Connecting…", "Ringing…") */
    private val _callStatus = MutableStateFlow("Calling…")
    val callStatus: StateFlow<String> = _callStatus.asStateFlow()

    private var peerEmail: String = ""
    private var _pendingSdp: SessionDescription? = null

    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            if (peerEmail.isBlank()) return
            stompClient.send("/app/call.ice", gson.toJson(mapOf(
                "targetEmail" to peerEmail,
                "candidate" to candidate.sdp,
                "sdpMid" to (candidate.sdpMid ?: ""),
                "sdpMLineIndex" to candidate.sdpMLineIndex
            )))
        }

        // With Unified Plan, onAddTrack fires per-track; streams[] may be empty.
        // We capture the VideoTrack directly from the RtpReceiver.
        override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, streams: Array<out MediaStream>?) {
            when (val track = receiver?.track()) {
                is VideoTrack -> _remoteVideoTrack.value = track
                else -> Unit
            }
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            when (state) {
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED -> {
                    val type = when (val s = _callState.value) {
                        is CallState.Outgoing -> s.callType
                        is CallState.Incoming -> s.callType
                        else -> "video"
                    }
                    _callStatus.value = "Connected"
                    _callState.value = CallState.Active(type)
                }
                PeerConnection.IceConnectionState.DISCONNECTED,
                PeerConnection.IceConnectionState.FAILED,
                PeerConnection.IceConnectionState.CLOSED -> {
                    _callState.value = CallState.Idle
                    _remoteVideoTrack.value = null
                    _callStatus.value = "Calling…"
                    scope.launch { webRtcManager.dispose() }
                }
                else -> Unit
            }
        }

        override fun onAddStream(stream: MediaStream) {}
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
        override fun onIceConnectionReceivingChange(p0: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        override fun onRemoveStream(stream: MediaStream?) {}
        override fun onDataChannel(dc: org.webrtc.DataChannel?) {}
        override fun onRenegotiationNeeded() {}
    }

    fun onIncomingOffer(callerId: String, callerName: String, callerAvatar: String,
                        callerEmail: String, convId: String, callType: String, sdp: String) {
        if (_callState.value !is CallState.Idle) return
        peerEmail = callerEmail
        _pendingSdp = SessionDescription(SessionDescription.Type.OFFER, sdp)
        _callState.value = CallState.Incoming(callerId, callerName, callerAvatar, callerEmail, convId, callType)
    }

    // Phase 1: update state so MainActivity navigates to CallScreen
    fun prepareOutgoingCall(
        targetEmail: String, targetName: String, targetAvatar: String?,
        convId: String, callType: String
    ) {
        if (_callState.value !is CallState.Idle) return
        peerEmail = targetEmail
        _callState.value = CallState.Outgoing(targetEmail, targetName, targetAvatar, convId, callType)
    }

    // Phase 2: called from CallScreen after it has rendered the local VideoSink
    fun startOutgoingMedia(localSink: VideoSink, isVideo: Boolean) {
        if (_callState.value !is CallState.Outgoing) return
        val outgoing = _callState.value as CallState.Outgoing
        scope.launch {
            _callStatus.value = "Initializing…"
            webRtcManager.init()
            if (isVideo) webRtcManager.startLocalStream(localSink)
            else webRtcManager.startAudioOnlyStream()
            webRtcManager.createPeerConnection(peerConnectionObserver)
            _callStatus.value = "Creating offer…"
            webRtcManager.createOffer { sdp ->
                val payload = gson.toJson(mapOf(
                    "targetEmail" to outgoing.targetEmail,
                    "conversationId" to outgoing.conversationId,
                    "sdp" to sdp.description,
                    "callType" to outgoing.callType
                ))
                var sent = stompClient.send("/app/call.offer", payload)
                if (!sent) {
                    // Socket may have been idle-dropped — retry once after 1s
                    scope.launch {
                        delay(1_000)
                        sent = stompClient.send("/app/call.offer", payload)
                        _callStatus.value = if (sent) "Ringing…" else "No connection — check network"
                    }
                } else {
                    _callStatus.value = "Ringing…"
                }
            }
        }
    }

    fun acceptCall(localSink: VideoSink, isVideo: Boolean) {
        val incoming = _callState.value as? CallState.Incoming ?: return
        val pendingSdp = _pendingSdp ?: return
        _pendingSdp = null
        scope.launch {
            webRtcManager.init()
            if (isVideo) webRtcManager.startLocalStream(localSink)
            else webRtcManager.startAudioOnlyStream()
            webRtcManager.createPeerConnection(peerConnectionObserver)
            webRtcManager.setRemoteDescription(pendingSdp)
            webRtcManager.createAnswer { sdp ->
                stompClient.send("/app/call.answer", gson.toJson(mapOf(
                    "targetEmail" to incoming.callerEmail,
                    "sdp" to sdp.description
                )))
            }
        }
    }

    fun rejectCall() {
        val incoming = _callState.value as? CallState.Incoming ?: return
        stompClient.send("/app/call.reject", gson.toJson(mapOf("targetEmail" to incoming.callerEmail)))
        _callState.value = CallState.Idle
        _pendingSdp = null
    }

    fun endCall() {
        if (_callState.value is CallState.Idle) return
        stompClient.send("/app/call.end", gson.toJson(mapOf("targetEmail" to peerEmail)))
        scope.launch { webRtcManager.dispose() }
        _callState.value = CallState.Idle
        _remoteVideoTrack.value = null
        _callStatus.value = "Calling…"
        peerEmail = ""
    }

    fun onRemoteAnswer(sdp: String) {
        _callStatus.value = "Connecting…"
        webRtcManager.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    fun onRemoteIce(candidate: String, sdpMid: String, sdpMLineIndex: Int) {
        webRtcManager.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
    }

    fun onCallEnded() {
        scope.launch { webRtcManager.dispose() }
        _callState.value = CallState.Idle
        _remoteVideoTrack.value = null
        _callStatus.value = "Calling…"
        peerEmail = ""
    }

    fun toggleMic(enabled: Boolean) = webRtcManager.toggleMic(enabled)
    fun toggleCamera(enabled: Boolean) = webRtcManager.toggleCamera(enabled)
    fun switchCamera() = webRtcManager.switchCamera()
    fun getEglBase() = webRtcManager.eglBase
}
