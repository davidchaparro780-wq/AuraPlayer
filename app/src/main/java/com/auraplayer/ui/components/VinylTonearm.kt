package com.auraplayer.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Brazo mecánico realista de tocadiscos analógico (Vinyl Tonearm).
 * Rota suavemente posándose sobre los surcos del vinilo cuando la música
 * se reproduce, y regresa a su base de descanso al pausar la reproducción.
 */
@Composable
fun VinylTonearm(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 130.dp,
    accentColor: Color = Color(0xFF38BDF8)
) {
    // Ángulo de rotación del brazo: en descanso (-14°) vs sobre el vinilo (24°)
    val armAngle by animateFloatAsState(
        targetValue = if (isPlaying) 23f else -14f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessLow
        ),
        label = "tonearmAngle"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Centro del pivote del brazo (esquina superior derecha)
        val pivotX = w * 0.78f
        val pivotY = h * 0.16f

        // 1. Base metálica del pivote (fija)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A)),
                center = Offset(pivotX, pivotY),
                radius = w * 0.14f
            ),
            radius = w * 0.13f,
            center = Offset(pivotX, pivotY)
        )
        drawCircle(
            color = Color(0xFF94A3B8).copy(alpha = 0.5f),
            radius = w * 0.13f,
            center = Offset(pivotX, pivotY),
            style = Stroke(width = 1.5f)
        )

        // 2. Contrapeso cilíndrico metálico detrás del pivote
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF64748B), Color(0xFF334155)),
                center = Offset(pivotX + w * 0.04f, pivotY - h * 0.05f),
                radius = w * 0.08f
            ),
            radius = w * 0.075f,
            center = Offset(pivotX + w * 0.04f, pivotY - h * 0.05f)
        )

        // Dibujar el brazo rotatorio alrededor del pivote
        rotate(degrees = armAngle, pivot = Offset(pivotX, pivotY)) {
            // Sombra del brazo metálico
            drawLine(
                color = Color.Black.copy(alpha = 0.35f),
                start = Offset(pivotX, pivotY + 4f),
                end = Offset(pivotX - w * 0.52f, pivotY + h * 0.62f + 4f),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )

            // Brazo cromado principal (tubo metálico)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFFCBD5E1)),
                    start = Offset(pivotX, pivotY),
                    end = Offset(pivotX - w * 0.52f, pivotY + h * 0.62f)
                ),
                start = Offset(pivotX, pivotY),
                end = Offset(pivotX - w * 0.52f, pivotY + h * 0.62f),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )

            // Codo curvado del brazo hacia la cápsula
            val elbowX = pivotX - w * 0.52f
            val elbowY = pivotY + h * 0.62f
            val headX = elbowX - w * 0.14f
            val headY = elbowY + h * 0.16f

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFCBD5E1), Color(0xFF64748B))
                ),
                start = Offset(elbowX, elbowY),
                end = Offset(headX, headY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Cápsula fonocaptora y cabezal (Headshell)
            val headPath = Path().apply {
                moveTo(headX - 6f, headY - 10f)
                lineTo(headX + 10f, headY - 4f)
                lineTo(headX + 4f, headY + 12f)
                lineTo(headX - 12f, headY + 6f)
                close()
            }
            drawPath(
                path = headPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )

            // Aguja de diamante brillante (Stylus)
            drawCircle(
                color = if (isPlaying) accentColor else Color(0xFFE2E8F0),
                radius = 2.5f,
                center = Offset(headX - 8f, headY + 8f)
            )

            // Brillo neón en la aguja al reproducir
            if (isPlaying) {
                drawCircle(
                    color = accentColor.copy(alpha = 0.6f),
                    radius = 5.5f,
                    center = Offset(headX - 8f, headY + 8f)
                )
            }
        }

        // Centro plateado del eje del pivote
        drawCircle(
            color = Color(0xFFF1F5F9),
            radius = w * 0.035f,
            center = Offset(pivotX, pivotY)
        )
    }
}
