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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    var lines by remember { mutableStateOf<List<LrcLine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

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
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatQuote,
                                        contentDescription = null,
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No hay letras sincronizadas disponibles para esta canción.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center
                                    )
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
