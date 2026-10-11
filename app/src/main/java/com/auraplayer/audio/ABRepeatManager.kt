package com.auraplayer.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ABRepeatManager private constructor() {

    companion object {
        val instance: ABRepeatManager by lazy { ABRepeatManager() }
    }

    private val _pointA = MutableStateFlow<Long?>(null)
    val pointA: StateFlow<Long?> = _pointA.asStateFlow()

    private val _pointB = MutableStateFlow<Long?>(null)
    val pointB: StateFlow<Long?> = _pointB.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun setPointA(positionMs: Long) {
        _pointA.value = positionMs
        // If B is behind A, reset B
        if (_pointB.value != null && _pointB.value!! <= positionMs) {
            _pointB.value = null
            _isActive.value = false
        }
    }

    fun setPointB(positionMs: Long) {
        val a = _pointA.value
        if (a != null && positionMs > a) {
            _pointB.value = positionMs
            _isActive.value = true
        }
    }

    fun toggleActive() {
        if (_pointA.value != null && _pointB.value != null) {
            _isActive.value = !_isActive.value
        }
    }

    fun clear() {
        _pointA.value = null
        _pointB.value = null
        _isActive.value = false
    }

    fun checkPositionAndLoop(currentPositionMs: Long, onSeekToA: (Long) -> Unit) {
        if (!_isActive.value) return
        val a = _pointA.value ?: return
        val b = _pointB.value ?: return

        if (currentPositionMs >= b) {
            onSeekToA(a)
        }
    }

    fun formatTime(ms: Long?): String {
        if (ms == null) return "--:--"
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%02d:%02d", m, s)
    }
}
