package com.auraplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Efecto de Onda de Choque Supersónica (Supernova Beat Shockwave).
 * Libera anillos concéntricos expansivos de alta velocidad con gradiente de neón
 * desde el centro del vinilo en transiciones o picos acústicos.
 */
@Composable
fun SupernovaShockwave(
    trigger: Long,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF38BDF8),
    secondaryColor: Color = Color(0xFFEC4899)
) {
    if (trigger <= 0L) return

    val progress = remember(trigger) { Animatable(0f) }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
    }

    val currentProg = progress.value
    if (currentProg >= 1f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension * 0.95f
        val radius = maxRadius * currentProg
        val alpha = (1f - currentProg).coerceIn(0f, 1f)

        // Anillo principal supersónico
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    accentColor.copy(alpha = alpha * 0.9f),
                    secondaryColor.copy(alpha = alpha * 0.6f),
                    Color.Transparent
                ),
                center = center,
                radius = radius.coerceAtLeast(1f)
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 6f * (1f - currentProg * 0.6f))
        )

        // Anillo secundario con desfase
        val subRadius = (radius * 0.72f).coerceAtLeast(0f)
        if (subRadius > 0f) {
            drawCircle(
                color = secondaryColor.copy(alpha = alpha * 0.5f),
                radius = subRadius,
                center = center,
                style = Stroke(width = 3.5f * (1f - currentProg))
            )
        }
    }
}
