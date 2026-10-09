package com.auraplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fondo Ambilight Fluido Reactivo (Aura Mesh Gradient).
 * Crea una atmósfera envolvente e hipnótica estilo Apple Music iOS 18 / Canvas,
 * con esferas de luz líquida orgánica que respiran al ritmo visual de la música.
 */
@Composable
fun FluidAmbilightGlow(
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_ambilight")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambilight_phase"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambilight_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .blur(60.dp) // Desenfoque Gaussiano profundo estilo Mesh Gradient
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Esfera de luz 1 (Primaria - Arriba izquierda / centro)
            val x1 = width * (0.35f + 0.2f * sin(phase))
            val y1 = height * (0.3f + 0.15f * cos(phase))
            val r1 = (width * 0.65f) * pulseScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(x1, y1),
                    radius = r1
                ),
                radius = r1,
                center = Offset(x1, y1)
            )

            // Esfera de luz 2 (Secundaria - Abajo derecha)
            val x2 = width * (0.65f - 0.2f * cos(phase))
            val y2 = height * (0.7f - 0.15f * sin(phase))
            val r2 = (width * 0.7f) * pulseScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondaryColor.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(x2, y2),
                    radius = r2
                ),
                radius = r2,
                center = Offset(x2, y2)
            )

            // Esfera de luz 3 (Acento central profundo)
            val x3 = width * 0.5f
            val y3 = height * 0.5f
            val r3 = width * 0.5f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(x3, y3),
                    radius = r3
                ),
                radius = r3,
                center = Offset(x3, y3)
            )
        }
    }
}
