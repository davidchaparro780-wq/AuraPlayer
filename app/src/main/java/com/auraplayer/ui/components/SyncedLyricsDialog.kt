package com.auraplayer.ui.components

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.scale
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import kotlinx.coroutines.launch
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.auraplayer.data.model.MediaModel
import com.auraplayer.data.repository.LrcLine
import com.auraplayer.data.repository.SyncedLyricsManager

/**
 * Pantalla / Diálogo de Letras Sincronizadas en Tiempo Real (Estilo Spotify & Apple Music Sing).
 * Ofrece auto-scroll fluido, resaltado dinámico verso a verso, Tap to Seek y creador de tarjetas para compartir.
 */
@Composable
fun SyncedLyricsDialog(
    song: MediaModel,
    currentPositionMs: Long,
    lyricsManager: SyncedLyricsManager,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var lines by remember { mutableStateOf<List<LrcLine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isManualSearching by remember { mutableStateOf(false) }
    var manualError by remember { mutableStateOf(false) }
    var manualArtist by remember { mutableStateOf(song.artist) }
    var manualTitle by remember { mutableStateOf(song.title) }

    LaunchedEffect(song.id) {
        isLoading = true
        val local = lyricsManager.getLocalSyncedLyrics(song.id)
        if (local != null && local.isNotEmpty()) {
            lines = local
            isLoading = false
        } else {
            val fetched = lyricsManager.fetchSyncedLyrics(song)
            lines = fetched ?: emptyList()
            isLoading = false
        }
    }

    // Calcular índice activo
    val activeIndex = remember(lines, currentPositionMs) {
        if (lines.isEmpty()) -1
        else lines.indexOfLast { it.timeMs <= currentPositionMs }
    }

    // Auto-scroll suave hacia el verso activo
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lines.size) {
            val target = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    fun runManualSearch() {
        if (isManualSearching) return
        isManualSearching = true
        manualError = false
        scope.launch {
            val result = lyricsManager.searchByText(manualArtist.trim(), manualTitle.trim())
            if (result != null && result.isNotEmpty()) {
                lines = result
            } else {
                manualError = true
            }
            isManualSearching = false
        }
    }

    fun retryAutoSearch() {
        if (isLoading) return
        manualError = false
        isLoading = true
        scope.launch {
            val fetched = lyricsManager.fetchSyncedLyrics(song)
            lines = fetched ?: emptyList()
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF070B16)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0D1527), Color(0xFF070B16), Color(0xFF04060C))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    // Cabecera superior
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.artist} • Letras Sincronizadas",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Botón de compartir frase activa
                        if (activeIndex >= 0 && activeIndex < lines.size) {
                            IconButton(
                                onClick = {
                                    val shareText = "🎵 \"${lines[activeIndex].text}\"\n— ${song.title} (${song.artist})\n✨ Escuchando en DaVE Player"
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartir Frase"))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartir verso",
                                    tint = Color(0xFF38BDF8)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contenido central: Lista de letras o indicador de carga
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoading -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.size(38.dp)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Buscando letras sincronizadas en LRCLIB...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            lines.isEmpty() -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scrollState)
                                        .padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatQuote,
                                        contentDescription = null,
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No encontramos la letra automáticamente",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Escribe el artista y la canción para buscarla manualmente.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))

                                    OutlinedTextField(
                                        value = manualArtist,
                                        onValueChange = { manualArtist = it },
                                        label = { Text("Artista") },
                                        singleLine = true,
                                        enabled = !isManualSearching,
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF131A2B),
                                            unfocusedContainerColor = Color(0xFF131A2B),
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = Color(0xFF334155),
                                            focusedLabelColor = Color(0xFF38BDF8),
                                            unfocusedLabelColor = Color(0xFF94A3B8),
                                            cursorColor = Color(0xFF38BDF8)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = manualTitle,
                                        onValueChange = { manualTitle = it },
                                        label = { Text("Canción") },
                                        singleLine = true,
                                        enabled = !isManualSearching,
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF131A2B),
                                            unfocusedContainerColor = Color(0xFF131A2B),
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = Color(0xFF334155),
                                            focusedLabelColor = Color(0xFF38BDF8),
                                            unfocusedLabelColor = Color(0xFF94A3B8),
                                            cursorColor = Color(0xFF38BDF8)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))

                                    if (isManualSearching) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Buscando letra...",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp
                                        )
                                    } else {
                                        Button(
                                            onClick = { runManualSearch() },
                                            enabled = manualArtist.isNotBlank() && manualTitle.isNotBlank(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF38BDF8),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Buscar letra",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }

                                    if (manualError) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Sin resultados para esta búsqueda",
                                            color = Color(0xFFF87171),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedButton(
                                            onClick = { retryAutoSearch() },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFF38BDF8)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Reintentar",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 120.dp),
                                    verticalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    itemsIndexed(lines) { index, line ->
                                        val isActive = (index == activeIndex)
                                        val textColor by animateColorAsState(
                                            targetValue = if (isActive) Color(0xFF38BDF8) else Color(0xFF64748B),
                                            animationSpec = tween(durationMillis = 250),
                                            label = "lyricsColor"
                                        )
                                        val lineScale by animateFloatAsState(
                                            targetValue = if (isActive) 1.04f else 0.98f,
                                            animationSpec = spring(dampingRatio = 0.65f),
                                            label = "lineScale"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .scale(lineScale)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isActive) Color(0xFF38BDF8).copy(alpha = 0.12f)
                                                    else Color.Transparent
                                                )
                                                .border(
                                                    width = if (isActive) 1.dp else 0.dp,
                                                    color = if (isActive) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color.Transparent,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    onSeekTo(line.timeMs)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isActive) {
                                                    LiveEqualizerMini(
                                                        isPlaying = true,
                                                        barColor = Color(0xFF38BDF8),
                                                        modifier = Modifier.padding(end = 10.dp)
                                                    )
                                                }
                                                Text(
                                                    text = line.text,
                                                    color = textColor,
                                                    fontSize = if (isActive) 23.sp else 17.sp,
                                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                                    lineHeight = if (isActive) 32.sp else 24.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pie inferior: Pista táctil
                    if (lines.isNotEmpty()) {
                        Text(
                            text = "💡 Toca cualquier frase para saltar directamente a ese segundo",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
