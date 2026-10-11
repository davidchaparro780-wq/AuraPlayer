package com.auraplayer.data.repository

import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SearchCoverResult(
    val title: String,
    val artist: String,
    val album: String,
    val hdCoverUrl: String
)

object CoverArtSearchService {

    private const val TAG = "CoverArtSearchService"

    suspend fun searchCover(query: String): List<SearchCoverResult> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val apiUrl = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=6"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 12000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonText)
                val resultsArray = root.optJSONArray("results") ?: return@withContext emptyList()
                val list = mutableListOf<SearchCoverResult>()

                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.getJSONObject(i)
                    val trackName = item.optString("trackName", "")
                    val artistName = item.optString("artistName", "")
                    val albumName = item.optString("collectionName", "")
                    val rawArtwork = item.optString("artworkUrl100", "")

                    if (trackName.isNotBlank() && rawArtwork.isNotBlank()) {
                        // Reemplazar 100x100bb por 1000x1000bb para máxima resolución
                        val hdArtwork = rawArtwork.replace("100x100bb", "1000x1000bb")
                            .replace("60x60bb", "1000x1000bb")

                        list.add(
                            SearchCoverResult(
                                title = trackName,
                                artist = artistName,
                                album = albumName,
                                hdCoverUrl = hdArtwork
                            )
                        )
                    }
                }
                return@withContext list
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "Búsqueda de carátulas falló: ${e.message}", e)
        }
        emptyList()
    }
}
