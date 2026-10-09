package com.auraplayer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import com.auraplayer.MainActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.rememberCoroutineScope
import com.auraplayer.ui.components.VinylTonearm
import com.auraplayer.ui.components.AuraRippleRings
import com.auraplayer.ui.components.FloatingMusicParticles
import com.auraplayer.ui.components.PlayPauseMorphButton
import com.auraplayer.ui.components.LiquidShimmerProgressBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import com.auraplayer.ui.components.bounceClick
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.platform.LocalView
import com.auraplayer.audio.AuraHaptic
import com.auraplayer.audio.DynamicPaletteManager
import com.auraplayer.audio.RelaxAmbienceManager
import com.auraplayer.ui.components.RelaxAmbienceDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.auraplayer.audio.EqualizerManager
import com.auraplayer.ui.components.AudioCutterDialog
import com.auraplayer.ui.components.EdgeLighting
import com.auraplayer.ui.components.FluidAmbilightGlow
import com.auraplayer.ui.components.SoundboardDialog
import com.auraplayer.ui.components.StoryShareHelper
import com.auraplayer.audio.ViralAudioEffectsManager
import com.auraplayer.audio.ViralAudioMode
import com.auraplayer.data.repository.SyncedLyricsManager
import com.auraplayer.ui.components.SyncedLyricsDialog
import com.auraplayer.ui.components.CassetteTapeSkin
import com.auraplayer.ui.components.IpodClassicSkin
import com.auraplayer.ui.components.CanvasLoopsOverlay
import com.auraplayer.ui.components.CanvasLoopTheme
import com.auraplayer.ui.components.AudioQualityDialog
import com.auraplayer.ui.components.WaveformVisualizer
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auraplayer.data.model.MediaModel
import com.auraplayer.data.model.RepeatMode
import com.auraplayer.data.repository.SongLyrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    currentMedia: MediaModel?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    isFavorite: Boolean,
    playbackSpeed: Float,
    playbackPitch: Float,
    isShakeEnabled: Boolean,
    isWaveEnabled: Boolean = false,
    lyrics: SongLyrics?,
    isLoadingLyrics: Boolean,
    queueSongs: List<MediaModel> = emptyList(),
    onQueueSongClick: (MediaModel) -> Unit = {},
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShake: () -> Unit,
    onToggleWave: () -> Unit = {},
    onOpenSoundboard: () -> Unit = {},
    onOpenWrapped: () -> Unit = {},
    onAudioFxChange: (speed: Float, pitch: Float) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenCarMode: () -> Unit = {},
    onOpenStandBy: () -> Unit = {},
    onDeleteSong: (MediaModel) -> Unit,
    vibeMode: com.auraplayer.audio.AudioVibe = com.auraplayer.audio.AudioVibe.NORMAL,
    onCycleVibe: () -> com.auraplayer.audio.AudioVibe = { com.auraplayer.audio.AudioVibe.NORMAL },
    isHapticBass: Boolean = false,
    onToggleHapticBass: () -> Unit = {},
    onOpenHeadphones: () -> Unit = {},
    isHeadphonesConnected: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentMedia == null) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var isEdgeLightingEnabled by remember { mutableStateOf(false) }
    var isSpatial8D by remember { mutableStateOf(EqualizerManager.instance.isSpatial8DEnabled) }
    var centerVisualizerMode by remember { mutableIntStateOf(0) } // 0: Vinyl disc, 1: Cassette Tape, 2: Live Spectrum Waves
    var showSoundboardDialog by remember { mutableStateOf(false) }
    var showSyncedLyricsDialog by remember { mutableStateOf(false) }
    var showIpodClassicDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    val syncedLyricsManager = remember { SyncedLyricsManager.getInstance(context) }
    val viralEffectsManager = remember { ViralAudioEffectsManager.getInstance(context) }
    var currentViralMode by remember { mutableStateOf(viralEffectsManager.currentMode) }
    var currentCanvasLoop by remember { mutableStateOf(CanvasLoopTheme.OFF) }

    val tubeAmpManager = remember { com.auraplayer.audio.TubeAmpManager.getInstance(context) }
    var isTubeAmpActive by remember { mutableStateOf(tubeAmpManager.isTubeAmpEnabled) }
    var isNightDrcActive by remember { mutableStateOf(tubeAmpManager.isNightDrcEnabled) }

    var showFxDialog by remember { mutableStateOf(false) }
    var showCutterDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLyricsView by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showAudioSpecSheet by remember { mutableStateOf(false) }
    var visualizerMode by remember { mutableIntStateOf(0) } // 0: Spectrum bars, 1: Neon wave, 2: Radial pulse, 3: Starfield
    var lyricsFontSizeMultiplier by remember { mutableFloatStateOf(1.0f) }

    var seekBadgeText by remember { mutableStateOf("") }
    var showSeekBadge by remember { mutableStateOf(false) }
    var seekBadgeIsForward by remember { mutableStateOf(true) }
    var showHeartBurst by remember { mutableStateOf(false) }

    LaunchedEffect(showSeekBadge) {
        if (showSeekBadge) {
            delay(850)
            showSeekBadge = false
        }
    }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(950)
            showHeartBurst = false
        }
    }

    fun triggerPiP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val act = context as? Activity
                val params = android.app.PictureInPictureParams.Builder().build()
                act?.enterPictureInPictureMode(params)
            } catch (e: Exception) {
                Toast.makeText(context, "Ventana flotante: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Ventana flotante requiere Android 8+", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareSong() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, currentMedia.uri)
                putExtra(Intent.EXTRA_TEXT, "Escuchando '${currentMedia.title}' de ${currentMedia.artist} en DaVE Player")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir música"))
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo compartir: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    var showStickerDialog by remember { mutableStateOf(false) }
    var showPitchSpeedDialog by remember { mutableStateOf(false) }
    var showStoryCardDialog by remember { mutableStateOf(false) }
    var showFullscreenVisualizer by remember { mutableStateOf(false) }
    val view = LocalView.current
    var showQuickEqDialog by remember { mutableStateOf(false) }
    var showRelaxAmbienceDialog by remember { mutableStateOf(false) }
    var isKaraokeActive by remember { mutableStateOf(com.auraplayer.audio.KaraokeVocalManager.instance.isKaraokeEnabled) }
    val looper = remember { com.auraplayer.audio.AbLooperManager.instance }

    var qualityInfo by remember(currentMedia.path) { mutableStateOf<com.auraplayer.data.repository.AudioQualityInfo?>(null) }
    LaunchedEffect(currentMedia.path) {
        qualityInfo = com.auraplayer.data.repository.AudioQualityAnalyzer.analyze(currentMedia)
    }

    var dynamicArtworkColor by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(currentMedia.artworkUri, currentMedia.id) {
        val uri = currentMedia.artworkUri
        if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = coil.ImageLoader(context)
                    val req = coil.request.ImageRequest.Builder(context)
                        .data(uri)
                        .allowHardware(false)
                        .build()
                    val result = (loader.execute(req) as? coil.request.SuccessResult)?.drawable
                    val bmp = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    if (bmp != null) {
                        val color = DynamicPaletteManager.extractAccentColor(bmp)
                        withContext(Dispatchers.Main) {
                            dynamicArtworkColor = color
                        }
                    }
                } catch (_: Exception) {}
            }
        } else {
            dynamicArtworkColor = null
        }
    }

    BackHandler {
        if (showRelaxAmbienceDialog) {
            showRelaxAmbienceDialog = false
        } else if (showQuickEqDialog) {
            showQuickEqDialog = false
        } else if (showFullscreenVisualizer) {
            showFullscreenVisualizer = false
        } else if (showStoryCardDialog) {
            showStoryCardDialog = false
        } else if (showPitchSpeedDialog) {
            showPitchSpeedDialog = false
        } else if (showStickerDialog) {
            showStickerDialog = false
        } else if (showCutterDialog) {
            showCutterDialog = false
        } else if (showQueueSheet) {
            showQueueSheet = false
        } else if (showAudioSpecSheet) {
            showAudioSpecSheet = false
        } else if (showLyricsView) {
            showLyricsView = false
        } else if (showFxDialog) {
            showFxDialog = false
        } else if (showDeleteConfirmDialog) {
            showDeleteConfirmDialog = false
        } else {
            onDismiss()
        }
    }

    // Dynamic Atmospheric Gradient Background (100% Opaque, zero bleed-through)
    val dynamicBg = remember(currentMedia.id, dynamicArtworkColor) {
        val baseColor = dynamicArtworkColor
        if (baseColor != null) {
            val topColor = Color(
                red = (baseColor.red * 0.45f + 0.05f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.45f + 0.06f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.45f + 0.10f).coerceIn(0f, 1f),
                alpha = 1.0f
            )
            listOf(
                topColor,
                Color(0xFF101524),
                Color(0xFF070A12)
            )
        } else {
            val hash = Math.abs((currentMedia.artist + currentMedia.title).hashCode())
            val hue = (hash % 360).toFloat()
            val col1 = Color.hsl(hue, 0.45f, 0.12f)
            val col2 = Color.hsl((hue + 45) % 360, 0.30f, 0.08f)
            val col3 = Color(0xFF070A12)
            listOf(col1, col2, col3)
        }
    }

    // On-Screen HUD for Gestures (Volume & Brightness)
    var hudText by remember { mutableStateOf("") }
    var hudIcon by remember { mutableStateOf(Icons.Default.VolumeUp) }
    var hudPercent by remember { mutableFloatStateOf(0f) }
    var isHudVisible by remember { mutableStateOf(false) }
    var isPocketLocked by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val shuffleRotation = remember { Animatable(0f) }
    val repeatBounce = remember { Animatable(1f) }

    // Hardware-accelerated continuous vinyl rotation (RenderThread / GPU execution with zero Compose recomposition)
    val vinylRotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                vinylRotation.animateTo(
                    targetValue = vinylRotation.value + 360f,
                    animationSpec = tween(12000, easing = LinearEasing)
                )
            }
        }
    }

    // Visualizer wave phase animation
    val infiniteTransition = rememberInfiniteTransition(label = "player_infinite")
    val visualizerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "visualizerPhase"
    )

    val audioBadgeText = remember(currentMedia.path) {
        val ext = File(currentMedia.path).extension.lowercase()
        when (ext) {
            "flac" -> "FLAC • 24-BIT / 96kHz LOSSLESS"
            "wav" -> "WAV • 1411 KBPS LOSSLESS"
            "ape" -> "APE • MONKEY'S AUDIO"
            "dsf", "dff" -> "DSD • DIRECT STREAM DIGITAL"
            "m4a", "alac" -> "ALAC / M4A • LOSSLESS"
            "aac" -> "AAC • 256 KBPS VBR"
            "ogg", "opus" -> "OPUS • HI-RES STEREO"
            else -> "MP3 • 320 KBPS HQ AUDIO"
        }
    }

    LaunchedEffect(isHudVisible) {
        if (isHudVisible) {
            delay(1200)
            isHudVisible = false
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isPocketLocked) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        if (isPocketLocked) return@detectVerticalDragGestures
                        val isLeftSide = change.position.x < size.width / 2f
                        if (isLeftSide) {
                            // Brightness Gesture (Left side)
                            val activity = context as? Activity
                            activity?.let { act ->
                                val window = act.window
                                val params = window.attributes
                                val currentBrightness = if (params.screenBrightness < 0) {
                                    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
                                } else params.screenBrightness

                                val delta = -dragAmount / 400f
                                val newBrightness = (currentBrightness + delta).coerceIn(0.05f, 1.0f)
                                params.screenBrightness = newBrightness
                                window.attributes = params

                                hudIcon = Icons.Default.Brightness6
                                hudPercent = newBrightness
                                hudText = "Brillo: ${(newBrightness * 100).toInt()}%"
                                AuraHaptic.tick(view)
                                isHudVisible = true
                            }
                        } else {
                            // Volume Gesture (Right side)
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val delta = if (dragAmount < -12) 1 else if (dragAmount > 12) -1 else 0
                            if (delta != 0) {
                                val newVol = (currentVol + delta).coerceIn(0, maxVol)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                val pct = (newVol.toFloat() / maxVol.toFloat())
                                hudIcon = Icons.Default.VolumeUp
                                hudPercent = pct
                                hudText = "Volumen: ${(pct * 100).toInt()}%"
                                AuraHaptic.tick(view)
                                isHudVisible = true
                            }
                        }
                    }
                )
            },
        color = Color(0xFF070A12)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(dynamicBg))
        ) {
            FluidAmbilightGlow(
                primaryColor = dynamicBg.firstOrNull() ?: MaterialTheme.colorScheme.primary,
                secondaryColor = dynamicBg.getOrNull(1) ?: MaterialTheme.colorScheme.secondary
            )

            CanvasLoopsOverlay(theme = currentCanvasLoop)
            FloatingMusicParticles(
                isPlaying = isPlaying,
                accentColor = dynamicArtworkColor ?: MaterialTheme.colorScheme.primary
            )

            if (isEdgeLightingEnabled) {
                EdgeLighting(isPlaying = isPlaying)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Swipe-down dismiss handle
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.28f))
                )

                // Header bar (swipe down to dismiss)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 20f) {
                                    AuraHaptic.click(view)
                                    onDismiss()
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimizar",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showAudioSpecSheet = true }
                    ) {
                        Text(
                            text = "DaVE SOUND",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showLyricsView) "KARAOKE SYNC LYRICS" else audioBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (showLyricsView) Color(0xFFEC4899) else if (audioBadgeText.contains("LOSSLESS")) Color(0xFF06B6D4) else MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Info, contentDescription = "Detalles técnicos", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(10.dp))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Headphones Hub Button
                        IconButton(onClick = onOpenHeadphones) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "Mis Audífonos & Audio",
                                tint = if (isHeadphonesConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Car Mode Button (Spotify Style)
                        IconButton(onClick = onOpenCarMode) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Modo Conducción",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // StandBy OLED Button (Always-On Display Style)
                        IconButton(onClick = onOpenStandBy) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Modo StandBy OLED",
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Queue Button (Musicolet Style)
                        IconButton(onClick = { showQueueSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Cola de Reproducción",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Shake to Skip Toggle
                        IconButton(onClick = onToggleShake) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = "Agitar para saltar",
                                tint = if (isShakeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Lyrics / Vinyl Toggle
                        IconButton(onClick = { showLyricsView = !showLyricsView }) {
                            Icon(
                                imageVector = if (showLyricsView) Icons.Default.Album else Icons.Default.Mic,
                                contentDescription = "Letras",
                                tint = if (showLyricsView) Color(0xFFEC4899) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Sleep Timer
                        IconButton(onClick = onOpenSleepTimer) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Temporizador",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Favorite Toggle
                        IconButton(onClick = {
                            AuraHaptic.click(view)
                            onToggleFavorite()
                        }) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (isFavorite) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Delete Song
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Pocket Lock Mode
                        IconButton(onClick = {
                            AuraHaptic.click(view)
                            isPocketLocked = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Bloqueo de bolsillo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Center Display: Either Glowing Vinyl OR Synced Karaoke Lyrics (Swipe to change track)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 10.dp)
                        .pointerInput(isPocketLocked) {
                            if (isPocketLocked) return@pointerInput
                            var totalDragX = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (isPocketLocked) return@detectHorizontalDragGestures
                                    if (totalDragX < -60f) {
                                        AuraHaptic.tick(view)
                                        onNextClick()
                                    } else if (totalDragX > 60f) {
                                        AuraHaptic.tick(view)
                                        onPreviousClick()
                                    }
                                    totalDragX = 0f
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    if (isPocketLocked) return@detectHorizontalDragGestures
                                    change.consume()
                                    totalDragX += dragAmount
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!showLyricsView) {
                        if (centerVisualizerMode == 0) {
                            // Glowing Ambient Aura Disc with Vinyl Record Grooves (Dynamic Palette Glow)
                            val bassEnergy = com.auraplayer.audio.RealtimeVisualizerManager.instance.getBassEnergy(isPlaying)
                            val dynamicElevation = if (isPlaying) (18.dp + (22.dp * bassEnergy)) else 12.dp
                            val dynamicGlowAlpha = if (isPlaying) (0.35f + (bassEnergy * 0.55f)).coerceIn(0.2f, 0.95f) else 0.2f
                            val activeAccent = dynamicArtworkColor ?: MaterialTheme.colorScheme.primary

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                AuraRippleRings(
                                    isPlaying = isPlaying,
                                    accentColor = activeAccent
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.91f)
                                        .aspectRatio(1f)
                                    .shadow(
                                        elevation = dynamicElevation,
                                        shape = CircleShape,
                                        spotColor = activeAccent.copy(alpha = dynamicGlowAlpha),
                                        ambientColor = Color(0xFF8B5CF6).copy(alpha = dynamicGlowAlpha * 0.7f)
                                    )
                                    .clip(CircleShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                AuraHaptic.click(view)
                                                centerVisualizerMode = 1
                                            },
                                            onDoubleTap = { offset ->
                                                val w = size.width
                                                if (offset.x < w * 0.35f) {
                                                    onSeekRelative(-10000L)
                                                    seekBadgeText = "-10s"
                                                    seekBadgeIsForward = false
                                                    showSeekBadge = true
                                                    AuraHaptic.click(view)
                                                } else if (offset.x > w * 0.65f) {
                                                    onSeekRelative(10000L)
                                                    seekBadgeText = "+10s"
                                                    seekBadgeIsForward = true
                                                    showSeekBadge = true
                                                    AuraHaptic.click(view)
                                                } else {
                                                    onToggleFavorite()
                                                    showHeartBurst = true
                                                    AuraHaptic.heavy(view)
                                                }
                                            }
                                        )
                                    }
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFF222B40),
                                                Color(0xFF131828),
                                                Color(0xFF090D18)
                                            )
                                        )
                                    )
                                    .border(
                                        2.5.dp,
                                        Brush.sweepGradient(
                                            listOf(
                                                activeAccent,
                                                Color(0xFF38BDF8),
                                                Color(0xFFEC4899),
                                                activeAccent
                                            )
                                        ),
                                        CircleShape
                                    )
                                    .graphicsLayer {
                                        rotationZ = vinylRotation.value
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Concentric Vinyl Record Grooves
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val radiusStep = size.minDimension / 14f
                                    for (i in 3..6) {
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.045f),
                                            radius = radiusStep * i,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f)
                                        )
                                    }
                                }

                                if (currentMedia.artworkUri != null) {
                                    AsyncImage(
                                        model = currentMedia.artworkUri,
                                        contentDescription = "Carátula",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize(0.68f)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Center hole (100% opaque solid, zero bleed)
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF070A12))
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                 )
                            }

                            // Realistic Mechanical Vinyl Tonearm
                            VinylTonearm(
                                isPlaying = isPlaying,
                                accentColor = activeAccent,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 10.dp, y = (-24).dp)
                            )
                        }
                        } else if (centerVisualizerMode == 1) {
                            // Skin Retro Vintage: Cassette Tape Interactivo
                            CassetteTapeSkin(
                                song = currentMedia,
                                isPlaying = isPlaying,
                                currentPositionMs = currentPositionMs,
                                durationMs = durationMs,
                                onTogglePlay = onPlayPauseClick,
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                AuraHaptic.click(view)
                                                centerVisualizerMode = 2
                                            }
                                        )
                                    }
                            )
                        } else {
                            // Live Cyber Spectrum Waveform Visualizer (Real-time FFT audio engine)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF131A2E), Color(0xFF090D18))
                                        )
                                    )
                                    .border(
                                        1.5.dp,
                                        Brush.sweepGradient(listOf(Color(0xFF00F0FF), Color(0xFFFF0055), Color(0xFF8B5CF6), Color(0xFF00F0FF))),
                                        RoundedCornerShape(28.dp)
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                AuraHaptic.click(view)
                                                centerVisualizerMode = 0
                                            },
                                            onDoubleTap = { offset ->
                                                val w = size.width
                                                if (offset.x < w * 0.35f) {
                                                    onSeekRelative(-10000L)
                                                    seekBadgeText = "-10s"
                                                    seekBadgeIsForward = false
                                                    showSeekBadge = true
                                                    AuraHaptic.click(view)
                                                } else if (offset.x > w * 0.65f) {
                                                    onSeekRelative(10000L)
                                                    seekBadgeText = "+10s"
                                                    seekBadgeIsForward = true
                                                    showSeekBadge = true
                                                    AuraHaptic.click(view)
                                                } else {
                                                    onToggleFavorite()
                                                    showHeartBurst = true
                                                    AuraHaptic.heavy(view)
                                                }
                                            }
                                        )
                                    }
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val numBars = 20
                                    val spacing = size.width / numBars
                                    val barWidth = spacing * 0.65f
                                    val maxHeight = size.height * 0.72f
                                    val midY = size.height / 2f

                                    for (i in 0 until numBars) {
                                        val wave = com.auraplayer.audio.RealtimeVisualizerManager.instance.getBand(i, numBars, isPlaying)
                                        val barHeight = (maxHeight * wave).coerceIn(6f, maxHeight)
                                        val x = i * spacing + (spacing - barWidth) / 2f
                                        val y = midY - (barHeight / 2f)

                                        drawRoundRect(
                                            brush = Brush.verticalGradient(
                                                listOf(Color(0xFFFF0055), Color(0xFF8B5CF6), Color(0xFF00F0FF)),
                                                startY = y,
                                                endY = y + barHeight
                                            ),
                                            topLeft = Offset(x, y),
                                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                        )
                                    }
                                }

                                Text(
                                    text = "⚡ ESPECTRO CYBER EN VIVO (Toca para Vinilo)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF00F0FF).copy(alpha = 0.85f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                                )
                            }
                        }
                    } else {
                        // Karaoke Synced & Plain Lyrics View
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF0B0E17).copy(alpha = 0.9f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingLyrics) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Buscando letras en vivo...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (lyrics == null || (lyrics.lines.isEmpty() && lyrics.plainLyrics.isNullOrBlank())) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No se encontraron letras en vivo para esta pista.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Controls for lyrics
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (lyrics.lines.isNotEmpty()) "✨ Sincronizado en Vivo" else "📄 Texto Completo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFEC4899),
                                            fontWeight = FontWeight.Bold
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { lyricsFontSizeMultiplier = (lyricsFontSizeMultiplier - 0.15f).coerceAtLeast(0.7f) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.TextDecrease, contentDescription = "Menor tamaño", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { lyricsFontSizeMultiplier = (lyricsFontSizeMultiplier + 0.15f).coerceAtMost(1.6f) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.TextIncrease, contentDescription = "Mayor tamaño", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    val textToCopy = lyrics.plainLyrics ?: lyrics.lines.joinToString("\n") { it.text }
                                                    clipboardManager.setText(AnnotatedString(textToCopy))
                                                    Toast.makeText(context, "Letras copiadas al portapapeles", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    if (lyrics.lines.isNotEmpty()) {
                                        val listState = rememberLazyListState()
                                        val activeIndex = remember(currentPositionMs, lyrics.lines) {
                                            val idx = lyrics.lines.indexOfLast { currentPositionMs >= it.timeMs }
                                            if (idx >= 0) idx else 0
                                        }

                                        LaunchedEffect(activeIndex) {
                                            if (activeIndex >= 0) {
                                                listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                                            }
                                        }

                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            itemsIndexed(
                                                items = lyrics.lines,
                                                key = { index, line -> "${line.timeMs}_$index" },
                                                contentType = { _, _ -> "lyric_line" }
                                            ) { index, line ->
                                                val isActive = index == activeIndex
                                                Text(
                                                    text = line.text,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontSize = if (isActive) (20 * lyricsFontSizeMultiplier).sp else (15 * lyricsFontSizeMultiplier).sp,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isActive) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { onSeek(line.timeMs) }
                                                        .padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            item {
                                                Text(
                                                    text = lyrics.plainLyrics ?: "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontSize = (15 * lyricsFontSizeMultiplier).sp,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Animated Neon Seek Badge Overlay (+10s / -10s)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSeekBadge,
                        enter = fadeIn() + scaleIn(initialScale = 0.65f),
                        exit = fadeOut() + scaleOut(targetScale = 1.35f),
                        modifier = Modifier
                            .align(if (seekBadgeIsForward) Alignment.CenterEnd else Alignment.CenterStart)
                            .padding(horizontal = 24.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xE6080C16),
                            border = BorderStroke(
                                1.5.dp,
                                Brush.horizontalGradient(
                                    if (seekBadgeIsForward) listOf(Color(0xFF00F0FF), Color(0xFF38BDF8))
                                    else listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                                )
                            ),
                            shadowElevation = 16.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (seekBadgeIsForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                                    contentDescription = null,
                                    tint = if (seekBadgeIsForward) Color(0xFF00F0FF) else Color(0xFFEC4899),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = seekBadgeText,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // Animated Glowing Neon Heart Burst Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showHeartBurst,
                        enter = fadeIn() + scaleIn(initialScale = 0.35f),
                        exit = fadeOut() + scaleOut(targetScale = 1.6f),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .shadow(28.dp, CircleShape, spotColor = Color(0xFFFF0055), ambientColor = Color(0xFFFF2A85))
                                .background(Color(0xF0140718), CircleShape)
                                .border(
                                    2.5.dp,
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFFF0055),
                                            Color(0xFFEC4899),
                                            Color(0xFFF43F5E),
                                            Color(0xFFFF0055)
                                        )
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = Color(0xFFFF0055),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }

                // Song Title & Artist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = currentMedia.title,
                        transitionSpec = {
                            (slideInVertically { it / 2 } + fadeIn(tween(260)))
                                .togetherWith(slideOutVertically { -it / 2 } + fadeOut(tween(180)))
                        },
                        label = "titleAnim"
                    ) { t ->
                        Text(
                            text = t,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    AnimatedContent(
                        targetState = currentMedia.artist,
                        transitionSpec = {
                            (slideInVertically { it / 2 } + fadeIn(tween(260)))
                                .togetherWith(slideOutVertically { -it / 2 } + fadeOut(tween(180)))
                        },
                        label = "artistAnim"
                    ) { a ->
                        Text(
                            text = "$a • ${currentMedia.album.ifEmpty { "Aura Audio" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    qualityInfo?.let { q ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(q.qualityColor).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(q.qualityColor).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${q.codec} • ${q.bitrateKbps} kbps • ${q.qualityLabel}",
                                color = Color(q.qualityColor),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 4-Mode Interactive Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { visualizerMode = (visualizerMode + 1) % 4 },
                    contentAlignment = Alignment.Center
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val tertiaryColor = MaterialTheme.colorScheme.tertiary

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val midY = height / 2f

                        when (visualizerMode) {
                            0 -> { // Spectrum Bars (Real FFT Frequency Spectrum)
                                val barCount = 28
                                val barWidth = width / (barCount * 1.6f)
                                for (i in 0 until barCount) {
                                    val factor = com.auraplayer.audio.RealtimeVisualizerManager.instance.getBand(i, barCount, isPlaying)
                                    val barHeight = (height * factor).coerceIn(4f, height)
                                    val x = i * (barWidth * 1.6f) + (barWidth * 0.3f)
                                    val y = midY - (barHeight / 2f)

                                    drawRoundRect(
                                        brush = Brush.verticalGradient(listOf(secondaryColor, primaryColor)),
                                        topLeft = Offset(x, y),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                            1 -> { // Neon Wave (Real Audio Waveform Oscilloscope)
                                val path = Path()
                                path.moveTo(0f, midY)
                                val points = 80
                                for (i in 0..points) {
                                    val x = (i.toFloat() / points) * width
                                    val wave = com.auraplayer.audio.RealtimeVisualizerManager.instance.getWaveform(i, points, isPlaying)
                                    val y = midY + (wave * (height * 0.45f))
                                    path.lineTo(x, y)
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor, tertiaryColor, primaryColor)),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                            2 -> { // Dual Laser Pulse (Real Bass and Treble Modulated)
                                val path1 = Path()
                                val path2 = Path()
                                path1.moveTo(0f, midY)
                                path2.moveTo(0f, midY)
                                val points = 60
                                val bassEnergy = com.auraplayer.audio.RealtimeVisualizerManager.instance.getBassEnergy(isPlaying)
                                val trebleEnergy = com.auraplayer.audio.RealtimeVisualizerManager.instance.getTrebleEnergy(isPlaying)
                                for (i in 0..points) {
                                    val x = (i.toFloat() / points) * width
                                    val wave1 = com.auraplayer.audio.RealtimeVisualizerManager.instance.getWaveform(i, points, isPlaying) * (height * 0.42f) * (0.5f + bassEnergy)
                                    val wave2 = -com.auraplayer.audio.RealtimeVisualizerManager.instance.getWaveform(points - i, points, isPlaying) * (height * 0.42f) * (0.5f + trebleEnergy)
                                    path1.lineTo(x, midY + wave1)
                                    path2.lineTo(x, midY + wave2)
                                }
                                drawPath(path1, Brush.horizontalGradient(listOf(primaryColor, tertiaryColor)), style = Stroke(width = 2.dp.toPx()))
                                drawPath(path2, Brush.horizontalGradient(listOf(secondaryColor, primaryColor)), style = Stroke(width = 2.dp.toPx()))
                            }
                            3 -> { // Starfield Beat Dots (Real Frequency Pulsing)
                                val dotCount = 20
                                for (i in 0 until dotCount) {
                                    val x = (i.toFloat() / dotCount) * width + (width / (dotCount * 2))
                                    val factor = com.auraplayer.audio.RealtimeVisualizerManager.instance.getBand(i, dotCount, isPlaying)
                                    val radius = 3.dp.toPx() + (factor * 7.dp.toPx())
                                    val color = if (i % 2 == 0) primaryColor else secondaryColor
                                    drawCircle(color = color, radius = radius, center = Offset(x, midY))
                                }
                            }
                        }
                    }
                }

                // Seek Bar & Timeline
                Column(modifier = Modifier.fillMaxWidth()) {
                    val effectiveDurationMs = remember(durationMs, currentMedia.duration) {
                        if (durationMs > 0L) durationMs else currentMedia.duration
                    }
                    val sliderValue = if (effectiveDurationMs > 0L) (currentPositionMs.toFloat() / effectiveDurationMs.toFloat()) else 0f

                    // Dynamic Rhythmic Audio Waveform Track (Reactive to real audio frequencies)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val barCount = 36
                        for (i in 0 until barCount) {
                            val barProgress = i.toFloat() / barCount
                            val isPassed = sliderValue >= barProgress
                            val energy = com.auraplayer.audio.RealtimeVisualizerManager.instance.getBand(i, barCount, isPlaying)
                            val baseHeight = 3.dp + (11.dp * energy)
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(baseHeight)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(
                                        if (isPassed) Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6)))
                                        else Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                                    )
                            )
                        }
                    }

                    LiquidShimmerProgressBar(
                        progress = sliderValue,
                        isPlaying = isPlaying,
                        onSeek = { percent ->
                            val targetMs = (percent * effectiveDurationMs).toLong()
                            onSeek(targetMs)
                        },
                        activeColor = Color(0xFF38BDF8),
                        secondaryColor = Color(0xFF8B5CF6)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val remainingMs = (effectiveDurationMs - currentPositionMs).coerceAtLeast(0L)
                        Text(
                            text = if (effectiveDurationMs > 0L) "-${formatTime(remainingMs)}" else "--:--",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // FX Studio & Seek Jump Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio FX Studio pill
                    val fxLabel = when {
                        playbackSpeed == 0.85f && playbackPitch == 0.85f -> "🌌 SLOWED"
                        playbackSpeed == 1.25f && playbackPitch == 1.25f -> "⚡ NIGHTCORE"
                        playbackSpeed == 0.75f && playbackPitch == 0.75f -> "📼 VAPORWAVE"
                        playbackSpeed == 1.35f && playbackPitch == 1.0f -> "🚀 SPED UP"
                        playbackSpeed == 1.0f && playbackPitch == 1.0f -> "🎵 ORIGINAL"
                        else -> "🎛️ ${playbackSpeed}x"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { showFxDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "FX Studio",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = fxLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Tube Amp Analog Warmth Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTubeAmpActive) Color(0xFFD97706).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, if (isTubeAmpActive) Color(0xFFF59E0B) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable {
                                isTubeAmpActive = !isTubeAmpActive
                                tubeAmpManager.isTubeAmpEnabled = isTubeAmpActive
                                if (isTubeAmpActive) {
                                    EqualizerManager.instance.setBassBoostStrength(650.toShort())
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isTubeAmpActive) "🔥 TUBE AMP" else "💡 TUBE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isTubeAmpActive) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    // Night DRC Compressor Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isNightDrcActive) Color(0xFF6366F1).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, if (isNightDrcActive) Color(0xFF818CF8) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable {
                                isNightDrcActive = !isNightDrcActive
                                tubeAmpManager.isNightDrcEnabled = isNightDrcActive
                                EqualizerManager.instance.setReplayGainEnabled(isNightDrcActive)
                            }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isNightDrcActive) "🌙 DRC NOCHE" else "🌙 DRC",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isNightDrcActive) Color(0xFF818CF8) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    // Karaoke Vocal Reducer Pill
                    val gaplessManager = remember { com.auraplayer.audio.GaplessManager.getInstance(context) }
                    var isKaraokeActive by remember { mutableStateOf(gaplessManager.isVocalReducerEnabled) }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isKaraokeActive) Color(0xFFE040FB).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, if (isKaraokeActive) Color(0xFFE040FB) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable {
                                isKaraokeActive = !isKaraokeActive
                                gaplessManager.toggleVocalReducer(isKaraokeActive)
                                Toast.makeText(context, if (isKaraokeActive) "🎤 Modo Karaoke ACTIVADO (Voz Reducida)" else "🎤 Modo Karaoke DESACTIVADO", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isKaraokeActive) "🎤 KARAOKE" else "🎙️ VOCAL",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isKaraokeActive) Color(0xFFE040FB) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    // Viral Audio Mode Pill (Slowed+Reverb / Nightcore)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (currentViralMode != ViralAudioMode.NORMAL) Color(0xFF00F5FF).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, if (currentViralMode != ViralAudioMode.NORMAL) Color(0xFF00F5FF) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable {
                                currentViralMode = viralEffectsManager.cycleMode(MainActivity.activeController)
                                Toast.makeText(context, "Efecto: ${currentViralMode.displayName} (${currentViralMode.tag})", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (currentViralMode) {
                                ViralAudioMode.NORMAL -> "⚡ VIRAL"
                                ViralAudioMode.SLOWED_REVERB -> "🌊 SLOWED"
                                ViralAudioMode.NIGHTCORE -> "⚡ NIGHTCORE"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentViralMode != ViralAudioMode.NORMAL) Color(0xFF00F5FF) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    // Synced Lyrics LRC Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { showSyncedLyricsDialog = true }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🎤 LRC EN VIVO",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }

                    // Retro iPod Classic Skin Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { showIpodClassicDialog = true }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🕹️ iPOD",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp
                        )
                    }

                    // Canvas Loop Ambient Background Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (currentCanvasLoop != CanvasLoopTheme.OFF) Color(0xFFE879F9).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, if (currentCanvasLoop != CanvasLoopTheme.OFF) Color(0xFFE879F9) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable {
                                currentCanvasLoop = when (currentCanvasLoop) {
                                    CanvasLoopTheme.OFF -> CanvasLoopTheme.RAIN
                                    CanvasLoopTheme.RAIN -> CanvasLoopTheme.SYNTHWAVE
                                    CanvasLoopTheme.SYNTHWAVE -> CanvasLoopTheme.STARFIELD
                                    CanvasLoopTheme.STARFIELD -> CanvasLoopTheme.OFF
                                }
                                Toast.makeText(context, "Fondo: ${currentCanvasLoop.label}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (currentCanvasLoop != CanvasLoopTheme.OFF) "🌌 ${currentCanvasLoop.name}" else "🌌 CANVAS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentCanvasLoop != CanvasLoopTheme.OFF) Color(0xFFE879F9) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }

                    // Ringtone Cutter Tool
                    IconButton(
                        onClick = { showCutterDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = "Cortar Audio / Ringtone",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Floating Lyrics / PiP Mode
                    IconButton(
                        onClick = { triggerPiP() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Letras Flotantes PiP",
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Native Share
                    IconButton(
                        onClick = { shareSong() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = { onSeekRelative(-10000L) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Retroceder 10s",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { onSeekRelative(10000L) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Avanzar 10s",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // VIP Pro Suite Quick Action Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 8D Audio Chip
                    FilterChip(
                        selected = isSpatial8D,
                        onClick = {
                            val newState = !isSpatial8D
                            isSpatial8D = newState
                            EqualizerManager.instance.setSpatial8DEnabled(newState)
                            val msg = if (newState) "🎧 Sonido Espacial 8D: ACTIVADO" else "Sonido Espacial 8D: DESACTIVADO"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("🎧 8D Espacial", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00F0FF).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFF00F0FF)
                        )
                    )

                    // Wave Control Chip
                    FilterChip(
                        selected = isWaveEnabled,
                        onClick = onToggleWave,
                        label = { Text("🌊 Wave Control", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFF10B981)
                        )
                    )

                    // Neon Edge Lighting Chip
                    FilterChip(
                        selected = isEdgeLightingEnabled,
                        onClick = {
                            isEdgeLightingEnabled = !isEdgeLightingEnabled
                            val msg = if (isEdgeLightingEnabled) "🌈 Borde Neón: ACTIVADO" else "Borde Neón: DESACTIVADO"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("🌈 Borde Neón", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF0055).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFFFF0055)
                        )
                    )

                    // Share Story Card Chip
                    FilterChip(
                        selected = false,
                        onClick = { StoryShareHelper.shareMusicStory(context, currentMedia) },
                        label = { Text("📸 Estado / Historia", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color(0xFFEC4899)
                        )
                    )

                    // DJ Soundboard Chip
                    FilterChip(
                        selected = false,
                        onClick = { showSoundboardDialog = true },
                        label = { Text("📢 DJ Soundboard", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color(0xFFFFD700)
                        )
                    )

                    // Wrapped Stats Chip
                    FilterChip(
                        selected = false,
                        onClick = onOpenWrapped,
                        label = { Text("🏆 Wrapped", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color(0xFF38BDF8)
                        )
                    )

                    // Audio Vibe Mode Chip (Slowed+Reverb / Nightcore / Normal)
                    FilterChip(
                        selected = vibeMode != com.auraplayer.audio.AudioVibe.NORMAL,
                        onClick = {
                            val next = onCycleVibe()
                            Toast.makeText(context, "${next.icon} Modo: ${next.label}", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("${vibeMode.icon} ${vibeMode.label}", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFA855F7).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFFA855F7)
                        )
                    )

                    // Bass Haptics Chip
                    FilterChip(
                        selected = isHapticBass,
                        onClick = onToggleHapticBass,
                        label = { Text("📳 Bajos Hápticos", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFFF59E0B)
                        )
                    )

                    // WhatsApp Sticker Generator Chip
                    FilterChip(
                        selected = false,
                        onClick = { showStickerDialog = true },
                        label = { Text("💬 Crear Sticker", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color(0xFF25D366)
                        )
                    )

                    // Karaoke Vocal Remover Chip
                    FilterChip(
                        selected = isKaraokeActive,
                        onClick = {
                            isKaraokeActive = com.auraplayer.audio.KaraokeVocalManager.instance.toggleKaraoke(context)
                        },
                        label = { Text(if (isKaraokeActive) "🎤 Karaoke: ACTIVO" else "🎤 Modo Karaoke", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE040FB).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFFE040FB),
                            labelColor = Color(0xFFE040FB)
                        )
                    )

                    // Pitch & Speed Shifter Chip
                    FilterChip(
                        selected = false,
                        onClick = { showPitchSpeedDialog = true },
                        label = { Text("🎚️ Tono & Temp", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color(0xFF38BDF8)
                        )
                    )

                    // 9:16 Story Lyric Card Chip
                    FilterChip(
                        selected = false,
                        onClick = { showStoryCardDialog = true },
                        label = { Text("📱 Historia 9:16", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color(0xFFEC4899)
                        )
                    )

                    // 3D Fullscreen Visualizer & OLED Saver Chip
                    FilterChip(
                        selected = false,
                        onClick = { showFullscreenVisualizer = true },
                        label = { Text("🌌 Visualizador 3D", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color(0xFF00F0FF)
                        )
                    )

                    // A-B Looper Chip
                    FilterChip(
                        selected = looper.isEnabled,
                        onClick = {
                            if (looper.pointAMs == null) {
                                looper.setPointA(currentPositionMs)
                                Toast.makeText(context, "Punto A marcado en ${formatTime(currentPositionMs)}", Toast.LENGTH_SHORT).show()
                            } else if (looper.pointBMs == null) {
                                looper.setPointB(currentPositionMs)
                                Toast.makeText(context, "Punto B marcado en ${formatTime(currentPositionMs)} (Bucle activo)", Toast.LENGTH_SHORT).show()
                            } else {
                                looper.clear()
                                Toast.makeText(context, "Bucle A-B reiniciado", Toast.LENGTH_SHORT).show()
                            }
                        },
                        label = {
                            Text(
                                when {
                                    looper.pointAMs == null -> "🔂 Marcar A-B"
                                    looper.pointBMs == null -> "🔂 Marcar Fin B"
                                    else -> "🔂 Bucle Activo [A-B]"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFD700).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFFFFD700),
                            labelColor = Color(0xFFFFD700)
                        )
                    )

                    // Audífonos Hub Chip
                    FilterChip(
                        selected = isHeadphonesConnected,
                        onClick = onOpenHeadphones,
                        label = { Text(if (isHeadphonesConnected) "🎧 Audífonos" else "🔊 Salida de Audio", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFF10B981),
                            labelColor = if (isHeadphonesConnected) Color(0xFF10B981) else Color(0xFF38BDF8)
                        )
                    )

                    // Quick EQ Chip
                    FilterChip(
                        selected = false,
                        onClick = {
                            AuraHaptic.click(view)
                            showQuickEqDialog = true
                        },
                        label = { Text("🎚️ EQ Rápido", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color(0xFF38BDF8)
                        )
                    )

                    // Relax Ambience Chip
                    val ambienceMgr = remember { RelaxAmbienceManager.instance }
                    FilterChip(
                        selected = ambienceMgr.currentSound != com.auraplayer.audio.AmbienceSound.NONE,
                        onClick = {
                            AuraHaptic.click(view)
                            showRelaxAmbienceDialog = true
                        },
                        label = {
                            Text(
                                if (ambienceMgr.currentSound != com.auraplayer.audio.AmbienceSound.NONE)
                                    "${ambienceMgr.currentSound.emoji} ${ambienceMgr.currentSound.title}"
                                else "🌧️ Relax & Ambiente",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFF38BDF8),
                            labelColor = Color(0xFF38BDF8)
                        )
                    )
                }

                // Primary Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(
                        onClick = {
                            AuraHaptic.click(view)
                            coroutineScope.launch {
                                shuffleRotation.snapTo(0f)
                                shuffleRotation.animateTo(
                                    360f,
                                    spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
                                )
                            }
                            onShuffleToggle()
                        },
                        modifier = Modifier
                            .graphicsLayer { rotationZ = shuffleRotation.value }
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = {
                            AuraHaptic.tick(view)
                            onPreviousClick()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Aesthetic Glowing Play / Pause with Elastic Morphing & Glow Burst
                    PlayPauseMorphButton(
                        isPlaying = isPlaying,
                        onClick = {
                            AuraHaptic.heavy(view)
                            onPlayPauseClick()
                        },
                        size = 76.dp,
                        iconSize = 42.dp,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.secondary
                    )

                    // Next
                    IconButton(
                        onClick = {
                            AuraHaptic.tick(view)
                            onNextClick()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Repeat with Elastic Pop Animation
                    IconButton(
                        onClick = {
                            AuraHaptic.click(view)
                            coroutineScope.launch {
                                repeatBounce.animateTo(1.35f, tween(100))
                                repeatBounce.animateTo(1f, spring(dampingRatio = 0.5f))
                            }
                            onRepeatToggle()
                        },
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = repeatBounce.value
                                scaleY = repeatBounce.value
                            }
                            .bounceClick()
                    ) {
                        Icon(
                            imageVector = when (repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repetir",
                            tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Floating Cyberpunk HUD (Volume / Brightness)
            AnimatedVisibility(
                visible = isHudVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0F1424).copy(alpha = 0.92f))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = hudIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = hudText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        // Cyberpunk Neon Bar
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(hudPercent.coerceIn(0f, 1f))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(MaterialTheme.colorScheme.primary, Color(0xFF06B6D4))
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // Pocket Lock Overlay
            AnimatedVisibility(
                visible = isPocketLocked,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.88f))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable {
                                    AuraHaptic.heavy(view)
                                    isPocketLocked = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Desbloquear pantalla",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Modo Bolsillo Activado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toque el candado para desbloquear la pantalla",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Audiophile Technical Spec Sheet Dialog
    if (showAudioSpecSheet) {
        val file = File(currentMedia.path)
        val sizeMb = if (currentMedia.size > 0) String.format("%.2f MB", currentMedia.size / (1024.0 * 1024.0)) else "Desconocido"
        val kbpsEstimate = if (durationMs > 0 && currentMedia.size > 0) {
            val kb = currentMedia.size * 8 / 1024
            val sec = durationMs / 1000
            if (sec > 0) "${kb / sec} kbps" else "320 kbps"
        } else "320 kbps"

        AlertDialog(
            onDismissRequest = { showAudioSpecSheet = false },
            containerColor = Color(0xFF101422),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Inspector Audiófilo", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Insignia de Formato", audioBadgeText)
                    DetailRow("Bitrate Estimado", kbpsEstimate)
                    DetailRow("Motor de Procesamiento", "32-bit Float DSP • 48.0 kHz Estéreo")
                    DetailRow("Tamaño del Archivo", sizeMb)
                    DetailRow("Ruta", currentMedia.path)
                }
            },
            confirmButton = {
                Button(onClick = { showAudioSpecSheet = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Aura Audio FX Studio Dialog (Slowed, Nightcore, Vaporwave, Custom Pitch & Speed)
    if (showFxDialog) {
        var tempSpeed by remember { mutableFloatStateOf(playbackSpeed) }
        var tempPitch by remember { mutableFloatStateOf(playbackPitch) }

        AlertDialog(
            onDismissRequest = { showFxDialog = false },
            containerColor = Color(0xFF101422),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DaVE Studio FX",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Estilos y Efectos Predefinidos:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Buttons Grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FxPresetButton("Original", "1.0x", tempSpeed == 1.0f && tempPitch == 1.0f, Modifier.weight(1f)) {
                            tempSpeed = 1.0f
                            tempPitch = 1.0f
                            onAudioFxChange(1.0f, 1.0f)
                        }
                        FxPresetButton("Slowed", "0.85x", tempSpeed == 0.85f && tempPitch == 0.85f, Modifier.weight(1f)) {
                            tempSpeed = 0.85f
                            tempPitch = 0.85f
                            onAudioFxChange(0.85f, 0.85f)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FxPresetButton("Nightcore", "1.25x", tempSpeed == 1.25f && tempPitch == 1.25f, Modifier.weight(1f)) {
                            tempSpeed = 1.25f
                            tempPitch = 1.25f
                            onAudioFxChange(1.25f, 1.25f)
                        }
                        FxPresetButton("Vaporwave", "0.75x", tempSpeed == 0.75f && tempPitch == 0.75f, Modifier.weight(1f)) {
                            tempSpeed = 0.75f
                            tempPitch = 0.75f
                            onAudioFxChange(0.75f, 0.75f)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Semitones Vocal Pitch Shift Chips (Karaoke Key Shifter)
                    Text(text = "Cambio de Tonalidad Vocal (Semitonos):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(-3 to "-3♭", -2 to "-2♭", -1 to "-1♭", 0 to "0", 1 to "+1♯", 2 to "+2♯", 3 to "+3♯").forEach { (semi, lbl) ->
                            val pitchVal = Math.pow(2.0, semi.toDouble() / 12.0).toFloat()
                            val isSelected = kotlin.math.abs(tempPitch - pitchVal) < 0.03f
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .clickable {
                                        tempPitch = pitchVal
                                        onAudioFxChange(tempSpeed, tempPitch)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Velocidad de Reproducción", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(String.format("%.2fx", tempSpeed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = tempSpeed,
                        onValueChange = {
                            tempSpeed = it
                            onAudioFxChange(tempSpeed, tempPitch)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pitch Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tono Musical (Pitch Shift)", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Text(String.format("%.2fx", tempPitch), style = MaterialTheme.typography.bodySmall, color = Color(0xFFEC4899), fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = tempPitch,
                        onValueChange = {
                            tempPitch = it
                            onAudioFxChange(tempSpeed, tempPitch)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFEC4899), activeTrackColor = Color(0xFFEC4899))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFxDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Listo", color = Color.White)
                }
            }
        )
    }

    // Interactive Queue Sheet Modal
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF0D1222),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF38BDF8).copy(alpha = 0.6f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cola de Reproducción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${queueSongs.size} canciones en cola",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    IconButton(onClick = { showQueueSheet = false }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (queueSongs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("La cola de reproducción está vacía", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = queueSongs,
                            key = { idx, song -> "${song.id}_$idx" }
                        ) { index, song ->
                            val isPlayingThis = song.id == currentMedia.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isPlayingThis) Color(0xFF1E1B4B).copy(alpha = 0.85f)
                                        else Color(0xFF13182E).copy(alpha = 0.6f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isPlayingThis) Color(0xFF38BDF8).copy(alpha = 0.8f)
                                        else Color.White.copy(alpha = 0.06f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        AuraHaptic.click(view)
                                        onQueueSongClick(song)
                                        showQueueSheet = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover or Note Icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (song.artworkUri != null) {
                                        AsyncImage(
                                            model = song.artworkUri,
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (isPlayingThis && isPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.GraphicEq,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isPlayingThis) Color(0xFF38BDF8) else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.artist} • ${song.formattedDuration}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isPlayingThis) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isPlayingThis) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Sonando",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "¿Eliminar canción?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Se eliminará permanentemente '${currentMedia.title}' del almacenamiento de tu teléfono.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteSong(currentMedia)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = Color(0xFF101422),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Audio Cutter & Ringtone Maker Dialog
    if (showCutterDialog && currentMedia != null) {
        AudioCutterDialog(
            song = currentMedia,
            onDismiss = { showCutterDialog = false }
        )
    }

    // Party Soundboard Dialog
    if (showSoundboardDialog) {
        SoundboardDialog(
            onDismiss = { showSoundboardDialog = false }
        )
    }

    // WhatsApp Sticker Generator Dialog
    if (showStickerDialog) {
        com.auraplayer.ui.components.StickerGeneratorDialog(
            song = currentMedia,
            onDismiss = { showStickerDialog = false }
        )
    }

    // Pitch & Speed Shifter Dialog
    if (showPitchSpeedDialog) {
        com.auraplayer.ui.components.PitchSpeedDialog(
            initialSpeed = playbackSpeed,
            initialPitch = playbackPitch,
            onApply = onAudioFxChange,
            onDismiss = { showPitchSpeedDialog = false }
        )
    }

    // 9:16 Story Lyric Card Generator
    if (showStoryCardDialog) {
        com.auraplayer.ui.components.StoryCardDialog(
            song = currentMedia,
            onDismiss = { showStoryCardDialog = false }
        )
    }

    // 3D Fullscreen Visualizer & OLED Pure Black
    if (showFullscreenVisualizer) {
        com.auraplayer.ui.components.VisualizerFullscreenDialog(
            song = currentMedia,
            isPlaying = isPlaying,
            onDismiss = { showFullscreenVisualizer = false }
        )
    }

    // Quick 3-Band Equalizer Dialog
    if (showQuickEqDialog) {
        com.auraplayer.ui.components.QuickEqDialog(
            onDismiss = { showQuickEqDialog = false }
        )
    }

    // Relax & Ambience Mixer Dialog
    if (showRelaxAmbienceDialog) {
        RelaxAmbienceDialog(
            onDismiss = { showRelaxAmbienceDialog = false }
        )
    }

    // Synced Lyrics LRC Dialog
    if (showSyncedLyricsDialog) {
        SyncedLyricsDialog(
            song = currentMedia,
            currentPositionMs = currentPositionMs,
            lyricsManager = syncedLyricsManager,
            onSeekTo = onSeek,
            onDismiss = { showSyncedLyricsDialog = false }
        )
    }

    // Retro iPod Classic Skin Dialog
    if (showIpodClassicDialog) {
        IpodClassicSkin(
            song = currentMedia,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onTogglePlay = onPlayPauseClick,
            onNext = onNextClick,
            onPrevious = onPreviousClick,
            onSeekRelative = onSeekRelative,
            onDismiss = { showIpodClassicDialog = false }
        )
    }

    // Audio Quality Inspector Dialog
    if (showAudioQualityDialog) {
        AudioQualityDialog(
            song = currentMedia,
            onDismiss = { showAudioQualityDialog = false }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FxPresetButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
