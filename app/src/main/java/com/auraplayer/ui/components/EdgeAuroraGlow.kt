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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

/**
 * Resplandor de Aurora Boreal Perimetral (Edge Aurora Breathing Glow).
 * Genera un halo suave y vivo de luz multicolor en los 4 bordes de la pantalla
 * que respira, ondula y reacciona cromáticamente con la música.
 */
@Composable
fun EdgeAuroraGlow(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF38BDF8),
    secondaryColor: Color = Color(0xFF8B5CF6),
    tertiaryColor: Color = Color(0xFFEC4899)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "edge_aurora")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraPulse"
    )

    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val activeAlpha = if (isPlaying) pulse else 0.2f

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val edgeThickness = 32f

        // Borde Superior
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = activeAlpha * 0.7f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = edgeThickness * 1.5f
            ),
            topLeft = Offset(0f, 0f),
            size = Size(w, edgeThickness * 1.5f)
        )

        // Borde Inferior
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    secondaryColor.copy(alpha = activeAlpha * 0.7f)
                ),
                startY = h - edgeThickness * 1.5f,
                endY = h
            ),
            topLeft = Offset(0f, h - edgeThickness * 1.5f),
            size = Size(w, edgeThickness * 1.5f)
        )

        // Borde Izquierdo
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    tertiaryColor.copy(alpha = activeAlpha * 0.6f),
                    Color.Transparent
                ),
                startX = 0f,
                endX = edgeThickness
            ),
            topLeft = Offset(0f, 0f),
            size = Size(edgeThickness, h)
        )

        // Borde Derecho
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    primaryColor.copy(alpha = activeAlpha * 0.6f)
                ),
                startX = w - edgeThickness,
                endX = w
            ),
            topLeft = Offset(w - edgeThickness, 0f),
            size = Size(edgeThickness, h)
        )
    }
}
