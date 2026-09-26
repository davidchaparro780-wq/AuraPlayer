package com.auraplayer.data.repository

import android.media.MediaMetadataRetriever
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class AudioQualityInfo(
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val channels: Int,
    val codec: String,
    val fileSizeMb: Float,
    val durationMs: Long,
    val qualityLabel: String,
    val qualityColor: Long // ARGB
)

object AudioQualityAnalyzer {

    suspend fun analyze(song: MediaModel): AudioQualityInfo = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        return@withContext try {
            retriever.setDataSource(song.path)

            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toLongOrNull()?.div(1000)?.toInt() ?: estimateBitrateFromFile(song)

            val sampleRate = 44100 // Standard — retriever doesn't expose this directly on all APIs

            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/*"
            val codec = when {
                mime.contains("flac", ignoreCase = true) -> "FLAC"
                mime.contains("opus", ignoreCase = true) -> "Opus"
                mime.contains("ogg", ignoreCase = true) -> "Ogg Vorbis"
                mime.contains("mp4") || mime.contains("aac") -> "AAC"
                mime.contains("mpeg") || mime.contains("mp3") -> "MP3"
                mime.contains("wav") -> "WAV PCM"
                else -> "Audio"
            }

            val fileSizeMb = try { File(song.path).length().toFloat() / (1024f * 1024f) } catch (_: Exception) { 0f }
            val durationMs = song.duration

            val (label, color) = when {
                codec == "FLAC" || codec == "WAV PCM" -> Pair("Lossless 🏆", 0xFF00E5FF)
                bitrate >= 256 -> Pair("Excelente ⭐", 0xFF00C853)
                bitrate >= 192 -> Pair("Muy Buena ✅", 0xFF76FF03)
                bitrate >= 128 -> Pair("Buena 👍", 0xFFFFD600)
                bitrate >= 64 -> Pair("Baja ⚠️", 0xFFFF6D00)
                else -> Pair("Muy Baja ❌", 0xFFD50000)
            }

            AudioQualityInfo(
                bitrateKbps = bitrate,
                sampleRateHz = sampleRate,
                channels = 2,
                codec = codec,
                fileSizeMb = fileSizeMb,
                durationMs = durationMs,
                qualityLabel = label,
                qualityColor = color
            )
        } catch (e: Exception) {
            AudioQualityInfo(0, 0, 0, "Desconocido", 0f, 0L, "Desconocido", 0xFF888888)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun estimateBitrateFromFile(song: MediaModel): Int {
        return try {
            val sizeBytes = File(song.path).length()
            val durationSec = (song.duration / 1000L).coerceAtLeast(1L)
            ((sizeBytes * 8L) / durationSec / 1000L).toInt()
        } catch (_: Exception) { 128 }
    }
}
