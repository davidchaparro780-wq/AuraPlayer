package com.auraplayer.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
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

            // Crear archivo destino en la carpeta de Música o archivos de la app
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
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

            while (true) {
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                extractor.advance()
            }

            outputFile
        } catch (_: Exception) {
            null
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
            try {
                extractor?.release()
            } catch (_: Exception) {}
        }
    }
}
