package com.loe159.rekordbot.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val RekordbotDarkColorScheme = darkColorScheme(
    primary = RekordbotPrimary,
    onPrimary = RekordbotOnPrimary,
    primaryContainer = RekordbotPrimary.copy(alpha = 0.18f),
    onPrimaryContainer = RekordbotOnBackground,
    background = RekordbotBackground,
    onBackground = RekordbotOnBackground,
    surface = RekordbotSurface,
    onSurface = RekordbotOnBackground,
    surfaceVariant = RekordbotSurfaceRaised,
    onSurfaceVariant = RekordbotMutedText,
    outline = RekordbotBorder,
    error = RekordbotError,
)

@Composable
fun RekordbotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RekordbotDarkColorScheme,
        typography = RekordbotTypography,
        shapes = RekordbotShapes,
        content = content,
    )
}

