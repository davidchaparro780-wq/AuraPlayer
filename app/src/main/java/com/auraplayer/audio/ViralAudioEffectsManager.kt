package com.auraplayer.audio

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.session.MediaController

enum class ViralAudioMode(val displayName: String, val speed: Float, val tag: String) {
    NORMAL("Normal", 1.0f, "✨ Original"),
    SLOWED_REVERB("Slowed + Reverb", 0.85f, "🌊 Lo-Fi Chill"),
    NIGHTCORE("Nightcore", 1.25f, "⚡ Sped Up")
}

/**
 * Gestor de Efectos Virales en 1 Toque: Slowed + Reverb & Nightcore / Sped Up.
 * Modula la velocidad de reproducción, reverberación espacial y ecualización Lo-Fi al compás.
 */
class ViralAudioEffectsManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dave_viral_audio_effects", Context.MODE_PRIVATE)

    var currentMode: ViralAudioMode
        get() {
            val name = prefs.getString("viral_mode", ViralAudioMode.NORMAL.name) ?: ViralAudioMode.NORMAL.name
            return try {
                ViralAudioMode.valueOf(name)
            } catch (_: Exception) {
                ViralAudioMode.NORMAL
            }
        }
        set(value) {
            prefs.edit().putString("viral_mode", value.name).apply()
        }

    fun applyMode(mode: ViralAudioMode, controller: MediaController?) {
        currentMode = mode
        controller?.let { c ->
            c.setPlaybackSpeed(mode.speed)
        }

        when (mode) {
            ViralAudioMode.SLOWED_REVERB -> {
                EqualizerManager.instance.setBassBoostStrength(600.toShort())
                EqualizerManager.instance.setSpatial8DEnabled(true)
            }
            ViralAudioMode.NIGHTCORE -> {
                EqualizerManager.instance.setBassBoostStrength(350.toShort())
                EqualizerManager.instance.setSpatial8DEnabled(false)
            }
            ViralAudioMode.NORMAL -> {
                EqualizerManager.instance.setSpatial8DEnabled(false)
            }
        }
    }

    fun cycleMode(controller: MediaController?): ViralAudioMode {
        val next = when (currentMode) {
            ViralAudioMode.NORMAL -> ViralAudioMode.SLOWED_REVERB
            ViralAudioMode.SLOWED_REVERB -> ViralAudioMode.NIGHTCORE
            ViralAudioMode.NIGHTCORE -> ViralAudioMode.NORMAL
        }
        applyMode(next, controller)
        return next
    }

    companion object {
        @Volatile
        private var instance: ViralAudioEffectsManager? = null

        fun getInstance(context: Context): ViralAudioEffectsManager {
            return instance ?: synchronized(this) {
                instance ?: ViralAudioEffectsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
