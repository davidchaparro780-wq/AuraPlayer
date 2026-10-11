package com.auraplayer.util

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ArtworkPalette(
    val seed: Color,
    val accent: Color
)

object DynamicArtworkTheme {
    var current: ArtworkPalette? by mutableStateOf(null)
        private set

    fun update(palette: ArtworkPalette?) {
        current = palette
    }
}

object ArtworkPaletteExtractor {

    private val cache = HashMap<String, ArtworkPalette>()

    suspend fun extract(context: Context, uri: Uri?): ArtworkPalette? {
        if (uri == null) return null
        val key = uri.toString()
        cache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .size(160)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                val drawable = (result as? SuccessResult)?.drawable ?: return@withContext null
                val bitmap = (drawable as? BitmapDrawable)?.bitmap
                if (bitmap == null || bitmap.isRecycled) return@withContext null
                val palette = Palette.from(bitmap)
                    .maximumColorCount(24)
                    .generate()
                val swatch = palette.vibrantSwatch
                    ?: palette.lightVibrantSwatch
                    ?: palette.dominantSwatch
                    ?: palette.mutedSwatch
                    ?: return@withContext null
                val extracted = ArtworkPalette(
                    seed = Color(swatch.rgb),
                    accent = Color(swatch.titleTextColor)
                )
                if (cache.size > 32) cache.clear()
                cache[key] = extracted
                extracted
            } catch (e: Exception) {
                AppLog.w("ArtworkPalette", "No se pudo extraer la paleta de la carátula", e)
                null
            }
        }
    }
}
