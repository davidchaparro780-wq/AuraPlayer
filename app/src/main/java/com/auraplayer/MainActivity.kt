package com.auraplayer

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import android.net.Uri
import com.auraplayer.audio.ShakeDetector
import com.auraplayer.audio.SleepTimerManager
import com.auraplayer.data.model.MediaModel
import com.auraplayer.data.model.OnlineTrack
import com.auraplayer.data.model.RepeatMode
import com.auraplayer.data.repository.CoverArtManager
import com.auraplayer.data.repository.DownloadEngine
import com.auraplayer.data.repository.FavoritesManager
import com.auraplayer.data.repository.LyricsManager
import com.auraplayer.data.repository.MediaRepository
import com.auraplayer.data.repository.OnlineMusicRepository
import com.auraplayer.data.repository.PlaylistManager
import com.auraplayer.data.repository.GlobalSearchService
import com.auraplayer.data.repository.SpotifyMetadataService
import com.auraplayer.data.repository.UpdateManager
import com.auraplayer.data.repository.UpdateInfo
import com.auraplayer.data.repository.SongLyrics
import com.auraplayer.data.repository.VaultManager
import com.auraplayer.sensor.WaveGestureManager
import com.auraplayer.service.PlaybackService
import com.auraplayer.ui.components.DaveSplashIntro
import com.auraplayer.ui.components.DaveWrappedDialog
import com.auraplayer.ui.components.MiniPlayer
import com.auraplayer.ui.components.SleepTimerDialog
import com.auraplayer.data.repository.SettingsManager
import com.auraplayer.ui.screens.CarModeScreen
import com.auraplayer.ui.screens.DiscoverScreen
import com.auraplayer.ui.screens.EqualizerScreen
import com.auraplayer.ui.screens.MusicScreen
import com.auraplayer.ui.screens.PlayerScreen
import com.auraplayer.ui.screens.SettingsScreen
import com.auraplayer.ui.screens.VaultScreen
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

        // Maximize display refresh rate (90Hz / 120Hz) for silky smooth 90 FPS rendering
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    display
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay
                }
                val modes = display?.supportedModes
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxMode.modeId
                    window.attributes = params
                }
            } catch (_: Exception) {}
        }

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
    val coverArtManager = remember { CoverArtManager(context) }
    val searchService = remember {
        GlobalSearchService(
            spotifyService = SpotifyMetadataService(
                clientId = "TU_CLIENT_ID_AQUI",       // ← pega tu Spotify Client ID aquí
                clientSecret = "TU_CLIENT_SECRET_AQUI" // ← pega tu Spotify Client Secret aquí
            )
        )
    }
    val downloadEngine = remember { DownloadEngine(context, coverArtManager, lyricsManager) }

    val appPrefs = remember { context.getSharedPreferences("dave_app_prefs", Context.MODE_PRIVATE) }
    val permKey = "media_permissions_granted"
    val isPermanentlyGranted = appPrefs.getBoolean(permKey, false)

    var hasPermission by remember {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val hasAnySystemPerm = permissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        mutableStateOf(isPermanentlyGranted || hasAnySystemPerm)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it } || isPermanentlyGranted
        hasPermission = granted
        if (granted) {
            appPrefs.edit().putBoolean(permKey, true).apply()
        }
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
    var showCarModeScreen by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showWrappedDialog by remember { mutableStateOf(false) }
    var showRouletteDialog by remember { mutableStateOf(false) }
    var showDiagnosticDialog by remember { mutableStateOf(false) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var showOracleCard by remember { mutableStateOf(false) }
    var showTournamentDialog by remember { mutableStateOf(false) }
    var showJukeboxScreen by remember { mutableStateOf(false) }
    var showWifiServerDialog by remember { mutableStateOf(false) }
    var showQuizDialog by remember { mutableStateOf(false) }
    var showAchievementsDialog by remember { mutableStateOf(false) }
    var showBpmDialog by remember { mutableStateOf(false) }
    var showBinauralDialog by remember { mutableStateOf(false) }
    var showTimeCapsuleDialog by remember { mutableStateOf(false) }
    var showBatchCleanerDialog by remember { mutableStateOf(false) }
    var showHeadphonesDialog by remember { mutableStateOf(false) }
    val userManager = remember { com.auraplayer.data.repository.UserManager(context) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var hasPromptedStartupAuth by remember { mutableStateOf(false) }

    var controller by remember { mutableStateOf<MediaController?>(null) }

    val headphoneManager = remember { com.auraplayer.audio.HeadphoneManager(context) }
    DisposableEffect(Unit) {
        headphoneManager.startListening()
        onDispose {
            headphoneManager.stopListening()
        }
    }

    val achievementManager = remember { com.auraplayer.audio.AchievementManager(context) }
    val virtualDjManager = remember { com.auraplayer.audio.VirtualDjManager(context) }
    val flashlightManager = remember { com.auraplayer.audio.FlashlightBeatManager(context) }
    val airGestureManager = remember {
        com.auraplayer.audio.AirGestureManager(
            context,
            onNext = { controller?.seekToNextMediaItem() },
            onPlayPause = {
                val c = controller
                if (c != null) {
                    if (c.isPlaying) c.pause() else c.play()
                }
            }
        )
    }

    var isBubbleActive by remember { mutableStateOf(false) }
    var isAirGesturesActive by remember { mutableStateOf(false) }
    var isFlashlightActive by remember { mutableStateOf(false) }
    var isVirtualDjActive by remember { mutableStateOf(false) }

    val alarmManager = remember { com.auraplayer.audio.MusicAlarmManager(context) }
    val wifiServer = remember { com.auraplayer.service.LocalMusicServer() }
    var isWifiServerRunning by remember { mutableStateOf(false) }
    val hapticBassManager = remember { com.auraplayer.audio.HapticBassManager(context) }
    var isHapticBass by remember { mutableStateOf(hapticBassManager.isEnabled) }
    var vibeMode by remember { mutableStateOf(com.auraplayer.audio.VibeModeManager.currentVibe) }

    var activeVideo by remember { mutableStateOf<MediaModel?>(null) }
    var showVaultScreen by remember { mutableStateOf(false) }
    val vaultManager = remember { VaultManager(context) }
    val settingsManager = remember { SettingsManager.getInstance(context) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    // In-App Auto Updater (DaVE Updater)
    val updateManager = remember { UpdateManager(context) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val currentIntent = (context as? Activity)?.intent
        if (currentIntent?.action == "ACTION_SHOW_UPDATE") {
            val vName = currentIntent.getStringExtra("UPDATE_VERSION_NAME") ?: ""
            val cLog = currentIntent.getStringExtra("UPDATE_CHANGELOG") ?: ""
            val dUrl = currentIntent.getStringExtra("UPDATE_DOWNLOAD_URL") ?: ""
            val fSize = currentIntent.getDoubleExtra("UPDATE_FILE_SIZE", 23.0)
            if (vName.isNotBlank() && dUrl.isNotBlank()) {
                updateInfo = UpdateInfo(vName, cLog, dUrl, fSize)
                showUpdateDialog = true
            }
        }

        downloadEngine.onUpdateNeeded = {
            scope.launch {
                val info = updateManager.checkForUpdate()
                if (info != null) {
                    updateInfo = info.copy(
                        changelog = "⚡ YouTube actualizó su reproductor. Esta nueva versión de DaVE corrige la descarga:\n" + info.changelog
                    )
                    showUpdateDialog = true
                }
            }
        }
        val info = updateManager.checkForUpdate()
        if (info != null) {
            updateInfo = info
            showUpdateDialog = true
        }
    }

    LaunchedEffect(showPlayerScreen, settingsManager.keepScreenOnInPlayer) {
        val window = (context as? Activity)?.window
        if (showPlayerScreen && settingsManager.keepScreenOnInPlayer) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var showAppIntro by remember { mutableStateOf(true) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(showAppIntro) {
        if (!showAppIntro && !userManager.isLoggedIn && !hasPromptedStartupAuth) {
            hasPromptedStartupAuth = true
            showAuthDialog = true
        }
    }

    // Global Back Navigation Handler (Cascade return to Main screen / prevent accidental exit)
    BackHandler(enabled = true) {
        when {
            showAppIntro -> {
                showAppIntro = false
                if (!userManager.isLoggedIn && !hasPromptedStartupAuth) {
                    hasPromptedStartupAuth = true
                    showAuthDialog = true
                }
            }
            showPlayerScreen -> {
                showPlayerScreen = false
            }
            showSettingsScreen -> {
                showSettingsScreen = false
            }
            showCarModeScreen -> {
                showCarModeScreen = false
            }
            showSleepTimerDialog -> {
                showSleepTimerDialog = false
            }
            showWrappedDialog -> {
                showWrappedDialog = false
            }
            showJukeboxScreen -> {
                showJukeboxScreen = false
            }
            showTournamentDialog -> {
                showTournamentDialog = false
            }
            showRouletteDialog -> {
                showRouletteDialog = false
            }
            showDiagnosticDialog -> {
                showDiagnosticDialog = false
            }
            showAlarmDialog -> {
                showAlarmDialog = false
            }
            showWifiServerDialog -> {
                showWifiServerDialog = false
            }
            showQuizDialog -> {
                showQuizDialog = false
            }
            showAchievementsDialog -> {
                showAchievementsDialog = false
            }
            showBpmDialog -> {
                showBpmDialog = false
            }
            showBinauralDialog -> {
                showBinauralDialog = false
            }
            showTimeCapsuleDialog -> {
                showTimeCapsuleDialog = false
            }
            showBatchCleanerDialog -> {
                showBatchCleanerDialog = false
            }
            showHeadphonesDialog -> {
                showHeadphonesDialog = false
            }
            showAuthDialog -> {
                showAuthDialog = false
            }
            showProfileDialog -> {
                showProfileDialog = false
            }
            showOracleCard -> {
                showOracleCard = false
            }
            activeVideo != null -> {
                activeVideo = null
            }
            showVaultScreen -> {
                showVaultScreen = false
                scope.launch {
                    videos = mediaRepository.loadVideoFiles()
                }
            }
            showUpdateDialog -> {
                showUpdateDialog = false
            }
            selectedNavTab != 0 -> {
                // If in Explorar (1), Videos (2), or Ecualizador (3), return directly to Música (0)
                selectedNavTab = 0
            }
            else -> {
                // Already on the main screen (Tab 0: Música)
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    (context as? Activity)?.moveTaskToBack(true)
                } else {
                    lastBackPressTime = now
                    Toast.makeText(context, "Presiona de nuevo para salir", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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

    // Wave Gesture Control (Hand wave over proximity sensor to skip song)
    val waveGestureManager = remember {
        WaveGestureManager(context) {
            controller?.seekToNextMediaItem()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(45)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    var isWaveEnabled by remember { mutableStateOf(waveGestureManager.isEnabled) }

    DisposableEffect(isWaveEnabled) {
        waveGestureManager.isEnabled = isWaveEnabled
        onDispose {
            waveGestureManager.stop()
        }
    }

    // Auto-fetch lyrics and trigger DJ/achievement hooks whenever track changes
    LaunchedEffect(currentMedia?.id) {
        val song = currentMedia
        if (song != null) {
            virtualDjManager.announceSong(song)
            achievementManager.recordSongPlayed(song.path.endsWith(".flac", true))
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

    var pendingVaultVideoToHide by remember { mutableStateOf<Pair<MediaModel, java.io.File>?>(null) }
    val hideVideoDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pending = pendingVaultVideoToHide
        pendingVaultVideoToHide = null
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "🔒 Video ocultado de la galería y protegido en Bóveda", Toast.LENGTH_SHORT).show()
            scope.launch {
                pending?.let { (video, _) ->
                    try {
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(video.path), null, null)
                    } catch (_: Exception) {}
                }
                videos = mediaRepository.loadVideoFiles()
            }
        } else {
            pending?.first?.let { video ->
                vaultManager.unmarkPathAsHidden(video.path)
            }
            vaultManager.cleanVaultFile(pending?.second)
            scope.launch {
                videos = mediaRepository.loadVideoFiles()
            }
            Toast.makeText(context, "Cancelado: el video permanece en tu galería", Toast.LENGTH_SHORT).show()
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
                    val d = mediaCtrl.duration
                    if (d > 0L) {
                        durationMs = d
                    } else if ((currentMedia?.duration ?: 0L) > 0L) {
                        durationMs = currentMedia!!.duration
                    }
                }

                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    sleepTimerManager.onTrackEnded()
                    val mediaId = item?.mediaId?.toLongOrNull()
                    if (mediaId != null) {
                        val found = songs.find { it.id == mediaId }
                        if (found != null) {
                            currentMedia = found
                            durationMs = found.duration
                            playlistManager.recordPlay(found.id)
                        }
                    } else if (item != null) {
                        val meta = item.mediaMetadata
                        val extraDuration = meta.extras?.getLong("duration_ms", 0L) ?: 0L
                        val fallbackDuration = if (extraDuration > 0L) extraDuration else (currentMedia?.takeIf { it.id == -1L }?.duration ?: 0L)
                        currentMedia = MediaModel(
                            id = -1L,
                            title = meta.title?.toString() ?: "Canción",
                            artist = meta.artist?.toString() ?: "Artista",
                            album = meta.albumTitle?.toString() ?: "Online",
                            duration = fallbackDuration,
                            uri = item.localConfiguration?.uri ?: Uri.EMPTY,
                            path = item.localConfiguration?.uri?.toString() ?: "",
                            artworkUri = meta.artworkUri
                        )
                        if (fallbackDuration > 0L) {
                            durationMs = fallbackDuration
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    error.printStackTrace()
                    val media = currentMedia
                    if (media != null && media.id == -1L) {
                        // Attempt automatic stream self-healing before failing
                        scope.launch(Dispatchers.IO) {
                            val freshUrl = searchService.fetchFreshDeezerPreview(media.artist, media.title)
                            if (!freshUrl.isNullOrBlank() && freshUrl != media.path) {
                                withContext(Dispatchers.Main) {
                                    currentMedia = media.copy(uri = Uri.parse(freshUrl), path = freshUrl)
                                    val retryItem = MediaItem.Builder()
                                        .setUri(freshUrl)
                                        .setMediaId("online_retry_${System.currentTimeMillis()}")
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setTitle(media.title)
                                                .setArtist(media.artist)
                                                .setAlbumTitle(media.album)
                                                .setArtworkUri(media.artworkUri)
                                                .build()
                                        )
                                        .build()
                                    controller?.run {
                                        setMediaItem(retryItem)
                                        prepare()
                                        play()
                                    }
                                }
                                return@launch
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "No se pudo reproducir este audio. Intenta con otra canción.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Error al reproducir audio.", Toast.LENGTH_SHORT).show()
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
                com.auraplayer.audio.AbLooperManager.instance.checkAndLoop(currentPositionMs) { target ->
                    it.seekTo(target)
                }
                val d = it.duration
                if (d > 0L) {
                    durationMs = d
                } else if (durationMs <= 0L && (currentMedia?.duration ?: 0L) > 0L) {
                    durationMs = currentMedia!!.duration
                }
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

            // Auto-fetch & save cover art in background coroutine without blocking UI (optimized: only for songs without artwork)
            scope.launch(Dispatchers.IO) {
                val songsWithoutArt = songs.filter { it.artworkUri == null }.take(15)
                if (songsWithoutArt.isNotEmpty()) {
                    var updated = false
                    songsWithoutArt.forEach { song ->
                        val savedCover = mediaRepository.coverArtManager.autoFetchAndSaveCover(song)
                        if (savedCover != null && song.artworkUri != savedCover) {
                            updated = true
                        }
                    }
                    if (updated) {
                        val reloaded = mediaRepository.loadAudioFiles()
                        val reloadedSongs = reloaded.map { s ->
                            val override = playlistManager.getTagOverride(s.id)
                            if (override != null) {
                                s.copy(title = override.title, artist = override.artist, album = override.album)
                            } else s
                        }
                        withContext(Dispatchers.Main) {
                            songs = reloadedSongs
                        }
                    }
                }
            }
        }
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "DaVE necesita permisos para explorar tu música y videos.",
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

    LaunchedEffect(activeVideo) {
        if (activeVideo != null) {
            controller?.pause()
        }
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

    // DaVE Wrapped Dialog (Personal Music Stats)
    if (showWrappedDialog) {
        DaveWrappedDialog(
            playlistManager = playlistManager,
            allSongs = songs,
            onDismiss = { showWrappedDialog = false }
        )
    }

    // Music Roulette Dialog
    if (showRouletteDialog) {
        com.auraplayer.ui.components.MusicRouletteDialog(
            songs = songs,
            onSongSelected = { song ->
                controller?.let { c ->
                    val idx = songs.indexOfFirst { it.id == song.id }
                    if (idx >= 0) {
                        c.seekTo(idx, 0L)
                        c.play()
                    }
                }
            },
            onDismiss = { showRouletteDialog = false }
        )
    }

    // Library Diagnostic Dialog
    if (showDiagnosticDialog) {
        com.auraplayer.ui.components.LibraryDiagnosticDialog(
            songs = songs,
            onDeleteFiles = { toDelete ->
                toDelete.forEach { handleDeleteSong(it) }
                Toast.makeText(context, "${toDelete.size} archivos limpiados", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showDiagnosticDialog = false }
        )
    }

    // Gentle Music Alarm Dialog
    if (showAlarmDialog) {
        com.auraplayer.ui.components.AlarmSetupDialog(
            alarmManager = alarmManager,
            onDismiss = { showAlarmDialog = false }
        )
    }

    // WiFi Remote Control Dialog
    if (showWifiServerDialog) {
        com.auraplayer.ui.components.WifiServerDialog(
            wifiServer = wifiServer,
            onStopServer = {
                wifiServer.stop()
                isWifiServerRunning = false
                showWifiServerDialog = false
                Toast.makeText(context, "Servidor WiFi apagado", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showWifiServerDialog = false }
        )
    }

    // DaVE Music Quiz Dialog
    if (showQuizDialog) {
        com.auraplayer.ui.components.MusicQuizDialog(
            songs = songs,
            achievementManager = achievementManager,
            onPlaySnippet = { song, startMs ->
                controller?.let { c ->
                    val idx = songs.indexOfFirst { it.id == song.id }
                    if (idx >= 0) {
                        c.seekTo(idx, startMs)
                        c.play()
                    }
                }
            },
            onStopSnippet = { controller?.pause() },
            onDismiss = { showQuizDialog = false }
        )
    }

    // Achievements & Listener Level Dialog
    if (showAchievementsDialog) {
        com.auraplayer.ui.components.AchievementsDialog(
            achievementManager = achievementManager,
            onDismiss = { showAchievementsDialog = false }
        )
    }

    // BPM Workout Playlists Dialog
    if (showBpmDialog) {
        com.auraplayer.ui.components.BpmWorkoutDialog(
            songs = songs,
            onPlayPlaylist = { workoutSongs ->
                controller?.let { c ->
                    val first = workoutSongs.firstOrNull() ?: return@let
                    val idx = songs.indexOfFirst { it.id == first.id }
                    if (idx >= 0) {
                        c.seekTo(idx, 0L)
                        c.play()
                    }
                }
            },
            onDismiss = { showBpmDialog = false }
        )
    }

    // Binaural Beats & Sleep Synthesizer Dialog
    if (showBinauralDialog) {
        com.auraplayer.ui.components.BinauralNoiseDialog(
            onDismiss = { showBinauralDialog = false }
        )
    }

    // Music Time Capsule Dialog
    if (showTimeCapsuleDialog) {
        com.auraplayer.ui.components.TimeCapsuleDialog(
            songs = songs,
            onDismiss = { showTimeCapsuleDialog = false }
        )
    }

    // Smart Batch Metadata Cleaner Dialog
    if (showBatchCleanerDialog) {
        com.auraplayer.ui.components.BatchMetadataDialog(
            songs = songs,
            onDismiss = { showBatchCleanerDialog = false }
        )
    }

    // Mis Audífonos & Audio Hub Dialog
    if (showHeadphonesDialog) {
        com.auraplayer.ui.components.HeadphonesDialog(
            headphoneManager = headphoneManager,
            onDismiss = { showHeadphonesDialog = false }
        )
    }

    // Iniciar Sesión / Crear Cuenta con Correo Dialog
    if (showAuthDialog) {
        com.auraplayer.ui.components.AuthDialog(
            userManager = userManager,
            onSuccess = { showAuthDialog = false },
            onDismiss = { showAuthDialog = false }
        )
    }

    // Perfil de Usuario DaVE VIP Dialog
    if (showProfileDialog) {
        com.auraplayer.ui.components.UserProfileDialog(
            userManager = userManager,
            favoritesManager = favoritesManager,
            playlistManager = playlistManager,
            totalPlaysCount = achievementManager.songsPlayed,
            onDismiss = { showProfileDialog = false }
        )
    }

    // Oracle Daily Card
    if (showOracleCard) {
        val prediction = com.auraplayer.ui.components.predictDailySong(songs, context.getSharedPreferences("dave_oracle", Context.MODE_PRIVATE))
        if (prediction != null) {
            com.auraplayer.ui.components.OracleCard(
                predictedSong = prediction.first,
                confidencePercent = prediction.second,
                onPlay = {
                    controller?.let { c ->
                        val idx = songs.indexOfFirst { it.id == prediction.first.id }
                        if (idx >= 0) {
                            c.seekTo(idx, 0L)
                            c.play()
                        }
                    }
                },
                onDismiss = { showOracleCard = false }
            )
        } else {
            showOracleCard = false
            Toast.makeText(context, "El Oráculo necesita canciones en tu biblioteca", Toast.LENGTH_SHORT).show()
        }
    }

    // Song Tournament Bracket Dialog
    if (showTournamentDialog) {
        com.auraplayer.ui.components.SongTournamentDialog(
            songs = songs,
            onPlaySong = { song ->
                controller?.let { c ->
                    val idx = songs.indexOfFirst { it.id == song.id }
                    if (idx >= 0) {
                        c.seekTo(idx, 0L)
                        c.play()
                    }
                }
            },
            onDismiss = { showTournamentDialog = false }
        )
    }

    // Main Container with Animated Fullscreen Player & Update Dialog
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                // Mini Player
                val effectiveDuration = if (durationMs > 0L) durationMs else (currentMedia?.duration ?: 0L)
                val progress = if (effectiveDuration > 0L) currentPositionMs.toFloat() / effectiveDuration.toFloat() else 0f
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
                    onPreviousClick = {
                        controller?.seekToPreviousMediaItem()
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
                        icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                        label = { Text("Explorar") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 2,
                        onClick = { selectedNavTab = 2 },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                        label = { Text("Videos") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        selected = selectedNavTab == 3,
                        onClick = { selectedNavTab = 3 },
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
        AnimatedContent(
            targetState = selectedNavTab,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> fullWidth / 3 }
                    ) + fadeIn(animationSpec = tween(200))) togetherWith (
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> -fullWidth / 3 }
                        ) + fadeOut(animationSpec = tween(180))
                    )
                } else {
                    (slideInHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> -fullWidth / 3 }
                    ) + fadeIn(animationSpec = tween(200))) togetherWith (
                        slideOutHorizontally(
                            animationSpec = tween(200, easing = FastOutSlowInEasing),
                            targetOffsetX = { fullWidth -> fullWidth / 3 }
                        ) + fadeOut(animationSpec = tween(180))
                    )
                }
            },
            label = "MainTabNavTransition",
            modifier = Modifier.padding(innerPadding)
        ) { targetTab ->
            when (targetTab) {
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
                    onCheckUpdates = {
                        scope.launch {
                            Toast.makeText(context, "Buscando actualizaciones...", Toast.LENGTH_SHORT).show()
                            val info = updateManager.checkForUpdate()
                            if (info != null) {
                                updateInfo = info
                                showUpdateDialog = true
                            } else {
                                Toast.makeText(context, "¡DaVE está al día! Tienes la última versión (${updateManager.currentVersionName}) 🎉", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onOpenRoulette = { showRouletteDialog = true },
                    onOpenDiagnostic = { showDiagnosticDialog = true },
                    onOpenTournament = { showTournamentDialog = true },
                    onOpenJukebox = { showJukeboxScreen = true },
                    onOpenOracle = { showOracleCard = true },
                    onOpenAlarm = { showAlarmDialog = true },
                    onToggleWifiServer = {
                        if (isWifiServerRunning) {
                            showWifiServerDialog = true
                        } else {
                            val serverCtrl = com.auraplayer.service.ServerController(
                                getCurrentSong = { currentMedia },
                                isPlaying = { isPlaying },
                                onPlayPause = {
                                    controller?.let { if (it.isPlaying) it.pause() else it.play() }
                                },
                                onNext = { controller?.seekToNextMediaItem() },
                                onPrev = { controller?.seekToPreviousMediaItem() },
                                onPlaySongById = { id ->
                                    val idx = songs.indexOfFirst { s -> s.id == id }
                                    if (idx >= 0) {
                                        controller?.seekTo(idx, 0L)
                                        controller?.play()
                                    }
                                }
                            )
                            wifiServer.start(songs, serverCtrl, context)
                            isWifiServerRunning = true
                            showWifiServerDialog = true
                        }
                    },
                    isWifiServerRunning = isWifiServerRunning,
                    onOpenQuiz = { showQuizDialog = true },
                    onOpenAchievements = { showAchievementsDialog = true },
                    onOpenBpmWorkout = { showBpmDialog = true },
                    onOpenBinaural = { showBinauralDialog = true },
                    onOpenTimeCapsule = { showTimeCapsuleDialog = true },
                    onOpenBatchCleaner = { showBatchCleanerDialog = true },
                    onToggleBubble = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) {
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                            Toast.makeText(context, "Concede el permiso de superposición para la burbuja", Toast.LENGTH_LONG).show()
                        } else {
                            val intent = android.content.Intent(context, com.auraplayer.service.FloatingBubbleService::class.java)
                            if (isBubbleActive) {
                                context.stopService(intent)
                                isBubbleActive = false
                                Toast.makeText(context, "Burbuja flotante desactivada", Toast.LENGTH_SHORT).show()
                            } else {
                                androidx.core.content.ContextCompat.startForegroundService(context, intent)
                                isBubbleActive = true
                                Toast.makeText(context, "Burbuja flotante activada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    isBubbleActive = isBubbleActive,
                    onToggleAirGestures = {
                        isAirGesturesActive = !isAirGesturesActive
                        if (isAirGesturesActive) {
                            airGestureManager.start()
                            Toast.makeText(context, "✋ Gestos en el Aire ACTIVADOS (Pasa la mano para pasar canción)", Toast.LENGTH_LONG).show()
                        } else {
                            airGestureManager.stop()
                            Toast.makeText(context, "✋ Gestos en el Aire DESACTIVADOS", Toast.LENGTH_SHORT).show()
                        }
                    },
                    isAirGesturesActive = isAirGesturesActive,
                    onToggleShake = {
                        isShakeEnabled = !isShakeEnabled
                        val msg = if (isShakeEnabled) "📳 Agitar para cambiar pista ACTIVADO" else "📳 Agitar para cambiar pista DESACTIVADO"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    isShakeActive = isShakeEnabled,
                    onToggleFlashlight = {
                        isFlashlightActive = flashlightManager.toggle()
                        Toast.makeText(context, if (isFlashlightActive) "🔦 Linterna Rítmica ACTIVADA" else "🔦 Linterna Rítmica DESACTIVADA", Toast.LENGTH_SHORT).show()
                    },
                    isFlashlightActive = isFlashlightActive,
                    onToggleVirtualDj = {
                        isVirtualDjActive = !isVirtualDjActive
                        virtualDjManager.setDjEnabled(isVirtualDjActive)
                        Toast.makeText(context, if (isVirtualDjActive) "📻 Locutor DJ DaVE ACTIVADO" else "📻 Locutor DJ DaVE DESACTIVADO", Toast.LENGTH_SHORT).show()
                    },
                    isVirtualDjActive = isVirtualDjActive,
                    onOpenHeadphones = { showHeadphonesDialog = true },
                    onOpenSettings = { showSettingsScreen = true },
                    userManager = userManager,
                    onOpenAuth = { showAuthDialog = true },
                    onOpenProfile = { showProfileDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
            1 -> DiscoverScreen(
                searchService = searchService,
                downloadEngine = downloadEngine,
                onPreviewTrack = { onlineTrack ->
                    scope.launch {
                        val validUrl = searchService.resolveValidAudioUrl(onlineTrack)
                        val trackDurationMs = onlineTrack.durationSec * 1000L
                        val bundle = android.os.Bundle().apply {
                            putLong("duration_ms", trackDurationMs)
                        }
                        val previewItem = MediaItem.Builder()
                            .setUri(validUrl)
                            .setMediaId("online_${onlineTrack.id}")
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(onlineTrack.title)
                                    .setArtist(onlineTrack.artist)
                                    .setAlbumTitle(onlineTrack.album)
                                    .setArtworkUri(if (onlineTrack.coverUrl.isNotBlank()) Uri.parse(onlineTrack.coverUrl) else null)
                                    .setExtras(bundle)
                                    .build()
                            )
                            .build()
                        currentMedia = MediaModel(
                            id = -1L,
                            title = onlineTrack.title,
                            artist = onlineTrack.artist,
                            album = onlineTrack.album,
                            duration = trackDurationMs,
                            uri = Uri.parse(validUrl),
                            path = validUrl,
                            artworkUri = if (onlineTrack.coverUrl.isNotBlank()) Uri.parse(onlineTrack.coverUrl) else null
                        )
                        durationMs = trackDurationMs
                        controller?.run {
                            setMediaItem(previewItem)
                            prepare()
                            play()
                        }
                    }
                },
                onDownloadComplete = {
                    scope.launch(Dispatchers.IO) {
                        kotlinx.coroutines.delay(400)
                        val reloaded = mediaRepository.loadAudioFiles()
                        val updatedSongs = reloaded.map { s ->
                            val override = playlistManager.getTagOverride(s.id)
                            if (override != null) {
                                s.copy(title = override.title, artist = override.artist, album = override.album)
                            } else s
                        }
                        withContext(Dispatchers.Main) {
                            songs = updatedSongs
                        }

                        // Secondary refresh after MediaScanner finishes background indexing
                        kotlinx.coroutines.delay(1200)
                        val secondReload = mediaRepository.loadAudioFiles().map { s ->
                            val override = playlistManager.getTagOverride(s.id)
                            if (override != null) {
                                s.copy(title = override.title, artist = override.artist, album = override.album)
                            } else s
                        }
                        withContext(Dispatchers.Main) {
                            songs = secondReload
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            2 -> VideoScreen(
                videos = videos,
                isLoading = isLoading,
                onVideoClick = { video ->
                    activeVideo = video
                },
                onOpenVault = {
                    showVaultScreen = true
                },
                onHideVideo = { videoToHide ->
                    // Instantly remove from UI and mark in VaultManager so it vanishes immediately
                    vaultManager.markPathAsHidden(videoToHide.path)
                    videos = videos.filter { it.id != videoToHide.id && it.path != videoToHide.path }

                    scope.launch {
                        val vaultFile = vaultManager.copyMediaToVault(
                            sourcePath = videoToHide.path,
                            sourceUri = videoToHide.uri,
                            isVideo = true
                        )
                        if (vaultFile == null) {
                            vaultManager.unmarkPathAsHidden(videoToHide.path)
                            videos = mediaRepository.loadVideoFiles()
                            Toast.makeText(context, "Error al copiar video a la Bóveda", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val deleted = vaultManager.deleteOriginalMedia(
                            context,
                            filePath = videoToHide.path,
                            uri = videoToHide.uri,
                            isVideo = true
                        )
                        if (deleted) {
                            Toast.makeText(context, "🔒 Video ocultado de la galería y protegido en Bóveda", Toast.LENGTH_SHORT).show()
                            try {
                                android.media.MediaScannerConnection.scanFile(context, arrayOf(videoToHide.path), null, null)
                            } catch (_: Exception) {}
                            videos = mediaRepository.loadVideoFiles()
                        } else {
                            val pendingIntent = vaultManager.getDeleteRequestPendingIntent(
                                context,
                                uri = videoToHide.uri,
                                filePath = videoToHide.path,
                                isVideo = true
                            )
                            if (pendingIntent != null) {
                                pendingVaultVideoToHide = Pair(videoToHide, vaultFile)
                                hideVideoDeleteLauncher.launch(
                                    androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                )
                            } else {
                                Toast.makeText(context, "Para borrar el video de la galería, activa el permiso de archivos", Toast.LENGTH_LONG).show()
                                vaultManager.openAllFilesAccessSettings(context)
                                videos = mediaRepository.loadVideoFiles()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            3 -> EqualizerScreen(
                playlistManager = playlistManager,
                currentAccent = currentAccent,
                onAccentChange = onAccentChange,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

    // Animated Fullscreen Audio Player Screen (Slides up smoothly with spring physics)
    AnimatedVisibility(
        visible = showPlayerScreen && currentMedia != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(animationSpec = tween(250)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(250)
        ) + fadeOut(animationSpec = tween(200))
    ) {
        currentMedia?.let { media ->
            val isFav = favoritesTrigger.let { favoritesManager.isFavorite(media.id) }
            PlayerScreen(
                currentMedia = media,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = if (durationMs > 0L) durationMs else media.duration,
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
                    favoritesManager.toggleFavorite(media.id)
                    favoritesTrigger++
                },
                onToggleShake = {
                    isShakeEnabled = !isShakeEnabled
                    val msg = if (isShakeEnabled) "Agitar para saltar canción: ACTIVADO" else "Agitar para saltar canción: DESACTIVADO"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                isWaveEnabled = isWaveEnabled,
                onToggleWave = {
                    isWaveEnabled = !isWaveEnabled
                    val msg = if (isWaveEnabled) "🌊 Control por gestos (Wave Control): ACTIVADO" else "Control por gestos (Wave Control): DESACTIVADO"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onOpenWrapped = {
                    showWrappedDialog = true
                },
                vibeMode = vibeMode,
                onCycleVibe = {
                    val next = com.auraplayer.audio.VibeModeManager.cycleNext(controller)
                    vibeMode = next
                    next
                },
                isHapticBass = isHapticBass,
                onToggleHapticBass = {
                    isHapticBass = !isHapticBass
                    hapticBassManager.isEnabled = isHapticBass
                    if (isHapticBass) hapticBassManager.triggerBassPulse()
                    val msg = if (isHapticBass) "📳 Bajos Hápticos: ACTIVADO" else "Bajos Hápticos: DESACTIVADO"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                onAudioFxChange = { speed, pitch ->
                    playbackSpeed = speed
                    playbackPitch = pitch
                    controller?.playbackParameters = PlaybackParameters(speed, pitch)
                },
                onOpenSleepTimer = { showSleepTimerDialog = true },
                onOpenCarMode = { showCarModeScreen = true },
                onDeleteSong = { song ->
                    handleDeleteSong(song)
                },
                onOpenHeadphones = { showHeadphonesDialog = true },
                isHeadphonesConnected = headphoneManager.isHeadphonesConnected,
                onDismiss = { showPlayerScreen = false },
                modifier = Modifier.fillMaxSize().background(Color(0xFF070A12))
            )
        }
    }

    // Animated Fullscreen Car Mode Screen (Spring slide up)
    AnimatedVisibility(
        visible = showCarModeScreen && currentMedia != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(220)
        ) + fadeOut(animationSpec = tween(180))
    ) {
        CarModeScreen(
            currentMedia = currentMedia,
            isPlaying = isPlaying,
            onPlayPause = {
                controller?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
            },
            onNext = {
                controller?.seekToNextMediaItem()
            },
            onPrevious = {
                controller?.seekToPreviousMediaItem()
            },
            onClose = {
                showCarModeScreen = false
            }
        )
    }

    // Animated Fullscreen Private Safe Vault Screen (Smooth Scale & Fade)
    AnimatedVisibility(
        visible = showVaultScreen,
        enter = scaleIn(initialScale = 0.93f, animationSpec = tween(220)) + fadeIn(animationSpec = tween(200)),
        exit = scaleOut(targetScale = 0.93f, animationSpec = tween(180)) + fadeOut(animationSpec = tween(160))
    ) {
        VaultScreen(
            vaultManager = vaultManager,
            onBack = {
                showVaultScreen = false
                scope.launch {
                    videos = mediaRepository.loadVideoFiles()
                }
            },
            onPlayHiddenVideo = { hiddenVideo ->
                activeVideo = hiddenVideo
            }
        )
    }

    // Animated Fullscreen Settings & Comfort Screen
    AnimatedVisibility(
        visible = showSettingsScreen,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(240, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(220, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(180))
    ) {
        SettingsScreen(
            onDismiss = { showSettingsScreen = false },
            onRescanLibrary = {
                scope.launch {
                    val reloaded = mediaRepository.loadAudioFiles()
                    songs = reloaded
                    Toast.makeText(context, "Biblioteca re-escaneada: ${reloaded.size} canciones encontradas", Toast.LENGTH_SHORT).show()
                }
            },
            onCheckUpdate = {
                scope.launch {
                    isCheckingUpdate = true
                    val info = updateManager.checkForUpdate()
                    isCheckingUpdate = false
                    if (info != null) {
                        updateInfo = info
                        showUpdateDialog = true
                    } else {
                        Toast.makeText(context, "¡DaVE está al día! Tienes la última versión (${updateManager.currentVersionName}) 🎉", Toast.LENGTH_LONG).show()
                    }
                }
            },
            isCheckingUpdate = isCheckingUpdate,
            appVersion = updateManager.currentVersionName.removePrefix("v")
        )
    }

    // Animated Fullscreen Retro Jukebox Screen
    AnimatedVisibility(
        visible = showJukeboxScreen,
        enter = scaleIn(initialScale = 0.93f, animationSpec = tween(220)) + fadeIn(animationSpec = tween(200)),
        exit = scaleOut(targetScale = 0.93f, animationSpec = tween(180)) + fadeOut(animationSpec = tween(160))
    ) {
        com.auraplayer.ui.screens.JukeboxScreen(
            songs = songs,
            currentSong = currentMedia,
            isPlaying = isPlaying,
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
            onBack = { showJukeboxScreen = false }
        )
    }

    // Animated Fullscreen Video Player Screen (Smooth Scale & Fade)
    AnimatedVisibility(
        visible = activeVideo != null,
        enter = scaleIn(initialScale = 0.95f, animationSpec = tween(220)) + fadeIn(animationSpec = tween(200)),
        exit = scaleOut(targetScale = 0.95f, animationSpec = tween(180)) + fadeOut(animationSpec = tween(160))
    ) {
        activeVideo?.let { video ->
            VideoPlayerScreen(
                video = video,
                onBack = { activeVideo = null }
            )
        }
    }

    // In-App Auto Update Dialog (DaVE Updater)
    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { if (!isDownloadingUpdate) showUpdateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚀 Nueva versión disponible", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "DaVE ${updateInfo!!.versionName} (${updateInfo!!.fileSizeMb} MB)",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = updateInfo!!.changelog.ifBlank { "Novedades, optimizaciones de velocidad y nuevas funciones." },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (isDownloadingUpdate) {
                        Text("Descargando actualización: $updateProgress%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { updateProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (!isDownloadingUpdate) {
                    val downloadedApk = updateInfo?.let { updateManager.getDownloadedApkFile(it) }
                    val alreadyDownloaded = downloadedApk != null && downloadedApk.exists() && downloadedApk.length() > 15_000_000L

                    Button(
                        onClick = {
                            if (alreadyDownloaded) {
                                updateManager.installApk(downloadedApk!!)
                                showUpdateDialog = false
                            } else {
                                isDownloadingUpdate = true
                                scope.launch {
                                    updateManager.downloadAndInstall(
                                        updateInfo = updateInfo!!,
                                        onProgress = { updateProgress = it },
                                        onSuccess = {
                                            isDownloadingUpdate = false
                                            showUpdateDialog = false
                                        },
                                        onError = { err ->
                                            isDownloadingUpdate = false
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (alreadyDownloaded) "Instalar APK" else "Actualizar Ahora")
                    }
                }
            },
            dismissButton = {
                if (!isDownloadingUpdate) {
                    Row {
                        TextButton(onClick = {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo!!.downloadUrl))
                                context.startActivity(browserIntent)
                            } catch (_: Exception) {}
                        }) {
                            Text("Navegador")
                        }
                        TextButton(onClick = { showUpdateDialog = false }) {
                            Text("Más tarde")
                        }
                    }
                }
            }
        )
    }

    // Opening cinematic splash and animated aura
    if (showAppIntro) {
        DaveSplashIntro(
            onFinish = {
                showAppIntro = false
                if (!userManager.isLoggedIn && !hasPromptedStartupAuth) {
                    hasPromptedStartupAuth = true
                    showAuthDialog = true
                }
            }
        )
    }
}
}
