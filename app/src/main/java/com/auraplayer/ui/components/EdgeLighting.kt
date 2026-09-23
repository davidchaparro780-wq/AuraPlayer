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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * EdgeLighting renders a stunning neon glow perimeter along the edges of the screen,
 * pulsing in sync with music playback.
 */
@Composable
fun EdgeLighting(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    colors: List<Color> = listOf(
        Color(0xFF00F0FF), // Neon Cyan
        Color(0xFFFF0055), // Neon Pink
        Color(0xFF8B5CF6), // Purple
        Color(0xFFFFD700), // Sunset Gold
        Color(0xFF00F0FF)
    )
) {
    val transition = rememberInfiniteTransition(label = "EdgeLightingLoop")

    val pulseAlpha by transition.animateFloat(
        initialValue = if (isPlaying) 0.5f else 0.2f,
        targetValue = if (isPlaying) 1.0f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 650 else 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EdgePulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 4.dp.toPx()
        val glowStrokeWidth = 14.dp.toPx()

        // Outer ambient glow
        val glowGradient = Brush.sweepGradient(
            colors = colors.map { it.copy(alpha = pulseAlpha * 0.7f) },
            center = Offset(size.width / 2f, size.height / 2f)
        )
        drawRect(
            brush = glowGradient,
            topLeft = Offset.Zero,
            size = size,
            style = Stroke(width = glowStrokeWidth)
        )

        // Crisp inner neon stroke
        val innerGradient = Brush.sweepGradient(
            colors = colors.map { it.copy(alpha = pulseAlpha) },
            center = Offset(size.width / 2f, size.height / 2f)
        )
        drawRect(
            brush = innerGradient,
            topLeft = Offset.Zero,
            size = size,
            style = Stroke(width = strokeWidth)
        )
    }
}
