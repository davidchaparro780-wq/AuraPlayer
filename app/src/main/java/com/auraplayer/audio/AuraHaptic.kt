package com.auraplayer.audio

import android.view.HapticFeedbackConstants
import android.view.View

object AuraHaptic {
    private fun isHapticEnabled(): Boolean {
        return com.auraplayer.data.repository.SettingsManager.instance?.hapticFeedbackEnabled ?: true
    }

    fun tick(view: View?) {
        if (!isHapticEnabled()) return
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Exception) {}
    }

    fun click(view: View?) {
        if (!isHapticEnabled()) return
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (_: Exception) {}
    }

    fun heavy(view: View?) {
        if (!isHapticEnabled()) return
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
    }

    fun heavyClick(view: View?) = heavy(view)
}
