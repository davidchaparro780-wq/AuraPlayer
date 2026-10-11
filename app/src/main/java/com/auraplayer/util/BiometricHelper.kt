package com.auraplayer.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val biometricManager = context.getSystemService(BiometricManager::class.java)
                biometricManager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val fingerprintManager = FingerprintManagerCompat.from(context)
                fingerprintManager.isHardwareDetected && fingerprintManager.hasEnrolledFingerprints()
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
        } else {
            // Android 6.0 to 8.1 fallback via FingerprintManagerCompat
            val fingerprintManager = FingerprintManagerCompat.from(activity)
            val androidxSignal = androidx.core.os.CancellationSignal()
            cancellationSignal.setOnCancelListener { androidxSignal.cancel() }

            fingerprintManager.authenticate(
                null,
                0,
                androidxSignal,
                object : FingerprintManagerCompat.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                        onError(errString?.toString() ?: "Error en lector de huellas")
                    }

                    override fun onAuthenticationFailed() {
                        onError("Huella no reconocida. Intenta de nuevo.")
                    }
                },
                null
            )
        }

        return cancellationSignal
    }
}
