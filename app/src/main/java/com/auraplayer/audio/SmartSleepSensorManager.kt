package com.auraplayer.audio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.auraplayer.util.AppLog
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detector de Sueño Inteligente por Acelerómetro.
 * Detecta cuando el usuario se queda dormido por inmovilidad del teléfono en la cama o mesa.
 */
class SmartSleepSensorManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var isMonitoring = false
        private set

    private var lastMovementTimestamp = System.currentTimeMillis()
    private var inactivityThresholdMs = 15 * 60 * 1000L // 15 minutos por defecto
    private var onSleepDetected: (() -> Unit)? = null

    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    fun startMonitoring(
        minutesInactivity: Int = 15,
        onSleep: () -> Unit
    ): Boolean {
        if (accelerometer == null) return false
        inactivityThresholdMs = (minutesInactivity * 60 * 1000L).coerceAtLeast(60000L)
        onSleepDetected = onSleep
        lastMovementTimestamp = System.currentTimeMillis()

        val registered = sensorManager?.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        ) ?: false

        isMonitoring = registered
        AppLog.d("SmartSleepSensor", "Monitoreo de sueño por acelerómetro iniciado ($minutesInactivity min)")
        return registered
    }

    fun stopMonitoring() {
        if (!isMonitoring) return
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
        isMonitoring = false
        AppLog.d("SmartSleepSensor", "Monitoreo de sueño detenido")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isMonitoring) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val deltaX = abs(x - lastX)
        val deltaY = abs(y - lastY)
        val deltaZ = abs(z - lastZ)

        lastX = x
        lastY = y
        lastZ = z

        val totalMovement = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()

        val now = System.currentTimeMillis()
        if (totalMovement > 0.45f) {
            // El usuario movió el teléfono, se reinicia el contador de inactividad
            lastMovementTimestamp = now
        } else {
            // Comprobar si ya pasó el tiempo de inactividad
            if (now - lastMovementTimestamp >= inactivityThresholdMs) {
                AppLog.i("SmartSleepSensor", "Inactividad confirmada: Sueño detectado. Apagando música.")
                stopMonitoring()
                onSleepDetected?.invoke()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
