package com.auraplayer.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Botón Play/Pause con Elastic Morphing, Glow Burst y pulsación rítmica.
 * Proporciona respuesta visual táctil ultra-fluida al alternar reproducción.
 */
@Composable
fun PlayPauseMorphButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    iconSize: Dp = 42.dp,
    primaryColor: Color = Color(0xFF38BDF8),
    secondaryColor: Color = Color(0xFF8B5CF6)
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // Animación de pulso continuo cuando está reproduciendo
    val infiniteTransition = rememberInfiniteTransition(label = "play_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Explosión de halo (Glow Shockwave Burst) en tap
    val burstScale = remember { Animatable(1f) }
    val burstAlpha = remember { Animatable(0f) }

    // Escala elástica del botón principal al pulsar
    val buttonScale by animateFloatAsState(
        targetValue = if (isPlaying) pulseScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    Box(
        modifier = modifier.size(size * 1.5f),
        contentAlignment = Alignment.Center
    ) {
        // Halo de choque expansivo (Glow Shockwave)
        if (burstAlpha.value > 0.02f) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(burstScale.value)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = burstAlpha.value))
            )
        }

        // Anillo de luz ambiental trasero
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(size * 1.15f)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Botón principal
        Box(
            modifier = Modifier
                .size(size)
                .scale(buttonScale)
                .shadow(
                    elevation = if (isPlaying) 18.dp else 10.dp,
                    shape = CircleShape,
                    spotColor = primaryColor,
                    ambientColor = secondaryColor
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(primaryColor, secondaryColor)
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    // Disparar onda de choque visual
                    coroutineScope.launch {
                        burstScale.snapTo(1f)
                        burstAlpha.snapTo(0.55f)
                        burstScale.animateTo(1.65f, tween(420, easing = LinearEasing))
                    }
                    coroutineScope.launch {
                        burstAlpha.animateTo(0f, tween(420, easing = LinearEasing))
                    }
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            // Transición animada del icono (PlayArrow <-> Pause) con rotación y escala
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = 0.6f)) + fadeIn(tween(180)))
                        .togetherWith(scaleOut(targetScale = 0.5f, animationSpec = tween(120)) + fadeOut(tween(120)))
                },
                label = "playPauseIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pausar" else "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
