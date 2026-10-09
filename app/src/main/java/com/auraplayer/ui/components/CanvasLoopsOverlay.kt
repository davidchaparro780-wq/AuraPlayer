package com.auraplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class CanvasLoopTheme(val label: String) {
    OFF("Apagado"),
    RAIN("🌧️ Lluvia Lo-Fi"),
    SYNTHWAVE("🌆 Olas Synthwave"),
    STARFIELD("✨ Partículas Estelares")
}

/**
 * Lienzos Cinemáticos en Bucle (Canvas Loops Overlay estilo Spotify Canvas).
 * Renderiza animaciones ambientales fluidas a 60 fps como fondo vivo en el reproductor.
 */
@Composable
fun CanvasLoopsOverlay(
    theme: CanvasLoopTheme,
    modifier: Modifier = Modifier
) {
    if (theme == CanvasLoopTheme.OFF) return

    val transition = rememberInfiniteTransition(label = "canvas_loop")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loop_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        when (theme) {
            CanvasLoopTheme.RAIN -> {
                // Gotas de lluvia diagonales cayendo suavemente
                for (i in 0 until 40) {
                    val randX = ((i * 137) % width.toInt()).toFloat()
                    val startY = ((height * progress) + (i * 73)) % height
                    val dropLength = 22f + (i % 15)
                    drawLine(
                        color = Color(0xFF67E8F9).copy(alpha = 0.25f),
                        start = Offset(randX, startY),
                        end = Offset(randX - 4f, startY + dropLength),
                        strokeWidth = 1.5f
                    )
                }
            }
            CanvasLoopTheme.SYNTHWAVE -> {
                // Líneas de perspectiva de rejilla retro que avanzan
                val horizonY = height * 0.65f
                for (i in 0 until 8) {
                    val stepY = horizonY + (((i + progress) / 8f) * (height - horizonY))
                    val alpha = (((stepY - horizonY) / (height - horizonY)) * 0.4f).coerceIn(0f, 0.4f)
                    drawLine(
                        color = Color(0xFFE879F9).copy(alpha = alpha),
                        start = Offset(0f, stepY),
                        end = Offset(width, stepY),
                        strokeWidth = 2f
                    )
                }
            }
            CanvasLoopTheme.STARFIELD -> {
                // Partículas que flotan hacia arriba lentamente
                for (i in 0 until 35) {
                    val seed = (i * 997)
                    val x = (seed % width.toInt()).toFloat()
                    val y = (height - ((height * progress) + (i * 89)) % height)
                    val radius = 1.5f + (i % 3)
                    drawCircle(
                        color = Color(0xFFFDE047).copy(alpha = 0.35f),
                        radius = radius,
                        center = Offset(x, y)
                    )
                }
            }
            CanvasLoopTheme.OFF -> {}
        }
    }
}
