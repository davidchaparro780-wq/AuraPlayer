package com.auraplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Barra de progreso de audio con fluido Shimmer Wave.
 * Muestra un destello líquido continuo viajando a lo largo del progreso
 * y un thumb pulsante interactivo para una estética visual de gama alta.
 */
@Composable
fun LiquidShimmerProgressBar(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF38BDF8),
    secondaryColor: Color = Color(0xFF8B5CF6)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_progress")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val thumbPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thumbPulse"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth

        // Pista inactiva de fondo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF1E293B))
        )

        // Pista activa con degradado dinámico y Shimmer Wave líquido
        val safeProgress = progress.coerceIn(0f, 1f)
        if (safeProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(safeProgress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isPlaying) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    activeColor,
                                    Color.White.copy(alpha = 0.8f),
                                    secondaryColor,
                                    activeColor
                                ),
                                startX = shimmerOffset * 300f,
                                endX = (shimmerOffset + 1f) * 300f
                            )
                        } else {
                            Brush.horizontalGradient(listOf(activeColor, secondaryColor))
                        }
                    )
            )
        }

        // Thumb de arrastre interactivo pulsante
        val thumbScale = if (isPlaying) thumbPulse else 1f
        val thumbOffset = ((totalWidth - 16.dp) * safeProgress).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(16.dp * thumbScale)
                .shadow(8.dp, CircleShape, spotColor = activeColor)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
