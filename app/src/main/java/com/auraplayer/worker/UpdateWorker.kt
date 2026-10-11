package com.auraplayer.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.auraplayer.data.repository.UpdateManager
import com.auraplayer.util.AppLog
import java.util.concurrent.TimeUnit

class UpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            AppLog.d(TAG, "Ejecutando comprobación de actualización periódica en segundo plano...")
            val updateManager = UpdateManager(applicationContext)
            val info = updateManager.checkForUpdate()
            if (info != null) {
                AppLog.d(TAG, "¡Actualización encontrada! Notificación enviada: ${info.versionName}")
            } else {
                AppLog.d(TAG, "La aplicación está en la última versión.")
            }
            Result.success()
        } catch (e: Exception) {
            AppLog.e(TAG, "Error durante la comprobación de actualización: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "UpdateWorker"
        private const val WORK_NAME = "dave_periodic_update_checker"

        fun enqueuePeriodicWork(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val periodicWork = PeriodicWorkRequestBuilder<UpdateWorker>(
                    4, TimeUnit.HOURS,
                    30, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWork
                )
                AppLog.d(TAG, "Trabajo periódico de actualización configurado (cada 4h con conexión)")
            } catch (e: Exception) {
                AppLog.e(TAG, "Error configurando WorkManager: ${e.message}", e)
            }
        }
    }
}
