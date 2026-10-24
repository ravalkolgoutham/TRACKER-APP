package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OneUiDarkColorScheme = darkColorScheme(
    primary = OneUiPrimary,
    onPrimary = Color.Black,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = Color.White,
    secondary = OneUiSecondary,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = OneUiSecondary,
    tertiary = OneUiAccent,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = OneUiError,
    onError = Color.White
)

private val OneUiLightColorScheme = darkColorScheme(
    primary = OneUiPrimary,
    onPrimary = Color.Black,
    secondary = OneUiSecondary,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = OneUiError
)

@Composable
fun SyncTaskTheme(
    darkTheme: Boolean = true, // Strict dark black mode as requested
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OneUiDarkColorScheme,
        typography = Typography,
        content = content
    )
}
