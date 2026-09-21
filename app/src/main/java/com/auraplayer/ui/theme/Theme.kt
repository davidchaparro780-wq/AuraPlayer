package com.auraplayer.ui.theme

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

// Aesthetic Velvet & Neon Glow Palette
private val AuraDarkPalette = darkColorScheme(
    primary = Color(0xFFA855F7),        // Electric Violet
    secondary = Color(0xFFEC4899),      // Neon Rose
    tertiary = Color(0xFF06B6D4),       // Cyan Glow
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

private val AuraLightPalette = lightColorScheme(
    primary = Color(0xFF7C3AED),        // Deep Violet
    secondary = Color(0xFFDB2777),      // Magenta
    tertiary = Color(0xFF0891B2),       // Teal
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun AuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Default to curated aesthetic palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AuraDarkPalette
        else -> AuraDarkPalette // Aesthetic dark first
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
