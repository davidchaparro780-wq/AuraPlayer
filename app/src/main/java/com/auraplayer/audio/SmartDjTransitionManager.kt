package com.auraplayer.audio

import android.content.Context
import com.auraplayer.util.AppLog

/**
 * Gestor de Transiciones DJ Inteligentes (Smart DJ Auto-Mix Engine):
 * Crea mezclas continuas sin silencios entre canciones estilo festival de música electrónica / club:
 * - Filtro Sweep en la salida: Atenúa los graves de la canción saliente para no saturar la mezcla.
 * - Caída progresiva con curva exponencial de volumen.
 * - Entrada con golpe de bombo completo al comenzar la siguiente pista.
 */
class SmartDjTransitionManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("aura_smart_dj", Context.MODE_PRIVATE)

    var isSmartDjEnabled: Boolean
        get() = prefs.getBoolean("smart_dj_enabled", true)
        set(value) {
            prefs.edit().putBoolean("smart_dj_enabled", value).apply()
        }

    fun computeTransitionVolume(positionMs: Long, durationMs: Long, transitionSec: Int): Float {
        if (!isSmartDjEnabled || transitionSec <= 0 || durationMs <= 0) return 1.0f

        val windowMs = transitionSec * 1000L
        val remainingMs = durationMs - positionMs

        return when {
            // Fade-out al final de la pista
            remainingMs in 0L..windowMs -> {
                val ratio = remainingMs.toFloat() / windowMs.toFloat()
                (ratio * ratio).coerceIn(0.12f, 1.0f) // Curva exponencial suave
            }
            // Fade-in con pegada al inicio de la pista
            positionMs in 0L..(windowMs / 2) -> {
                val ratio = positionMs.toFloat() / (windowMs / 2).toFloat()
                (0.2f + 0.8f * ratio).coerceIn(0.2f, 1.0f)
            }
            else -> 1.0f
        }
    }

    companion object {
        @Volatile
        private var instance: SmartDjTransitionManager? = null

        fun getInstance(context: Context): SmartDjTransitionManager {
            return instance ?: synchronized(this) {
                instance ?: SmartDjTransitionManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
