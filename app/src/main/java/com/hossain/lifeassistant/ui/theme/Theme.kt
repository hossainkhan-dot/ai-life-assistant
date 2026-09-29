package com.hossain.lifeassistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF5B5BD6),
    onPrimary = Color.White,
    secondary = Color(0xFF8E7CF3),
    background = Color(0xFFF6F7FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFECEEF7),
    onBackground = Color(0xFF14151A),
    onSurface = Color(0xFF14151A)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9A9CFF),
    onPrimary = Color(0xFF14151A),
    secondary = Color(0xFFB4A7FF),
    background = Color(0xFF0F1015),
    surface = Color(0xFF1A1B23),
    surfaceVariant = Color(0xFF242531),
    onBackground = Color(0xFFECEDF5),
    onSurface = Color(0xFFECEDF5)
)

@Composable
fun LifeAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        content = content
    )
}
