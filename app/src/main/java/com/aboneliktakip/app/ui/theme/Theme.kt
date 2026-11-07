package com.aboneliktakip.app.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6B5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6F2DD),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4A635C),
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFF7FAF8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AD5C1),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFFA6F2DD),
    secondary = Color(0xFFB1CCC3),
)

@Composable
fun AboneTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
