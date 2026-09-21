package com.auraplayer.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer

class EqualizerManager {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    var isEnabled: Boolean = true
        private set

    var bassStrength: Short = 400
        private set

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        release()

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = isEnabled
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = isEnabled
                if (strengthSupported) {
                    setStrength(bassStrength)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEnabled(enable: Boolean) {
        isEnabled = enable
        equalizer?.enabled = enable
        bassBoost?.enabled = enable
    }

    fun getNumberOfBands(): Short {
        return equalizer?.numberOfBands ?: 5
    }

    fun getBandLevelRange(): Pair<Short, Short> {
        val range = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
        return Pair(range[0], range[1])
    }

    fun getCenterFreq(band: Short): Int {
        return (equalizer?.getCenterFreq(band) ?: 1000) / 1000 // In Hz
    }

    fun getBandLevel(band: Short): Short {
        return equalizer?.getBandLevel(band) ?: 0
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoostStrength(strength: Short) {
        bassStrength = strength.coerceIn(0, 1000)
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(bassStrength)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPresetNames(): List<String> {
        val count = equalizer?.numberOfPresets ?: 0
        val names = mutableListOf<String>()
        for (i in 0 until count) {
            names.add(equalizer?.getPresetName(i.toShort()) ?: "Preset $i")
        }
        return if (names.isEmpty()) listOf("Normal", "Rock", "Pop", "Jazz", "Classical", "Bass") else names
    }

    fun usePreset(presetIndex: Short) {
        try {
            equalizer?.usePreset(presetIndex)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            equalizer = null
            bassBoost = null
        }
    }
}
