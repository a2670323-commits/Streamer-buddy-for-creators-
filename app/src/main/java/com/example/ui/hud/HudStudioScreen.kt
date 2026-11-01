package com.example.ui.hud

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StreamerBlueSecondary
import com.example.ui.theme.StreamerBorder
import com.example.ui.theme.StreamerDarkBackground
import com.example.ui.theme.StreamerOrangePrimary
import com.example.ui.theme.StreamerSurface
import com.example.ui.theme.StreamerSurfaceVariant
import com.example.ui.theme.StreamerTextMuted
import com.example.ui.theme.StreamerTextPrimary
import com.example.ui.theme.StreamerTextSecondary

@Composable
fun HudStudioScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    var chatOverlayEnabled by remember { mutableStateOf(true) }
    var subGoalEnabled by remember { mutableStateOf(true) }
    var alertBoxEnabled by remember { mutableStateOf(true) }
    var cameraBorderEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            Surface(
                color = StreamerSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, StreamerBorder, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .testTag("btn_hud_back")
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StreamerTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HUD Studio & Overlays",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = StreamerTextPrimary
                        )
                    )
                }
            }
        },
        containerColor = StreamerDarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview Canvas
            Surface(
                color = StreamerSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .border(1.5.dp, StreamerOrangePrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F1523), Color(0xFF162035))
                            )
                        )
                        .padding(14.dp)
                ) {
                    // Simulated overlay placement
                    Text(
                        text = "STUDIO PREVIEW (1080p)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = StreamerTextMuted
                        ),
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    if (cameraBorderEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(64.dp, 44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.5.dp, StreamerOrangePrimary, RoundedCornerShape(6.dp))
                                .background(Color(0x33FF8717)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("WEBCAM", fontSize = 8.sp, color = StreamerOrangePrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (alertBoxEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xCC080B12))
                                .border(1.dp, StreamerBlueSecondary, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "🎉 New Sub: @GamingStar99",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StreamerBlueSecondary
                            )
                        }
                    }

                    if (chatOverlayEnabled) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .width(140.dp)
                        ) {
                            Text("User1: GG nice play!", fontSize = 9.sp, color = Color.White)
                            Text("Mod2: Welcome new viewers", fontSize = 9.sp, color = StreamerOrangePrimary)
                        }
                    }

                    if (subGoalEnabled) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .width(120.dp)
                        ) {
                            Text("Goal: 25K Subs", fontSize = 9.sp, color = StreamerTextSecondary)
                            LinearProgressIndicator(
                                progress = { 0.88f },
                                color = StreamerOrangePrimary,
                                trackColor = StreamerBorder,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }

            Text(
                text = "OVERLAY COMPONENTS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = StreamerTextMuted
                )
            )

            // Toggles
            OverlayToggleItem(
                icon = Icons.Default.ChatBubble,
                title = "Live Chat Overlay",
                subtitle = "Render transparent YouTube chat directly on broadcast",
                checked = chatOverlayEnabled,
                onCheckedChange = { chatOverlayEnabled = it }
            )

            OverlayToggleItem(
                icon = Icons.Default.Campaign,
                title = "Subscriber & Tip Alerts",
                subtitle = "Popups for new members, super chats & donations",
                checked = alertBoxEnabled,
                onCheckedChange = { alertBoxEnabled = it }
            )

            OverlayToggleItem(
                icon = Icons.Default.Flag,
                title = "Subscriber Milestone Bar",
                subtitle = "Visual progress toward your next community goal",
                checked = subGoalEnabled,
                onCheckedChange = { subGoalEnabled = it }
            )

            OverlayToggleItem(
                icon = Icons.Default.Videocam,
                title = "Webcam Frame Neon Accent",
                subtitle = "Streamer Buddy neon orange frame surrounding camera",
                checked = cameraBorderEnabled,
                onCheckedChange = { cameraBorderEnabled = it }
            )
        }
    }
}

@Composable
private fun OverlayToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = StreamerSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StreamerBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StreamerSurfaceVariant)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = StreamerOrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = StreamerTextPrimary
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = StreamerTextSecondary
                        )
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = StreamerOrangePrimary,
                    uncheckedThumbColor = StreamerTextSecondary,
                    uncheckedTrackColor = StreamerSurfaceVariant
                )
            )
        }
    }
}
