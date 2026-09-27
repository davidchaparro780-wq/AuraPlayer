package com.auraplayer.service

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

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
    private var serverJob: Job? = null
    private var songList: List<MediaModel> = emptyList()
    private var controller: ServerController? = null
    val port = 8080

    @Volatile
    var isRunning = false
        private set

    fun start(songs: List<MediaModel>, ctrl: ServerController, scope: CoroutineScope) {
        if (isRunning) return
        songList = songs
        controller = ctrl
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                Log.d("LocalMusicServer", "Server listening on port $port")
                while (isActive && isRunning) {
                    val client = serverSocket?.accept() ?: break
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                Log.d("LocalMusicServer", "Server stopped: ${e.message}")
            } finally {
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverJob?.cancel()
        serverSocket = null
    }

    fun getLocalIpAddress(context: Context): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }
        } catch (_: Exception) {}

        // Fallback: iterate network interfaces
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}

        return "127.0.0.1"
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return
            val out = PrintStream(socket.getOutputStream())

            val path = requestLine.split(" ").getOrNull(1) ?: "/"

            when {
                path == "/" || path == "/index.html" -> serveIndex(out)
                path.startsWith("/api/status") -> serveStatusApi(out)
                path.startsWith("/api/playpause") -> {
                    controller?.onPlayPause()
                    serveJsonSuccess(out, "toggled")
                }
                path.startsWith("/api/next") -> {
                    controller?.onNext()
                    serveJsonSuccess(out, "next")
                }
                path.startsWith("/api/prev") -> {
                    controller?.onPrev()
                    serveJsonSuccess(out, "prev")
                }
                path.startsWith("/api/playid/") -> {
                    val id = path.removePrefix("/api/playid/").toLongOrNull()
                    if (id != null) controller?.onPlaySongById(id)
                    serveJsonSuccess(out, "playing_$id")
                }
                path.startsWith("/stream/") -> {
                    val id = path.removePrefix("/stream/").toLongOrNull()
                    val song = songList.firstOrNull { it.id == id }
                    if (song != null) streamSong(out, socket, song)
                    else serve404(out)
                }
                else -> serve404(out)
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun serveStatusApi(out: PrintStream) {
        val current = controller?.getCurrentSong()
        val playing = controller?.isPlaying() ?: false

        val json = JSONObject().apply {
            put("isPlaying", playing)
            put("title", current?.title ?: "Sin reproducción")
            put("artist", current?.artist ?: "DaVE Player")
            put("album", current?.album ?: "")
            put("id", current?.id ?: -1)
        }.toString()

        out.print("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\nContent-Length: ${json.toByteArray().size}\r\nConnection: close\r\n\r\n")
        out.print(json)
        out.flush()
    }

    private fun serveJsonSuccess(out: PrintStream, msg: String) {
        val json = JSONObject().put("status", "ok").put("message", msg).toString()
        out.print("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nAccess-Control-Allow-Origin: *\r\nContent-Length: ${json.toByteArray().size}\r\nConnection: close\r\n\r\n")
        out.print(json)
        out.flush()
    }

    private fun serveIndex(out: PrintStream) {
        val rows = songList.joinToString("") { song ->
            "<tr>" +
            "<td><b>${escHtml(song.title)}</b></td>" +
            "<td>${escHtml(song.artist)}</td>" +
            "<td><button class='btn-play' onclick='playSong(${song.id})'>▶ Reproducir en Teléfono</button></td>" +
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
body{background:#0a0e1a;color:#e2e8f0;font-family:system-ui,-apple-system,sans-serif;padding:20px;max-width:900px;margin:0 auto}
h1{color:#00f0ff;font-size:24px;margin-bottom:6px;display:flex;align-items:center;gap:10px}
p.sub{color:#64748b;margin-bottom:20px;font-size:14px}
.player-card{background:linear-gradient(135deg,#1e1b4b,#0f172a);border:1px solid #6366f1;border-radius:20px;padding:24px;margin-bottom:24px;text-align:center;box-shadow:0 10px 30px rgba(0,240,255,0.1)}
.now-title{font-size:20px;font-weight:800;color:#fff;margin-bottom:4px}
.now-artist{font-size:14px;color:#a5b4fc;margin-bottom:20px}
.controls{display:flex;justify-content:center;align-items:center;gap:16px}
.btn-ctrl{background:#312e81;color:#fff;border:none;border-radius:50px;padding:12px 24px;font-size:16px;font-weight:700;cursor:pointer;transition:all 0.2s}
.btn-ctrl:hover{background:#4338ca;transform:scale(1.05)}
.btn-main{background:#00f0ff;color:#000;padding:14px 32px;font-size:18px}
.btn-main:hover{background:#38bdf8}
table{width:100%;border-collapse:collapse;margin-top:10px;background:#0f172a;border-radius:12px;overflow:hidden}
th{background:#1e293b;padding:14px;text-align:left;color:#94a3b8;font-size:13px}
td{padding:12px 14px;border-bottom:1px solid #1e293b;font-size:14px}
tr:hover{background:#1e293b}
.btn-play{background:#059669;color:#fff;border:none;padding:6px 12px;border-radius:8px;cursor:pointer;font-weight:600}
.btn-play:hover{background:#10b981}
.btn-dl{color:#38bdf8;text-decoration:none;font-weight:600}
.btn-dl:hover{text-decoration:underline}
</style>
</head>
<body>
<h1>🎵 DaVE Player — Control Remoto WiFi</h1>
<p class='sub'>Conectado en vivo a tu celular • ${songList.size} canciones disponibles</p>

<div class='player-card'>
<div class='now-title' id='nowTitle'>Cargando reproductor...</div>
<div class='now-artist' id='nowArtist'>DaVE Player</div>
<div class='controls'>
<button class='btn-ctrl' onclick='sendCmd("prev")'>⏮️ Anterior</button>
<button class='btn-ctrl btn-main' id='btnPlayPause' onclick='sendCmd("playpause")'>⏯️ Play / Pausa</button>
<button class='btn-ctrl' onclick='sendCmd("next")'>⏭️ Siguiente</button>
</div>
</div>

<h2 style='color:#fff;font-size:18px;margin-bottom:12px'>📜 Tu Biblioteca de Canciones</h2>
<table>
<thead><tr><th>Canción</th><th>Artista</th><th>Acción</th><th>Descargar</th></tr></thead>
<tbody>$rows</tbody>
</table>

<script>
function sendCmd(cmd){
  fetch('/api/' + cmd).then(()=>updateStatus());
}
function playSong(id){
  fetch('/api/playid/' + id).then(()=>updateStatus());
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
setInterval(updateStatus, 2000);
updateStatus();
</script>
</body>
</html>"""

        out.print("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Length: ${html.toByteArray().size}\r\nConnection: close\r\n\r\n")
        out.print(html)
        out.flush()
    }

    private fun streamSong(out: PrintStream, socket: Socket, song: MediaModel) {
        val file = File(song.path)
        if (!file.exists()) { serve404(out); return }
        val mimeType = when (file.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "aac", "m4a" -> "audio/mp4"
            else -> "audio/mpeg"
        }
        out.print("HTTP/1.1 200 OK\r\nContent-Type: $mimeType\r\nContent-Length: ${file.length()}\r\nAccept-Ranges: bytes\r\nConnection: close\r\n\r\n")
        out.flush()
        FileInputStream(file).use { it.copyTo(socket.getOutputStream()) }
    }

    private fun serve404(out: PrintStream) {
        val body = "<h1>404 — No encontrado</h1>"
        out.print("HTTP/1.1 404 Not Found\r\nContent-Type: text/html\r\nContent-Length: ${body.length}\r\n\r\n$body")
        out.flush()
    }

    private fun escHtml(s: String) = s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
}
