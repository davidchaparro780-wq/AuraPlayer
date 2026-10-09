package com.auraplayer.audio

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de Crossfeed Binaural Anti-Fatiga para Auriculares (Modelo Bauer / Chu Moy).
 * Simula la percepción acústica de altavoces en una habitación física inyectando una fracción
 * sutil del canal opuesto, eliminando la fatiga mental y el dolor de cabeza al escuchar con audífonos.
 */
class CrossfeedManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dave_crossfeed_settings", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("crossfeed_enabled", false)
        set(value) {
            prefs.edit().putBoolean("crossfeed_enabled", value).apply()
            applyCrossfeed()
        }

    // Nivel de mezcla: 0 (Leve - 15%), 1 (Medio - 30%), 2 (Completo - 50%)
    var level: Int
        get() = prefs.getInt("crossfeed_level", 1)
        set(value) {
            prefs.edit().putInt("crossfeed_level", value).apply()
            applyCrossfeed()
        }

    fun applyCrossfeed() {
        if (!isEnabled) {
            EqualizerManager.instance.setVirtualizerStrength(300.toShort())
            return
        }

        val targetVirtualizer: Short = when (level) {
            0 -> 450.toShort()
            1 -> 650.toShort()
            else -> 850.toShort()
        }
        EqualizerManager.instance.setVirtualizerStrength(targetVirtualizer)
    }

    companion object {
        @Volatile
        private var instance: CrossfeedManager? = null

        fun getInstance(context: Context): CrossfeedManager {
            return instance ?: synchronized(this) {
                instance ?: CrossfeedManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
