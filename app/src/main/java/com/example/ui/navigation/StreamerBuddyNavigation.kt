package com.example.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.assistant.AiCopilotScreen
import com.example.ui.assistant.AiCopilotViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.hud.HudStudioScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.StreamerBlueSecondary
import com.example.ui.theme.StreamerBorder
import com.example.ui.theme.StreamerDarkBackground
import com.example.ui.theme.StreamerOrangePrimary
import com.example.ui.theme.StreamerSurface
import com.example.ui.theme.StreamerTextMuted
import com.example.ui.theme.StreamerTextPrimary
import com.example.ui.theme.StreamerTextSecondary
import com.example.ui.voice.VoiceRoomScreen

enum class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Default.Podcasts),
    HUD_STUDIO("hud_studio", "Studio", Icons.AutoMirrored.Filled.ViewQuilt),
    VOICE_ROOM("voice_room", "Voice", Icons.Default.Mic),
    AI_COPILOT("ai_copilot", "AI Co-Pilot", Icons.Default.AutoAwesome),
    SETTINGS("settings", "Settings", Icons.Default.Home)
}

@Composable
fun StreamerBuddyApp(
    homeViewModel: HomeViewModel = viewModel(),
    aiCopilotViewModel: AiCopilotViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    Scaffold(
        bottomBar = {
            // Only show bottom navigation on main tabs
            if (currentScreen != Screen.SETTINGS) {
                Surface(
                    color = StreamerSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .border(1.dp, StreamerBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    NavigationBar(
                        containerColor = StreamerSurface,
                        contentColor = StreamerTextPrimary,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .testTag("streamer_bottom_nav")
                    ) {
                        val navItems = listOf(
                            Screen.HOME,
                            Screen.HUD_STUDIO,
                            Screen.VOICE_ROOM,
                            Screen.AI_COPILOT
                        )

                        navItems.forEach { screen ->
                            val selected = currentScreen == screen
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = StreamerOrangePrimary,
                                    indicatorColor = StreamerOrangePrimary,
                                    unselectedIconColor = StreamerTextMuted,
                                    unselectedTextColor = StreamerTextMuted
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.route}")
                            )
                        }
                    }
                }
            }
        },
        containerColor = StreamerDarkBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StreamerDarkBackground)
        ) {
            Crossfade(
                targetState = currentScreen,
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToVoiceRoom = { currentScreen = Screen.VOICE_ROOM },
                        onNavigateToHudStudio = { currentScreen = Screen.HUD_STUDIO },
                        onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                        onNavigateToAiCopilot = { currentScreen = Screen.AI_COPILOT }
                    )

                    Screen.HUD_STUDIO -> HudStudioScreen(
                        onNavigateBack = { currentScreen = Screen.HOME }
                    )

                    Screen.VOICE_ROOM -> VoiceRoomScreen(
                        onNavigateBack = { currentScreen = Screen.HOME }
                    )

                    Screen.AI_COPILOT -> AiCopilotScreen(
                        viewModel = aiCopilotViewModel,
                        onNavigateBack = { currentScreen = Screen.HOME }
                    )

                    Screen.SETTINGS -> SettingsScreen(
                        viewModel = homeViewModel,
                        onNavigateBack = { currentScreen = Screen.HOME }
                    )
                }
            }
        }
    }
}
