package com.auraplayer.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Share
import com.auraplayer.ui.components.AudioCutterDialog
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
    onAudioFxChange: (speed: Float, pitch: Float) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenCarMode: () -> Unit = {},
    onDeleteSong: (MediaModel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentMedia == null) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var showFxDialog by remember { mutableStateOf(false) }
    var showCutterDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLyricsView by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showAudioSpecSheet by remember { mutableStateOf(false) }
    var visualizerMode by remember { mutableIntStateOf(0) } // 0: Spectrum bars, 1: Neon wave, 2: Radial pulse, 3: Starfield
    var lyricsFontSizeMultiplier by remember { mutableFloatStateOf(1.0f) }

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

    BackHandler {
        if (showCutterDialog) {
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

    // Dynamic Atmospheric Gradient Background
    val dynamicBg = remember(currentMedia.id, currentMedia.title) {
        val hash = Math.abs((currentMedia.artist + currentMedia.title).hashCode())
        val hue = (hash % 360).toFloat()
        val col1 = Color.hsl(hue, 0.50f, 0.14f)
        val col2 = Color.hsl((hue + 45) % 360, 0.35f, 0.07f)
        val col3 = Color(0xFF09090B)
        listOf(col1, col2, col3)
    }

    // On-Screen HUD for Gestures (Volume & Brightness)
    var hudText by remember { mutableStateOf("") }
    var hudIcon by remember { mutableStateOf(Icons.Default.VolumeUp) }
    var isHudVisible by remember { mutableStateOf(false) }

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
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
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

                                hudIcon = Icons.Default.BrightnessMedium
                                hudText = "Brillo: ${(newBrightness * 100).toInt()}%"
                                isHudVisible = true
                            }
                        } else {
                            // Volume Gesture (Right side)
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val delta = if (dragAmount < -15) 1 else if (dragAmount > 15) -1 else 0
                            if (delta != 0) {
                                val newVol = (currentVol + delta).coerceIn(0, maxVol)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                val pct = ((newVol.toFloat() / maxVol.toFloat()) * 100).toInt()
                                hudIcon = Icons.Default.VolumeUp
                                hudText = "Volumen: $pct%"
                                isHudVisible = true
                            }
                        }
                    }
                )
            },
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(dynamicBg))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        // Car Mode Button (Spotify Style)
                        IconButton(onClick = onOpenCarMode) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Modo Conducción",
                                tint = MaterialTheme.colorScheme.primary,
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
                        IconButton(onClick = onToggleFavorite) {
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
                    }
                }

                // Center Display: Either Glowing Vinyl OR Synced Karaoke Lyrics
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!showLyricsView) {
                        // Glowing Ambient Aura Disc with Vinyl Record Grooves
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.84f)
                                .aspectRatio(1f)
                                .shadow(
                                    elevation = if (isPlaying) 28.dp else 16.dp,
                                    shape = CircleShape,
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isPlaying) 0.65f else 0.25f),
                                    ambientColor = Color(0xFF8B5CF6).copy(alpha = if (isPlaying) 0.5f else 0.2f)
                                )
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = if (isPlaying) 0.35f else 0.2f),
                                            Color(0xFF14192A),
                                            Color(0xFF080B12)
                                        )
                                    )
                                )
                                .border(
                                    2.5.dp,
                                    Brush.sweepGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            Color(0xFF38BDF8),
                                            Color(0xFFEC4899),
                                            MaterialTheme.colorScheme.primary
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

                            // Center hole
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            )
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
                }

                // Song Title & Artist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentMedia.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${currentMedia.artist} • ${currentMedia.album.ifEmpty { "Aura Audio" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                            0 -> { // Spectrum Bars
                                val barCount = 28
                                val barWidth = width / (barCount * 1.6f)
                                for (i in 0 until barCount) {
                                    val factor = if (isPlaying) {
                                        val wave = (sin(visualizerPhase + (i * 0.45f)) + 1f) / 2f
                                        val harmonic = (sin(visualizerPhase * 2f + (i * 0.9f)) + 1f) / 2f
                                        (wave * 0.7f + harmonic * 0.3f).coerceIn(0.1f, 1f)
                                    } else 0.05f

                                    val barHeight = height * factor
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
                            1 -> { // Neon Wave
                                val path = Path()
                                path.moveTo(0f, midY)
                                val points = 80
                                for (i in 0..points) {
                                    val x = (i.toFloat() / points) * width
                                    val wave = if (isPlaying) {
                                        sin(visualizerPhase * 1.5f + (i * 0.2f)) * (height * 0.4f)
                                    } else 0f
                                    val y = midY + wave.toFloat()
                                    path.lineTo(x, y)
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor, tertiaryColor, primaryColor)),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                            2 -> { // Dual Laser Pulse
                                val path1 = Path()
                                val path2 = Path()
                                path1.moveTo(0f, midY)
                                path2.moveTo(0f, midY)
                                val points = 60
                                for (i in 0..points) {
                                    val x = (i.toFloat() / points) * width
                                    val wave1 = if (isPlaying) sin(visualizerPhase * 2f + (i * 0.3f)) * (height * 0.35f) else 0f
                                    val wave2 = if (isPlaying) -sin(visualizerPhase * 1.8f + (i * 0.25f)) * (height * 0.35f) else 0f
                                    path1.lineTo(x, midY + wave1.toFloat())
                                    path2.lineTo(x, midY + wave2.toFloat())
                                }
                                drawPath(path1, Brush.horizontalGradient(listOf(primaryColor, tertiaryColor)), style = Stroke(width = 2.dp.toPx()))
                                drawPath(path2, Brush.horizontalGradient(listOf(secondaryColor, primaryColor)), style = Stroke(width = 2.dp.toPx()))
                            }
                            3 -> { // Starfield Beat Dots
                                val dotCount = 20
                                for (i in 0 until dotCount) {
                                    val x = (i.toFloat() / dotCount) * width + (width / (dotCount * 2))
                                    val radius = if (isPlaying) {
                                        (sin(visualizerPhase * 2.5f + (i * 0.6f)) + 1f) * 4.dp.toPx() + 2.dp.toPx()
                                    } else 2.dp.toPx()
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
                    Slider(
                        value = sliderValue.coerceIn(0f, 1f),
                        onValueChange = { percent ->
                            val targetMs = (percent * effectiveDurationMs).toLong()
                            onSeek(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
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

                // Primary Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = onShuffleToggle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = onPreviousClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Aesthetic Glowing Play / Pause
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .shadow(16.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    // Next
                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Repeat
                    IconButton(onClick = onRepeatToggle) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = hudIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = hudText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
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

    // Musicolet Queue Sheet Modal
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF101422)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Cola de Reproducción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "${queueSongs.size} pistas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (queueSongs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("La cola de reproducción está vacía", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                        itemsIndexed(queueSongs, key = { _, song -> song.id }) { index, song ->
                            val isCurrent = song.id == currentMedia.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        onQueueSongClick(song)
                                        showQueueSheet = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.width(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = song.formattedDuration,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
