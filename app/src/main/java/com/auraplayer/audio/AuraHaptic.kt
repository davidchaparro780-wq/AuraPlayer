package com.auraplayer.audio

import android.view.HapticFeedbackConstants
import android.view.View

object AuraHaptic {
    fun tick(view: View?) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Exception) {}
    }

    fun click(view: View?) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (_: Exception) {}
    }

    fun heavy(view: View?) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } catch (_: Exception) {}
    }
}
