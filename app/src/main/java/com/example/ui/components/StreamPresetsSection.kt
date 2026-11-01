package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.StreamPreset
import com.example.ui.theme.StreamerBlueSecondary
import com.example.ui.theme.StreamerBorder
import com.example.ui.theme.StreamerOrangePrimary
import com.example.ui.theme.StreamerSurface
import com.example.ui.theme.StreamerSurfaceVariant
import com.example.ui.theme.StreamerTextMuted
import com.example.ui.theme.StreamerTextPrimary
import com.example.ui.theme.StreamerTextSecondary

@Composable
fun StreamPresetsSection(
    selectedPreset: StreamPreset,
    onPresetSelected: (StreamPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = null,
                    tint = StreamerOrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_presets),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = StreamerTextPrimary
                    )
                )
            }

            Text(
                text = "${selectedPreset.bitrateKbps} Kbps Target",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = StreamerBlueSecondary
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive Preset Pills Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StreamPreset.ALL.filterNotNull().forEach { preset ->
                val isSelected = preset.id == selectedPreset?.id
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) StreamerOrangePrimary else StreamerSurfaceVariant,
                    animationSpec = tween(250),
                    label = "pillBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.Black else StreamerTextSecondary,
                    animationSpec = tween(250),
                    label = "pillText"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) StreamerOrangePrimary else StreamerBorder,
                    animationSpec = tween(250),
                    label = "pillBorder"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp) // Minimum 48dp touch target
                        .clip(RoundedCornerShape(24.dp))
                        .background(backgroundColor)
                        .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                        .clickable { onPresetSelected(preset) }
                        .testTag("preset_pill_${preset.id}")
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = textColor
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Technical Specs Detail Card
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
                    Text(
                        text = selectedPreset.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = StreamerTextSecondary,
                            lineHeight = 17.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StreamerSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    SpecItem(label = "Resolution", value = selectedPreset.resolution)
                    SpecItem(label = "Framerate", value = "${selectedPreset.fps} FPS")
                    SpecItem(label = "Video Bitrate", value = "${selectedPreset.bitrateKbps} Kbps")
                    SpecItem(label = "Audio", value = "${selectedPreset.audioBitrateKbps}k AAC")
                }
            }
        }
    }
}

@Composable
private fun SpecItem(label: String, value: String) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = StreamerTextMuted
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = StreamerTextPrimary
            )
        )
    }
}
