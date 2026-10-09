package com.auraplayer.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SettingsManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("dave_settings_prefs", Context.MODE_PRIVATE)

    // State holders for Compose reactivity
    var fadePlayback by mutableStateOf(prefs.getBoolean(KEY_FADE_PLAYBACK, true))
        private set

    var pauseOnHeadphonesDisconnect by mutableStateOf(prefs.getBoolean(KEY_PAUSE_ON_HEADPHONES_DISCONNECT, true))
        private set

    var resumeOnHeadphonesConnect by mutableStateOf(prefs.getBoolean(KEY_RESUME_ON_HEADPHONES_CONNECT, false))
        private set

    var hideShortAudioDurationSec by mutableIntStateOf(prefs.getInt(KEY_HIDE_SHORT_AUDIO_SEC, 30))
        private set

    var hapticFeedbackEnabled by mutableStateOf(prefs.getBoolean(KEY_HAPTIC_ENABLED, true))
        private set

    var keepScreenOnInPlayer by mutableStateOf(prefs.getBoolean(KEY_KEEP_SCREEN_ON, false))
        private set

    var dynamicPaletteEnabled by mutableStateOf(prefs.getBoolean(KEY_DYNAMIC_PALETTE, true))
        private set

    var visualizerSensitivity by mutableFloatStateOf(prefs.getFloat(KEY_VISUALIZER_SENSITIVITY, 1.0f))
        private set

    var notifyUpdates by mutableStateOf(prefs.getBoolean(KEY_NOTIFY_UPDATES, true))
        private set

    fun setFadePlaybackEnabled(enabled: Boolean) {
        fadePlayback = enabled
        prefs.edit().putBoolean(KEY_FADE_PLAYBACK, enabled).apply()
    }

    fun setPauseOnHeadphonesDisconnectEnabled(enabled: Boolean) {
        pauseOnHeadphonesDisconnect = enabled
        prefs.edit().putBoolean(KEY_PAUSE_ON_HEADPHONES_DISCONNECT, enabled).apply()
    }

    fun setResumeOnHeadphonesConnectEnabled(enabled: Boolean) {
        resumeOnHeadphonesConnect = enabled
        prefs.edit().putBoolean(KEY_RESUME_ON_HEADPHONES_CONNECT, enabled).apply()
    }

    fun setHideShortAudioSec(seconds: Int) {
        hideShortAudioDurationSec = seconds
        prefs.edit().putInt(KEY_HIDE_SHORT_AUDIO_SEC, seconds).apply()
    }

    fun setHapticEnabled(enabled: Boolean) {
        hapticFeedbackEnabled = enabled
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    fun setKeepScreenOnInPlayerEnabled(enabled: Boolean) {
        keepScreenOnInPlayer = enabled
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
    }

    fun setDynamicPalette(enabled: Boolean) {
        dynamicPaletteEnabled = enabled
        prefs.edit().putBoolean(KEY_DYNAMIC_PALETTE, enabled).apply()
    }

    fun setVisualizerSensitivityValue(value: Float) {
        visualizerSensitivity = value
        prefs.edit().putFloat(KEY_VISUALIZER_SENSITIVITY, value).apply()
    }

    fun setNotifyUpdatesEnabled(enabled: Boolean) {
        notifyUpdates = enabled
        prefs.edit().putBoolean(KEY_NOTIFY_UPDATES, enabled).apply()
    }

    companion object {
        private const val KEY_FADE_PLAYBACK = "fade_playback"
        private const val KEY_PAUSE_ON_HEADPHONES_DISCONNECT = "pause_on_headphones_disconnect"
        private const val KEY_RESUME_ON_HEADPHONES_CONNECT = "resume_on_headphones_connect"
        private const val KEY_HIDE_SHORT_AUDIO_SEC = "hide_short_audio_sec"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on_player"
        private const val KEY_DYNAMIC_PALETTE = "dynamic_palette_enabled"
        private const val KEY_VISUALIZER_SENSITIVITY = "visualizer_sensitivity"
        private const val KEY_NOTIFY_UPDATES = "notify_updates"

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        val instance: SettingsManager?
            get() = INSTANCE
    }
}
