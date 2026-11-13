package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GwenDarkColorScheme = darkColorScheme(
    primary = GwenCyan,
    onPrimary = GwenDeepBlack,
    primaryContainer = Color(0xFF0A2A3B),
    onPrimaryContainer = GwenCyanSoft,
    secondary = GwenViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF281340),
    onSecondaryContainer = Color(0xFFE3C8FF),
    tertiary = GwenElectricBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF102447),
    onTertiaryContainer = Color(0xFFC4DCFF),
    background = GwenDeepBlack,
    onBackground = GwenTextPrimary,
    surface = GwenObsidian,
    onSurface = GwenTextPrimary,
    surfaceVariant = GwenSurface,
    onSurfaceVariant = GwenTextSecondary,
    surfaceContainerLow = GwenObsidian,
    surfaceContainer = GwenSurface,
    surfaceContainerHigh = GwenSurfaceElevated,
    outline = GwenGlassBorder,
    outlineVariant = Color(0xFF1D2742),
    error = GwenDanger,
    onError = Color.White
)

val GwenShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun GwenAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GwenDarkColorScheme,
        typography = GwenTypography,
        shapes = GwenShapes,
        content = content
    )
}
