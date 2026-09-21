package com.auraplayer.data.repository

import com.auraplayer.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class YouTubeMusicRepository {

    suspend fun searchYouTube(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val list = mutableListOf<OnlineTrack>()

        try {
            val url = URL("https://www.youtube.com/youtubei/v1/search")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20231201.00.00")
                        put("hl", "es")
                        put("gl", "US")
                    })
                })
                put("query", query.trim())
            }

            connection.outputStream.use { it.write(payload.toString().toByteArray()) }

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val sections = root.optJSONObject("contents")
                    ?.optJSONObject("twoColumnSearchResultsRenderer")
                    ?.optJSONObject("primaryContents")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents")

                val items = sections?.optJSONObject(0)
                    ?.optJSONObject("itemSectionRenderer")
                    ?.optJSONArray("contents")

                if (items != null) {
                    for (i in 0 until items.length()) {
                        val video = items.getJSONObject(i).optJSONObject("videoRenderer") ?: continue
                        val videoId = video.optString("videoId")
                        if (videoId.isBlank()) continue

                        val title = video.optJSONObject("title")
                            ?.optJSONArray("runs")
                            ?.optJSONObject(0)
                            ?.optString("text") ?: "Canción"

                        val author = video.optJSONObject("ownerText")
                            ?.optJSONArray("runs")
                            ?.optJSONObject(0)
                            ?.optString("text") ?: "Artista"

                        val durationStr = video.optJSONObject("lengthText")
                            ?.optString("simpleText", "0:00") ?: "0:00"

                        val thumbArray = video.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        val rawCover = if (thumbArray != null && thumbArray.length() > 0) {
                            thumbArray.optJSONObject(thumbArray.length() - 1)?.optString("url") ?: ""
                        } else {
                            ""
                        }
                        val coverUrl = when {
                            rawCover.startsWith("//") -> "https:$rawCover"
                            rawCover.startsWith("http") -> rawCover
                            else -> "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        }

                        // Parse duration mm:ss or hh:mm:ss to seconds
                        val parts = durationStr.split(":")
                        val durationSec = when (parts.size) {
                            2 -> (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
                            3 -> (parts[0].toIntOrNull() ?: 0) * 3600 + (parts[1].toIntOrNull() ?: 0) * 60 + (parts[2].toIntOrNull() ?: 0)
                            else -> 180
                        }

                        // Initial stream endpoint placeholder
                        val audioUrl = "https://www.youtube.com/watch?v=$videoId"

                        list.add(
                            OnlineTrack(
                                id = "yt_$videoId",
                                title = title,
                                artist = author,
                                album = "YouTube Music",
                                durationSec = durationSec,
                                audioUrl = audioUrl,
                                coverUrl = coverUrl,
                                format = "HQ Audio",
                                bitrateKbps = 320,
                                license = "YouTube"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun resolveAudioStream(videoId: String): String? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null

        try {
            val url = URL("https://www.youtube.com/youtubei/v1/player")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID_VR")
                        put("clientVersion", "1.61.48")
                        put("hl", "es")
                        put("gl", "US")
                    })
                })
                put("videoId", videoId)
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray()) }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(resp)
                val formats = root.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats")
                if (formats != null) {
                    var bestUrl: String? = null
                    var highestBitrate = 0
                    for (i in 0 until formats.length()) {
                        val fmt = formats.getJSONObject(i)
                        val mime = fmt.optString("mimeType")
                        if (mime.startsWith("audio/")) {
                            val streamUrl = fmt.optString("url")
                            val bitrate = fmt.optInt("bitrate", 0)
                            if (streamUrl.isNotBlank() && bitrate >= highestBitrate) {
                                highestBitrate = bitrate
                                bestUrl = streamUrl
                            }
                        }
                    }
                    if (!bestUrl.isNullOrBlank()) {
                        return@withContext bestUrl
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }
}
