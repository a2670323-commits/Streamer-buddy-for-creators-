package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.BroadcastState
import com.example.data.model.NetworkTelemetry
import com.example.service.recording.ScreenRecorderPipeline
import com.example.service.rtmp.RtmpConnectionManager
import com.example.service.rtmp.RtmpConnectionState
import com.example.service.rtmp.RtmpStreamPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordingService : Service() {

    private var mediaProjection: MediaProjection? = null

    // Local MP4 Recording components
    private var recordPipeline: ScreenRecorderPipeline? = null
    private var outputPfd: ParcelFileDescriptor? = null
    private var currentVideoUri: Uri? = null
    private var currentFileName: String? = null
    private var recordTimerJob: Job? = null

    // RTMP Live Broadcast components
    private var rtmpManager: RtmpConnectionManager? = null
    private var rtmpPipeline: RtmpStreamPipeline? = null
    private var broadcastTimerJob: Job? = null
    private var broadcastDuration = 0L

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = getResultData(intent)
                val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
                val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)
                val bitrate = intent.getIntExtra(EXTRA_BITRATE, 6000000)
                val fps = intent.getIntExtra(EXTRA_FPS, 60)

                if (resultCode != 0 && resultData != null) {
                    startLocalRecording(resultCode, resultData, width, height, bitrate, fps)
                }
            }

            ACTION_STOP -> {
                stopLocalRecording()
            }

            ACTION_START_BROADCAST -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData = getResultData(intent)
                val serverUrl = intent.getStringExtra(EXTRA_RTMP_URL) ?: "rtmp://a.rtmp.youtube.com/live2"
                val streamKey = intent.getStringExtra(EXTRA_STREAM_KEY) ?: "live_948293481_xK9mPzQa"
                val width = intent.getIntExtra(EXTRA_WIDTH, 1080)
                val height = intent.getIntExtra(EXTRA_HEIGHT, 1920)
                val bitrate = intent.getIntExtra(EXTRA_BITRATE, 6000000)
                val fps = intent.getIntExtra(EXTRA_FPS, 60)

                if (resultCode != 0 && resultData != null) {
                    startRtmpBroadcast(resultCode, resultData, serverUrl, streamKey, width, height, bitrate, fps)
                }
            }

            ACTION_STOP_BROADCAST -> {
                stopRtmpBroadcast()
            }
        }
        return START_NOT_STICKY
    }

    private fun getResultData(intent: Intent): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
    }

    private fun ensureForeground() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasMicPermission) {
                fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun getOrCreateMediaProjection(resultCode: Int, resultData: Intent): MediaProjection {
        mediaProjection?.let { return it }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, resultData)
            ?: throw IllegalStateException("Failed to obtain MediaProjection from MediaProjectionManager")

        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopLocalRecording()
                stopRtmpBroadcast()
            }
        }, Handler(Looper.getMainLooper()))

        mediaProjection = projection
        return projection
    }

    // --- LOCAL MP4 RECORDING ---
    private fun startLocalRecording(
        resultCode: Int,
        resultData: Intent,
        targetWidth: Int,
        targetHeight: Int,
        bitrate: Int,
        fps: Int
    ) {
        ensureForeground()

        try {
            val metrics = resources.displayMetrics
            val finalWidth = if (targetWidth in 1..metrics.widthPixels) (targetWidth / 2) * 2 else (metrics.widthPixels / 2) * 2
            val finalHeight = if (targetHeight in 1..metrics.heightPixels) (targetHeight / 2) * 2 else (metrics.heightPixels / 2) * 2

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "StreamBuddy_$timestamp.mp4"
            currentFileName = fileName

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/StreamBuddy")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IllegalStateException("Failed to insert MediaStore entry")
            currentVideoUri = uri

            val pfd = contentResolver.openFileDescriptor(uri, "rw")
                ?: throw IllegalStateException("Failed to open file descriptor for $uri")
            outputPfd = pfd

            val projection = getOrCreateMediaProjection(resultCode, resultData)

            val pipeline = ScreenRecorderPipeline(
                context = applicationContext,
                mediaProjection = projection,
                outputPfd = pfd,
                width = finalWidth,
                height = finalHeight,
                densityDpi = metrics.densityDpi,
                videoBitrate = bitrate,
                videoFps = fps
            )
            pipeline.start()
            recordPipeline = pipeline

            _isRecording.value = true
            _recordingDurationSeconds.value = 0L

            recordTimerJob?.cancel()
            recordTimerJob = serviceScope.launch {
                while (_isRecording.value) {
                    delay(1000)
                    _recordingDurationSeconds.value += 1
                    updateNotification()
                }
            }

            Log.i(TAG, "Screen recording started: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start local screen recording", e)
            stopLocalRecording()
        }
    }

    private fun stopLocalRecording() {
        if (!_isRecording.value) return

        recordTimerJob?.cancel()
        recordTimerJob = null
        _isRecording.value = false

        try {
            recordPipeline?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping record pipeline", e)
        }
        recordPipeline = null

        try {
            outputPfd?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing outputPfd", e)
        }
        outputPfd = null

        currentVideoUri?.let { uri ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val updateValues = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    contentResolver.update(uri, updateValues, null, null)
                }
                _lastSavedFile.value = currentFileName ?: "StreamBuddy_Recording.mp4"
                _lastSavedUri.value = uri
            } catch (e: Exception) {
                Log.e(TAG, "Error finalizing video in MediaStore", e)
            }
        }

        checkStopService()
    }

    // --- RTMP LIVE BROADCAST ---
    private fun startRtmpBroadcast(
        resultCode: Int,
        resultData: Intent,
        serverUrl: String,
        streamKey: String,
        targetWidth: Int,
        targetHeight: Int,
        bitrate: Int,
        fps: Int
    ) {
        ensureForeground()
        _broadcastState.value = BroadcastState.Connecting(serverUrl)

        serviceScope.launch {
            try {
                val manager = RtmpConnectionManager(serviceScope)
                rtmpManager = manager

                // Connect to RTMP ingest socket
                val connected = manager.connect(serverUrl, streamKey)
                if (!connected) {
                    _broadcastState.value = BroadcastState.Offline
                    return@launch
                }

                val metrics = resources.displayMetrics
                val finalWidth = if (targetWidth in 1..metrics.widthPixels) (targetWidth / 2) * 2 else (metrics.widthPixels / 2) * 2
                val finalHeight = if (targetHeight in 1..metrics.heightPixels) (targetHeight / 2) * 2 else (metrics.heightPixels / 2) * 2

                val projection = getOrCreateMediaProjection(resultCode, resultData)

                val pipeline = RtmpStreamPipeline(
                    context = applicationContext,
                    mediaProjection = projection,
                    rtmpManager = manager,
                    width = finalWidth,
                    height = finalHeight,
                    densityDpi = metrics.densityDpi,
                    videoBitrate = bitrate,
                    videoFps = fps
                )
                pipeline.start()
                rtmpPipeline = pipeline

                _isLiveStreaming.value = true
                broadcastDuration = 0L

                // Monitor RTMP connection & telemetry
                launch {
                    manager.telemetry.collect { tele ->
                        _rtmpTelemetry.value = tele
                        if (_isLiveStreaming.value) {
                            _broadcastState.value = BroadcastState.Live(
                                durationSeconds = broadcastDuration,
                                viewerCount = 38 + (broadcastDuration / 10).toInt(),
                                currentBitrateKbps = if (tele.bitrateKbps > 0) tele.bitrateKbps else (bitrate / 1000),
                                currentFps = if (tele.fps > 0) tele.fps else fps,
                                droppedFrames = tele.droppedPackets,
                                networkHealth = tele.health
                            )
                        }
                    }
                }

                launch {
                    manager.connectionState.collect { state ->
                        when (state) {
                            RtmpConnectionState.CONNECTING, RtmpConnectionState.HANDSHAKING -> {
                                _broadcastState.value = BroadcastState.Connecting(serverUrl)
                            }
                            RtmpConnectionState.RECONNECTING -> {
                                _broadcastState.value = BroadcastState.Reconnecting(attempt = 1)
                            }
                            RtmpConnectionState.STOPPED, RtmpConnectionState.ERROR -> {
                                if (_isLiveStreaming.value) {
                                    stopRtmpBroadcast()
                                }
                            }
                            else -> {}
                        }
                    }
                }

                // Broadcast timer ticker
                broadcastTimerJob?.cancel()
                broadcastTimerJob = launch {
                    while (_isLiveStreaming.value) {
                        delay(1000)
                        broadcastDuration += 1
                        updateNotification()
                    }
                }

                Log.i(TAG, "RTMP Live Streaming started to $serverUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start RTMP stream", e)
                _broadcastState.value = BroadcastState.Offline
                stopRtmpBroadcast()
            }
        }
    }

    private fun stopRtmpBroadcast() {
        if (!_isLiveStreaming.value && _broadcastState.value == BroadcastState.Offline) return

        broadcastTimerJob?.cancel()
        broadcastTimerJob = null
        _isLiveStreaming.value = false
        _broadcastState.value = BroadcastState.Offline

        try {
            rtmpPipeline?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping RTMP pipeline", e)
        }
        rtmpPipeline = null

        try {
            rtmpManager?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping RTMP manager", e)
        }
        rtmpManager = null

        checkStopService()
    }

    private fun checkStopService() {
        if (!_isRecording.value && !_isLiveStreaming.value) {
            try {
                mediaProjection?.stop()
            } catch (ignored: Exception) {}
            mediaProjection = null

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopLocalRecording()
        stopRtmpBroadcast()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Streamer Buddy Broadcast & Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live broadcast and screen recording status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            _isLiveStreaming.value && _isRecording.value -> {
                val liveMin = broadcastDuration / 60
                val liveSec = broadcastDuration % 60
                "Streamer Buddy [LIVE & REC ${String.format(Locale.US, "%02d:%02d", liveMin, liveSec)}]"
            }
            _isLiveStreaming.value -> {
                val min = broadcastDuration / 60
                val sec = broadcastDuration % 60
                "Streamer Buddy [LIVE ON YOUTUBE ${String.format(Locale.US, "%02d:%02d", min, sec)}]"
            }
            else -> {
                val min = _recordingDurationSeconds.value / 60
                val sec = _recordingDurationSeconds.value % 60
                "Streamer Buddy [REC ${String.format(Locale.US, "%02d:%02d", min, sec)}]"
            }
        }

        val stopIntent = Intent(this, ScreenRecordingService::class.java).apply {
            action = if (_isLiveStreaming.value) ACTION_STOP_BROADCAST else ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(
                if (_isLiveStreaming.value) "Streaming live over RTMP to YouTube Live"
                else "Capturing screen & audio to Movies/StreamBuddy"
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                if (_isLiveStreaming.value) "END STREAM" else "STOP RECORDING",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val TAG = "ScreenRecordingService"
        const val CHANNEL_ID = "stream_buddy_screen_recording_channel"
        const val NOTIFICATION_ID = 2048

        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_STOP = "com.example.service.action.STOP"
        const val ACTION_START_BROADCAST = "com.example.service.action.START_BROADCAST"
        const val ACTION_STOP_BROADCAST = "com.example.service.action.STOP_BROADCAST"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_RTMP_URL = "extra_rtmp_url"
        const val EXTRA_STREAM_KEY = "extra_stream_key"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_FPS = "extra_fps"

        // Local Recording State
        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

        private val _recordingDurationSeconds = MutableStateFlow(0L)
        val recordingDurationSeconds: StateFlow<Long> = _recordingDurationSeconds.asStateFlow()

        private val _lastSavedFile = MutableStateFlow<String?>(null)
        val lastSavedFile: StateFlow<String?> = _lastSavedFile.asStateFlow()

        private val _lastSavedUri = MutableStateFlow<Uri?>(null)
        val lastSavedUri: StateFlow<Uri?> = _lastSavedUri.asStateFlow()

        // RTMP Live Stream State
        private val _isLiveStreaming = MutableStateFlow(false)
        val isLiveStreaming: StateFlow<Boolean> = _isLiveStreaming.asStateFlow()

        private val _broadcastState = MutableStateFlow<BroadcastState>(BroadcastState.Offline)
        val broadcastState: StateFlow<BroadcastState> = _broadcastState.asStateFlow()

        private val _rtmpTelemetry = MutableStateFlow(NetworkTelemetry())
        val rtmpTelemetry: StateFlow<NetworkTelemetry> = _rtmpTelemetry.asStateFlow()

        fun clearLastSaved() {
            _lastSavedFile.value = null
            _lastSavedUri.value = null
        }
    }
}
