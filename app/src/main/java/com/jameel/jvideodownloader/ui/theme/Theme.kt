package com.jameel.jvideodownloader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val dark = darkColorScheme(primary = Color(0xFF6366F1), secondary = Color(0xFF22D3EE), background = Color(0xFF0B0F19), surface = Color(0xFF151B2B), onSurface = Color.White, onSurfaceVariant = Color(0xFF94A3B8))
private val light = lightColorScheme(primary = Color(0xFF4F46E5), secondary = Color(0xFF00788A), background = Color(0xFFF5F6FC), surface = Color.White, onSurface = Color(0xFF111827))
@Composable fun JTheme(theme: String, content: @Composable () -> Unit) {
    val useDark = theme == "dark" || theme == "system" && isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (useDark) dark else light, typography = Typography(), content = content)
}
