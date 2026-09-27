package com.auraplayer.audio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AbLooperManager private constructor() {

    companion object {
        val instance: AbLooperManager by lazy { AbLooperManager() }
    }

    var isEnabled by mutableStateOf(false)
        private set

    var pointAMs by mutableStateOf<Long?>(null)
        private set

    var pointBMs by mutableStateOf<Long?>(null)
        private set

    fun setPointA(posMs: Long) {
        pointAMs = posMs
        if (pointBMs != null && pointBMs!! <= posMs) {
            pointBMs = null
        }
        isEnabled = pointAMs != null && pointBMs != null
    }

    fun setPointB(posMs: Long) {
        val a = pointAMs
        if (a != null && posMs > a) {
            pointBMs = posMs
            isEnabled = true
        }
    }

    fun clear() {
        pointAMs = null
        pointBMs = null
        isEnabled = false
    }

    fun toggle() {
        if (pointAMs != null && pointBMs != null) {
            isEnabled = !isEnabled
        }
    }

    fun checkAndLoop(currentPosMs: Long, seekTo: (Long) -> Unit) {
        if (!isEnabled) return
        val a = pointAMs ?: return
        val b = pointBMs ?: return

        if (currentPosMs >= b || currentPosMs < a) {
            seekTo(a)
        }
    }
}
