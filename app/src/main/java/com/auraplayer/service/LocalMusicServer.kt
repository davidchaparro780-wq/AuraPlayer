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

                path == "/" || path == "/index.html" -> serveIndex(socket)
                path == "/party" -> serveParty(socket)
                path == "/favicon.ico" -> sendNoContent(socket)
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
                path.startsWith("/stream/") -> {
                    val id = path.removePrefix("/stream/").toLongOrNull()
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
<title>🎵 DaVE Player — Control Remoto WiFi</title>
<style>
*{box-sizing:border-box;margin:0;padding:0}
body{background:#0a0e1a;color:#e2e8f0;font-family:system-ui,-apple-system,BlinkMacSystemFont,sans-serif;padding:20px;max-width:960px;margin:0 auto}
header{display:flex;align-items:center;justify-content:space-between;margin-bottom:20px;flex-wrap:wrap;gap:10px}
h1{color:#00f0ff;font-size:24px;display:flex;align-items:center;gap:10px;text-shadow:0 0 15px rgba(0,240,255,0.4)}
.badge{background:#10b981;color:#000;font-size:12px;font-weight:800;padding:4px 10px;border-radius:20px;text-transform:uppercase}
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
</style>
</head>
<body>
<header>
<div>
<h1>🎵 DaVE Player</h1>
<p style='color:#64748b;font-size:14px;margin-top:2px;'>Consola de Control Remoto WiFi • ${songList.size} canciones sincronizadas</p>
</div>
<div class='badge'>🟢 Conectado</div>
</header>

<div class='player-card'>
<div class='now-title' id='nowTitle'>Conectando a DaVE Player...</div>
<div class='now-artist' id='nowArtist'>Celular vinculado</div>
<div class='controls'>
<button class='btn-ctrl' onclick='sendCmd("prev")'>⏮️ Anterior</button>
<button class='btn-ctrl btn-main' id='btnPlayPause' onclick='sendCmd("playpause")'>⏯️ Play / Pausa</button>
<button class='btn-ctrl' onclick='sendCmd("next")'>⏭️ Siguiente</button>
</div>
</div>

<div class='search-container'>
<input type='text' id='searchBox' class='search-box' placeholder='🔍 Buscar en tus canciones...' oninput='filterSongs()'>
</div>

<table>
<thead><tr><th>Canción</th><th>Artista</th><th>En Teléfono</th><th>En PC</th></tr></thead>
<tbody id='songTbody'>$rows</tbody>
</table>

<footer>DaVE Player Legendary Edition • Transmisión Directa sin Intermediarios</footer>

<script>
function sendCmd(cmd){
  fetch('/api/' + cmd).then(()=>updateStatus());
}
function playSong(id){
  fetch('/api/playid/' + id).then(()=>updateStatus());
}
function filterSongs(){
  var q = document.getElementById('searchBox').value.toLowerCase();
  var rows = document.querySelectorAll('.song-row');
  rows.forEach(function(r){
    var name = r.getAttribute('data-name');
    r.style.display = (!q || name.indexOf(q) !== -1) ? '' : 'none';
  });
}
function updateStatus(){
  fetch('/api/status')
    .then(r=>r.json())
    .then(data=>{
      document.getElementById('nowTitle').innerText = data.title;
      document.getElementById('nowArtist').innerText = data.artist + (data.album ? ' • ' + data.album : '');
      document.getElementById('btnPlayPause').innerText = data.isPlaying ? '⏸️ Pausar' : '▶️ Reproducir';
    }).catch(()=>{});
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
