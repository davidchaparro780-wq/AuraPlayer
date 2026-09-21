package com.auraplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Cyberpunk Accent Palettes
fun getAuraDarkPalette(accent: String): androidx.compose.material3.ColorScheme {
    val (primary, secondary, tertiary) = when (accent.uppercase()) {
        "CYAN" -> Triple(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFFA855F7))
        "MAGENTA" -> Triple(Color(0xFFF43F5E), Color(0xFFA855F7), Color(0xFF06B6D4))
        "GREEN" -> Triple(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFFF59E0B))
        "GOLD" -> Triple(Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFA855F7))
        else -> Triple(Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF06B6D4)) // Velvet Purple default
    }

    return darkColorScheme(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        background = Color(0xFF07090E),     // Deep Abyss Black
        surface = Color(0xFF101422),        // Midnight Velvet
        surfaceVariant = Color(0xFF1B2236), // Deep Slate Translucent
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFFF1F5F9),
        onSurface = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF94A3B8)
    )
}

@Composable
fun AuraTheme(
    accent: String = "PURPLE",
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getAuraDarkPalette(accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
