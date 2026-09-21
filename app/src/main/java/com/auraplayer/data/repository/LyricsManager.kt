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

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class SongLyrics(
    val isSynced: Boolean,
    val lines: List<LyricLine>,
    val plainLyrics: String?
)

class LyricsManager(private val context: Context) {

    private val cacheDir = File(context.filesDir, "lyrics").apply { mkdirs() }
    private val lrcPattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]\\s*(.*)")

    suspend fun getLyrics(song: MediaModel): SongLyrics? = withContext(Dispatchers.IO) {
        // 1. Check local cache first
        val cached = getCachedLyrics(song.id)
        if (cached != null) return@withContext cached

        // 2. Query LRCLIB API
        try {
            val cleanTitle = cleanSongTitle(song.title)
            val cleanArtist = cleanArtistName(song.artist)

            val urlString = "https://lrclib.net/api/get?artist_name=" +
                    URLEncoder.encode(cleanArtist, "UTF-8") +
                    "&track_name=" + URLEncoder.encode(cleanTitle, "UTF-8")

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AuraPlayer/1.3.0 (Android)")
            }

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = JSONObject(jsonStr)

                val syncedLyrics = jsonObj.optString("syncedLyrics", "").ifBlank { null }
                val plainLyrics = jsonObj.optString("plainLyrics", "").ifBlank { null }

                if (syncedLyrics != null || plainLyrics != null) {
                    saveToCache(song.id, jsonStr)
                    return@withContext parseLyrics(syncedLyrics, plainLyrics)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

    private fun parseLyrics(syncedLyrics: String?, plainLyrics: String?): SongLyrics {
        if (!syncedLyrics.isNullOrBlank()) {
            val lines = mutableListOf<LyricLine>()
            syncedLyrics.lineSequence().forEach { line ->
                val matcher = lrcPattern.matcher(line)
                if (matcher.matches()) {
                    val min = matcher.group(1)?.toLongOrNull() ?: 0L
                    val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                    val millisStr = matcher.group(3) ?: "0"
                    val millis = when (millisStr.length) {
                        2 -> (millisStr.toLongOrNull() ?: 0L) * 10
                        else -> millisStr.toLongOrNull() ?: 0L
                    }
                    val timeMs = min * 60000 + sec * 1000 + millis
                    val text = matcher.group(4)?.trim() ?: ""
                    if (text.isNotEmpty() && text != "♪") {
                        lines.add(LyricLine(timeMs, text))
                    }
                }
            }
            if (lines.isNotEmpty()) {
                return SongLyrics(isSynced = true, lines = lines, plainLyrics = plainLyrics)
            }
        }

        // Fallback to plain lyrics
        return SongLyrics(isSynced = false, lines = emptyList(), plainLyrics = plainLyrics)
    }

    private fun getCachedLyrics(songId: Long): SongLyrics? {
        val file = File(cacheDir, "$songId.json")
        if (!file.exists()) return null
        return try {
            val jsonStr = file.readText()
            val jsonObj = JSONObject(jsonStr)
            val synced = jsonObj.optString("syncedLyrics", "").ifBlank { null }
            val plain = jsonObj.optString("plainLyrics", "").ifBlank { null }
            parseLyrics(synced, plain)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveToCache(songId: Long, jsonStr: String) {
        try {
            File(cacheDir, "$songId.json").writeText(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cleanSongTitle(title: String): String {
        return title
            .replace(Regex("\\(.*\\)|\\[.*\\]"), "")
            .replace(Regex("(?i)remix|feat|ft\\.|official|video|audio|lyrics"), "")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .replace(Regex("\\(.*\\)|\\[.*\\]"), "")
            .replace(Regex("(?i)feat.*|ft\\..*"), "")
            .trim()
    }
}
