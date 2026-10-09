package com.auraplayer.audio

import android.media.audiofx.Visualizer
import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

/**
 * High-performance real-time audio FFT and waveform visualizer engine.
 * Connects directly to ExoPlayer's audio session to extract live frequency bands,
 * bass energy, and raw waveforms with silky smooth 60/120 FPS rendering.
 */
class RealtimeVisualizerManager private constructor() {

    companion object {
        val instance: RealtimeVisualizerManager by lazy { RealtimeVisualizerManager() }
        const val NUM_BANDS = 32
        const val WAVEFORM_POINTS = 64
    }

    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = -1

    // 32 frequency bands (0.0f .. 1.0f) representing Sub-bass, Bass, Mids, Highs
    private val bandEnergies = FloatArray(NUM_BANDS) { 0.05f }
    // 64 raw waveform points (-1.0f .. 1.0f)
    private val wavePoints = FloatArray(WAVEFORM_POINTS) { 0.0f }

    private var bassEnergy: Float = 0.08f
    private var trebleEnergy: Float = 0.06f
    private var lastDataTime: Long = 0L

    @Synchronized
    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        try {
            val captureSizeRange = Visualizer.getCaptureSizeRange()
            val captureSize = if (captureSizeRange != null && captureSizeRange.size >= 2) {
                // Optimal size: 256 or 512 for fast real-time spectrum analysis
                256.coerceIn(captureSizeRange[0], captureSizeRange[1])
            } else 256

            val vis = Visualizer(audioSessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveform?.let { processWaveform(it) }
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            fft?.let { processFft(it) }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    true
                )
                enabled = true
            }
            visualizer = vis
        } catch (_: Exception) {
            visualizer = null
        }
    }

    private fun processWaveform(bytes: ByteArray) {
        lastDataTime = SystemClock.uptimeMillis()
        val step = (bytes.size / WAVEFORM_POINTS).coerceAtLeast(1)
        for (i in 0 until WAVEFORM_POINTS) {
            val srcIdx = (i * step).coerceIn(0, bytes.size - 1)
            // PCM unsigned byte 0..255 centered at 128 -> normalize to -1.0 .. 1.0
            val raw = (bytes[srcIdx].toInt() and 0xFF) - 128
            val target = raw / 128.0f
            wavePoints[i] = wavePoints[i] * 0.4f + target * 0.6f
        }
    }

    private fun processFft(bytes: ByteArray) {
        lastDataTime = SystemClock.uptimeMillis()
        val n = bytes.size
        val binCount = n / 2
        if (binCount <= 0) return

        // Compute magnitude for each bin
        val magnitudes = FloatArray(binCount)
        for (k in 0 until binCount) {
            val r = bytes[2 * k].toFloat()
            val im = if (2 * k + 1 < n) bytes[2 * k + 1].toFloat() else 0f
            magnitudes[k] = hypot(r, im)
        }

        // Map bins into 32 bands with logarithmic frequency spacing
        var totalBass = 0f
        var totalTreble = 0f

        for (b in 0 until NUM_BANDS) {
            // Logarithmic index spread: low frequencies get fine bins, highs get grouped
            val fracStart = (b.toFloat() / NUM_BANDS)
            val fracEnd = ((b + 1).toFloat() / NUM_BANDS)
            val startBin = (Math.pow(fracStart.toDouble(), 1.6) * (binCount - 1)).toInt().coerceIn(0, binCount - 1)
            val endBin = (Math.pow(fracEnd.toDouble(), 1.6) * (binCount - 1)).toInt().coerceIn(startBin, binCount - 1)

            var bandSum = 0f
            var count = 0
            for (k in startBin..endBin) {
                bandSum += magnitudes[k]
                count++
            }
            val avg = if (count > 0) bandSum / count else magnitudes[startBin]

            // Normalize and enhance dynamics (bass slightly boosted, treble balanced)
            val boost = if (b < 8) 1.25f else if (b < 18) 1.0f else 0.85f
            val target = ((avg / 65f) * boost).coerceIn(0.04f, 1.0f)

            // Instant attack, smooth decay
            if (target > bandEnergies[b]) {
                bandEnergies[b] = target
            } else {
                bandEnergies[b] = bandEnergies[b] * 0.76f + target * 0.24f
            }

            if (b < 6) totalBass += bandEnergies[b]
            if (b >= 20) totalTreble += bandEnergies[b]
        }

        bassEnergy = (totalBass / 6f).coerceIn(0.05f, 1.0f)
        trebleEnergy = (totalTreble / 12f).coerceIn(0.04f, 1.0f)
    }

    /**
     * Retrieves the amplitude factor (0.05f .. 1.0f) for a specific bar out of [totalBars].
     */
    private fun getSensitivity(): Float {
        return com.auraplayer.data.repository.SettingsManager.instance?.visualizerSensitivity ?: 1.0f
    }

    /**
     * Retrieves normalized energy for a specific bar/band (0.0f .. 1.0f).
     */
    fun getBand(index: Int, totalBars: Int, isPlaying: Boolean): Float {
        if (!isPlaying) {
            return 0.05f
        }

        val sens = getSensitivity()

        // Check if we have recent real FFT data (within last 350ms)
        val hasRecentData = (SystemClock.uptimeMillis() - lastDataTime) < 350L

        if (hasRecentData) {
            val mappedIdx = ((index.toFloat() / totalBars.toFloat()) * NUM_BANDS)
                .toInt()
                .coerceIn(0, NUM_BANDS - 1)
            return (bandEnergies[mappedIdx] * sens).coerceIn(0.05f, 1.0f)
        }

        // High-energy music reactive fallback if Visualizer is temporarily unattached
        val now = SystemClock.uptimeMillis() / 1000.0
        val freqBase = (sin(now * 5.2 + index * 0.45) + 1.0) * 0.5
        val freqKick = if (sin(now * 2.8) > 0.6) 0.35 else 0.0
        val harmonic = (sin(now * 8.4 + index * 0.8) + 1.0) * 0.25
        return ((freqBase * 0.55 + freqKick + harmonic).toFloat() * sens).coerceIn(0.08f, 1.0f)
    }

    /**
     * Retrieves raw waveform displacement (-1.0f .. 1.0f) for wave visualizers.
     */
    fun getWaveform(index: Int, totalPoints: Int, isPlaying: Boolean): Float {
        if (!isPlaying) return 0.0f

        val sens = getSensitivity()
        val hasRecentData = (SystemClock.uptimeMillis() - lastDataTime) < 350L
        if (hasRecentData) {
            val mappedIdx = ((index.toFloat() / totalPoints.toFloat()) * WAVEFORM_POINTS)
                .toInt()
                .coerceIn(0, WAVEFORM_POINTS - 1)
            return (wavePoints[mappedIdx] * sens).coerceIn(-1.0f, 1.0f)
        }

        // Fallback synthetic wave
        val now = SystemClock.uptimeMillis() / 600.0
        return (sin(now + (index.toFloat() / totalPoints.toFloat()) * 4.0 * Math.PI).toFloat() * 0.45f * sens).coerceIn(-1.0f, 1.0f)
    }

    /**
     * Bass energy for disc pulsing and glow (0.0f .. 1.0f).
     */
    fun getBassEnergy(isPlaying: Boolean): Float {
        if (!isPlaying) return 0.05f
        val sens = getSensitivity()
        val hasRecentData = (SystemClock.uptimeMillis() - lastDataTime) < 350L
        if (hasRecentData) return (bassEnergy * sens).coerceIn(0.05f, 1.0f)

        val now = SystemClock.uptimeMillis() / 1000.0
        return ((sin(now * 4.2) * 0.4 + 0.5).toFloat() * sens).coerceIn(0.08f, 1.0f)
    }

    /**
     * Treble energy for sparkles and high-frequency effects (0.0f .. 1.0f).
     */
    fun getTrebleEnergy(isPlaying: Boolean): Float {
        if (!isPlaying) return 0.04f
        val sens = getSensitivity()
        val hasRecentData = (SystemClock.uptimeMillis() - lastDataTime) < 350L
        if (hasRecentData) return (trebleEnergy * sens).coerceIn(0.04f, 1.0f)

        val now = SystemClock.uptimeMillis() / 1000.0
        return ((sin(now * 7.5) * 0.35 + 0.45).toFloat() * sens).coerceIn(0.05f, 1.0f)
    }

    @Synchronized
    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {}
        visualizer = null
        currentSessionId = -1
    }
}
