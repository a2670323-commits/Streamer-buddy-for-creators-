package com.example.ui.voice

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlin.math.roundToInt

@Composable
fun VoiceRoomScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    var micVolume by remember { mutableFloatStateOf(0.85f) }
    var gameVolume by remember { mutableFloatStateOf(0.65f) }
    var musicVolume by remember { mutableFloatStateOf(0.40f) }
    var noiseSuppression by remember { mutableStateOf(true) }
    var soundTriggerMessage by remember { mutableStateOf<String?>(null) }

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
                            .testTag("btn_voice_back")
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
                        text = "Voice Room & Audio Mixer",
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Audio Channels Section
            Text(
                text = "AUDIO CHANNELS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = StreamerTextMuted
                )
            )

            AudioChannelSlider(
                title = "Primary Microphone",
                icon = Icons.Default.Mic,
                volume = micVolume,
                accentColor = StreamerOrangePrimary,
                onVolumeChange = { micVolume = it }
            )

            AudioChannelSlider(
                title = "Game & System Audio",
                icon = Icons.Default.SportsEsports,
                volume = gameVolume,
                accentColor = StreamerBlueSecondary,
                onVolumeChange = { gameVolume = it }
            )

            AudioChannelSlider(
                title = "Background Music (BGM)",
                icon = Icons.Default.MusicNote,
                volume = musicVolume,
                accentColor = Color(0xFF00E5FF),
                onVolumeChange = { musicVolume = it }
            )

            // AI Noise Filter
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
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = StreamerBlueSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AI Background Noise Gate",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StreamerTextPrimary
                                )
                            )
                            Text(
                                text = "Suppresses mechanical keyboards & fans",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = StreamerTextSecondary
                                )
                            )
                        }
                    }

                    Switch(
                        checked = noiseSuppression,
                        onCheckedChange = { noiseSuppression = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = StreamerBlueSecondary,
                            uncheckedThumbColor = StreamerTextSecondary,
                            uncheckedTrackColor = StreamerSurfaceVariant
                        )
                    )
                }
            }

            // Quick Soundboard Section
            Text(
                text = "STREAM SOUNDBOARD",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = StreamerTextMuted
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SoundboardButton(
                    label = "📢 Air Horn",
                    onClick = { soundTriggerMessage = "Triggered: Air Horn sound!" },
                    modifier = Modifier.weight(1f)
                )
                SoundboardButton(
                    label = "🏆 GG Cheer",
                    onClick = { soundTriggerMessage = "Triggered: GG Cheer sound!" },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SoundboardButton(
                    label = "👏 Applause",
                    onClick = { soundTriggerMessage = "Triggered: Applause sound!" },
                    modifier = Modifier.weight(1f)
                )
                SoundboardButton(
                    label = "🥁 Drum Roll",
                    onClick = { soundTriggerMessage = "Triggered: Drum Roll sound!" },
                    modifier = Modifier.weight(1f)
                )
            }

            soundTriggerMessage?.let { msg ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StreamerSurfaceVariant)
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = StreamerOrangePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioChannelSlider(
    title: String,
    icon: ImageVector,
    volume: Float,
    accentColor: Color,
    onVolumeChange: (Float) -> Unit
) {
    Surface(
        color = StreamerSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StreamerBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = StreamerTextPrimary
                        )
                    )
                }

                Text(
                    text = "${(volume * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
            }

            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = StreamerSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SoundboardButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StreamerSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(52.dp)
            .border(1.dp, StreamerBorder, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = StreamerTextPrimary
                )
            )
        }
    }
}
