package com.auraplayer.widget

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.media3.common.Player
import com.auraplayer.MainActivity
import com.auraplayer.R
import com.auraplayer.service.PlaybackService

class QuickPlayTileService : TileService() {

    private fun activePlayer(): Player? = MainActivity.activeController ?: PlaybackService.activePlayer

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val player = activePlayer()
        if (player == null || player.mediaItemCount == 0) {
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pending = PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pending)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }
        if (player.isPlaying) player.pause() else player.play()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val player = activePlayer()
        val hasTrack = (player?.mediaItemCount ?: 0) > 0
        val playing = player?.isPlaying == true && hasTrack
        tile.state = if (playing) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (playing) "Pausar" else if (hasTrack) "Reproducir" else getString(R.string.app_name)
        tile.icon = Icon.createWithResource(
            this,
            if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )
        tile.updateTile()
    }
}
