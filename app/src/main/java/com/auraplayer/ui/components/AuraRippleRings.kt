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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Anillos concéntricos de pulso acústico (Aura Ripple Rings).
 * Ondas circulares de luz y energía que emanan desde detrás de la carátula,
 * expandiéndose y desvaneciéndose hacia el borde para un efecto visual hipnótico.
 */
@Composable
fun AuraRippleRings(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF38BDF8),
    ringCount: Int = 3
) {
    if (!isPlaying) return

    val infiniteTransition = rememberInfiniteTransition(label = "aura_ripples")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0.33f,
        targetValue = 1.33f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0.66f,
        targetValue = 1.66f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    val phases = listOf(phase1 % 1f, phase2 % 1f, phase3 % 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val baseRadius = size.minDimension * 0.44f
        val maxExtraRadius = size.minDimension * 0.22f

        phases.forEach { p ->
            val radius = baseRadius + (maxExtraRadius * p)
            val alpha = (1f - p) * 0.45f
            val strokeWidth = (3.5f * (1f - p * 0.5f))

            drawCircle(
                color = accentColor.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
