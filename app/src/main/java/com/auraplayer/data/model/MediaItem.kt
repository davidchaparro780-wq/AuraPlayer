package com.auraplayer.data.model

import android.net.Uri

data class MediaModel(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val artworkUri: Uri? = null,
    val isVideo: Boolean = false,
    val folderName: String = "",
    val path: String = "",
    val size: Long = 0L
) {
    val formattedDuration: String
        get() {
            val totalSeconds = (duration / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}

enum class RepeatMode {
    OFF, ALL, ONE
}
