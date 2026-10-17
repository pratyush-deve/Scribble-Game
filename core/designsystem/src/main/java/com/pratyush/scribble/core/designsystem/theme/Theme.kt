package com.pratyush.scribble.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = VibrantViolet,
    onPrimary = TextPrimary,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = VibrantVioletLight,
    secondary = VibrantCyan,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = VibrantCyanLight,
    tertiary = VibrantMint,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun ScribbleTheme(
    content: @Composable () -> Unit
) {
    // Force dark theme as mandated by architecture requirements
    val colorScheme = DarkColorScheme

    CompositionLocalProvider(
        LocalSpacing provides Spacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ScribbleTypography,
            content = content
        )
    }
}
