package com.auraplayer.audio

import android.content.Context
import androidx.media3.session.MediaController
import com.auraplayer.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Gestor de Crossfade Dinámico y Transiciones Suaves para DaVE Player.
 * Permite fundidos cruzados entre canciones y desvanecimiento progresivo (Fade In/Out)
 * al pausar, reproducir o cambiar de pista.
 */
class CrossfadeManager(context: Context) {

    private val prefs = context.getSharedPreferences("dave_audio_fx_prefs", Context.MODE_PRIVATE)

    var isCrossfadeEnabled: Boolean
        get() = prefs.getBoolean("crossfade_enabled", true)
        set(value) = prefs.edit().putBoolean("crossfade_enabled", value).apply()

    var crossfadeSeconds: Int
        get() = prefs.getInt("crossfade_seconds", 3).coerceIn(1, 10)
        set(value) = prefs.edit().putInt("crossfade_seconds", value.coerceIn(1, 10)).apply()

    var isDuckingEnabled: Boolean
        get() = prefs.getBoolean("audio_ducking_enabled", true)
        set(value) = prefs.edit().putBoolean("audio_ducking_enabled", value).apply()

    private var fadeJob: Job? = null

    /**
     * Aplica un desvanecimiento suave de volumen (Fade In o Fade Out)
     */
    fun fadeVolume(
        controller: MediaController?,
        fromVolume: Float,
        toVolume: Float,
        durationMs: Long = 400L,
        onEnd: (() -> Unit)? = null
    ) {
        if (controller == null) {
            onEnd?.invoke()
            return
        }

        fadeJob?.cancel()
        fadeJob = CoroutineScope(Dispatchers.Main).launch {
            val steps = 15
            val stepDelay = (durationMs / steps).coerceAtLeast(15L)
            val volumeDelta = (toVolume - fromVolume) / steps

            var currentVol = fromVolume
            for (i in 0 until steps) {
                currentVol = (currentVol + volumeDelta).coerceIn(0f, 1f)
                try {
                    controller.volume = currentVol
                } catch (e: Exception) {
                    AppLog.w(TAG, "Fallo al ajustar el volumen durante el crossfade", e)
                }
                delay(stepDelay)
            }
            try {
                controller.volume = toVolume
            } catch (e: Exception) {
                AppLog.w(TAG, "Fallo al fijar el volumen final del crossfade", e)
            }
            onEnd?.invoke()
        }
    }

    /**
     * Pausa suave con Fade-out progresivo
     */
    fun smoothPause(controller: MediaController?, durationMs: Long = 350L) {
        if (controller == null) return
        fadeVolume(controller, fromVolume = 1.0f, toVolume = 0.05f, durationMs = durationMs) {
            controller.pause()
            controller.volume = 1.0f
        }
    }

    /**
     * Reproduce con Fade-in progresivo
     */
    fun smoothPlay(controller: MediaController?, durationMs: Long = 350L) {
        if (controller == null) return
        controller.volume = 0.05f
        controller.play()
        fadeVolume(controller, fromVolume = 0.05f, toVolume = 1.0f, durationMs = durationMs)
    }

    companion object {
        private const val TAG = "CrossfadeManager"

        @Volatile
        private var instance: CrossfadeManager? = null

        fun getInstance(context: Context): CrossfadeManager {
            return instance ?: synchronized(this) {
                instance ?: CrossfadeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
