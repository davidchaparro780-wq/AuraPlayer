package com.auraplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Indicador visual miniatura de ecualizador en vivo (Live Equalizer Mini).
 * 4 barras neón saltando a ritmos dinámicos para indicar reproducción activa.
 */
@Composable
fun LiveEqualizerMini(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    barColor: Color = Color(0xFF38BDF8),
    width: Dp = 16.dp,
    height: Dp = 14.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars_mini")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(310, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(390, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4"
    )

    Canvas(
        modifier = modifier.size(width = width, height = height)
    ) {
        val barWidth = 2.5.dp.toPx()
        val spacing = 1.5.dp.toPx()
        val cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        val heights = if (isPlaying) floatArrayOf(h1, h2, h3, h4) else floatArrayOf(0.2f, 0.2f, 0.2f, 0.2f)

        for (i in 0 until 4) {
            val barH = (heights[i] * size.height).coerceAtLeast(2.5.dp.toPx())
            val left = i * (barWidth + spacing)
            val top = size.height - barH
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = cornerRadius
            )
        }
    }
}
