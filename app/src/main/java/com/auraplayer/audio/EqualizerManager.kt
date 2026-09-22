package com.auraplayer.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build

class EqualizerManager private constructor() {

    companion object {
        val instance: EqualizerManager by lazy { EqualizerManager() }

        val TEN_BAND_FREQUENCIES = listOf(
            "31 Hz", "62 Hz", "125 Hz", "250 Hz", "500 Hz",
            "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz"
        )
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var prefs: SharedPreferences? = null

    var isEnabled: Boolean = true
        private set

    var bassStrength: Short = 400
        private set

    var virtualizerStrength: Short = 300
        private set

    var loudnessGain: Int = 0 // in mB (0 to 1000)
        private set

    // 10 virtual band levels in dB (-12 dB to +12 dB)
    val bandLevels = FloatArray(10) { 0f }

    fun initPrefs(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences("aura_equalizer_settings", Context.MODE_PRIVATE)
            loadSavedSettings()
        }
    }

    private fun loadSavedSettings() {
        prefs?.let { p ->
            isEnabled = p.getBoolean("eq_enabled", true)
            bassStrength = p.getInt("bass_strength", 400).toShort()
            virtualizerStrength = p.getInt("virtualizer_strength", 300).toShort()
            loudnessGain = p.getInt("loudness_gain", 0)
            for (i in 0 until 10) {
                bandLevels[i] = p.getFloat("band_$i", 0f)
            }
        }
    }

    private fun persistSettings() {
        prefs?.edit()?.apply {
            putBoolean("eq_enabled", isEnabled)
            putInt("bass_strength", bassStrength.toInt())
            putInt("virtualizer_strength", virtualizerStrength.toInt())
            putInt("loudness_gain", loudnessGain)
            for (i in 0 until 10) {
                putFloat("band_$i", bandLevels[i])
            }
            apply()
        }
    }

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
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = isEnabled
                if (strengthSupported) {
                    setStrength(virtualizerStrength)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                        enabled = isEnabled
                        setTargetGain(loudnessGain)
                    }
                } catch (e: Exception) {
                    loudnessEnhancer = null
                }
            }
            applyBandsToHardware()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEnabled(enable: Boolean) {
        isEnabled = enable
        equalizer?.enabled = enable
        bassBoost?.enabled = enable
        virtualizer?.enabled = enable
        loudnessEnhancer?.enabled = enable
        persistSettings()
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        try {
            if (enabled) {
                val target = if (loudnessGain > 0) loudnessGain else 300
                loudnessEnhancer?.setTargetGain(target)
                loudnessEnhancer?.enabled = isEnabled
            } else {
                loudnessEnhancer?.setTargetGain(loudnessGain)
                loudnessEnhancer?.enabled = loudnessGain > 0 && isEnabled
            }
        } catch (_: Exception) {}
    }

    fun setBandLevel(bandIndex: Int, levelDb: Float) {
        if (bandIndex in 0 until 10) {
            bandLevels[bandIndex] = levelDb.coerceIn(-12f, 12f)
            applyBandsToHardware()
            persistSettings()
        }
    }

    private fun applyBandsToHardware() {
        val eq = equalizer ?: return
        val hwBands = eq.numberOfBands.toInt()
        val range = eq.bandLevelRange // in millibels, e.g. -1500 to +1500
        val minMb = range[0]
        val maxMb = range[1]

        if (hwBands >= 10) {
            // Direct 1-to-1 mapping
            for (i in 0 until 10.coerceAtMost(hwBands)) {
                val mb = (bandLevels[i] * 100).toInt().coerceIn(minMb.toInt(), maxMb.toInt()).toShort()
                try {
                    eq.setBandLevel(i.toShort(), mb)
                } catch (e: Exception) { }
            }
        } else if (hwBands > 0) {
            // Map 10 bands down into available hardware bands (typically 5)
            for (hw in 0 until hwBands) {
                // Map hw index (0..hwBands-1) to virtual range
                val startV = (hw * 10) / hwBands
                val endV = (((hw + 1) * 10) / hwBands).coerceAtMost(10)
                var avg = 0f
                var count = 0
                for (v in startV until endV) {
                    avg += bandLevels[v]
                    count++
                }
                if (count > 0) avg /= count
                val mb = (avg * 100).toInt().coerceIn(minMb.toInt(), maxMb.toInt()).toShort()
                try {
                    eq.setBandLevel(hw.toShort(), mb)
                } catch (e: Exception) { }
            }
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
        persistSettings()
    }

    fun setVirtualizerStrength(strength: Short) {
        virtualizerStrength = strength.coerceIn(0, 1000)
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(virtualizerStrength)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        persistSettings()
    }

    fun setLoudnessGain(gainMb: Int) {
        loudnessGain = gainMb.coerceIn(0, 1000)
        try {
            loudnessEnhancer?.setTargetGain(loudnessGain)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        persistSettings()
    }

    fun applyPreset(presetIndex: Int) {
        when (presetIndex) {
            0 -> { // Flat
                for (i in 0 until 10) bandLevels[i] = 0f
                bassStrength = 0
                virtualizerStrength = 0
                loudnessGain = 0
            }
            1 -> { // Cyber Bass
                val preset = floatArrayOf(8f, 7f, 5f, 3f, 1f, 0f, 1f, 2f, 3f, 4f)
                System.arraycopy(preset, 0, bandLevels, 0, 10)
                bassStrength = 800
                virtualizerStrength = 400
                loudnessGain = 200
            }
            2 -> { // Vocal Glow
                val preset = floatArrayOf(-2f, -1f, 1f, 4f, 7f, 6f, 5f, 3f, 2f, 1f)
                System.arraycopy(preset, 0, bandLevels, 0, 10)
                bassStrength = 200
                virtualizerStrength = 500
                loudnessGain = 100
            }
            3 -> { // Neon Pop
                val preset = floatArrayOf(4f, 3f, 2f, 1f, 3f, 5f, 6f, 5f, 4f, 3f)
                System.arraycopy(preset, 0, bandLevels, 0, 10)
                bassStrength = 500
                virtualizerStrength = 600
                loudnessGain = 150
            }
            4 -> { // Rock Velvet
                val preset = floatArrayOf(7f, 5f, 3f, -1f, -2f, 1f, 4f, 6f, 7f, 8f)
                System.arraycopy(preset, 0, bandLevels, 0, 10)
                bassStrength = 600
                virtualizerStrength = 300
                loudnessGain = 250
            }
            5 -> { // Lo-Fi Lounge
                val preset = floatArrayOf(5f, 4f, 3f, 1f, 0f, -1f, -2f, -3f, -4f, -5f)
                System.arraycopy(preset, 0, bandLevels, 0, 10)
                bassStrength = 400
                virtualizerStrength = 200
                loudnessGain = 0
            }
            6 -> { // Electronic
                val preset = floatArrayOf(7f, 6f, 2f, 0f, -1f, 2f, 4f, 6f, 8f, 8f)
                System.arraycopy(preset, 0, bandLevels, 0, 10)
                bassStrength = 750
                virtualizerStrength = 700
                loudnessGain = 300
            }
            7 -> { // Audiophile Hi-Fi
                val preset = floatArrayOf(2f, 1f, 0f, 0f, 1f, 2f, 2f, 3f, 3f, 4f)
                System.arraycopy(preset, 0, bandLevels, 0, 10)
                bassStrength = 300
                virtualizerStrength = 400
                loudnessGain = 100
            }
        }
        applyBandsToHardware()
        setBassBoostStrength(bassStrength)
        setVirtualizerStrength(virtualizerStrength)
        setLoudnessGain(loudnessGain)
        persistSettings()
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            loudnessEnhancer = null
        }
    }
}
