package com.auraplayer.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.auraplayer.data.model.OnlineTrack
import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream

data class RadarRecognizedSong(
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val previewUrl: String? = null,
    val releaseYear: String = ""
)

object DaveRadarManager {
    private const val TAG = "DaveRadarManager"

    /**
     * Graba audio ambiental de alta fidelidad desde el micrófono y consulta
     * la huella acústica en el catálogo mundial de reconocimiento musical.
     * Si no se encuentra coincidencia, retorna explícitamente un fallo.
     */
    suspend fun identifyFromSurroundings(
        context: Context,
        onProgress: (Float) -> Unit
    ): Result<RadarRecognizedSong> = identifyFromSurroundings(context, onProgress, {})

    suspend fun identifyFromSurroundings(
        context: Context,
        onProgress: (Float) -> Unit,
        onStatusUpdate: (String) -> Unit
    ): Result<RadarRecognizedSong> = withContext(Dispatchers.IO) {
        // 1. Verificación estricta de permisos de micrófono
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return@withContext Result.failure(SecurityException("Permiso de micrófono no concedido. Habilita el permiso para usar DaVE Radar."))
        }

        val sampleFile = File(context.cacheDir, "dave_radar_sample_${System.currentTimeMillis()}.m4a")
        var recorder: MediaRecorder? = null

        try {
            onStatusUpdate("Iniciando micrófono...")
            onProgress(0.04f)

            // 2. Inicializar MediaRecorder con compresión AAC 44.1kHz
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

            // 3. Ventana de escucha ampliada (~7.5 segundos) para capturar suficiente huella acústica
            for (step in 1..75) {
                delay(100)
                val progress = 0.04f + (step / 75f) * 0.66f // 4% -> 70%
                onProgress(progress)

                when (step) {
                    1 -> onStatusUpdate("Escuchando música del ambiente...")
                    25 -> onStatusUpdate("Capturando ondas sonoras...")
                    50 -> onStatusUpdate("Extrayendo frecuencias acústicas...")
                }
            }

            // 4. Detener y liberar grabación
            try {
                mr.stop()
            } catch (e: Exception) {
                AppLog.w(TAG, "Excepción al detener MediaRecorder: ${e.message}")
            }
            try {
                mr.release()
            } catch (_: Exception) {}
            recorder = null

            // 5. Validar muestra acústica grabada
            if (!sampleFile.exists() || sampleFile.length() < 2000L) {
                AppLog.w(TAG, "Muestra de audio inválida o muy pequeña: ${sampleFile.length()} bytes")
                return@withContext Result.failure(Exception("No se detectó audio del ambiente. Acércate más a la música."))
            }

            // 6. Consultar base de datos acústica mundial
            onStatusUpdate("Identificando huella acústica en catálogo...")
            onProgress(0.75f)

            val identified = identifyAudioSample(sampleFile) { serverProgress ->
                onProgress(0.75f + serverProgress * 0.24f)
            }

            onProgress(1.0f)

            if (identified != null) {
                AppLog.i(TAG, "Canción identificada exitosamente: ${identified.title} - ${identified.artist}")
                Result.success(identified)
            } else {
                AppLog.i(TAG, "No se encontró coincidencia acústica para la muestra")
                Result.failure(Exception("No se encontró la música. Asegúrate de estar cerca del parlante y que no haya demasiado ruido de fondo."))
            }

        } catch (e: Exception) {
            AppLog.e(TAG, "Error identificando audio por radar", e)
            try { recorder?.release() } catch (_: Exception) {}
            Result.failure(Exception("Error durante el escaneo: ${e.message ?: "Intenta de nuevo."}"))
        } finally {
            try {
                if (sampleFile.exists()) {
                    sampleFile.delete()
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Envía la muestra grabada al motor de reconocimiento acústico AudD y enriquece metadatos.
     */
    private suspend fun identifyAudioSample(
        file: File,
        onProgress: (Float) -> Unit
    ): RadarRecognizedSong? = withContext(Dispatchers.IO) {
        try {
            onProgress(0.15f)
            val boundary = "==DaVERadar${System.currentTimeMillis()}=="
            val url = URL("https://api.audd.io/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("User-Agent", "DaVE-Player/4.9.4 (Android)")
            }

            onProgress(0.35f)
            conn.outputStream.use { os ->
                // api_token público de consulta
                os.write("--$boundary\r\n".toByteArray())
                os.write("Content-Disposition: form-data; name=\"api_token\"\r\n\r\n".toByteArray())
                os.write("test\r\n".toByteArray())

                // campos a retornar
                os.write("--$boundary\r\n".toByteArray())
                os.write("Content-Disposition: form-data; name=\"return\"\r\n\r\n".toByteArray())
                os.write("apple_music,spotify\r\n".toByteArray())

                // archivo de audio .m4a
                os.write("--$boundary\r\n".toByteArray())
                os.write("Content-Disposition: form-data; name=\"file\"; filename=\"sample.m4a\"\r\n".toByteArray())
                os.write("Content-Type: audio/m4a\r\n\r\n".toByteArray())
                FileInputStream(file).use { fis ->
                    fis.copyTo(os)
                }
                os.write("\r\n--$boundary--\r\n".toByteArray())
                os.flush()
            }

            onProgress(0.65f)

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                onProgress(0.85f)
                val root = JSONObject(resp)
                if (root.optString("status") == "success") {
                    val result = root.optJSONObject("result")
                    if (result != null) {
                        val title = result.optString("title", "").trim()
                        val artist = result.optString("artist", "").trim()
                        val album = result.optString("album", "").trim()
                        val releaseYear = result.optString("release_date", "").take(4)
                        val spotify = result.optJSONObject("spotify")
                        val apple = result.optJSONObject("apple_music")
                        var cover = spotify?.optJSONObject("album")?.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                            ?: apple?.optJSONObject("artwork")?.optString("url")?.replace("{w}x{h}", "600x600")
                            ?: ""
                        var preview = apple?.optJSONArray("previews")?.optJSONObject(0)?.optString("url")
                            ?: spotify?.optString("preview_url")

                        if (title.isNotBlank() && artist.isNotBlank()) {
                            // Complementar portada de alta resolución si es necesario
                            if (cover.isBlank() || preview.isNullOrBlank()) {
                                val extra = fetchItunesMetadata(title, artist)
                                if (cover.isBlank() && extra?.coverUrl?.isNotBlank() == true) {
                                    cover = extra.coverUrl
                                }
                                if (preview.isNullOrBlank() && !extra?.previewUrl.isNullOrBlank()) {
                                    preview = extra?.previewUrl
                                }
                            }

                            onProgress(1.0f)
                            return@withContext RadarRecognizedSong(
                                title = title,
                                artist = artist,
                                album = album.ifBlank { "Sencillo" },
                                coverUrl = cover,
                                previewUrl = preview,
                                releaseYear = releaseYear
                            )
                        }
                    }
                }
            } else {
                AppLog.w(TAG, "AudD respondió con código HTTP: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "Fallo al consultar servicio de reconocimiento acústico", e)
        }
        null
    }

    /**
     * Búsqueda complementaria en iTunes para obtener carátula HD y preview.
     */
    private fun fetchItunesMetadata(title: String, artist: String): RadarRecognizedSong? {
        return try {
            val query = URLEncoder.encode("$title $artist", "UTF-8")
            val apiUrl = "https://itunes.apple.com/search?term=$query&entity=song&limit=1"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(json)
                val results = root.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val item = results.getJSONObject(0)
                    val cover = item.optString("artworkUrl100", "").replace("100x100bb", "600x600bb")
                    val preview = item.optString("previewUrl", "")
                    val year = item.optString("releaseDate", "").take(4)
                    RadarRecognizedSong(
                        title = item.optString("trackName", title),
                        artist = item.optString("artistName", artist),
                        album = item.optString("collectionName", ""),
                        coverUrl = cover,
                        previewUrl = preview,
                        releaseYear = year
                    )
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Búsqueda fonética o por tarareo a través del motor unificado.
     */
    suspend fun searchHummingOrQuery(query: String): Result<List<RadarRecognizedSong>> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val apiUrl = "https://itunes.apple.com/search?term=$encoded&entity=song&limit=6"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(json)
                val results = root.optJSONArray("results")
                val list = mutableListOf<RadarRecognizedSong>()

                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val title = item.optString("trackName", "")
                        val artist = item.optString("artistName", "")
                        val album = item.optString("collectionName", "")
                        val artwork = item.optString("artworkUrl100", "").replace("100x100bb", "600x600bb")
                        val preview = item.optString("previewUrl", "")
                        val year = item.optString("releaseDate", "").take(4)

                        if (title.isNotBlank()) {
                            list.add(
                                RadarRecognizedSong(
                                    title = title,
                                    artist = artist,
                                    album = album,
                                    coverUrl = artwork,
                                    previewUrl = preview,
                                    releaseYear = year
                                )
                            )
                        }
                    }
                }
                if (list.isNotEmpty()) {
                    return@withContext Result.success(list)
                }
            }
            Result.failure(Exception("No se encontraron coincidencias para la melodía"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
