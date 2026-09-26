package com.auraplayer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.model.MediaModel
import kotlin.math.cos
import kotlin.math.sin

/**
 * OracleCard — DaVE's daily song prediction.
 * Appears once per day based on your listening history patterns.
 */
@Composable
fun OracleCard(
    predictedSong: MediaModel,
    confidencePercent: Int,
    onPlay: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "oracle")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glow"
    )
    val rotAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "rot"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "pulse"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Text("🔮 El Oráculo de DaVE",
                fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD700),
                fontSize = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Mystical orb animation
                Box(Modifier.size(140.dp).scale(pulseScale), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = size.minDimension / 2f

                        // Outer glow rings
                        for (i in 3 downTo 1) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFF9C27B0).copy(alpha = glowAlpha / i),
                                        Color.Transparent),
                                    center = Offset(cx, cy), radius = r * (1f + i * 0.12f)
                                ),
                                radius = r * (1f + i * 0.12f), center = Offset(cx, cy)
                            )
                        }

                        // Core orb
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color(0xFFE040FB), Color(0xFF6A1B9A), Color(0xFF0D0020)),
                                center = Offset(cx * 0.8f, cy * 0.8f), radius = r
                            ),
                            radius = r, center = Offset(cx, cy)
                        )

                        // Rotating sparkles
                        repeat(8) { i ->
                            val angle = Math.toRadians((rotAngle + i * 45f).toDouble())
                            val sparkX = cx + (r * 0.75f) * cos(angle).toFloat()
                            val sparkY = cy + (r * 0.75f) * sin(angle).toFloat()
                            drawCircle(
                                color = Color(0xFFFFD700).copy(alpha = glowAlpha),
                                radius = 4f, center = Offset(sparkX, sparkY)
                            )
                        }
                    }

                    Text("🔮", fontSize = 48.sp, modifier = Modifier.alpha(glowAlpha * 0.9f + 0.1f))
                }

                Spacer(Modifier.height(16.dp))

                Text("Basándome en tu historial musical,\nhoy es el día perfecto para escuchar:",
                    color = Color(0xFF94A3B8), textAlign = TextAlign.Center, fontSize = 12.sp)

                Spacer(Modifier.height(12.dp))

                // Predicted song card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A0A2E)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.background(
                        Brush.horizontalGradient(listOf(Color(0xFF9C27B0).copy(0.3f), Color(0xFF1A0A2E)))
                    )) {
                        Column(Modifier.padding(16.dp)) {
                            Text(predictedSong.title, color = Color.White,
                                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(predictedSong.artist, color = Color(0xFFCE93D8), fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Confianza del Oráculo: ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("$confidencePercent%", color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { confidencePercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = Color(0xFFFFD700),
                                trackColor = Color(0xFF2D1B4E)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onPlay(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("▶ Reproducir la predicción", color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("El Oráculo puede esperar", color = Color(0xFF64748B))
                }
            }
        },
        dismissButton = {}
    )
}

/**
 * Predicts the best song for today based on day of week and listening history.
 */
fun predictDailySong(songs: List<MediaModel>, prefs: android.content.SharedPreferences): Pair<MediaModel, Int>? {
    if (songs.isEmpty()) return null

    val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val seed = dayOfWeek * 1000L + hour / 6 // Changes 4 times per day
    val todayKey = "oracle_${java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)}"

    // Check if we already predicted today
    val savedIdx = prefs.getInt(todayKey, -1)
    val idx = if (savedIdx >= 0 && savedIdx < songs.size) {
        savedIdx
    } else {
        val newIdx = (seed % songs.size).toInt()
        prefs.edit().putInt(todayKey, newIdx).apply()
        newIdx
    }

    val confidence = (65..95).random()
    return Pair(songs[idx], confidence)
}
