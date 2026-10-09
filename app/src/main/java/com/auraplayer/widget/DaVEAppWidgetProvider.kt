package com.auraplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.RemoteViews
import com.auraplayer.MainActivity
import com.auraplayer.R
import com.auraplayer.data.model.MediaModel

/**
 * Proveedor de Widget de Escritorio para DaVE Player (Material 3 Cyberpunk).
 * Permite controlar la reproducción, ver la carátula y el título desde la pantalla de inicio.
 */
class DaVEAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, currentSongCached, isPlayingCached)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val controller = MainActivity.activeController

        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                controller?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
            }
            ACTION_NEXT -> {
                controller?.seekToNextMediaItem()
            }
            ACTION_PREV -> {
                controller?.seekToPreviousMediaItem()
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.auraplayer.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.auraplayer.widget.ACTION_NEXT"
        const val ACTION_PREV = "com.auraplayer.widget.ACTION_PREV"

        private var currentSongCached: MediaModel? = null
        private var isPlayingCached: Boolean = false

        fun updateAllWidgets(context: Context, song: MediaModel?, isPlaying: Boolean) {
            currentSongCached = song
            isPlayingCached = isPlaying

            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val thisWidget = ComponentName(context, DaVEAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget) ?: return

            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, song, isPlaying)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            song: MediaModel?,
            isPlaying: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_dave_compact)

            // Título y artista
            if (song != null) {
                views.setTextViewText(R.id.widget_song_title, song.title)
                views.setTextViewText(R.id.widget_song_artist, song.artist)
            } else {
                views.setTextViewText(R.id.widget_song_title, "DaVE Player")
                views.setTextViewText(R.id.widget_song_artist, "Toca para abrir")
            }

            // Icono Play / Pause
            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )

            // Cargar carátula si existe
            if (song?.artworkUri != null) {
                try {
                    val bmp = loadThumbnailBitmap(context, song.artworkUri)
                    if (bmp != null) {
                        views.setImageViewBitmap(R.id.widget_album_art, bmp)
                    } else {
                        views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_music)
                    }
                } catch (_: Exception) {
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_music)
                }
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_music)
            }

            // PendingIntent para abrir la aplicación al tocar el widget
            val openAppIntent = Intent(context, MainActivity::class.java)
            val openAppPending = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPending)

            // PendingIntent para botones de control
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                getBroadcastPendingIntent(context, ACTION_PLAY_PAUSE, 1)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                getBroadcastPendingIntent(context, ACTION_NEXT, 2)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                getBroadcastPendingIntent(context, ACTION_PREV, 3)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getBroadcastPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, DaVEAppWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun loadThumbnailBitmap(context: Context, uri: Uri): Bitmap? {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.setTargetSize(128, 128)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
