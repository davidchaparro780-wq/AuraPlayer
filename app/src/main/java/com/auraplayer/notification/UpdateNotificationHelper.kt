package com.auraplayer.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.auraplayer.MainActivity
import com.auraplayer.R
import com.auraplayer.data.repository.SettingsManager
import com.auraplayer.data.repository.UpdateInfo

object UpdateNotificationHelper {

    const val CHANNEL_ID = "dave_updates_channel"
    const val NOTIFICATION_ID = 2026

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Actualizaciones de DaVE"
            val descriptionText = "Notificaciones cuando haya nuevas versiones y mejoras disponibles"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUpdateNotification(context: Context, updateInfo: UpdateInfo) {
        val settings = SettingsManager.getInstance(context)
        if (!settings.notifyUpdates) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_SHOW_UPDATE"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("UPDATE_VERSION_NAME", updateInfo.versionName)
            putExtra("UPDATE_CHANGELOG", updateInfo.changelog)
            putExtra("UPDATE_DOWNLOAD_URL", updateInfo.downloadUrl)
            putExtra("UPDATE_FILE_SIZE", updateInfo.fileSizeMb)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("✨ ¡Nueva actualización de DaVE Player!")
            .setContentText("Versión ${updateInfo.versionName} lista para instalar. Toca aquí para actualizar.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("✨ ¡DaVE Player ${updateInfo.versionName} ya está disponible!\n\n${updateInfo.changelog}\n\nToca aquí para instalar la actualización con un toque.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }
}
