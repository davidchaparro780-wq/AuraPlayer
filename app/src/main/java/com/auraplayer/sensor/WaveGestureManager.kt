package com.auraplayer.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * WaveGestureManager enables Wave Control: passing a hand over the phone's
 * proximity sensor (without touching the screen) to skip to the next track.
 */
class WaveGestureManager(
    private val context: Context,
    private val onWaveDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var isNear = false
    private var lastWaveTime = 0L
    private val prefs = context.getSharedPreferences("aura_gesture_settings", Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean("wave_enabled", false)
        set(value) {
            prefs.edit().putBoolean("wave_enabled", value).apply()
            if (value) start() else stop()
        }

    fun start() {
        if (!isEnabled || proximitySensor == null) return
        sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || proximitySensor == null) return
        val distance = event.values.firstOrNull() ?: return
        val maxRange = proximitySensor.maximumRange

        // Proximity sensor: values < 5cm or < maxRange/2 indicate hand is hovering over phone
        val near = distance < (maxRange.coerceAtMost(5.0f))

        if (near) {
            isNear = true
        } else if (isNear) {
            // Hand moved away -> wave gesture completed
            isNear = false
            val now = System.currentTimeMillis()
            if (now - lastWaveTime > 900L) { // 900ms debounce
                lastWaveTime = now
                onWaveDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
