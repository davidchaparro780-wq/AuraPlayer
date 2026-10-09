package com.auraplayer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class SparkParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val color: Color,
    val life: Animatable<Float, *>
)

/**
 * Lienzo Interactivo de Chispas Neón Táctiles (Interactive Neon Touch Sparks).
 * Al tocar, arrastrar o deslizar los dedos por cualquier zona de la pantalla,
 * brotan partículas luminosas de neón y polvo estelar con físicas de inercia y desvanecimiento.
 */
@Composable
fun AuraTouchSparks(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF38BDF8),
    secondaryColor: Color = Color(0xFFEC4899),
    content: @Composable () -> Unit
) {
    val sparks = remember { mutableStateListOf<SparkParticle>() }

    fun addSparks(origin: Offset, count: Int = 6) {
        val colors = listOf(accentColor, secondaryColor, Color(0xFF8B5CF6), Color.White)
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 8f + 3f
            val vx = kotlin.math.cos(angle) * speed
            val vy = kotlin.math.sin(angle) * speed
            val size = Random.nextFloat() * 4f + 2f
            val color = colors[Random.nextInt(colors.size)]
            val anim = Animatable(1f)

            val p = SparkParticle(
                id = System.nanoTime() + i,
                x = origin.x,
                y = origin.y,
                vx = vx,
                vy = vy,
                size = size,
                color = color,
                life = anim
            )
            sparks.add(p)
        }
    }

    LaunchedEffect(sparks.size) {
        if (sparks.isNotEmpty()) {
            val toRemove = mutableListOf<SparkParticle>()
            sparks.forEach { spark ->
                launch {
                    spark.life.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 650, easing = LinearEasing)
                    )
                    toRemove.add(spark)
                }
            }
            delay(50)
            if (toRemove.isNotEmpty()) {
                sparks.removeAll(toRemove)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        addSparks(offset, count = 8)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        if (Random.nextFloat() < 0.65f) {
                            addSparks(change.position, count = 3)
                        }
                    }
                )
            }
    ) {
        content()

        Canvas(modifier = Modifier.fillMaxSize()) {
            sparks.forEach { s ->
                val progress = 1f - s.life.value
                val curX = s.x + s.vx * progress * 15f
                val curY = s.y + s.vy * progress * 15f + (progress * progress * 20f) // Gravedad suave
                val curAlpha = s.life.value.coerceIn(0f, 1f)
                val curRadius = s.size * curAlpha

                if (curRadius > 0.5f && curAlpha > 0.05f) {
                    drawCircle(
                        color = s.color.copy(alpha = curAlpha),
                        radius = curRadius,
                        center = Offset(curX, curY)
                    )
                    // Halo luminoso
                    drawCircle(
                        color = s.color.copy(alpha = curAlpha * 0.4f),
                        radius = curRadius * 2.2f,
                        center = Offset(curX, curY)
                    )
                }
            }
        }
    }
}
