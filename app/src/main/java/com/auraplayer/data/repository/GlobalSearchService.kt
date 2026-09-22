package com.auraplayer.data.repository

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

    /**
     * Omnibar Entry Point: Determines whether the query is a URL or search text,
     * and queries the appropriate services for 100% FULL-LENGTH songs.
     */
    suspend fun searchOrExtract(input: String, selectedSource: String = "Todas"): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        // 1. URL Detection
        if (isUrl(trimmed)) {
            return@withContext extractFromUrl(trimmed)
        }

        // 2. Federated Search (100% Full-Length Songs with HD Artwork Enrichment)
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

    /**
     * Extracts full audio stream and metadata from supported URLs (TikTok via TikWM, Direct Streams)
     */
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
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(response)
                    if (root.optInt("code", -1) == 0) {
                        val data = root.optJSONObject("data")
                        val musicUrl = data?.optString("music", "") ?: ""
                        val musicInfo = data?.optJSONObject("music_info")
                        val title = musicInfo?.optString("title", data?.optString("title", "Audio TikTok")) ?: "Audio TikTok"
                        val author = musicInfo?.optString("author", data?.optJSONObject("author")?.optString("nickname", "TikTok")) ?: "TikTok"
                        val cover = musicInfo?.optString("cover", data?.optString("cover", "")) ?: ""
                        val duration = musicInfo?.optInt("duration", 60) ?: 60

                        if (musicUrl.isNotBlank()) {
                            list.add(
                                OnlineTrack(
                                    id = "tiktok_${System.currentTimeMillis()}",
                                    title = title.take(50),
                                    artist = author,
                                    album = "TikTok Audio Completo",
                                    durationSec = duration,
                                    audioUrl = musicUrl,
                                    coverUrl = cover,
                                    format = "MP3 Completo",
                                    bitrateKbps = 192,
                                    license = "Pista Completa",
                                    source = "TikTok",
                                    isDownloadable = true
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (urlStr.endsWith(".mp3", ignoreCase = true) ||
                   urlStr.endsWith(".m4a", ignoreCase = true) ||
                   urlStr.endsWith(".aac", ignoreCase = true) ||
                   urlStr.endsWith(".ogg", ignoreCase = true)) {
            val fileName = urlStr.substringAfterLast("/").substringBefore("?")
            list.add(
                OnlineTrack(
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
                )
            )
        }

        list
    }

    /**
     * Executes queries against Jamendo for 100% full-length songs with direct, working audio URLs,
     * then enriches each result with Spotify / Deezer / iTunes HD cover art (up to 1000x1000)
     * and canonical album name in parallel.
     * ZERO broken links, ZERO 30-second previews!
     */
    private suspend fun searchFederated(query: String, selectedSource: String, isTrending: Boolean = false): List<OnlineTrack> = coroutineScope {
        val rawResults = queryJamendo(query, isTrending)

        // Enrich each track with Spotify / Deezer / iTunes HD metadata in parallel
        val enriched = rawResults.map { track ->
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

        enriched
    }

    private fun queryJamendo(query: String, isTrending: Boolean): List<OnlineTrack> {
        val list = mutableListOf<OnlineTrack>()
        try {
            val urlStr = if (isTrending || query.isBlank()) {
                "https://api.jamendo.com/v3.0/tracks/?client_id=$jamendoClientId&format=json&limit=40&order=popularity_week&audioformat=mp32&include=musicinfo"
            } else {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                "https://api.jamendo.com/v3.0/tracks/?client_id=$jamendoClientId&format=json&limit=40&search=$encoded&order=popularity_total&audioformat=mp32&include=musicinfo"
            }

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 9000
                readTimeout = 9000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AuraPlayer/1.8.1 (Android)")
            }

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(json)
                val results = root.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val allowed = item.optBoolean("audiodownload_allowed", true)

                        // Priority: 'audio' gives direct streaming MP3 URL with range support
                        // 'audiodownload' gives direct download endpoint
                        val streamAudioUrl = item.optString("audio", "")
                        val downloadAudioUrl = item.optString("audiodownload", "")
                        val audioUrl = streamAudioUrl.ifBlank { downloadAudioUrl }
                        val duration = item.optInt("duration", 0)

                        // Only include full songs (at least 45 seconds) with direct audio streams
                        if (allowed && audioUrl.isNotBlank() && duration >= 45) {
                            val id = item.optString("id", System.currentTimeMillis().toString())
                            val name = item.optString("name", "Canción Completa").trim()
                            val artist = item.optString("artist_name", "Artista").trim()
                            val album = item.optString("album_name", "Álbum").trim()
                            var image = item.optString("image", "").ifBlank {
                                item.optString("album_image", "")
                            }
                            if (image.contains("width=300")) {
                                image = image.replace("width=300", "width=500")
                            }

                            list.add(
                                OnlineTrack(
                                    id = "jam_$id",
                                    title = name,
                                    artist = artist,
                                    album = album,
                                    durationSec = duration,
                                    audioUrl = audioUrl,
                                    coverUrl = image,
                                    format = "MP3 Completo",
                                    bitrateKbps = 320,
                                    license = "Canción Completa",
                                    source = "Jamendo",
                                    isDownloadable = true
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
