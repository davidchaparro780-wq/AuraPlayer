package com.auraplayer.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SleepTimerManager {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isTimerActive = MutableStateFlow(false)
    val isTimerActive: StateFlow<Boolean> = _isTimerActive.asStateFlow()

    private val _isEndOfTrackActive = MutableStateFlow(false)
    val isEndOfTrackActive: StateFlow<Boolean> = _isEndOfTrackActive.asStateFlow()

    private var endOfTrackCallback: (() -> Unit)? = null

    private val _fadeVolumeFactor = MutableStateFlow(1.0f)
    val fadeVolumeFactor: StateFlow<Float> = _fadeVolumeFactor.asStateFlow()

    companion object {
        @Volatile
        var globalFadeFactor: Float = 1.0f
    }

    fun startTimer(minutes: Int, onFinish: () -> Unit) {
        cancelTimer()
        _remainingSeconds.value = minutes * 60
        _fadeVolumeFactor.value = 1.0f
        globalFadeFactor = 1.0f
        _isTimerActive.value = true

        timerJob = scope.launch {
            while (isActive && _remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value -= 1

                // Zen Sleep Fade-Out in the last 120 seconds
                if (_remainingSeconds.value in 1..120) {
                    val factor = (_remainingSeconds.value.toFloat() / 120f).coerceIn(0.05f, 1.0f)
                    _fadeVolumeFactor.value = factor
                    globalFadeFactor = factor
                }
            }
            if (_remainingSeconds.value <= 0) {
                _fadeVolumeFactor.value = 1.0f
                globalFadeFactor = 1.0f
                _isTimerActive.value = false
                onFinish()
            }
        }
    }

    fun startEndOfTrackTimer(onFinish: () -> Unit) {
        cancelTimer()
        _isEndOfTrackActive.value = true
        endOfTrackCallback = onFinish
    }

    fun onTrackEnded() {
        if (_isEndOfTrackActive.value) {
            _isEndOfTrackActive.value = false
            val cb = endOfTrackCallback
            endOfTrackCallback = null
            cb?.invoke()
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = 0
        _fadeVolumeFactor.value = 1.0f
        globalFadeFactor = 1.0f
        _isTimerActive.value = false
        _isEndOfTrackActive.value = false
        endOfTrackCallback = null
    }

    fun getFormattedTime(): String {
        if (_isEndOfTrackActive.value) return "Fin de canción"
        val totalSecs = _remainingSeconds.value
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
