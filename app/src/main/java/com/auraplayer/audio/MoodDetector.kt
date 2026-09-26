package com.auraplayer.audio

import android.content.Context
import com.auraplayer.data.model.MediaModel
import com.auraplayer.data.repository.SongLyrics

/**
 * MoodDetector monitors late-night listening patterns and detects when the user
 * might be going through a tough emotional moment based on song lyrics.
 */
class MoodDetector(private val context: Context) {

    private val sadKeywords = listOf(
        "llorar", "lloro", "llorando", "dolor", "duele", "duele", "sufrir", "sufriendo",
        "sin ti", "adiós", "adiós", "olvidar", "olvidaste", "olvidé", "perdí", "perdiste",
        "soledad", "solo", "sola", "triste", "tristeza", "vacío", "vacía", "roto", "rota",
        "deprimido", "deprimida", "oscuridad", "oscuro", "alone", "lonely", "broken",
        "crying", "tears", "heartbreak", "pain", "hurt", "missing", "lost", "empty",
        "sad", "sadness", "sorrow", "desolate", "numb", "hopeless", "goodbye", "regret"
    )

    private val sadSongsHistory = ArrayDeque<Long>() // timestamps of sad song detections

    fun analyzeLyrics(lyrics: SongLyrics?): Boolean {
        if (lyrics == null) return false
        val allText = (lyrics.lines.joinToString(" ") { it.text }).lowercase()
        val matchCount = sadKeywords.count { keyword -> allText.contains(keyword) }
        return matchCount >= 2 // At least 2 keywords to avoid false positives
    }

    fun recordSadSong() {
        sadSongsHistory.addLast(System.currentTimeMillis())
        // Keep only last 10 entries
        while (sadSongsHistory.size > 10) sadSongsHistory.removeFirst()
    }

    fun isInMoodCrisis(): Boolean {
        val now = System.currentTimeMillis()
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isLateNight = hour in 0..3 || hour in 23..23

        // 5+ sad songs in the last 30 minutes late at night
        val recentSadCount = sadSongsHistory.count { now - it < 30 * 60 * 1000L }
        return isLateNight && recentSadCount >= 5
    }

    fun getSupportMessages(): List<String> = listOf(
        "Oye… ¿estás bien? DaVE está aquí contigo 💙",
        "Las noches difíciles siempre terminan. Tú puedes 🌙✨",
        "Escuchar música triste es válido. Pero también mereces canciones que te levanten 🎵",
        "Recuerda: mañana es un día nuevo. Aquí estoy para acompañarte 💫",
        "Eres más fuerte de lo que crees. DaVE cree en ti 🔥"
    )

    fun getRandomSupportMessage() = getSupportMessages().random()
}
