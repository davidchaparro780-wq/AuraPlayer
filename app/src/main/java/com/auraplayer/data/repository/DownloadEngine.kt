package com.auraplayer.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import com.auraplayer.data.model.MediaModel
import com.auraplayer.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
            // 1. Prepare target directory in standard Music/AuraPlayer
            val publicMusicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "AuraPlayer")
            val targetDir = if (publicMusicDir.exists() || publicMusicDir.mkdirs()) {
                publicMusicDir
            } else {
                File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "AuraPlayer").apply { mkdirs() }
            }

            val safeFileName = "${sanitize(track.artist)} - ${sanitize(track.title)}.mp3"
            val targetFile = File(targetDir, safeFileName)

            // 2. Stream & Download Audio File with Progress
            val url = URL(track.audioUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setRequestProperty("Accept", "*/*")
            }

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Servidor respondió con código ${connection.responseCode}")
            }

            val contentLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 99)
                    updateState(track.id, DownloadStatus.Downloading(progress))
                }
            }

            outputStream.flush()
            outputStream.close()
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
                uri = android.net.Uri.fromFile(targetFile),
                artworkUri = if (track.coverUrl.isNotBlank()) android.net.Uri.parse(track.coverUrl) else null,
                path = targetFile.absolutePath,
                size = targetFile.length()
            )
            try {
                lyricsManager.getLyrics(dummyMedia)
            } catch (e: Exception) {
                // Non-fatal
            }

            // 5. Notify MediaScanner to index track in Android MediaStore instantly
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                null
            ) { _, _ ->
                // Media store scanned
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
