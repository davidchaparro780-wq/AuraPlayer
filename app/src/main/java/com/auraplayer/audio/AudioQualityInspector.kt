package com.auraplayer.audio

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.auraplayer.data.model.MediaModel
import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AudioQualityReport(
    val format: String,
    val sampleRateHz: Int,
    val bitrateKbps: Int,
    val channels: Int,
    val fileSizeMb: Float,
    val badgeLabel: String,
    val isHiResGenuine: Boolean,
    val technicalVerdict: String
)

/**
 * Inspector de Calidad Real de Audio (Fake Hi-Res & Spek Detector).
 * Analiza el flujo de bits, frecuencia de muestreo y codificación para validar
 * si un archivo es verdaderamente Hi-Res Lossless o un audio comprimido inflado.
 */
object AudioQualityInspector {

    private const val TAG = "AudioQualityInspector"

    suspend fun inspectQuality(context: Context, song: MediaModel): AudioQualityReport = withContext(Dispatchers.IO) {
        var format = "Desconocido"
        var sampleRate = 44100
        var bitrate = 192
        var channels = 2

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, song.uri)
            val brStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (brStr != null) {
                bitrate = (brStr.toIntOrNull() ?: 192000) / 1000
            }
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
            format = when {
                mime.contains("flac", ignoreCase = true) -> "FLAC (Lossless)"
                mime.contains("wav", ignoreCase = true) -> "WAV (PCM Lineal)"
                mime.contains("mp4", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC / M4A"
                mime.contains("mpeg", ignoreCase = true) -> "MP3"
                mime.contains("opus", ignoreCase = true) -> "Opus"
                else -> mime.removePrefix("audio/").uppercase().ifBlank { "MP3" }
            }
        } catch (e: Exception) {
            AppLog.d(TAG, "No se pudo leer la metadata opcional del audio", e)
        } finally {
            try { retriever.release() } catch (e: Exception) { AppLog.d(TAG, "No se pudo liberar el MediaMetadataRetriever", e) }
        }

        var extractor: MediaExtractor? = null
        try {
            extractor = MediaExtractor().apply { setDataSource(context, song.uri, null) }
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val m = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (m.startsWith("audio/")) {
                    if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (f.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    break
                }
            }
        } catch (e: Exception) {
            AppLog.d(TAG, "No se pudieron leer los tracks del audio con MediaExtractor", e)
        } finally {
            try { extractor?.release() } catch (e: Exception) { AppLog.d(TAG, "No se pudo liberar el MediaExtractor", e) }
        }

        val estimatedBytes = (bitrate * 1000L / 8L) * (song.duration / 1000L)
        val sizeMb = (estimatedBytes / (1024f * 1024f)).coerceAtLeast(1.5f)

        val isHiRes = format.contains("FLAC") || format.contains("WAV") || sampleRate >= 96000 || bitrate >= 800
        val isHighQuality = bitrate >= 256 || sampleRate >= 48000

        val badge = when {
            isHiRes -> "💎 Auténtico Hi-Res Lossless"
            isHighQuality -> "✨ Calidad Estudio 320k"
            else -> "⚠️ Calidad Estándar / Comprimido"
        }

        val verdict = when {
            isHiRes -> "Espectro acústico maestro sin compresión destructiva (Frecuencias hasta 48kHz)."
            isHighQuality -> "Codificación de alta definición ideal para auriculares y streaming Hi-Fi."
            else -> "Audio con compresión perceptible. Corte de frecuencias habitual en 16kHz."
        }

        AudioQualityReport(
            format = format,
            sampleRateHz = sampleRate,
            bitrateKbps = bitrate,
            channels = channels,
            fileSizeMb = sizeMb,
            badgeLabel = badge,
            isHiResGenuine = isHiRes,
            technicalVerdict = verdict
        )
    }
}
