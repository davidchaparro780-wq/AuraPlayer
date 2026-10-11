package com.auraplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/**
 * Barra de búsqueda con Onda de Audio Interactiva (Waveform Seekbar).
 * Muestra 42 barras de onda armónica reactivas con gradiente neón en la zona reproducida,
 * efecto de magnificación táctil (lente de aumento) al deslizar el dedo y destellos fluidos.
 */
@Composable
fun DynamicWaveformSeekbar(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF38BDF8),
    secondaryColor: Color = Color(0xFF8B5CF6)
) {
    val barCount = 42

    // Shimmer wave que recorre las barras
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_shimmer")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPhase"
    )

    var isDragging by remember { mutableStateOf(false) }
    var touchX by remember { mutableFloatStateOf(0f) }

    // Generar alturas armónicas pseudo-musicales fijas para consistencia visual
    val baseHeights = remember {
        FloatArray(barCount) { i ->
            val angle = (i.toFloat() / barCount) * Math.PI * 3.5f
            val wave1 = (sin(angle).toFloat() * 0.5f + 0.5f)
            val wave2 = (sin(angle * 2.1f + 1.2f).toFloat() * 0.35f + 0.35f)
            (0.25f + 0.45f * wave1 + 0.30f * wave2).coerceIn(0.2f, 1.0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchX = offset.x
                        val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(newProgress)
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        touchX = offset.x
                        val newProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(newProgress)
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        touchX = change.position.x
                        val newProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(newProgress)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val totalBarSpace = w / barCount
            val barWidth = totalBarSpace * 0.65f
            val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

            val currentPlayX = progress * w

            for (i in 0 until barCount) {
                val barCenterX = i * totalBarSpace + (totalBarSpace / 2f)
                val isPassed = barCenterX <= currentPlayX

                var normalizedHeight = baseHeights[i]

                // Animación sutil de respiración armónica si está reproduciendo
                if (isPlaying) {
                    val flutter = 0.12f * sin(shimmerPhase + i * 0.3f)
                    normalizedHeight = (normalizedHeight + flutter).coerceIn(0.18f, 1.0f)
                }

                // Efecto de lente de magnificación al arrastrar con el dedo
                if (isDragging) {
                    val distToTouch = abs(barCenterX - touchX)
                    val maxInfluence = w * 0.14f
                    if (distToTouch < maxInfluence) {
                        val factor = 1f - (distToTouch / maxInfluence)
                        normalizedHeight = (normalizedHeight + factor * 0.45f).coerceAtMost(1.2f)
                    }
                }

                val barH = (h * 0.78f) * normalizedHeight
                val barTop = (h - barH) / 2f

                if (isPassed) {
                    // Barras ya reproducidas: Gradiente Neón brillante
                    val progressFraction = (barCenterX / (currentPlayX.coerceAtLeast(1f)))
                    val barColor = Brush.verticalGradient(
                        colors = listOf(
                            activeColor,
                            secondaryColor
                        ),
                        startY = barTop,
                        endY = barTop + barH
                    )
                    drawRoundRect(
                        brush = barColor,
                        topLeft = Offset(barCenterX - barWidth / 2f, barTop),
                        size = Size(barWidth, barH),
                        cornerRadius = cornerRadius
                    )

                    // Destello superior
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = barWidth * 0.45f,
                        center = Offset(barCenterX, barTop + barWidth * 0.5f)
                    )
                } else {
                    // Barras futuras por reproducir
                    val futureColor = Color(0xFF242E42)
                    drawRoundRect(
                        color = futureColor,
                        topLeft = Offset(barCenterX - barWidth / 2f, barTop),
                        size = Size(barWidth, barH),
                        cornerRadius = cornerRadius
                    )
                }
            }

            // Cursor cabezal de reproducción (Glow Scrub Indicator)
            val headX = currentPlayX.coerceIn(0f, w)
            val headCenterY = h / 2f

            // Sombra y halo de brillo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(activeColor.copy(alpha = 0.85f), Color.Transparent),
                    center = Offset(headX, headCenterY),
                    radius = 16.dp.toPx()
                ),
                radius = 16.dp.toPx(),
                center = Offset(headX, headCenterY)
            )

            // Núcleo blanco nítido del cabezal
            drawCircle(
                color = Color.White,
                radius = if (isDragging) 6.dp.toPx() else 4.5.dp.toPx(),
                center = Offset(headX, headCenterY)
            )
        }
    }
}
