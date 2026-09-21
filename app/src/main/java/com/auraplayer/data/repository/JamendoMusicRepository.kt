package com.auraplayer.data.repository

import com.auraplayer.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class JamendoMusicRepository {

    private val clientId = "3dce8b55"
    private val baseUrl = "https://api.jamendo.com/v3.0/tracks/"

    suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "$baseUrl?client_id=$clientId&format=json&limit=30&namesearch=$encoded&order=popularity_total&audioformat=mp32&include=musicinfo"
        fetchFromJamendo(url)
    }

    suspend fun getTrendingTracks(tag: String = "Trending"): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val url = if (tag.isBlank() || tag == "Trending" || tag == "Todas") {
            "$baseUrl?client_id=$clientId&format=json&limit=30&order=popularity_week&audioformat=mp32&include=musicinfo"
        } else {
            val encodedTag = URLEncoder.encode(tag.lowercase().replace(" ", ""), "UTF-8")
            "$baseUrl?client_id=$clientId&format=json&limit=30&tags=$encodedTag&order=popularity_total&audioformat=mp32&include=musicinfo"
        }
        fetchFromJamendo(url)
    }

    private fun fetchFromJamendo(urlString: String): List<OnlineTrack> {
        val list = mutableListOf<OnlineTrack>()
        try {
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 9000
                readTimeout = 9000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AuraPlayer/1.7.4 (Android)")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
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
                            
                            // High resolution cover (500x500)
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
                                    format = "MP3 Full",
                                    bitrateKbps = 320,
                                    license = "Libre / Completa"
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
