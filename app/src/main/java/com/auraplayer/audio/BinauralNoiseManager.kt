package com.auraplayer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.auraplayer.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

enum class BinauralMode(val title: String, val desc: String, val baseFreq: Float, val diffFreq: Float) {
    OFF("Apagado", "Sin frecuencias", 0f, 0f),
    FREQ_432("432 Hz Milagrosa", "Relajación, meditación profunda y armonía", 432f, 0f),
    FREQ_528("528 Hz Transformación", "Concentración y claridad mental", 528f, 0f),
    THETA_WAVE("Ondas Theta (6 Hz)", "Sueño profundo y descanso cerebral", 200f, 6f),
    ALPHA_WAVE("Ondas Alfa (10 Hz)", "Flujo de estudio y calma despierta", 220f, 10f),
    RAIN_SOUND("Lluvia & Ruido Marrón", "Ruido relajante para aislar distracciones", 0f, -1f)
}

class BinauralNoiseManager private constructor() {

    companion object {
        private const val TAG = "BinauralNoiseManager"

        val instance: BinauralNoiseManager by lazy { BinauralNoiseManager() }
        private const val SAMPLE_RATE = 44100
    }

    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var currentMode: BinauralMode = BinauralMode.OFF
        private set

    var volume: Float = 0.25f // 0.0f to 1.0f

    fun setMode(mode: BinauralMode) {
        if (currentMode == mode) return
        currentMode = mode
        stop()

        if (mode != BinauralMode.OFF) {
            start(mode)
        }
    }

    private fun start(mode: BinauralMode) {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        synthesisJob = scope.launch {
            val buffer = ShortArray(2048)
            var phaseLeft = 0.0
            var phaseRight = 0.0
            var brownVal = 0.0f

            val freqLeft = mode.baseFreq
            val freqRight = if (mode.diffFreq > 0) mode.baseFreq + mode.diffFreq else mode.baseFreq

            while (isActive && currentMode != BinauralMode.OFF) {
                val vol = volume.coerceIn(0f, 1f)

                if (mode == BinauralMode.RAIN_SOUND) {
                    // Brown Noise / Soft Rain simulation
                    for (i in 0 until buffer.size step 2) {
                        val white = (Random.nextFloat() * 2f - 1f)
                        brownVal = (brownVal + (0.02f * white)) / 1.02f
                        val sample = (brownVal * 32767f * vol * 0.8f).toInt().coerceIn(-32767, 32767).toShort()
                        buffer[i] = sample
                        buffer[i + 1] = sample
                    }
                } else {
                    // Pure Sine Wave Synthesis (Stereo Binaural)
                    for (i in 0 until buffer.size step 2) {
                        val sampleL = (sin(phaseLeft) * 32767.0 * vol * 0.35).toInt().coerceIn(-32767, 32767).toShort()
                        val sampleR = (sin(phaseRight) * 32767.0 * vol * 0.35).toInt().coerceIn(-32767, 32767).toShort()
                        buffer[i] = sampleL
                        buffer[i + 1] = sampleR

                        phaseLeft += 2.0 * Math.PI * freqLeft / SAMPLE_RATE
                        phaseRight += 2.0 * Math.PI * freqRight / SAMPLE_RATE
                        if (phaseLeft > 2.0 * Math.PI) phaseLeft -= 2.0 * Math.PI
                        if (phaseRight > 2.0 * Math.PI) phaseRight -= 2.0 * Math.PI
                    }
                }

                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stop() {
        synthesisJob?.cancel()
        synthesisJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (e: Exception) {
            AppLog.d(TAG, "No se pudo detener la pista de sonido binaural", e)
        }
        audioTrack = null
    }
}
