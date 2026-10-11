package com.auraplayer.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.auraplayer.audio.EqualizerManager
import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupSummary(
    val playlists: Int = 0,
    val favorites: Int = 0,
    val settings: Int = 0,
    val queueSongs: Int = 0
) {
    fun toMessage(): String {
        return "Copia restaurada: $playlists listas, $favorites favoritos, $settings ajustes y $queueSongs canciones en la cola"
    }
}

class BackupManager(private val context: Context) {

    companion object {
        private const val TAG = "BackupManager"
        private const val FORMAT_VERSION = 1
    }

    private val playlistManager = PlaylistManager(context)
    private val favoritesManager = FavoritesManager(context)
    private val queueStore = QueueStore(context)
    private val settingsManager = SettingsManager.getInstance(context)
    private val equalizerManager = EqualizerManager.instance

    suspend fun exportBackup(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("version", FORMAT_VERSION)
            root.put("timestamp", System.currentTimeMillis())
            root.put("playlists", buildPlaylistsJson())
            root.put("favorites", buildFavoritesJson())
            root.put("settings", buildSettingsJson())
            root.put("queue", buildQueueJson())

            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (dir == null) {
                AppLog.w(TAG, "No hay carpeta externa disponible para exportar la copia")
                return@withContext Result.failure(
                    IllegalStateException("No hay almacenamiento disponible para guardar la copia")
                )
            }
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "aura_backup_$stamp.json")
            file.writeText(root.toString(2))
            AppLog.i(TAG, "Copia de seguridad exportada en ${file.absolutePath}")
            Result.success(file)
        } catch (e: Exception) {
            AppLog.w(TAG, "No se pudo exportar la copia de seguridad", e)
            Result.failure(e)
        }
    }

    suspend fun importBackup(uri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream == null) {
                AppLog.w(TAG, "No se pudo abrir el archivo de copia seleccionado")
                return@withContext Result.failure(
                    IllegalStateException("No se pudo leer el archivo seleccionado")
                )
            }
            val raw = stream.bufferedReader().use { it.readText() }
            importBackupFromJson(raw)
        } catch (e: Exception) {
            AppLog.w(TAG, "No se pudo importar la copia de seguridad", e)
            Result.failure(e)
        }
    }

    private fun importBackupFromJson(raw: String): Result<BackupSummary> {
        return try {
            val root = JSONObject(raw)
            val version = root.optInt("version", 0)
            if (version != FORMAT_VERSION) {
                AppLog.w(TAG, "Versión de copia no compatible: $version")
                return Result.failure(
                    IllegalArgumentException("La copia tiene un formato no compatible (versión $version)")
                )
            }

            val playlistsRestored = restorePlaylists(root.optJSONArray("playlists"))
            val favoritesRestored = restoreFavorites(root.optJSONArray("favorites"))

            var settingsRestored = 0
            val settingsObj = root.optJSONObject("settings")
            if (settingsObj != null) {
                settingsRestored += restoreSettings(settingsObj)
                settingsRestored += restoreEqualizer(settingsObj.optJSONObject("equalizer"))
            }

            val queueSongs = restoreQueue(root.optJSONObject("queue"))

            Result.success(
                BackupSummary(
                    playlists = playlistsRestored,
                    favorites = favoritesRestored,
                    settings = settingsRestored,
                    queueSongs = queueSongs
                )
            )
        } catch (e: Exception) {
            AppLog.w(TAG, "El archivo de copia no tiene un JSON válido", e)
            Result.failure(IllegalArgumentException("El archivo seleccionado no es una copia válida"))
        }
    }

    private fun restorePlaylists(array: JSONArray?): Int {
        if (array == null) return 0
        var restored = 0
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("name", "")
            if (name.isBlank()) continue
            val existing = playlistManager.getPlaylists().firstOrNull { it.name == name }
            val playlistId = existing?.id ?: playlistManager.createPlaylist(name).id
            val songs = obj.optJSONArray("songs")
            if (songs != null) {
                for (j in 0 until songs.length()) {
                    val songId = songs.optLong(j, -1L)
                    if (songId > 0L) {
                        playlistManager.addSongToPlaylist(playlistId, songId)
                    }
                }
            }
            restored++
        }
        return restored
    }

    private fun restoreFavorites(array: JSONArray?): Int {
        if (array == null) return 0
        val ids = mutableSetOf<Long>()
        for (i in 0 until array.length()) {
            val id = array.optLong(i, -1L)
            if (id > 0L) {
                ids.add(id)
            }
        }
        if (ids.isEmpty()) return 0
        favoritesManager.setFavorites(favoritesManager.getFavoriteIds() + ids)
        return ids.size
    }

    private fun restoreSettings(obj: JSONObject): Int {
        var restored = 0
        if (obj.has("fade_playback")) {
            settingsManager.setFadePlaybackEnabled(obj.optBoolean("fade_playback", true))
            restored++
        }
        if (obj.has("pause_on_headphones_disconnect")) {
            settingsManager.setPauseOnHeadphonesDisconnectEnabled(obj.optBoolean("pause_on_headphones_disconnect", true))
            restored++
        }
        if (obj.has("resume_on_headphones_connect")) {
            settingsManager.setResumeOnHeadphonesConnectEnabled(obj.optBoolean("resume_on_headphones_connect", false))
            restored++
        }
        if (obj.has("hide_short_audio_sec")) {
            settingsManager.setHideShortAudioSec(obj.optInt("hide_short_audio_sec", 30))
            restored++
        }
        if (obj.has("haptic_enabled")) {
            settingsManager.setHapticEnabled(obj.optBoolean("haptic_enabled", true))
            restored++
        }
        if (obj.has("keep_screen_on_player")) {
            settingsManager.setKeepScreenOnInPlayerEnabled(obj.optBoolean("keep_screen_on_player", false))
            restored++
        }
        if (obj.has("dynamic_palette_enabled")) {
            settingsManager.setDynamicPalette(obj.optBoolean("dynamic_palette_enabled", true))
            restored++
        }
        if (obj.has("visualizer_sensitivity")) {
            settingsManager.setVisualizerSensitivityValue(obj.optDouble("visualizer_sensitivity", 1.0).toFloat())
            restored++
        }
        if (obj.has("notify_updates")) {
            settingsManager.setNotifyUpdatesEnabled(obj.optBoolean("notify_updates", true))
            restored++
        }
        return restored
    }

    private fun restoreEqualizer(obj: JSONObject?): Int {
        if (obj == null) return 0
        equalizerManager.initPrefs(context)
        var restored = 0
        if (obj.has("enabled")) {
            equalizerManager.setEnabled(obj.optBoolean("enabled", true))
            restored++
        }
        if (obj.has("spatial_8d")) {
            equalizerManager.setSpatial8DEnabled(obj.optBoolean("spatial_8d", false))
            restored++
        }
        if (obj.has("bass")) {
            equalizerManager.setBassBoostStrength(obj.optInt("bass", 400).toShort())
            restored++
        }
        if (obj.has("virtualizer")) {
            equalizerManager.setVirtualizerStrength(obj.optInt("virtualizer", 300).toShort())
            restored++
        }
        if (obj.has("loudness")) {
            equalizerManager.setLoudnessGain(obj.optInt("loudness", 0))
            restored++
        }
        val bands = obj.optJSONArray("bands")
        if (bands != null) {
            for (i in 0 until minOf(bands.length(), equalizerManager.bandLevels.size)) {
                equalizerManager.setBandLevel(i, bands.optDouble(i, 0.0).toFloat())
            }
            restored++
        }
        return restored
    }

    private fun restoreQueue(obj: JSONObject?): Int {
        if (obj == null) return 0
        val entriesArray = obj.optJSONArray("entries") ?: return 0
        val entries = mutableListOf<SavedQueueEntry>()
        for (i in 0 until entriesArray.length()) {
            val entry = entriesArray.optJSONObject(i) ?: continue
            val uri = entry.optString("uri", "")
            if (uri.isBlank()) continue
            entries.add(
                SavedQueueEntry(
                    id = entry.optLong("id", 0L),
                    uri = uri,
                    title = entry.optString("title", "Canción"),
                    artist = entry.optString("artist", "Artista"),
                    album = entry.optString("album", ""),
                    artworkUri = entry.optString("artwork", "").ifBlank { null },
                    durationMs = entry.optLong("duration", 0L)
                )
            )
        }
        if (entries.isEmpty()) return 0
        val index = obj.optInt("currentIndex", 0).coerceIn(0, entries.lastIndex)
        queueStore.save(
            entries,
            index,
            obj.optLong("positionMs", 0L),
            obj.optBoolean("wasPlaying", false)
        )
        return entries.size
    }

    private fun buildPlaylistsJson(): JSONArray {
        val array = JSONArray()
        playlistManager.getPlaylists().forEach { playlist ->
            val obj = JSONObject()
            obj.put("id", playlist.id)
            obj.put("name", playlist.name)
            obj.put("songs", JSONArray(playlist.songIds))
            array.put(obj)
        }
        return array
    }

    private fun buildFavoritesJson(): JSONArray {
        val array = JSONArray()
        favoritesManager.getFavoriteIds().forEach { id ->
            array.put(id)
        }
        return array
    }

    private fun buildSettingsJson(): JSONObject {
        val obj = JSONObject()
        obj.put("fade_playback", settingsManager.fadePlayback)
        obj.put("pause_on_headphones_disconnect", settingsManager.pauseOnHeadphonesDisconnect)
        obj.put("resume_on_headphones_connect", settingsManager.resumeOnHeadphonesConnect)
        obj.put("hide_short_audio_sec", settingsManager.hideShortAudioDurationSec)
        obj.put("haptic_enabled", settingsManager.hapticFeedbackEnabled)
        obj.put("keep_screen_on_player", settingsManager.keepScreenOnInPlayer)
        obj.put("dynamic_palette_enabled", settingsManager.dynamicPaletteEnabled)
        obj.put("visualizer_sensitivity", settingsManager.visualizerSensitivity.toDouble())
        obj.put("notify_updates", settingsManager.notifyUpdates)
        obj.put("equalizer", buildEqualizerJson())
        return obj
    }

    private fun buildEqualizerJson(): JSONObject {
        val obj = JSONObject()
        equalizerManager.initPrefs(context)
        obj.put("enabled", equalizerManager.isEnabled)
        obj.put("spatial_8d", equalizerManager.isSpatial8DEnabled)
        obj.put("bass", equalizerManager.bassStrength.toInt())
        obj.put("virtualizer", equalizerManager.virtualizerStrength.toInt())
        obj.put("loudness", equalizerManager.loudnessGain)
        obj.put("bands", JSONArray(equalizerManager.bandLevels.map { it.toDouble() }))
        return obj
    }

    private fun buildQueueJson(): JSONObject {
        val obj = JSONObject()
        val queue = queueStore.load()
        if (queue == null) {
            obj.put("entries", JSONArray())
            return obj
        }
        val array = JSONArray()
        queue.entries.forEach { entry ->
            val entryObj = JSONObject()
            entryObj.put("id", entry.id)
            entryObj.put("uri", entry.uri)
            entryObj.put("title", entry.title)
            entryObj.put("artist", entry.artist)
            entryObj.put("album", entry.album)
            entryObj.put("artwork", entry.artworkUri ?: "")
            entryObj.put("duration", entry.durationMs)
            array.put(entryObj)
        }
        obj.put("entries", array)
        obj.put("currentIndex", queue.currentIndex)
        obj.put("positionMs", queue.positionMs)
        obj.put("wasPlaying", queue.wasPlaying)
        return obj
    }
}
