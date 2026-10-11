package com.auraplayer.audio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import kotlin.math.abs
import kotlin.math.sqrt

class SleepTimerManager {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isTimerActive = MutableStateFlow(false)
    val isTimerActive: StateFlow<Boolean> = _isTimerActive.asStateFlow()

    private val _isEndOfTrackActive = MutableStateFlow(false)
    val isEndOfTrackActive: StateFlow<Boolean> = _isEndOfTrackActive.asStateFlow()

    private val _isSmartSensorActive = MutableStateFlow(false)
    val isSmartSensorActive: StateFlow<Boolean> = _isSmartSensorActive.asStateFlow()

    private var endOfTrackCallback: (() -> Unit)? = null
    private var sensorManager: SensorManager? = null
    private var sensorListener: SensorEventListener? = null
    private var lastMovementTime: Long = 0L

    private val _fadeVolumeFactor = MutableStateFlow(1.0f)
    val fadeVolumeFactor: StateFlow<Float> = _fadeVolumeFactor.asStateFlow()

    companion object {
        private const val TAG = "SleepTimerManager"
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

    fun startSmartSensorTimer(context: Context, idleMinutes: Int, onFinish: () -> Unit) {
        cancelTimer()
        _isSmartSensorActive.value = true
        _isTimerActive.value = true
        _remainingSeconds.value = idleMinutes * 60
        lastMovementTime = System.currentTimeMillis()

        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            var lastX = 0f
            var lastY = 0f
            var lastZ = 0f

            sensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null) return
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val delta = abs(x - lastX) + abs(y - lastY) + abs(z - lastZ)
                    lastX = x
                    lastY = y
                    lastZ = z

                    // Significant motion detected -> user is still awake/moving
                    if (delta > 0.45f) {
                        lastMovementTime = System.currentTimeMillis()
                        if (_fadeVolumeFactor.value < 1.0f) {
                            // Wake up / restore volume if moved during fade-out
                            _fadeVolumeFactor.value = 1.0f
                            globalFadeFactor = 1.0f
                        }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager?.registerListener(sensorListener, accel, SensorManager.SENSOR_DELAY_UI)
        } catch (e: Exception) {
            AppLog.w(TAG, "Sensor de movimiento no disponible", e)
        }

        val idleThresholdMs = idleMinutes * 60 * 1000L
        timerJob = scope.launch {
            while (isActive && _isSmartSensorActive.value) {
                delay(1000)
                val stillDuration = System.currentTimeMillis() - lastMovementTime
                val remainingMs = (idleThresholdMs - stillDuration).coerceAtLeast(0L)
                _remainingSeconds.value = (remainingMs / 1000).toInt()

                // If still for long enough, enter 45-second gradual fade out
                if (remainingMs in 1..45000L) {
                    val factor = (remainingMs.toFloat() / 45000f).coerceIn(0.05f, 1.0f)
                    _fadeVolumeFactor.value = factor
                    globalFadeFactor = factor
                } else if (remainingMs <= 0L) {
                    _fadeVolumeFactor.value = 1.0f
                    globalFadeFactor = 1.0f
                    cancelTimer()
                    onFinish()
                    break
                }
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
        try {
            sensorListener?.let { sensorManager?.unregisterListener(it) }
        } catch (_: Exception) {}
        sensorListener = null
        sensorManager = null

        _remainingSeconds.value = 0
        _fadeVolumeFactor.value = 1.0f
        globalFadeFactor = 1.0f
        _isTimerActive.value = false
        _isEndOfTrackActive.value = false
        _isSmartSensorActive.value = false
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
