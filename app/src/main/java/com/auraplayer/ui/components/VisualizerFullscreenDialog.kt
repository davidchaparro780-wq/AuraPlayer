package com.auraplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.auraplayer.data.model.MediaModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VisualizerFullscreenDialog(
    song: MediaModel?,
    isPlaying: Boolean,
    onDismiss: () -> Unit
) {
    var visualizerMode by remember { mutableIntStateOf(0) } // 0: Bars, 1: Orbital Ring, 2: OLED Pure Black

    val infiniteTransition = rememberInfiniteTransition(label = "vis_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (visualizerMode == 2) Color.Black else Color(0xFF060913))
                .clickable { visualizerMode = (visualizerMode + 1) % 3 },
            contentAlignment = Alignment.Center
        ) {
            when (visualizerMode) {
                0 -> {
                    // Holographic Cyberpunk Bars
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barCount = 28
                        val barWidth = size.width / (barCount * 1.5f)
                        val maxBarHeight = size.height * 0.45f
                        val centerY = size.height * 0.55f

                        for (i in 0 until barCount) {
                            val wave = com.auraplayer.audio.RealtimeVisualizerManager.instance.getBand(i, barCount, isPlaying)
                            val h = (wave * maxBarHeight).coerceIn(8f, maxBarHeight)
                            val x = i * (barWidth * 1.5f) + (barWidth * 0.25f)

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFF00F0FF), Color(0xFFEC4899), Color(0xFF8B5CF6))
                                ),
                                topLeft = Offset(x, centerY - h / 2),
                                size = Size(barWidth, h),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                            )
                        }
                    }
                }
                1 -> {
                    // Orbital Pulsing Ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val baseRadius = size.minDimension * 0.28f
                        val particleCount = 36

                        for (i in 0 until particleCount) {
                            val angle = (i.toFloat() / particleCount) * 2 * Math.PI + phase
                            val pulsate = com.auraplayer.audio.RealtimeVisualizerManager.instance.getBand(i, particleCount, isPlaying) * 45f
                            val r = baseRadius + pulsate
                            val x = center.x + (cos(angle) * r).toFloat()
                            val y = center.y + (sin(angle) * r).toFloat()

                            drawCircle(
                                color = if (i % 2 == 0) Color(0xFF00F0FF) else Color(0xFFFFD700),
                                radius = 7f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
                2 -> {
                    // OLED Pure Black Extreme Battery Saver (<1% drain)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("🌑 MODO OLED PURE BLACK", color = Color(0xFF334155), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(song?.title ?: "Sin reproducción", color = Color(0xFFE2E8F0), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(song?.artist ?: "", color = Color(0xFF64748B), fontSize = 13.sp)
                        Spacer(Modifier.height(24.dp))
                        Text("Consumo de pantalla: < 1% por hora", color = Color(0xFF10B981), fontSize = 11.sp)
                    }
                }
            }

            // Overlay Info Text & Mode Indicator
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (visualizerMode != 2 && song != null) {
                    Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(song.artist, color = Color(0xFFA5B4FC), fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                }
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        when (visualizerMode) {
                            0 -> "Espectro Cyberpunk • Toca para cambiar"
                            1 -> "Anillo Orbital 3D • Toca para cambiar"
                            else -> "OLED Batería Extrema • Toca para cambiar"
                        },
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
