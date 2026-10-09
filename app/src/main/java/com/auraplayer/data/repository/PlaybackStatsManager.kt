package com.auraplayer.data.repository

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ArtistStat(
    val artistName: String,
    val playMinutes: Int
)

/**
 * Gestor de Estadísticas y Métricas Musicales en Tiempo Real (DaVE Insights).
 * Mide minutos reproducidos diarios, hora pico de escucha y artistas más sonados.
 */
class PlaybackStatsManager(context: Context) {

    private val prefs = context.getSharedPreferences("dave_insights_store", Context.MODE_PRIVATE)

    private fun getTodayKey(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    }

    /**
     * Registra segundos de reproducción activa para una pista y artista
     */
    fun recordListeningTime(artist: String, seconds: Int) {
        if (seconds <= 0) return

        val todayKey = "secs_${getTodayKey()}"
        val currentTodaySecs = prefs.getInt(todayKey, 0)
        prefs.edit().putInt(todayKey, currentTodaySecs + seconds).apply()

        // Total histórico
        val totalSecs = prefs.getInt("total_secs_all_time", 0)
        prefs.edit().putInt("total_secs_all_time", totalSecs + seconds).apply()

        // Por hora del día (0..23)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val hourKey = "hour_$hour"
        val hourSecs = prefs.getInt(hourKey, 0)
        prefs.edit().putInt(hourKey, hourSecs + seconds).apply()

        // Por artista
        val cleanArtist = artist.trim().ifEmpty { "Desconocido" }
        val artistKey = "artist_$cleanArtist"
        val artistSecs = prefs.getInt(artistKey, 0)
        prefs.edit().putInt(artistKey, artistSecs + seconds).apply()
    }

    fun getTodayListeningMinutes(): Int {
        val todayKey = "secs_${getTodayKey()}"
        return (prefs.getInt(todayKey, 0) / 60)
    }

    fun getTotalListeningHours(): Float {
        return (prefs.getInt("total_secs_all_time", 0) / 3600f)
    }

    /**
     * Retorna la hora pico de escucha más activa del usuario (formato "20:00 - 21:00")
     */
    fun getPeakListeningHour(): String {
        var maxHour = 20
        var maxSecs = -1
        for (h in 0..23) {
            val secs = prefs.getInt("hour_$h", 0)
            if (secs > maxSecs) {
                maxSecs = secs
                maxHour = h
            }
        }
        val endHour = (maxHour + 1) % 24
        return String.format("%02d:00 - %02d:00", maxHour, endHour)
    }

    /**
     * Retorna el top de artistas más escuchados
     */
    fun getTopArtists(limit: Int = 5): List<ArtistStat> {
        val all = prefs.all
        val list = mutableListOf<ArtistStat>()
        all.forEach { (k, v) ->
            if (k.startsWith("artist_") && v is Int) {
                val artistName = k.removePrefix("artist_")
                val minutes = v / 60
                if (minutes > 0) {
                    list.add(ArtistStat(artistName, minutes))
                }
            }
        }
        return list.sortedByDescending { it.playMinutes }.take(limit)
    }

    companion object {
        @Volatile
        private var instance: PlaybackStatsManager? = null

        fun getInstance(context: Context): PlaybackStatsManager {
            return instance ?: synchronized(this) {
                instance ?: PlaybackStatsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
