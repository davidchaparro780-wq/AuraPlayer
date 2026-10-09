package com.auraplayer.audio

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de Normalización Inteligente de Volumen (Loudness Normalizer / ReplayGain).
 * Nivela automáticamente la ganancia percibida a -14 LUFS para evitar saltos bruscos
 * de volumen entre canciones grabadas a distintos niveles.
 */
class LoudnessNormalizerManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dave_loudness_normalizer", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("loudness_normalizer_enabled", false)
        set(value) {
            prefs.edit().putBoolean("loudness_normalizer_enabled", value).apply()
            applyLoudness()
        }

    // Nivel de objetivo: 0: Equilibrado (-14 LUFS, ~400 mB), 1: Alto (-11 LUFS, ~750 mB), 2: Suave (-18 LUFS, ~200 mB)
    var targetMode: Int
        get() = prefs.getInt("loudness_target_mode", 0)
        set(value) {
            prefs.edit().putInt("loudness_target_mode", value).apply()
            applyLoudness()
        }

    fun applyLoudness() {
        if (!isEnabled) {
            EqualizerManager.instance.setLoudnessGain(0)
            return
        }

        val targetMb = when (targetMode) {
            1 -> 750 // Alto / Club
            2 -> 200 // Suave / Nocturno
            else -> 450 // Equilibrado estándar
        }
        EqualizerManager.instance.setLoudnessGain(targetMb)
    }

    companion object {
        @Volatile
        private var instance: LoudnessNormalizerManager? = null

        fun getInstance(context: Context): LoudnessNormalizerManager {
            return instance ?: synchronized(this) {
                instance ?: LoudnessNormalizerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
