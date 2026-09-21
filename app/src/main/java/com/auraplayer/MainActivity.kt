package com.auraplayer

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auraplayer.audio.ShakeDetector
import com.auraplayer.audio.SleepTimerManager
import com.auraplayer.data.model.MediaModel
import com.auraplayer.data.model.RepeatMode
import com.auraplayer.data.repository.FavoritesManager
import com.auraplayer.data.repository.LyricsManager
import com.auraplayer.data.repository.MediaRepository
import com.auraplayer.data.repository.PlaylistManager
import com.auraplayer.data.repository.SongLyrics
import com.auraplayer.service.PlaybackService
import com.auraplayer.ui.components.MiniPlayer
import com.auraplayer.ui.components.SleepTimerDialog
import com.auraplayer.ui.screens.EqualizerScreen
import com.auraplayer.ui.screens.MusicScreen
import com.auraplayer.ui.screens.PlayerScreen
import com.auraplayer.ui.screens.VideoPlayerScreen
import com.auraplayer.ui.screens.VideoScreen
import com.auraplayer.ui.theme.AuraTheme
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val playlistManager = remember { PlaylistManager(context) }
            var currentAccent by remember { mutableStateOf(playlistManager.getThemeAccent()) }

            AuraTheme(accent = currentAccent) {
                AuraApp(
                    playlistManager = playlistManager,
                    currentAccent = currentAccent,
                    onAccentChange = { currentAccent = it }
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun AuraApp(
    playlistManager: PlaylistManager,
    currentAccent: String,
    onAccentChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaRepository = remember { MediaRepository(context) }
    val favoritesManager = remember { FavoritesManager(context) }
    val sleepTimerManager = remember { SleepTimerManager() }
    val lyricsManager = remember { LyricsManager(context) }

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
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var playbackPitch by remember { mutableFloatStateOf(1.0f) }
    var favoritesTrigger by remember { mutableIntStateOf(0) }

    var showPlayerScreen by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var activeVideo by remember { mutableStateOf<MediaModel?>(null) }

    // Live Lyrics state
    var lyrics by remember { mutableStateOf<SongLyrics?>(null) }
    var isLoadingLyrics by remember { mutableStateOf(false) }

    // Shake to Skip state & detector
    var isShakeEnabled by remember { mutableStateOf(false) }
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    var controller by remember { mutableStateOf<MediaController?>(null) }

    val shakeDetector = remember {
        ShakeDetector(context) {
            controller?.seekToNextMediaItem()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(50)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(isShakeEnabled) {
        shakeDetector.isEnabled = isShakeEnabled
        onDispose {
            shakeDetector.stop()
        }
    }

    // Auto-fetch lyrics whenever track changes
    LaunchedEffect(currentMedia?.id) {
        val song = currentMedia
        if (song != null) {
            isLoadingLyrics = true
            lyrics = null
            lyrics = lyricsManager.getLyrics(song)
            isLoadingLyrics = false
        } else {
            lyrics = null
        }
    }

    // Scoped Storage Delete Activity Result Launcher
    var pendingSongToDelete by remember { mutableStateOf<MediaModel?>(null) }

    val deleteMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingSongToDelete?.let { song ->
                scope.launch(Dispatchers.IO) {
                    mediaRepository.cleanupSongCache(song)
                    withContext(Dispatchers.Main) {
                        songs = songs.filter { it.id != song.id }
                        if (currentMedia?.id == song.id) {
                            if (songs.isNotEmpty()) {
                                controller?.seekToNextMediaItem()
                            } else {
                                controller?.stop()
                                currentMedia = null
                            }
                        }
                        Toast.makeText(context, "Canción eliminada del teléfono", Toast.LENGTH_SHORT).show()
                        pendingSongToDelete = null
                    }
                }
            }
        } else {
            Toast.makeText(context, "Eliminación cancelada", Toast.LENGTH_SHORT).show()
            pendingSongToDelete = null
        }
    }

    val handleDeleteSong: (MediaModel) -> Unit = { song ->
        scope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // Full access granted: direct file deletion
                    val success = mediaRepository.deleteAudioFile(song)
                    if (success) {
                        songs = songs.filter { it.id != song.id }
                        if (currentMedia?.id == song.id) {
                            if (songs.isNotEmpty()) {
                                controller?.seekToNextMediaItem()
                            } else {
                                controller?.stop()
                                currentMedia = null
                            }
                        }
                        Toast.makeText(context, "Canción eliminada del teléfono", Toast.LENGTH_SHORT).show()
                    } else {
                        // Fallback to Scoped Storage system dialog
                        try {
                            pendingSongToDelete = song
                            val pendingIntent = MediaStore.createDeleteRequest(
                                context.contentResolver,
                                listOf(song.uri)
                            )
                            deleteMediaLauncher.launch(
                                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo eliminar el archivo", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Standard Scoped Storage delete request dialog
                    try {
                        pendingSongToDelete = song
                        val pendingIntent = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            listOf(song.uri)
                        )
                        deleteMediaLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        val success = mediaRepository.deleteAudioFile(song)
                        if (success) {
                            songs = songs.filter { it.id != song.id }
                            if (currentMedia?.id == song.id) {
                                if (songs.isNotEmpty()) {
                                    controller?.seekToNextMediaItem()
                                } else {
                                    controller?.stop()
                                    currentMedia = null
                                }
                            }
                            Toast.makeText(context, "Canción eliminada del teléfono", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error al eliminar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                try {
                    val rows = context.contentResolver.delete(song.uri, null, null)
                    if (rows > 0) {
                        mediaRepository.cleanupSongCache(song)
                        songs = songs.filter { it.id != song.id }
                        if (currentMedia?.id == song.id) {
                            if (songs.isNotEmpty()) {
                                controller?.seekToNextMediaItem()
                            } else {
                                controller?.stop()
                                currentMedia = null
                            }
                        }
                        Toast.makeText(context, "Canción eliminada del teléfono", Toast.LENGTH_SHORT).show()
                    }
                } catch (securityEx: SecurityException) {
                    if (securityEx is RecoverableSecurityException) {
                        pendingSongToDelete = song
                        deleteMediaLauncher.launch(
                            IntentSenderRequest.Builder(securityEx.userAction.actionIntent.intentSender).build()
                        )
                    }
                }
            } else {
                val success = mediaRepository.deleteAudioFile(song)
                if (success) {
                    songs = songs.filter { it.id != song.id }
                    if (currentMedia?.id == song.id) {
                        if (songs.isNotEmpty()) {
                            controller?.seekToNextMediaItem()
                        } else {
                            controller?.stop()
                            currentMedia = null
                        }
                    }
                    Toast.makeText(context, "Canción eliminada del teléfono", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No se pudo eliminar el archivo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
                        val found = songs.find { it.id == mediaId }
                        currentMedia = found
                        if (found != null) {
                            playlistManager.recordPlay(found.id)
                        }
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

    // Load media files once permission is granted and auto-fetch cover art
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            isLoading = true
            val rawSongs = mediaRepository.loadAudioFiles()
            songs = rawSongs.map { s ->
                val override = playlistManager.getTagOverride(s.id)
                if (override != null) {
                    s.copy(title = override.title, artist = override.artist, album = override.album)
                } else s
            }
            videos = mediaRepository.loadVideoFiles()
            isLoading = false

            // Auto-fetch & save cover art in background for all downloaded tracks
            withContext(Dispatchers.IO) {
                var updated = false
                songs.forEach { song ->
                    val savedCover = mediaRepository.coverArtManager.autoFetchAndSaveCover(song)
                    if (savedCover != null && song.artworkUri != savedCover) {
                        updated = true
                    }
                }
                if (updated) {
                    val reloaded = mediaRepository.loadAudioFiles()
                    songs = reloaded.map { s ->
                        val override = playlistManager.getTagOverride(s.id)
                        if (override != null) {
                            s.copy(title = override.title, artist = override.artist, album = override.album)
                        } else s
                    }
                }
            }
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

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            sleepTimerManager = sleepTimerManager,
            onDismiss = { showSleepTimerDialog = false },
            onFinish = {
                controller?.pause()
            }
        )
    }

    // Fullscreen Audio Player Screen
    if (showPlayerScreen && currentMedia != null) {
        val isFav = favoritesTrigger.let { favoritesManager.isFavorite(currentMedia!!.id) }
        PlayerScreen(
            currentMedia = currentMedia,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            isFavorite = isFav,
            playbackSpeed = playbackSpeed,
            playbackPitch = playbackPitch,
            isShakeEnabled = isShakeEnabled,
            lyrics = lyrics,
            isLoadingLyrics = isLoadingLyrics,
            queueSongs = songs,
            onQueueSongClick = { song ->
                currentMedia = song
                playlistManager.recordPlay(song.id)
                controller?.run {
                    val songIndex = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                    seekToDefaultPosition(songIndex)
                    play()
                }
            },
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
            onSeekRelative = { deltaMs ->
                controller?.let { c ->
                    val target = (c.currentPosition + deltaMs).coerceIn(0L, durationMs)
                    c.seekTo(target)
                    currentPositionMs = target
                }
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
            onToggleFavorite = {
                currentMedia?.let {
                    favoritesManager.toggleFavorite(it.id)
                    favoritesTrigger++
                }
            },
            onToggleShake = {
                isShakeEnabled = !isShakeEnabled
                val msg = if (isShakeEnabled) "Agitar para saltar canción: ACTIVADO" else "Agitar para saltar canción: DESACTIVADO"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onAudioFxChange = { speed, pitch ->
                playbackSpeed = speed
                playbackPitch = pitch
                controller?.playbackParameters = PlaybackParameters(speed, pitch)
            },
            onOpenSleepTimer = { showSleepTimerDialog = true },
            onDeleteSong = { song ->
                handleDeleteSong(song)
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
            0 -> {
                // Trigger recomposition when favorites change
                favoritesTrigger.let { }
                MusicScreen(
                    songs = songs,
                    isLoading = isLoading,
                    currentMedia = currentMedia,
                    favoritesManager = favoritesManager,
                    playlistManager = playlistManager,
                    onSongClick = { song ->
                        currentMedia = song
                        playlistManager.recordPlay(song.id)
                        controller?.run {
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
                    onPlayNext = { song ->
                        controller?.let { ctrl ->
                            val item = MediaItem.Builder()
                                .setUri(song.uri)
                                .setMediaId(song.id.toString())
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(song.title)
                                        .setArtist(song.artist)
                                        .setAlbumTitle(song.album)
                                        .setArtworkUri(song.artworkUri)
                                        .build()
                                )
                                .build()
                            val nextIndex = (ctrl.currentMediaItemIndex + 1).coerceAtMost(ctrl.mediaItemCount)
                            ctrl.addMediaItem(nextIndex, item)
                            Toast.makeText(context, "'${song.title}' se reproducirá a continuación", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAddToQueue = { song ->
                        controller?.let { ctrl ->
                            val item = MediaItem.Builder()
                                .setUri(song.uri)
                                .setMediaId(song.id.toString())
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(song.title)
                                        .setArtist(song.artist)
                                        .setAlbumTitle(song.album)
                                        .setArtworkUri(song.artworkUri)
                                        .build()
                                )
                                .build()
                            ctrl.addMediaItem(item)
                            Toast.makeText(context, "'${song.title}' añadida a la cola", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSaveTags = { song, newTitle, newArtist, newAlbum ->
                        playlistManager.saveTagOverride(song.id, newTitle, newArtist, newAlbum)
                        songs = songs.map {
                            if (it.id == song.id) it.copy(title = newTitle, artist = newArtist, album = newAlbum)
                            else it
                        }
                        if (currentMedia?.id == song.id) {
                            currentMedia = currentMedia?.copy(title = newTitle, artist = newArtist, album = newAlbum)
                        }
                        Toast.makeText(context, "Etiquetas guardadas", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteSong = { song ->
                        handleDeleteSong(song)
                    },
                    onFetchCover = { song ->
                        scope.launch(Dispatchers.IO) {
                            mediaRepository.coverArtManager.autoFetchAndSaveCover(song)
                            val reloaded = mediaRepository.loadAudioFiles()
                            val updatedSongs = reloaded.map { s ->
                                val override = playlistManager.getTagOverride(s.id)
                                if (override != null) {
                                    s.copy(title = override.title, artist = override.artist, album = override.album)
                                } else s
                            }
                            withContext(Dispatchers.Main) {
                                songs = updatedSongs
                                if (currentMedia?.id == song.id) {
                                    currentMedia = updatedSongs.find { it.id == song.id }
                                }
                            }
                        }
                    },
                    onOpenSleepTimer = {
                        showSleepTimerDialog = true
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            1 -> VideoScreen(
                videos = videos,
                isLoading = isLoading,
                onVideoClick = { video ->
                    activeVideo = video
                },
                modifier = Modifier.padding(innerPadding)
            )
            2 -> EqualizerScreen(
                playlistManager = playlistManager,
                currentAccent = currentAccent,
                onAccentChange = onAccentChange,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
