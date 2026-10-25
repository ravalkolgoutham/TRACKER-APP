package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class AppThemePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val bg: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val primary: Color,
    val primaryLight: Color,
    val secondary: Color,
    val accent: Color
) {
    AERO_COBALT_GLASS(
        id = "aero_cobalt",
        title = "Aero Cobalt Glass",
        subtitle = "Deep Slate Navy • Electric Sky Cyan & Royal Cobalt Wing",
        bg = Color(0xFF181E29),
        surface = Color(0xFF222B3B),
        surfaceElevated = Color(0xFF2B364B),
        border = Color(0xFF45587A),
        primary = Color(0xFF4B4CED),
        primaryLight = Color(0xFF689BFF),
        secondary = Color(0xFF34C8E8),
        accent = Color(0xFF38B6FF)
    ),
    COSMIC_VIOLET(
        id = "cosmic_violet",
        title = "Cosmic Violet Neon",
        subtitle = "Royal Indigo • Electric Violet & Cyber Aqua",
        bg = Color(0xFF0A0D20),
        surface = Color(0xFF141834),
        surfaceElevated = Color(0xFF1E244B),
        border = Color(0xFF3A4382),
        primary = Color(0xFF7C4DFF),
        primaryLight = Color(0xFFB388FF),
        secondary = Color(0xFF00E5FF),
        accent = Color(0xFFFF4081)
    ),
    EMERALD_CYBER(
        id = "emerald_cyber",
        title = "Emerald Cyber Glass",
        subtitle = "Obsidian Teal • Neon Mint & Glacial Cyan",
        bg = Color(0xFF0E191F),
        surface = Color(0xFF162730),
        surfaceElevated = Color(0xFF1F3642),
        border = Color(0xFF355C6E),
        primary = Color(0xFF00BFA5),
        primaryLight = Color(0xFF64FFDA),
        secondary = Color(0xFF00E676),
        accent = Color(0xFF00E5FF)
    ),
    SUNSET_QUARTZ(
        id = "sunset_quartz",
        title = "Sunset Rose Quartz",
        subtitle = "Velvet Plum • Coral Magenta & Solar Amber",
        bg = Color(0xFF1A1221),
        surface = Color(0xFF281B33),
        surfaceElevated = Color(0xFF362546),
        border = Color(0xFF63437D),
        primary = Color(0xFFFF4081),
        primaryLight = Color(0xFFFF80AB),
        secondary = Color(0xFFFFAB00),
        accent = Color(0xFFE040FB)
    ),
    AMOLED_ICE(
        id = "amoled_ice",
        title = "AMOLED Midnight Ice",
        subtitle = "True Pitch Black • Crystal Cyan & Sapphire",
        bg = Color(0xFF05070C),
        surface = Color(0xFF101522),
        surfaceElevated = Color(0xFF182034),
        border = Color(0xFF2E3F66),
        primary = Color(0xFF2979FF),
        primaryLight = Color(0xFF82B1FF),
        secondary = Color(0xFF00E5FF),
        accent = Color(0xFF00E676)
    )
}

object ThemeManager {
    var currentPreset by mutableStateOf(AppThemePreset.AERO_COBALT_GLASS)
    var glassAlpha by mutableFloatStateOf(0.74f)
    var showDiagonalBackdrop by mutableStateOf(true)
    var enableSpecularRim by mutableStateOf(true)
    var useDynamicSystemColors by mutableStateOf(false)

    fun applyPreset(preset: AppThemePreset) {
        currentPreset = preset
        DarkBackground = preset.bg
        DarkSurface = preset.surface
        DarkSurfaceElevated = preset.surfaceElevated
        DarkBorder = preset.border
        OneUiPrimary = preset.primary
        OneUiPrimaryLight = preset.primaryLight
        OneUiSecondary = preset.secondary
        OneUiAccent = preset.accent
    }
}

// Reactive Theme Tokens (backed by Compose MutableState so every screen updates immediately)
var DarkBackground by mutableStateOf(AppThemePreset.AERO_COBALT_GLASS.bg)
var DarkSurface by mutableStateOf(AppThemePreset.AERO_COBALT_GLASS.surface)
var DarkSurfaceElevated by mutableStateOf(AppThemePreset.AERO_COBALT_GLASS.surfaceElevated)
var DarkSurfaceVariant by mutableStateOf(Color(0xFF323F59))
var DarkBorder by mutableStateOf(AppThemePreset.AERO_COBALT_GLASS.border)
var DarkBorderSubtle by mutableStateOf(Color(0xFF263147))

var OneUiPrimary by mutableStateOf(AppThemePreset.AERO_COBALT_GLASS.primary)
var OneUiPrimaryLight by mutableStateOf(AppThemePreset.AERO_COBALT_GLASS.primaryLight)
var OneUiSecondary by mutableStateOf(AppThemePreset.AERO_COBALT_GLASS.secondary)
var OneUiAccent by mutableStateOf(AppThemePreset.AERO_COBALT_GLASS.accent)

val OneUiSuccess = Color(0xFF00E676)
val OneUiWarning = Color(0xFFFFAB00)
val OneUiError = Color(0xFFFF5252)

// Typography Text Hierarchy with Crisp Contrast
val TextPrimary = Color(0xFFF8FAFF)
val TextSecondary = Color(0xFFB8C6E0)
val TextTertiary = Color(0xFF7B8DB0)

// High-Saturation Category Accents
val CategoryWork = Color(0xFF38B6FF)
val CategoryPersonal = Color(0xFF00E676)
val CategoryFocus = Color(0xFF7C4DFF)
val CategoryHealth = Color(0xFFFF4081)
val CategoryMeeting = Color(0xFFFFAB00)

// Glassmorphic & Action Gradient Helpers matching the reference UI
fun aeroActionBrush(): Brush = Brush.linearGradient(
    colors = listOf(OneUiSecondary, OneUiPrimary),
    start = Offset(0f, 0f),
    end = Offset(240f, 240f)
)

fun glassSurfaceBrush(alphaMultiplier: Float = 1f): Brush {
    val alpha = (ThemeManager.glassAlpha * alphaMultiplier).coerceIn(0.35f, 0.96f)
    return Brush.linearGradient(
        colors = listOf(
            DarkSurfaceElevated.copy(alpha = alpha),
            DarkSurface.copy(alpha = (alpha + 0.08f).coerceAtMost(0.98f))
        ),
        start = Offset(0f, 0f),
        end = Offset(400f, 500f)
    )
}

fun glassSpecularBorderBrush(accent: Color = OneUiSecondary): Brush {
    return if (ThemeManager.enableSpecularRim) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.42f),
                accent.copy(alpha = 0.38f),
                DarkBorder.copy(alpha = 0.25f)
            ),
            start = Offset(0f, 0f),
            end = Offset(350f, 350f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(DarkBorder, DarkBorder)
        )
    }
}
