package com.auraplayer.data.repository

import android.content.Context
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

data class LrcLine(
    val timeMs: Long,
    val text: String
)

/**
 * Gestor de Letras Sincronizadas LRC en Tiempo Real.
 * Parsea marcas de tiempo [mm:ss.xx], almacena en caché local y descarga automáticamente
 * letras sincronizadas oficiales desde la API abierta de LRCLIB.
 */
class SyncedLyricsManager(private val context: Context) {

    private val lyricsDir: File = File(context.filesDir, "synced_lyrics").apply { mkdirs() }

    fun parseLrc(lrcText: String): List<LrcLine> {
        val lines = mutableListOf<LrcLine>()
        val timePattern = Pattern.compile("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]")

        lrcText.lines().forEach { line ->
            val matcher = timePattern.matcher(line)
            if (matcher.find()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val rawMillis = matcher.group(3) ?: "0"
                val ms = when (rawMillis.length) {
                    1 -> rawMillis.toLong() * 100
                    2 -> rawMillis.toLong() * 10
                    else -> rawMillis.take(3).toLong()
                }
                val totalMs = (min * 60 + sec) * 1000 + ms
                val text = line.substring(matcher.end()).trim()
                if (text.isNotBlank()) {
                    lines.add(LrcLine(totalMs, text))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    private fun getCacheFile(songId: Long): File {
        return File(lyricsDir, "lrc_$songId.lrc")
    }

    fun getLocalSyncedLyrics(songId: Long): List<LrcLine>? {
        val file = getCacheFile(songId)
        if (file.exists() && file.length() > 0) {
            return parseLrc(file.readText())
        }
        return null
    }

    fun saveSyncedLyrics(songId: Long, lrcContent: String) {
        val file = getCacheFile(songId)
        file.writeText(lrcContent)
    }

    /**
     * Busca y descarga letras sincronizadas desde la API gratuita de LRCLIB
     */
    suspend fun fetchSyncedLyrics(song: MediaModel): List<LrcLine>? = withContext(Dispatchers.IO) {
        // 1. Verificar caché local
        val local = getLocalSyncedLyrics(song.id)
        if (local != null && local.isNotEmpty()) return@withContext local

        // 2. Consultar API pública de LRCLIB
        try {
            val cleanTitle = song.title.replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "").trim()
            val cleanArtist = song.artist.replace(Regex("(?i)\\(.*?\\)|\\[.*?\\]"), "").trim()
            val encTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val durSec = song.duration / 1000

            val urlString = "https://lrclib.net/api/get?track_name=$encTitle&artist_name=$encArtist&duration=$durSec"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "DaVEPlayer/3.0 (Android)")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                if (obj.has("syncedLyrics") && !obj.isNull("syncedLyrics")) {
                    val syncedText = obj.getString("syncedLyrics")
                    if (syncedText.isNotBlank()) {
                        saveSyncedLyrics(song.id, syncedText)
                        return@withContext parseLrc(syncedText)
                    }
                } else if (obj.has("plainLyrics") && !obj.isNull("plainLyrics")) {
                    val plain = obj.getString("plainLyrics")
                    // Convertir texto plano en líneas simples espaciadas si no hay LRC
                    val pseudoLines = plain.lines().filter { it.isNotBlank() }.mapIndexed { idx, txt ->
                        LrcLine(idx * 3500L, txt)
                    }
                    return@withContext pseudoLines
                }
            }
        } catch (_: Exception) {
            // Error de red ignorado limpiamente
        }
        null
    }

    companion object {
        @Volatile
        private var instance: SyncedLyricsManager? = null

        fun getInstance(context: Context): SyncedLyricsManager {
            return instance ?: synchronized(this) {
                instance ?: SyncedLyricsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
