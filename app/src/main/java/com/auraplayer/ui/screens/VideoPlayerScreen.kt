package com.auraplayer.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.auraplayer.data.model.MediaModel
import com.auraplayer.util.AppLog
import kotlinx.coroutines.delay

private const val TAG = "VideoPlayerScreen"

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    video: MediaModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val resumePrefs = remember { context.getSharedPreferences("dave_video_resume", Context.MODE_PRIVATE) }
    val resumeKey = remember(video.id, video.path) {
        "pos_${if (video.id > 0L) video.id else video.path.hashCode().toString()}"
    }
    val savedPos = remember { resumePrefs.getLong(resumeKey, 0L) }
    var showResumePrompt by remember { mutableStateOf(savedPos > 4000L) }

    var isBackgroundAudio by remember { mutableStateOf(false) }

    var isInPipMode by remember {
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) activity?.isInPictureInPictureMode == true else false)
    }

    DisposableEffect(activity) {
        val componentActivity = activity as? ComponentActivity
        val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
            isInPipMode = info.isInPictureInPictureMode
        }
        componentActivity?.addOnPictureInPictureModeChangedListener(listener)
        onDispose {
            componentActivity?.removeOnPictureInPictureModeChangedListener(listener)
        }
    }

    val exoPlayer = remember(video.uri) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 1500,
                /* maxBufferMs = */ 30000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1000
            )
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                setMediaItem(MediaItem.fromUri(video.uri))
                prepare()
                playWhenReady = true
            }
    }

    // Background playback & lifecycle handling
    DisposableEffect(activity, isBackgroundAudio) {
        val componentActivity = activity as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    if (!isBackgroundAudio && !isInPipMode) {
                        exoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (isBackgroundAudio && !exoPlayer.isPlaying) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        componentActivity?.lifecycle?.addObserver(observer)
        onDispose {
            componentActivity?.lifecycle?.removeObserver(observer)
        }
    }

    // Auto-save playback position periodically
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(2000L)
            try {
                val currentPos = exoPlayer.currentPosition
                val duration = exoPlayer.duration
                if (duration > 0 && currentPos > duration - 8000L) {
                    resumePrefs.edit().putLong(resumeKey, 0L).apply()
                } else if (currentPos > 3000L) {
                    resumePrefs.edit().putLong(resumeKey, currentPos).apply()
                }
            } catch (e: Exception) {
                AppLog.w(TAG, "No se pudo guardar la posición de reproducción", e)
            }
        }
    }

    // Auto-hide resume prompt after 7 seconds
    LaunchedEffect(showResumePrompt) {
        if (showResumePrompt) {
            delay(7000L)
            showResumePrompt = false
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            try {
                val currentPos = exoPlayer.currentPosition
                val duration = exoPlayer.duration
                if (duration > 0 && currentPos > duration - 8000L) {
                    resumePrefs.edit().putLong(resumeKey, 0L).apply()
                } else if (currentPos > 3000L) {
                    resumePrefs.edit().putLong(resumeKey, currentPos).apply()
                }
            } catch (e: Exception) {
                AppLog.w(TAG, "No se pudo guardar la posición al salir del vídeo", e)
            }
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = !isInPipMode
                }
            },
            update = { playerView ->
                playerView.useController = !isInPipMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay top controls with dark scrim & statusBarsPadding protection (hidden in PiP mode)
        if (!isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = cleanVideoTitle(video.title, video.folderName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Background Audio Mode Toggle
                    IconButton(onClick = {
                        isBackgroundAudio = !isBackgroundAudio
                        if (isBackgroundAudio) {
                            Toast.makeText(
                                context,
                                "🎧 Modo audio en segundo plano activado: el video seguirá sonando con la pantalla apagada",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "Modo audio en segundo plano desactivado", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Audio en segundo plano",
                            tint = if (isBackgroundAudio) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.75f)
                        )
                    }

                    // Picture-in-Picture Button
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                    ) {
                        IconButton(onClick = {
                            val act = context as? Activity
                            val params = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build()
                            act?.enterPictureInPictureMode(params)
                        }) {
                            Icon(
                                imageVector = Icons.Default.PictureInPictureAlt,
                                contentDescription = "Ventana Flotante (PiP)",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Resume Playback Floating Chip
            AnimatedVisibility(
                visible = showResumePrompt,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 78.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Reanudar desde ${formatMs(savedPos)}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF38BDF8))
                            .clickable {
                                exoPlayer.seekTo(savedPos)
                                showResumePrompt = false
                                Toast.makeText(
                                    context,
                                    "⏮️ Reanudado desde ${formatMs(savedPos)}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Reanudar",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { showResumePrompt = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Descartar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remMin = minutes % 60
        "%d:%02d:%02d".format(hours, remMin, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/**
 * Replaces ugly raw hash filenames (like 0e46a9d0ce7ac6fe2189ea9a11b9a...) with clean, user-friendly titles.
 */
private fun cleanVideoTitle(title: String, folderName: String): String {
    val clean = title.trim()
        .removeSuffix(".mp4").removeSuffix(".MP4")
        .removeSuffix(".mkv").removeSuffix(".MKV")
        .removeSuffix(".webm").removeSuffix(".WEBM")
        .removeSuffix(".3gp").removeSuffix(".3GP")

    val isHash = clean.matches(Regex("^[a-fA-F0-9_-]{16,}$")) || clean.matches(Regex("^[a-fA-F0-9]{12,}$"))
    if (isHash) {
        val folder = folderName.trim()
        return if (folder.isNotBlank() && !folder.equals("Videos", ignoreCase = true) && !folder.equals("0", ignoreCase = true)) {
            "Video $folder"
        } else {
            "Video #${clean.take(6).uppercase()}"
        }
    }
    return clean
}
