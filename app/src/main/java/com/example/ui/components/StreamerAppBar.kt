package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BroadcastState
import com.example.ui.theme.StreamerBorder
import com.example.ui.theme.StreamerLiveRed
import com.example.ui.theme.StreamerOrangePrimary
import com.example.ui.theme.StreamerSurface
import com.example.ui.theme.StreamerTextMuted
import com.example.ui.theme.StreamerTextPrimary
import com.example.ui.theme.StreamerTextSecondary

@Composable
fun StreamerAppBar(
    broadcastState: BroadcastState,
    onOpenAiCopilot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLive = broadcastState is BroadcastState.Live

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        color = StreamerSurface.copy(alpha = 0.95f),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = StreamerBorder.copy(alpha = 0.7f),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Brand Logo & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        StreamerOrangePrimary.copy(alpha = 0.25f),
                                        Color(0xFF258BFF).copy(alpha = 0.25f)
                                    )
                                )
                            )
                            .border(1.dp, StreamerOrangePrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Streamer Buddy",
                            tint = StreamerOrangePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.app_bar_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = StreamerTextPrimary,
                                letterSpacing = (-0.2).sp
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Pulsing dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isLive) StreamerLiveRed else StreamerTextMuted)
                                    .alpha(if (isLive) pulseAlpha else 1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLive) "LIVE BROADCAST ACTIVE" else "STUDIO READY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLive) StreamerLiveRed else StreamerTextSecondary
                                )
                            )
                        }
                    }
                }

                // AI Co-Pilot Quick Button
                IconButton(
                    onClick = onOpenAiCopilot,
                    modifier = Modifier
                        .testTag("app_bar_copilot_button")
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(StreamerOrangePrimary.copy(alpha = 0.15f))
                        .border(1.dp, StreamerOrangePrimary.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Stream Assistant",
                        tint = StreamerOrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
