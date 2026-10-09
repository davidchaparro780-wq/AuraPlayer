package com.auraplayer.audio

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.session.MediaController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Gestor del Modo "ZAP" (DJ Escucha Rápida de Coros / Track Previewer).
 * Reproduce 12 a 15 segundos del punto álgido de cada canción para explorar
 * listas completas a velocidad relámpago.
 */
class ZapPreviewManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dave_zap_preview", Context.MODE_PRIVATE)

    var isZapActive: Boolean = false
        private set

    var previewDurationSeconds: Int
        get() = prefs.getInt("zap_duration_sec", 12)
        set(value) {
            prefs.edit().putInt("zap_duration_sec", value.coerceIn(8, 25)).apply()
        }

    private var zapJob: Job? = null

    fun startZap(controller: MediaController?, scope: CoroutineScope) {
        isZapActive = true
        zapJob?.cancel()
        zapJob = scope.launch(Dispatchers.Main) {
            while (isActive && isZapActive) {
                controller?.let { c ->
                    val dur = c.duration
                    if (dur > 20_000L) {
                        // Saltar al 35% de la canción (generalmente el coro)
                        val chorusStart = (dur * 0.35).toLong()
                        c.seekTo(chorusStart)
                    }
                }
                delay(previewDurationSeconds * 1000L)
                if (isActive && isZapActive) {
                    controller?.seekToNextMediaItem()
                }
            }
        }
    }

    fun stopZap() {
        isZapActive = false
        zapJob?.cancel()
        zapJob = null
    }

    companion object {
        @Volatile
        private var instance: ZapPreviewManager? = null

        fun getInstance(context: Context): ZapPreviewManager {
            return instance ?: synchronized(this) {
                instance ?: ZapPreviewManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
