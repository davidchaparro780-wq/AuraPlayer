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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.auraplayer.util.ArtworkPalette
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fondo líquido dinámico estilo "Aurora Mesh Gradient" (Apple Music iOS 18 / VisionOS).
 * Orquesta 4 orbes de gradiente radial vivos que orbitan en curvas armónicas de Lissajous,
 * difuminándose con blend modes suaves sobre un lienzo de ultra profundidad oscura.
 * 100% acelerado por hardware en Compose DrawScope (cero recomposiciones de layout).
 */
@Composable
fun AuroraMeshBackground(
    palette: ArtworkPalette?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    baseDarkColor: Color = Color(0xFF060810)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora_mesh_flow")

    // Tiempo armónico continuo (0..2π)
    val timePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 16000 else 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timePhase"
    )

    // Pulsación suave de respiración
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 4000 else 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // Colores dinámicos extraídos de la carátula o paleta cyber por defecto
    val primary = palette?.seed ?: Color(0xFF6366F1)
    val accent = palette?.accent ?: Color(0xFFEC4899)
    val tertiary = Color(
        red = (primary.red * 0.3f + accent.red * 0.7f).coerceIn(0f, 1f),
        green = (primary.green * 0.6f + accent.green * 0.2f).coerceIn(0f, 1f),
        blue = (primary.blue * 0.4f + accent.blue * 0.6f).coerceIn(0f, 1f)
    )
    val cyanGlow = Color(0xFF00F0FF)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Fondo base ultra oscuro
        drawRect(color = baseDarkColor)

        if (w <= 0f || h <= 0f) return@Canvas

        val t = timePhase
        val b = breathScale

        // Trayectorias paramétricas Lissajous para los 4 orbes luminosos
        // Orbe 1: Primario (esquina superior izquierda oscilante)
        val x1 = w * (0.35f + 0.22f * sin(t))
        val y1 = h * (0.28f + 0.16f * cos(t * 1.3f))
        val r1 = (w * 0.65f) * b

        // Orbe 2: Acento magenta/rosa (lado derecho medio)
        val x2 = w * (0.68f + 0.20f * cos(t * 0.8f))
        val y2 = h * (0.48f + 0.22f * sin(t * 1.1f))
        val r2 = (w * 0.70f) * (2f - b)

        // Orbe 3: Cyan / Neón profundo (parte inferior flotante)
        val x3 = w * (0.42f + 0.28f * sin(t * 0.7f + 1.2f))
        val y3 = h * (0.75f + 0.14f * cos(t * 0.9f))
        val r3 = (w * 0.58f) * b

        // Orbe 4: Púrpura central / profundo
        val x4 = w * (0.50f + 0.18f * cos(t * 1.5f + 2.0f))
        val y4 = h * (0.40f + 0.18f * sin(t * 0.6f))
        val r4 = (w * 0.50f) * (0.95f + 0.1f * sin(t))

        // Dibujar orbes con gradientes radiales y opacidades difusas
        val orbAlpha = if (isPlaying) 0.38f else 0.24f

        // Orbe 1
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = orbAlpha), Color.Transparent),
                center = Offset(x1, y1),
                radius = r1
            ),
            center = Offset(x1, y1),
            radius = r1
        )

        // Orbe 2
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = orbAlpha * 0.9f), Color.Transparent),
                center = Offset(x2, y2),
                radius = r2
            ),
            center = Offset(x2, y2),
            radius = r2
        )

        // Orbe 3
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(cyanGlow.copy(alpha = orbAlpha * 0.6f), Color.Transparent),
                center = Offset(x3, y3),
                radius = r3
            ),
            center = Offset(x3, y3),
            radius = r3
        )

        // Orbe 4
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = orbAlpha * 0.75f), Color.Transparent),
                center = Offset(x4, y4),
                radius = r4
            ),
            center = Offset(x4, y4),
            radius = r4
        )

        // Velo superior sutil para fusionar todo en una atmósfera elegante
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x22000000),
                    Color(0x44050810),
                    Color(0x8804060C)
                )
            )
        )
    }
}
