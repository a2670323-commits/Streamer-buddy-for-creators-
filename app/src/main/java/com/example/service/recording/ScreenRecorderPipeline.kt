package com.example.service.recording

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
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.FileDescriptor
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class ScreenRecorderPipeline(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val outputPfd: ParcelFileDescriptor,
    private val width: Int,
    private val height: Int,
    private val densityDpi: Int,
    private val videoBitrate: Int,
    private val videoFps: Int,
    private val audioBitrate: Int = 160000,
    private val sampleRate: Int = 44100
) {

    private val isRunning = AtomicBoolean(false)

    // Video components
    private var videoEncoder: MediaCodec? = null
    private var virtualDisplay: VirtualDisplay? = null

    // Audio components
    private var audioEncoder: MediaCodec? = null
    private var micAudioRecord: AudioRecord? = null
    private var internalAudioRecord: AudioRecord? = null

    // Muxer & synchronization
    private var mediaMuxer: MediaMuxer? = null
    private val muxerLock = Any()
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var isMuxerStarted = false
    private var hasAudioTrack = false

    // Worker threads
    private var videoDrainThread: Thread? = null
    private var audioCaptureThread: Thread? = null
    private var audioDrainThread: Thread? = null

    fun start() {
        if (isRunning.getAndSet(true)) return

        try {
            initMuxer()
            initVideoEncoder()
            initAudioPipeline()

            startThreads()
            Log.i(TAG, "ScreenRecorderPipeline started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ScreenRecorderPipeline", e)
            stop()
            throw e
        }
    }

    private fun initMuxer() {
        mediaMuxer = MediaMuxer(outputPfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
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
            "StreamBuddy_CaptureSurface",
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
        // Step 1: Configure AAC Audio MediaCodec
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
            hasAudioTrack = true
        } catch (e: Exception) {
            Log.w(TAG, "AAC Audio encoder creation failed, falling back to video-only", e)
            hasAudioTrack = false
            return
        }

        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)
        val bufferSize = maxOf(minBufferSize * 2, 8192)

        // Step 2: Configure Microphone AudioRecord (if permission is granted)
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
                    Log.i(TAG, "Microphone AudioRecord initialized & recording")
                } else {
                    Log.w(TAG, "Microphone AudioRecord failed to initialize")
                    micAudioRecord = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception initializing microphone AudioRecord", e)
                micAudioRecord = null
            }
        }

        // Step 3: Configure Internal Playback AudioRecord using AudioPlaybackCaptureConfiguration (Android 10+)
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
                    Log.i(TAG, "Internal AudioRecord (AudioPlaybackCapture) initialized & recording")
                } else {
                    Log.w(TAG, "Internal AudioRecord failed to initialize")
                    internalAudioRecord = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception initializing internal playback capture AudioRecord", e)
                internalAudioRecord = null
            }
        }
    }

    private fun startThreads() {
        // Video Drain Thread
        videoDrainThread = Thread({ drainVideoEncoder() }, "StreamBuddy-VideoDrain").apply { start() }

        if (hasAudioTrack) {
            // Audio Capture & Mixer Thread
            audioCaptureThread = Thread({ captureAndMixAudio() }, "StreamBuddy-AudioCapture").apply { start() }

            // Audio Drain Thread
            audioDrainThread = Thread({ drainAudioEncoder() }, "StreamBuddy-AudioDrain").apply { start() }
        }
    }

    private fun drainVideoEncoder() {
        val encoder = videoEncoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        while (isRunning.get()) {
            val outputIndex = try {
                encoder.dequeueOutputBuffer(bufferInfo, 10000L)
            } catch (e: Exception) {
                break
            }

            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                synchronized(muxerLock) {
                    videoTrackIndex = mediaMuxer?.addTrack(newFormat) ?: -1
                    checkStartMuxer()
                }
            } else if (outputIndex >= 0) {
                val outputBuffer = encoder.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    synchronized(muxerLock) {
                        if (isMuxerStarted && videoTrackIndex >= 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            try {
                                mediaMuxer?.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error writing video sample data", e)
                            }
                        }
                    }
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

        var totalSamplesRecorded = 0L

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

            // Mix the audio streams with custom AudioMixer
            val bytesToMix = AudioMixer.mix16BitPcm(
                micBuffer = if (micBytes > 0) micBuffer else null,
                micBytes = micBytes.coerceAtLeast(0),
                micGain = 1.0f,
                internalBuffer = if (internalBytes > 0) internalBuffer else null,
                internalBytes = internalBytes.coerceAtLeast(0),
                internalGain = 0.85f,
                outputBuffer = mixedBuffer
            )

            if (bytesToMix > 0) {
                val inputIndex = encoder.dequeueInputBuffer(10000L)
                if (inputIndex >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputIndex)
                    if (inputBuffer != null) {
                        inputBuffer.clear()
                        inputBuffer.put(mixedBuffer, 0, bytesToMix)

                        // 16-bit stereo = 4 bytes per sample frame
                        val sampleFrames = bytesToMix / 4
                        val presentationTimeUs = (totalSamplesRecorded * 1_000_000L) / sampleRate
                        totalSamplesRecorded += sampleFrames

                        encoder.queueInputBuffer(
                            inputIndex,
                            0,
                            bytesToMix,
                            presentationTimeUs,
                            0
                        )
                    }
                }
            }
        }

        // Send End of Stream to Audio Encoder
        try {
            val inputIndex = encoder.dequeueInputBuffer(10000L)
            if (inputIndex >= 0) {
                encoder.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
        } catch (ignored: Exception) {}
    }

    private fun drainAudioEncoder() {
        val encoder = audioEncoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        while (isRunning.get()) {
            val outputIndex = try {
                encoder.dequeueOutputBuffer(bufferInfo, 10000L)
            } catch (e: Exception) {
                break
            }

            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val newFormat = encoder.outputFormat
                synchronized(muxerLock) {
                    audioTrackIndex = mediaMuxer?.addTrack(newFormat) ?: -1
                    checkStartMuxer()
                }
            } else if (outputIndex >= 0) {
                val outputBuffer = encoder.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    synchronized(muxerLock) {
                        if (isMuxerStarted && audioTrackIndex >= 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            try {
                                mediaMuxer?.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error writing audio sample data", e)
                            }
                        }
                    }
                }
                encoder.releaseOutputBuffer(outputIndex, false)

                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }
        }
    }

    private fun checkStartMuxer() {
        if (isMuxerStarted) return

        val videoReady = videoTrackIndex >= 0
        val audioReady = !hasAudioTrack || audioTrackIndex >= 0

        if (videoReady && audioReady) {
            try {
                mediaMuxer?.start()
                isMuxerStarted = true
                Log.i(TAG, "MediaMuxer started with videoTrack=$videoTrackIndex, audioTrack=$audioTrackIndex")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MediaMuxer", e)
            }
        }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return

        Log.i(TAG, "Stopping ScreenRecorderPipeline...")

        // Signal video EOS
        try {
            videoEncoder?.signalEndOfInputStream()
        } catch (ignored: Exception) {}

        // Wait for drain threads to finish
        try {
            videoDrainThread?.join(1000)
            audioCaptureThread?.join(1000)
            audioDrainThread?.join(1000)
        } catch (ignored: InterruptedException) {}

        // Release Audio components
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

        // Release Video components
        try {
            virtualDisplay?.release()
        } catch (ignored: Exception) {}
        virtualDisplay = null

        try {
            videoEncoder?.stop()
            videoEncoder?.release()
        } catch (ignored: Exception) {}
        videoEncoder = null

        // Stop MediaMuxer
        synchronized(muxerLock) {
            if (isMuxerStarted) {
                try {
                    mediaMuxer?.stop()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping MediaMuxer", e)
                }
                isMuxerStarted = false
            }
            try {
                mediaMuxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMuxer", e)
            }
            mediaMuxer = null
        }

        Log.i(TAG, "ScreenRecorderPipeline stopped and resources released")
    }

    companion object {
        private const val TAG = "ScreenRecorderPipeline"
    }
}
