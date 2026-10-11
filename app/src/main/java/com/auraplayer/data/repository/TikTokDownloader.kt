package com.auraplayer.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class TikTokVideoDetails(
    val id: String,
    val title: String,
    val author: String,
    val username: String,
    val coverUrl: String,
    val videoNoWatermarkUrl: String,
    val videoHdUrl: String?,
    val audioUrl: String?,
    val durationSeconds: Int = 0
)

sealed class TikTokDownloadProgress {
    object Resolving : TikTokDownloadProgress()
    data class Downloading(val progress: Int) : TikTokDownloadProgress()
    data class Completed(val fileUri: Uri, val filePath: String?, val isVideo: Boolean) : TikTokDownloadProgress()
    data class Error(val message: String) : TikTokDownloadProgress()
}

object TikTokDownloader {
    private const val TAG = "TikTokDownloader"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    fun isTikTokUrl(url: String): Boolean {
        val trimmed = url.trim().lowercase()
        return (trimmed.contains("tiktok.com") || trimmed.contains("douyin.com")) &&
                (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
    }

    /**
     * Resuelve los enlaces limpios sin marca de agua del video de TikTok a través de múltiples motores resilientes.
     */
    suspend fun resolveTikTok(rawUrl: String): Result<TikTokVideoDetails> = withContext(Dispatchers.IO) {
        val cleanUrl = rawUrl.trim()
        if (!isTikTokUrl(cleanUrl)) {
            return@withContext Result.failure(IllegalArgumentException("El enlace no parece ser un enlace válido de TikTok."))
        }

        // 1. Intento primario: TikWM API
        try {
            val encoded = URLEncoder.encode(cleanUrl, "UTF-8")
            val tikWmApi = "https://www.tikwm.com/api/?url=$encoded&hd=1"
            val conn = (URL(tikWmApi).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseText)
                if (root.optInt("code", -1) == 0) {
                    val data = root.optJSONObject("data")
                    if (data != null) {
                        val videoUrl = data.optString("play", "")
                        val hdVideoUrl = data.optString("hdplay", "").takeIf { it.isNotBlank() }
                        val audioUrl = data.optString("music", "").takeIf { it.isNotBlank() }
                        val cover = data.optString("cover", "")
                        val title = data.optString("title", "Video de TikTok").trim().ifBlank { "Video de TikTok" }
                        val authorObj = data.optJSONObject("author")
                        val authorName = authorObj?.optString("nickname", "TikTok Creator") ?: "TikTok Creator"
                        val username = authorObj?.optString("unique_id", "tiktok") ?: "tiktok"
                        val id = data.optString("id", System.currentTimeMillis().toString())
                        val duration = data.optInt("duration", 0)

                        val finalVideoUrl = hdVideoUrl ?: videoUrl
                        if (finalVideoUrl.isNotBlank()) {
                            AppLog.d(TAG, "TikTok resuelto con éxito vía TikWM: $id ($title)")
                            return@withContext Result.success(
                                TikTokVideoDetails(
                                    id = id,
                                    title = title,
                                    author = authorName,
                                    username = username,
                                    coverUrl = cover,
                                    videoNoWatermarkUrl = finalVideoUrl,
                                    videoHdUrl = hdVideoUrl,
                                    audioUrl = audioUrl,
                                    durationSeconds = duration
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "TikWM API falló, probando motor alternativo...", e)
        }

        // 2. Intento secundario: Tiklydown API
        try {
            val encoded = URLEncoder.encode(cleanUrl, "UTF-8")
            val tiklyApi = "https://api.tiklydown.eu.org/api/download/v5?url=$encoded"
            val conn = (URL(tiklyApi).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseText)
                val resultObj = root.optJSONObject("result")
                val videoObj = resultObj?.optJSONObject("video")
                val noWmUrl = videoObj?.optString("no_watermark", "") ?: ""
                val musicObj = resultObj?.optJSONObject("music")
                val audioUrl = musicObj?.optString("play_url", "")

                if (noWmUrl.isNotBlank()) {
                    val authorObj = resultObj?.optJSONObject("author")
                    val authorName = authorObj?.optString("name", "TikTok Creator") ?: "TikTok Creator"
                    val username = authorObj?.optString("unique_id", "tiktok") ?: "tiktok"
                    val title = resultObj?.optString("title", "Video de TikTok") ?: "Video de TikTok"
                    val cover = videoObj?.optString("cover", "") ?: ""
                    val id = resultObj?.optString("id", System.currentTimeMillis().toString()) ?: System.currentTimeMillis().toString()

                    AppLog.d(TAG, "TikTok resuelto con éxito vía Tiklydown: $id")
                    return@withContext Result.success(
                        TikTokVideoDetails(
                            id = id,
                            title = title,
                            author = authorName,
                            username = username,
                            coverUrl = cover,
                            videoNoWatermarkUrl = noWmUrl,
                            videoHdUrl = null,
                            audioUrl = audioUrl,
                            durationSeconds = 0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "Tiklydown API falló: ${e.message}", e)
        }

        Result.failure(Exception("No se pudo obtener el video sin marca de agua. Verifica que el enlace sea público e intenta de nuevo."))
    }

    /**
     * Descarga el archivo de video (MP4) o audio (MP3) en el almacenamiento público del dispositivo
     * y notifica a MediaStore y MediaScannerConnection para que aparezca al instante en la app y galería.
     */
    suspend fun downloadMedia(
        context: Context,
        downloadUrl: String,
        details: TikTokVideoDetails,
        asAudioOnly: Boolean = false,
        onProgress: (Int) -> Unit
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val isVideo = !asAudioOnly
            val extension = if (isVideo) ".mp4" else ".mp3"
            val mimeType = if (isVideo) "video/mp4" else "audio/mpeg"

            val safeTitle = sanitizeFileName(details.title).take(50).ifBlank { "TikTok_${details.id}" }
            val safeAuthor = sanitizeFileName(details.author).take(30).ifBlank { details.username }
            val fileName = "TikTok - ${safeAuthor}_$safeTitle$extension"

            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirects = 0

            while (true) {
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 25000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", "*/*")
                    setRequestProperty("Connection", "keep-alive")
                }
                val code = connection.responseCode
                if ((code in 301..303 || code == 307 || code == 308) && redirects < 6) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirects++
                        connection.disconnect()
                        continue
                    }
                }
                break
            }

            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return@withContext Result.failure(Exception("Error del servidor: HTTP ${connection.responseCode}"))
            }

            val contentLength = connection.getHeaderField("Content-Length")?.toLongOrNull() ?: connection.contentLength.toLong()
            val inputStream = connection.inputStream

            var finalUri: Uri? = null
            var finalPath: String? = null

            // Almacenamiento Scoped Storage (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val collectionUri = if (isVideo) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }

                    val relativePath = if (isVideo) "Movies/DaVEPlayer" else "Music/DaVEPlayer"

                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                        if (!isVideo) {
                            put(MediaStore.Audio.Media.TITLE, details.title)
                            put(MediaStore.Audio.Media.ARTIST, details.author)
                        } else {
                            put(MediaStore.Video.Media.TITLE, details.title)
                        }
                    }

                    val uri = context.contentResolver.insert(collectionUri, values)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            copyStreamWithProgress(inputStream, output, contentLength) { pct ->
                                onProgress(pct)
                            }
                        }

                        val updateValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                        context.contentResolver.update(uri, updateValues, null, null)
                        finalUri = uri
                    }
                } catch (e: Exception) {
                    AppLog.w(TAG, "Fallo al guardar vía MediaStore, recurriendo a File directo", e)
                }
            }

            // Fallback para Android 9 o si falló MediaStore
            if (finalUri == null) {
                val baseDir = if (isVideo) {
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "DaVEPlayer")
                } else {
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DaVEPlayer")
                }
                if (!baseDir.exists()) baseDir.mkdirs()

                val targetFile = File(baseDir, fileName)
                FileOutputStream(targetFile).use { output ->
                    copyStreamWithProgress(inputStream, output, contentLength) { pct ->
                        onProgress(pct)
                    }
                }
                finalPath = targetFile.absolutePath
                finalUri = Uri.fromFile(targetFile)

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }

            inputStream.close()
            connection.disconnect()

            val resultUri = finalUri ?: return@withContext Result.failure(Exception("No se pudo obtener el URI del archivo guardado"))
            AppLog.i(TAG, "Descarga completada: $fileName ($resultUri)")
            Result.success(resultUri)
        } catch (e: Exception) {
            AppLog.e(TAG, "Error durante la descarga de TikTok", e)
            Result.failure(e)
        }
    }

    private fun copyStreamWithProgress(
        input: InputStream,
        output: OutputStream,
        totalBytes: Long,
        onProgress: (Int) -> Unit
    ) {
        val buffer = ByteArray(16 * 1024)
        var bytesRead: Int
        var totalRead = 0L
        var lastReported = -1

        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
            totalRead += bytesRead
            if (totalBytes > 0) {
                val progress = ((totalRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                if (progress != lastReported) {
                    lastReported = progress
                    onProgress(progress)
                }
            } else {
                // Si el servidor no envió Content-Length, simular un progreso razonable
                val estimated = (totalRead / (100 * 1024)).toInt().coerceIn(10, 90)
                if (estimated != lastReported) {
                    lastReported = estimated
                    onProgress(estimated)
                }
            }
        }
        output.flush()
        onProgress(100)
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
