package com.auraplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SearchHistoryManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("aura_search_history", Context.MODE_PRIVATE)

    var historyList by mutableStateOf(loadHistory())
        private set

    private fun loadHistory(): List<String> {
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        return if (raw.isBlank()) emptyList()
        else raw.split(SEPARATOR).filter { it.isNotBlank() }
    }

    fun addQuery(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return

        val current = loadHistory().toMutableList()
        current.removeAll { it.equals(clean, ignoreCase = true) }
        current.add(0, clean)
        val trimmed = current.take(MAX_ITEMS)

        prefs.edit().putString(KEY_HISTORY, trimmed.joinToString(SEPARATOR)).apply()
        historyList = trimmed
    }

    fun removeQuery(query: String) {
        val current = loadHistory().toMutableList()
        current.removeAll { it.equals(query, ignoreCase = true) }
        prefs.edit().putString(KEY_HISTORY, current.joinToString(SEPARATOR)).apply()
        historyList = current
    }

    fun clear() {
        prefs.edit().remove(KEY_HISTORY).apply()
        historyList = emptyList()
    }

    companion object {
        private const val KEY_HISTORY = "recent_queries"
        private const val SEPARATOR = "|||#"
        private const val MAX_ITEMS = 6

        @Volatile
        private var INSTANCE: SearchHistoryManager? = null

        fun getInstance(context: Context): SearchHistoryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SearchHistoryManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
