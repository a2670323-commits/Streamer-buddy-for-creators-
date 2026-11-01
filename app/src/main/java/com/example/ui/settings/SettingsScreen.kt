package com.example.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.home.HomeViewModel
import com.example.ui.theme.StreamerBlueSecondary
import com.example.ui.theme.StreamerBorder
import com.example.ui.theme.StreamerDarkBackground
import com.example.ui.theme.StreamerOrangePrimary
import com.example.ui.theme.StreamerSuccessGreen
import com.example.ui.theme.StreamerSurface
import com.example.ui.theme.StreamerSurfaceVariant
import com.example.ui.theme.StreamerTextMuted
import com.example.ui.theme.StreamerTextPrimary
import com.example.ui.theme.StreamerTextSecondary

@Composable
fun SettingsScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var serverUrl by remember(uiState.rtmpServerUrl) { mutableStateOf(uiState.rtmpServerUrl) }
    var streamKey by remember(uiState.rtmpStreamKey) { mutableStateOf(uiState.rtmpStreamKey) }
    var streamKeyVisible by remember { mutableStateOf(false) }
    var lowLatencyMode by remember { mutableStateOf(true) }
    var hardwareEncoder by remember { mutableStateOf(true) }

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
                            .testTag("btn_settings_back")
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
                        text = "Broadcast & Ingest Settings",
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
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // RTMP Credentials Header
                Text(
                    text = "RTMP INGEST CONFIGURATION",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = StreamerTextMuted,
                        letterSpacing = 1.sp
                    )
                )

                Surface(
                    color = StreamerSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StreamerBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Quick Presets Chips
                        Text(
                            text = "QUICK ENDPOINTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StreamerTextMuted
                            )
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = serverUrl == "rtmp://a.rtmp.youtube.com/live2",
                                onClick = { serverUrl = "rtmp://a.rtmp.youtube.com/live2" },
                                label = { Text("YouTube Primary", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StreamerOrangePrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = StreamerOrangePrimary
                                )
                            )
                            FilterChip(
                                selected = serverUrl == "rtmp://b.rtmp.youtube.com/live2?backup=1",
                                onClick = { serverUrl = "rtmp://b.rtmp.youtube.com/live2?backup=1" },
                                label = { Text("YouTube Backup", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StreamerBlueSecondary.copy(alpha = 0.2f),
                                    selectedLabelColor = StreamerBlueSecondary
                                )
                            )
                        }

                        // RTMP URL Input
                        Text(
                            text = "RTMP SERVER URL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StreamerTextMuted
                            )
                        )
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = StreamerBlueSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StreamerOrangePrimary,
                                unfocusedBorderColor = StreamerBorder,
                                focusedTextColor = StreamerTextPrimary,
                                unfocusedTextColor = StreamerTextPrimary,
                                focusedContainerColor = StreamerSurfaceVariant,
                                unfocusedContainerColor = StreamerSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_rtmp_url")
                        )

                        // Stream Key Input
                        Text(
                            text = "STREAM KEY (PRIVATE)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StreamerTextMuted
                            )
                        )
                        OutlinedTextField(
                            value = streamKey,
                            onValueChange = { streamKey = it },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = StreamerOrangePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            visualTransformation = if (streamKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { streamKeyVisible = !streamKeyVisible }) {
                                    Icon(
                                        imageVector = if (streamKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility",
                                        tint = StreamerTextMuted
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StreamerOrangePrimary,
                                unfocusedBorderColor = StreamerBorder,
                                focusedTextColor = StreamerTextPrimary,
                                unfocusedTextColor = StreamerTextPrimary,
                                focusedContainerColor = StreamerSurfaceVariant,
                                unfocusedContainerColor = StreamerSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_stream_key")
                        )

                        // Save Configuration Button
                        Button(
                            onClick = {
                                viewModel.updateStreamSettings(serverUrl, streamKey)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StreamerOrangePrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_save_rtmp_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAVE RTMP CREDENTIALS",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Video Encoding
                Text(
                    text = "ENCODER & TELEMETRY",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = StreamerTextMuted,
                        letterSpacing = 1.sp
                    )
                )

                Surface(
                    color = StreamerSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StreamerBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hardware Acceleration (AVC / H.264)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StreamerTextPrimary
                                    )
                                )
                                Text(
                                    text = "Device MediaCodec hardware video encoding",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = StreamerTextSecondary
                                    )
                                )
                            }

                            Switch(
                                checked = hardwareEncoder,
                                onCheckedChange = { hardwareEncoder = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = StreamerOrangePrimary,
                                    uncheckedThumbColor = StreamerTextSecondary,
                                    uncheckedTrackColor = StreamerSurfaceVariant
                                )
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ultra Low Latency Mode",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StreamerTextPrimary
                                    )
                                )
                                Text(
                                    text = "Minimizes ingest buffer for fast viewer chat interaction",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = StreamerTextSecondary
                                    )
                                )
                            }

                            Switch(
                                checked = lowLatencyMode,
                                onCheckedChange = { lowLatencyMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = StreamerBlueSecondary,
                                    uncheckedThumbColor = StreamerTextSecondary,
                                    uncheckedTrackColor = StreamerSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
