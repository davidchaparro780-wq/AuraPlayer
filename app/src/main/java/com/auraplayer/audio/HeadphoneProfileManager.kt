package com.auraplayer.audio

import android.content.Context

data class HeadphoneProfile(
    val id: String,
    val name: String,
    val brand: String,
    val presetIndex: Int,
    val description: String
)

/**
 * Gestor de Perfiles de Calibración Acústica de Audífonos (AutoEQ Studio).
 * Corrige la curva de respuesta acústica para los auriculares y altavoces más populares.
 */
class HeadphoneProfileManager(context: Context) {

    private val prefs = context.getSharedPreferences("dave_headphone_profiles", Context.MODE_PRIVATE)

    val availableProfiles = listOf(
        HeadphoneProfile(
            id = "airpods",
            name = "AirPods / AirPods Pro",
            brand = "Apple",
            presetIndex = 8,
            description = "Bajos cálidos y agudos aireados compensados"
        ),
        HeadphoneProfile(
            id = "sony_xm",
            name = "WH-1000XM4 & XM5 / WF-1000",
            brand = "Sony",
            presetIndex = 9,
            description = "Sub-bajos profundos y medios cristalinos"
        ),
        HeadphoneProfile(
            id = "galaxy_buds",
            name = "Galaxy Buds2 / Pro",
            brand = "Samsung",
            presetIndex = 10,
            description = "Firma Harman Neutra y voces nítidas"
        ),
        HeadphoneProfile(
            id = "jbl_tune",
            name = "JBL Tune Series & Live",
            brand = "JBL",
            presetIndex = 11,
            description = "Bajo Pure Bass de alta pegada y dinámica"
        ),
        HeadphoneProfile(
            id = "kz_iem",
            name = "KZ / IEMs Monitores de Estudio",
            brand = "KZ & IEMs",
            presetIndex = 12,
            description = "Respuesta plana analítica y espacialidad abierta"
        ),
        HeadphoneProfile(
            id = "bose_qc",
            name = "Bose QuietComfort / Hi-Fi",
            brand = "Bose",
            presetIndex = 7,
            description = "Curva balanceada audiófila anti-fatiga"
        )
    )

    var selectedProfileId: String?
        get() = prefs.getString("selected_headphone_profile", null)
        set(value) = prefs.edit().putString("selected_headphone_profile", value).apply()

    fun applyProfile(profileId: String) {
        selectedProfileId = profileId
        val profile = availableProfiles.find { it.id == profileId }
        if (profile != null) {
            EqualizerManager.instance.applyPreset(profile.presetIndex)
        }
    }

    companion object {
        @Volatile
        private var instance: HeadphoneProfileManager? = null

        fun getInstance(context: Context): HeadphoneProfileManager {
            return instance ?: synchronized(this) {
                instance ?: HeadphoneProfileManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
