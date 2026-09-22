package com.auraplayer.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

            // Header Title (Protected with notch & status bar padding)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
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
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Explorar y Descargar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Top TikTok y música completa en MP3 HD",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Omnibar Global Input (Aesthetic Neon Glow)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
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
                            "Buscar canción, artista o pegar enlace (TikTok/Web)...",
                            color = Color(0xFF94A3B8).copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        IconButton(onClick = { executeSearch() }) {
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
                    keyboardActions = KeyboardActions(onSearch = { executeSearch() })
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category & Genre Filter Chips
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
                                if (isSelected) Color.Transparent else Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedGenre = key
                                if (searchQuery.isNotBlank()) {
                                    searchQuery = ""
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
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

            Spacer(modifier = Modifier.height(10.dp))

            // Batch Download Card & Results Header
            if (!isLoading && trackList.isNotEmpty()) {
                val downloadableTracks = remember(trackList) { trackList.filter { it.isDownloadable } }
                val downloadableCount = downloadableTracks.size

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF1E1B4B).copy(alpha = 0.85f),
                                    Color(0xFF0F172A).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF8B5CF6).copy(alpha = 0.6f),
                                    Color(0xFF38BDF8).copy(alpha = 0.4f)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Resultados / Álbum" else "Tendencias ($selectedSource)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${trackList.size} canciones ($downloadableCount descargables en MP3 HD)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            if (!isDownloadingBatch) {
                                Button(
                                    onClick = {
                                        if (downloadableCount > 0) {
                                            showBatchConfirmDialog = true
                                        } else {
                                            Toast.makeText(context, "No hay canciones descargables en esta lista", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF8B5CF6),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Descargar Todo",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
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
                            Spacer(modifier = Modifier.height(8.dp))
                            val progress = if (batchTotal > 0) batchCompleted.toFloat() / batchTotal.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF38BDF8),
                                trackColor = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Descargando: ${batchCurrentTitle.ifBlank { "audio..." }}",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // Results Counter & Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "Resultados unificados" else "Tendencias ($selectedSource)",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp, top = 4.dp)
                ) {
                    items(trackList, key = { it.id }, contentType = { "online_track" }) { track ->
                        val status = downloadStates[track.id] ?: DownloadStatus.Idle

                        OnlineTrackCard(
                            track = track,
                            status = status,
                            onPreview = {
                                Toast.makeText(context, "Reproduciendo: ${track.title}", Toast.LENGTH_SHORT).show()
                                onPreviewTrack(track)
                            },
                            onDownload = {
                                if (!track.isDownloadable) {
                                    Toast.makeText(context, "⚠️ Esta pista es una muestra de 30s. Filtra por Jamendo o YouTube para canciones completas.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Iniciando descarga: ${track.title}", Toast.LENGTH_SHORT).show()
                                    scope.launch {
                                        try {
                                            val validUrl = searchService.resolveValidAudioUrl(track)
                                            val readyTrack = if (validUrl.isNotBlank() && validUrl != track.audioUrl) {
                                                track.copy(audioUrl = validUrl)
                                            } else {
                                                track
                                            }
                                            downloadEngine.downloadTrack(
                                                track = readyTrack,
                                                onComplete = {
                                                    Toast.makeText(context, "✓ Canción completa guardada en tu biblioteca: ${track.title}", Toast.LENGTH_LONG).show()
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
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineTrackCard(
    track: OnlineTrack,
    status: DownloadStatus,
    onPreview: () -> Unit,
    onDownload: () -> Unit
) {
    val context = LocalContext.current

    val sourceBadgeColor = when (track.source) {
        "YouTube" -> Color(0xFFEF4444)
        "Jamendo" -> Color(0xFF8B5CF6)
        "Deezer" -> Color(0xFF3B82F6)
        "Archive" -> Color(0xFFF59E0B)
        "TikTok" -> Color(0xFFEC4899)
        else -> Color(0xFF10B981)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Cover Art
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
                    Color(0xFF8B5CF6).copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF8B5CF6).copy(alpha = 0.7f),
                modifier = Modifier.size(26.dp)
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
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info: Title, Artist, Badges
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${track.artist} • ${track.durationFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Source Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(sourceBadgeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
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
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = track.format,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Play Stream / Preview Button
            IconButton(
                onClick = onPreview,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Escuchar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Download Button with Dynamic State
            when (status) {
                is DownloadStatus.Idle -> {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (track.isDownloadable) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                    ) {
                        Icon(
                            imageVector = if (track.isDownloadable) Icons.Default.CloudDownload else Icons.Default.Info,
                            contentDescription = if (track.isDownloadable) "Descargar Canción Completa" else "Muestra 30s",
                            tint = if (track.isDownloadable) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                is DownloadStatus.Downloading -> {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { status.progress / 100f },
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "${status.progress}%",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is DownloadStatus.Tagging -> {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Color(0xFFEC4899),
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
                is DownloadStatus.Completed -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Descargado",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                is DownloadStatus.Error -> {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Reintentar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
