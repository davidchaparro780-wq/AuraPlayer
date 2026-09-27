package com.auraplayer.audio

import android.content.Context
import com.auraplayer.data.model.MediaModel
import kotlin.math.abs

data class BpmGroup(
    val category: String,
    val icon: String,
    val minBpm: Int,
    val maxBpm: Int,
    val description: String,
    val songs: List<MediaModel>
)

object BpmDetector {

    fun getEstimatedBpm(song: MediaModel, context: Context): Int {
        val prefs = context.getSharedPreferences("dave_bpm_cache", Context.MODE_PRIVATE)
        val key = "bpm_${song.id}"
        val cached = prefs.getInt(key, 0)
        if (cached > 0) return cached

        // Derive consistent realistic BPM from title, artist and duration
        val seed = abs((song.title + song.artist + song.duration).hashCode())
        val calculatedBpm = when {
            song.title.contains("remix", true) || song.title.contains("dance", true) || song.title.contains("edm", true) -> 128 + (seed % 32) // 128-160
            song.title.contains("slow", true) || song.title.contains("lofi", true) || song.title.contains("relax", true) -> 70 + (seed % 25) // 70-95
            song.title.contains("rock", true) || song.title.contains("metal", true) || song.title.contains("punk", true) -> 135 + (seed % 40) // 135-175
            song.title.contains("reggaeton", true) || song.title.contains("trap", true) || song.title.contains("rap", true) -> 90 + (seed % 35) // 90-125
            else -> 85 + (seed % 65) // 85-150
        }

        prefs.edit().putInt(key, calculatedBpm).apply()
        return calculatedBpm
    }

    fun classifyLibrary(songs: List<MediaModel>, context: Context): List<BpmGroup> {
        val cardio = mutableListOf<MediaModel>()
        val gym = mutableListOf<MediaModel>()
        val chill = mutableListOf<MediaModel>()

        songs.forEach { song ->
            val bpm = getEstimatedBpm(song, context)
            when {
                bpm >= 145 -> cardio.add(song)
                bpm in 115..144 -> gym.add(song)
                else -> chill.add(song)
            }
        }

        return listOf(
            BpmGroup("Cardio & Running", "⚡", 145, 180, "Ritmo rápido para correr y zancadas activas", cardio),
            BpmGroup("Gimnasio & Motivación", "💪", 115, 144, "Golpe constante y energía para entrenamiento de fuerza", gym),
            BpmGroup("Relax & Caminata", "🧘", 60, 114, "Tempo suave para estudiar, calmarse o caminar", chill)
        )
    }
}
