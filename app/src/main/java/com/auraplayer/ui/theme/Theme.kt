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

// Cyberpunk Accent & VIP Luxury Palettes
fun getAuraDarkPalette(accent: String): androidx.compose.material3.ColorScheme {
    val upper = accent.uppercase()
    val isAmoled = upper == "AMOLED" || upper == "AMOLED_BLACK"

    val (primary, secondary, tertiary) = when (upper) {
        "AMOLED", "AMOLED_BLACK" -> Triple(Color(0xFF38BDF8), Color(0xFFA855F7), Color(0xFF22C55E)) // Pure Black + Vivid Neon
        "CYBERPUNK" -> Triple(Color(0xFF00F0FF), Color(0xFFFF0055), Color(0xFFFFE500)) // Electric Cyan & Hot Magenta
        "SUNSET_GOLD", "GOLD" -> Triple(Color(0xFFFFD700), Color(0xFFFF9100), Color(0xFFF59E0B)) // Royal Gold & Amber
        "CYAN" -> Triple(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFFA855F7))
        "MAGENTA" -> Triple(Color(0xFFF43F5E), Color(0xFFA855F7), Color(0xFF06B6D4))
        "GREEN" -> Triple(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFFF59E0B))
        else -> Triple(Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFF06B6D4)) // Velvet Purple default
    }

    val bg = if (isAmoled) Color(0xFF000000) else Color(0xFF07090E)
    val surf = if (isAmoled) Color(0xFF0A0A0A) else Color(0xFF101422)
    val surfVar = if (isAmoled) Color(0xFF141414) else Color(0xFF1B2236)

    return darkColorScheme(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        background = bg,
        surface = surf,
        surfaceVariant = surfVar,
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
