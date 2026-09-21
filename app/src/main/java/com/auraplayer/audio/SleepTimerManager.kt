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

    fun startTimer(minutes: Int, onFinish: () -> Unit) {
        cancelTimer()
        _remainingSeconds.value = minutes * 60
        _isTimerActive.value = true

        timerJob = scope.launch {
            while (isActive && _remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value -= 1
            }
            if (_remainingSeconds.value <= 0) {
                _isTimerActive.value = false
                onFinish()
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = 0
        _isTimerActive.value = false
    }

    fun getFormattedTime(): String {
        val totalSecs = _remainingSeconds.value
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%02d:%02d", mins, secs)
    }
}
