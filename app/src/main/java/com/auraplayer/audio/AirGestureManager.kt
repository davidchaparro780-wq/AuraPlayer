package com.auraplayer.audio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AirGestureManager(
    private val context: Context,
    private val onNext: () -> Unit,
    private val onPlayPause: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    var isEnabled: Boolean = false
        private set

    private var nearStartTime = 0L
    private var hoverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun start() {
        if (isEnabled || proximitySensor == null) return
        isEnabled = true
        sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        if (!isEnabled) return
        isEnabled = false
        hoverJob?.cancel()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isEnabled || event == null) return

        val distance = event.values[0]
        val maxRange = proximitySensor?.maximumRange ?: 5f
        val isNear = distance < maxRange

        if (isNear) {
            nearStartTime = System.currentTimeMillis()
            // Schedule hover check (holding hand over sensor for 1.2s toggles Play/Pause)
            hoverJob?.cancel()
            hoverJob = scope.launch {
                delay(1100)
                // If still near after 1.1s, it's a hover -> toggle play/pause
                onPlayPause()
                nearStartTime = 0L // reset so releasing doesn't trigger swipe
            }
        } else {
            // Hand moved away
            hoverJob?.cancel()
            if (nearStartTime > 0) {
                val duration = System.currentTimeMillis() - nearStartTime
                if (duration in 80..600) {
                    // Quick wave/swipe -> Next track
                    onNext()
                }
                nearStartTime = 0L
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
