package com.auraplayer.audio

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.auraplayer.service.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MusicAlarm(
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarma DaVE",
    val isEnabled: Boolean = true
)

class MusicAlarmManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("dave_alarms", Context.MODE_PRIVATE)

    private val _alarms = MutableStateFlow<List<MusicAlarm>>(emptyList())
    val alarms: StateFlow<List<MusicAlarm>> = _alarms.asStateFlow()

    init {
        loadAlarms()
    }

    fun setAlarm(alarm: MusicAlarm) {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, alarm.hour)
            set(java.util.Calendar.MINUTE, alarm.minute)
            set(java.util.Calendar.SECOND, 0)
            // If time has passed today, set for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, MusicAlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_label", alarm.label)
        }
        val pi = PendingIntent.getBroadcast(
            context, alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }

        saveAlarm(alarm)
    }

    fun cancelAlarm(alarmId: Int) {
        val intent = Intent(context, MusicAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, alarmId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
        deleteAlarm(alarmId)
    }

    private fun saveAlarm(alarm: MusicAlarm) {
        val existing = _alarms.value.filter { it.id != alarm.id }.toMutableList()
        existing.add(alarm)
        _alarms.value = existing
        prefs.edit()
            .putString("alarm_${alarm.id}", "${alarm.hour}:${alarm.minute}:${alarm.label}")
            .putInt("alarm_count", existing.size)
            .apply()
    }

    private fun deleteAlarm(id: Int) {
        val existing = _alarms.value.filter { it.id != id }
        _alarms.value = existing
        prefs.edit().remove("alarm_$id").apply()
    }

    private fun loadAlarms() {
        val count = prefs.getInt("alarm_count", 0)
        val loaded = mutableListOf<MusicAlarm>()
        for (i in 0 until count) {
            val raw = prefs.getString("alarm_$i", null) ?: continue
            val parts = raw.split(":")
            if (parts.size >= 2) {
                loaded.add(MusicAlarm(
                    id = i,
                    hour = parts[0].toIntOrNull() ?: 8,
                    minute = parts[1].toIntOrNull() ?: 0,
                    label = parts.getOrNull(2) ?: "Alarma DaVE"
                ))
            }
        }
        _alarms.value = loaded
    }

    fun formatTime(hour: Int, minute: Int): String =
        String.format("%02d:%02d", hour, minute)
}

class MusicAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmLabel = intent.getStringExtra("alarm_label") ?: "Alarma DaVE"
        val channelId = "dave_alarm_channel"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val channel = NotificationChannel(
                channelId,
                "Alarmas DaVE Player",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de alarma musical"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, Class.forName("com.auraplayer.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("alarm_wake", true)
            putExtra("alarm_label", alarmLabel)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            intent.getIntExtra("alarm_id", 0),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $alarmLabel")
            .setContentText("¡Hora de despertar! Toca para abrir tu música en DaVE Player.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        notificationManager?.notify(1001 + intent.getIntExtra("alarm_id", 0), notification)

        // Reproducir suavemente si el reproductor está activo
        try {
            PlaybackService.activePlayer?.let { player ->
                if (!player.isPlaying) {
                    player.play()
                }
            }
        } catch (_: Exception) {}

        try {
            context.startActivity(launchIntent)
        } catch (_: Exception) {}
    }
}
