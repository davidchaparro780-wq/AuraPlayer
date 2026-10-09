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
import androidx.compose.ui.graphics.StrokeCap
import kotlin.random.Random

private data class WarpStar(
    val initialAngle: Float,
    val initialDistance: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

/**
 * Túnel Cósmico Hiperespacial (Hyperdrive Warp Speed Tunnel).
 * Simula un viaje estelar en 3D donde las estrellas se estiran en rayos de luz
 * a velocidad hiperespacial con el ritmo de la música.
 */
@Composable
fun HyperdriveWarpTunnel(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF38BDF8),
    starCount: Int = 45
) {
    val stars = remember {
        val colors = listOf(accentColor, Color(0xFF8B5CF6), Color(0xFFEC4899), Color.White)
        List(starCount) {
            WarpStar(
                initialAngle = Random.nextFloat() * 2f * Math.PI.toFloat(),
                initialDistance = Random.nextFloat(),
                speed = 0.4f + Random.nextFloat() * 0.8f,
                size = 1.2f + Random.nextFloat() * 2.2f,
                color = colors[Random.nextInt(colors.size)]
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "warp_stars")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 1800 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "warpPhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxDist = size.minDimension * 0.85f

        stars.forEach { s ->
            val curProgress = (s.initialDistance + phase * s.speed) % 1f
            val dist = curProgress * curProgress * maxDist // Expansión cuadrática 3D
            val prevDist = (curProgress - if (isPlaying) 0.08f else 0.02f).coerceAtLeast(0f) * maxDist

            val x = centerX + kotlin.math.cos(s.initialAngle) * dist
            val y = centerY + kotlin.math.sin(s.initialAngle) * dist

            val px = centerX + kotlin.math.cos(s.initialAngle) * prevDist
            val py = centerY + kotlin.math.sin(s.initialAngle) * prevDist

            val alpha = (curProgress * 1.2f).coerceIn(0.1f, 0.95f)

            if (isPlaying && curProgress > 0.15f) {
                // Estela hiperespacial
                drawLine(
                    color = s.color.copy(alpha = alpha),
                    start = Offset(px, py),
                    end = Offset(x, y),
                    strokeWidth = s.size * (0.8f + curProgress * 1.2f),
                    cap = StrokeCap.Round
                )
            } else {
                drawCircle(
                    color = s.color.copy(alpha = alpha),
                    radius = s.size * (0.5f + curProgress * 1.5f),
                    center = Offset(x, y)
                )
            }
        }
    }
}
