package com.auraplayer.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auraplayer.data.model.MediaModel
import com.auraplayer.data.repository.CoverArtSearchService
import com.auraplayer.data.repository.SearchCoverResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun TagEditorDialog(
    song: MediaModel,
    onDismiss: () -> Unit,
    onSave: (newTitle: String, newArtist: String, newAlbum: String, newCoverUri: Uri?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var customCoverUri by remember { mutableStateOf<Uri?>(null) }
    var selectedWebCoverUrl by remember { mutableStateOf<String?>(null) }

    var isSearchingCover by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<SearchCoverResult>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customCoverUri = uri
            selectedWebCoverUrl = null
        }
    }

    fun searchOnlineCover() {
        val query = "${artist.ifBlank { song.artist }} ${title.ifBlank { song.title }}".trim()
        if (query.isBlank()) return

        isSearchingCover = true
        searchError = null
        searchResults = emptyList()

        scope.launch {
            val results = CoverArtSearchService.searchCover(query)
            isSearchingCover = false
            if (results.isEmpty()) {
                searchError = "No se encontraron carátulas en línea para esta canción."
            } else {
                searchResults = results
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101422),
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Editor de Etiquetas & Carátula",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Cover Artwork Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E2438))
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        val displayModel: Any? = selectedWebCoverUrl ?: customCoverUri ?: song.artworkUri
                        if (displayModel != null) {
                            AsyncImage(
                                model = displayModel,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Overlay badge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Cambiar",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedWebCoverUrl != null || customCoverUri != null) "✨ Carátula seleccionada" else "Carátula de la pista",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedWebCoverUrl != null || customCoverUri != null) Color(0xFF10B981) else Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row {
                            TextButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Galería 🖼️",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { searchOnlineCover() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Buscar HD", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Loading or Search results
                if (isSearchingCover) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF6366F1))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buscando portadas oficiales en HD...", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }

                AnimatedVisibility(
                    visible = searchResults.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(modifier = Modifier.padding(bottom = 10.dp)) {
                        Text(
                            text = "SELECCIONA UNA CARÁTULA HD (TOCA PARA APLICAR):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(searchResults) { result ->
                                val isSelected = selectedWebCoverUrl == result.hdCoverUrl
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF10B981) else Color(0xFF334155),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedWebCoverUrl = result.hdCoverUrl
                                            customCoverUri = null
                                            // Auto-fill tags if they are default or empty
                                            if (result.title.isNotBlank()) title = result.title
                                            if (result.artist.isNotBlank()) artist = result.artist
                                            if (result.album.isNotBlank()) album = result.album
                                        }
                                ) {
                                    AsyncImage(
                                        model = result.hdCoverUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF10B981).copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (searchError != null) {
                    Text(
                        text = searchError ?: "",
                        fontSize = 11.sp,
                        color = Color(0xFFF87171),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la canción") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Artist Input
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista / Intérprete") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Album Input
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Álbum") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim().ifEmpty { song.title }
                    val cleanArtist = artist.trim().ifEmpty { song.artist }
                    val cleanAlbum = album.trim().ifEmpty { song.album }

                    if (selectedWebCoverUrl != null) {
                        scope.launch {
                            val localUri = downloadCoverToLocal(context, selectedWebCoverUrl!!, song.id)
                            onSave(cleanTitle, cleanArtist, cleanAlbum, localUri ?: customCoverUri)
                            onDismiss()
                        }
                    } else {
                        onSave(cleanTitle, cleanArtist, cleanAlbum, customCoverUri)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar Cambios", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

private suspend fun downloadCoverToLocal(context: Context, imageUrl: String, songId: Long): Uri? = withContext(Dispatchers.IO) {
    try {
        val conn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 15000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }
        if (conn.responseCode == 200) {
            val dir = File(context.filesDir, "custom_covers")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "cover_${songId}_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { out ->
                conn.inputStream.use { inp -> inp.copyTo(out) }
            }
            return@withContext Uri.fromFile(file)
        }
    } catch (_: Exception) {}
    null
}
