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
    val license: String = "Open / CC"
) {
    val durationFormatted: String
        get() {
            val minutes = durationSec / 60
            val seconds = durationSec % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
