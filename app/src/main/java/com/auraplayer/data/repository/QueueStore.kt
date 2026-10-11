package com.auraplayer.data.repository

import android.content.Context
import com.auraplayer.util.AppLog
import org.json.JSONArray
import org.json.JSONObject

data class SavedQueueEntry(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: String?,
    val durationMs: Long
)

data class SavedQueue(
    val entries: List<SavedQueueEntry>,
    val currentIndex: Int,
    val positionMs: Long,
    val wasPlaying: Boolean
)

class QueueStore(context: Context) {

    private val prefs = context.getSharedPreferences("dave_playback_queue", Context.MODE_PRIVATE)

    fun save(entries: List<SavedQueueEntry>, currentIndex: Int, positionMs: Long, isPlaying: Boolean) {
        if (entries.isEmpty()) {
            clear()
            return
        }
        try {
            val array = JSONArray()
            entries.forEach { entry ->
                array.put(
                    JSONObject().apply {
                        put("id", entry.id)
                        put("uri", entry.uri)
                        put("title", entry.title)
                        put("artist", entry.artist)
                        put("album", entry.album)
                        put("artwork", entry.artworkUri ?: "")
                        put("duration", entry.durationMs)
                    }
                )
            }
            prefs.edit()
                .putString("entries", array.toString())
                .putInt("index", currentIndex)
                .putLong("position", positionMs)
                .putBoolean("playing", isPlaying)
                .apply()
        } catch (e: Exception) {
            AppLog.w("QueueStore", "No se pudo guardar la cola de reproducción", e)
        }
    }

    fun load(): SavedQueue? {
        return try {
            val raw = prefs.getString("entries", null) ?: return null
            val array = JSONArray(raw)
            if (array.length() == 0) return null
            val entries = (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                SavedQueueEntry(
                    id = obj.optLong("id", 0L),
                    uri = obj.optString("uri", ""),
                    title = obj.optString("title", "Canción"),
                    artist = obj.optString("artist", "Artista"),
                    album = obj.optString("album", ""),
                    artworkUri = obj.optString("artwork", "").ifBlank { null },
                    durationMs = obj.optLong("duration", 0L)
                )
            }.filter { it.uri.isNotBlank() }
            if (entries.isEmpty()) return null
            SavedQueue(
                entries = entries,
                currentIndex = prefs.getInt("index", 0).coerceIn(0, entries.lastIndex),
                positionMs = prefs.getLong("position", 0L),
                wasPlaying = prefs.getBoolean("playing", false)
            )
        } catch (e: Exception) {
            AppLog.w("QueueStore", "No se pudo leer la cola guardada", e)
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
