package com.auraplayer.data.repository

import com.auraplayer.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class OnlineMusicRepository {

    suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val urlString = "https://api.deezer.com/search?q=$encodedQuery&limit=30"
        fetchFromDeezer(urlString)
    }

    suspend fun getTrendingTracks(genre: String? = null): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val urlString = if (genre.isNullOrBlank() || genre == "Trending" || genre == "Todas") {
            "https://api.deezer.com/chart/0/tracks?limit=30"
        } else {
            val genreQuery = URLEncoder.encode(genre.lowercase(), "UTF-8")
            "https://api.deezer.com/search?q=$genreQuery&limit=30"
        }
        fetchFromDeezer(urlString)
    }

    private fun fetchFromDeezer(urlString: String): List<OnlineTrack> {
        val trackList = mutableListOf<OnlineTrack>()
        try {
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AuraPlayer/1.6.0 (Android)")
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(response)
                val dataArray = root.optJSONArray("data")
                if (dataArray != null) {
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val id = item.optString("id", System.currentTimeMillis().toString())
                        val title = item.optString("title", "Sin título")
                        val artistObj = item.optJSONObject("artist")
                        val artistName = artistObj?.optString("name") ?: "Artista Desconocido"
                        val albumObj = item.optJSONObject("album")
                        val albumName = albumObj?.optString("title") ?: "Single"
                        val duration = item.optInt("duration", 0)
                        val audioUrl = item.optString("preview", "")

                        var coverUrl = albumObj?.optString("cover_big")
                            ?: albumObj?.optString("cover_medium")
                            ?: artistObj?.optString("picture_big")
                            ?: ""

                        if (audioUrl.isNotBlank()) {
                            trackList.add(
                                OnlineTrack(
                                    id = id,
                                    title = title,
                                    artist = artistName,
                                    album = albumName,
                                    durationSec = duration,
                                    audioUrl = audioUrl,
                                    coverUrl = coverUrl,
                                    format = "MP3",
                                    bitrateKbps = 320,
                                    license = "Digital Master HD"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return trackList
    }
}
