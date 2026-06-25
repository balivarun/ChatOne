package com.connectchat.data.webrtc

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var localStream: MediaStream? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    val eglBase: EglBase = EglBase.create()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    fun init() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()
    }

    fun startLocalStream(localRenderer: VideoSink): MediaStream? {
        val factory = peerConnectionFactory ?: return null

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoCapturer = createCameraCapturer()
        localVideoTrack = factory.createVideoTrack("local_video", factory.createVideoSource(false).also { source ->
            videoCapturer?.initialize(surfaceTextureHelper, context, source.capturerObserver)
            videoCapturer?.startCapture(1280, 720, 30)
        })
        localVideoTrack?.addSink(localRenderer)

        localAudioTrack = factory.createAudioTrack("local_audio", factory.createAudioSource(MediaConstraints()))

        localStream = factory.createLocalMediaStream("local_stream").also {
            it.addTrack(localVideoTrack)
            it.addTrack(localAudioTrack)
        }
        return localStream
    }

    fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        val factory = peerConnectionFactory ?: return null
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        peerConnection = factory.createPeerConnection(rtcConfig, observer)
        localStream?.let { stream ->
            stream.audioTracks.forEach { peerConnection?.addTrack(it) }
            stream.videoTracks.forEach { peerConnection?.addTrack(it) }
        }
        return peerConnection
    }

    fun createOffer(onSdp: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                onSdp(sdp)
            }
        }, constraints)
    }

    fun createAnswer(onSdp: (SessionDescription) -> Unit) {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SdpObserverAdapter(), sdp)
                onSdp(sdp)
            }
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(SdpObserverAdapter(), sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun toggleMic(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun toggleCamera(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    fun dispose() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        localStream?.dispose()
        peerConnection?.dispose()
        peerConnection = null
        localVideoTrack = null
        localAudioTrack = null
        localStream = null
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val frontCamera = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
        val backCamera = enumerator.deviceNames.firstOrNull { enumerator.isBackFacing(it) }
        return (frontCamera ?: backCamera)?.let { enumerator.createCapturer(it, null) }
    }
}

open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
