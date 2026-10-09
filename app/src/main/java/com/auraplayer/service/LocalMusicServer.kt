package com.auraplayer.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.Locale

data class ServerController(
    val getCurrentSong: () -> MediaModel?,
    val isPlaying: () -> Boolean,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrev: () -> Unit,
    val onPlaySongById: (Long) -> Unit
)

class LocalMusicServer {

    private var serverSocket: ServerSocket? = null
    private var serverScope: CoroutineScope? = null
    private var songList: List<MediaModel> = emptyList()
    private var controller: ServerController? = null
    private var appContext: Context? = null
    val port = 8080

    @Volatile
    var isRunning = false
        private set

    fun start(songs: List<MediaModel>, ctrl: ServerController, context: Context? = null) {
        if (isRunning) return
        songList = songs
        controller = ctrl
        appContext = context?.applicationContext

        serverScope?.cancel()
        serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        serverScope?.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
                isRunning = true
                Log.d("LocalMusicServer", "DaVE WiFi Server running on port $port")
                while (isActive && isRunning) {
                    val client = try {
                        serverSocket?.accept() ?: break
                    } catch (_: Exception) {
                        break
                    }
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                Log.e("LocalMusicServer", "Server socket error: ${e.message}", e)
            } finally {
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverScope?.cancel()
        serverScope = null
        serverSocket = null
    }

    fun getLocalIpAddress(context: Context): String {
        return getAllIpAddresses(context).firstOrNull() ?: "127.0.0.1"
    }

    fun getAllIpAddresses(context: Context): List<String> {
        val result = mutableListOf<String>()

        // 1. Try ConnectivityManager on active network
        try {
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            if (activeNetwork != null) {
                val linkProps = cm.getLinkProperties(activeNetwork)
                linkProps?.linkAddresses?.forEach { linkAddr ->
                    val addr = linkAddr.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank() && host != "127.0.0.1" && !result.contains(host)) {
                            result.add(host)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Try WifiManager
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wm?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                val ip = String.format(
                    Locale.US,
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
                if (ip != "0.0.0.0" && ip != "127.0.0.1" && !result.contains(ip)) {
                    result.add(ip)
                }
            }
        } catch (_: Exception) {}

        // 3. Network Interfaces iteration
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

            // Prioritize Wi-Fi and Hotspot interfaces
            val prioritized = interfaces.filter { iface ->
                val n = iface.name.lowercase(Locale.ROOT)
                n.startsWith("wlan") || n.startsWith("ap") || n.startsWith("softap") || n.startsWith("rndis") || n.startsWith("eth")
            }

            for (iface in prioritized) {
                if (iface.isUp && !iface.isLoopback) {
                    for (addr in Collections.list(iface.inetAddresses)) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val host = addr.hostAddress ?: continue
                            if (!result.contains(host)) {
                                result.add(host)
                            }
                        }
                    }
                }
            }

            // Fallback: other interfaces excluding cellular if possible
            for (iface in interfaces) {
                val n = iface.name.lowercase(Locale.ROOT)
                if (n.startsWith("rmnet") || n.startsWith("dummy") || n.startsWith("tun") || n.startsWith("p2p")) continue
                if (iface.isUp && !iface.isLoopback) {
                    for (addr in Collections.list(iface.inetAddresses)) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val host = addr.hostAddress ?: continue
                            if (!result.contains(host)) {
                                result.add(host)
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Order results so 192.168.x.x comes first, then 10.x.x.x, then others
        result.sortWith(Comparator { a, b ->
            when {
                a.startsWith("192.168.") && !b.startsWith("192.168.") -> -1
                !a.startsWith("192.168.") && b.startsWith("192.168.") -> 1
                a.startsWith("10.") && !b.startsWith("10.") -> -1
                !a.startsWith("10.") && b.startsWith("10.") -> 1
                else -> a.compareTo(b)
            }
        })

        return if (result.isNotEmpty()) result else listOf("127.0.0.1")
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 8000
            val inputStream = socket.getInputStream()
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

            val requestLine = reader.readLine() ?: return

            // Consume all remaining headers to avoid TCP RST on close
            var headerLine: String? = reader.readLine()
            while (!headerLine.isNullOrEmpty()) {
                headerLine = reader.readLine()
            }

            val parts = requestLine.split(" ")
            val method = parts.getOrNull(0) ?: "GET"
            val rawPath = parts.getOrNull(1) ?: "/"
            val path = rawPath.substringBefore("?")

            if (method.equals("OPTIONS", ignoreCase = true)) {
                sendOptionsResponse(socket)
                return
            }

            when {
                path == "/" || path == "/index.html" -> serveIndex(socket)
                path == "/party" -> serveParty(socket)
                path == "/favicon.ico" -> sendNoContent(socket)
                path.startsWith("/api/songs") -> serveSongsApi(socket)
                path.startsWith("/api/status") -> serveStatusApi(socket)
                path.startsWith("/api/playpause") -> {
                    controller?.onPlayPause()
                    serveJsonSuccess(socket, "toggled")
                }
                path.startsWith("/api/next") -> {
                    controller?.onNext()
                    serveJsonSuccess(socket, "next")
                }
                path.startsWith("/api/prev") -> {
                    controller?.onPrev()
                    serveJsonSuccess(socket, "prev")
                }
                path.startsWith("/api/playid/") -> {
                    val id = path.removePrefix("/api/playid/").toLongOrNull()
                    if (id != null) controller?.onPlaySongById(id)
                    serveJsonSuccess(socket, "playing_$id")
                }
                path == "/stream/current" -> {
                    val song = controller?.getCurrentSong()
                    if (song != null) streamSong(socket, song)
                    else serve404(socket)
                }
                path.startsWith("/stream") -> {
                    val id = if (rawPath.contains("id=")) rawPath.substringAfter("id=").substringBefore("&").toLongOrNull()
                             else path.removePrefix("/stream/").removePrefix("/stream").toLongOrNull()
                    val song = songList.firstOrNull { it.id == id }
                    if (song != null) streamSong(socket, song)
                    else serve404(socket)
                }
                else -> serve404(socket)
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun sendResponse(
        socket: Socket,
        statusCode: String = "200 OK",
        contentType: String = "text/html; charset=UTF-8",
        body: ByteArray
    ) {
        val headers = "HTTP/1.1 $statusCode\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        val out = socket.getOutputStream()
        out.write(headers.toByteArray(Charsets.ISO_8859_1))
        out.write(body)
        out.flush()
        try { socket.shutdownOutput() } catch (_: Exception) {}
    }

    private fun sendNoContent(socket: Socket) {
        val headers = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        val out = socket.getOutputStream()
        out.write(headers.toByteArray(Charsets.ISO_8859_1))
        out.flush()
        try { socket.shutdownOutput() } catch (_: Exception) {}
    }

    private fun sendOptionsResponse(socket: Socket) {
        val headers = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type\r\n" +
                "Connection: close\r\n\r\n"
        val out = socket.getOutputStream()
        out.write(headers.toByteArray(Charsets.ISO_8859_1))
        out.flush()
        try { socket.shutdownOutput() } catch (_: Exception) {}
    }

    private fun serveSongsApi(socket: Socket) {
        val array = org.json.JSONArray()
        songList.forEach { song ->
            val obj = JSONObject().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("album", song.album)
                put("duration", song.duration)
            }
            array.put(obj)
        }
        sendResponse(socket, "200 OK", "application/json; charset=UTF-8", array.toString().toByteArray(Charsets.UTF_8))
    }

    private fun serveStatusApi(socket: Socket) {
        val current = controller?.getCurrentSong()
        val playing = controller?.isPlaying() ?: false

        val json = JSONObject().apply {
            put("isPlaying", playing)
            put("title", current?.title ?: "Sin reproducción")
            put("artist", current?.artist ?: "DaVE Player")
            put("album", current?.album ?: "")
            put("id", current?.id ?: -1)
        }.toString()

        sendResponse(socket, "200 OK", "application/json", json.toByteArray(Charsets.UTF_8))
    }

    private fun serveJsonSuccess(socket: Socket, msg: String) {
        val json = JSONObject().put("status", "ok").put("message", msg).toString()
        sendResponse(socket, "200 OK", "application/json", json.toByteArray(Charsets.UTF_8))
    }

    private fun serveIndex(socket: Socket) {
        val rows = songList.joinToString("") { song ->
            "<tr class='song-row' data-name='${escHtml(song.title.lowercase())} ${escHtml(song.artist.lowercase())}'>" +
            "<td><b>${escHtml(song.title)}</b></td>" +
            "<td>${escHtml(song.artist)}</td>" +
            "<td><button class='btn-play' onclick='playSong(${song.id})'>▶ Reproducir</button></td>" +
            "<td><a class='btn-dl' href='/stream/${song.id}' download='${escHtml(song.title)}.mp3'>⬇ Descargar</a></td>" +
            "</tr>"
        }

        val html = """<!DOCTYPE html>
<html lang='es'>
<head>
<meta charset='UTF-8'>
<meta name='viewport' content='width=device-width,initial-scale=1'>
<title>🎵 DaVE Player — Control Remoto WiFi & Cloud</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{background:#0a0e1a;color:#e2e8f0;font-family:system-ui,-apple-system,BlinkMacSystemFont,sans-serif;padding:20px;max-width:960px;margin:0 auto}
header{display:flex;align-items:center;justify-content:space-between;margin-bottom:20px;flex-wrap:wrap;gap:10px}
h1{color:#00f0ff;font-size:24px;display:flex;align-items:center;gap:10px;text-shadow:0 0 15px rgba(0,240,255,0.4)}
.badge{background:#10b981;color:#000;font-size:12px;font-weight:800;padding:4px 10px;border-radius:20px;text-transform:uppercase}
.badge-danger{background:#ef4444;color:#fff}
.badge-cloud{background:#00f0ff;color:#000}
.player-card{background:linear-gradient(135deg,#1e1b4b 0%,#0f172a 100%);border:1px solid #6366f1;border-radius:24px;padding:28px;margin-bottom:24px;text-align:center;box-shadow:0 15px 35px rgba(99,102,241,0.2)}
.now-title{font-size:22px;font-weight:800;color:#fff;margin-bottom:6px;word-break:break-word}
.now-artist{font-size:15px;color:#a5b4fc;margin-bottom:22px}
.controls{display:flex;justify-content:center;align-items:center;gap:16px;flex-wrap:wrap}
.btn-ctrl{background:#312e81;color:#fff;border:none;border-radius:50px;padding:12px 26px;font-size:16px;font-weight:700;cursor:pointer;transition:all 0.2s;box-shadow:0 4px 12px rgba(0,0,0,0.3)}
.btn-ctrl:hover{background:#4338ca;transform:scale(1.05)}
.btn-main{background:#00f0ff;color:#000;padding:15px 36px;font-size:18px;box-shadow:0 0 20px rgba(0,240,255,0.4)}
.btn-main:hover{background:#38bdf8}
.search-container{margin-bottom:14px}
.search-box{width:100%;padding:14px 18px;border-radius:12px;border:1px solid #334155;background:#1e293b;color:#fff;font-size:15px;outline:none;transition:border 0.2s}
.search-box:focus{border-color:#00f0ff}
table{width:100%;border-collapse:collapse;background:#0f172a;border-radius:14px;overflow:hidden;box-shadow:0 8px 20px rgba(0,0,0,0.4)}
th{background:#1e293b;padding:14px;text-align:left;color:#94a3b8;font-size:13px;text-transform:uppercase}
td{padding:12px 14px;border-bottom:1px solid #1e293b;font-size:14px}
tr:hover{background:#1e293b}
.btn-play{background:#059669;color:#fff;border:none;padding:7px 14px;border-radius:8px;cursor:pointer;font-weight:700;transition:background 0.2s}
.btn-play:hover{background:#10b981}
.btn-dl{color:#38bdf8;text-decoration:none;font-weight:600}
.btn-dl:hover{text-decoration:underline}
footer{text-align:center;color:#64748b;font-size:13px;margin-top:30px}

/* Modal Iniciar Sesion */
.modal-overlay{position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(5,7,14,0.92);backdrop-filter:blur(10px);display:none;justify-content:center;align-items:center;z-index:9999;padding:20px}
.modal-card{background:linear-gradient(135deg,#111827 0%,#0f172a 100%);border:1px solid #00f0ff;box-shadow:0 0 45px rgba(0,240,255,0.3);border-radius:22px;max-width:460px;width:100%;padding:28px;text-align:center}
.modal-title{font-size:20px;font-weight:800;color:#fff;margin-bottom:8px}
.modal-alert{background:rgba(239,68,68,0.15);border:1px solid rgba(239,68,68,0.5);border-radius:12px;padding:12px;margin-bottom:18px;text-align:left;color:#fca5a5;font-size:13px;line-height:1.4}
.modal-alert strong{color:#fff;display:block;margin-bottom:3px;font-size:14px}
.form-group{margin-bottom:14px;text-align:left}
.form-group label{display:block;font-size:12px;color:#94a3b8;margin-bottom:5px;font-weight:600}
.form-input{width:100%;padding:12px 14px;border-radius:10px;border:1px solid #334155;background:#1e293b;color:#fff;font-size:14px;outline:none}
.form-input:focus{border-color:#00f0ff}
.btn-submit-login{background:#00f0ff;color:#000;border:none;border-radius:12px;padding:14px;font-size:15px;font-weight:800;width:100%;cursor:pointer;transition:all 0.2s;box-shadow:0 0 20px rgba(0,240,255,0.35);margin-top:6px}
.btn-submit-login:hover{background:#38bdf8}
.btn-aux{background:#1e293b;color:#e2e8f0;border:1px solid #475569;border-radius:10px;padding:10px 14px;font-size:13px;cursor:pointer;flex:1;text-decoration:none;display:inline-flex;align-items:center;justify-content:center}
.btn-aux:hover{background:#334155;color:#fff}
.cover-thumb{width:40px;height:40px;border-radius:6px;object-fit:cover;vertical-align:middle;margin-right:10px}
</style>
</head>
<body>
<header>
<div>
<h1 id='pageHeaderTitle'>🎵 DaVE Player</h1>
<p id='pageHeaderSub' style='color:#64748b;font-size:14px;margin-top:2px;'>Consola de Control Remoto WiFi • ${songList.size} canciones sincronizadas</p>
</div>
<div style='display:flex;align-items:center;gap:10px;'>
<div id='connectionBadge' class='badge'>🟢 Conectado</div>
<button id='btnHeaderLogout' class='btn-aux' style='background:rgba(239,68,68,0.15);color:#ef4444;border-color:rgba(239,68,68,0.3);padding:6px 12px;font-size:12px;font-weight:700;' onclick='onServerDisconnected()'>🚪 Cerrar Sesión</button>
</div>
</header>

<div class='player-card'>
<div class='now-title' id='nowTitle'>Conectando a DaVE Player...</div>
<div class='now-artist' id='nowArtist'>Celular vinculado</div>
<div class='controls'>
<button class='btn-ctrl' onclick='sendCmd("prev")'>⏮️ Anterior</button>
<button class='btn-ctrl btn-main' id='btnPlayPause' onclick='togglePlayback()'>⏯️ Play / Pausa</button>
<button class='btn-ctrl' onclick='sendCmd("next")'>⏭️ Siguiente</button>
</div>
<audio id='cloudAudioPlayer' style='display:none;' onended='playNextCloudSong()'></audio>
</div>

<div class='search-container'>
<input type='text' id='searchBox' class='search-box' placeholder='🔍 Buscar en tus canciones...' oninput='filterSongs()'>
</div>

<table>
<thead><tr><th>Canción</th><th>Artista</th><th>Acción</th><th>Descarga</th></tr></thead>
<tbody id='songTbody'>$rows</tbody>
</table>

<footer>DaVE Player Legendary Edition • Transmisión Directa & Sincronización Cloud</footer>

<!-- Modal Iniciar Sesion por Desconexion -->
<div id='disconnectLoginModal' class='modal-overlay'>
  <div class='modal-card'>
    <div style='font-size:36px;margin-bottom:10px;'>🔒</div>
    <div class='modal-title'>Iniciar Sesión en DaVE Cloud</div>
    
    <div class='modal-alert'>
      <strong>⚠️ Servidor del teléfono desconectado</strong>
      Se ha cerrado el servidor de tu teléfono para esta PC. Inicia sesión para cargar y reproducir todas las canciones que tenías en la nube.
    </div>

    <form onsubmit='submitCloudLogin(event)'>
      <div class='form-group'>
        <label>Correo Electrónico</label>
        <input type='email' id='loginEmail' class='form-input' value='david.chaparro@daveplayer.app' required>
      </div>
      <div class='form-group'>
        <label>Contraseña / PIN</label>
        <input type='password' id='loginPass' class='form-input' value='••••••••' required>
      </div>
      <button type='submit' class='btn-submit-login'>☁️ Iniciar Sesión y Cargar Canciones de la Nube</button>
    </form>

    <div style='display:flex;gap:8px;margin-top:14px;'>
      <button class='btn-aux' onclick='quickDemoLogin()'>⚡ Cuenta Rápida</button>
      <a class='btn-aux' href='https://davidchaparro780-wq.github.io/AuraPlayer/' target='_blank'>🌐 Abrir App Web</a>
    </div>
  </div>
</div>

<script>
var isCloudMode = false;
var currentCloudIndex = -1;
var failedChecks = 0;
var cloudAudio = document.getElementById('cloudAudioPlayer');

var cloudTracks = [
  { title: 'Happy Nation', artist: 'Ace of Base', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Ace%20of%20Base%20-%20Happy%20Nation.mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music3/v4/fb/fd/a8/fbfda872-a03c-4c01-c3f1-8e185c081f7b/cover.jpg/600x600bb.jpg' },
  { title: 'NADIE SABE', artist: 'Bad Bunny', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/BAD%20BUNNY%20-%20%20NADIE%20SABE%20(Visualizer)%20_%20nadie%20sabe%20lo%20que%20va%20a%20pasar%20ma%C3%B1ana.mp3', coverUrl: 'https://upload.wikimedia.org/wikipedia/en/7/74/Bad_Bunny_-_Nadie_Sabe_Lo_Que_Va_a_Pasar_Ma%C3%B1ana.png' },
  { title: 'Dos Mil 16', artist: 'Bad Bunny', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Bad%20Bunny%20-%20Dos%20Mil%2016%20(360%C2%B0%20Visualizer)%20_%20Un%20Verano%20Sin%20Ti.mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/3e/04/eb/3e04ebf6-370f-f59d-ec84-2c2643db92f1/196626945068.jpg/600x600bb.jpg' },
  { title: 'Breakin\' Dishes', artist: 'Rihanna', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Breakin_%20Dishes(M4A_128K).mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/2b/c0/81/2bc081c8-25f0-ba43-d451-587a54613778/16UMGIM59202.rgb.jpg/600x600bb.jpg' },
  { title: 'QUÉ LÍO', artist: 'Blessd', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Blessd%20%20-%20QU%C3%89%20L%C3%8DO%20(Lyric%20Video)%20_%20CantoYo.mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/b4/8f/cc/b48fccb4-aaf0-913f-1dd6-bd4aa9c8ac2d/827568017680.jpg/600x600bb.jpg' },
  { title: 'Después De La Una', artist: 'Cris MJ, FloyyMenor, LOUKI', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Cris%20MJ_%20FloyyMenor_%20LOUKI%20-%20Despu%C3%A9s%20De%20La%20Una%20(Vi(M4A_128K).mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/47/64/f9/4764f901-1f97-26c4-61cf-fcf3dc016c09/430931.jpg/600x600bb.jpg' },
  { title: 'Guardian', artist: 'Curly & QORA', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Curly%20%26%20QORA%20-%20Guardian.mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music113/v4/73/5c/46/735c4661-ce10-71f4-1021-5a8efe0e9173/73589bec-0d08-4524-addf-e7d684335c1d.jpg/600x600bb.jpg' },
  { title: 'Let You Down (Ending Theme)', artist: 'Dawid Podsiadło', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Cyberpunk_%20Edgerunners%20-%20Ending%20Theme%20_%20Let%20You%20Down%20by%20Dawid%20Podsiadlo%20_%20Netflix.mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music122/v4/82/35/0e/82350ed4-f66f-600b-f572-c7507fc66a10/196589453082.jpg/600x600bb.jpg' },
  { title: 'Phantom Liberty', artist: 'Dawid Podsiadło, P.T. Adamczyk', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Dawid%20Podsiadlo%2C%20P.T.%20Adamczyk%20-%20Phantom%20Liberty%20(Official%20Cyberpunk%202077%20Music%20Video).mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/e1/6e/79/e16e7907-1a77-8e18-64df-6a5d28ecc17d/196871442299.jpg/600x600bb.jpg' },
  { title: 'Pose', artist: 'Daddy Yankee', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Daddy%20Yankee%20_%20Pose%20%5BLetra%5D(M4A_128K).mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/f9/95/52/f99552d9-b212-a3a0-cea4-fe0c5dc26243/24CRGIM46809.rgb.jpg/600x600bb.jpg' },
  { title: 'L\'Amour Toujours (Tanzen Vision Rmx)', artist: 'Gigi D\'Agostino', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Topic%20-%20L%27Amour%20Toujours%20(Tanzen%20Vision%20Rmx).mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/86/4c/46/864c4651-6277-6126-f58f-fec0ef34109f/090204669776_neu.jpg/600x600bb.jpg' },
  { title: 'Abracadabra', artist: 'Lady Gaga', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Lady%20Gaga%20-%20Abracadabra%20(Official%20Music%20Video).mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/08/12/80/08128053-d7df-489d-bfde-be6f45f075be/26UMGIM57129.rgb.jpg/600x600bb.jpg' },
  { title: 'Bad Romance', artist: 'Lady Gaga', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Lady%20Gaga%20-%20Bad%20Romance%20(Official%20Music%20Video).mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music124/v4/1f/25/c4/1f25c4bf-7f7a-ff26-8769-20ab6052dadf/09UMGIM40719.rgb.jpg/600x600bb.jpg' },
  { title: 'Bloody Mary', artist: 'Lady Gaga', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Lady%20Gaga%20-%20Bloody%20Mary%20(Official%20Audio).mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/42/9f/0f/429f0fd2-30bd-b64e-27fc-76d8fbbd0988/11UMGIM12476.rgb.jpg/600x600bb.jpg' },
  { title: 'Just Dance', artist: 'Lady Gaga ft. Colby O\'Donis', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Lady%20Gaga%20-%20Just%20Dance%20(Official%20Music%20Video)%20ft.%20Colby%20O%27Donis.mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music126/v4/a6/68/28/a66828c0-3fe3-5419-374d-ad98739f3166/08UMGIM13954.rgb.jpg/600x600bb.jpg' },
  { title: 'Color Your Night', artist: 'Lotus Juice', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Topic%20-%20Color%20Your%20Night.mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music112/v4/d7/0e/72/d70e724e-d8c0-8043-516a-ba47299b1554/PA00136839_1_185077_jacket.jpg/600x600bb.jpg' },
  { title: 'A Phantom Pain', artist: 'Ludvig Forssell', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Topic%20-%20A%20Phantom%20Pain.mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music49/v4/c2/87/60/c2876016-b688-18cc-7649-c44049450c79/007725_4988602168907.jpg/600x600bb.jpg' },
  { title: 'Somos de Calle', artist: 'Daddy Yankee', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Somos%20de%20Calle.mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/f9/95/52/f99552d9-b212-a3a0-cea4-fe0c5dc26243/24CRGIM46809.rgb.jpg/600x600bb.jpg' },
  { title: 'Duvet (Serial Experiments Lain)', artist: 'Bôa (sweetblue.)', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/B%C3%B4a%20-%20Duvet%20(Sub.%20Espa%C3%B1ol%20%2B%20Lyrics).mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/45/44/15/45441528-0288-eedc-f6fc-93137b8cfe96/067003248969.png/600x600bb.jpg' },
  { title: 'LA PLENA (W Sound 05)', artist: 'Beéle, Westcol, Ovy On The Drums', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/W%20Sound%2005%20_LA%20PLENA_%20-%20Be%C3%A9le%2C%20Westcol%2C%20Ovy%20On%20The%20Drums.mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/76/c1/83/76c18371-1a13-b500-12a5-da71f33a8d25/0.jpg/600x600bb.jpg' },
  { title: 'SSRHD (Remix)', artist: 'Ziraki', streamUrl: 'https://davidchaparro780-wq.github.io/AuraPlayer/music/Ziraki%20-%20SSRHD%20(Remix).mp3', coverUrl: 'https://is1-ssl.mzstatic.com/image/thumb/Music211/v4/25/b9/d0/25b9d0a5-323a-fd54-bce4-6c768c8b00e4/8721056924288.png/600x600bb.jpg' }
];

function sendCmd(cmd){
  if(isCloudMode){
    if(cmd === 'prev') playPrevCloudSong();
    else if(cmd === 'next') playNextCloudSong();
    return;
  }
  fetch('/api/' + cmd).then(()=>updateStatus());
}

function playSong(id){
  fetch('/api/playid/' + id).then(()=>updateStatus());
}

function togglePlayback(){
  if(isCloudMode){
    if(cloudAudio.paused) cloudAudio.play();
    else cloudAudio.pause();
    document.getElementById('btnPlayPause').innerText = cloudAudio.paused ? '▶️ Reproducir' : '⏸️ Pausar';
    return;
  }
  sendCmd('playpause');
}

function playCloudSong(index){
  currentCloudIndex = index;
  var track = cloudTracks[index];
  if(!track) return;
  cloudAudio.src = track.streamUrl;
  cloudAudio.play().catch(function(e){ console.log(e); });
  document.getElementById('nowTitle').innerText = track.title;
  document.getElementById('nowArtist').innerText = track.artist + ' • Nube DaVE';
  document.getElementById('btnPlayPause').innerText = '⏸️ Pausar';
}

function playNextCloudSong(){
  if(cloudTracks.length === 0) return;
  var next = (currentCloudIndex + 1) % cloudTracks.length;
  playCloudSong(next);
}

function playPrevCloudSong(){
  if(cloudTracks.length === 0) return;
  var prev = (currentCloudIndex - 1 + cloudTracks.length) % cloudTracks.length;
  playCloudSong(prev);
}

function filterSongs(){
  var q = document.getElementById('searchBox').value.toLowerCase();
  var rows = document.querySelectorAll('.song-row');
  rows.forEach(function(r){
    var name = r.getAttribute('data-name');
    r.style.display = (!q || name.indexOf(q) !== -1) ? '' : 'none';
  });
}

function onServerDisconnected(){
  // 1. Vaciar completamente la lista de canciones para que no aparezca nada
  var tbody = document.getElementById('songTbody');
  if (tbody) tbody.innerHTML = '';

  // 2. Ocultar la tabla de musica y los controles
  var table = document.querySelector('table');
  if (table) table.style.display = 'none';
  var pcard = document.querySelector('.player-card');
  if (pcard) pcard.style.display = 'none';
  var sbox = document.querySelector('.search-container');
  if (sbox) sbox.style.display = 'none';

  // 3. Detener reproduccion
  if (cloudAudio) { cloudAudio.pause(); cloudAudio.src = ''; }

  // 4. Mostrar exclusivamente la ventana de inicio de sesion (100% opaca)
  var btnHLogout = document.getElementById('btnHeaderLogout');
  if (btnHLogout) btnHLogout.style.display = 'none';
  var modal = document.getElementById('disconnectLoginModal');
  if (modal) {
    modal.style.display = 'flex';
    modal.style.background = '#0a0e1a';
  }
}

function submitCloudLogin(e){
  e.preventDefault();
  restoreCloudLibrary();
}

function quickDemoLogin(){
  restoreCloudLibrary();
}

function restoreCloudLibrary(){
  isCloudMode = true;
  document.getElementById('disconnectLoginModal').style.display = 'none';
  var btnHLogout = document.getElementById('btnHeaderLogout');
  if (btnHLogout) btnHLogout.style.display = 'inline-block';

  // Re-mostrar tabla y reproductor
  var table = document.querySelector('table');
  if (table) table.style.display = 'table';
  var pcard = document.querySelector('.player-card');
  if (pcard) pcard.style.display = 'block';
  var sbox = document.querySelector('.search-container');
  if (sbox) sbox.style.display = 'block';

  document.getElementById('pageHeaderTitle').innerText = '☁️ DaVE Player Cloud';
  document.getElementById('pageHeaderSub').innerText = 'Sesión iniciada con éxito • ' + cloudTracks.length + ' canciones de la nube sincronizadas';
  document.getElementById('connectionBadge').className = 'badge badge-cloud';
  document.getElementById('connectionBadge').innerText = '☁️ Nube Activa';

  var tbody = document.getElementById('songTbody');
  tbody.innerHTML = '';
  cloudTracks.forEach(function(track, idx){
    var tr = document.createElement('tr');
    tr.className = 'song-row';
    tr.setAttribute('data-name', (track.title + ' ' + track.artist).toLowerCase());
    tr.innerHTML = "<td><img src='" + track.coverUrl + "' class='cover-thumb'><b>" + track.title + "</b></td>" +
                   "<td>" + track.artist + "</td>" +
                   "<td><button class='btn-play' onclick='playCloudSong(" + idx + ")'>▶ Reproducir</button></td>" +
                   "<td><a class='btn-dl' href='" + track.streamUrl + "' target='_blank' download>⬇ Descargar</a></td>";
    tbody.appendChild(tr);
  });

  if(cloudTracks.length > 0){
    playCloudSong(0);
  }
}

function updateStatus(){
  if(isCloudMode) return;
  fetch('/api/status', { cache: 'no-store' })
    .then(function(r){
      if(!r.ok) throw new Error('status err');
      return r.json();
    })
    .then(function(data){
      failedChecks = 0;
      document.getElementById('nowTitle').innerText = data.title;
      document.getElementById('nowArtist').innerText = data.artist + (data.album ? ' • ' + data.album : '');
      document.getElementById('btnPlayPause').innerText = data.isPlaying ? '⏸️ Pausar' : '▶️ Reproducir';
    })
    .catch(function(){
      failedChecks++;
      if (failedChecks >= 2) {
        onServerDisconnected();
      }
    });
}

setInterval(updateStatus, 1500);
updateStatus();
</script>
</body>
</html>"""

        sendResponse(socket, "200 OK", "text/html; charset=UTF-8", html.toByteArray(Charsets.UTF_8))
    }

    private fun serveParty(socket: Socket) {
        val html = """<!DOCTYPE html>
<html lang='es'>
<head>
<meta charset='UTF-8'>
<meta name='viewport' content='width=device-width,initial-scale=1'>
<title>🎉 DaVE Party Sync — Discoteca Silenciosa</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{background:#05070e;color:#fff;font-family:system-ui,sans-serif;text-align:center;padding:30px 20px}
.disco-card{background:linear-gradient(135deg,#3b0764,#0f172a);border:2px solid #e040fb;border-radius:28px;padding:36px;max-width:500px;margin:20px auto;box-shadow:0 0 50px rgba(224,64,251,0.3)}
h1{color:#00f0ff;font-size:26px;margin-bottom:8px}
.sub{color:#cbd5e1;font-size:14px;margin-bottom:24px}
.song-box{background:#1e1b4b;padding:16px;border-radius:18px;margin-bottom:24px}
.title{font-size:20px;font-weight:800;color:#fff}
.artist{font-size:14px;color:#c084fc;margin-top:4px}
.btn-join{background:linear-gradient(135deg,#e040fb,#00f0ff);color:#000;font-size:18px;font-weight:900;border:none;border-radius:50px;padding:16px 40px;cursor:pointer;box-shadow:0 0 30px rgba(0,240,255,0.5);transition:all 0.2s}
.btn-join:hover{transform:scale(1.05)}
audio{width:100%;margin-top:20px;outline:none}
</style>
</head>
<body>
<div class='disco-card'>
<h1>🎉 DaVE Party Sync</h1>
<p class='sub'>Discoteca Silenciosa WiFi • Sintonizado con el DJ</p>
<div class='song-box'>
<div class='title' id='title'>Cargando pista...</div>
<div class='artist' id='artist'>DaVE Player</div>
</div>
<button class='btn-join' id='btnJoin' onclick='joinParty()'>🔊 Unirse al Audio en Vivo</button>
<audio id='partyAudio' controls style='display:none'></audio>
</div>
<script>
var audio = document.getElementById('partyAudio');
var currentId = -1;
function joinParty(){
  audio.style.display = 'block';
  audio.src = '/stream/current?t=' + Date.now();
  audio.play();
  document.getElementById('btnJoin').innerText = '🟢 En Sintonía';
}
function checkSong(){
  fetch('/api/status').then(r=>r.json()).then(data=>{
    document.getElementById('title').innerText = data.title;
    document.getElementById('artist').innerText = data.artist;
    if(currentId !== -1 && currentId !== data.id && audio.src){
      audio.src = '/stream/current?t=' + Date.now();
      audio.play();
    }
    currentId = data.id;
  }).catch(()=>{});
}
setInterval(checkSong, 2000);
checkSong();
</script>
</body>
</html>"""
        sendResponse(socket, "200 OK", "text/html; charset=UTF-8", html.toByteArray(Charsets.UTF_8))
    }

    private fun streamSong(socket: Socket, song: MediaModel) {
        var inputStream: InputStream? = null
        var fileSize = 0L
        val file = if (song.path.isNotBlank()) File(song.path) else null

        if (file != null && file.exists() && file.canRead()) {
            fileSize = file.length()
            inputStream = FileInputStream(file)
        } else if (appContext != null) {
            try {
                fileSize = song.size
                inputStream = appContext?.contentResolver?.openInputStream(song.uri)
            } catch (_: Exception) {}
        }

        if (inputStream == null) {
            serve404(socket)
            return
        }

        val mimeType = when {
            song.title.endsWith(".flac", true) || song.path.endsWith(".flac", true) -> "audio/flac"
            song.title.endsWith(".wav", true) || song.path.endsWith(".wav", true) -> "audio/wav"
            song.title.endsWith(".ogg", true) || song.path.endsWith(".ogg", true) -> "audio/ogg"
            song.title.endsWith(".m4a", true) || song.title.endsWith(".aac", true) || song.path.endsWith(".m4a", true) -> "audio/mp4"
            else -> "audio/mpeg"
        }

        val headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: $mimeType\r\n" +
                (if (fileSize > 0) "Content-Length: $fileSize\r\n" else "") +
                "Accept-Ranges: bytes\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"

        val out = socket.getOutputStream()
        out.write(headers.toByteArray(Charsets.ISO_8859_1))
        out.flush()

        inputStream.use { ins ->
            ins.copyTo(out)
        }
        out.flush()
        try { socket.shutdownOutput() } catch (_: Exception) {}
    }

    private fun serve404(socket: Socket) {
        val html = "<!DOCTYPE html><html><body style='background:#0a0e1a;color:#fff;font-family:sans-serif;text-align:center;padding:50px;'><h1>404 — No encontrado</h1></body></html>"
        sendResponse(socket, "404 Not Found", "text/html; charset=UTF-8", html.toByteArray(Charsets.UTF_8))
    }

    private fun escHtml(s: String) = s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
}
