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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

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
    private val lyricsManager: LyricsManager
) {

    private val _downloadStates = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadStatus>> = _downloadStates.asStateFlow()

    fun getStatus(trackId: String): DownloadStatus {
        return _downloadStates.value[trackId] ?: DownloadStatus.Idle
    }

    suspend fun downloadTrack(
        track: OnlineTrack,
        onComplete: (() -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        updateState(track.id, DownloadStatus.Downloading(0))

        try {
            val safeArtist = sanitize(track.artist).ifBlank { "Aura Artist" }
            val safeTitle = sanitize(track.title).ifBlank { "Aura Song" }
            val safeFileName = "$safeArtist - $safeTitle.mp3"

            // 1. Establish HTTP connection with multi-redirect support (CDNs, Storage)
            var currentUrl = track.audioUrl
            var connection: HttpURLConnection
            var redirectCount = 0
            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 15000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    setRequestProperty("Accept", "*/*")
                }
                val code = connection.responseCode
                if ((code in 301..303 || code == 307 || code == 308) && redirectCount < 5) {
                    val newLocation = connection.getHeaderField("Location")
                    if (!newLocation.isNullOrBlank()) {
                        currentUrl = newLocation
                        redirectCount++
                        continue
                    }
                }
                break
            }

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Servidor respondió con código ${connection.responseCode}")
            }

            val contentLength = connection.contentLength
            val inputStream = connection.inputStream

            var savedFileUri: Uri? = null
            var savedFilePath: String? = null

            // 2. Write file using MediaStore on Android 10+ (Scoped Storage safe) or direct File
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, safeFileName)
                    put(MediaStore.Audio.Media.TITLE, track.title)
                    put(MediaStore.Audio.Media.ARTIST, track.artist)
                    put(MediaStore.Audio.Media.ALBUM, track.album)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/AuraPlayer")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    savedFileUri = uri
                    val outputStream = context.contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        streamWithProgress(inputStream, outputStream, contentLength, track.id)
                        outputStream.close()
                    }
                    values.clear()
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
            }

            // Fallback for Android 9 or below, or if MediaStore insert failed
            if (savedFileUri == null) {
                val publicMusicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "AuraPlayer")
                val targetDir = if (publicMusicDir.exists() || publicMusicDir.mkdirs()) {
                    publicMusicDir
                } else {
                    File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AuraPlayer").apply { mkdirs() }
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
                    arrayOf("audio/mpeg"),
                    null
                )
            }

            inputStream.close()

            updateState(track.id, DownloadStatus.Tagging)

            // 3. Save Cover Art locally for instant offline display
            if (track.coverUrl.isNotBlank()) {
                downloadCoverArt(track.artist, track.title, track.coverUrl)
            }

            // 4. Pre-cache synced lyrics if available
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
            } catch (_: Exception) {
            }

            updateState(track.id, DownloadStatus.Completed)
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            updateState(track.id, DownloadStatus.Error(e.localizedMessage ?: "Error de descarga"))
        }
    }

    private fun streamWithProgress(
        inputStream: java.io.InputStream,
        outputStream: OutputStream,
        contentLength: Int,
        trackId: String
    ) {
        val buffer = ByteArray(8192)
        var bytesRead: Int
        var totalBytesRead = 0L

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead
            if (contentLength > 0) {
                val progress = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 99)
                updateState(trackId, DownloadStatus.Downloading(progress))
            }
        }
        outputStream.flush()
    }

    private fun downloadCoverArt(artist: String, title: String, coverUrl: String) {
        try {
            val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
            val cleanKey = sanitize("${artist}_${title}")
            val namedFile = File(coversDir, "$cleanKey.jpg")

            val conn = URL(coverUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            if (conn.responseCode == 200) {
                conn.inputStream.use { input ->
                    FileOutputStream(namedFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
