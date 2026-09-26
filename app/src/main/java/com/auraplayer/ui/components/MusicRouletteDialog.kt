package com.auraplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun MusicRouletteDialog(
    songs: List<MediaModel>,
    onSongSelected: (MediaModel) -> Unit,
    onDismiss: () -> Unit
) {
    if (songs.isEmpty()) { onDismiss(); return }

    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<MediaModel?>(null) }

    val wheelColors = listOf(
        Color(0xFF6C63FF), Color(0xFFFF6584), Color(0xFF00C9FF),
        Color(0xFF00F0C0), Color(0xFFFFD700), Color(0xFFFF4081),
        Color(0xFF7C4DFF), Color(0xFF00BFA5)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shuffle, contentDescription = null,
                    tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("🎲 Ruleta Musical", fontWeight = FontWeight.ExtraBold,
                    color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Spinning Wheel
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)) {

                    // The wheel
                    Canvas(modifier = Modifier.fillMaxSize().rotate(rotation.value)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = min(cx, cy) - 4f
                        val sliceAngle = 360f / songs.size.coerceAtLeast(1)

                        songs.forEachIndexed { i, _ ->
                            val startAngle = i * sliceAngle - 90f
                            drawArc(
                                color = wheelColors[i % wheelColors.size],
                                startAngle = startAngle,
                                sweepAngle = sliceAngle - 1f,
                                useCenter = true,
                                topLeft = Offset(cx - radius, cy - radius),
                                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                            )
                            // Spoke text
                            val midAngle = Math.toRadians((startAngle + sliceAngle / 2).toDouble())
                            val textR = radius * 0.65f
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                radius = 18f,
                                center = Offset(
                                    cx + textR * cos(midAngle).toFloat(),
                                    cy + textR * sin(midAngle).toFloat()
                                )
                            )
                        }

                        // Center circle
                        drawCircle(color = Color(0xFF0C101D), radius = 28f, center = Offset(cx, cy))
                        drawCircle(color = Color(0xFFFFD700), radius = 28f, center = Offset(cx, cy),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                    }

                    // Pointer triangle at top
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx - 12f, 0f)
                                lineTo(cx + 12f, 0f)
                                lineTo(cx, 28f)
                                close()
                            },
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Show result
                selectedSong?.let { song ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2035)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎯 ¡Cayó en!", color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(song.title, color = Color.White,
                                fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                                textAlign = TextAlign.Center)
                            Text(song.artist, color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                } ?: Text(
                    "Toca GIRAR para elegir\nuna canción aleatoria",
                    color = Color(0xFF64748B), textAlign = TextAlign.Center, fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (!isSpinning) {
                            isSpinning = true
                            val extraSpins = (5..10).random() * 360f
                            val randomOffset = (0 until songs.size).random() * (360f / songs.size)
                            scope.launch {
                                rotation.animateTo(
                                    rotation.value + extraSpins + randomOffset,
                                    animationSpec = tween(3000, easing = FastOutSlowInEasing)
                                )
                                // Determine which song landed
                                val normalizedAngle = ((rotation.value % 360f) + 360f) % 360f
                                val sliceAngle = 360f / songs.size
                                // pointer is at top (270° in standard coords)
                                val pointerAngle = (270f - normalizedAngle + 360f) % 360f
                                val idx = (pointerAngle / sliceAngle).toInt().coerceIn(0, songs.size - 1)
                                selectedSong = songs[idx]
                                isSpinning = false
                            }
                        }
                    },
                    enabled = !isSpinning,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSpinning) "Girando… 🌀" else "🎲 ¡GIRAR!",
                        color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
                if (selectedSong != null) {
                    Button(
                        onClick = { selectedSong?.let { onSongSelected(it) }; onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("▶ Reproducir esta canción", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            }
        },
        dismissButton = {}
    )
}
