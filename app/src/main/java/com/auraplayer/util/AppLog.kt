package com.auraplayer.util

import android.util.Log

/**
 * Logger centralizado de Aura Player.
 *
 * Envoltorio fino sobre [android.util.Log] con un tag común ("Aura") para poder
 * filtrar toda la traza de la app de golpe (`adb logcat -s Aura:*`). El origen
 * concreto se marca dentro del mensaje: `[Vault] mensaje`.
 *
 * Regla del proyecto: ningún bloque `catch` debe quedar vacío. Si el fallo es
 * benigno (cerrar un socket, liberar un recurso) se registra con [d] o [w];
 * si rompe una función visible para el usuario se registra con [e] incluyendo
 * siempre la excepción para conservar el stack trace.
 */
object AppLog {

    private const val TAG = "Aura"

    fun v(tag: String, message: String, error: Throwable? = null) {
        Log.v(TAG, "[$tag] $message", error)
    }

    fun d(tag: String, message: String, error: Throwable? = null) {
        Log.d(TAG, "[$tag] $message", error)
    }

    fun i(tag: String, message: String, error: Throwable? = null) {
        Log.i(TAG, "[$tag] $message", error)
    }

    fun w(tag: String, message: String, error: Throwable? = null) {
        Log.w(TAG, "[$tag] $message", error)
    }

    fun e(tag: String, message: String, error: Throwable? = null) {
        Log.e(TAG, "[$tag] $message", error)
    }
}
