package com.musicplayer.localmusicplayer.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.musicplayer.localmusicplayer.domain.model.ThemeColor
import com.musicplayer.localmusicplayer.domain.model.ThemeMode

fun buildLightColorScheme(seed: Color): ColorScheme {
    val primary = seed
    val primaryContainer = Color(
        red = (primary.red * 0.2f + 0.8f).coerceIn(0f, 1f),
        green = (primary.green * 0.2f + 0.8f).coerceIn(0f, 1f),
        blue = (primary.blue * 0.2f + 0.8f).coerceIn(0f, 1f)
    )
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = primary,
        secondary = primary,
        onSecondary = Color.White,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = primary,
        surface = Color(0xFFFEFBFF),
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
    )
}

fun buildDarkColorScheme(seed: Color): ColorScheme {
    val primary = seed.copy(alpha = 0.9f)
    val primaryContainer = Color(
        red = (seed.red * 0.3f),
        green = (seed.green * 0.3f),
        blue = (seed.blue * 0.3f)
    )
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF1C1B1F),
        primaryContainer = primaryContainer,
        onPrimaryContainer = seed.copy(alpha = 0.7f),
        secondary = seed.copy(alpha = 0.8f),
        onSecondary = Color(0xFF1C1B1F),
        secondaryContainer = primaryContainer,
        onSecondaryContainer = seed.copy(alpha = 0.6f),
        surface = Color(0xFF1C1B1F),
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
    )
}

@Composable
fun MusicPlayerTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as com.musicplayer.localmusicplayer.MusicPlayerApplication
    val themeRepository = app.themeRepository

    val themeMode by themeRepository.themeMode.collectAsState(initial = ThemeMode.System)
    val themeColor by themeRepository.themeColor.collectAsState(initial = ThemeColor.Blue)

    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val colorScheme = if (darkTheme) {
        buildDarkColorScheme(themeColor.seedColor)
    } else {
        buildLightColorScheme(themeColor.seedColor)
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
