package com.auraplayer.audio

import android.content.Context
import android.content.SharedPreferences

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val progress: Float
)

class AchievementManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dave_achievements", Context.MODE_PRIVATE)

    fun addExp(amount: Int) {
        val current = prefs.getInt("total_exp", 0)
        prefs.edit().putInt("total_exp", current + amount).apply()
    }

    fun recordSongPlayed(isFlac: Boolean = false) {
        val total = prefs.getInt("songs_played", 0) + 1
        val flacTotal = prefs.getInt("flac_played", 0) + (if (isFlac) 1 else 0)
        val edit = prefs.edit()
            .putInt("songs_played", total)
            .putInt("flac_played", flacTotal)

        // Check late night (between 1 AM and 5 AM)
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour in 1..4) {
            val late = prefs.getInt("late_night_count", 0) + 1
            edit.putInt("late_night_count", late)
        }

        edit.apply()
        addExp(10)
    }

    fun recordQuizWin(score: Int) {
        val high = prefs.getInt("quiz_high_score", 0)
        if (score > high) {
            prefs.edit().putInt("quiz_high_score", score).apply()
        }
        addExp(score * 5)
    }

    val totalExp: Int get() = prefs.getInt("total_exp", 0)

    val listenerLevel: Int get() = (totalExp / 100) + 1

    val levelTitle: String get() = when {
        listenerLevel >= 15 -> "Audiófilo Leyenda 👑"
        listenerLevel >= 10 -> "Maestro del Sonido 🎧"
        listenerLevel >= 5 -> "Melómano Experto 🎵"
        listenerLevel >= 2 -> "Explorador Musical 🚀"
        else -> "Novato Acústico 🐣"
    }

    fun getAchievements(): List<Achievement> {
        val songs = prefs.getInt("songs_played", 0)
        val flacs = prefs.getInt("flac_played", 0)
        val late = prefs.getInt("late_night_count", 0)
        val quiz = prefs.getInt("quiz_high_score", 0)

        return listOf(
            Achievement("first_step", "Primer Acorde", "Reproduce tu primera canción", "🎵", songs >= 1, (songs / 1f).coerceIn(0f, 1f)),
            Achievement("marathon", "Maratón Sonora", "Reproduce 50 canciones", "🏃", songs >= 50, (songs / 50f).coerceIn(0f, 1f)),
            Achievement("centurion", "El Centurión", "Alcanza 100 canciones escuchadas", "💯", songs >= 100, (songs / 100f).coerceIn(0f, 1f)),
            Achievement("night_owl", "Noctámbulo de DaVE", "Escucha música entre 1 AM y 5 AM (5 veces)", "🦉", late >= 5, (late / 5f).coerceIn(0f, 1f)),
            Achievement("audiophile", "Pureza Lossless", "Disfruta de 10 pistas FLAC en alta fidelidad", "💎", flacs >= 10, (flacs / 10f).coerceIn(0f, 1f)),
            Achievement("quiz_master", "Genio Musical", "Consigue 50 puntos o más en DaVE Quiz", "🧠", quiz >= 50, (quiz / 50f).coerceIn(0f, 1f)),
            Achievement("exp_hunter", "Coleccionista de EXP", "Acumula 500 puntos de experiencia", "⭐", totalExp >= 500, (totalExp / 500f).coerceIn(0f, 1f)),
            Achievement("legend", "Nivel Legendario", "Alcanza el nivel 10 de oyente", "👑", listenerLevel >= 10, (listenerLevel / 10f).coerceIn(0f, 1f))
        )
    }
}
