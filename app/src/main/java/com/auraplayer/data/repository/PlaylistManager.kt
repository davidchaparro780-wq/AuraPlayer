package com.auraplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.auraplayer.data.model.MediaModel
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long>
)

data class TagOverride(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String = "",
    val year: String = ""
)

class PlaylistManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("aura_playlists_store", Context.MODE_PRIVATE)

    // --- PLAY COUNT & HISTORY (AIMP / Pulsar Smart Playlists) ---

    fun recordPlay(songId: Long) {
        val currentCount = prefs.getInt("count_$songId", 0)
        prefs.edit().putInt("count_$songId", currentCount + 1).apply()

        // Update recently played list (max 50)
        val history = getRecentlyPlayedIds().toMutableList()
        history.remove(songId)
        history.add(0, songId)
        if (history.size > 50) {
            history.removeAt(history.size - 1)
        }
        val jsonArray = JSONArray(history)
        prefs.edit().putString("recent_history", jsonArray.toString()).apply()
    }

    fun getPlayCount(songId: Long): Int {
        return prefs.getInt("count_$songId", 0)
    }

    fun getRecentlyPlayedIds(): List<Long> {
        val jsonStr = prefs.getString("recent_history", "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Long>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getLong(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Smart Playlist: Detects the user's most listened to songs based on play counts.
     * Returns the tracks ordered by play count descending.
     */
    fun getMostPlayedSongs(allSongs: List<MediaModel>, limit: Int = 30): List<MediaModel> {
        if (allSongs.isEmpty()) return emptyList()
        val songsWithCounts = allSongs.map { song ->
            song to getPlayCount(song.id)
        }
        val played = songsWithCounts
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }

        return if (played.isNotEmpty()) {
            played.take(limit)
        } else {
            // If no play counts recorded yet, provide first batch as initial recommendation
            allSongs.take(minOf(limit, allSongs.size))
        }
    }

    // --- CUSTOM PLAYLISTS (Musicolet / Poweramp Style) ---

    fun getPlaylists(): List<Playlist> {
        val jsonStr = prefs.getString("playlists_data", "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Playlist>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val songsArr = obj.getJSONArray("songs")
                val songs = mutableListOf<Long>()
                for (j in 0 until songsArr.length()) {
                    songs.add(songsArr.getLong(j))
                }
                list.add(Playlist(id, name, songs))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun createPlaylist(name: String): Playlist {
        val current = getPlaylists().toMutableList()
        val id = System.currentTimeMillis().toString()
        val newPl = Playlist(id, name, emptyList())
        current.add(0, newPl)
        savePlaylists(current)
        return newPl
    }

    fun deletePlaylist(id: String) {
        val current = getPlaylists().filter { it.id != id }
        savePlaylists(current)
    }

    fun addSongToPlaylist(playlistId: String, songId: Long): Boolean {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val pl = current[index]
            if (!pl.songIds.contains(songId)) {
                val updatedSongs = pl.songIds + songId
                current[index] = pl.copy(songIds = updatedSongs)
                savePlaylists(current)
                return true
            }
        }
        return false
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        val current = getPlaylists().toMutableList()
        val index = current.indexOfFirst { it.id == playlistId }
        if (index != -1) {
            val pl = current[index]
            val updatedSongs = pl.songIds.filter { it != songId }
            current[index] = pl.copy(songIds = updatedSongs)
            savePlaylists(current)
        }
    }

    fun savePlaylists(playlists: List<Playlist>) {
        val jsonArray = JSONArray()
        playlists.forEach { pl ->
            val obj = JSONObject()
            obj.put("id", pl.id)
            obj.put("name", pl.name)
            obj.put("songs", JSONArray(pl.songIds))
            jsonArray.put(obj)
        }
        prefs.edit().putString("playlists_data", jsonArray.toString()).apply()
    }

    // --- TAG EDITOR PERSISTENCE (Pulsar / Musicolet Style) ---

    fun saveTagOverride(songId: Long, title: String, artist: String, album: String, genre: String = "", year: String = "") {
        val obj = JSONObject().apply {
            put("title", title)
            put("artist", artist)
            put("album", album)
            put("genre", genre)
            put("year", year)
        }
        prefs.edit().putString("tag_$songId", obj.toString()).apply()
    }

    fun getTagOverride(songId: Long): TagOverride? {
        val jsonStr = prefs.getString("tag_$songId", null) ?: return null
        return try {
            val obj = JSONObject(jsonStr)
            TagOverride(
                title = obj.optString("title", ""),
                artist = obj.optString("artist", ""),
                album = obj.optString("album", ""),
                genre = obj.optString("genre", ""),
                year = obj.optString("year", "")
            )
        } catch (e: Exception) {
            null
        }
    }

    // --- COPIA DE SEGURIDAD & RESTAURACIÓN (.davebackup JSON) ---

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("playlists", JSONArray(prefs.getString("playlists_data", "[]")))

        val tagsObj = JSONObject()
        prefs.all.forEach { (k, v) ->
            if (k.startsWith("tag_") && v is String) {
                tagsObj.put(k, v)
            }
        }
        root.put("tags", tagsObj)
        root.put("theme_accent", getThemeAccent())
        root.put("crossfade_sec", getCrossfadeSeconds())
        return root.toString(2)
    }

    fun importBackupJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val editor = prefs.edit()
            if (root.has("playlists")) {
                editor.putString("playlists_data", root.getJSONArray("playlists").toString())
            }
            if (root.has("tags")) {
                val tagsObj = root.getJSONObject("tags")
                val keys = tagsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    editor.putString(k, tagsObj.getString(k))
                }
            }
            if (root.has("theme_accent")) {
                editor.putString("theme_accent", root.getString("theme_accent"))
            }
            if (root.has("crossfade_sec")) {
                editor.putInt("audio_crossfade_sec", root.getInt("crossfade_sec"))
            }
            editor.apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- DJ CROSSFADE SETTINGS (Poweramp Style) ---

    fun getCrossfadeSeconds(): Int {
        return prefs.getInt("audio_crossfade_sec", 0) // 0 means off
    }

    fun setCrossfadeSeconds(sec: Int) {
        prefs.edit().putInt("audio_crossfade_sec", sec).apply()
    }

    // --- CYBER THEME ACCENT (BlackPlayer Style) ---

    fun getThemeAccent(): String {
        return prefs.getString("theme_accent", "PURPLE") ?: "PURPLE"
    }

    fun setThemeAccent(accent: String) {
        prefs.edit().putString("theme_accent", accent).apply()
    }

    // --- SPOTIFY REPLAYGAIN & AUTOPLAY ---

    fun isReplayGainEnabled(): Boolean {
        return prefs.getBoolean("audio_replay_gain", true)
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("audio_replay_gain", enabled).apply()
    }

    fun isAutoplayEnabled(): Boolean {
        return prefs.getBoolean("audio_autoplay_radio", true)
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("audio_autoplay_radio", enabled).apply()
    }

    // --- DOWNLOAD QUALITY SETTING (320 kbps Hi-Fi vs 160 kbps Fast) ---

    fun getDownloadQuality(): String {
        return prefs.getString("download_quality", "320") ?: "320"
    }

    fun setDownloadQuality(quality: String) {
        prefs.edit().putString("download_quality", quality).apply()
    }
}
