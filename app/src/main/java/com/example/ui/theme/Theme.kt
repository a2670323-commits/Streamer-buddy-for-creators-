package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StreamerOrangePrimary,
    onPrimary = Color(0xFF000000),
    primaryContainer = StreamerOrangeContainer,
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = StreamerBlueSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = StreamerBlueContainer,
    onSecondaryContainer = Color(0xFFD4E3FF),
    tertiary = StreamerCyanAccent,
    onTertiary = Color(0xFF000000),
    background = StreamerDarkBackground,
    onBackground = StreamerTextPrimary,
    surface = StreamerSurface,
    onSurface = StreamerTextPrimary,
    surfaceVariant = StreamerSurfaceVariant,
    onSurfaceVariant = StreamerTextSecondary,
    surfaceContainer = StreamerSurfaceContainer,
    surfaceContainerHigh = StreamerSurfaceContainerHigh,
    outline = StreamerBorder,
    outlineVariant = StreamerBorderSubtle,
    error = StreamerLiveRed,
    onError = Color.White
)

@Composable
fun StreamerBuddyTheme(
    content: @Composable () -> Unit
) {
    // Streamer Buddy uses the custom dark theme as its signature aesthetic
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    StreamerBuddyTheme(content = content)
}
