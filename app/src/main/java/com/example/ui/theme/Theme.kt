package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val OneUiDarkColorScheme = darkColorScheme(
    primary = OneUiPrimary,
    onPrimary = Color.White,
    primaryContainer = OneUiPrimary.copy(alpha = 0.2f),
    onPrimaryContainer = OneUiPrimaryLight,
    secondary = OneUiSecondary,
    onSecondary = Color.Black,
    secondaryContainer = OneUiSecondary.copy(alpha = 0.15f),
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

private val OneUiLightColorScheme = lightColorScheme(
    primary = OneUiPrimary,
    onPrimary = Color.White,
    secondary = OneUiSecondary,
    onSecondary = Color.Black,
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF1E212D),
    surface = Color.White,
    onSurface = Color(0xFF1E212D),
    surfaceVariant = Color(0xFFEFF1F5),
    onSurfaceVariant = Color(0xFF5A6072),
    outline = Color(0xFFD6DAE5),
    error = OneUiError
)

@Composable
fun SyncTaskTheme(
    darkTheme: Boolean = true, // Default to dark mode as requested
    dynamicColor: Boolean = false, // Keep bespoke One UI styling consistent
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OneUiDarkColorScheme else OneUiLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
