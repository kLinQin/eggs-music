package com.eggs.musicplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    secondary = Accent,
    onSecondary = Color.Black,
    background = Bg,
    onBackground = TextMain,
    surface = BgElev,
    onSurface = TextMain,
    surfaceVariant = BgPanel,
    onSurfaceVariant = TextMute,
    outline = Border,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun EggMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
