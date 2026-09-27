package com.auraplayer.audio

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FlashlightBeatManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var strobeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    var isEnabled: Boolean = false
        private set

    private fun getCameraIdWithFlash(): String? {
        val cm = cameraManager ?: return null
        return try {
            cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cm.cameraIdList.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun start() {
        if (isEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val camId = getCameraIdWithFlash() ?: return
        isEnabled = true

        strobeJob = scope.launch {
            try {
                while (isActive && isEnabled) {
                    // Flash ON (strobe peak: 45ms)
                    cameraManager?.setTorchMode(camId, true)
                    delay(45)
                    // Flash OFF
                    cameraManager?.setTorchMode(camId, false)
                    // Gap between beats (approx 125 BPM = 480ms period)
                    delay(435)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    cameraManager?.setTorchMode(camId, false)
                } catch (_: Exception) {}
            }
        }
    }

    fun stop() {
        if (!isEnabled) return
        isEnabled = false
        strobeJob?.cancel()
        strobeJob = null
        val camId = getCameraIdWithFlash()
        if (camId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                cameraManager?.setTorchMode(camId, false)
            } catch (_: Exception) {}
        }
    }

    fun toggle(): Boolean {
        if (isEnabled) stop() else start()
        return isEnabled
    }
}
