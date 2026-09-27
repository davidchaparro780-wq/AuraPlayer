package com.auraplayer.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import com.auraplayer.data.model.MediaModel
import java.util.Locale
import kotlin.random.Random

class VirtualDjManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    var isEnabled: Boolean = false
        private set

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (_: Exception) {}
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(Locale("es", "ES"))
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(1.1f)
            isInitialized = true
        }
    }

    fun setDjEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            tts?.stop()
        }
    }

    fun announceSong(song: MediaModel) {
        if (!isEnabled || !isInitialized) return

        val phrases = listOf(
            "Y ahora en DaVE Player, escucha ${song.title} de ${song.artist}.",
            "¡Sube el volumen! Llega ${song.title}, por ${song.artist}.",
            "Seguimos con buena música en DaVE. Esto es ${song.title}.",
            "A continuación, un temazo de ${song.artist}: ${song.title}."
        )

        val announcement = phrases[Random.nextInt(phrases.size)]
        tts?.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, "dj_announcement")
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
