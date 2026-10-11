package com.auraplayer.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.auraplayer.util.AppLog

private const val TAG = "HapticBassManager"

class HapticBassManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val prefs = context.getSharedPreferences("aura_haptic_bass", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("haptic_bass_enabled", false)
        set(value) = prefs.edit().putBoolean("haptic_bass_enabled", value).apply()

    fun triggerBassPulse() {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Short, punchy haptic tap (35ms, medium amplitude)
                val effect = VibrationEffect.createOneShot(35, 160)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "Fallo al emitir el pulso háptico de graves", e)
        }
    }
}
