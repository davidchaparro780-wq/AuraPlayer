package com.auraplayer.data.repository

import com.auraplayer.data.model.OnlineTrack
import com.auraplayer.data.network.OkHttpDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.AudioStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

class YouTubeMusicRepository {

    companion object {
        private val isInitialized = AtomicBoolean(false)
        fun initNewPipe() {
            if (isInitialized.compareAndSet(false, true)) {
                try {
                    NewPipe.init(OkHttpDownloader.instance, Localization.DEFAULT, ContentCountry("US"))
                } catch (e: Exception) {
                    try {
                        NewPipe.init(OkHttpDownloader.instance)
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                }
            }
        }
    }

    init {
        initNewPipe()
    }

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
                                license = "YouTube",
                                source = "YouTube",
                                isDownloadable = true
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

    private var cobaltMirrors = listOf(
        "https://cobalt-api.kwiatekm.pl",
        "https://api.cobalt.tools",
        "https://cobalt.api.sciter.io"
    )

    private var invidiousMirrors = listOf(
        "https://invidious.nerdvpn.de",
        "https://inv.nadeko.net",
        "https://invidious.tiekoetter.com",
        "https://invidious.f5.si",
        "https://yt.chocolatemoo53.com"
    )

    var onYouTubeApiChangedDetected: (() -> Unit)? = null
    private var lastConfigFetchTime = 0L

    private fun refreshRemoteConfigIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastConfigFetchTime < 3600_000L) return
        lastConfigFetchTime = now

        try {
            val url = URL("https://raw.githubusercontent.com/davidchaparro780-wq/AuraPlayer/main/api-config.json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val cList = json.optJSONArray("cobaltInstances")
                if (cList != null && cList.length() > 0) {
                    val list = mutableListOf<String>()
                    for (i in 0 until cList.length()) list.add(cList.getString(i))
                    cobaltMirrors = list
                }
                val iList = json.optJSONArray("invidiousInstances")
                if (iList != null && iList.length() > 0) {
                    val list = mutableListOf<String>()
                    for (i in 0 until iList.length()) list.add(iList.getString(i))
                    invidiousMirrors = list
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun resolveAudioStream(videoId: String): String? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null
        refreshRemoteConfigIfNeeded()

        // 1. Try InnerTube IOS direct player endpoint (Fastest, AAC 256kbps, bypasses n-sig)
        try {
            val iosUrl = queryInnerTube(
                videoId = videoId,
                clientName = "IOS",
                clientVersion = "19.29.1",
                userAgent = "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X; en_US)",
                deviceModel = "iPhone16,2"
            )
            if (!iosUrl.isNullOrBlank()) {
                return@withContext iosUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Try NewPipeExtractor (Direct googlevideo audio stream with highest bitrate)
        try {
            initNewPipe()
            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val extractor = ServiceList.YouTube.getStreamExtractor(watchUrl)
            extractor.fetchPage()
            val audioStreams: List<AudioStream>? = extractor.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                val aacStream = audioStreams.filter { stream ->
                    val fmtName = stream.format?.name ?: ""
                    fmtName.contains("M4A", ignoreCase = true)
                }.maxByOrNull { it.averageBitrate }

                val chosenStream = aacStream ?: audioStreams.maxByOrNull { it.averageBitrate } ?: audioStreams.first()
                val directUrl = chosenStream.url
                if (!directUrl.isNullOrBlank()) {
                    return@withContext directUrl
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Try Cobalt API instances
        try {
            val cobaltUrl = queryCobalt(videoId)
            if (!cobaltUrl.isNullOrBlank()) {
                return@withContext cobaltUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Try InnerTube WEB_REMIX (YouTube Music Web Client)
        try {
            val ytmUrl = queryInnerTube(
                videoId = videoId,
                clientName = "WEB_REMIX",
                clientVersion = "1.20240901.01.00",
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
            )
            if (!ytmUrl.isNullOrBlank()) {
                return@withContext ytmUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 5. Try InnerTube ANDROID_VR direct player endpoint
        try {
            val vrUrl = queryInnerTube(
                videoId = videoId,
                clientName = "ANDROID_VR",
                clientVersion = "1.61.48"
            )
            if (!vrUrl.isNullOrBlank()) {
                return@withContext vrUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 6. Try Invidious dynamic mirror endpoints
        for (mirror in invidiousMirrors) {
            try {
                val invUrl = "$mirror/api/v1/videos/$videoId"
                val conn = (URL(invUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 3500
                    readTimeout = 3500
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(resp)
                    val formats = root.optJSONArray("adaptiveFormats")
                    if (formats != null) {
                        var bestUrl: String? = null
                        var highestBitrate = 0
                        for (i in 0 until formats.length()) {
                            val fmt = formats.getJSONObject(i)
                            val type = fmt.optString("type", "")
                            if (type.contains("audio", ignoreCase = true)) {
                                val u = fmt.optString("url", "")
                                val bitrate = fmt.optInt("bitrate", 0)
                                if (u.isNotBlank()) {
                                    if (type.contains("mp4", ignoreCase = true)) {
                                        return@withContext u
                                    }
                                    if (bitrate >= highestBitrate) {
                                        highestBitrate = bitrate
                                        bestUrl = u
                                    }
                                }
                            }
                        }
                        if (!bestUrl.isNullOrBlank()) {
                            return@withContext bestUrl
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // If all providers failed, YouTube likely updated its cipher/player JS — trigger self-updater check!
        try {
            onYouTubeApiChangedDetected?.invoke()
        } catch (_: Exception) {}

        null
    }

    private fun queryCobalt(videoId: String): String? {
        val targetUrl = "https://www.youtube.com/watch?v=$videoId"
        for (instance in cobaltMirrors) {
            try {
                val url = URL(instance)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "DaVE-Player/2.0")
                    doOutput = true
                }
                val payload = JSONObject().apply {
                    put("url", targetUrl)
                    put("downloadMode", "audio")
                    put("audioFormat", "mp3")
                }
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }

                if (conn.responseCode in 200..299) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(resp)
                    val streamUrl = root.optString("url", "")
                    if (streamUrl.isNotBlank()) {
                        return streamUrl
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun queryInnerTube(
        videoId: String,
        clientName: String,
        clientVersion: String,
        userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        deviceModel: String? = null
    ): String? {
        val url = URL("https://www.youtube.com/youtubei/v1/player")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 7000
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", userAgent)
            doOutput = true
        }

        val clientObj = JSONObject().apply {
            put("clientName", clientName)
            put("clientVersion", clientVersion)
            put("hl", "es")
            put("gl", "US")
            if (deviceModel != null) {
                put("deviceModel", deviceModel)
            }
        }

        val payload = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", clientObj)
            })
            put("videoId", videoId)
        }

        conn.outputStream.use { it.write(payload.toString().toByteArray()) }

        if (conn.responseCode == 200) {
            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(resp)
            val streamingData = root.optJSONObject("streamingData") ?: return null
            val formats = streamingData.optJSONArray("adaptiveFormats")
                ?: streamingData.optJSONArray("formats")
                ?: return null

            var bestAacUrl: String? = null
            var bestAacBitrate = 0
            var bestAnyUrl: String? = null
            var highestBitrate = 0

            for (i in 0 until formats.length()) {
                val fmt = formats.getJSONObject(i)
                val mime = fmt.optString("mimeType", "")
                val streamUrl = fmt.optString("url", "")
                val bitrate = fmt.optInt("bitrate", 0)

                if (mime.contains("audio", ignoreCase = true) && streamUrl.isNotBlank()) {
                    if (mime.contains("mp4", ignoreCase = true) || mime.contains("m4a", ignoreCase = true)) {
                        if (bitrate >= bestAacBitrate) {
                            bestAacBitrate = bitrate
                            bestAacUrl = streamUrl
                        }
                    }
                    if (bitrate >= highestBitrate) {
                        highestBitrate = bitrate
                        bestAnyUrl = streamUrl
                    }
                }
            }

            return bestAacUrl ?: bestAnyUrl
        }
        return null
    }
}
