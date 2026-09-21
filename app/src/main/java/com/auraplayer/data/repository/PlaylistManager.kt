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
    val album: String
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

    private fun savePlaylists(playlists: List<Playlist>) {
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

    fun saveTagOverride(songId: Long, title: String, artist: String, album: String) {
        val obj = JSONObject().apply {
            put("title", title)
            put("artist", artist)
            put("album", album)
        }
        prefs.edit().putString("tag_$songId", obj.toString()).apply()
    }

    fun getTagOverride(songId: Long): TagOverride? {
        val jsonStr = prefs.getString("tag_$songId", null) ?: return null
        return try {
            val obj = JSONObject(jsonStr)
            TagOverride(
                title = obj.getString("title"),
                artist = obj.getString("artist"),
                album = obj.getString("album")
            )
        } catch (e: Exception) {
            null
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
}
