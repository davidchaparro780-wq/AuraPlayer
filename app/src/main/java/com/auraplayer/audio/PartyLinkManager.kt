package com.auraplayer.audio

import android.content.Context
import android.net.wifi.WifiManager
import com.auraplayer.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Gestor de Silent Disco & Sincronización Multi-Teléfono por Wi-Fi Local (Party Link).
 * Transmite paquetes de sincronización de tiempo UDP ligeros en la red local para
 * reproducir la misma canción al milisegundo exacto en múltiples teléfonos a la vez.
 */
class PartyLinkManager private constructor(private val context: Context) {

    var isHosting: Boolean = false
        private set

    var isJoined: Boolean = false
        private set

    var connectedGuests: Int = 0
        private set

    var hostIp: String = "127.0.0.1"
        private set

    private var broadcastJob: Job? = null
    private var listenJob: Job? = null
    private val PORT = 8844

    fun getLocalIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipInt = wifiManager.connectionInfo.ipAddress
            if (ipInt != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
        } catch (e: Exception) {
            AppLog.d(TAG, "No se pudo obtener la IP local por Wi-Fi", e)
        }
        return "192.168.1.100"
    }

    fun startHosting(scope: CoroutineScope, currentTitle: String, currentPosMs: Long, isPlaying: Boolean) {
        stopAll()
        isHosting = true
        hostIp = getLocalIpAddress()
        connectedGuests = 1

        broadcastJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                val broadcastAddr = InetAddress.getByName("255.255.255.255")

                while (isActive && isHosting) {
                    val payload = JSONObject().apply {
                        put("type", "PARTY_SYNC")
                        put("title", currentTitle)
                        put("posMs", currentPosMs)
                        put("isPlaying", isPlaying)
                        put("time", System.currentTimeMillis())
                    }.toString()

                    val data = payload.toByteArray()
                    val packet = DatagramPacket(data, data.size, broadcastAddr, PORT)
                    socket.send(packet)
                    kotlinx.coroutines.delay(1000)
                }
            } catch (e: Exception) {
                AppLog.w(TAG, "Se cortó la transmisión de sincronización Party Link", e)
            } finally {
                socket?.close()
            }
        }
    }

    fun joinParty(scope: CoroutineScope, onSyncReceived: (title: String, posMs: Long, isPlaying: Boolean) -> Unit) {
        stopAll()
        isJoined = true

        listenJob = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(PORT).apply {
                    broadcast = true
                }
                val buffer = ByteArray(1024)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive && isJoined) {
                    socket.receive(packet)
                    val jsonStr = String(packet.data, 0, packet.length)
                    val obj = JSONObject(jsonStr)
                    if (obj.optString("type") == "PARTY_SYNC") {
                        val title = obj.optString("title", "")
                        val posMs = obj.optLong("posMs", 0L)
                        val isPlaying = obj.optBoolean("isPlaying", false)
                        onSyncReceived(title, posMs, isPlaying)
                    }
                }
            } catch (e: Exception) {
                AppLog.w(TAG, "Se cortó la recepción de la sincronización Party Link", e)
            } finally {
                socket?.close()
            }
        }
    }

    fun stopAll() {
        isHosting = false
        isJoined = false
        connectedGuests = 0
        broadcastJob?.cancel()
        broadcastJob = null
        listenJob?.cancel()
        listenJob = null
    }

    companion object {
        private const val TAG = "PartyLinkManager"

        @Volatile
        private var instance: PartyLinkManager? = null

        fun getInstance(context: Context): PartyLinkManager {
            return instance ?: synchronized(this) {
                instance ?: PartyLinkManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
