package com.auraplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Efecto de micro-animación explosiva de partículas al marcar Favorito / Like.
 * Dispara un anillo de choque de neón junto con 14 partículas brillantes
 * radiales que se dispersan en órbita y un corazón central elástico.
 */
@Composable
fun ExplosiveHeartBurst(
    modifier: Modifier = Modifier
) {
    val heartScale = remember { Animatable(0.2f) }
    val burstProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Escala elástica del corazón central
        heartScale.animateTo(
            targetValue = 1.35f,
            animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f)
        )
        heartScale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(180, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        // Dispersión radial de partículas y anillo de choque
        burstProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Partículas radiales y anillo de choque
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val p = burstProgress.value
            val alpha = (1f - p).coerceIn(0f, 1f)

            if (alpha > 0f) {
                // Anillo de choque expansivo
                val shockwaveRadius = 24.dp.toPx() + (56.dp.toPx() * p)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF0055).copy(alpha = alpha * 0.8f),
                            Color(0xFFEC4899).copy(alpha = alpha * 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = shockwaveRadius
                    ),
                    radius = shockwaveRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = (4.dp.toPx() * (1f - p)).coerceAtLeast(1f))
                )

                // 14 partículas radiales brillantes
                val particleCount = 14
                val particleColors = listOf(
                    Color(0xFFFF0055),
                    Color(0xFFEC4899),
                    Color(0xFFFF71D4),
                    Color(0xFFF43F5E),
                    Color(0xFFFFB4E6)
                )

                for (i in 0 until particleCount) {
                    val angle = (i.toFloat() / particleCount) * (2 * Math.PI).toFloat()
                    val dist = (28.dp.toPx() + (58.dp.toPx() * p)) * (0.85f + 0.3f * ((i % 3) / 2f))
                    val px = cx + dist * cos(angle)
                    val py = cy + dist * sin(angle)
                    val pRadius = (5.dp.toPx() * (1f - p * 0.75f)).coerceAtLeast(1.5f)
                    val pColor = particleColors[i % particleColors.size].copy(alpha = alpha)

                    drawCircle(
                        color = pColor,
                        radius = pRadius,
                        center = Offset(px, py)
                    )
                }
            }
        }

        // Corazón central con resplandor neón
        Box(
            modifier = Modifier
                .size(90.dp)
                .scale(heartScale.value)
                .shadow(24.dp, CircleShape, spotColor = Color(0xFFFF0055), ambientColor = Color(0xFFFF2A85))
                .background(Color(0xF018081C), CircleShape)
                .border(
                    2.dp,
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFF0055),
                            Color(0xFFEC4899),
                            Color(0xFFF43F5E),
                            Color(0xFFFF0055)
                        )
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF0055),
                modifier = Modifier.size(52.dp)
            )
        }
    }
}
