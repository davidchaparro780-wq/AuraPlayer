package com.auraplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import com.auraplayer.audio.AuraHaptic

/**
 * Modificador interactivo que aplica un suave efecto de rebote táctil (escala 0.94x)
 * y respuesta de físicas elásticas a cualquier componente (ej. IconButton o Card)
 * sin interferir con sus listeners de click existentes.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.94f
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 600f
        ),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
        }
}

/**
 * Sobrecarga de bounceClick que además registra el callback onClick con retroalimentación háptica.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    this
        .bounceClick(scaleDown = scaleDown)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            AuraHaptic.click(view)
            onClick()
        }
}
