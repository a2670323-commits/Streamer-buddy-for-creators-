package com.example.ui.home

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BroadcastState
import com.example.data.model.ChecklistItem
import com.example.data.model.NetworkTelemetry
import com.example.data.model.StreamPreset
import com.example.data.model.YouTubeAccountStatus
import com.example.service.ScreenRecordingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val accountStatus: YouTubeAccountStatus = YouTubeAccountStatus.Unlinked(),
    val selectedPreset: StreamPreset = StreamPreset.Preset1080p,
    val broadcastState: BroadcastState = BroadcastState.Offline,
    val showGoLiveSheet: Boolean = false,
    val showLinkDialog: Boolean = false,
    val isScreenRecording: Boolean = false,
    val recordingDurationSeconds: Long = 0L,
    val lastRecordedFile: String? = null,
    val rtmpServerUrl: String = "rtmp://a.rtmp.youtube.com/live2",
    val rtmpStreamKey: String = "live_948293481_xK9mPzQa",
    val checklist: List<ChecklistItem> = listOf(
        ChecklistItem("preset", "Encoder Preset Configured", "1080p 60fps profile loaded", true),
        ChecklistItem("audio", "Microphone & Soundboard", "Dual channel 48kHz audio active", true),
        ChecklistItem("hud", "HUD Studio Overlays", "Live chat & alert box mounted", true),
        ChecklistItem("stream_key", "YouTube RTMP Ingest Endpoint", "Auto-generated ingestion token", false)
    ),
    val snackbarMessage: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Collect real-time screen recording state
        viewModelScope.launch {
            ScreenRecordingService.isRecording.collect { recording ->
                _uiState.update { it.copy(isScreenRecording = recording) }
            }
        }
        viewModelScope.launch {
            ScreenRecordingService.recordingDurationSeconds.collect { duration ->
                _uiState.update { it.copy(recordingDurationSeconds = duration) }
            }
        }
        viewModelScope.launch {
            ScreenRecordingService.lastSavedFile.collect { file ->
                if (file != null) {
                    _uiState.update {
                        it.copy(
                            lastRecordedFile = file,
                            snackbarMessage = "🎬 Screen recording saved: Movies/StreamBuddy/$file"
                        )
                    }
                    ScreenRecordingService.clearLastSaved()
                }
            }
        }

        // Collect RTMP live broadcasting state
        viewModelScope.launch {
            ScreenRecordingService.broadcastState.collect { bState ->
                _uiState.update { it.copy(broadcastState = bState) }
            }
        }
    }

    fun selectPreset(preset: StreamPreset) {
        _uiState.update { state ->
            val updatedChecklist = state.checklist.map { item ->
                if (item.id == "preset") item.copy(
                    subtitle = "${preset.name} (${preset.bitrateKbps} Kbps)",
                    isReady = true
                ) else item
            }
            state.copy(
                selectedPreset = preset,
                checklist = updatedChecklist,
                snackbarMessage = "Preset switched to ${preset.name}"
            )
        }
    }

    fun updateStreamSettings(serverUrl: String, streamKey: String) {
        _uiState.update {
            it.copy(
                rtmpServerUrl = serverUrl,
                rtmpStreamKey = streamKey,
                snackbarMessage = "RTMP credentials updated"
            )
        }
    }

    fun openGoLivePreparation() {
        _uiState.update { it.copy(showGoLiveSheet = true) }
    }

    fun closeGoLivePreparation() {
        _uiState.update { it.copy(showGoLiveSheet = false) }
    }

    fun openLinkDialog() {
        _uiState.update { it.copy(showLinkDialog = true) }
    }

    fun closeLinkDialog() {
        _uiState.update { it.copy(showLinkDialog = false) }
    }

    fun linkChannel(channelName: String, handle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(accountStatus = YouTubeAccountStatus.Connecting) }
            delay(800)
            val cleanName = channelName.ifBlank { "Pro Streamer Live" }
            val cleanHandle = if (handle.startsWith("@")) handle else "@${handle.ifBlank { "streamerbuddy" }}"
            _uiState.update { state ->
                val updatedChecklist = state.checklist.map { item ->
                    if (item.id == "stream_key") item.copy(isReady = true, subtitle = "RTMP Key linked to $cleanHandle")
                    else item
                }
                state.copy(
                    accountStatus = YouTubeAccountStatus.Linked(
                        channelName = cleanName,
                        channelHandle = cleanHandle,
                        subscriberCount = "24.8K"
                    ),
                    showLinkDialog = false,
                    checklist = updatedChecklist,
                    snackbarMessage = "Connected to $cleanName!"
                )
            }
        }
    }

    fun unlinkChannel() {
        _uiState.update { state ->
            val updatedChecklist = state.checklist.map { item ->
                if (item.id == "stream_key") item.copy(isReady = false, subtitle = "RTMP Key unlinked")
                else item
            }
            state.copy(
                accountStatus = YouTubeAccountStatus.Unlinked(),
                checklist = updatedChecklist,
                snackbarMessage = "YouTube Account unlinked."
            )
        }
    }

    fun toggleChecklistItem(id: String) {
        _uiState.update { state ->
            val updated = state.checklist.map { item ->
                if (item.id == id) item.copy(isReady = !item.isReady) else item
            }
            state.copy(checklist = updated)
        }
    }

    fun startLiveBroadcast(context: Context, resultCode: Int, resultData: Intent) {
        _uiState.update { it.copy(showGoLiveSheet = false) }

        val serviceIntent = Intent(context, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_START_BROADCAST
            putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordingService.EXTRA_RESULT_DATA, resultData)
            putExtra(ScreenRecordingService.EXTRA_RTMP_URL, _uiState.value.rtmpServerUrl)
            putExtra(ScreenRecordingService.EXTRA_STREAM_KEY, _uiState.value.rtmpStreamKey)
            putExtra(ScreenRecordingService.EXTRA_WIDTH, _uiState.value.selectedPreset.width)
            putExtra(ScreenRecordingService.EXTRA_HEIGHT, _uiState.value.selectedPreset.height)
            putExtra(ScreenRecordingService.EXTRA_BITRATE, _uiState.value.selectedPreset.bitrateKbps * 1000)
            putExtra(ScreenRecordingService.EXTRA_FPS, _uiState.value.selectedPreset.fps)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    fun stopBroadcast(context: Context) {
        val stopIntent = Intent(context, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_STOP_BROADCAST
        }
        context.startService(stopIntent)
    }

    fun stopRecording(context: Context) {
        val stopIntent = Intent(context, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_STOP
        }
        context.startService(stopIntent)
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
