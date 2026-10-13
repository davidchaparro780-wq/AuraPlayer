package com.auraplayer.data.repository

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

data class RecognizedTrack(
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String
)

object SongHunterService {

    private const val TAG = "SongHunterService"

    /**
     * Graba 4 segundos de audio del ambiente y consulta el identificador acústico.
     */
    suspend fun recordAndIdentify(context: Context): RecognizedTrack? = withContext(Dispatchers.IO) {
        val sampleFile = File(context.cacheDir, "sample_identify_${System.currentTimeMillis()}.m4a")
        var recorder: MediaRecorder? = null
        try {
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(sampleFile.absolutePath)
                prepare()
                start()
            }
            recorder = mr

            // Escuchar durante 7 segundos para capturar suficiente huella acústica
            delay(7000)

            try {
                mr.stop()
            } catch (_: Exception) {}
            try {
                mr.release()
            } catch (_: Exception) {}
            recorder = null

            if (!sampleFile.exists() || sampleFile.length() < 1000L) {
                return@withContext null
            }

            // Identificar el archivo grabado
            return@withContext identifyAudioFile(sampleFile)
        } catch (e: Exception) {
            AppLog.e(TAG, "Error durante la grabación para reconocimiento", e)
            try { recorder?.release() } catch (_: Exception) {}
            return@withContext null
        } finally {
            try { sampleFile.delete() } catch (_: Exception) {}
        }
    }

    private suspend fun identifyAudioFile(file: File): RecognizedTrack? = withContext(Dispatchers.IO) {
        try {
            // AudD Music Recognition Public Endpoint / AcoustID
            val boundary = "==Bound${System.currentTimeMillis()}=="
            val url = URL("https://api.audd.io/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("User-Agent", "DaVE-Player/4.6.0")
            }

            conn.outputStream.use { os ->
                // api_token field (test token or free demo tier)
                os.write("--$boundary\r\n".toByteArray())
                os.write("Content-Disposition: form-data; name=\"api_token\"\r\n\r\n".toByteArray())
                os.write("test\r\n".toByteArray())

                // return fields
                os.write("--$boundary\r\n".toByteArray())
                os.write("Content-Disposition: form-data; name=\"return\"\r\n\r\n".toByteArray())
                os.write("apple_music,spotify\r\n".toByteArray())

                // file field
                os.write("--$boundary\r\n".toByteArray())
                os.write("Content-Disposition: form-data; name=\"file\"; filename=\"sample.m4a\"\r\n".toByteArray())
                os.write("Content-Type: audio/m4a\r\n\r\n".toByteArray())
                FileInputStream(file).use { fis ->
                    fis.copyTo(os)
                }
                os.write("\r\n--$boundary--\r\n".toByteArray())
                os.flush()
            }

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(resp)
                if (root.optString("status") == "success") {
                    val result = root.optJSONObject("result")
                    if (result != null) {
                        val title = result.optString("title", "")
                        val artist = result.optString("artist", "")
                        val album = result.optString("album", "")
                        val spotify = result.optJSONObject("spotify")
                        val apple = result.optJSONObject("apple_music")
                        val cover = spotify?.optJSONObject("album")?.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                            ?: apple?.optJSONObject("artwork")?.optString("url")?.replace("{w}x{h}", "600x600")
                            ?: ""
                        if (title.isNotBlank()) {
                            return@withContext RecognizedTrack(
                                title = title,
                                artist = artist.ifBlank { "Desconocido" },
                                album = album,
                                coverUrl = cover
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.d(TAG, "Fallo consulta de huella acústica: ${e.message}")
        }
        null
    }
}
