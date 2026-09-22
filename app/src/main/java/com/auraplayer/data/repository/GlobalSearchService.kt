package com.auraplayer.data.repository

import android.util.Log
import com.auraplayer.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GlobalSearchService(
    private val spotifyService: SpotifyMetadataService = SpotifyMetadataService()
) {

    private val jamendoClientId = "3dce8b55"
    private val tag = "GlobalSearch"

    suspend fun searchOrExtract(input: String, selectedSource: String = "Todas"): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        if (isUrl(trimmed)) return@withContext extractFromUrl(trimmed)
        searchFederated(trimmed, selectedSource)
    }

    suspend fun getTrending(genre: String = "Trending", selectedSource: String = "Todas"): List<OnlineTrack> = withContext(Dispatchers.IO) {
        searchFederated(if (genre == "Trending" || genre == "Todas") "" else genre, selectedSource, isTrending = true)
    }

    private fun isUrl(text: String): Boolean {
        return text.startsWith("http://", ignoreCase = true) ||
               text.startsWith("https://", ignoreCase = true) ||
               text.contains("tiktok.com", ignoreCase = true)
    }

    private suspend fun extractFromUrl(urlStr: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val list = mutableListOf<OnlineTrack>()

        if (urlStr.contains("tiktok.com", ignoreCase = true)) {
            try {
                val encodedUrl = URLEncoder.encode(urlStr, "UTF-8")
                val apiUrl = "https://www.tikwm.com/api/?url=$encodedUrl"
                val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                if (conn.responseCode == 200) {
                    val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    if (root.optInt("code", -1) == 0) {
                        val data = root.optJSONObject("data")
                        val musicInfo = data?.optJSONObject("music_info")
                        val title = musicInfo?.optString("title", data?.optString("title", "Audio TikTok")) ?: "Audio TikTok"
                        val author = musicInfo?.optString("author", data?.optJSONObject("author")?.optString("nickname", "TikTok") ?: "TikTok") ?: "TikTok"
                        val cover = musicInfo?.optString("cover", data?.optString("cover", "")) ?: ""

                        val videoDuration = data?.optInt("duration", 0) ?: 0
                        val musicDuration = musicInfo?.optInt("duration", 0) ?: 0
                        val realDuration = if (videoDuration > 0) videoDuration else if (musicDuration > 0) musicDuration else 60

                        val musicUrl = data?.optString("music", "") ?: ""
                        val playUrl = data?.optString("play", "") ?: ""

                        // Priority: In TikTok, 'music' catalog is limited to 60s snippet.
                        // 'play' is the full video containing the complete 4+ minute soundtrack without limits.
                        val audioUrl = if (videoDuration > musicDuration && playUrl.isNotBlank()) {
                            playUrl
                        } else if (musicUrl.isNotBlank()) {
                            musicUrl
                        } else {
                            playUrl
                        }

                        if (audioUrl.isNotBlank()) {
                            list.add(
                                OnlineTrack(
                                    id = "tiktok_${System.currentTimeMillis()}",
                                    title = title.take(50),
                                    artist = author,
                                    album = "TikTok Audio Completo",
                                    durationSec = realDuration,
                                    audioUrl = audioUrl,
                                    coverUrl = cover,
                                    format = if (audioUrl == playUrl) "Audio HD Completo" else "MP3 Completo",
                                    bitrateKbps = 320,
                                    license = "Pista Completa",
                                    source = "TikTok",
                                    isDownloadable = true
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else if (urlStr.endsWith(".mp3", true) || urlStr.endsWith(".m4a", true) ||
                   urlStr.endsWith(".aac", true) || urlStr.endsWith(".ogg", true)) {
            val fileName = urlStr.substringAfterLast("/").substringBefore("?")
            list.add(OnlineTrack(
                id = "direct_${System.currentTimeMillis()}",
                title = fileName.ifBlank { "Pista Completa Web" },
                artist = "Enlace Directo",
                album = "Descarga Directa",
                durationSec = 0,
                audioUrl = urlStr,
                coverUrl = "",
                format = "MP3 Completo",
                bitrateKbps = 320,
                license = "Pista Completa",
                source = "Enlace Web",
                isDownloadable = true
            ))
        }
        list
    }

    private suspend fun searchFederated(query: String, selectedSource: String, isTrending: Boolean = false): List<OnlineTrack> = coroutineScope {
        val rawResults = queryJamendo(query, isTrending)

        // Enrich each track with HD artwork in parallel (Spotify → Deezer → iTunes fallback)
        rawResults.map { track ->
            async(Dispatchers.IO) {
                val meta = try { spotifyService.fetchMeta(track.title, track.artist) } catch (_: Exception) { null }
                if (meta != null && meta.coverUrl.isNotBlank()) {
                    track.copy(
                        coverUrl = meta.coverUrl,
                        album = meta.albumName.ifBlank { track.album }
                    )
                } else {
                    track
                }
            }
        }.awaitAll()
    }

    /**
     * Resolves the final direct CDN URL by following all HTTP redirects from a Jamendo
     * audiodownload URL. This bypasses Jamendo's 1-minute streaming restriction.
     * Returns the original URL if resolution fails.
     */
    private fun resolveFullUrl(originalUrl: String): String {
        return try {
            var currentUrl = originalUrl
            var hops = 0
            while (hops < 6) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 6000
                    readTimeout = 6000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 AuraPlayer/1.8.3")
                    setRequestProperty("Accept", "audio/mpeg,audio/*,*/*")
                }
                val code = conn.responseCode
                Log.d(tag, "resolveFullUrl hop $hops: $currentUrl → $code")
                when (code) {
                    in 300..308 -> {
                        val location = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (location.isNullOrBlank()) break
                        // Resolve relative URLs
                        currentUrl = if (location.startsWith("http")) location
                                     else URL(URL(currentUrl), location).toString()
                        hops++
                    }
                    200 -> {
                        conn.disconnect()
                        Log.d(tag, "resolveFullUrl final URL: $currentUrl")
                        return currentUrl
                    }
                    else -> {
                        conn.disconnect()
                        break
                    }
                }
            }
            originalUrl // fallback
        } catch (e: Exception) {
            Log.e(tag, "resolveFullUrl error for $originalUrl: ${e.message}")
            originalUrl // fallback
        }
    }

    private fun queryJamendo(query: String, isTrending: Boolean): List<OnlineTrack> {
        val list = mutableListOf<OnlineTrack>()
        try {
            val urlStr = if (isTrending || query.isBlank()) {
                "https://api.jamendo.com/v3.0/tracks/?client_id=$jamendoClientId&format=json&limit=30&order=popularity_week&audioformat=mp32&include=musicinfo"
            } else {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                "https://api.jamendo.com/v3.0/tracks/?client_id=$jamendoClientId&format=json&limit=30&search=$encoded&order=popularity_total&audioformat=mp32&include=musicinfo"
            }

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 9000
                readTimeout = 9000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AuraPlayer/1.8.3 (Android)")
            }

            if (conn.responseCode != 200) return list

            val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val results = root.optJSONArray("results") ?: return list

            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val allowed = item.optBoolean("audiodownload_allowed", true)
                val duration = item.optInt("duration", 0)

                if (!allowed || duration < 45) continue

                val trackId = item.optString("id", "")
                val name = item.optString("name", "Canción").trim()
                val artist = item.optString("artist_name", "Artista").trim()
                val album = item.optString("album_name", "Álbum").trim()

                // ──────────────────────────────────────────────────────────────────────
                // AUDIO URL STRATEGY (from most reliable to least):
                //
                // 1. audiodownload → redirect chain → final CDN URL (full file, no limits)
                // 2. audio field   → signed streaming URL (full song but expires faster)
                // 3. Constructed   → direct storage URL by track ID (no expiry)
                // ──────────────────────────────────────────────────────────────────────

                val downloadEndpoint = item.optString("audiodownload", "")
                val streamEndpoint   = item.optString("audio", "")

                // Resolve audiodownload redirects to get the real CDN URL (full song, unlimited)
                val resolvedUrl = if (downloadEndpoint.isNotBlank()) {
                    resolveFullUrl(downloadEndpoint)
                } else if (streamEndpoint.isNotBlank()) {
                    resolveFullUrl(streamEndpoint)
                } else {
                    // Fallback: construct direct storage URL
                    "https://prod-1.storage.jamendo.com/download/track/$trackId/mp32/"
                }

                var image = item.optString("image", "").ifBlank { item.optString("album_image", "") }
                if (image.contains("width=300")) image = image.replace("width=300", "width=500")

                list.add(OnlineTrack(
                    id = "jam_$trackId",
                    title = name,
                    artist = artist,
                    album = album,
                    durationSec = duration,
                    audioUrl = resolvedUrl,
                    coverUrl = image,
                    format = "MP3 Completo",
                    bitrateKbps = 320,
                    license = "Canción Completa",
                    source = "Jamendo",
                    isDownloadable = true
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
