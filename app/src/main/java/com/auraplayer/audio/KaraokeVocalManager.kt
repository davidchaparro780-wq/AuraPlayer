package com.auraplayer.audio

import android.content.Context
import android.widget.Toast

class KaraokeVocalManager private constructor() {

    companion object {
        val instance: KaraokeVocalManager by lazy { KaraokeVocalManager() }
    }

    var isKaraokeEnabled: Boolean = false
        private set

    // Backup of previous EQ settings before karaoke was enabled
    private val previousBands = FloatArray(10) { 0f }
    private var previousBass: Short = 0
    private var previousVirtualizer: Short = 0

    fun toggleKaraoke(context: Context? = null): Boolean {
        isKaraokeEnabled = !isKaraokeEnabled
        val eq = EqualizerManager.instance

        if (isKaraokeEnabled) {
            // Backup current bands
            System.arraycopy(eq.bandLevels, 0, previousBands, 0, 10)
            previousBass = eq.bassStrength
            previousVirtualizer = eq.virtualizerStrength

            // Apply vocal suppression curve:
            // Boost low end (bass/kick) and highs (cymbals/air), drastically cut mid-band vocal frequencies (300Hz - 3.5kHz)
            // Bands: 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
            val vocalCutCurve = floatArrayOf(
                4.0f,  // 31 Hz: keep bass
                4.0f,  // 62 Hz: keep sub
                2.0f,  // 125 Hz: keep body
                -3.0f, // 250 Hz: cut lower vocal fundamentals
                -10.0f, // 500 Hz: heavy cut vocal body
                -12.0f, // 1 kHz: maximum vocal presence scoop
                -10.0f, // 2 kHz: cut vocal clarity
                -5.0f, // 4 kHz: cut vocal sibilance
                3.0f,  // 8 kHz: preserve crisp hi-hats
                4.0f   // 16 kHz: preserve air & instrumental shimmer
            )

            for (i in 0 until 10) {
                eq.setBandLevel(i, vocalCutCurve[i])
            }
            // Maximize virtualizer to push center vocals to the sides/cancel out
            eq.setVirtualizerStrength(900.toShort())

            context?.let {
                Toast.makeText(it, "🎤 Modo Karaoke ACTIVO: Voz atenuada", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Restore previous user bands
            for (i in 0 until 10) {
                eq.setBandLevel(i, previousBands[i])
            }
            eq.setBassBoostStrength(previousBass)
            eq.setVirtualizerStrength(previousVirtualizer)

            context?.let {
                Toast.makeText(it, "🎤 Modo Karaoke DESACTIVADO", Toast.LENGTH_SHORT).show()
            }
        }

        return isKaraokeEnabled
    }
}
