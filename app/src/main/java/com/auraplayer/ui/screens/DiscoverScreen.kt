package com.auraplayer.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.auraplayer.audio.AuraHaptic
import com.auraplayer.data.model.OnlineTrack
import com.auraplayer.data.repository.DownloadEngine
import com.auraplayer.data.repository.DownloadStatus
import com.auraplayer.data.repository.GlobalSearchService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    searchService: GlobalSearchService,
    downloadEngine: DownloadEngine,
    onPreviewTrack: (OnlineTrack) -> Unit,
    onDownloadComplete: () -> Unit,
    currentPlayingTitle: String? = null,
    isPlaying: Boolean = false,
    onAddToQueue: ((OnlineTrack) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("Todas") } // "Todas", "Jamendo", "Deezer", "Archive"
    var selectedGenre by remember { mutableStateOf("Trending") }
    var trackList by remember { mutableStateOf<List<OnlineTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // Batch download state
    var isDownloadingBatch by remember { mutableStateOf(false) }
    var batchTotal by remember { mutableIntStateOf(0) }
    var batchCompleted by remember { mutableIntStateOf(0) }
    var batchCurrentTitle by remember { mutableStateOf("") }
    var showBatchConfirmDialog by remember { mutableStateOf(false) }

    val appPrefs = remember { context.getSharedPreferences("dave_app_prefs", Context.MODE_PRIVATE) }
    var downloadQuality by remember { mutableStateOf(appPrefs.getString("download_quality", "320") ?: "320") }
    var trackForDownloadChoice by remember { mutableStateOf<OnlineTrack?>(null) }

    // When searching or viewing specific genre, BackHandler resets to Trending
    BackHandler(enabled = searchQuery.isNotBlank() || selectedGenre != "Trending") {
        if (searchQuery.isNotBlank()) {
            searchQuery = ""
        }
        selectedGenre = "Trending"
        scope.launch {
            isLoading = true
            trackList = searchService.getTrending("Trending", selectedSource)
            isLoading = false
        }
    }

    val downloadStates by downloadEngine.downloadStates.collectAsState()

    val sourceFilters = listOf(
        "Todas" to "🌐 Todas (Completas)",
        "YouTube" to "🔴 YouTube (RYT)",
        "Jamendo" to "⚡ Jamendo (Full)"
    )

    val genres = listOf(
        "TikTok" to "🎵 Top TikTok",
        "Trending" to "🔥 Tendencias",
        "Pop" to "⚡ Pop",
        "Rock" to "🎸 Rock",
        "Electronic" to "🎧 Dance / EDM",
        "Lofi" to "☕ Lo-Fi",
        "HipHop" to "🎤 Hip-Hop",
        "Jazz" to "🎷 Jazz",
        "Acoustic" to "🌿 Acústico"
    )

    fun executeSearch() {
        if (searchQuery.isBlank()) return
        focusManager.clearFocus()
        scope.launch {
            isLoading = true
            trackList = searchService.searchOrExtract(searchQuery, selectedSource)
            isLoading = false
        }
    }

    // Load initial trending tracks
    LaunchedEffect(selectedGenre, selectedSource) {
        if (searchQuery.isBlank()) {
            isLoading = true
            trackList = searchService.getTrending(selectedGenre, selectedSource)
            isLoading = false
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Compact Header Row with Integrated Quality Pill (Saves ~50dp vertical space)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF38BDF8)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Explorar y Descargar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Top TikTok, Pop y MP3 HD",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Compact Glassmorphic Quality Toggle Pill
                val is320 = downloadQuality == "320"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF13182E))
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                if (is320) listOf(Color(0xFF38BDF8).copy(alpha = 0.8f), Color(0xFF8B5CF6).copy(alpha = 0.8f))
                                else listOf(Color(0xFFF59E0B).copy(alpha = 0.8f), Color(0xFFE11D48).copy(alpha = 0.8f))
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            AuraHaptic.tick(view)
                            val next = if (is320) "160" else "320"
                            downloadQuality = next
                            appPrefs.edit().putString("download_quality", next).apply()
                            val msg = if (next == "320") "💎 Calidad: 320 kbps (Hi-Fi)" else "⚡ Calidad: 160 kbps (Rápido)"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (is320) "💎 320k Hi-Fi" else "⚡ 160k Rápido",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (is320) Color(0xFF38BDF8) else Color(0xFFF59E0B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Omnibar Global Input (Aesthetic Neon Glow)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF13182E))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.5f),
                                Color(0xFF38BDF8).copy(alpha = 0.4f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Buscar canción, artista o pegar enlace...",
                            color = Color(0xFF94A3B8).copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        IconButton(onClick = {
                            AuraHaptic.click(view)
                            executeSearch()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    AuraHaptic.click(view)
                                    searchQuery = ""
                                    scope.launch {
                                        isLoading = true
                                        trackList = searchService.getTrending(selectedGenre, selectedSource)
                                        isLoading = false
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpiar",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                // Quick Paste Button from Clipboard
                                IconButton(onClick = {
                                    AuraHaptic.click(view)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!clip.isNullOrBlank()) {
                                        searchQuery = clip.trim()
                                        executeSearch()
                                    } else {
                                        Toast.makeText(context, "Portapapeles vacío", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Pegar enlace",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        AuraHaptic.click(view)
                        executeSearch()
                    })
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Category & Genre Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { (key, label) ->
                    val isSelected = selectedGenre == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(Color(0xFF13182E), Color(0xFF0F172A))
                                    )
                                }
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                AuraHaptic.tick(view)
                                selectedGenre = key
                                if (searchQuery.isNotBlank()) {
                                    searchQuery = ""
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Compact Results & Batch Download Action Bar (Replaces bulky 80dp box)
            if (!isLoading && trackList.isNotEmpty()) {
                val downloadableTracks = remember(trackList) { trackList.filter { it.isDownloadable } }
                val downloadableCount = downloadableTracks.size

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "Resultados" else "Tendencias ($selectedGenre)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${trackList.size} canciones",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        if (!isDownloadingBatch) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                        )
                                    )
                                    .clickable {
                                        AuraHaptic.click(view)
                                        if (downloadableCount > 0) {
                                            showBatchConfirmDialog = true
                                        } else {
                                            Toast.makeText(context, "No hay canciones descargables en esta lista", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Descargar Todo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF38BDF8)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$batchCompleted/$batchTotal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }

                    // Live progress bar when batch download is active
                    if (isDownloadingBatch) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val progress = if (batchTotal > 0) batchCompleted.toFloat() / batchTotal.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Descargando: ${batchCurrentTitle.ifBlank { "audio..." }}",
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Empty / Initial state counter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "Resultados unificados" else "Tendencias ($selectedGenre)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${trackList.size} canciones",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showBatchConfirmDialog) {
                val downloadableTracks = trackList.filter { it.isDownloadable }
                AlertDialog(
                    onDismissRequest = { showBatchConfirmDialog = false },
                    title = {
                        Text(
                            text = "Descargar Álbum o Lista",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = "Se descargarán ${downloadableTracks.size} canciones completas con carátula oficial y metadatos en segundo plano directamente a tu música local.\n\n¿Deseas continuar?",
                            color = Color(0xFFCBD5E1),
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showBatchConfirmDialog = false
                                isDownloadingBatch = true
                                batchTotal = downloadableTracks.size
                                batchCompleted = 0
                                batchCurrentTitle = ""
                                Toast.makeText(context, "Iniciando descarga por lotes (${downloadableTracks.size} canciones)...", Toast.LENGTH_SHORT).show()

                                scope.launch {
                                    downloadEngine.downloadBatch(
                                        tracks = downloadableTracks,
                                        onProgress = { completed, total, currentTitle ->
                                            batchCompleted = completed
                                            batchTotal = total
                                            batchCurrentTitle = currentTitle
                                        },
                                        onAllCompleted = {
                                            isDownloadingBatch = false
                                            Toast.makeText(context, "✓ ¡Descarga completa! $batchTotal canciones guardadas en tu biblioteca.", Toast.LENGTH_LONG).show()
                                            onDownloadComplete()
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CF6),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Descargar (${downloadableTracks.size})")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showBatchConfirmDialog = false }
                        ) {
                            Text("Cancelar", color = Color(0xFF94A3B8))
                        }
                    },
                    containerColor = Color(0xFF1E1B4B),
                    shape = RoundedCornerShape(18.dp)
                )
            }

            // Results List / Loading State
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Buscando en catálogo global...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (trackList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No se encontraron canciones.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 130.dp, top = 4.dp)
                ) {
                    items(trackList, key = { it.id }, contentType = { "online_track" }) { track ->
                        val status = downloadStates[track.id] ?: DownloadStatus.Idle
                        val isPlayingThis = isPlaying && !currentPlayingTitle.isNullOrBlank() &&
                                (currentPlayingTitle.equals(track.title, ignoreCase = true) ||
                                 currentPlayingTitle.contains(track.title, ignoreCase = true) ||
                                 track.title.contains(currentPlayingTitle, ignoreCase = true))

                        OnlineTrackCard(
                            track = track,
                            status = status,
                            isPlayingThis = isPlayingThis,
                            onPreview = {
                                onPreviewTrack(track)
                            },
                            onDownload = {
                                if (!track.isDownloadable) {
                                    Toast.makeText(context, "⚠️ Esta pista es una muestra de 30s. Filtra por Jamendo o YouTube para canciones completas.", Toast.LENGTH_LONG).show()
                                } else {
                                    trackForDownloadChoice = track
                                }
                            },
                            onAddToQueue = {
                                onAddToQueue?.invoke(track) ?: run {
                                    Toast.makeText(context, "Añadida: ${track.title}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal de Descarga Dual: Música (M4A/MP3) o Video (MP4 HD)
    if (trackForDownloadChoice != null) {
        val targetTrack = trackForDownloadChoice!!
        AlertDialog(
            onDismissRequest = { trackForDownloadChoice = null },
            title = {
                Text(
                    text = "📥 Opciones de Descarga",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = targetTrack.title,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "¿Cómo deseas guardar este contenido en tu dispositivo?",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    // Opción 1: Solo Música
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131A2A))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable {
                                val tr = targetTrack
                                trackForDownloadChoice = null
                                Toast.makeText(context, "Iniciando descarga de audio: ${tr.title}", Toast.LENGTH_SHORT).show()
                                scope.launch {
                                    try {
                                        val validUrl = searchService.resolveValidAudioUrl(tr)
                                        val readyTrack = if (validUrl.isNotBlank() && validUrl != tr.audioUrl) {
                                            tr.copy(audioUrl = validUrl)
                                        } else {
                                            tr
                                        }
                                        downloadEngine.downloadTrack(
                                            track = readyTrack,
                                            onComplete = {
                                                Toast.makeText(context, "✓ Música guardada en tu biblioteca: ${tr.title}", Toast.LENGTH_LONG).show()
                                                onDownloadComplete()
                                            },
                                            onError = { errorMsg ->
                                                Toast.makeText(context, "Error al descargar: $errorMsg", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.localizedMessage ?: "No se pudo descargar"}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "🎵 Solo Música (HQ Audio)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Guarda en Music/DaVEPlayer para escuchar", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }

                    // Opción 2: Video Completo MP4
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF131A2A))
                            .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable {
                                val tr = targetTrack
                                trackForDownloadChoice = null
                                Toast.makeText(context, "Iniciando descarga de video MP4: ${tr.title}", Toast.LENGTH_SHORT).show()
                                scope.launch {
                                    try {
                                        downloadEngine.downloadVideo(
                                            track = tr,
                                            onComplete = {
                                                Toast.makeText(context, "🎬 Video MP4 guardado en tus Videos: ${tr.title}", Toast.LENGTH_LONG).show()
                                                onDownloadComplete()
                                            },
                                            onError = { errorMsg ->
                                                Toast.makeText(context, "Error al descargar video: $errorMsg", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.localizedMessage ?: "No se pudo descargar el video"}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "🎬 Video Completo (MP4 HD)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "Guarda en Movies/DaVEPlayer para ver offline", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { trackForDownloadChoice = null }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF101626),
            tonalElevation = 8.dp
        )
    }
}

@Composable
fun AnimatedEqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF38BDF8)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(bar1)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(bar3)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(bar2)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
    }
}

@Composable
fun OnlineTrackCard(
    track: OnlineTrack,
    status: DownloadStatus,
    isPlayingThis: Boolean,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    val sourceBadgeColor = when (track.source) {
        "YouTube" -> Color(0xFFEF4444)
        "Jamendo" -> Color(0xFF8B5CF6)
        "Deezer" -> Color(0xFF3B82F6)
        "Archive" -> Color(0xFFF59E0B)
        "TikTok" -> Color(0xFFEC4899)
        else -> Color(0xFF10B981)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isPlayingThis) Color(0xFF1E1B4B).copy(alpha = 0.7f)
                else Color(0xFF0F172A).copy(alpha = 0.55f)
            )
            .border(
                width = if (isPlayingThis) 1.5.dp else 1.dp,
                brush = if (isPlayingThis) {
                    Brush.horizontalGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))
                    )
                } else {
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))
                    )
                },
                shape = RoundedCornerShape(14.dp)
            )
            .clickable {
                AuraHaptic.click(view)
                onPreview()
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Cover Art with Playing Overlay / Animated Bars
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.25f),
                            Color(0xFF38BDF8).copy(alpha = 0.2f)
                        )
                    )
                )
                .border(
                    1.dp,
                    if (isPlayingThis) Color(0xFF38BDF8).copy(alpha = 0.6f)
                    else Color(0xFF8B5CF6).copy(alpha = 0.25f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF8B5CF6).copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            if (track.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(track.coverUrl)
                        .size(180, 180)
                        .crossfade(true)
                        .build(),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // If playing: Translucent dark scrim + Animated Equalizer Bars
            if (isPlayingThis) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedEqualizerBars(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Info: Title, Artist, Badges
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlayingThis) Color(0xFF38BDF8) else Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${track.artist} • ${track.durationFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Source Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(sourceBadgeColor.copy(alpha = 0.18f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = track.source,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = sourceBadgeColor
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Bitrate / Quality Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = track.format,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Right Actions: Sleek Download Action + More Options Menu
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (status) {
                is DownloadStatus.Idle -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (track.isDownloadable) Color(0xFF1E293B).copy(alpha = 0.9f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                            .border(
                                1.dp,
                                if (track.isDownloadable) Color(0xFF38BDF8).copy(alpha = 0.35f)
                                else Color.White.copy(alpha = 0.08f),
                                CircleShape
                            )
                            .clickable {
                                AuraHaptic.click(view)
                                onDownload()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (track.isDownloadable) Icons.Default.CloudDownload else Icons.Default.Info,
                            contentDescription = if (track.isDownloadable) "Descargar Canción Completa" else "Muestra 30s",
                            tint = if (track.isDownloadable) Color(0xFF38BDF8) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                is DownloadStatus.Downloading -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { status.progress / 100f },
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "${status.progress}%",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
                is DownloadStatus.Tagging -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFEC4899),
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                is DownloadStatus.Completed -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFF10B981), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Descargado",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                is DownloadStatus.Error -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.error, CircleShape)
                            .clickable {
                                AuraHaptic.click(view)
                                onDownload()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Reintentar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // More Options Menu (⋮)
            Box {
                IconButton(
                    onClick = {
                        AuraHaptic.tick(view)
                        showMenu = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Más opciones",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF1E1B4B))
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PlaylistAdd,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Añadir a la cola", color = Color.White, fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            showMenu = false
                            AuraHaptic.click(view)
                            onAddToQueue()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copiar título", color = Color.White, fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            showMenu = false
                            AuraHaptic.tick(view)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("song_title", "${track.title} - ${track.artist}")
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "Copiado: ${track.title}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
