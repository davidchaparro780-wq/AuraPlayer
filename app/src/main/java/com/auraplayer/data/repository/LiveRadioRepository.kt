package com.auraplayer.data.repository

import com.auraplayer.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LiveRadioRepository {

    suspend fun searchStations(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        fetchFromRadioApi("https://de1.api.radio-browser.info/json/stations/search?name=$encoded&limit=25&hidebroken=true&order=clickcount&reverse=true")
    }

    suspend fun getTopStations(tag: String = "top"): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val url = if (tag.isBlank() || tag == "top" || tag == "Trending") {
            "https://de1.api.radio-browser.info/json/stations/topclick/25"
        } else {
            val encodedTag = URLEncoder.encode(tag.lowercase(), "UTF-8")
            "https://de1.api.radio-browser.info/json/stations/bytag/$encodedTag?limit=25&hidebroken=true&order=clickcount&reverse=true"
        }
        fetchFromRadioApi(url)
    }

    private fun fetchFromRadioApi(urlString: String): List<OnlineTrack> {
        val list = mutableListOf<OnlineTrack>()
        try {
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AuraPlayer/1.7.0 (Android)")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("stationuuid", System.currentTimeMillis().toString())
                    val name = obj.optString("name", "Estación de Radio").trim()
                    val country = obj.optString("country", "Mundial").trim()
                    val tags = obj.optString("tags", "Radio").trim()
                    val streamUrl = obj.optString("url_resolved", "").ifBlank { obj.optString("url", "") }
                    val favicon = obj.optString("favicon", "")
                    val bitrate = obj.optInt("bitrate", 128)

                    if (streamUrl.isNotBlank() && (streamUrl.startsWith("http://") || streamUrl.startsWith("https://"))) {
                        list.add(
                            OnlineTrack(
                                id = "radio_$id",
                                title = name,
                                artist = if (country.isNotBlank()) "En Vivo • $country" else "En Vivo",
                                album = if (tags.isNotBlank()) tags.take(30) else "Radio Online",
                                durationSec = 0,
                                audioUrl = streamUrl,
                                coverUrl = favicon,
                                format = "Live Stream",
                                bitrateKbps = if (bitrate > 0) bitrate else 128,
                                license = "Radio 24/7"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
