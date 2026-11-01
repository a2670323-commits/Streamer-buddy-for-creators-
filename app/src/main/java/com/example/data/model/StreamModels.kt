package com.example.data.model

sealed class StreamPreset(
    val id: String,
    val name: String,
    val resolution: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrateKbps: Int,
    val audioBitrateKbps: Int,
    val tag: String,
    val description: String
) {
    data object Preset1080p : StreamPreset(
        id = "1080p_60",
        name = "1080p 60fps",
        resolution = "1920 × 1080",
        width = 1080,
        height = 1920,
        fps = 60,
        bitrateKbps = 6000,
        audioBitrateKbps = 160,
        tag = "Pro Quality",
        description = "Crisp, silky smooth broadcast for fast-paced gaming and esports."
    )

    data object Preset720p : StreamPreset(
        id = "720p_60",
        name = "720p 60fps",
        resolution = "1280 × 720",
        width = 720,
        height = 1280,
        fps = 60,
        bitrateKbps = 3500,
        audioBitrateKbps = 128,
        tag = "Balanced",
        description = "Optimal bandwidth balance for mobile networks and stable frame pacing."
    )

    data object Preset480p : StreamPreset(
        id = "480p_30",
        name = "480p 30fps",
        resolution = "854 × 480",
        width = 480,
        height = 854,
        fps = 30,
        bitrateKbps = 1500,
        audioBitrateKbps = 96,
        tag = "Low Data",
        description = "Ultra light stream profile for weak cellular connections and low heat."
    )

    companion object {
        val ALL: List<StreamPreset>
            get() = listOf(Preset1080p, Preset720p, Preset480p)
    }
}

sealed interface YouTubeAccountStatus {
    data class Unlinked(
        val message: String = "No channel currently linked"
    ) : YouTubeAccountStatus

    data object Connecting : YouTubeAccountStatus

    data class Linked(
        val channelName: String,
        val channelHandle: String,
        val subscriberCount: String,
        val streamKeyConfigured: Boolean = true
    ) : YouTubeAccountStatus
}

enum class NetworkHealth(val label: String, val colorHex: Long) {
    EXCELLENT("EXCELLENT", 0xFF00E676),
    GOOD("GOOD", 0xFF258BFF),
    UNSTABLE("UNSTABLE", 0xFFFF8717),
    CRITICAL("CRITICAL", 0xFFFF3B30)
}

data class NetworkTelemetry(
    val bitrateKbps: Int = 0,
    val fps: Int = 0,
    val droppedPackets: Int = 0,
    val rttMs: Long = 28L,
    val health: NetworkHealth = NetworkHealth.EXCELLENT
)

sealed interface BroadcastState {
    data object Offline : BroadcastState
    data object Preparing : BroadcastState
    data class Connecting(val endpointUrl: String = "rtmp://a.rtmp.youtube.com/live2") : BroadcastState
    data class Reconnecting(val attempt: Int = 1, val reason: String = "Network jitter") : BroadcastState
    data class Live(
        val durationSeconds: Long = 0,
        val viewerCount: Int = 142,
        val currentBitrateKbps: Int = 5980,
        val currentFps: Int = 60,
        val droppedFrames: Int = 0,
        val networkHealth: NetworkHealth = NetworkHealth.EXCELLENT
    ) : BroadcastState
}

data class RtmpStreamConfig(
    val serverUrl: String = "rtmp://a.rtmp.youtube.com/live2",
    val streamKey: String = "live_948293481_xK9mPzQa",
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 60,
    val videoBitrateKbps: Int = 6000,
    val audioBitrateKbps: Int = 160
)

data class ChecklistItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val isReady: Boolean = false
)
