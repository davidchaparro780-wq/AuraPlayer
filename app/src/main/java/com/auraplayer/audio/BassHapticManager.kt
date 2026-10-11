package com.auraplayer.audio

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.auraplayer.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class HapticIntensity(val label: String, val ms: Long, val amplitude: Int) {
    OFF("Apagado", 0L, 0),
    LIGHT("Suave", 14L, 75),
    MEDIUM("Medio", 20L, 160),
    HEAVY("Subwoofer Potente", 28L, 255)
}

class BassHapticManager private constructor() {

    companion object {
        private const val TAG = "BassHapticManager"
        val instance: BassHapticManager by lazy { BassHapticManager() }
    }

    private var vibrator: Vibrator? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var hapticJob: Job? = null

    private val _intensity = MutableStateFlow(HapticIntensity.OFF)
    val intensity: StateFlow<HapticIntensity> = _intensity.asStateFlow()

    fun init(context: Context) {
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
    }

    fun setIntensity(newIntensity: HapticIntensity) {
        _intensity.value = newIntensity
        if (newIntensity == HapticIntensity.OFF) {
            stopLoop()
        } else {
            startLoop()
        }
    }

    fun toggle() {
        val next = when (_intensity.value) {
            HapticIntensity.OFF -> HapticIntensity.MEDIUM
            HapticIntensity.LIGHT -> HapticIntensity.MEDIUM
            HapticIntensity.MEDIUM -> HapticIntensity.HEAVY
            HapticIntensity.HEAVY -> HapticIntensity.OFF
        }
        setIntensity(next)
    }

    private fun startLoop() {
        if (hapticJob?.isActive == true) return

        hapticJob = scope.launch {
            var lastHitTime = 0L
            val vib = vibrator ?: return@launch

            while (isActive && _intensity.value != HapticIntensity.OFF) {
                val isPlaying = com.auraplayer.service.PlaybackService.activePlayer?.isPlaying == true
                if (!isPlaying) {
                    delay(150)
                    continue
                }

                val current = _intensity.value
                val now = System.currentTimeMillis()
                val bass = RealtimeVisualizerManager.instance.getBassEnergy(true)

                // Trigger punch when sub-bass drops (> 0.62f threshold) and at least 110ms spaced
                if (bass > 0.62f && (now - lastHitTime) > 115L) {
                    lastHitTime = now
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (vib.hasAmplitudeControl()) {
                                vib.vibrate(VibrationEffect.createOneShot(current.ms, current.amplitude))
                            } else {
                                vib.vibrate(VibrationEffect.createOneShot(current.ms, VibrationEffect.DEFAULT_AMPLITUDE))
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            vib.vibrate(current.ms)
                        }
                    } catch (e: Exception) {
                        AppLog.w(TAG, "Vibración de bajo háptico falló", e)
                    }
                }
                delay(30)
            }
        }
    }

    private fun stopLoop() {
        hapticJob?.cancel()
        hapticJob = null
    }
}
