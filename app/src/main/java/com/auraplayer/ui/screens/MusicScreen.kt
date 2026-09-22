package com.auraplayer.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auraplayer.data.model.MediaModel
import com.auraplayer.data.repository.FavoritesManager
import com.auraplayer.data.repository.Playlist
import com.auraplayer.data.repository.PlaylistManager
import com.auraplayer.ui.components.TagEditorDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    songs: List<MediaModel>,
    isLoading: Boolean,
    currentMedia: MediaModel?,
    favoritesManager: FavoritesManager,
    playlistManager: PlaylistManager,
    onSongClick: (MediaModel) -> Unit,
    onPlayNext: (MediaModel) -> Unit,
    onAddToQueue: (MediaModel) -> Unit,
    onSaveTags: (song: MediaModel, title: String, artist: String, album: String) -> Unit,
    onDeleteSong: (MediaModel) -> Unit,
    onFetchCover: (MediaModel) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onCheckUpdates: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Canciones", "Playlists 📂", "Favoritos ❤️", "Carpetas", "Artistas")

    // Filter & Sort States
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: Todas, 1: HD/Lossless, 2: Favoritas, 3: Cortas, 4: Largas
    var selectedSortMode by remember { mutableIntStateOf(0) } // 0: Título, 1: Artista, 2: Duración, 3: Más Escuchadas
    var showSortMenu by remember { mutableStateOf(false) }

    var selectedSongForMenu by remember { mutableStateOf<MediaModel?>(null) }
    var songToDelete by remember { mutableStateOf<MediaModel?>(null) }
    var songForDetails by remember { mutableStateOf<MediaModel?>(null) }
    var selectedSongForTagEdit by remember { mutableStateOf<MediaModel?>(null) }
    var songToAddToPlaylist by remember { mutableStateOf<MediaModel?>(null) }

    // Custom Playlist view / create state
    var selectedPlaylistForView by remember { mutableStateOf<Playlist?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var playlistSubCategory by remember { mutableIntStateOf(0) } // 0: Mis Playlists, 1: Top Escuchadas, 2: Historial

    // BackHandler to handle playlist view, search query, or sub-tab navigation
    BackHandler(enabled = selectedPlaylistForView != null || searchQuery.isNotBlank() || selectedTab != 0 || selectedFilterIndex != 0) {
        if (selectedPlaylistForView != null) {
            selectedPlaylistForView = null
        } else if (searchQuery.isNotBlank()) {
            searchQuery = ""
        } else if (selectedFilterIndex != 0) {
            selectedFilterIndex = 0
        } else if (selectedTab != 0) {
            selectedTab = 0
        }
    }

    // Process Filter and Sort
    val filteredSongs = remember(songs, searchQuery, selectedFilterIndex, selectedSortMode, favoritesManager) {
        var list = if (searchQuery.isBlank()) songs
        else songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true)
        }

        // Apply quick filter
        list = when (selectedFilterIndex) {
            1 -> playlistManager.getMostPlayedSongs(list)
            2 -> list.filter {
                val ext = File(it.path).extension.lowercase()
                ext in listOf("flac", "wav", "m4a", "alac", "dsf", "dff")
            }
            3 -> {
                val favs = favoritesManager.getFavoriteIds()
                list.filter { favs.contains(it.id) }
            }
            4 -> list.filter { it.duration in 1..150000L } // < 2.5 min
            5 -> list.filter { it.duration >= 240000L } // > 4 min
            else -> list
        }

        // Apply sort
        when (selectedSortMode) {
            1 -> list.sortedBy { it.artist.lowercase() }
            2 -> list.sortedByDescending { it.duration }
            3 -> list.sortedByDescending { playlistManager.getPlayCount(it.id) }
            else -> list.sortedBy { it.title.lowercase() }
        }
    }

    val favoriteSongs = remember(songs, favoritesManager) {
        val favIds = favoritesManager.getFavoriteIds()
        songs.filter { favIds.contains(it.id) }
    }

    val totalDurationFormatted = remember(songs) {
        val totalMs = songs.sumOf { it.duration }
        val hours = totalMs / (1000 * 60 * 60)
        val minutes = (totalMs / (1000 * 60)) % 60
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Aesthetic Search & Quick Actions Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Neon Glow Search Input Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF13182E))
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.45f),
                                Color(0xFF38BDF8).copy(alpha = 0.35f)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Buscar canción, artista...",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8).copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sort Menu Button (Aesthetic Neon Squircle)
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF13182E))
                        .border(
                            1.dp,
                            Color(0xFFA855F7).copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { showSortMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Ordenar",
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(Color(0xFF0F172A))
                ) {
                    DropdownMenuItem(text = { Text("🔤 Título (A-Z)", color = Color.White) }, onClick = { selectedSortMode = 0; showSortMenu = false })
                    DropdownMenuItem(text = { Text("👤 Artista", color = Color.White) }, onClick = { selectedSortMode = 1; showSortMenu = false })
                    DropdownMenuItem(text = { Text("⏱️ Mayor Duración", color = Color.White) }, onClick = { selectedSortMode = 2; showSortMenu = false })
                    DropdownMenuItem(text = { Text("🔥 Más Reproducidas", color = Color.White) }, onClick = { selectedSortMode = 3; showSortMenu = false })
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sleep Timer Button (Aesthetic Neon Squircle)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF13182E))
                    .border(
                        1.dp,
                        Color(0xFF38BDF8).copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onOpenSleepTimer() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Temporizador",
                    tint = Color(0xFF38BDF8),
            }

            Spacer(modifier = Modifier.width(8.dp))

            // In-App Updates Button (Aesthetic Neon Squircle)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF13182E))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.5f),
                                Color(0xFFEC4899).copy(alpha = 0.5f)
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onCheckUpdates() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Buscar Actualizaciones",
                    tint = Color(0xFFEC4899),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Tab Row with Aesthetic Neon Accent
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        selectedPlaylistForView = null
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        // Aesthetic Quick Filter Chips (When in Canciones tab)
        if (selectedTab == 0) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("Todas", "🔥 Más Escuchadas", "💎 Lossless", "❤️ Favoritas", "⚡ Cortas", "☕ Largas")
                itemsIndexed(filters) { index, label ->
                    val isSelected = selectedFilterIndex == index
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
                            .clickable { selectedFilterIndex = index }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            when (selectedTab) {
                0 -> { // Canciones
                    if (filteredSongs.isEmpty()) {
                        EmptyListMessage(if (searchQuery.isEmpty()) "No se encontraron canciones en el dispositivo." else "Sin resultados")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                // Stats Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${filteredSongs.size} canciones • $totalDurationFormatted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "100% Offline • 0 Ads",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            items(filteredSongs, key = { it.id }) { song ->
                                val isSelected = currentMedia?.id == song.id
                                SongListItem(
                                    song = song,
                                    isSelected = isSelected,
                                    isFavorite = favoritesManager.isFavorite(song.id),
                                    onClick = { onSongClick(song) },
                                    onLongClick = { selectedSongForMenu = song },
                                    onOptionsClick = { selectedSongForMenu = song }
                                )
                            }
                        }
                    }
                }
                1 -> { // Playlists 📂 (Smart Playlists & Custom)
                    if (selectedPlaylistForView != null) {
                        // Inside a specific playlist
                        val pl = selectedPlaylistForView!!
                        val plSongs = remember(pl, songs) {
                            songs.filter { pl.songIds.contains(it.id) }
                        }

                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { selectedPlaylistForView = null }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = pl.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(text = "${plSongs.size} canciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (plSongs.isNotEmpty()) {
                                    Button(
                                        onClick = { onSongClick(plSongs.first()) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Reproducir", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (plSongs.isEmpty()) {
                                EmptyListMessage("Esta playlist está vacía.\nAñade canciones desde el menú de 3 puntos de cualquier pista.")
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(plSongs, key = { it.id }) { song ->
                                        SongListItem(
                                            song = song,
                                            isSelected = currentMedia?.id == song.id,
                                            isFavorite = favoritesManager.isFavorite(song.id),
                                            onClick = { onSongClick(song) },
                                            onLongClick = { selectedSongForMenu = song },
                                            onOptionsClick = { selectedSongForMenu = song }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Playlists Hub
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            // Sub-chips (Mis Playlists, Más Escuchadas, Historial)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = playlistSubCategory == 0,
                                    onClick = { playlistSubCategory = 0 },
                                    label = { Text("Mis Playlists") },
                                    leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
                                )
                                FilterChip(
                                    selected = playlistSubCategory == 1,
                                    onClick = { playlistSubCategory = 1 },
                                    label = { Text("🔥 Top") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
                                )
                                FilterChip(
                                    selected = playlistSubCategory == 2,
                                    onClick = { playlistSubCategory = 2 },
                                    label = { Text("🕒 Historial") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            when (playlistSubCategory) {
                                0 -> {
                                    // Custom user playlists
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Colecciones Personalizadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Button(
                                            onClick = { showCreatePlaylistDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Nueva", fontSize = 13.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val playlists = playlistManager.getPlaylists()
                                    if (playlists.isEmpty()) {
                                        EmptyListMessage("No has creado playlists aún.\nToca '+ Nueva' para organizar tu música.")
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            items(playlists, key = { it.id }) { pl ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp)
                                                        .clickable { selectedPlaylistForView = pl },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(46.dp)
                                                                    .clip(RoundedCornerShape(12.dp))
                                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                            }
                                                            Spacer(modifier = Modifier.width(14.dp))
                                                            Column {
                                                                Text(text = pl.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                                                Text(text = "${pl.songIds.size} canciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        }

                                                        IconButton(onClick = { playlistManager.deletePlaylist(pl.id); selectedPlaylistForView = null }) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    // Top Played (AIMP / Pulsar Smart Playlist)
                                    val topSongs = remember(songs, playlistManager) {
                                        songs.filter { playlistManager.getPlayCount(it.id) > 0 }
                                            .sortedByDescending { playlistManager.getPlayCount(it.id) }
                                    }

                                    if (topSongs.isEmpty()) {
                                        EmptyListMessage("Escucha tus canciones para que aparezcan aquí ordenadas por las más reproducidas.")
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            items(topSongs, key = { it.id }) { song ->
                                                val count = playlistManager.getPlayCount(song.id)
                                                SongListItem(
                                                    song = song,
                                                    extraBadge = "🔥 $count plays",
                                                    isSelected = currentMedia?.id == song.id,
                                                    isFavorite = favoritesManager.isFavorite(song.id),
                                                    onClick = { onSongClick(song) },
                                                    onLongClick = { selectedSongForMenu = song },
                                                    onOptionsClick = { selectedSongForMenu = song }
                                                )
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    // Recently Played History
                                    val recentIds = playlistManager.getRecentlyPlayedIds()
                                    val recentSongs = remember(recentIds, songs) {
                                        recentIds.mapNotNull { id -> songs.find { it.id == id } }
                                    }

                                    if (recentSongs.isEmpty()) {
                                        EmptyListMessage("Tu historial de reproducción está vacío.")
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            items(recentSongs, key = { it.id }) { song ->
                                                SongListItem(
                                                    song = song,
                                                    extraBadge = "🕒 Reciente",
                                                    isSelected = currentMedia?.id == song.id,
                                                    isFavorite = favoritesManager.isFavorite(song.id),
                                                    onClick = { onSongClick(song) },
                                                    onLongClick = { selectedSongForMenu = song },
                                                    onOptionsClick = { selectedSongForMenu = song }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // Favoritos
                    if (favoriteSongs.isEmpty()) {
                        EmptyListMessage("Aún no tienes canciones favoritas.\nToca el corazón o mantén presionada cualquier canción.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(favoriteSongs, key = { it.id }) { song ->
                                SongListItem(
                                    song = song,
                                    isSelected = currentMedia?.id == song.id,
                                    isFavorite = true,
                                    onClick = { onSongClick(song) },
                                    onLongClick = { selectedSongForMenu = song },
                                    onOptionsClick = { selectedSongForMenu = song }
                                )
                            }
                        }
                    }
                }
                3 -> { // Carpetas
                    val folderGroups = remember(songs) {
                        songs.groupBy { it.folderName.ifEmpty { "Música" } }
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(folderGroups.keys.toList()) { folderName ->
                            val folderSongs = folderGroups[folderName] ?: emptyList()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { folderSongs.firstOrNull()?.let { onSongClick(it) } }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = folderName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${folderSongs.size} canciones",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                4 -> { // Artistas
                    val artistGroups = remember(songs) {
                        songs.groupBy { it.artist }
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(artistGroups.keys.toList()) { artistName ->
                            val artistSongs = artistGroups[artistName] ?: emptyList()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { artistSongs.firstOrNull()?.let { onSongClick(it) } }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = artistName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${artistSongs.size} canciones",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Nueva Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Nombre de la lista") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newPlaylistName.trim()
                        if (clean.isNotEmpty()) {
                            playlistManager.createPlaylist(clean)
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Crear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = Color(0xFF101422),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Add to Playlist Selection Dialog
    if (songToAddToPlaylist != null) {
        val song = songToAddToPlaylist!!
        val playlists = playlistManager.getPlaylists()
        AlertDialog(
            onDismissRequest = { songToAddToPlaylist = null },
            title = { Text("Añadir a Playlist", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (playlists.isEmpty()) {
                        Text("No tienes playlists aún. Crea una primero.")
                    } else {
                        playlists.forEach { pl ->
                            TextButton(
                                onClick = {
                                    val added = playlistManager.addSongToPlaylist(pl.id, song.id)
                                    if (added) {
                                        Toast.makeText(context, "Añadida a '${pl.name}'", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Ya estaba en '${pl.name}'", Toast.LENGTH_SHORT).show()
                                    }
                                    songToAddToPlaylist = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "📂 ${pl.name} (${pl.songIds.size} pistas)", color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    songToAddToPlaylist = null
                    showCreatePlaylistDialog = true
                }) {
                    Text("+ Crear Nueva", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToAddToPlaylist = null }) {
                    Text("Cerrar")
                }
            },
            containerColor = Color(0xFF101422),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ID3 Tag Editor Dialog (Pulsar / Musicolet Style)
    if (selectedSongForTagEdit != null) {
        TagEditorDialog(
            song = selectedSongForTagEdit!!,
            onDismiss = { selectedSongForTagEdit = null },
            onSave = { newTitle, newArtist, newAlbum ->
                onSaveTags(selectedSongForTagEdit!!, newTitle, newArtist, newAlbum)
                selectedSongForTagEdit = null
            }
        )
    }

    // Song Options BottomSheet Modal (Musicolet Style Queuing & Tags)
    if (selectedSongForMenu != null) {
        val song = selectedSongForMenu!!
        val isFav = favoritesManager.isFavorite(song.id)

        ModalBottomSheet(
            onDismissRequest = { selectedSongForMenu = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color(0xFF101422)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Song Header Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.artworkUri != null) {
                            AsyncImage(
                                model = song.artworkUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${song.artist} • ${song.formattedDuration}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Musicolet Queue Options
                MenuOptionItem(
                    icon = Icons.Default.SkipNext,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = "Reproducir Siguiente (Play Next)",
                    onClick = {
                        selectedSongForMenu = null
                        onPlayNext(song)
                    }
                )

                MenuOptionItem(
                    icon = Icons.Default.PlaylistAdd,
                    iconColor = Color(0xFF06B6D4),
                    title = "Añadir a la Cola (Queue)",
                    onClick = {
                        selectedSongForMenu = null
                        onAddToQueue(song)
                    }
                )

                MenuOptionItem(
                    icon = Icons.Default.QueueMusic,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    title = "Añadir a una Playlist...",
                    onClick = {
                        val target = song
                        selectedSongForMenu = null
                        songToAddToPlaylist = target
                    }
                )

                MenuOptionItem(
                    icon = Icons.Default.Edit,
                    iconColor = Color(0xFFF59E0B),
                    title = "Editar Etiquetas (ID3 Tag Editor)",
                    onClick = {
                        val target = song
                        selectedSongForMenu = null
                        selectedSongForTagEdit = target
                    }
                )

                MenuOptionItem(
                    icon = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    iconColor = if (isFav) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurfaceVariant,
                    title = if (isFav) "Quitar de Favoritos" else "Añadir a Favoritos",
                    onClick = {
                        favoritesManager.toggleFavorite(song.id)
                        selectedSongForMenu = null
                    }
                )

                MenuOptionItem(
                    icon = Icons.Default.Share,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = "Compartir Canción",
                    onClick = {
                        selectedSongForMenu = null
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, song.uri)
                            putExtra(Intent.EXTRA_TEXT, "Escuchando '${song.title}' de ${song.artist} en Aura Player")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir audio"))
                    }
                )

                MenuOptionItem(
                    icon = Icons.Default.Image,
                    iconColor = Color(0xFF06B6D4),
                    title = "Descargar / Actualizar Portada Oficial",
                    onClick = {
                        selectedSongForMenu = null
                        onFetchCover(song)
                    }
                )

                MenuOptionItem(
                    icon = Icons.Default.Info,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    title = "Detalles del Archivo",
                    onClick = {
                        selectedSongForMenu = null
                        songForDetails = song
                    }
                )

                MenuOptionItem(
                    icon = Icons.Default.NotificationsActive,
                    iconColor = Color(0xFFF59E0B),
                    title = "Establecer como Tono de Llamada",
                    onClick = {
                        selectedSongForMenu = null
                        setAsRingtone(context, song)
                    }
                )

                MenuOptionItem(
                    icon = Icons.Default.Delete,
                    iconColor = MaterialTheme.colorScheme.error,
                    title = "Eliminar del Teléfono",
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        selectedSongForMenu = null
                        songToDelete = song
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (songToDelete != null) {
        val target = songToDelete!!
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = {
                Text(
                    text = "¿Eliminar canción?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Se eliminará permanentemente '${target.title}' del almacenamiento de tu teléfono.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = target
                        songToDelete = null
                        onDeleteSong(s)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Cancelar")
                }
            },
            containerColor = Color(0xFF101422),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // File Details Dialog
    if (songForDetails != null) {
        val details = songForDetails!!
        val file = File(details.path)
        val sizeMb = if (details.size > 0) String.format("%.2f MB", details.size / (1024.0 * 1024.0)) else "Desconocido"

        AlertDialog(
            onDismissRequest = { songForDetails = null },
            title = {
                Text(
                    text = "Detalles de la Pista",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Título", details.title)
                    DetailRow("Artista", details.artist)
                    DetailRow("Álbum", details.album)
                    DetailRow("Duración", details.formattedDuration)
                    DetailRow("Tamaño", sizeMb)
                    DetailRow("Formato", file.extension.uppercase().ifEmpty { "AUDIO" })
                    DetailRow("Ruta", details.path)
                }
            },
            confirmButton = {
                Button(onClick = { songForDetails = null }) {
                    Text("Cerrar")
                }
            },
            containerColor = Color(0xFF101422),
            shape = RoundedCornerShape(20.dp)
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
private fun MenuOptionItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun LiveEqualizerIndicator(
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF38BDF8)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(310, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(390, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(h1, h2, h3, h4).forEach { heightFraction ->
            val finalHeight = (heightFraction * 14).coerceAtLeast(3f)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(finalHeight.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: MediaModel,
    isSelected: Boolean,
    isFavorite: Boolean,
    extraBadge: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    val ext = remember(song.path) {
        File(song.path).extension.uppercase().ifEmpty { "AUDIO" }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.35f),
                                Color(0xFF38BDF8).copy(alpha = 0.35f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF13182E),
                                Color(0xFF0F172A)
                            )
                        )
                    }
                )
                .border(
                    1.dp,
                    if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF8B5CF6).copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            if (song.artworkUri != null) {
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Live Equalizer Overlay on top of the active song's album art
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    LiveEqualizerIndicator(barColor = Color(0xFF38BDF8))
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    LiveEqualizerIndicator(barColor = Color(0xFF38BDF8))
                }
                if (isFavorite) {
                    val heartScale by animateFloatAsState(
                        targetValue = if (isFavorite) 1.2f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "heartScale"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorito",
                        tint = Color(0xFFEC4899),
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer(scaleX = heartScale, scaleY = heartScale)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${song.artist} • ${song.formattedDuration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Small dynamic format badge chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (ext == "FLAC" || ext == "WAV") Color(0xFF06B6D4).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = ext,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ext == "FLAC" || ext == "WAV") Color(0xFF06B6D4) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (extraBadge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = extraBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        IconButton(onClick = onOptionsClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opciones",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyListMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

private fun setAsRingtone(context: android.content.Context, song: MediaModel) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.System.canWrite(context)) {
                val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = android.net.Uri.parse("package:" + context.packageName)
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                android.widget.Toast.makeText(context, "Concede permiso para modificar tono de llamada", android.widget.Toast.LENGTH_LONG).show()
                return
            }
        }
        android.media.RingtoneManager.setActualDefaultRingtoneUri(
            context,
            android.media.RingtoneManager.TYPE_RINGTONE,
            song.uri
        )
        android.widget.Toast.makeText(context, "¡'${song.title}' es ahora tu tono de llamada!", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "No se pudo establecer como tono: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}
