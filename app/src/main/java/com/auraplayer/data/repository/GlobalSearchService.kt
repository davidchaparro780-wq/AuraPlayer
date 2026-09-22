package com.auraplayer.data.repository

import android.util.Log
import com.auraplayer.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GlobalSearchService(
    private val spotifyService: SpotifyMetadataService = SpotifyMetadataService(),
    private val youtubeRepo: YouTubeMusicRepository = YouTubeMusicRepository()
) {

    private val jamendoClientId = "3dce8b55"
    private val tag = "GlobalSearch"
    private val trendingCache = java.util.concurrent.ConcurrentHashMap<String, List<OnlineTrack>>()

    suspend fun searchOrExtract(input: String, selectedSource: String = "Todas"): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        if (isUrl(trimmed)) return@withContext extractFromUrl(trimmed)
        searchFederated(trimmed, selectedSource)
    }

    suspend fun getTrending(genre: String = "Trending", selectedSource: String = "Todas"): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val cacheKey = "${genre.lowercase().trim()}_$selectedSource"
        trendingCache[cacheKey]?.let { cached ->
            if (cached.isNotEmpty()) return@withContext cached
        }

        val list = mutableListOf<OnlineTrack>()

        val isTikTok = genre.equals("TikTok", ignoreCase = true) || genre.contains("tiktok", ignoreCase = true)

        // Concurrently query live charts with a quick timeout (3.5s max)
        try {
            coroutineScope {
                val deezerDeferred = async {
                    withTimeoutOrNull(3500) {
                        if (isTikTok) {
                            val viral = queryDeezer("tiktok viral 2025")
                            if (viral.isNotEmpty()) viral else queryDeezer("tiktok hits")
                        } else if (genre.isBlank() || genre.equals("Trending", ignoreCase = true) || genre.equals("Todas", ignoreCase = true)) {
                            queryDeezerCharts()
                        } else {
                            queryDeezer(genre)
                        }
                    } ?: emptyList()
                }

                val jamendoDeferred = async {
                    if (!isTikTok && (selectedSource == "Todas" || selectedSource == "Jamendo")) {
                        withTimeoutOrNull(3000) {
                            queryJamendo(if (genre == "Trending" || genre == "Todas") "" else genre, isTrending = true)
                        } ?: emptyList()
                    } else {
                        emptyList()
                    }
                }

                val deezerTracks = deezerDeferred.await()
                val jamendoTracks = jamendoDeferred.await()

                list.addAll(deezerTracks)
                list.addAll(jamendoTracks)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (list.isEmpty()) {
            list.addAll(getRealFallbackCatalog())
        }

        val result = list.distinctBy { "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}" }
        if (result.isNotEmpty()) {
            trendingCache[cacheKey] = result
        }
        result
    }

    fun fetchFreshDeezerPreview(artist: String, title: String): String? {
        try {
            val q = "$artist $title".trim()
            val encoded = URLEncoder.encode(q, "UTF-8")
            val urlStr = "https://api.deezer.com/search?q=$encoded&limit=1"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (conn.responseCode == 200) {
                val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val data = root.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val preview = data.getJSONObject(0).optString("preview", "")
                    if (preview.isNotBlank()) return preview
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun resolveValidAudioUrl(track: OnlineTrack): String = withContext(Dispatchers.IO) {
        // If already a direct audio stream URL, return immediately
        if (track.audioUrl.contains("googlevideo.com", ignoreCase = true)) {
            Log.d(tag, "resolveValidAudioUrl: Already a direct googlevideo stream")
            return@withContext track.audioUrl
        }

        if (track.source == "YouTube" || track.id.startsWith("yt_") || track.license == "YouTube" || track.audioUrl.contains("youtube.com/watch")) {
            val videoId = when {
                track.id.startsWith("yt_") -> track.id.removePrefix("yt_")
                track.audioUrl.contains("v=") -> track.audioUrl.substringAfter("v=").substringBefore("&")
                else -> ""
            }
            if (videoId.isNotBlank()) {
                Log.d(tag, "resolveValidAudioUrl: Resolving YouTube stream for videoId=$videoId")
                val streamUrl = youtubeRepo.resolveAudioStream(videoId)
                if (!streamUrl.isNullOrBlank()) {
                    Log.d(tag, "resolveValidAudioUrl: Got direct stream URL (${streamUrl.take(60)}...)")
                    return@withContext streamUrl
                }
                Log.w(tag, "resolveValidAudioUrl: YouTube resolution failed for videoId=$videoId")
            }
        }
        if (track.audioUrl.contains("hdnea=") || track.audioUrl.contains("jamendo") || track.audioUrl.contains("tikwm") || track.audioUrl.contains("radio-browser") || track.audioUrl.contains("googlevideo.com")) {
            return@withContext track.audioUrl
        }
        val fresh = fetchFreshDeezerPreview(track.artist, track.title)
        if (!fresh.isNullOrBlank()) {
            Log.d(tag, "resolveValidAudioUrl: Using Deezer preview fallback")
            return@withContext fresh
        }
        track.audioUrl
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
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13)")
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
                        val realDuration = if (musicDuration > 0) musicDuration else if (videoDuration > 0) videoDuration else 60

                        val musicUrl = data?.optString("music", "") ?: ""
                        val playUrl = data?.optString("play", "") ?: ""

                        val audioUrl = if (musicUrl.isNotBlank()) {
                            musicUrl
                        } else if (playUrl.isNotBlank()) {
                            playUrl
                        } else {
                            ""
                        }

                        if (audioUrl.isNotBlank()) {
                            list.add(
                                OnlineTrack(
                                    id = "tt_${data?.optString("id", System.currentTimeMillis().toString())}",
                                    title = title,
                                    artist = author,
                                    album = "TikTok Audio Completo",
                                    durationSec = realDuration,
                                    audioUrl = audioUrl,
                                    coverUrl = cover,
                                    format = "MP3 Completo",
                                    bitrateKbps = 320,
                                    license = "Pista Completa TikTok",
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

        // Execute searches concurrently with quick timeouts
        val youtubeDeferred = async {
            if (selectedSource == "Todas" || selectedSource == "YouTube") {
                withTimeoutOrNull(4000) {
                    youtubeRepo.searchYouTube(query)
                } ?: emptyList()
            } else {
                emptyList()
            }
        }

        val deezerDeferred = async {
            if (selectedSource == "Todas" || selectedSource == "Deezer") {
                withTimeoutOrNull(3500) {
                    queryDeezer(query)
                } ?: emptyList()
            } else {
                emptyList()
            }
        }

        val jamendoDeferred = async {
            if (selectedSource == "Todas" || selectedSource == "Jamendo") {
                withTimeoutOrNull(3000) {
                    queryJamendo(query, isTrending)
                } ?: emptyList()
            } else {
                emptyList()
            }
        }

        val ytResults = youtubeDeferred.await()
        val deezerResults = deezerDeferred.await()
        val jamendoResults = jamendoDeferred.await()

        list.addAll(ytResults)
        list.addAll(deezerResults)
        list.addAll(jamendoResults)

        if (list.isEmpty()) {
            val q = query.lowercase().trim()
            val matchedFallback = getRealFallbackCatalog().filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
            }
            list.addAll(matchedFallback)
        }

        list.distinctBy { "${it.title.lowercase().trim()}_${it.artist.lowercase().trim()}" }
    }

    private fun queryDeezer(query: String): List<OnlineTrack> {
        val list = mutableListOf<OnlineTrack>()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val urlStr = "https://api.deezer.com/search?q=$encoded&limit=25"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
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

                // High-resolution CDN cover art (500x500)
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
                            source = "TikTok / Deezer",
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
                connectTimeout = 3500
                readTimeout = 3500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
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
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conn.responseCode != 200) return list
            val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val results = root.optJSONArray("results") ?: return list

            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val duration = item.optInt("duration", 0)
                if (duration < 30) continue

                val trackId = item.optString("id", "")
                val name = item.optString("name", "Canción").trim()
                val artist = item.optString("artist_name", "Artista").trim()
                val album = item.optString("album_name", "Álbum").trim()

                val audioUrl = item.optString("audio", "")
                    .ifBlank { item.optString("audiodownload", "") }

                val cover = item.optString("image", "")

                if (name.isNotBlank() && audioUrl.isNotBlank()) {
                    list.add(
                        OnlineTrack(
                            id = "jm_$trackId",
                            title = name,
                            artist = artist,
                            album = album,
                            durationSec = duration,
                            audioUrl = audioUrl,
                            coverUrl = cover,
                            format = "MP3 Completo",
                            bitrateKbps = 320,
                            license = "Creative Commons",
                            source = "Jamendo",
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

    fun getRealFallbackCatalog(): List<OnlineTrack> = listOf(
        OnlineTrack(
            id = "dz_2629469792",
            title = "Gata Only",
            artist = "FloyyMenor, Cris Mj",
            album = "Gata Only - Single",
            durationSec = 222,
            audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/8/6/7/0/86748734ecd3254461e062cededb86b5.mp3",
            coverUrl = "https://cdn-images.dzcdn.net/images/cover/2b637eeb2dadc47f968313f017d2546c/500x500-000000-80-0-0.jpg",
            format = "MP3 Completo",
            bitrateKbps = 320,
            license = "Top TikTok Viral",
            source = "TikTok Viral",
            isDownloadable = true
        ),
        OnlineTrack(
            id = "dz_2550186982",
            title = "LUNA",
            artist = "Feid, ATL Jacob",
            album = "FERXXOCALIPSIS",
            durationSec = 196,
            audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/2/5/9/0/2591604a4341b55979f4c3c3a0785f26.mp3",
            coverUrl = "https://cdn-images.dzcdn.net/images/cover/c96d93375c3db6bbd1d58079555c4d08/500x500-000000-80-0-0.jpg",
            format = "MP3 Completo",
            bitrateKbps = 320,
            license = "Top TikTok Viral",
            source = "TikTok Viral",
            isDownloadable = true
        ),
        OnlineTrack(
            id = "dz_2839218492",
            title = "REAL GANGSTA LOVE",
            artist = "Trueno",
            album = "EL ÚLTIMO BAILE",
            durationSec = 145,
            audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/a/1/f/0/a1f592ff37c86a64b9d0315488582cae.mp3",
            coverUrl = "https://cdn-images.dzcdn.net/images/cover/2e5d7790b4bf5ba42968ffc6010b9f87/500x500-000000-80-0-0.jpg",
            format = "MP3 Completo",
            bitrateKbps = 320,
            license = "Top TikTok Viral",
            source = "TikTok Viral",
            isDownloadable = true
        ),
        OnlineTrack(
            id = "dz_2881293812",
            title = "Si Antes Te Hubiera Conocido",
            artist = "KAROL G",
            album = "Si Antes Te Hubiera Conocido",
            durationSec = 195,
            audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/f/b/4/0/fb4ce05c48b2929e71ec9d9a7ec8f679.mp3",
            coverUrl = "https://cdn-images.dzcdn.net/images/cover/9079f2df12678c1b3d68fb46487e4ea6/500x500-000000-80-0-0.jpg",
            format = "MP3 Completo",
            bitrateKbps = 320,
            license = "Top TikTok Viral",
            source = "TikTok Viral",
            isDownloadable = true
        ),
        OnlineTrack(
            id = "dz_2719283712",
            title = "Espresso",
            artist = "Sabrina Carpenter",
            album = "Short n' Sweet",
            durationSec = 175,
            audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/1/2/4/0/1247ff5f0c9769da89ce0c91ba4e320f.mp3",
            coverUrl = "https://cdn-images.dzcdn.net/images/cover/7452e8502db6aa5e62fba23dd7759881/500x500-000000-80-0-0.jpg",
            format = "MP3 Completo",
            bitrateKbps = 320,
            license = "Top TikTok Viral",
            source = "TikTok Viral",
            isDownloadable = true
        ),
        OnlineTrack(
            id = "dz_2638192842",
            title = "Beautiful Things",
            artist = "Benson Boone",
            album = "Fireworks & Rollerblades",
            durationSec = 180,
            audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/3/4/2/0/3421d0171a48c4d3da9a63c65e8a7194.mp3",
            coverUrl = "https://cdn-images.dzcdn.net/images/cover/96f8c7b8ff5e135cb17b8f97fc5bf261/500x500-000000-80-0-0.jpg",
            format = "MP3 Completo",
            bitrateKbps = 320,
            license = "Top TikTok Viral",
            source = "TikTok Viral",
            isDownloadable = true
        ),
        OnlineTrack(
            id = "dz_2819203912",
            title = "BIRDS OF A FEATHER",
            artist = "Billie Eilish",
            album = "HIT ME HARD AND SOFT",
            durationSec = 194,
            audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/e/1/6/0/e16f7344931ea7085a6a2468ea90c422.mp3",
            coverUrl = "https://cdn-images.dzcdn.net/images/cover/fc4da0a86e921d7b38d386bb0bbf0281/500x500-000000-80-0-0.jpg",
            format = "MP3 Completo",
            bitrateKbps = 320,
            license = "Top TikTok Viral",
            source = "TikTok Viral",
            isDownloadable = true
        ),
        OnlineTrack(
            id = "dz_2718291029",
            title = "MILLION DOLLAR BABY",
            artist = "Tommy Richman",
            album = "MILLION DOLLAR BABY",
            durationSec = 155,
            audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/c/2/7/0/c276332ec28eafe9fca9510103759c9d.mp3",
            coverUrl = "https://cdn-images.dzcdn.net/images/cover/5f58c73656919db8ce4d5fbfa0fafe96/500x500-000000-80-0-0.jpg",
            format = "MP3 Completo",
            bitrateKbps = 320,
            license = "Top TikTok Viral",
            source = "TikTok Viral",
            isDownloadable = true
        ),
        OnlineTrack(
            id = "dz_2518291039",
            title = "MONACO",
            artist = "Bad Bunny",
            album = "nadie sabe lo que va a pasar mañana",
            durationSec = 267,
            audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/b/9/4/0/b9449f87424ad5a23072ae0b3956cb6b.mp3",
            coverUrl = "https://cdn-images.dzcdn.net/images/cover/59eecf3f4c633a6943b1c674251cb7e4/500x500-000000-80-0-0.jpg",
            format = "MP3 Completo",
            bitrateKbps = 320,
            license = "Top TikTok Viral",
            source = "TikTok Viral",
            isDownloadable = true
        ),
        OnlineTrack(
            id = "dz_2729102938",
            title = "I Like the Way You Kiss Me",
            artist = "Artemas",
            album = "I Like the Way You Kiss Me",
            durationSec = 142,
            audioUrl = "https://cdnt-preview.dzcdn.net/api/1/1/2/4/1/0/2418579df6e16694602bb6c8a2b5e28c.mp3",
            coverUrl = "https://cdn-images.dzcdn.net/images/cover/ea0a520bfd8a4c0cbdddf2b270cf27ce/500x500-000000-80-0-0.jpg",
            format = "MP3 Completo",
            bitrateKbps = 320,
            license = "Top TikTok Viral",
            source = "TikTok Viral",
            isDownloadable = true
        )
    )
}
