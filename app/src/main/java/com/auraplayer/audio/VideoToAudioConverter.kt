package com.auraplayer.audio

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

object VideoToAudioConverter {

    private const val TAG = "VideoToAudioConverter"

    suspend fun extractAudio(
        context: Context,
        videoUri: Uri,
        videoTitle: String,
        onProgress: (Int) -> Unit = {}
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        val tempAudioFile = File(context.cacheDir, "extracted_${System.currentTimeMillis()}.m4a")

        try {
            onProgress(10)
            extractor.setDataSource(context, videoUri, null)

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

            if (audioTrackIndex < 0 || audioFormat == null) {
                return@withContext Result.failure(Exception("El video seleccionado no contiene ninguna pista de audio reconocible."))
            }

            extractor.selectTrack(audioTrackIndex)
            onProgress(30)

            muxer = MediaMuxer(tempAudioFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val maxBufferSize = audioFormat.let {
                if (it.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) it.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                else 1024 * 1024
            }.coerceAtLeast(64 * 1024)

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            onProgress(50)
            while (true) {
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                extractor.advance()

                if (durationUs > 0) {
                    val pct = (50 + ((bufferInfo.presentationTimeUs.toFloat() / durationUs) * 40)).toInt().coerceIn(50, 90)
                    onProgress(pct)
                }
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()
            onProgress(92)

            // Obtener metadatos reales del audio extraído
            var durationMs = 0L
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(tempAudioFile.absolutePath)
                durationMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } catch (_: Exception) {
            } finally {
                try { mmr.release() } catch (_: Exception) {}
            }

            // Guardar en la biblioteca pública de Música (MediaStore / Scoped Storage)
            val cleanTitle = videoTitle.replace(Regex("[\\\\/:*?\"<>|\\r\\n]"), "_").trim().ifBlank { "Audio_Extraído" }
            val fileName = "$cleanTitle.m4a"
            val mimeType = "audio/mp4"

            var finalUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var pendingInsertUri: Uri? = null
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/DaVEPlayer")
                        put(MediaStore.Audio.Media.TITLE, cleanTitle)
                        put(MediaStore.Audio.Media.ARTIST, "Video Extraído")
                        put(MediaStore.Audio.Media.ALBUM, "DaVE Extractor")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                        if (durationMs > 0) {
                            put(MediaStore.Audio.Media.DURATION, durationMs)
                        }
                    }

                    val insertUri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    pendingInsertUri = insertUri
                    if (insertUri != null) {
                        context.contentResolver.openOutputStream(insertUri)?.use { outStream ->
                            FileInputStream(tempAudioFile).use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        val updateValues = ContentValues().apply {
                            put(MediaStore.Audio.Media.IS_PENDING, 0)
                        }
                        context.contentResolver.update(insertUri, updateValues, null, null)
                        finalUri = insertUri
                    }
                } catch (e: Exception) {
                    AppLog.w(TAG, "Fallo al insertar en MediaStore, recurriendo a File directo", e)
                    pendingInsertUri?.let {
                        try { context.contentResolver.delete(it, null, null) } catch (_: Exception) {}
                    }
                }
            }

            if (finalUri == null) {
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DaVEPlayer")
                if (!musicDir.exists()) musicDir.mkdirs()
                val targetFile = File(musicDir, fileName)
                FileInputStream(tempAudioFile).use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                finalUri = try {
                    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", targetFile)
                } catch (_: Exception) {
                    Uri.fromFile(targetFile)
                }
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf(mimeType), null)
            }

            // Limpieza de temporal
            try { tempAudioFile.delete() } catch (_: Exception) {}

            onProgress(100)
            val result = finalUri ?: return@withContext Result.failure(Exception("No se pudo guardar el archivo de audio extraído"))
            AppLog.i(TAG, "Audio extraído con éxito: $fileName ($result)")
            Result.success(result)

        } catch (e: Exception) {
            AppLog.e(TAG, "Error durante la extracción de audio", e)
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
            try { tempAudioFile.delete() } catch (_: Exception) {}
            Result.failure(e)
        }
    }
}
