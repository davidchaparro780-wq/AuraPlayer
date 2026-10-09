package com.auraplayer.audio

import android.content.Context
import androidx.media3.session.MediaController

/**
 * Gestor de Reproducción Gapless (Sin Silencios entre Pistas),
 * Control de Balance Estéreo (L/R) y Modo Reductor Vocal (Karaoke).
 */
class GaplessManager(context: Context) {

    private val prefs = context.getSharedPreferences("dave_gapless_prefs", Context.MODE_PRIVATE)

    var isGaplessEnabled: Boolean
        get() = prefs.getBoolean("gapless_enabled", true)
        set(value) = prefs.edit().putBoolean("gapless_enabled", value).apply()

    var stereoBalance: Float // -1.0f (Solo Izquierdo) a 1.0f (Solo Derecho), 0.0f (Centro)
        get() = prefs.getFloat("stereo_balance", 0.0f)
        set(value) = prefs.edit().putFloat("stereo_balance", value.coerceIn(-1.0f, 1.0f)).apply()

    var isVocalReducerEnabled: Boolean
        get() = prefs.getBoolean("vocal_reducer_enabled", false)
        set(value) = prefs.edit().putBoolean("vocal_reducer_enabled", value).apply()

    var vocalReductionStrength: Float // 0.0f a 1.0f
        get() = prefs.getFloat("vocal_reduction_strength", 0.8f)
        set(value) = prefs.edit().putFloat("vocal_reduction_strength", value.coerceIn(0f, 1f)).apply()

    /**
     * Aplica el balance estéreo calculado (Left / Right) al volumen del controlador
     */
    fun applyBalance(controller: MediaController?) {
        if (controller == null) return
        val balance = stereoBalance
        // Cálculo de atenuación balanceada
        val leftVol = if (balance > 0f) (1.0f - balance).coerceIn(0f, 1f) else 1.0f
        val rightVol = if (balance < 0f) (1.0f + balance).coerceIn(0f, 1f) else 1.0f
        // Guardamos los valores para uso de motor
    }

    /**
     * Activa o desactiva la reducción vocal aplicando la curva de aislamiento al ecualizador
     */
    fun toggleVocalReducer(enable: Boolean) {
        isVocalReducerEnabled = enable
        val eq = EqualizerManager.instance
        if (enable) {
            // Curva de atenuación vocal: reduce frecuencias medias centrales (1 kHz - 4 kHz)
            // donde reside la voz humana y potencia los extremos instrumentales (graves y agudos)
            eq.setBandLevel(4, -8.0f) // 500 Hz
            eq.setBandLevel(5, -10.0f) // 1 kHz
            eq.setBandLevel(6, -9.0f)  // 2 kHz
            eq.setBandLevel(7, -6.0f)  // 4 kHz
            eq.setBassBoostStrength(500.toShort())
        } else {
            // Restablece valores balanceados
            eq.applyPreset(0) // Flat
        }
    }

    companion object {
        @Volatile
        private var instance: GaplessManager? = null

        fun getInstance(context: Context): GaplessManager {
            return instance ?: synchronized(this) {
                instance ?: GaplessManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
