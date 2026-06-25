package com.connectchat.data.call

import com.connectchat.data.webrtc.WebRtcManager
import com.connectchat.data.websocket.StompClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.VideoSink
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
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var peerEmail: String = ""

    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            if (peerEmail.isBlank()) return
            stompClient.send("/app/call.ice", gson.toJson(mapOf(
                "targetEmail" to peerEmail,
                "candidate" to candidate.sdp,
                "sdpMid" to candidate.sdpMid,
                "sdpMLineIndex" to candidate.sdpMLineIndex
            )))
        }
        override fun onAddStream(stream: MediaStream) {
            _remoteStream.value = stream
        }
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            if (state == PeerConnection.IceConnectionState.CONNECTED ||
                state == PeerConnection.IceConnectionState.COMPLETED) {
                val current = _callState.value
                val type = when (current) {
                    is CallState.Outgoing -> current.callType
                    is CallState.Incoming -> current.callType
                    else -> "video"
                }
                _callState.value = CallState.Active(type)
            }
            if (state == PeerConnection.IceConnectionState.DISCONNECTED ||
                state == PeerConnection.IceConnectionState.FAILED ||
                state == PeerConnection.IceConnectionState.CLOSED) {
                _callState.value = CallState.Idle
                webRtcManager.dispose()
            }
        }
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
        override fun onIceConnectionReceivingChange(p0: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        override fun onRemoveStream(stream: MediaStream?) {}
        override fun onDataChannel(dc: org.webrtc.DataChannel?) {}
        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, streams: Array<out MediaStream>?) {
            streams?.firstOrNull()?.let { _remoteStream.value = it }
        }
    }

    private val _remoteStream = MutableStateFlow<MediaStream?>(null)
    val remoteStream: StateFlow<MediaStream?> = _remoteStream.asStateFlow()

    fun onIncomingOffer(callerId: String, callerName: String, callerAvatar: String,
                        callerEmail: String, convId: String, callType: String, sdp: String) {
        if (_callState.value !is CallState.Idle) return
        peerEmail = callerEmail
        _pendingSdp = SessionDescription(SessionDescription.Type.OFFER, sdp)
        _callState.value = CallState.Incoming(callerId, callerName, callerAvatar, callerEmail, convId, callType)
    }

    private var _pendingSdp: SessionDescription? = null

    // Phase 1: set state to Outgoing (navigates to CallScreen)
    fun prepareOutgoingCall(
        targetEmail: String, targetName: String, targetAvatar: String?,
        convId: String, callType: String
    ) {
        if (_callState.value !is CallState.Idle) return
        peerEmail = targetEmail
        _callState.value = CallState.Outgoing(targetEmail, targetName, targetAvatar, convId, callType)
    }

    // Phase 2: called from CallScreen once it has a local VideoSink ready
    fun startOutgoingMedia(localSink: VideoSink) {
        val outgoing = _callState.value as? CallState.Outgoing ?: return
        webRtcManager.init()
        webRtcManager.startLocalStream(localSink)
        webRtcManager.createPeerConnection(peerConnectionObserver)
        webRtcManager.createOffer { sdp ->
            stompClient.send("/app/call.offer", gson.toJson(mapOf(
                "targetEmail" to outgoing.targetEmail,
                "conversationId" to outgoing.conversationId,
                "sdp" to sdp.description,
                "callType" to outgoing.callType
            )))
        }
    }

    fun acceptCall(localSink: VideoSink) {
        val incoming = _callState.value as? CallState.Incoming ?: return
        webRtcManager.init()
        webRtcManager.startLocalStream(localSink)
        webRtcManager.createPeerConnection(peerConnectionObserver)
        _pendingSdp?.let { webRtcManager.setRemoteDescription(it) }
        _pendingSdp = null
        webRtcManager.createAnswer { sdp ->
            stompClient.send("/app/call.answer", gson.toJson(mapOf(
                "targetEmail" to incoming.callerEmail,
                "sdp" to sdp.description
            )))
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
        webRtcManager.dispose()
        _callState.value = CallState.Idle
        _remoteStream.value = null
        peerEmail = ""
    }

    fun onRemoteAnswer(sdp: String) {
        webRtcManager.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp))
    }

    fun onRemoteIce(candidate: String, sdpMid: String, sdpMLineIndex: Int) {
        webRtcManager.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
    }

    fun onCallEnded() {
        webRtcManager.dispose()
        _callState.value = CallState.Idle
        _remoteStream.value = null
        peerEmail = ""
    }

    fun toggleMic(enabled: Boolean) = webRtcManager.toggleMic(enabled)
    fun toggleCamera(enabled: Boolean) = webRtcManager.toggleCamera(enabled)
    fun switchCamera() = webRtcManager.switchCamera()
    fun getEglBase() = webRtcManager.eglBase
}
