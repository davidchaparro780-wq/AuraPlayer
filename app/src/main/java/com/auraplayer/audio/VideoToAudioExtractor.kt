package com.auraplayer.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Extractor Nativo de Audio desde Video (Video to MP3/M4A 320k).
 * Utiliza MediaExtractor y MediaMuxer para clonar el flujo de audio del video
 * a un archivo .m4a/.aac de alta fidelidad sin pérdida de calidad y en segundos.
 */
object VideoToAudioExtractor {

    private const val TAG = "VideoToAudioExtractor"

    suspend fun extractAudioFromVideo(
        context: Context,
        videoUri: Uri,
        outputFileName: String
    ): File? = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        try {
            extractor = MediaExtractor().apply {
                setDataSource(context, videoUri, null)
            }

            // Encontrar el track de audio
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || audioFormat == null) return@withContext null

            extractor.selectTrack(audioTrackIndex)

            // Crear archivo destino en la carpeta pública de Música (o fallback a almacenamiento de la app)
            val publicMusicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "DaVE_Extracted"
            ).apply {
                if (!exists()) mkdirs()
            }
            val musicDir = if (publicMusicDir.exists() && publicMusicDir.canWrite()) {
                publicMusicDir
            } else {
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            }
            val safeName = outputFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val outputFile = File(musicDir, "${safeName}.m4a")

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(64 * 1024)
            } else {
                64 * 1024
            }

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var lastPresentationTimeUs = 0L
            var isFirstSample = true

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                val sampleTimeUs = extractor.sampleTime
                if (isFirstSample) {
                    lastPresentationTimeUs = 0L
                    isFirstSample = false
                } else if (sampleTimeUs > lastPresentationTimeUs) {
                    lastPresentationTimeUs = sampleTimeUs
                } else {
                    lastPresentationTimeUs += 1000L
                }

                bufferInfo.presentationTimeUs = lastPresentationTimeUs
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                extractor.advance()
            }

            // Escanear el archivo para que aparezca inmediatamente en MediaStore y la biblioteca
            try {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    arrayOf("audio/mp4", "audio/m4a"),
                    null
                )
            } catch (e: Exception) {
                AppLog.d(TAG, "No se pudo registrar el audio extraído en MediaStore", e)
            }

            outputFile
        } catch (e: Exception) {
            AppLog.e(TAG, "Fallo al extraer el audio del vídeo", e)
            null
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                AppLog.d(TAG, "No se pudo liberar el muxer de audio", e)
            }
            try {
                extractor?.release()
            } catch (e: Exception) {
                AppLog.d(TAG, "No se pudo liberar el extractor del vídeo", e)
            }
        }
    }
}
