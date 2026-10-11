package com.auraplayer.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.auraplayer.data.model.MediaModel
import com.auraplayer.data.model.OnlineTrack
import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progress: Int) : DownloadStatus()
    object Tagging : DownloadStatus()
    object Completed : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

class DownloadEngine(
    private val context: Context,
    private val coverArtManager: CoverArtManager,
    private val lyricsManager: LyricsManager,
    val youtubeRepo: YouTubeMusicRepository = YouTubeMusicRepository()
) {

    companion object {
        private const val TAG = "DownloadEngine"
    }

    var onUpdateNeeded: (() -> Unit)? = null

    init {
        youtubeRepo.onYouTubeApiChangedDetected = {
            onUpdateNeeded?.invoke()
        }
    }

    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    fun getStatus(trackId: String): DownloadStatus {
        return _downloadStates.value[trackId] ?: DownloadStatus.Idle
    }

    /**
     * Downloads an entire playlist or album batch sequentially in background with live progress.
     */
    suspend fun downloadBatch(
        tracks: List<OnlineTrack>,
        onProgress: (completed: Int, total: Int, currentTitle: String) -> Unit = { _, _, _ -> },
        onAllCompleted: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        val downloadable = tracks.filter { it.isDownloadable }
        var done = 0
        val total = downloadable.size

        for (track in downloadable) {
            onProgress(done, total, track.title)
            try {
                downloadTrack(
                    track = track,
                    onComplete = {
                        done++
                        onProgress(done, total, track.title)
                    },
                    onError = {
                        done++
                        onProgress(done, total, track.title)
                    }
                )
            } catch (_: Exception) {
                done++
                onProgress(done, total, track.title)
            }
        }
        withContext(Dispatchers.Main) {
            onAllCompleted()
        }
    }

    suspend fun downloadTrack(
        track: OnlineTrack,
        onComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        updateState(track.id, DownloadStatus.Downloading(0))

        try {
            val safeArtist = sanitize(track.artist).ifBlank { "DaVE Artist" }
            val safeTitle = sanitize(track.title).ifBlank { "DaVE Song" }

            // 1. Resolve direct stream URL — skip if already resolved to a direct stream
            var resolvedAudioUrl = track.audioUrl

            // If the track is from Deezer or has a 30s preview CDN, upgrade to the full song via YouTube
            val isDeezerPreview = resolvedAudioUrl.contains("dzcdn.net", ignoreCase = true) ||
                resolvedAudioUrl.contains("cdns-preview-", ignoreCase = true) ||
                resolvedAudioUrl.contains("cdnt-preview", ignoreCase = true) ||
                track.source == "Deezer"

            if (isDeezerPreview) {
                try {
                    val ytMatches = youtubeRepo.searchYouTube("${track.artist} ${track.title}")
                    val bestYt = ytMatches.firstOrNull()
                    if (bestYt != null) {
                        val videoId = bestYt.id.removePrefix("yt_")
                        val fullStream = youtubeRepo.resolveAudioStream(videoId)
                        if (!fullStream.isNullOrBlank()) {
                            resolvedAudioUrl = fullStream
                            AppLog.d(TAG, "Preview de Deezer actualizado a stream HQ de YouTube para ${track.title}")
                        }
                    }
                } catch (e: Exception) {
                    AppLog.w(TAG, "No se pudo actualizar el preview de Deezer: ${e.message}", e)
                }
            }

            val isAlreadyDirectStream = resolvedAudioUrl.contains("googlevideo.com", ignoreCase = true) ||
                resolvedAudioUrl.contains("jamendo.com", ignoreCase = true) ||
                resolvedAudioUrl.contains("tikwm.com", ignoreCase = true) ||
                resolvedAudioUrl.contains("archive.org", ignoreCase = true)

            val needsYouTubeResolution = !isAlreadyDirectStream && (
                resolvedAudioUrl.contains("youtube.com/watch", ignoreCase = true) ||
                resolvedAudioUrl.contains("youtu.be/", ignoreCase = true) ||
                resolvedAudioUrl.contains("music.youtube.com", ignoreCase = true))

            if (needsYouTubeResolution) {
                AppLog.d(TAG, "URL de YouTube detectada, resolviendo stream para: ${track.title}")
                val videoId = when {
                    track.id.startsWith("yt_") -> track.id.removePrefix("yt_")
                    resolvedAudioUrl.contains("v=") -> resolvedAudioUrl.substringAfter("v=").substringBefore("&")
                    resolvedAudioUrl.contains("youtu.be/") -> resolvedAudioUrl.substringAfter("youtu.be/").substringBefore("?")
                    else -> ""
                }

                var directUrl: String? = null
                if (videoId.isNotBlank()) {
                    directUrl = youtubeRepo.resolveAudioStream(videoId)
                }
                if (directUrl.isNullOrBlank()) {
                    directUrl = searchFallbackAudioUrl(track.artist, track.title)
                }
                if (directUrl.isNullOrBlank()) {
                    throw IllegalStateException("No se pudo obtener el audio. Verifica tu conexión o intenta con otra canción.")
                }
                resolvedAudioUrl = directUrl
            } else if (isAlreadyDirectStream) {
                AppLog.d(TAG, "La URL ya es un stream directo, se omite re-resolución: ${resolvedAudioUrl.take(80)}...")
            }

            // 2. Download with retry (max 2 attempts for transient network failures)
            var lastException: Exception? = null
            for (attempt in 1..2) {
                try {
                    performDownload(resolvedAudioUrl, track, safeArtist, safeTitle)

                    updateState(track.id, DownloadStatus.Completed)
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke()
                    }
                    return@withContext // SUCCESS - exit
                } catch (e: Exception) {
                    lastException = e
                    AppLog.w(TAG, "Intento de descarga $attempt falló: ${e.message}", e)

                    // On first failure for YouTube tracks, try re-resolving the stream URL
                    if (attempt == 1 && (track.source == "YouTube" || track.id.startsWith("yt_"))) {
                        val videoId = if (track.id.startsWith("yt_")) track.id.removePrefix("yt_") else ""
                        if (videoId.isNotBlank()) {
                            val freshUrl = youtubeRepo.resolveAudioStream(videoId)
                            if (!freshUrl.isNullOrBlank() && freshUrl != resolvedAudioUrl) {
                                resolvedAudioUrl = freshUrl
                                AppLog.d(TAG, "Stream de YouTube re-resuelto para el reintento")
                                continue
                            }
                        }
                        // Also try Deezer fallback on retry
                        val deezerUrl = searchFallbackAudioUrl(track.artist, track.title)
                        if (!deezerUrl.isNullOrBlank()) {
                            resolvedAudioUrl = deezerUrl
                            AppLog.d(TAG, "Usando fallback de Deezer para el reintento")
                            continue
                        }
                    }
                }
            }

            // All attempts failed
            throw lastException ?: IllegalStateException("Error de descarga desconocido")

        } catch (e: Exception) {
            AppLog.e(TAG, "No se pudo descargar la canción ${track.title}", e)
            val userMsg = when {
                e.message?.contains("403") == true -> "YouTube bloqueó el acceso. Intenta de nuevo."
                e.message?.contains("410") == true -> "El enlace de audio expiró. Intenta de nuevo."
                e.message?.contains("timeout", ignoreCase = true) == true -> "Tiempo agotado. Verifica tu conexión."
                e.message?.contains("audio", ignoreCase = true) == true -> e.localizedMessage ?: "Error de audio"
                else -> e.localizedMessage ?: "Error de descarga"
            }
            updateState(track.id, DownloadStatus.Error(userMsg))
            withContext(Dispatchers.Main) {
                onError?.invoke(userMsg)
            }
        }
    }

    private suspend fun performDownload(
        audioUrl: String,
        track: OnlineTrack,
        safeArtist: String,
        safeTitle: String
    ) {
        // Establish HTTP connection with multi-redirect support (CDNs, Storage)
        var currentUrl = audioUrl
        var connection: HttpURLConnection
        var redirectCount = 0
        while (true) {
            val url = URL(currentUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 25000
                readTimeout = 30000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                setRequestProperty("Connection", "keep-alive")
                // Range header helps resume and tells server we want the full body
                setRequestProperty("Range", "bytes=0-")
            }
            val code = connection.responseCode
            if ((code in 301..303 || code == 307 || code == 308) && redirectCount < 8) {
                val newLocation = connection.getHeaderField("Location")
                if (!newLocation.isNullOrBlank()) {
                    currentUrl = newLocation
                    redirectCount++
                    connection.disconnect()
                    continue
                }
            }
            break
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            connection.disconnect()
            throw IllegalStateException("Servidor respondió con código $responseCode")
        }

        val rawContentType = connection.contentType?.lowercase() ?: ""
        if (rawContentType.contains("text/html")) {
            connection.disconnect()
            throw IllegalStateException("La URL no contiene un archivo de audio válido")
        }

        val isMp4 = rawContentType.contains("mp4") ||
                    rawContentType.contains("m4a") ||
                    rawContentType.contains("aac") ||
                    currentUrl.contains("mime=audio%2fmp4", ignoreCase = true) ||
                    currentUrl.contains("mime=audio/mp4", ignoreCase = true) ||
                    currentUrl.contains(".m4a", ignoreCase = true) ||
                    currentUrl.contains(".mp4", ignoreCase = true)

        val isWebm = rawContentType.contains("webm") ||
                     rawContentType.contains("opus") ||
                     rawContentType.contains("ogg") ||
                     currentUrl.contains("mime=audio%2fwebm", ignoreCase = true) ||
                     currentUrl.contains("mime=audio/webm", ignoreCase = true) ||
                     currentUrl.contains(".opus", ignoreCase = true) ||
                     currentUrl.contains(".webm", ignoreCase = true)

        val extension = when {
            isMp4 -> ".m4a"
            isWebm -> ".opus"
            else -> ".mp3"
        }

        val mimeType = when {
            isMp4 -> "audio/mp4"
            isWebm -> "audio/ogg"
            else -> "audio/mpeg"
        }

        val safeFileName = "$safeArtist - $safeTitle$extension"
        // Use content-length header; fallback to -1 if unknown (chunked transfer)
        val contentLength = connection.getHeaderField("Content-Length")?.toLongOrNull()?.toInt() ?: connection.contentLength
        val inputStream = connection.inputStream

        var savedFileUri: Uri? = null
        var savedFilePath: String? = null

        // Write file using MediaStore on Android 10+ (Scoped Storage safe)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // First, delete any existing file with the same name to prevent conflicts
                val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(safeFileName, "Music/DaVEPlayer/")
                try {
                    context.contentResolver.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs)
                } catch (e: Exception) { AppLog.d(TAG, "No se pudo borrar el archivo existente en MediaStore antes de insertar", e) }

                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, safeFileName)
                    put(MediaStore.Audio.Media.TITLE, track.title)
                    put(MediaStore.Audio.Media.ARTIST, track.artist)
                    put(MediaStore.Audio.Media.ALBUM, track.album)
                    put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/DaVEPlayer")
                    put(MediaStore.Audio.Media.IS_MUSIC, 1)
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        streamWithProgress(inputStream, outputStream, contentLength, track.id)
                        outputStream.close()
                        savedFileUri = uri
                    }
                    val updateValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, updateValues, null, null)
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Escritura en MediaStore falló, se usa almacenamiento público directo: ${e.message}", e)
                savedFileUri = null
            }
        }

        // Fallback for Android 9 or below, or if MediaStore insert/update threw an exception
        if (savedFileUri == null) {
            val publicMusicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DaVEPlayer")
            val targetDir = if (publicMusicDir.exists() || publicMusicDir.mkdirs()) {
                publicMusicDir
            } else {
                File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "DaVEPlayer").apply { mkdirs() }
            }
            val targetFile = File(targetDir, safeFileName)
            savedFilePath = targetFile.absolutePath
            savedFileUri = Uri.fromFile(targetFile)

            val outputStream = FileOutputStream(targetFile)
            streamWithProgress(inputStream, outputStream, contentLength, track.id)
            outputStream.close()

            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf(mimeType),
                null
            )
        }

        try {
            inputStream.close()
        } catch (e: Exception) { AppLog.d(TAG, "No se pudo cerrar el flujo de entrada de la descarga", e) }
        try {
            connection.disconnect()
        } catch (e: Exception) { AppLog.d(TAG, "No se pudo cerrar la conexión HTTP de la descarga", e) }

        updateState(track.id, DownloadStatus.Tagging)

        // Save Cover Art locally for instant offline display
        if (track.coverUrl.isNotBlank()) {
            val mediaStoreId = savedFileUri?.lastPathSegment?.toLongOrNull()
            downloadCoverArt(track.artist, track.title, track.coverUrl, mediaStoreId, safeFileName)
        }

        // Pre-cache synced lyrics if available
        val dummyMedia = MediaModel(
            id = System.currentTimeMillis(),
            title = track.title,
            artist = track.artist,
            album = track.album,
            duration = track.durationSec * 1000L,
            uri = savedFileUri ?: Uri.EMPTY,
            artworkUri = if (track.coverUrl.isNotBlank()) Uri.parse(track.coverUrl) else null,
            path = savedFilePath ?: "",
            size = 0L
        )
        try {
            lyricsManager.getLyrics(dummyMedia)
        } catch (e: Exception) { AppLog.d(TAG, "No se pudo pre-cachear la letra de la canción", e) }
    }

    private fun searchFallbackAudioUrl(artist: String, title: String): String? {
        try {
            val q = "$artist $title".trim()
            val encoded = URLEncoder.encode(q, "UTF-8")
            val urlStr = "https://api.deezer.com/search?q=$encoded&limit=1"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
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
            AppLog.w(TAG, "No se pudo buscar una URL de audio alternativa en Deezer", e)
        }
        return null
    }

    private fun streamWithProgress(
        inputStream: java.io.InputStream,
        outputStream: OutputStream,
        contentLength: Int,
        trackId: String
    ) {
        val bufferedIn = if (inputStream is java.io.BufferedInputStream) inputStream else java.io.BufferedInputStream(inputStream, 65536)
        val bufferedOut = if (outputStream is java.io.BufferedOutputStream) outputStream else java.io.BufferedOutputStream(outputStream, 65536)
        val buffer = ByteArray(65536) // 64KB high-speed download buffer
        var bytesRead: Int
        var totalBytesRead = 0L
        var lastReportedProgress = -1

        while (bufferedIn.read(buffer).also { bytesRead = it } != -1) {
            bufferedOut.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            if (contentLength > 0) {
                val progress = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 99)
                if (progress != lastReportedProgress) {
                    lastReportedProgress = progress
                    updateState(trackId, DownloadStatus.Downloading(progress))
                }
            }
        }
        bufferedOut.flush()
    }

    private fun downloadCoverArt(artist: String, title: String, coverUrl: String, mediaStoreId: Long? = null, safeFileName: String? = null) {
        try {
            val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
            val cleanKey = sanitize("${artist}_${title}")
            val namedFile = File(coversDir, "$cleanKey.jpg")

            val conn = URL(coverUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            if (conn.responseCode == 200) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty()) {
                    FileOutputStream(namedFile).use { it.write(bytes) }
                    
                    val titleFile = File(coversDir, "${sanitize(title)}.jpg")
                    FileOutputStream(titleFile).use { it.write(bytes) }

                    if (mediaStoreId != null && mediaStoreId > 0) {
                        val idFile = File(coversDir, "$mediaStoreId.jpg")
                        FileOutputStream(idFile).use { it.write(bytes) }
                    }

                    if (!safeFileName.isNullOrBlank()) {
                        val safeNameNoExt = safeFileName.substringBeforeLast(".")
                        val safeFile = File(coversDir, "${sanitize(safeNameNoExt)}.jpg")
                        FileOutputStream(safeFile).use { it.write(bytes) }

                        val directFile = File(coversDir, "$safeNameNoExt.jpg")
                        FileOutputStream(directFile).use { it.write(bytes) }
                    }

                    // Register persistently in CoverArtManager
                    CoverArtManager(context).registerCover(
                        title = title,
                        artist = artist,
                        fileName = safeFileName ?: "",
                        coverBytes = bytes
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "No se pudo descargar la carátula durante la descarga", e)
        }
    }

    /**
     * Downloads a full video MP4 (video + audio muxed) to Movies/DaVEPlayer gallery with progress.
     */
    suspend fun downloadVideo(
        track: OnlineTrack,
        onComplete: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val downloadId = "vid_${track.id}"
        updateState(downloadId, DownloadStatus.Downloading(0))

        try {
            val videoId = when {
                track.id.startsWith("yt_") -> track.id.removePrefix("yt_")
                track.audioUrl.contains("v=") -> track.audioUrl.substringAfter("v=").substringBefore("&")
                track.audioUrl.contains("youtu.be/") -> track.audioUrl.substringAfter("youtu.be/").substringBefore("?")
                else -> ""
            }

            var videoStreamUrl: String? = null
            if (videoId.isNotBlank()) {
                videoStreamUrl = youtubeRepo.resolveVideoStream(videoId)
            }

            if (videoStreamUrl.isNullOrBlank()) {
                if (track.audioUrl.contains(".mp4", ignoreCase = true)) {
                    videoStreamUrl = track.audioUrl
                }
            }

            if (videoStreamUrl.isNullOrBlank()) {
                throw IllegalStateException("No se pudo obtener el stream de video MP4 de YouTube.")
            }

            val safeTitle = sanitize(track.title).ifBlank { "DaVE_Video_${System.currentTimeMillis()}" }
            val fileName = "$safeTitle.mp4"

            performVideoDownload(videoStreamUrl, downloadId, fileName, track.title)

            updateState(downloadId, DownloadStatus.Completed)
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "No se pudo descargar el video ${track.title}", e)
            val userMsg = e.localizedMessage ?: "Error al descargar el video"
            updateState(downloadId, DownloadStatus.Error(userMsg))
            withContext(Dispatchers.Main) {
                onError?.invoke(userMsg)
            }
        }
    }

    private suspend fun performVideoDownload(
        videoUrl: String,
        downloadId: String,
        fileName: String,
        videoTitle: String
    ) = withContext(Dispatchers.IO) {
        var currentUrl = videoUrl
        var connection: HttpURLConnection
        var redirectCount = 0
        while (true) {
            val url = URL(currentUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 25000
                readTimeout = 30000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                setRequestProperty("Range", "bytes=0-")
            }
            val code = connection.responseCode
            if ((code in 301..303 || code == 307 || code == 308) && redirectCount < 8) {
                val newLocation = connection.getHeaderField("Location")
                if (!newLocation.isNullOrBlank()) {
                    currentUrl = newLocation
                    redirectCount++
                    connection.disconnect()
                    continue
                }
            }
            break
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            connection.disconnect()
            throw IllegalStateException("Servidor de video respondió con código $responseCode")
        }

        val contentLength = connection.getHeaderField("Content-Length")?.toLongOrNull()?.toInt() ?: connection.contentLength
        val inputStream = connection.inputStream

        var savedUri: Uri? = null
        var savedPath: String? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val selection = "${MediaStore.Video.Media.DISPLAY_NAME} = ? AND ${MediaStore.Video.Media.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(fileName, "Movies/DaVEPlayer/")
                try {
                    context.contentResolver.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs)
                } catch (e: Exception) { AppLog.d(TAG, "No se pudo borrar el video existente en MediaStore antes de insertar", e) }

                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.TITLE, videoTitle)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/DaVEPlayer")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        streamWithProgress(inputStream, outputStream, contentLength, downloadId)
                        outputStream.close()
                        savedUri = uri
                    }
                    val updateValues = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, updateValues, null, null)
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Error de escritura de video en MediaStore, se usan archivos: ${e.message}", e)
                savedUri = null
            }
        }

        if (savedUri == null) {
            val publicMoviesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DaVEPlayer")
            val targetDir = if (publicMoviesDir.exists() || publicMoviesDir.mkdirs()) {
                publicMoviesDir
            } else {
                File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "DaVEPlayer").apply { mkdirs() }
            }
            val targetFile = File(targetDir, fileName)
            savedPath = targetFile.absolutePath
            savedUri = Uri.fromFile(targetFile)

            val outputStream = FileOutputStream(targetFile)
            streamWithProgress(inputStream, outputStream, contentLength, downloadId)
            outputStream.close()

            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )
        } else {
            try {
                MediaScannerConnection.scanFile(context, arrayOf(savedPath ?: fileName), arrayOf("video/mp4"), null)
            } catch (e: Exception) { AppLog.d(TAG, "No se pudo reescanear el video guardado en MediaStore", e) }
        }
    }

    private fun updateState(trackId: String, status: DownloadStatus) {
        val current = _downloadStates.value.toMutableMap()
        current[trackId] = status
        _downloadStates.value = current
    }

    private fun sanitize(text: String): String {
        return text.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }
}
