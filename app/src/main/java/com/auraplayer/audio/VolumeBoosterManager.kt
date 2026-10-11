package com.auraplayer.audio

import android.content.Context
import android.content.SharedPreferences
import com.auraplayer.util.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BoosterProfile(val title: String, val boostPercent: Int, val description: String) {
    NORMAL("Normal", 100, "Volumen estándar (100%) sin amplificación"),
    BALANCED("Potente 130%", 130, "Aumento limpio para interiores y audífonos"),
    HIGH("Fuerte 160%", 160, "Gran pegada para altavoces y exteriores"),
    SUPER_MAX("Máximo 200%", 200, "Doble de potencia con limitador dinámico"),
    CINEMA_3D("Cine 3D Surround", 150, "Espacialidad inmersiva de teatro"),
    STREET_LOUD("Modo Calle", 180, "Voz clara y graves reforzados para ruido exterior")
}

class VolumeBoosterManager private constructor() {

    companion object {
        private const val TAG = "VolumeBoosterManager"
        private const val PREFS_NAME = "volume_booster_prefs"
        private const val KEY_PERCENT = "boost_percent"
        private const val KEY_PROFILE = "boost_profile"

        val instance: VolumeBoosterManager by lazy { VolumeBoosterManager() }
    }

    private var prefs: SharedPreferences? = null

    private val _boostPercent = MutableStateFlow(100)
    val boostPercent: StateFlow<Int> = _boostPercent.asStateFlow()

    private val _activeProfile = MutableStateFlow(BoosterProfile.NORMAL)
    val activeProfile: StateFlow<BoosterProfile> = _activeProfile.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedPercent = prefs?.getInt(KEY_PERCENT, 100) ?: 100
            val savedProfileName = prefs?.getString(KEY_PROFILE, BoosterProfile.NORMAL.name) ?: BoosterProfile.NORMAL.name
            val savedProfile = try {
                BoosterProfile.valueOf(savedProfileName)
            } catch (_: Exception) {
                BoosterProfile.NORMAL
            }

            _boostPercent.value = savedPercent
            _activeProfile.value = savedProfile
            applyBoost(savedPercent, savedProfile == BoosterProfile.CINEMA_3D)
        }
    }

    fun setBoostPercent(percent: Int) {
        val clamped = percent.coerceIn(100, 200)
        _boostPercent.value = clamped

        // Detect matching profile or custom
        _activeProfile.value = BoosterProfile.values().firstOrNull { it.boostPercent == clamped } ?: BoosterProfile.NORMAL

        applyBoost(clamped, _activeProfile.value == BoosterProfile.CINEMA_3D)
        saveSettings()
    }

    fun applyProfile(profile: BoosterProfile) {
        _activeProfile.value = profile
        _boostPercent.value = profile.boostPercent

        applyBoost(profile.boostPercent, profile == BoosterProfile.CINEMA_3D)
        saveSettings()
    }

    private fun applyBoost(percent: Int, is3DCinema: Boolean) {
        try {
            // Percent ranges from 100 (0mB gain) to 200 (+1000mB gain)
            val gainMb = (((percent - 100) / 100.0f) * 1000).toInt().coerceIn(0, 1000)
            EqualizerManager.instance.setLoudnessGain(gainMb)

            if (is3DCinema) {
                EqualizerManager.instance.setSpatial8DEnabled(true)
            } else if (_activeProfile.value != BoosterProfile.CINEMA_3D && EqualizerManager.instance.isSpatial8DEnabled) {
                EqualizerManager.instance.setSpatial8DEnabled(false)
            }
            AppLog.d(TAG, "Super Booster aplicado: $percent% ($gainMb mB), Cinema3D=$is3DCinema")
        } catch (e: Exception) {
            AppLog.e(TAG, "Error al aplicar Super Booster", e)
        }
    }

    private fun saveSettings() {
        prefs?.edit()?.apply {
            putInt(KEY_PERCENT, _boostPercent.value)
            putString(KEY_PROFILE, _activeProfile.value.name)
            apply()
        }
    }
}
