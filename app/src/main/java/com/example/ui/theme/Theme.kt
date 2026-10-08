package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProPhotographyDarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF001F28),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = AmberAccent,
    onSecondary = Color(0xFF261A00),
    secondaryContainer = Color(0xFF4C3600),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = CyanAccentDark,
    onTertiary = Color.White,
    background = DarkSurfaceBackground,
    onBackground = TextPrimary,
    surface = DarkSurfaceElevated,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder,
    error = StatusDisconnected,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to Pro Dark theme for photo fidelity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ProPhotographyDarkColorScheme,
        typography = Typography,
        content = content
    )
}
