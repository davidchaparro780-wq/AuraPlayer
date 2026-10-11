package com.auraplayer.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import com.auraplayer.util.AppLog
import java.io.File

private const val TAG = "VoiceRecordingStudio"

/**
 * Estudio de Grabación Vocal de Alta Definición:
 * Permite grabar la voz del usuario directamente con micrófono (AAC/M4A a 48kHz HQ)
 * mientras suena la música en segundo plano o en modo Karaoke (para covers o notas de voz).
 */
class VoiceRecordingStudio(private val context: Context) {

    private var recorder: MediaRecorder? = null
    var isRecording: Boolean = false
        private set
    var currentOutputFile: File? = null
        private set

    fun startRecording(songTitle: String): File? {
        if (isRecording) return currentOutputFile

        return try {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DaVE_Recordings")
            if (!dir.exists()) dir.mkdirs()

            val safeTitle = songTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(25)
            val file = File(dir, "Cover_${safeTitle}_${System.currentTimeMillis()}.m4a")

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
                setAudioEncodingBitRate(192000)
                setAudioSamplingRate(48000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mr
            isRecording = true
            currentOutputFile = file
            file
        } catch (e: Exception) {
            AppLog.e(TAG, "Error iniciando grabación vocal", e)
            recorder?.release()
            recorder = null
            isRecording = false
            currentOutputFile = null
            null
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            val file = currentOutputFile
            if (file != null && file.exists()) {
                android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
            }
            file
        } catch (e: Exception) {
            AppLog.e(TAG, "Error deteniendo grabación vocal", e)
            recorder?.release()
            recorder = null
            isRecording = false
            null
        }
    }

    companion object {
        @Volatile
        private var instance: VoiceRecordingStudio? = null

        fun getInstance(context: Context): VoiceRecordingStudio {
            return instance ?: synchronized(this) {
                instance ?: VoiceRecordingStudio(context.applicationContext).also { instance = it }
            }
        }
    }
}
