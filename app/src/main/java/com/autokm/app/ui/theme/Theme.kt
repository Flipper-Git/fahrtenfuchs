package com.autokm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm, Claude-inspired palette. Refined further in Sprint 6 (Design).
val Terracotta = Color(0xFFCC785C)
val TerracottaDark = Color(0xFFB35F45)
val Cream = Color(0xFFF5F0E8)
val CreamDark = Color(0xFF1E1B18)
val InkLight = Color(0xFF2B2723)
val InkDark = Color(0xFFECE6DD)

private val LightColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    secondary = TerracottaDark,
    background = Cream,
    onBackground = InkLight,
    surface = Cream,
    onSurface = InkLight,
)

private val DarkColors = darkColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    secondary = TerracottaDark,
    background = CreamDark,
    onBackground = InkDark,
    surface = CreamDark,
    onSurface = InkDark,
)

@Composable
fun AutoKmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
