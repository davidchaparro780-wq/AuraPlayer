package com.auraplayer.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.auraplayer.data.model.MediaModel

/**
 * Tocadiscos Hi-Fi realista de alta fidelidad con aguja fonocaptora animada (Tonearm).
 * La aguja desciende suavemente sobre el surco del vinilo al reproducir y se levanta al pausar.
 */
@Composable
fun VinylTurntableSkin(
    song: MediaModel,
    isPlaying: Boolean,
    vinylRotation: Float,
    accentColor: Color,
    onTap: () -> Unit,
    onDoubleTap: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    // Ángulo de la aguja: reposo (-28°) vs sobre el vinilo (+18°)
    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 18f else -28f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "tonearm_angle"
    )

    // Chasis exterior del tocadiscos
    Box(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .aspectRatio(1f)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = accentColor.copy(alpha = 0.35f),
                ambientColor = Color(0xFF090D18)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E2638),
                        Color(0xFF111624),
                        Color(0xFF0A0D16)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0x55FFFFFF),
                        Color(0x11FFFFFF),
                        accentColor.copy(alpha = 0.4f),
                        Color(0x22FFFFFF)
                    )
                ),
                RoundedCornerShape(32.dp)
            )
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Plato metálico circular de soporte (Platter)
        Box(
            modifier = Modifier
                .fillMaxSize(0.94f)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFF2A3245),
                            Color(0xFF181F30),
                            Color(0xFF2A3245),
                            Color(0xFF151C2C),
                            Color(0xFF2A3245)
                        )
                    )
                )
                .border(2.dp, Color(0xFF38435C), CircleShape)
        )

        // El Vinilo que gira
        Box(
            modifier = Modifier
                .fillMaxSize(0.86f)
                .align(Alignment.CenterStart)
                .shadow(12.dp, CircleShape, spotColor = Color.Black)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF202738),
                            Color(0xFF121624),
                            Color(0xFF070912)
                        )
                    )
                )
                .border(
                    2.dp,
                    Brush.sweepGradient(
                        listOf(
                            accentColor.copy(alpha = 0.6f),
                            Color(0xFF38BDF8),
                            Color(0xFFEC4899),
                            accentColor.copy(alpha = 0.6f)
                        )
                    ),
                    CircleShape
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { offset -> onDoubleTap(offset) }
                    )
                }
                .graphicsLayer {
                    rotationZ = vinylRotation
                },
            contentAlignment = Alignment.Center
        ) {
            // Surcos concéntricos del vinilo
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = size.minDimension / 16f
                for (i in 3..7) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = step * i,
                        style = Stroke(width = 1.2f)
                    )
                }
                // Reflejo de luz especular en cruz (típico de vinilos giratorios)
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.minDimension * 0.45f
                drawLine(
                    color = Color.White.copy(alpha = 0.07f),
                    start = Offset(cx - r, cy - r),
                    end = Offset(cx + r, cy + r),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.07f),
                    start = Offset(cx - r, cy + r),
                    end = Offset(cx + r, cy - r),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )
            }

            // Etiqueta central con la carátula del álbum
            if (song.artworkUri != null) {
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = "Carátula",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize(0.55f)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(54.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Orificio central del eje (Spindle hole)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A0D16))
                    .border(1.5.dp, Color(0xFFC0C7D8), CircleShape)
            )
        }

        // Brazo y Aguja Fonocaptora (Tonearm) superpuesto en la esquina superior derecha
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val pivotX = size.width * 0.88f
            val pivotY = size.height * 0.18f

            // 1. Base giratoria del brazo (Pivot Gimbal)
            drawCircle(
                color = Color(0xFF0F1422),
                radius = 24.dp.toPx(),
                center = Offset(pivotX, pivotY)
            )
            drawCircle(
                color = Color(0xFF455068),
                radius = 24.dp.toPx(),
                center = Offset(pivotX, pivotY),
                style = Stroke(width = 2.dp.toPx())
            )
            // Contrapeso cromado posterior
            drawCircle(
                color = Color(0xFF8895B3),
                radius = 14.dp.toPx(),
                center = Offset(pivotX + 6.dp.toPx(), pivotY - 6.dp.toPx())
            )

            // Rotamos el dibujo del brazo según tonearmAngle alrededor de (pivotX, pivotY)
            rotate(degrees = tonearmAngle, pivot = Offset(pivotX, pivotY)) {
                val armLength = size.height * 0.58f

                // Varilla principal del brazo en aluminio pulido
                val armPath = Path().apply {
                    moveTo(pivotX, pivotY)
                    // Curva suave clásica de brazo en S
                    cubicTo(
                        pivotX - 25.dp.toPx(), pivotY + armLength * 0.35f,
                        pivotX - 10.dp.toPx(), pivotY + armLength * 0.70f,
                        pivotX - 35.dp.toPx(), pivotY + armLength
                    )
                }

                // Sombra del brazo
                drawPath(
                    path = armPath,
                    color = Color.Black.copy(alpha = 0.45f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Tubo cromado del brazo
                drawPath(
                    path = armPath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFFCBD5E1))
                    ),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // Cápsula fonocaptora y cabezal (Headshell)
                val headX = pivotX - 35.dp.toPx()
                val headY = pivotY + armLength

                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(headX - 10.dp.toPx(), headY),
                    size = androidx.compose.ui.geometry.Size(20.dp.toPx(), 32.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Fill
                )
                // Detalle dorado en la cápsula
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = 3.dp.toPx(),
                    center = Offset(headX, headY + 28.dp.toPx())
                )

                // Luz LED indicadora de estado en la aguja (turquesa al reproducir)
                if (isPlaying) {
                    drawCircle(
                        color = Color(0xFF00F0FF),
                        radius = 2.5.dp.toPx(),
                        center = Offset(headX, headY + 14.dp.toPx())
                    )
                }
            }
        }
    }
}
