package com.auraplayer.service

import android.util.Log
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.ServerSocket
import java.net.Socket

/**
 * LocalMusicServer — Lightweight HTTP server using pure ServerSocket (no extra libraries).
 * Serves your local music library at http://192.168.x.x:8080
 * Any browser on the same WiFi network can browse and stream your music.
 */
class LocalMusicServer {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var songList: List<MediaModel> = emptyList()
    val port = 8080

    fun start(songs: List<MediaModel>, scope: CoroutineScope) {
        if (serverSocket?.isClosed == false) return
        songList = songs
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                Log.d("LocalMusicServer", "Server started on port $port")
                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                Log.d("LocalMusicServer", "Server stopped: ${e.message}")
            }
        }
    }

    fun stop() {
        try { serverSocket?.close() } catch (_: Exception) {}
        serverJob?.cancel()
        serverSocket = null
    }

    val isRunning get() = serverSocket?.isClosed == false

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = reader.readLine() ?: return
            val out = PrintStream(socket.getOutputStream())

            val path = requestLine.split(" ").getOrNull(1) ?: "/"

            when {
                path == "/" || path == "/index.html" -> serveIndex(out)
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

    private fun serveIndex(out: PrintStream) {
        val rows = songList.joinToString("") { song ->
            "<tr>" +
            "<td>${escHtml(song.title)}</td>" +
            "<td>${escHtml(song.artist)}</td>" +
            "<td><a href='/stream/${song.id}'>▶ Reproducir</a></td>" +
            "<td><a href='/stream/${song.id}' download='${escHtml(song.title)}.mp3'>⬇ Descargar</a></td>" +
            "</tr>"
        }
        val html = """<!DOCTYPE html>
<html lang='es'><head><meta charset='UTF-8'>
<meta name='viewport' content='width=device-width,initial-scale=1'>
<title>🎵 DaVE Player — Servidor WiFi</title>
<style>
body{background:#0a0e1a;color:#e2e8f0;font-family:system-ui,sans-serif;padding:20px}
h1{color:#00f0ff;margin-bottom:4px}p{color:#64748b;margin-bottom:20px}
table{width:100%;border-collapse:collapse}
th{background:#1e293b;padding:12px;text-align:left;color:#94a3b8}
td{padding:10px;border-bottom:1px solid #1e293b}
a{color:#00f0ff;text-decoration:none;font-weight:bold}a:hover{color:#fff}
tr:hover{background:#1e293b}
</style></head><body>
<h1>🎵 DaVE Player — Servidor WiFi Local</h1>
<p>${songList.size} canciones disponibles en tu red WiFi</p>
<table><thead><tr><th>Canción</th><th>Artista</th><th>Reproducir</th><th>Descargar</th></tr></thead>
<tbody>$rows</tbody></table></body></html>"""

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
        val body = "<h1>404 — Canción no encontrada</h1>"
        out.print("HTTP/1.1 404 Not Found\r\nContent-Type: text/html\r\nContent-Length: ${body.length}\r\n\r\n$body")
        out.flush()
    }

    private fun escHtml(s: String) = s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
}
