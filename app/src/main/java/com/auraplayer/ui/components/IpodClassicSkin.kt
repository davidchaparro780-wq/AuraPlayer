package com.auraplayer.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.auraplayer.data.model.MediaModel
import kotlin.math.atan2

/**
 * Skin Retro: iPod Classic con Click Wheel Táctil Háptica.
 * Pantalla LCD superior estilo años 2000 y rueda táctil giratoria con
 * respuesta háptica mecánica continua (clic-clic) para control total.
 */
@Composable
fun IpodClassicSkin(
    song: MediaModel?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    var lastAngle by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0B0F19))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Chasis del iPod Classic (Carcasa blanca/plateada)
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(640.dp)
                        .shadow(24.dp, RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFAFAFA), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                            )
                        )
                        .border(3.dp, Color(0xFF94A3B8), RoundedCornerShape(32.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Botón para salir en la esquina superior
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF475569))
                        }
                    }

                    // 1. Pantalla LCD Superior
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE2E8F0))
                            .border(2.dp, Color(0xFF64748B), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Barra superior de estado
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isPlaying) "▶ En reproducción" else "⏸ En pausa",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "DaVE Pod",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0284C7)
                                )
                            }

                            // Canción actual + Carátula
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (song?.artworkUri != null) {
                                    AsyncImage(
                                        model = song.artworkUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF94A3B8)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🎵", fontSize = 32.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song?.title ?: "Sin canción",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song?.artist ?: "Desconocido",
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song?.album ?: "Álbum",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Barra de progreso LCD
                            Column {
                                val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF0284C7),
                                    trackColor = Color(0xFFCBD5E1)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatTime(currentPositionMs),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = "-${formatTime((durationMs - currentPositionMs).coerceAtLeast(0L))}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. La Mítica Click Wheel Táctil Inferior
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .shadow(12.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFFF8FAFC))
                            .border(2.dp, Color(0xFFCBD5E1), CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val currentAngle = Math
                                        .toDegrees(
                                            atan2(
                                                (change.position.y - center.y).toDouble(),
                                                (change.position.x - center.x).toDouble()
                                            )
                                        )
                                        .toFloat()
                                    val diff = currentAngle - lastAngle
                                    if (Math.abs(diff) > 18f && Math.abs(diff) < 180f) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        if (diff > 0) {
                                            onSeekRelative(4000L) // Giro horario: adelantar
                                        } else {
                                            onSeekRelative(-4000L) // Giro antihorario: retroceder
                                        }
                                        lastAngle = currentAngle
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Botón MENU superior
                        Text(
                            text = "MENU",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 18.dp)
                                .clickable { onDismiss() }
                        )

                        // Botón Anterior (Izquierda)
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Anterior",
                            tint = Color(0xFF64748B),
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 18.dp)
                                .size(28.dp)
                                .clickable { onPrevious() }
                        )

                        // Botón Siguiente (Derecha)
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Siguiente",
                            tint = Color(0xFF64748B),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 18.dp)
                                .size(28.dp)
                                .clickable { onNext() }
                        )

                        // Botón Play/Pausa (Abajo)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 18.dp)
                                .clickable { onTogglePlay() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "▶ ❚❚", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF64748B))
                        }

                        // Botón Central Seleccionar (Center Button)
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                                )
                            )
                            .border(1.dp, Color(0xFF94A3B8), CircleShape)
                            .clickable { onTogglePlay() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play/Pausa",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}
