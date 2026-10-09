package com.auraplayer.audio

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de Mezclador de Pistas Aisladas en Tiempo Real (Live AI Stem Mixer).
 * Permite modular de forma independiente el volumen y ganancia de:
 * 1. Voces (Vocal / Acapella / Karaoke)
 * 2. Batería & Percusión (Drums / Beat Punch)
 * 3. Línea de Bajo (Sub-Bass / 808)
 * 4. Melodías & Sintetizadores (Instruments / Mids)
 */
class StemMixerManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dave_stem_mixer_prefs", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("stem_mixer_enabled", false)
        set(value) {
            prefs.edit().putBoolean("stem_mixer_enabled", value).apply()
            applyStemLevels()
        }

    // Ganancias de 0.0f a 2.0f (1.0f = nivel estándar original)
    var vocalGain: Float
        get() = prefs.getFloat("stem_vocal_gain", 1.0f)
        set(value) {
            prefs.edit().putFloat("stem_vocal_gain", value.coerceIn(0f, 2f)).apply()
            applyStemLevels()
        }

    var drumsGain: Float
        get() = prefs.getFloat("stem_drums_gain", 1.0f)
        set(value) {
            prefs.edit().putFloat("stem_drums_gain", value.coerceIn(0f, 2f)).apply()
            applyStemLevels()
        }

    var bassGain: Float
        get() = prefs.getFloat("stem_bass_gain", 1.0f)
        set(value) {
            prefs.edit().putFloat("stem_bass_gain", value.coerceIn(0f, 2f)).apply()
            applyStemLevels()
        }

    var instrumentsGain: Float
        get() = prefs.getFloat("stem_instruments_gain", 1.0f)
        set(value) {
            prefs.edit().putFloat("stem_instruments_gain", value.coerceIn(0f, 2f)).apply()
            applyStemLevels()
        }

    fun applyStemLevels() {
        if (!isEnabled) {
            // Restore flat equalization
            EqualizerManager.instance.apply {
                setBandLevel(0, 0f)
                setBandLevel(1, 0f)
                setBandLevel(2, 0f)
                setBandLevel(3, 0f)
                setBandLevel(4, 0f)
                setBandLevel(5, 0f)
                setBandLevel(6, 0f)
                setBandLevel(7, 0f)
                setBandLevel(8, 0f)
                setBandLevel(9, 0f)
            }
            return
        }

        // Mapeo psicoacústico de las bandas hacia los 10 rangos de hardware
        // Bass (31Hz, 62Hz, 125Hz)
        val bassDb = (bassGain - 1.0f) * 12f
        // Drums Punch (250Hz, 8kHz, 16kHz)
        val drumsDb = (drumsGain - 1.0f) * 10f
        // Vocals (1kHz, 2kHz, 4kHz)
        val vocalDb = (vocalGain - 1.0f) * 12f
        // Instruments & Ambience (500Hz, 4kHz)
        val instrDb = (instrumentsGain - 1.0f) * 10f

        EqualizerManager.instance.apply {
            setBandLevel(0, bassDb)
            setBandLevel(1, bassDb * 0.9f)
            setBandLevel(2, (bassDb * 0.7f + drumsDb * 0.5f).coerceIn(-12f, 12f))
            setBandLevel(3, drumsDb)
            setBandLevel(4, instrDb)
            setBandLevel(5, (vocalDb * 0.8f + instrDb * 0.4f).coerceIn(-12f, 12f))
            setBandLevel(6, vocalDb)
            setBandLevel(7, (vocalDb * 0.7f + instrDb * 0.6f).coerceIn(-12f, 12f))
            setBandLevel(8, drumsDb * 0.8f)
            setBandLevel(9, drumsDb * 0.9f)
        }
    }

    fun resetToDefault() {
        vocalGain = 1.0f
        drumsGain = 1.0f
        bassGain = 1.0f
        instrumentsGain = 1.0f
        isEnabled = false
    }

    fun setKaraokePreset() {
        isEnabled = true
        vocalGain = 0.0f
        drumsGain = 1.15f
        bassGain = 1.1f
        instrumentsGain = 1.25f
    }

    fun setAcapellaPreset() {
        isEnabled = true
        vocalGain = 1.8f
        drumsGain = 0.15f
        bassGain = 0.1f
        instrumentsGain = 0.25f
    }

    fun setBassBoostedPreset() {
        isEnabled = true
        vocalGain = 1.0f
        drumsGain = 1.3f
        bassGain = 1.85f
        instrumentsGain = 0.95f
    }

    companion object {
        @Volatile
        private var instance: StemMixerManager? = null

        fun getInstance(context: Context): StemMixerManager {
            return instance ?: synchronized(this) {
                instance ?: StemMixerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
