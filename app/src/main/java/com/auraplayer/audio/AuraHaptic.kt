package com.auraplayer.audio

import android.view.HapticFeedbackConstants
import android.view.View
import com.auraplayer.util.AppLog

object AuraHaptic {
    private const val TAG = "AuraHaptic"

    private fun isHapticEnabled(): Boolean {
        return com.auraplayer.data.repository.SettingsManager.instance?.hapticFeedbackEnabled ?: true
    }

    fun tick(view: View?) {
        if (!isHapticEnabled()) return
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (e: Exception) {
            AppLog.w(TAG, "Fallo al ejecutar la vibración tick en la vista", e)
        }
    }

    fun click(view: View?) {
        if (!isHapticEnabled()) return
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (e: Exception) {
            AppLog.w(TAG, "Fallo al ejecutar la vibración de clic en la vista", e)
        }
    }

    fun heavy(view: View?) {
        if (!isHapticEnabled()) return
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (e: Exception) {
            AppLog.w(TAG, "Fallo al ejecutar la vibración larga en la vista", e)
        }
    }

    fun heavyClick(view: View?) = heavy(view)
}
