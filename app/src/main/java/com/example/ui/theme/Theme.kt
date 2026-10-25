package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SyncTaskTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = ThemeManager.useDynamicSystemColors,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activeDarkScheme = darkColorScheme(
        primary = OneUiPrimary,
        onPrimary = Color.White,
        primaryContainer = DarkSurfaceElevated,
        onPrimaryContainer = OneUiPrimaryLight,
        secondary = OneUiSecondary,
        onSecondary = Color(0xFF00262B),
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
        surfaceTint = OneUiPrimary,
        outline = DarkBorder,
        outlineVariant = DarkBorderSubtle,
        error = OneUiError,
        onError = Color.White
    )

    val baseScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dynamic.copy(
            primary = OneUiPrimary,
            secondary = OneUiSecondary,
            tertiary = OneUiAccent,
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceElevated,
            outline = DarkBorder
        )
    } else {
        activeDarkScheme
    }

    MaterialTheme(
        colorScheme = baseScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Renders the signature diagonal Electric Sky-to-Cobalt geometric backdrop slash
 * inspired by the reference UI (`image_57c15e71.png`), with ambient glass glow orbs.
 */
@Composable
fun AeroGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = DarkBackground
    val primary = OneUiPrimary
    val secondary = OneUiSecondary
    val showSlash = ThemeManager.showDiagonalBackdrop

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        if (showSlash) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Signature diagonal geometric wing slicing from top-right to bottom-center/right
                val wingPath = Path().apply {
                    moveTo(w * 0.56f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h * 0.92f)
                    lineTo(w * 0.18f, h)
                    close()
                }

                drawPath(
                    path = wingPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            secondary.copy(alpha = 0.30f),
                            primary.copy(alpha = 0.36f),
                            primary.copy(alpha = 0.12f)
                        ),
                        start = Offset(w * 0.85f, 0f),
                        end = Offset(w * 0.25f, h * 0.95f)
                    )
                )

                // Subtle secondary ambient glow in top-left for glass depth
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondary.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.15f, h * 0.18f),
                        radius = w * 0.55f
                    ),
                    center = Offset(w * 0.15f, h * 0.18f),
                    radius = w * 0.55f
                )
            }
        }
        content()
    }
}

/**
 * 3D Glowing Squircle Action Button matching the reference UI's `<`, `All`, `+`, and `Add to Cart` buttons.
 */
@Composable
fun AeroSquircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    cornerRadius: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val bgBrush = if (isActive) {
        aeroActionBrush()
    } else {
        glassSurfaceBrush(alphaMultiplier = 0.95f)
    }
    val borderBrush = if (isActive) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.55f),
                OneUiSecondary.copy(alpha = 0.45f),
                OneUiPrimary.copy(alpha = 0.6f)
            )
        )
    } else {
        glassSpecularBorderBrush()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderBrush, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
        content = content
    )
}
