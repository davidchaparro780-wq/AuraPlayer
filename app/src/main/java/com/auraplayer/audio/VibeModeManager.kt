package com.auraplayer.audio

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer

enum class AudioVibe(val label: String, val icon: String, val speed: Float, val pitch: Float) {
    NORMAL("Normal", "🎵", 1.0f, 1.0f),
    SLOWED_REVERB("Slowed + Reverb", "🌙", 0.85f, 0.85f),
    NIGHTCORE("Nightcore", "⚡", 1.25f, 1.25f),
    VOCAL_BOOST("Vocal Boost", "🎤", 1.0f, 1.05f)
}

object VibeModeManager {
    private const val PREFS_NAME = "aura_vibe_settings"
    private const val KEY_VIBE = "current_vibe"
    private var prefs: SharedPreferences? = null

    var currentVibe: AudioVibe = AudioVibe.NORMAL
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs?.getString(KEY_VIBE, AudioVibe.NORMAL.name) ?: AudioVibe.NORMAL.name
        currentVibe = try { AudioVibe.valueOf(saved) } catch (_: Exception) { AudioVibe.NORMAL }
    }

    fun setVibe(vibe: AudioVibe, player: ExoPlayer?) {
        currentVibe = vibe
        prefs?.edit()?.putString(KEY_VIBE, vibe.name)?.apply()

        player?.playbackParameters = PlaybackParameters(vibe.speed, vibe.pitch)

        // When in SLOWED_REVERB, boost concert hall reverb and virtualizer
        if (vibe == AudioVibe.SLOWED_REVERB) {
            EqualizerManager.instance.setSpatial8DEnabled(true)
        }
    }

    fun cycleNext(player: ExoPlayer?): AudioVibe {
        val next = when (currentVibe) {
            AudioVibe.NORMAL -> AudioVibe.SLOWED_REVERB
            AudioVibe.SLOWED_REVERB -> AudioVibe.NIGHTCORE
            AudioVibe.NIGHTCORE -> AudioVibe.VOCAL_BOOST
            AudioVibe.VOCAL_BOOST -> AudioVibe.NORMAL
        }
        setVibe(next, player)
        return next
    }
}
