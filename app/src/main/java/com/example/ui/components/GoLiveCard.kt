package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BroadcastState
import com.example.data.model.NetworkHealth
import com.example.data.model.StreamPreset
import com.example.ui.theme.StreamerBlueSecondary
import com.example.ui.theme.StreamerBorder
import com.example.ui.theme.StreamerLiveRed
import com.example.ui.theme.StreamerOrangePrimary
import com.example.ui.theme.StreamerSuccessGreen
import com.example.ui.theme.StreamerSurface
import com.example.ui.theme.StreamerSurfaceVariant
import com.example.ui.theme.StreamerTextMuted
import com.example.ui.theme.StreamerTextPrimary
import com.example.ui.theme.StreamerTextSecondary
import java.util.Locale

@Composable
fun GoLiveCard(
    broadcastState: BroadcastState,
    selectedPreset: StreamPreset,
    isScreenRecording: Boolean,
    recordingDurationSeconds: Long,
    onGoLiveClick: () -> Unit,
    onEndStreamClick: () -> Unit,
    onStartRecordingClick: () -> Unit,
    onStopRecordingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = broadcastState is BroadcastState.Live
    val isConnecting = broadcastState is BroadcastState.Connecting
    val isReconnecting = broadcastState is BroadcastState.Reconnecting

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val livePulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "livePulse"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StreamerSurface),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isLive || isScreenRecording || isConnecting || isReconnecting) 2.dp else 1.5.dp,
                brush = when {
                    isScreenRecording -> Brush.linearGradient(listOf(StreamerLiveRed, StreamerBlueSecondary))
                    isLive -> Brush.linearGradient(listOf(StreamerLiveRed, StreamerOrangePrimary))
                    isConnecting || isReconnecting -> Brush.linearGradient(listOf(StreamerOrangePrimary, StreamerBlueSecondary))
                    else -> Brush.linearGradient(listOf(StreamerOrangePrimary, StreamerOrangePrimary.copy(alpha = 0.3f)))
                },
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(
                elevation = if (isLive || isScreenRecording) 12.dp else 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isScreenRecording) StreamerLiveRed else StreamerOrangePrimary,
                spotColor = if (isScreenRecording) StreamerLiveRed else StreamerOrangePrimary
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = when {
                            isScreenRecording -> listOf(StreamerLiveRed.copy(alpha = 0.15f), StreamerSurface)
                            isLive -> listOf(StreamerLiveRed.copy(alpha = 0.15f), StreamerSurface)
                            isConnecting || isReconnecting -> listOf(StreamerOrangePrimary.copy(alpha = 0.15f), StreamerSurface)
                            else -> listOf(StreamerOrangePrimary.copy(alpha = 0.12f), StreamerSurface)
                        }
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isScreenRecording -> StreamerLiveRed.copy(alpha = 0.2f)
                                        isLive -> StreamerLiveRed.copy(alpha = 0.2f)
                                        isConnecting || isReconnecting -> StreamerOrangePrimary.copy(alpha = 0.2f)
                                        else -> StreamerOrangePrimary.copy(alpha = 0.2f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isLive || isScreenRecording) StreamerLiveRed else StreamerOrangePrimary,
                                    RoundedCornerShape(10.dp)
                                )
                        ) {
                            Icon(
                                imageVector = if (isScreenRecording) Icons.Default.Videocam else Icons.Default.Podcasts,
                                contentDescription = null,
                                tint = if (isLive || isScreenRecording) StreamerLiveRed else StreamerOrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = when {
                                    isScreenRecording -> "RECORDING SCREEN (H.264)"
                                    isConnecting -> "CONNECTING TO RTMP INGEST"
                                    isReconnecting -> "RECONNECTING TO SERVER"
                                    isLive -> "LIVE ON YOUTUBE (RTMP)"
                                    else -> "BROADCAST & CAPTURE"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        isScreenRecording || isLive -> StreamerLiveRed
                                        isConnecting || isReconnecting -> StreamerOrangePrimary
                                        else -> StreamerOrangePrimary
                                    },
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "${selectedPreset.name} • ${selectedPreset.bitrateKbps} Kbps",
                                style = MaterialTheme.typography.bodySmall.copy(color = StreamerTextSecondary)
                            )
                        }
                    }

                    // Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isScreenRecording || isLive -> StreamerLiveRed
                                    isConnecting || isReconnecting -> StreamerOrangePrimary.copy(alpha = 0.2f)
                                    else -> StreamerOrangePrimary.copy(alpha = 0.15f)
                                }
                            )
                            .border(
                                1.dp,
                                if (isLive || isScreenRecording) StreamerLiveRed else StreamerOrangePrimary.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isLive || isScreenRecording) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .alpha(livePulse)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = when {
                                    isScreenRecording -> "REC"
                                    isLive -> "LIVE"
                                    isConnecting -> "CONNECTING"
                                    isReconnecting -> "RETRY"
                                    else -> selectedPreset.tag
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLive || isScreenRecording) Color.White else StreamerOrangePrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Condition 1: RTMP Connecting or Reconnecting
                if (isConnecting || isReconnecting) {
                    Surface(
                        color = StreamerSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StreamerBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = StreamerOrangePrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isConnecting) "Negotiating RTMP Handshake & Stream Ingest..." else "Network drop detected, reconnecting...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = StreamerTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Endpoint: a.rtmp.youtube.com:1935/live2",
                                    style = MaterialTheme.typography.bodySmall.copy(color = StreamerTextMuted)
                                )
                            }
                        }
                    }
                }
                // Condition 2: Screen Recording Active
                else if (isScreenRecording) {
                    val minutes = recordingDurationSeconds / 60
                    val seconds = recordingDurationSeconds % 60
                    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

                    Surface(
                        color = StreamerSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StreamerLiveRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StreamerLiveRed)
                                            .alpha(livePulse)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "RECORDING TIME",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = StreamerTextMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = timeFormatted,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StreamerTextPrimary
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "DESTINATION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = StreamerTextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Movies/StreamBuddy",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = StreamerBlueSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onStopRecordingClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamerLiveRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .testTag("btn_stop_recording")
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STOP RECORDING",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
                // Condition 3: RTMP Live Broadcast Active
                else if (broadcastState is BroadcastState.Live) {
                    val hours = broadcastState.durationSeconds / 3600
                    val minutes = (broadcastState.durationSeconds % 3600) / 60
                    val seconds = broadcastState.durationSeconds % 60
                    val formattedTime = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

                    Surface(
                        color = StreamerSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StreamerBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Row 1: Duration, Viewers, Health
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "STREAM TIME",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = StreamerTextMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = StreamerTextPrimary
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "VIEWERS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = StreamerTextMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = StreamerOrangePrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${broadcastState.viewerCount}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = StreamerOrangePrimary
                                            )
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "NETWORK HEALTH",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = StreamerTextMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(broadcastState.networkHealth.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = broadcastState.networkHealth.label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(broadcastState.networkHealth.colorHex)
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Row 2: Live RTMP Telemetry (Bitrate, FPS, Packet Drops)
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StreamerSurface)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Bitrate: ${broadcastState.currentBitrateKbps} Kbps",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = StreamerBlueSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "FPS: ${broadcastState.currentFps}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = StreamerTextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Drops: ${broadcastState.droppedFrames}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (broadcastState.droppedFrames > 0) StreamerLiveRed else StreamerSuccessGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // End Stream Button
                    Button(
                        onClick = onEndStreamClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamerLiveRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .testTag("btn_end_stream")
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "END BROADCAST",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
                // Condition 4: Offline / Ready State
                else {
                    Text(
                        text = "Broadcast live to YouTube via RTMP or capture your screen directly to device storage.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = StreamerTextSecondary,
                            lineHeight = 19.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Prominent GO LIVE Launch Button
                    Button(
                        onClick = onGoLiveClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamerOrangePrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .testTag("btn_go_live")
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Podcasts,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.btn_go_live),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = Color.Black
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prominent START RECORDING Button
                    Button(
                        onClick = onStartRecordingClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StreamerBlueSecondary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .testTag("btn_start_recording")
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "START RECORDING (H.264 MP4)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
