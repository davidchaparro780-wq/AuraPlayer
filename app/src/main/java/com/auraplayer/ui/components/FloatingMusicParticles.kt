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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

private data class MusicParticle(
    val initialXRatio: Float,
    val initialYRatio: Float,
    val size: Float,
    val speed: Float,
    val amplitude: Float,
    val alphaBase: Float,
    val colorOffset: Float
)

/**
 * Fondo animado de micro-partículas estelares (Floating Music Particles).
 * Partículas de luz flotantes que se mueven con fluidas ondas sinusoidales,
 * iluminándose y acelerando orgánicamente según la reproducción musical.
 */
@Composable
fun FloatingMusicParticles(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF38BDF8),
    particleCount: Int = 22
) {
    val particles = remember {
        List(particleCount) { i ->
            MusicParticle(
                initialXRatio = ((i * 37) % 100) / 100f,
                initialYRatio = ((i * 59) % 100) / 100f,
                size = 1.8f + ((i % 4) * 1.4f),
                speed = 0.6f + ((i % 5) * 0.35f),
                amplitude = 18f + ((i % 3) * 14f),
                alphaBase = 0.22f + ((i % 4) * 0.14f),
                colorOffset = (i % 3).toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "music_particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTime"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val phase = time * p.speed
            val currentX = (p.initialXRatio * w) + sin(phase + p.colorOffset) * p.amplitude
            // Flotan hacia arriba suavemente si está reproduciendo
            val speedFactor = if (isPlaying) 1.5f else 0.5f
            val yShift = ((phase * speedFactor * 30f) % h)
            var currentY = (p.initialYRatio * h) - yShift
            if (currentY < 0f) currentY += h

            val dynamicAlpha = (p.alphaBase * (0.6f + 0.4f * sin(phase))).coerceIn(0.08f, 0.85f)
            val pColor = if (p.colorOffset > 1f) {
                Color(0xFF8B5CF6).copy(alpha = dynamicAlpha)
            } else {
                accentColor.copy(alpha = dynamicAlpha)
            }

            drawCircle(
                color = pColor,
                radius = p.size,
                center = Offset(currentX, currentY)
            )

            // Halo suave para las partículas más grandes
            if (p.size > 3f) {
                drawCircle(
                    color = pColor.copy(alpha = dynamicAlpha * 0.35f),
                    radius = p.size * 2.2f,
                    center = Offset(currentX, currentY)
                )
            }
        }
    }
}
