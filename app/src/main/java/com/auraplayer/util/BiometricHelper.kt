package com.auraplayer.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val biometricManager = context.getSystemService(BiometricManager::class.java)
                biometricManager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val fingerprintManager = context.getSystemService(FingerprintManager::class.java)
                fingerprintManager != null && fingerprintManager.isHardwareDetected && fingerprintManager.hasEnrolledFingerprints()
            } else {
                false
            }
        } catch (_: Exception) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        }
    }

    fun authenticate(
        activity: Activity,
        title: String = "Bóveda Privada DaVE",
        subtitle: String = "Toca el sensor de huella digital para desbloquear",
        negativeButtonText: String = "Ingresar PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ): CancellationSignal? {
        if (!isBiometricAvailable(activity)) {
            onError("Autenticación biométrica no disponible")
            return null
        }

        val cancellationSignal = CancellationSignal()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton(negativeButtonText, executor) { _, _ ->
                    onCancel()
                }
                .build()

            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                        ) {
                            onCancel()
                        } else {
                            onError(errString?.toString() ?: "Error de autenticación biométrica")
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Huella no reconocida. Intenta nuevamente.")
                    }
                }
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            val fingerprintManager = activity.getSystemService(FingerprintManager::class.java)
            if (fingerprintManager != null) {
                fingerprintManager.authenticate(
                    null,
                    cancellationSignal,
                    0,
                    object : FingerprintManager.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            onError(errString?.toString() ?: "Error en lector de huellas")
                        }

                        override fun onAuthenticationFailed() {
                            onError("Huella no reconocida. Intenta de nuevo.")
                        }
                    },
                    null
                )
            } else {
                onError("Lector de huellas no disponible")
            }
        }

        return cancellationSignal
    }
}
