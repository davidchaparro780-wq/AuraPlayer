package com.auraplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserProfile(
    val email: String,
    val name: String,
    val avatarEmoji: String = "🎧",
    val memberTier: String = "DaVE VIP Melómano",
    val memberSince: String = "Septiembre 2026",
    val isGuest: Boolean = false,
    val lastSyncTime: Long = 0L
)

class UserManager(private val context: Context) {

    private val authPrefs: SharedPreferences = context.getSharedPreferences("dave_user_auth", Context.MODE_PRIVATE)
    private val cloudBackupPrefs: SharedPreferences = context.getSharedPreferences("dave_cloud_sync", Context.MODE_PRIVATE)

    var currentUser by mutableStateOf<UserProfile?>(null)
        private set

    val isLoggedIn: Boolean
        get() = currentUser != null

    init {
        loadSavedUser()
    }

    private fun loadSavedUser() {
        val email = authPrefs.getString("user_email", null)
        if (email != null) {
            val name = authPrefs.getString("user_name", "Melómano") ?: "Melómano"
            val emoji = authPrefs.getString("user_emoji", "🎧") ?: "🎧"
            val tier = authPrefs.getString("user_tier", "DaVE VIP Melómano") ?: "DaVE VIP Melómano"
            val since = authPrefs.getString("user_since", "Septiembre 2026") ?: "Septiembre 2026"
            val isGuest = authPrefs.getBoolean("user_is_guest", false)
            val syncTime = cloudBackupPrefs.getLong("sync_time_${email.hashCode()}", 0L)

            currentUser = UserProfile(
                email = email,
                name = name,
                avatarEmoji = emoji,
                memberTier = tier,
                memberSince = since,
                isGuest = isGuest,
                lastSyncTime = syncTime
            )
        }
    }

    fun login(email: String, password: String):Result<UserProfile> {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido"))
        }
        if (password.length < 4) {
            return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 4 caracteres"))
        }

        // Check if user was previously registered or create account seamlessly
        val registeredPassword = authPrefs.getString("pwd_${cleanEmail.lowercase()}", null)
        val name = if (registeredPassword != null) {
            if (registeredPassword != password) {
                return Result.failure(IllegalArgumentException("Contraseña incorrecta"))
            }
            authPrefs.getString("name_${cleanEmail.lowercase()}", cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }) ?: "Melómano"
        } else {
            // First time login with this email -> create profile
            val defaultName = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            authPrefs.edit()
                .putString("pwd_${cleanEmail.lowercase()}", password)
                .putString("name_${cleanEmail.lowercase()}", defaultName)
                .apply()
            defaultName
        }

        val emoji = authPrefs.getString("emoji_${cleanEmail.lowercase()}", "👑") ?: "👑"
        val currentDate = SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(Date()).replaceFirstChar { it.uppercase() }

        val profile = UserProfile(
            email = cleanEmail,
            name = name,
            avatarEmoji = emoji,
            memberTier = "DaVE VIP Melómano",
            memberSince = currentDate,
            isGuest = false,
            lastSyncTime = cloudBackupPrefs.getLong("sync_time_${cleanEmail.hashCode()}", System.currentTimeMillis())
        )

        saveSession(profile)
        currentUser = profile
        return Result.success(profile)
    }

    fun register(name: String, email: String, password: String, emoji: String = "🎧"): Result<UserProfile> {
        val cleanName = name.trim()
        val cleanEmail = email.trim()

        if (cleanName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Ingresa tu nombre o apodo"))
        }
        if (cleanEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(IllegalArgumentException("Ingresa un correo electrónico válido"))
        }
        if (password.length < 4) {
            return Result.failure(IllegalArgumentException("La contraseña debe tener al menos 4 caracteres"))
        }

        authPrefs.edit()
            .putString("pwd_${cleanEmail.lowercase()}", password)
            .putString("name_${cleanEmail.lowercase()}", cleanName)
            .putString("emoji_${cleanEmail.lowercase()}", emoji)
            .apply()

        val currentDate = SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(Date()).replaceFirstChar { it.uppercase() }

        val profile = UserProfile(
            email = cleanEmail,
            name = cleanName,
            avatarEmoji = emoji,
            memberTier = "DaVE VIP Melómano",
            memberSince = currentDate,
            isGuest = false,
            lastSyncTime = System.currentTimeMillis()
        )

        saveSession(profile)
        currentUser = profile
        return Result.success(profile)
    }

    fun guestLogin(): UserProfile {
        val profile = UserProfile(
            email = "invitado@daveplayer.app",
            name = "Melómano Invitado",
            avatarEmoji = "🎵",
            memberTier = "Pase de Invitado",
            memberSince = "Sesión Activa",
            isGuest = true,
            lastSyncTime = 0L
        )
        saveSession(profile)
        currentUser = profile
        return profile
    }

    fun updateProfile(name: String, avatarEmoji: String) {
        val user = currentUser ?: return
        val updated = user.copy(name = name, avatarEmoji = avatarEmoji)
        authPrefs.edit()
            .putString("name_${user.email.lowercase()}", name)
            .putString("emoji_${user.email.lowercase()}", avatarEmoji)
            .apply()
        saveSession(updated)
        currentUser = updated
    }

    fun logout() {
        authPrefs.edit()
            .remove("user_email")
            .remove("user_name")
            .remove("user_emoji")
            .remove("user_tier")
            .remove("user_since")
            .remove("user_is_guest")
            .apply()
        currentUser = null
    }

    private fun saveSession(profile: UserProfile) {
        authPrefs.edit()
            .putString("user_email", profile.email)
            .putString("user_name", profile.name)
            .putString("user_emoji", profile.avatarEmoji)
            .putString("user_tier", profile.memberTier)
            .putString("user_since", profile.memberSince)
            .putBoolean("user_is_guest", profile.isGuest)
            .apply()
    }

    // --- Cloud Sync Simulation (Backup & Restore Favorites & Playlists) ---

    fun backupUserData(favoriteIds: Set<Long>, playlists: List<Playlist>): Boolean {
        val email = currentUser?.email ?: return false
        val now = System.currentTimeMillis()
        val key = email.hashCode().toString()

        val favsArray = JSONArray(favoriteIds.toList())

        val playlistsArray = JSONArray()
        for (pl in playlists) {
            val plObj = JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("songIds", JSONArray(pl.songIds))
            }
            playlistsArray.put(plObj)
        }

        cloudBackupPrefs.edit()
            .putString("favs_$key", favsArray.toString())
            .putString("playlists_$key", playlistsArray.toString())
            .putLong("sync_time_$key", now)
            .apply()

        currentUser = currentUser?.copy(lastSyncTime = now)
        return true
    }

    fun restoreUserData(): Pair<Set<Long>, List<Playlist>>? {
        val email = currentUser?.email ?: return null
        val key = email.hashCode().toString()

        val favsStr = cloudBackupPrefs.getString("favs_$key", null)
        val playlistsStr = cloudBackupPrefs.getString("playlists_$key", null)

        if (favsStr == null && playlistsStr == null) return null

        val favSet = mutableSetOf<Long>()
        if (favsStr != null) {
            try {
                val arr = JSONArray(favsStr)
                for (i in 0 until arr.length()) {
                    favSet.add(arr.getLong(i))
                }
            } catch (_: Exception) {}
        }

        val plList = mutableListOf<Playlist>()
        if (playlistsStr != null) {
            try {
                val arr = JSONArray(playlistsStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val songIdsArr = obj.getJSONArray("songIds")
                    val songIds = mutableListOf<Long>()
                    for (j in 0 until songIdsArr.length()) {
                        songIds.add(songIdsArr.getLong(j))
                    }
                    plList.add(Playlist(id, name, songIds))
                }
            } catch (_: Exception) {}
        }

        return Pair(favSet, plList)
    }
}
