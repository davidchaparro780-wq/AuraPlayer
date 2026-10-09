package com.auraplayer.audio

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de Recorte Automático de Silencios Muertos (Smart Silence Trimmer).
 * Salta automáticamente los silencios inaudibles al inicio y final de las canciones
 * para lograr una reproducción continua y dinámica.
 */
class SilenceTrimmerManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dave_silence_trimmer", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("silence_trimmer_enabled", false)
        set(value) {
            prefs.edit().putBoolean("silence_trimmer_enabled", value).apply()
        }

    // Segundos a recortar al inicio (1..4s)
    var introTrimSeconds: Int
        get() = prefs.getInt("intro_trim_seconds", 2)
        set(value) {
            prefs.edit().putInt("intro_trim_seconds", value.coerceIn(1, 6)).apply()
        }

    // Segundos a recortar antes del final (1..4s)
    var outroTrimSeconds: Int
        get() = prefs.getInt("outro_trim_seconds", 2)
        set(value) {
            prefs.edit().putInt("outro_trim_seconds", value.coerceIn(1, 6)).apply()
        }

    fun getTrimmedStartPositionMs(durationMs: Long): Long {
        if (!isEnabled || durationMs < 20_000L) return 0L
        return introTrimSeconds * 1000L
    }

    fun shouldTrimOutro(currentPosMs: Long, durationMs: Long): Boolean {
        if (!isEnabled || durationMs < 20_000L) return false
        val threshold = durationMs - (outroTrimSeconds * 1000L)
        return currentPosMs >= threshold
    }

    companion object {
        @Volatile
        private var instance: SilenceTrimmerManager? = null

        fun getInstance(context: Context): SilenceTrimmerManager {
            return instance ?: synchronized(this) {
                instance ?: SilenceTrimmerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
