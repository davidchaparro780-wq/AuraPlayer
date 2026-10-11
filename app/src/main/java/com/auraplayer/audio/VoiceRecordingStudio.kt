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
class VoiceRecordingStudio(context: Context) {

    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    var isRecording: Boolean = false
        private set
    var currentOutputFile: File? = null
        private set

    private fun getStorageDirectory(): File {
        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DaVE_Recordings")
        if (!publicDir.exists()) publicDir.mkdirs()
        return if (publicDir.canWrite()) {
            publicDir
        } else {
            File(appContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: appContext.filesDir, "DaVE_Recordings").apply { mkdirs() }
        }
    }

    fun startRecording(songTitle: String): File? {
        if (isRecording) return currentOutputFile

        return try {
            val dir = getStorageDirectory()
            val safeTitle = songTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(25).ifBlank { "Track" }
            val file = File(dir, "Cover_${safeTitle}_${System.currentTimeMillis()}.m4a")

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(appContext)
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
        } catch (e: Throwable) {
            AppLog.e(TAG, "Error iniciando grabación vocal", e)
            try {
                recorder?.reset()
            } catch (_: Throwable) {}
            try {
                recorder?.release()
            } catch (_: Throwable) {}
            recorder = null
            isRecording = false
            currentOutputFile = null
            null
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        val mr = recorder
        val file = currentOutputFile
        recorder = null
        isRecording = false
        currentOutputFile = null

        return try {
            if (mr != null) {
                try {
                    mr.stop()
                } catch (e: RuntimeException) {
                    AppLog.w(TAG, "Grabación vocal demasiado corta o detenida abruptamente", e)
                } finally {
                    try {
                        mr.reset()
                    } catch (_: Throwable) {}
                    try {
                        mr.release()
                    } catch (_: Throwable) {}
                }
            }
            if (file != null && file.exists() && file.length() > 1024L) {
                android.media.MediaScannerConnection.scanFile(appContext, arrayOf(file.absolutePath), null, null)
                file
            } else {
                file?.delete()
                null
            }
        } catch (e: Throwable) {
            AppLog.e(TAG, "Error deteniendo grabación vocal", e)
            null
        }
    }

    fun cancelRecording() {
        val mr = recorder
        val file = currentOutputFile
        recorder = null
        isRecording = false
        currentOutputFile = null

        try {
            mr?.reset()
        } catch (_: Throwable) {}
        try {
            mr?.release()
        } catch (_: Throwable) {}
        try {
            file?.delete()
        } catch (_: Throwable) {}
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
