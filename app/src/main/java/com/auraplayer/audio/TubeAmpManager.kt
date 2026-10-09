package com.auraplayer.audio

import android.content.Context

/**
 * Gestor de DSP Analógico: Simulador Tube Amp (Válvulas Cálidas),
 * Compresor Dinámico DRC (Modo Noche) y Auto-EQ por Género Musical.
 */
class TubeAmpManager(context: Context) {

    private val prefs = context.getSharedPreferences("dave_tube_amp_prefs", Context.MODE_PRIVATE)

    var isTubeAmpEnabled: Boolean
        get() = prefs.getBoolean("tube_amp_enabled", false)
        set(value) = prefs.edit().putBoolean("tube_amp_enabled", value).apply()

    var tubeWarmthLevel: Float
        get() = prefs.getFloat("tube_warmth_level", 0.65f)
        set(value) = prefs.edit().putFloat("tube_warmth_level", value.coerceIn(0f, 1f)).apply()

    var isNightDrcEnabled: Boolean
        get() = prefs.getBoolean("night_drc_enabled", false)
        set(value) = prefs.edit().putBoolean("night_drc_enabled", value).apply()

    var isAutoEqEnabled: Boolean
        get() = prefs.getBoolean("auto_eq_enabled", true)
        set(value) = prefs.edit().putBoolean("auto_eq_enabled", value).apply()

    /**
     * Detecta el género musical a partir de metadatos y aplica la curva Auto-EQ ideal
     */
    fun detectAndApplyAutoEq(title: String, artist: String, album: String): String {
        if (!isAutoEqEnabled) return "Estándar"

        val combined = "$title $artist $album".lowercase()
        return when {
            combined.contains("reggaeton") || combined.contains("trap") || combined.contains("bad bunny") || combined.contains("feid") -> {
                EqualizerManager.instance.applyGenrePreset("Urbano / Bajo Potente")
                "🔥 Urbano & 808"
            }
            combined.contains("rock") || combined.contains("metal") || combined.contains("guitar") -> {
                EqualizerManager.instance.applyGenrePreset("Rock Dinámico")
                "🎸 Rock & Guitarras"
            }
            combined.contains("electronic") || combined.contains("edm") || combined.contains("techno") || combined.contains("house") -> {
                EqualizerManager.instance.applyGenrePreset("Electro Punch")
                "⚡ Electro Punch"
            }
            combined.contains("acoustic") || combined.contains("piano") || combined.contains("classical") || combined.contains("jazz") -> {
                EqualizerManager.instance.applyGenrePreset("Acústico Cristalino")
                "🎻 Acústico Puro"
            }
            combined.contains("pop") || combined.contains("dance") -> {
                EqualizerManager.instance.applyGenrePreset("Pop Radiante")
                "✨ Pop Radiante"
            }
            else -> {
                "🎵 DaVE Balanceado"
            }
        }
    }

    companion object {
        @Volatile
        private var instance: TubeAmpManager? = null

        fun getInstance(context: Context): TubeAmpManager {
            return instance ?: synchronized(this) {
                instance ?: TubeAmpManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
