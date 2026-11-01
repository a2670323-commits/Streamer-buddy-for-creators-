package com.example.ui.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.ScreenRecordingService
import com.example.ui.components.GoLiveCard
import com.example.ui.components.GoLiveReadinessSheet
import com.example.ui.components.LinkChannelDialog
import com.example.ui.components.QuickShortcutsGrid
import com.example.ui.components.StreamPresetsSection
import com.example.ui.components.StreamerAppBar
import com.example.ui.components.YouTubeStatusCard
import com.example.ui.theme.StreamerDarkBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToVoiceRoom: () -> Unit,
    onNavigateToHudStudio: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAiCopilot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    val mediaProjectionManager = remember {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    var pendingCaptureMode by remember { mutableStateOf(CaptureMode.RECORDING) }

    // MediaProjection screen capture intent launcher
    val screenRecordingLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intentData = result.data
        if (result.resultCode == Activity.RESULT_OK && intentData != null) {
            if (pendingCaptureMode == CaptureMode.BROADCAST) {
                viewModel.startLiveBroadcast(context, result.resultCode, intentData)
            } else {
                val serviceIntent = Intent(context, ScreenRecordingService::class.java).apply {
                    action = ScreenRecordingService.ACTION_START
                    putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenRecordingService.EXTRA_RESULT_DATA, intentData)
                    putExtra(ScreenRecordingService.EXTRA_WIDTH, uiState.selectedPreset.width)
                    putExtra(ScreenRecordingService.EXTRA_HEIGHT, uiState.selectedPreset.height)
                    putExtra(ScreenRecordingService.EXTRA_BITRATE, uiState.selectedPreset.bitrateKbps * 1000)
                    putExtra(ScreenRecordingService.EXTRA_FPS, uiState.selectedPreset.fps)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }

    // Permissions launcher for Microphone Commentary & System Notifications
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        screenRecordingLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            StreamerAppBar(
                broadcastState = uiState.broadcastState,
                onOpenAiCopilot = onNavigateToAiCopilot
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = StreamerDarkBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_root")
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StreamerDarkBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp) // Large screen adaptive constraint
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. YouTube Account Status Card
                YouTubeStatusCard(
                    status = uiState.accountStatus,
                    onLinkClick = { viewModel.openLinkDialog() },
                    onUnlinkClick = { viewModel.unlinkChannel() }
                )

                // 2. Main Action Card: GO LIVE & Screen Recording
                GoLiveCard(
                    broadcastState = uiState.broadcastState,
                    selectedPreset = uiState.selectedPreset,
                    isScreenRecording = uiState.isScreenRecording,
                    recordingDurationSeconds = uiState.recordingDurationSeconds,
                    onGoLiveClick = { viewModel.openGoLivePreparation() },
                    onEndStreamClick = { viewModel.stopBroadcast(context) },
                    onStartRecordingClick = {
                        pendingCaptureMode = CaptureMode.RECORDING
                        val pendingPermissions = buildList {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                add(Manifest.permission.RECORD_AUDIO)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }

                        if (pendingPermissions.isNotEmpty()) {
                            permissionsLauncher.launch(pendingPermissions.toTypedArray())
                        } else {
                            screenRecordingLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                        }
                    },
                    onStopRecordingClick = { viewModel.stopRecording(context) }
                )

                // 3. Streaming Presets Section (1080p 60fps, 720p 60fps, 480p 30fps)
                StreamPresetsSection(
                    selectedPreset = uiState.selectedPreset,
                    onPresetSelected = { viewModel.selectPreset(it) }
                )

                // 4. Quick Shortcuts Section (Voice Room, HUD Studio, Settings, AI)
                QuickShortcutsGrid(
                    onNavigateToVoiceRoom = onNavigateToVoiceRoom,
                    onNavigateToHudStudio = onNavigateToHudStudio,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToAiCopilot = onNavigateToAiCopilot
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Bottom Sheet: Pre-stream preparation checklist
    if (uiState.showGoLiveSheet) {
        GoLiveReadinessSheet(
            sheetState = sheetState,
            checklist = uiState.checklist,
            selectedPreset = uiState.selectedPreset,
            onToggleItem = { viewModel.toggleChecklistItem(it) },
            onConfirmGoLive = {
                viewModel.closeGoLivePreparation()
                pendingCaptureMode = CaptureMode.BROADCAST
                val pendingPermissions = buildList {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                if (pendingPermissions.isNotEmpty()) {
                    permissionsLauncher.launch(pendingPermissions.toTypedArray())
                } else {
                    screenRecordingLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                }
            },
            onDismiss = { viewModel.closeGoLivePreparation() }
        )
    }

    // Dialog: Link YouTube Channel
    if (uiState.showLinkDialog) {
        LinkChannelDialog(
            onDismiss = { viewModel.closeLinkDialog() },
            onConfirm = { name, handle -> viewModel.linkChannel(name, handle) }
        )
    }
}

private enum class CaptureMode {
    RECORDING,
    BROADCAST
}
