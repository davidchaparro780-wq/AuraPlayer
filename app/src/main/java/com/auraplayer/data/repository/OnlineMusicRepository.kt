package com.auraplayer.data.repository

import com.auraplayer.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class OnlineMusicRepository {

    private val jamendoClientId = "56d30c95"

    suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val urlString = "https://api.jamendo.com/v3.0/tracks/?client_id=$jamendoClientId&format=jsonpretty&limit=30&namesearch=$encodedQuery&include=musicinfo&audioformat=mp32"
        fetchFromJamendo(urlString)
    }

    suspend fun getTrendingTracks(genre: String? = null): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val tagParam = if (!genre.isNullOrBlank() && genre != "Todas" && genre != "Trending") {
            "&tags=" + URLEncoder.encode(genre.lowercase(), "UTF-8")
        } else {
            "&boost=popularity_total"
        }
        val urlString = "https://api.jamendo.com/v3.0/tracks/?client_id=$jamendoClientId&format=jsonpretty&limit=30$tagParam&include=musicinfo&audioformat=mp32"
        fetchFromJamendo(urlString)
    }

    private fun fetchFromJamendo(urlString: String): List<OnlineTrack> {
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
                val results = root.optJSONArray("results")
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val id = item.optString("id", System.currentTimeMillis().toString())
                        val name = item.optString("name", "Sin título")
                        val artistName = item.optString("artist_name", "Artista Desconocido")
                        val albumName = item.optString("album_name", "Single")
                        val duration = item.optInt("duration", 0)
                        val audioUrl = item.optString("audio", "")
                        val coverUrl = item.optString("image", item.optString("album_image", ""))
                        val license = item.optString("license_ccurl", "CC-BY")

                        if (audioUrl.isNotBlank()) {
                            trackList.add(
                                OnlineTrack(
                                    id = id,
                                    title = name,
                                    artist = artistName,
                                    album = albumName,
                                    durationSec = duration,
                                    audioUrl = audioUrl,
                                    coverUrl = coverUrl,
                                    format = "MP3",
                                    bitrateKbps = 320,
                                    license = if (license.contains("creativecommons")) "Creative Commons" else "Open Audio"
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
