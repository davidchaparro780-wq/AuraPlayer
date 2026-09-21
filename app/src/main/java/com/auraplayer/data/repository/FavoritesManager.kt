package com.auraplayer.data.repository

import android.content.Context
import android.content.SharedPreferences

class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aura_favorites", Context.MODE_PRIVATE)

    fun isFavorite(songId: Long): Boolean {
        return prefs.getBoolean("fav_$songId", false)
    }

    fun toggleFavorite(songId: Long): Boolean {
        val current = isFavorite(songId)
        prefs.edit().putBoolean("fav_$songId", !current).apply()
        return !current
    }

    fun getFavoriteIds(): Set<Long> {
        return prefs.all.filter { it.key.startsWith("fav_") && it.value == true }
            .mapNotNull { it.key.removePrefix("fav_").toLongOrNull() }
            .toSet()
    }
}
