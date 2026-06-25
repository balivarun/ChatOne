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
    private var localVideoSource: VideoSource? = null
    private var localStream: MediaStream? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    val eglBase: EglBase = EglBase.create()

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    fun init() {
        // Safe to call multiple times — PeerConnectionFactory.initialize() is idempotent
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
        )
        if (peerConnectionFactory == null) {
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
                .createPeerConnectionFactory()
        }
    }

    fun startLocalStream(localRenderer: VideoSink): MediaStream? {
        val factory = peerConnectionFactory ?: return null

        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoCapturer = createCameraCapturer()

        // Keep VideoSource reference so it isn't GC'd while capturer is running
        localVideoSource = factory.createVideoSource(false)
        videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource!!.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack = factory.createVideoTrack("local_video", localVideoSource!!)
        localVideoTrack?.addSink(localRenderer)

        localAudioTrack = factory.createAudioTrack("local_audio", factory.createAudioSource(MediaConstraints()))

        localStream = factory.createLocalMediaStream("local_stream").also {
            it.addTrack(localVideoTrack)
            it.addTrack(localAudioTrack)
        }
        return localStream
    }

    fun startAudioOnlyStream(): MediaStream? {
        val factory = peerConnectionFactory ?: return null
        localAudioTrack = factory.createAudioTrack("local_audio", factory.createAudioSource(MediaConstraints()))
        localStream = factory.createLocalMediaStream("local_stream").also {
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
        localStream?.audioTracks?.forEach { peerConnection?.addTrack(it) }
        localStream?.videoTracks?.forEach { peerConnection?.addTrack(it) }
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

    fun toggleMic(enabled: Boolean) { localAudioTrack?.setEnabled(enabled) }
    fun toggleCamera(enabled: Boolean) { localVideoTrack?.setEnabled(enabled) }
    fun switchCamera() { (videoCapturer as? CameraVideoCapturer)?.switchCamera(null) }

    fun dispose() {
        runCatching { videoCapturer?.stopCapture() }
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        localVideoSource?.dispose()
        localStream?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        peerConnection = null
        peerConnectionFactory = null
        localVideoTrack = null
        localAudioTrack = null
        localVideoSource = null
        localStream = null
        videoCapturer = null
        surfaceTextureHelper = null
    }

    private fun createCameraCapturer(): VideoCapturer? {
        return runCatching {
            val enumerator = Camera2Enumerator(context)
            val deviceNames = enumerator.deviceNames
            val frontCamera = deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            val backCamera = deviceNames.firstOrNull { enumerator.isBackFacing(it) }
            (frontCamera ?: backCamera)?.let { enumerator.createCapturer(it, null) }
        }.getOrNull()
    }
}

open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
