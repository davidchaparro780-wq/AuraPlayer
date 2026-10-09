package com.auraplayer.data.repository

import android.content.Context
import com.auraplayer.data.model.MediaModel
import org.json.JSONArray

/**
 * Gestor de Filtros Inteligentes de Carpetas & Audios de WhatsApp/Telegram (Folder Shield).
 * Oculta automáticamente audios cortos de mensajería (< 30s), notas de voz y tonos de llamada
 * para que no contaminen la biblioteca musical del usuario.
 */
class FolderShieldManager(context: Context) {

    private val prefs = context.getSharedPreferences("dave_folder_shield_prefs", Context.MODE_PRIVATE)

    var isShortTracksFilterEnabled: Boolean
        get() = prefs.getBoolean("filter_short_tracks", true)
        set(value) = prefs.edit().putBoolean("filter_short_tracks", value).apply()

    var minTrackDurationSeconds: Int
        get() = prefs.getInt("min_duration_seconds", 30)
        set(value) = prefs.edit().putInt("min_duration_seconds", value.coerceIn(5, 120)).apply()

    var isWhatsAppFilterEnabled: Boolean
        get() = prefs.getBoolean("filter_whatsapp", true)
        set(value) = prefs.edit().putBoolean("filter_whatsapp", value).apply()

    var isTelegramFilterEnabled: Boolean
        get() = prefs.getBoolean("filter_telegram", true)
        set(value) = prefs.edit().putBoolean("filter_telegram", value).apply()

    var isRingtonesFilterEnabled: Boolean
        get() = prefs.getBoolean("filter_ringtones", true)
        set(value) = prefs.edit().putBoolean("filter_ringtones", value).apply()

    // Lista negra de carpetas personalizadas
    fun getBlacklistedFolders(): Set<String> {
        val json = prefs.getString("blacklisted_folders", "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            val set = mutableSetOf<String>()
            for (i in 0 until arr.length()) {
                set.add(arr.getString(i))
            }
            set
        } catch (_: Exception) {
            emptySet()
        }
    }

    fun setFolderBlacklisted(folderName: String, blacklisted: Boolean) {
        val current = getBlacklistedFolders().toMutableSet()
        if (blacklisted) current.add(folderName)
        else current.remove(folderName)
        prefs.edit().putString("blacklisted_folders", JSONArray(current).toString()).apply()
    }

    /**
     * Aplica los filtros de Folder Shield sobre una lista de canciones
     */
    fun filterSongs(allSongs: List<MediaModel>): List<MediaModel> {
        val minDurationMs = minTrackDurationSeconds * 1000L
        val blacklisted = getBlacklistedFolders().map { it.lowercase() }.toSet()

        return allSongs.filter { song ->
            // Filtro 1: Duración mínima (ignorar audios menores a 30s)
            if (isShortTracksFilterEnabled && song.duration in 1 until minDurationMs) {
                return@filter false
            }

            val pathLower = song.path.lowercase()
            val folderLower = song.folderName.lowercase()

            // Filtro 2: WhatsApp Voice Notes / Audio
            if (isWhatsAppFilterEnabled && (
                pathLower.contains("whatsapp audio") ||
                pathLower.contains("whatsapp voice notes") ||
                pathLower.contains(".opus") ||
                folderLower.contains("whatsapp")
            )) {
                return@filter false
            }

            // Filtro 3: Telegram Audio
            if (isTelegramFilterEnabled && (
                pathLower.contains("telegram audio") ||
                folderLower.contains("telegram")
            )) {
                return@filter false
            }

            // Filtro 4: Tonos de llamada / Notificaciones del sistema
            if (isRingtonesFilterEnabled && (
                pathLower.contains("/ringtones") ||
                pathLower.contains("/notifications") ||
                pathLower.contains("/alarms") ||
                folderLower == "ringtones" ||
                folderLower == "notifications"
            )) {
                return@filter false
            }

            // Filtro 5: Carpetas en lista negra personalizada
            if (blacklisted.contains(folderLower)) {
                return@filter false
            }

            true
        }
    }

    companion object {
        @Volatile
        private var instance: FolderShieldManager? = null

        fun getInstance(context: Context): FolderShieldManager {
            return instance ?: synchronized(this) {
                instance ?: FolderShieldManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
