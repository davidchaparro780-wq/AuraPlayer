package com.auraplayer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auraplayer.MainActivity
import com.auraplayer.audio.EqualizerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer

    companion object {
        const val CHANNEL_ID = "aura_playback_channel"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20000)
            .setReadTimeoutMs(20000)

        val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, httpDataSourceFactory)

        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
            .setDataSourceFactory(defaultDataSourceFactory)

        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2000,
                /* maxBufferMs = */ 30000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1000
            )
            .build()

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        EqualizerManager.instance.initPrefs(this)
        EqualizerManager.instance.attachToAudioSession(player.audioSessionId)

        val playlistManager = com.auraplayer.data.repository.PlaylistManager(this)
        EqualizerManager.instance.setReplayGainEnabled(playlistManager.isReplayGainEnabled())

        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                EqualizerManager.instance.setReplayGainEnabled(playlistManager.isReplayGainEnabled())
            }
        })

        // DJ Crossfade Real-time Volume Engine (Poweramp Style)
        serviceScope.launch {
            while (isActive) {
                val crossfadeSec = playlistManager.getCrossfadeSeconds()
                if (crossfadeSec > 0 && player.isPlaying && player.duration > 0L) {
                    val fadeWindowMs = crossfadeSec * 1000L
                    val pos = player.currentPosition
                    val dur = player.duration
                    val rem = dur - pos

                    if (rem in 0L..fadeWindowMs) {
                        // Fade out towards track end
                        val factor = (rem.toFloat() / fadeWindowMs.toFloat()).coerceIn(0.08f, 1.0f)
                        player.volume = factor
                    } else if (pos in 0L..(fadeWindowMs / 2)) {
                        // Fade in at track beginning
                        val factor = (pos.toFloat() / (fadeWindowMs / 2).toFloat()).coerceIn(0.15f, 1.0f)
                        player.volume = factor
                    } else if (player.volume < 1.0f) {
                        player.volume = 1.0f
                    }
                } else if (player.volume < 1.0f) {
                    player.volume = 1.0f
                }
                delay(150)
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aura Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Reproducción de música en segundo plano de Aura"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
