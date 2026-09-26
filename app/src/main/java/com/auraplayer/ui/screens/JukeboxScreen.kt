package com.auraplayer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.model.MediaModel

@Composable
fun JukeboxScreen(
    songs: List<MediaModel>,
    currentSong: MediaModel?,
    isPlaying: Boolean,
    onSongClick: (MediaModel) -> Unit,
    onBack: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jukebox")
    val vinylRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "vinyl"
    )
    val neonPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "neon"
    )
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0000), Color(0xFF1A0500), Color(0xFF000000))))
    ) {
        Column(Modifier.fillMaxSize()) {

            // Header — Retro Jukebox Title
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF3D0000), Color(0xFF8B0000), Color(0xFF3D0000))))
                    .padding(vertical = 16.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                            tint = Color(0xFFFFD700))
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎰 DaVE JUKEBOX", fontFamily = FontFamily.Monospace,
                            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD700))
                        Text("◆ SELECT YOUR SONG ◆", fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp, color = Color(0xFFFF6600).copy(neonPulse))
                    }
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(48.dp))
                }
            }

            // Now Playing vinyl section
            currentSong?.let { song ->
                Box(
                    Modifier.fillMaxWidth()
                        .background(Color(0xFF0D0000))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                        // Vinyl disc
                        Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                            Canvas(Modifier.fillMaxSize().rotate(if (isPlaying) vinylRotation else 0f)) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val r = size.minDimension / 2f

                                // Outer disc - dark grooves
                                drawCircle(color = Color(0xFF1A0A00), radius = r, center = Offset(cx, cy))
                                // Groove rings
                                for (i in 1..5) {
                                    drawCircle(color = Color(0xFF2D1500), radius = r * (1f - i * 0.12f),
                                        center = Offset(cx, cy), style = Stroke(1f))
                                }
                                // Red label center
                                drawCircle(color = Color(0xFFCC0000), radius = r * 0.35f, center = Offset(cx, cy))
                                // Center hole
                                drawCircle(color = Color(0xFF0A0000), radius = 8f, center = Offset(cx, cy))
                                // Gold highlight
                                drawArc(color = Color(0xFFFFD700).copy(0.4f), startAngle = -60f,
                                    sweepAngle = 40f, useCenter = false,
                                    topLeft = Offset(cx - r + 4f, cy - r + 4f),
                                    size = Size((r - 4f) * 2, (r - 4f) * 2),
                                    style = Stroke(3f))
                            }
                        }

                        Column(Modifier.weight(1f)) {
                            Text("▶ NOW PLAYING", fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp, color = Color(0xFFFF6600).copy(neonPulse))
                            Text(song.title, color = Color(0xFFFFD700),
                                fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
                            Text(song.artist, color = Color(0xFFFF9900), fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            // LED line separator
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                drawRect(Brush.horizontalGradient(
                    listOf(Color.Transparent, Color(0xFFFF6600).copy(neonPulse), Color(0xFFFFD700).copy(neonPulse),
                        Color(0xFFFF6600).copy(neonPulse), Color.Transparent)
                ))
            }

            // Song list — Jukebox style
            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                itemsIndexed(songs) { index, song ->
                    val isSelected = song.id == currentSong?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected)
                                    Brush.horizontalGradient(listOf(Color(0xFF3D0000), Color(0xFF200000)))
                                else if (index % 2 == 0)
                                    Brush.horizontalGradient(listOf(Color(0xFF0D0000), Color(0xFF150A00)))
                                else
                                    Brush.horizontalGradient(listOf(Color(0xFF0A0000), Color(0xFF0D0000)))
                            )
                            .clickable { onSongClick(song) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Track number in neon
                        Text(
                            String.format("%03d", index + 1),
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) Color(0xFFFF6600) else Color(0xFF4A2000),
                            fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(12.dp))
                        // Neon dot playing indicator
                        Box(
                            Modifier.size(8.dp)
                                .background(
                                    if (isSelected && isPlaying) Color(0xFF00FF00)
                                    else Color.Transparent,
                                    RoundedCornerShape(50)
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title,
                                color = if (isSelected) Color(0xFFFFD700) else Color(0xFFCC8800),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 14.sp, maxLines = 1, fontFamily = FontFamily.Monospace)
                            Text(song.artist,
                                color = if (isSelected) Color(0xFFFF9900) else Color(0xFF664400),
                                fontSize = 11.sp, maxLines = 1)
                        }
                        // Insert coin button style
                        Text("►",
                            color = if (isSelected) Color(0xFFFFD700) else Color(0xFF4A2000),
                            fontSize = 18.sp)
                    }
                    // Divider
                    if (index < songs.size - 1) {
                        HorizontalDivider(color = Color(0xFF1A0A00), thickness = 0.5.dp)
                    }
                }
            }

            // Bottom strip
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF3D0000), Color(0xFF8B0000), Color(0xFF3D0000))))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("◆ INSERT COIN TO CONTINUE ◆", fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp, color = Color(0xFFFFD700).copy(neonPulse))
            }
        }
    }
}
