package com.example.service.rtmp

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.service.recording.AudioMixer
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class RtmpStreamPipeline(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val rtmpManager: RtmpConnectionManager,
    private val width: Int,
    private val height: Int,
    private val densityDpi: Int,
    private val videoBitrate: Int,
    private val videoFps: Int,
    private val audioBitrate: Int = 160000,
    private val sampleRate: Int = 44100
) {

    private val isRunning = AtomicBoolean(false)

    private var videoEncoder: MediaCodec? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var audioEncoder: MediaCodec? = null
    private var micAudioRecord: AudioRecord? = null
    private var internalAudioRecord: AudioRecord? = null

    private var videoDrainThread: Thread? = null
    private var audioCaptureThread: Thread? = null
    private var audioDrainThread: Thread? = null

    private var startTimeMs = 0L
    private var hasSentVideoSequenceHeader = false
    private var hasSentAudioSequenceHeader = false

    fun start() {
        if (isRunning.getAndSet(true)) return

        startTimeMs = System.currentTimeMillis()
        hasSentVideoSequenceHeader = false
        hasSentAudioSequenceHeader = false

        try {
            initVideoEncoder()
            initAudioPipeline()
            startThreads()
            Log.i(TAG, "RtmpStreamPipeline initialized and running")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start RtmpStreamPipeline", e)
            stop()
            throw e
        }
    }

    private fun initVideoEncoder() {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, videoFps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface = encoder.createInputSurface()
        encoder.start()
        videoEncoder = encoder

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "StreamBuddy_RtmpVirtualDisplay",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null
        )
    }

    private fun initAudioPipeline() {
        try {
            val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 2).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate)
                setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
                setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            audioEncoder = encoder
        } catch (e: Exception) {
            Log.w(TAG, "Audio encoder init failed for RTMP, proceeding video only", e)
            return
        }

        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)
        val bufferSize = maxOf(minBufferSize * 2, 8192)

        // Microphone Commentary AudioRecord
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            try {
                val micFormat = AudioFormat.Builder()
                    .setEncoding(audioEncoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()

                micAudioRecord = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(micFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .build()

                if (micAudioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    micAudioRecord?.startRecording()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error initializing mic AudioRecord for RTMP", e)
                micAudioRecord = null
            }
        }

        // Internal Game Audio Playback Capture AudioRecord (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()

                val internalFormat = AudioFormat.Builder()
                    .setEncoding(audioEncoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()

                internalAudioRecord = AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(captureConfig)
                    .setAudioFormat(internalFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .build()

                if (internalAudioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    internalAudioRecord?.startRecording()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error initializing internal audio capture for RTMP", e)
                internalAudioRecord = null
            }
        }
    }

    private fun startThreads() {
        videoDrainThread = Thread({ drainVideoToRtmp() }, "StreamBuddy-RtmpVideoDrain").apply { start() }

        if (audioEncoder != null) {
            audioCaptureThread = Thread({ captureAndMixAudio() }, "StreamBuddy-RtmpAudioCapture").apply { start() }
            audioDrainThread = Thread({ drainAudioToRtmp() }, "StreamBuddy-RtmpAudioDrain").apply { start() }
        }
    }

    private fun drainVideoToRtmp() {
        val encoder = videoEncoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        var sps: ByteArray? = null
        var pps: ByteArray? = null

        while (isRunning.get()) {
            val outputIndex = try {
                encoder.dequeueOutputBuffer(bufferInfo, 10000L)
            } catch (e: Exception) {
                break
            }

            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                val spsBuf = newFormat.getByteBuffer("csd-0")
                val ppsBuf = newFormat.getByteBuffer("csd-1")

                if (spsBuf != null && ppsBuf != null) {
                    sps = ByteArray(spsBuf.remaining()).also { spsBuf.get(it) }
                    pps = ByteArray(ppsBuf.remaining()).also { ppsBuf.get(it) }

                    // Send AVC sequence header tag
                    val avcConfig = FlvMuxer.buildAvcSequenceHeader(sps, pps)
                    val flvTag = FlvMuxer.createVideoTag(
                        isKeyframe = true,
                        isSequenceHeader = true,
                        timestampMs = 0L,
                        payload = avcConfig
                    )
                    rtmpManager.sendFlvTag(flvTag)
                    hasSentVideoSequenceHeader = true
                    Log.i(TAG, "Sent AVC sequence header to RTMP")
                }
            } else if (outputIndex >= 0) {
                val outputBuffer = encoder.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    val isKeyframe = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0

                    val naluBytes = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.get(naluBytes)

                    val timestampMs = System.currentTimeMillis() - startTimeMs

                    val flvTag = FlvMuxer.createVideoTag(
                        isKeyframe = isKeyframe,
                        isSequenceHeader = false,
                        timestampMs = timestampMs.coerceAtLeast(0L),
                        payload = naluBytes
                    )
                    rtmpManager.sendFlvTag(flvTag)
                }

                encoder.releaseOutputBuffer(outputIndex, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    private fun captureAndMixAudio() {
        val encoder = audioEncoder ?: return
        val bufferSize = 4096
        val micBuffer = ByteArray(bufferSize)
        val internalBuffer = ByteArray(bufferSize)
        val mixedBuffer = ByteArray(bufferSize)

        var totalSamples = 0L

        while (isRunning.get()) {
            val micBytes = micAudioRecord?.read(micBuffer, 0, bufferSize) ?: 0
            val internalBytes = internalAudioRecord?.read(internalBuffer, 0, bufferSize) ?: 0

            if (micBytes <= 0 && internalBytes <= 0) {
                try {
                    Thread.sleep(10)
                } catch (ignored: InterruptedException) {
                    break
                }
                continue
            }

            val mixedBytes = AudioMixer.mix16BitPcm(
                micBuffer = if (micBytes > 0) micBuffer else null,
                micBytes = micBytes.coerceAtLeast(0),
                micGain = 1.0f,
                internalBuffer = if (internalBytes > 0) internalBuffer else null,
                internalBytes = internalBytes.coerceAtLeast(0),
                internalGain = 0.85f,
                outputBuffer = mixedBuffer
            )

            if (mixedBytes > 0) {
                val inputIndex = encoder.dequeueInputBuffer(10000L)
                if (inputIndex >= 0) {
                    val inputBuf = encoder.getInputBuffer(inputIndex)
                    if (inputBuf != null) {
                        inputBuf.clear()
                        inputBuf.put(mixedBuffer, 0, mixedBytes)

                        val sampleFrames = mixedBytes / 4
                        val presentationTimeUs = (totalSamples * 1_000_000L) / sampleRate
                        totalSamples += sampleFrames

                        encoder.queueInputBuffer(inputIndex, 0, mixedBytes, presentationTimeUs, 0)
                    }
                }
            }
        }
    }

    private fun drainAudioToRtmp() {
        val encoder = audioEncoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        // Send AAC sequence header tag first
        val aacConfig = FlvMuxer.buildAacAudioSpecificConfig(sampleRate, 2)
        val aacSeqTag = FlvMuxer.createAudioTag(
            isSequenceHeader = true,
            timestampMs = 0L,
            payload = aacConfig
        )
        rtmpManager.sendFlvTag(aacSeqTag)
        hasSentAudioSequenceHeader = true

        while (isRunning.get()) {
            val outputIndex = try {
                encoder.dequeueOutputBuffer(bufferInfo, 10000L)
            } catch (e: Exception) {
                break
            }

            if (outputIndex >= 0) {
                val outputBuffer = encoder.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    val audioBytes = ByteArray(bufferInfo.size)
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.get(audioBytes)

                    val timestampMs = System.currentTimeMillis() - startTimeMs

                    val flvAudioTag = FlvMuxer.createAudioTag(
                        isSequenceHeader = false,
                        timestampMs = timestampMs.coerceAtLeast(0L),
                        payload = audioBytes
                    )
                    rtmpManager.sendFlvTag(flvAudioTag)
                }

                encoder.releaseOutputBuffer(outputIndex, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return

        try {
            videoEncoder?.signalEndOfInputStream()
        } catch (ignored: Exception) {}

        try {
            videoDrainThread?.join(1000)
            audioCaptureThread?.join(1000)
            audioDrainThread?.join(1000)
        } catch (ignored: InterruptedException) {}

        try {
            micAudioRecord?.stop()
            micAudioRecord?.release()
        } catch (ignored: Exception) {}
        micAudioRecord = null

        try {
            internalAudioRecord?.stop()
            internalAudioRecord?.release()
        } catch (ignored: Exception) {}
        internalAudioRecord = null

        try {
            audioEncoder?.stop()
            audioEncoder?.release()
        } catch (ignored: Exception) {}
        audioEncoder = null

        try {
            virtualDisplay?.release()
        } catch (ignored: Exception) {}
        virtualDisplay = null

        try {
            videoEncoder?.stop()
            videoEncoder?.release()
        } catch (ignored: Exception) {}
        videoEncoder = null

        Log.i(TAG, "RtmpStreamPipeline stopped")
    }

    companion object {
        private const val TAG = "RtmpStreamPipeline"
    }
}
