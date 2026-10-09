package com.auraplayer

import android.app.Application
import com.auraplayer.notification.UpdateNotificationHelper
import com.auraplayer.worker.UpdateWorker

class DaVEApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Crear canal de notificación de actualizaciones
        UpdateNotificationHelper.createNotificationChannel(this)
        // Iniciar detector periódico en segundo plano
        UpdateWorker.enqueuePeriodicWork(this)
    }
}
