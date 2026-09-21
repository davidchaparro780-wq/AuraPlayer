package com.auraplayer.data.model

data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val audioUrl: String,
    val coverUrl: String,
    val format: String = "MP3",
    val bitrateKbps: Int = 320,
    val license: String = "Open / CC",
    val source: String = "Jamendo", // "Jamendo", "Deezer", "Archive", "TikTok"
    val isDownloadable: Boolean = true
) {
    val durationFormatted: String
        get() {
            if (durationSec <= 0) return "--:--"
            val minutes = durationSec / 60
            val seconds = durationSec % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
