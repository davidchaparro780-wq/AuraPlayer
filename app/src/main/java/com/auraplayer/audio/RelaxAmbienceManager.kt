package com.auraplayer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

enum class AmbienceSound(val title: String, val emoji: String, val description: String) {
    NONE("Silencio", "🔇", "Sonido ambiental desactivado"),
    RAIN("Lluvia & Gotas", "🌧️", "Lluvia relajante con suaves gotas en la ventana"),
    CAMPFIRE("Fogata Crepitante", "🔥", "Leña ardiendo con chispas y calidez nocturna"),
    OCEAN("Olas del Mar", "🌊", "Oleaje suave y constante de marea oceánica"),
    WIND("Viento de Bosque", "🍃", "Brisa fresca entre hojas y árboles"),
    BROWN_NOISE("Ruido Marrón Profundo", "☕", "Frecuencias bajas ideales para concentración y estudio")
}

class RelaxAmbienceManager private constructor() {

    companion object {
        val instance: RelaxAmbienceManager by lazy { RelaxAmbienceManager() }
        private const val SAMPLE_RATE = 44100
    }

    private var audioTrack: AudioTrack? = null
    private var generatorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var currentSound: AmbienceSound = AmbienceSound.NONE
        private set

    var volume: Float = 0.35f // 0.0f to 1.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            audioTrack?.setVolume(field)
        }

    fun setSound(sound: AmbienceSound) {
        if (currentSound == sound) return
        currentSound = sound
        stop()

        if (sound != AmbienceSound.NONE) {
            start(sound)
        }
    }

    private fun start(sound: AmbienceSound) {
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

        audioTrack?.setVolume(volume)
        audioTrack?.play()

        generatorJob = scope.launch {
            val chunk = ShortArray(2048)
            var lastSampleL = 0.0
            var lastSampleR = 0.0
            var phase = 0.0

            while (isActive) {
                for (i in chunk.indices step 2) {
                    phase += 1.0 / SAMPLE_RATE

                    val sampleVal: Pair<Double, Double> = when (sound) {
                        AmbienceSound.RAIN -> {
                            // Filtered white noise with random drop transients
                            val whiteL = Random.nextDouble(-1.0, 1.0)
                            val whiteR = Random.nextDouble(-1.0, 1.0)
                            // Low pass filter
                            lastSampleL = (lastSampleL * 0.88) + (whiteL * 0.12)
                            lastSampleR = (lastSampleR * 0.88) + (whiteR * 0.12)

                            // Rare raindrop tap
                            val drop = if (Random.nextDouble() < 0.0006) Random.nextDouble(0.3, 0.7) else 0.0
                            Pair(lastSampleL * 0.45 + drop, lastSampleR * 0.45 + drop)
                        }

                        AmbienceSound.CAMPFIRE -> {
                            // Low rumble + distinct random crackles
                            val white = Random.nextDouble(-1.0, 1.0)
                            lastSampleL = (lastSampleL * 0.95) + (white * 0.05)
                            lastSampleR = (lastSampleR * 0.95) + (white * 0.05)

                            // Fire pop / snap burst
                            val pop = if (Random.nextDouble() < 0.00035) Random.nextDouble(0.5, 0.95) else 0.0
                            Pair(lastSampleL * 0.35 + pop, lastSampleR * 0.35 + pop * 0.8)
                        }

                        AmbienceSound.OCEAN -> {
                            // Modulated swell: periodic envelope (period ~ 6 seconds)
                            val waveEnvelope = (sin(phase * 2.0 * Math.PI * 0.16) + 1.0) * 0.5
                            val whiteL = Random.nextDouble(-1.0, 1.0)
                            val whiteR = Random.nextDouble(-1.0, 1.0)
                            lastSampleL = (lastSampleL * 0.96) + (whiteL * 0.04)
                            lastSampleR = (lastSampleR * 0.96) + (whiteR * 0.04)

                            val surge = 0.18 + 0.65 * waveEnvelope
                            Pair(lastSampleL * surge, lastSampleR * surge)
                        }

                        AmbienceSound.WIND -> {
                            // Slowly modulating gentle gust (period ~ 8 seconds)
                            val gust = (sin(phase * 2.0 * Math.PI * 0.12) * 0.35) + 0.55
                            val whiteL = Random.nextDouble(-1.0, 1.0)
                            val whiteR = Random.nextDouble(-1.0, 1.0)
                            lastSampleL = (lastSampleL * 0.92) + (whiteL * 0.08)
                            lastSampleR = (lastSampleR * 0.92) + (whiteR * 0.08)
                            Pair(lastSampleL * gust * 0.5, lastSampleR * gust * 0.5)
                        }

                        AmbienceSound.BROWN_NOISE -> {
                            // True brown noise integration
                            val whiteL = Random.nextDouble(-1.0, 1.0)
                            val whiteR = Random.nextDouble(-1.0, 1.0)
                            lastSampleL = (lastSampleL + (0.035 * whiteL)) / 1.035
                            lastSampleR = (lastSampleR + (0.035 * whiteR)) / 1.035
                            Pair(lastSampleL * 1.8, lastSampleR * 1.8)
                        }

                        AmbienceSound.NONE -> Pair(0.0, 0.0)
                    }

                    chunk[i] = (sampleVal.first.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
                    chunk[i + 1] = (sampleVal.second.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
                }

                audioTrack?.write(chunk, 0, chunk.size)
            }
        }
    }

    fun stop() {
        generatorJob?.cancel()
        generatorJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
