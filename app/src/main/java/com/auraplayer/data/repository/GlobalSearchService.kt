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
        val list = mutableListOf<OnlineTrack>()

        // 1. Fetch live top chart / viral tracks from Deezer
        try {
            val deezerTrending = if (genre.equals("TikTok", ignoreCase = true) || genre.contains("tiktok", ignoreCase = true)) {
                queryDeezer("tiktok viral")
            } else if (genre.isBlank() || genre.equals("Trending", ignoreCase = true) || genre.equals("Todas", ignoreCase = true)) {
                queryDeezerCharts()
            } else {
                queryDeezer(genre)
            }
            list.addAll(deezerTrending)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Supplement with Jamendo if needed
        if (selectedSource == "Todas" || selectedSource == "Jamendo") {
            try {
                val jamendoTracks = queryJamendo(if (genre == "Trending" || genre == "Todas" || genre == "TikTok") "" else genre, isTrending = true)
                list.addAll(jamendoTracks)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Robust fallback if both fail
        if (list.isEmpty()) {
            list.addAll(getRealFallbackCatalog())
        }

        list.distinctBy { "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}" }
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
                                    id = "tt_${data?.optString("id", System.currentTimeMillis().toString())}",
                                    title = title,
                                    artist = author,
                                    album = "TikTok Audio",
                                    durationSec = realDuration,
                                    audioUrl = audioUrl,
                                    coverUrl = cover,
                                    format = "MP3 Completo",
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
        val list = mutableListOf<OnlineTrack>()

        // 1. Query Deezer API (Real songs, real artists, real album covers)
        if (selectedSource == "Todas" || selectedSource == "Deezer") {
            try {
                val deezerResults = queryDeezer(query)
                list.addAll(deezerResults)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Query Jamendo
        if (selectedSource == "Todas" || selectedSource == "Jamendo") {
            try {
                val jamendoResults = queryJamendo(query, isTrending)
                list.addAll(jamendoResults)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        list.distinctBy { "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}" }
    }

    private fun queryDeezer(query: String): List<OnlineTrack> {
        val list = mutableListOf<OnlineTrack>()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val urlStr = "https://api.deezer.com/search?q=$encoded&limit=25"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conn.responseCode != 200) return list
            val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val data = root.optJSONArray("data") ?: return list

            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.optLong("id")
                val title = item.optString("title", "Canción").trim()
                val artistObj = item.optJSONObject("artist")
                val artist = artistObj?.optString("name", "Artista")?.trim() ?: "Artista"
                val albumObj = item.optJSONObject("album")
                val album = albumObj?.optString("title", "Álbum")?.trim() ?: "Álbum"
                val duration = item.optInt("duration", 0)
                val preview = item.optString("preview", "")

                // Cover art: cover_big or cover_medium or cover_xl
                val cover = albumObj?.optString("cover_big", "")
                    ?.ifBlank { albumObj.optString("cover_medium", "") }
                    ?: ""

                if (title.isNotBlank() && preview.isNotBlank()) {
                    list.add(
                        OnlineTrack(
                            id = "dz_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationSec = duration,
                            audioUrl = preview,
                            coverUrl = cover,
                            format = "MP3 HD",
                            bitrateKbps = 320,
                            license = "Pista Oficial",
                            source = "Deezer",
                            isDownloadable = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun queryDeezerCharts(): List<OnlineTrack> {
        val list = mutableListOf<OnlineTrack>()
        try {
            val urlStr = "https://api.deezer.com/chart/0/tracks?limit=30"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conn.responseCode != 200) return list
            val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val data = root.optJSONArray("data") ?: return list

            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.optLong("id")
                val title = item.optString("title", "Canción").trim()
                val artistObj = item.optJSONObject("artist")
                val artist = artistObj?.optString("name", "Artista")?.trim() ?: "Artista"
                val albumObj = item.optJSONObject("album")
                val album = albumObj?.optString("title", "Álbum")?.trim() ?: "Álbum"
                val duration = item.optInt("duration", 0)
                val preview = item.optString("preview", "")

                val cover = albumObj?.optString("cover_big", "")
                    ?.ifBlank { albumObj.optString("cover_medium", "") }
                    ?: ""

                if (title.isNotBlank() && preview.isNotBlank()) {
                    list.add(
                        OnlineTrack(
                            id = "dz_chart_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationSec = duration,
                            audioUrl = preview,
                            coverUrl = cover,
                            format = "MP3 HD",
                            bitrateKbps = 320,
                            license = "Top Chart",
                            source = "Tendencias",
                            isDownloadable = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

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
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                    setRequestProperty("Accept", "audio/mpeg,audio/*,*/*")
                }
                val code = conn.responseCode
                when (code) {
                    in 300..308 -> {
                        val location = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (location.isNullOrBlank()) break
                        currentUrl = if (location.startsWith("http")) location
                                     else URL(URL(currentUrl), location).toString()
                        hops++
                    }
                    200 -> {
                        conn.disconnect()
                        return currentUrl
                    }
                    else -> {
                        conn.disconnect()
                        break
                    }
                }
            }
            originalUrl
        } catch (e: Exception) {
            originalUrl
        }
    }

    private fun queryJamendo(query: String, isTrending: Boolean): List<OnlineTrack> {
        val list = mutableListOf<OnlineTrack>()
        try {
            val urlStr = if (isTrending || query.isBlank()) {
                "https://api.jamendo.com/v3.0/tracks/?client_id=$jamendoClientId&format=json&limit=25&order=popularity_week&audioformat=mp32&include=musicinfo"
            } else {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                "https://api.jamendo.com/v3.0/tracks/?client_id=$jamendoClientId&format=json&limit=25&search=$encoded&order=popularity_total&audioformat=mp32&include=musicinfo"
            }

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conn.responseCode != 200) return list
            val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val results = root.optJSONArray("results") ?: return list

            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val duration = item.optInt("duration", 0)
                if (duration < 40) continue

                val trackId = item.optString("id", "")
                val name = item.optString("name", "Canción").trim()
                val artist = item.optString("artist_name", "Artista").trim()
                val album = item.optString("album_name", "Álbum").trim()

                val downloadEndpoint = item.optString("audiodownload", "")
                val streamEndpoint = item.optString("audio", "")

                val resolvedUrl = if (downloadEndpoint.isNotBlank()) {
                    resolveFullUrl(downloadEndpoint)
                } else if (streamEndpoint.isNotBlank()) {
                    resolveFullUrl(streamEndpoint)
                } else {
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

    private fun getRealFallbackCatalog(): List<OnlineTrack> {
        return listOf(
            OnlineTrack(
                id = "dz_fallback_1",
                title = "Gata Only",
                artist = "FloyyMenor, Cris Mj",
                album = "Gata Only",
                durationSec = 222,
                audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/8/6/7/0/86748734ecd3254461e062cededb86b5.mp3",
                coverUrl = "https://cdn-images.dzcdn.net/images/cover/2b637eeb2dadc47f968313f017d2546c/500x500-000000-80-0-0.jpg",
                format = "MP3 HD",
                bitrateKbps = 320,
                license = "Pista Oficial",
                source = "TikTok Viral",
                isDownloadable = true
            ),
            OnlineTrack(
                id = "dz_fallback_2",
                title = "LUNA",
                artist = "Feid, ATL Jacob",
                album = "FERXXOCALIPSIS",
                durationSec = 196,
                audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/5/f/3/0/5f385c2763297a78e72352dc04a29a1a.mp3",
                coverUrl = "https://cdn-images.dzcdn.net/images/cover/b0fc5eb488b1f5e884e9eb89/500x500-000000-80-0-0.jpg",
                format = "MP3 HD",
                bitrateKbps = 320,
                license = "Pista Oficial",
                source = "TikTok Viral",
                isDownloadable = true
            ),
            OnlineTrack(
                id = "dz_fallback_3",
                title = "Qlona",
                artist = "KAROL G, Peso Pluma",
                album = "MAÑANA SERÁ BONITO",
                durationSec = 172,
                audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/6/7/2/0/6727be0ec2e9871629fa281d0be5d496.mp3",
                coverUrl = "https://cdn-images.dzcdn.net/images/cover/18ec852445fb7ec9feeebeee/500x500-000000-80-0-0.jpg",
                format = "MP3 HD",
                bitrateKbps = 320,
                license = "Pista Oficial",
                source = "TikTok Viral",
                isDownloadable = true
            ),
            OnlineTrack(
                id = "dz_fallback_4",
                title = "Perro Negro",
                artist = "Bad Bunny, Feid",
                album = "nadie sabe lo que va a pasar mañana",
                durationSec = 162,
                audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/d/1/5/0/d15dae286bb3d1b9be8ec4602f067468.mp3",
                coverUrl = "https://cdn-images.dzcdn.net/images/cover/292419409849503463a56cf9/500x500-000000-80-0-0.jpg",
                format = "MP3 HD",
                bitrateKbps = 320,
                license = "Pista Oficial",
                source = "TikTok Viral",
                isDownloadable = true
            )
        )
    }
}
