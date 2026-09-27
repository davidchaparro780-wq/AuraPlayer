package com.auraplayer.audio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class AudioDeviceModel(
    val id: Int,
    val name: String,
    val typeName: String,
    val isBluetooth: Boolean,
    val isWired: Boolean,
    val isUsb: Boolean,
    val isSpeaker: Boolean,
    val batteryPercent: Int?,
    val sampleRatesText: String,
    val channelText: String,
    val recommendedPresetIndex: Int?,
    val recommendedPresetName: String?
)

class HeadphoneManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default)

    var currentDevice by mutableStateOf<AudioDeviceModel?>(null)
        private set

    var isHeadphonesConnected by mutableStateOf(false)
        private set

    var currentVolume by mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
        private set

    val maxVolume: Int
        get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var broadcastReceiver: BroadcastReceiver? = null
    private var isListening = false

    fun startListening() {
        if (isListening) return
        isListening = true

        refreshAudioDevices()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    mainHandler.post { refreshAudioDevices() }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    mainHandler.post { refreshAudioDevices() }
                }
            }
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
        }

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_HEADSET_PLUG,
                    BluetoothDevice.ACTION_ACL_CONNECTED,
                    BluetoothDevice.ACTION_ACL_DISCONNECTED,
                    "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED",
                    "android.media.VOLUME_CHANGED_ACTION" -> {
                        mainHandler.post {
                            refreshAudioDevices()
                            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction("android.media.VOLUME_CHANGED_ACTION")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(broadcastReceiver, filter)
            }
        } catch (_: Exception) {}
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
            audioDeviceCallback = null
        }

        broadcastReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
            broadcastReceiver = null
        }
    }

    fun refreshAudioDevices() {
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            // Priority: Bluetooth -> Wired -> USB -> Builtin Speaker
            val bestDevice = devices.firstOrNull { it.isBluetooth() }
                ?: devices.firstOrNull { it.isWired() }
                ?: devices.firstOrNull { it.isUsb() }
                ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                ?: devices.firstOrNull()

            if (bestDevice != null) {
                val isBt = bestDevice.isBluetooth()
                val isW = bestDevice.isWired()
                val isU = bestDevice.isUsb()
                val isSpk = bestDevice.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && bestDevice.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE)

                val rawName = bestDevice.productName.toString().trim()
                val name = if (rawName.isEmpty() || rawName.equals("Speaker", true) || rawName.equals("Default", true)) {
                    when {
                        isBt -> "Auriculares Bluetooth"
                        isW -> "Auriculares con cable 3.5mm"
                        isU -> "Audio USB-C (DAC)"
                        isSpk -> "Altavoz del Teléfono"
                        else -> "Dispositivo de Audio"
                    }
                } else rawName

                val typeName = when {
                    isBt -> "Bluetooth A2DP / LE Audio"
                    isW -> "Conector Jack 3.5mm"
                    isU -> "Audio Digital USB-C"
                    isSpk -> "Altavoz Estéreo Integrado"
                    else -> "Salida de Audio"
                }

                val battery = if (isBt) queryBluetoothBattery(name) else null

                val sampleRates = bestDevice.sampleRates
                val ratesText = if (sampleRates.isNotEmpty()) {
                    sampleRates.joinToString(", ") { "${it / 1000} kHz" }
                } else "48 kHz Hi-Fi"

                val channels = bestDevice.channelCounts
                val channelText = if (channels.isNotEmpty() && (channels.maxOrNull() ?: 1) > 1) "Estéreo 2.0" else "Mono / Estéreo"

                val (presetIdx, presetName) = matchPreset(name, isW)

                currentDevice = AudioDeviceModel(
                    id = bestDevice.id,
                    name = name,
                    typeName = typeName,
                    isBluetooth = isBt,
                    isWired = isW,
                    isUsb = isU,
                    isSpeaker = isSpk,
                    batteryPercent = battery,
                    sampleRatesText = ratesText,
                    channelText = channelText,
                    recommendedPresetIndex = presetIdx,
                    recommendedPresetName = presetName
                )
                isHeadphonesConnected = isBt || isW || isU
                return
            }
        }

        // Fallback for older devices or empty device list
        @Suppress("DEPRECATION")
        val isWired = audioManager.isWiredHeadsetOn
        @Suppress("DEPRECATION")
        val isBt = audioManager.isBluetoothA2dpOn
        val name = when {
            isBt -> "Auriculares Bluetooth"
            isWired -> "Auriculares con cable"
            else -> "Altavoz del Teléfono"
        }
        val (presetIdx, presetName) = matchPreset(name, isWired)
        currentDevice = AudioDeviceModel(
            id = 0,
            name = name,
            typeName = if (isBt) "Bluetooth A2DP" else if (isWired) "Cable Jack 3.5mm" else "Altavoz Integrado",
            isBluetooth = isBt,
            isWired = isWired,
            isUsb = false,
            isSpeaker = !isBt && !isWired,
            batteryPercent = null,
            sampleRatesText = "48 kHz Hi-Fi",
            channelText = "Estéreo 2.0",
            recommendedPresetIndex = presetIdx,
            recommendedPresetName = presetName
        )
        isHeadphonesConnected = isBt || isWired
    }

    private fun AudioDeviceInfo.isBluetooth(): Boolean {
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
               type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
               (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET) ||
               (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
    }

    private fun AudioDeviceInfo.isWired(): Boolean {
        return type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
    }

    private fun AudioDeviceInfo.isUsb(): Boolean {
        return type == AudioDeviceInfo.TYPE_USB_DEVICE || type == AudioDeviceInfo.TYPE_USB_HEADSET
    }

    private fun queryBluetoothBattery(deviceName: String): Int? {
        try {
            @Suppress("DEPRECATION")
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return null
            val bonded = adapter.bondedDevices ?: return null
            for (dev in bonded) {
                val devName = try { dev.name } catch (_: SecurityException) { null }
                if (devName != null && (devName.equals(deviceName, true) || deviceName.contains(devName, true) || devName.contains(deviceName, true))) {
                    try {
                        val batteryMethod = dev.javaClass.getMethod("getBatteryLevel")
                        val level = batteryMethod.invoke(dev) as? Int
                        if (level != null && level in 0..100) {
                            return level
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun matchPreset(name: String, isWired: Boolean): Pair<Int?, String?> {
        val lower = name.lowercase()
        return when {
            lower.contains("airpod") -> Pair(8, "AirPods / AirPods Pro")
            lower.contains("sony") || lower.contains("wh-1000") || lower.contains("wf-1000") -> Pair(9, "Sony WH/WF-1000XM4 & XM5")
            lower.contains("galaxy") || lower.contains("buds") -> Pair(10, "Samsung Galaxy Buds")
            lower.contains("jbl") -> Pair(11, "JBL Tune Series")
            lower.contains("kz") || lower.contains("iem") || lower.contains("moondrop") || isWired -> Pair(12, "Monitores In-Ear (KZ / IEM Studio)")
            else -> Pair(null, null)
        }
    }

    fun setVolume(volume: Int) {
        val clamped = volume.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        currentVolume = clamped
    }

    // Play Stereo Test Tone: 523.25 Hz Left or 659.25 Hz Right only
    fun playTestTone(isLeft: Boolean) {
        scope.launch {
            try {
                val sampleRate = 44100
                val durationSeconds = 0.7
                val numSamples = (sampleRate * durationSeconds).toInt()
                val buffer = ShortArray(numSamples * 2)

                val freq = if (isLeft) 523.25 else 659.25
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = 0.5 * (1 - cos(2 * PI * i / numSamples))
                    val sample = (sin(2 * PI * freq * t) * envelope * Short.MAX_VALUE * 0.75).toInt().toShort()

                    if (isLeft) {
                        buffer[i * 2] = sample
                        buffer[i * 2 + 1] = 0
                    } else {
                        buffer[i * 2] = 0
                        buffer[i * 2 + 1] = sample
                    }
                }

                val bufferSizeBytes = buffer.size * 2
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSizeBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                delay((durationSeconds * 1000).toLong() + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
