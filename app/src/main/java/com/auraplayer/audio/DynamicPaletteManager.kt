package com.auraplayer.audio

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DynamicPaletteManager extracts the dominant color from an album art bitmap
 * and returns a harmonious accent color for the player UI.
 */
object DynamicPaletteManager {

    suspend fun extractAccentColor(bitmap: Bitmap): Color = withContext(Dispatchers.Default) {
        return@withContext try {
            val palette = Palette.from(bitmap).generate()

            // Priority: Vibrant > LightVibrant > Muted > DominantSwatch
            val swatch = palette.vibrantSwatch
                ?: palette.lightVibrantSwatch
                ?: palette.mutedSwatch
                ?: palette.dominantSwatch

            if (swatch != null) {
                val rgb = swatch.rgb
                val r = (rgb shr 16 and 0xFF) / 255f
                val g = (rgb shr 8 and 0xFF) / 255f
                val b = (rgb and 0xFF) / 255f
                // Ensure minimum brightness for visibility on dark backgrounds
                val brightness = 0.299f * r + 0.587f * g + 0.114f * b
                if (brightness < 0.25f) {
                    // Boost dark colors
                    Color(r * 1.8f.coerceAtMost(1f), g * 1.8f.coerceAtMost(1f), b * 1.8f.coerceAtMost(1f))
                } else {
                    Color(r, g, b)
                }
            } else {
                Color(0xFF6C63FF) // default purple fallback
            }
        } catch (_: Exception) {
            Color(0xFF6C63FF)
        }
    }
}
