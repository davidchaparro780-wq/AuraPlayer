package com.auraplayer.data.repository

import com.auraplayer.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GlobalSearchService {

    private val jamendoClientId = "3dce8b55"

    /**
     * Omnibar Entry Point: Determines whether the query is a URL or search text,
     * and queries the appropriate services.
     */
    suspend fun searchOrExtract(input: String, selectedSource: String = "Todas"): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        // 1. URL Detection
        if (isUrl(trimmed)) {
            return@withContext extractFromUrl(trimmed)
        }

        // 2. Federated Multi-Source Search
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
     * Extracts audio stream and metadata from supported URLs (TikTok via TikWM, Direct Streams)
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
                        val title = musicInfo?.optString("title", data?.optString("title", "Audio de TikTok")) ?: "Audio de TikTok"
                        val author = musicInfo?.optString("author", data?.optJSONObject("author")?.optString("nickname", "TikTok")) ?: "TikTok"
                        val cover = musicInfo?.optString("cover", data?.optString("cover", "")) ?: ""
                        val duration = musicInfo?.optInt("duration", 30) ?: 30

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
                                    format = "MP3 Full",
                                    bitrateKbps = 192,
                                    license = "TikTok Audio",
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
                    title = fileName.ifBlank { "Pista de Audio Web" },
                    artist = "Enlace Directo",
                    album = "Descarga Web",
                    durationSec = 0,
                    audioUrl = urlStr,
                    coverUrl = "",
                    format = "MP3 Completo",
                    bitrateKbps = 320,
                    license = "Web Direct",
                    source = "Enlace Web",
                    isDownloadable = true
                )
            )
        }

        list
    }

    /**
     * Executes parallel queries against Jamendo, Internet Archive, and Deezer APIs.
     * Prioritizes full-length downloadable tracks first!
     */
    private suspend fun searchFederated(query: String, selectedSource: String, isTrending: Boolean = false): List<OnlineTrack> = coroutineScope {
        val fullSongs = mutableListOf<OnlineTrack>()
        val previewSongs = mutableListOf<OnlineTrack>()

        val includeJamendo = selectedSource == "Todas" || selectedSource == "Jamendo"
        val includeDeezer = selectedSource == "Todas" || selectedSource == "Deezer"
        val includeArchive = selectedSource == "Todas" || selectedSource == "Archive"

        val jamendoDeferred = if (includeJamendo) async { queryJamendo(query, isTrending) } else null
        val archiveDeferred = if (includeArchive && query.isNotBlank() && !isTrending) async { queryArchive(query) } else null
        val deezerDeferred = if (includeDeezer) async { queryDeezer(query, isTrending) } else null

        jamendoDeferred?.await()?.let { fullSongs.addAll(it) }
        archiveDeferred?.await()?.let { fullSongs.addAll(it) }
        deezerDeferred?.await()?.let { previewSongs.addAll(it) }

        // Full downloadable tracks appear first!
        val combined = mutableListOf<OnlineTrack>()
        combined.addAll(fullSongs)
        combined.addAll(previewSongs)
        combined
    }

    private fun queryJamendo(query: String, isTrending: Boolean): List<OnlineTrack> {
        val list = mutableListOf<OnlineTrack>()
        try {
            val urlStr = if (isTrending || query.isBlank()) {
                "https://api.jamendo.com/v3.0/tracks/?client_id=$jamendoClientId&format=json&limit=30&order=popularity_week&audioformat=mp32&include=musicinfo"
            } else {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                // Use search= to search title, artist, album and tags all together!
                "https://api.jamendo.com/v3.0/tracks/?client_id=$jamendoClientId&format=json&limit=30&search=$encoded&order=popularity_total&audioformat=mp32&include=musicinfo"
            }

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AuraPlayer/1.7.5 (Android)")
            }

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(json)
                val results = root.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val allowed = item.optBoolean("audiodownload_allowed", true)
                        val audioUrl = item.optString("audiodownload", "").ifBlank {
                            item.optString("audio", "")
                        }
                        if (allowed && audioUrl.isNotBlank()) {
                            val id = item.optString("id", System.currentTimeMillis().toString())
                            val name = item.optString("name", "Canción Completa").trim()
                            val artist = item.optString("artist_name", "Artista").trim()
                            val album = item.optString("album_name", "Álbum").trim()
                            val duration = item.optInt("duration", 0)
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

    private fun queryDeezer(query: String, isTrending: Boolean): List<OnlineTrack> {
        val list = mutableListOf<OnlineTrack>()
        try {
            val urlStr = if (isTrending || query.isBlank()) {
                "https://api.deezer.com/chart/0/tracks?limit=20"
            } else {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                "https://api.deezer.com/search?q=$encoded&limit=20"
            }

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AuraPlayer/1.7.5 (Android)")
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(response)
                val data = root.optJSONArray("data")
                if (data != null) {
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        val id = item.optString("id", System.currentTimeMillis().toString())
                        val title = item.optString("title", "Sin título")
                        val artist = item.optJSONObject("artist")?.optString("name") ?: "Artista"
                        val album = item.optJSONObject("album")?.optString("title") ?: "Álbum"
                        val audioUrl = item.optString("preview", "")
                        val cover = item.optJSONObject("album")?.optString("cover_big")
                            ?: item.optJSONObject("album")?.optString("cover_medium") ?: ""

                        if (audioUrl.isNotBlank()) {
                            list.add(
                                OnlineTrack(
                                    id = "dz_$id",
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    durationSec = 30, // Preview stream is 30s!
                                    audioUrl = audioUrl,
                                    coverUrl = cover,
                                    format = "Muestra 30s",
                                    bitrateKbps = 128,
                                    license = "Preview Deezer",
                                    source = "Deezer",
                                    isDownloadable = false
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

    private fun queryArchive(query: String): List<OnlineTrack> {
        val list = mutableListOf<OnlineTrack>()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val urlStr = "https://archive.org/advancedsearch.php?q=mediatype:(audio)+AND+($encoded)&fl[]=identifier,title,creator,year&sort[]=downloads+desc&rows=15&page=1&output=json"

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AuraPlayer/1.7.5 (Android)")
            }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(response)
                val docs = root.optJSONObject("response")?.optJSONArray("docs")
                if (docs != null) {
                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        val id = doc.optString("identifier", "")
                        val title = doc.optString("title", "Audio Libre").trim()
                        val creator = doc.optString("creator", "Internet Archive").trim()
                        val year = doc.optString("year", "")

                        if (id.isNotBlank()) {
                            val audioUrl = "https://archive.org/download/$id/${id}_vbr.mp3"
                            val coverUrl = "https://archive.org/services/img/$id"

                            list.add(
                                OnlineTrack(
                                    id = "arc_$id",
                                    title = title,
                                    artist = creator.take(30),
                                    album = if (year.isNotBlank()) "Archive ($year)" else "Internet Archive",
                                    durationSec = 0,
                                    audioUrl = audioUrl,
                                    coverUrl = coverUrl,
                                    format = "MP3 Completo",
                                    bitrateKbps = 192,
                                    license = "Dominio Público",
                                    source = "Archive",
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
