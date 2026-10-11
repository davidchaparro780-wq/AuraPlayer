package com.auraplayer.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.auraplayer.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "HapticBassManager"

/**
 * Subwoofer Háptico en Tiempo Real:
 * Conecta el motor de vibración háptica de precisión con el espectro de graves (< 100 Hz).
 * Vibra de forma proporcional y rítmica al compás de los golpes del bajo (kick/bass),
 * permitiendo sentir físicamente el impacto musical en las manos.
 */
class HapticBassManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val prefs = context.getSharedPreferences("aura_haptic_bass", Context.MODE_PRIVATE)
    private var syncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var isEnabled: Boolean
        get() = prefs.getBoolean("haptic_bass_enabled", false)
        set(value) {
            prefs.edit().putBoolean("haptic_bass_enabled", value).apply()
            if (value) startRealtimeBassSync() else stopRealtimeBassSync()
        }

    init {
        if (isEnabled) {
            startRealtimeBassSync()
        }
    }

    fun startRealtimeBassSync() {
        if (syncJob?.isActive == true) return
        syncJob = scope.launch {
            var lastPulseTime = 0L
            while (isActive) {
                if (!isEnabled) {
                    delay(500)
                    continue
                }

                val now = System.currentTimeMillis()
                // Obtener energía de graves en tiempo real del motor FFT (0.05f .. 1.0f)
                val bass = RealtimeVisualizerManager.instance.getBassEnergy(true)

                // Disparo de pulso cuando la energía de graves supera el umbral (punch de bajo)
                if (bass > 0.65f && (now - lastPulseTime) > 130L) {
                    lastPulseTime = now
                    val intensity = ((bass - 0.5f) * 2f).coerceIn(0.1f, 1.0f)
                    triggerDynamicPulse(intensity)
                }

                delay(40)
            }
        }
    }

    fun stopRealtimeBassSync() {
        syncJob?.cancel()
        syncJob = null
    }

    private fun triggerDynamicPulse(intensity: Float) {
        if (vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = (intensity * 255).toInt().coerceIn(40, 255)
                val duration = (25 + (intensity * 25)).toLong()
                val effect = VibrationEffect.createOneShot(duration, amp)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(28)
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "Fallo al emitir el pulso háptico de graves", e)
        }
    }

    fun triggerBassPulse() {
        triggerDynamicPulse(0.85f)
    }

    companion object {
        @Volatile
        private var instance: HapticBassManager? = null

        fun getInstance(context: Context): HapticBassManager {
            return instance ?: synchronized(this) {
                instance ?: HapticBassManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
