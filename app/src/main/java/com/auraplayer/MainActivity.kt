package com.auraplayer

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auraplayer.data.model.MediaModel
import com.auraplayer.data.model.RepeatMode
import com.auraplayer.data.repository.MediaRepository
import com.auraplayer.service.PlaybackService
import com.auraplayer.ui.components.MiniPlayer
import com.auraplayer.ui.screens.EqualizerScreen
import com.auraplayer.ui.screens.MusicScreen
import com.auraplayer.ui.screens.PlayerScreen
import com.auraplayer.ui.screens.VideoPlayerScreen
import com.auraplayer.ui.screens.VideoScreen
import com.auraplayer.ui.theme.AuraTheme
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AuraTheme {
                AuraApp()
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AuraApp() {
    val context = LocalContext.current
    val mediaRepository = remember { MediaRepository(context) }

    var hasPermission by remember {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result.values.all { it }
    }

    var songs by remember { mutableStateOf<List<MediaModel>>(emptyList()) }
    var videos by remember { mutableStateOf<List<MediaModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var selectedNavTab by remember { mutableIntStateOf(0) } // 0: Music, 1: Videos, 2: Equalizer
    var currentMedia by remember { mutableStateOf<MediaModel?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isShuffle by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableStateOf(RepeatMode.OFF) }

    var showPlayerScreen by remember { mutableStateOf(false) }
    var activeVideo by remember { mutableStateOf<MediaModel?>(null) }

    var controller by remember { mutableStateOf<MediaController?>(null) }

    // Connect to Media3 PlaybackService
    DisposableEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val mediaCtrl = controllerFuture.get()
            controller = mediaCtrl

            mediaCtrl.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    durationMs = mediaCtrl.duration.coerceAtLeast(0L)
                }

                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    val mediaId = item?.mediaId?.toLongOrNull()
                    if (mediaId != null) {
                        currentMedia = songs.find { it.id == mediaId }
                    }
                }
            })
        }, MoreExecutors.directExecutor())

        onDispose {
            controller?.release()
        }
    }

    // Position update loop
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            controller?.let {
                currentPositionMs = it.currentPosition
                durationMs = it.duration.coerceAtLeast(0L)
            }
            delay(400)
        }
    }

    // Load media files once permission is granted
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            songs = mediaRepository.loadAudioFiles()
            videos = mediaRepository.loadVideoFiles()
            isLoading = false
        }
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Aura necesita permisos para explorar tu música y videos.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp)
                )
                Button(
                    onClick = {
                        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(
                                Manifest.permission.READ_MEDIA_AUDIO,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        permissionLauncher.launch(perms)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Conceder Acceso", color = Color.White)
                }
            }
        }
        return
    }

    // Fullscreen Video Player
    if (activeVideo != null) {
        // Automatically pause music when opening a video
        controller?.pause()
        VideoPlayerScreen(
            video = activeVideo!!,
            onBack = { activeVideo = null }
        )
        return
    }

    // Fullscreen Audio Player Screen
    if (showPlayerScreen && currentMedia != null) {
        PlayerScreen(
            currentMedia = currentMedia,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            onPlayPauseClick = {
                controller?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
            },
            onNextClick = {
                controller?.seekToNextMediaItem()
            },
            onPreviousClick = {
                controller?.seekToPreviousMediaItem()
            },
            onSeek = { targetMs ->
                controller?.seekTo(targetMs)
                currentPositionMs = targetMs
            },
            onShuffleToggle = {
                isShuffle = !isShuffle
                controller?.shuffleModeEnabled = isShuffle
            },
            onRepeatToggle = {
                repeatMode = when (repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
                controller?.repeatMode = when (repeatMode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                }
            },
            onDismiss = { showPlayerScreen = false }
        )
        return
    }

    // Main Scaffold with Bottom Navigation and MiniPlayer
    Scaffold(
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                // Mini Player
                val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
                MiniPlayer(
                    currentMedia = currentMedia,
                    isPlaying = isPlaying,
                    progress = progress,
                    onPlayPauseClick = {
                        controller?.let {
                            if (it.isPlaying) it.pause() else it.play()
                        }
                    },
                    onNextClick = {
                        controller?.seekToNextMediaItem()
                    },
                    onClick = {
                        showPlayerScreen = true
                    }
                )

                // Aesthetic Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = selectedNavTab == 0,
                        onClick = { selectedNavTab = 0 },
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        label = { Text("Música") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 1,
                        onClick = { selectedNavTab = 1 },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                        label = { Text("Videos") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 2,
                        onClick = { selectedNavTab = 2 },
                        icon = { Icon(Icons.Default.Equalizer, contentDescription = null) },
                        label = { Text("Ecualizador") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedNavTab) {
            0 -> MusicScreen(
                songs = songs,
                isLoading = isLoading,
                currentMedia = currentMedia,
                onSongClick = { song ->
                    currentMedia = song
                    controller?.run {
                        // Populate entire queue for continuous uninterrupted playback!
                        val songIndex = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                        val mediaItemList = songs.map { s ->
                            MediaItem.Builder()
                                .setUri(s.uri)
                                .setMediaId(s.id.toString())
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(s.title)
                                        .setArtist(s.artist)
                                        .setAlbumTitle(s.album)
                                        .setArtworkUri(s.artworkUri)
                                        .build()
                                )
                                .build()
                        }
                        setMediaItems(mediaItemList, songIndex, 0L)
                        prepare()
                        play()
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
            1 -> VideoScreen(
                videos = videos,
                isLoading = isLoading,
                onVideoClick = { video ->
                    activeVideo = video
                },
                modifier = Modifier.padding(innerPadding)
            )
            2 -> EqualizerScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
