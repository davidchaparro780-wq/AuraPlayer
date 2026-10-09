package com.auraplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.model.MediaModel

/**
 * Skin Retro Vintage: Cassette Tape Interactivo (Años 80/90).
 * Simula un casete de audio físico con bobinas giratorias mecánicas en tiempo real
 * y trasvase de cinta magnética proporcional a la duración de la canción.
 */
@Composable
fun CassetteTapeSkin(
    song: MediaModel?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    val infiniteTransition = rememberInfiniteTransition(label = "cassette_spool")
    val spoolAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spool_rotation"
    )

    val currentSpoolAngle = if (isPlaying) spoolAngle else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E232E), Color(0xFF141720), Color(0xFF0F121A))
                )
            )
            .border(2.dp, Color(0xFF333B4D), RoundedCornerShape(20.dp))
            .clickable { onTogglePlay() }
            .padding(14.dp)
    ) {
        // Tornillos en las cuatro esquinas
        CornerScrew(Modifier.align(Alignment.TopStart))
        CornerScrew(Modifier.align(Alignment.TopEnd))
        CornerScrew(Modifier.align(Alignment.BottomStart))
        CornerScrew(Modifier.align(Alignment.BottomEnd))

        // Etiqueta central vintage (Label)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEDE8D0)) // Papel crema vintage
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Cabecera de la etiqueta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DaVE C-90",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = Color(0xFF991B1B)
                )
                Text(
                    text = "SIDE A • HI-FI STEREO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "NR [B]",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color(0xFF0369A1)
                )
            }

            // Nombre de la canción rotulado
            Text(
                text = song?.title ?: "Sin reproducción",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = Color(0xFF111827),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Ventana central transparente con bobinas y cinta magnética
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.5.dp, Color(0xFF475569), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bobina izquierda (con cinta restante)
                    CassetteSpool(
                        angle = currentSpoolAngle,
                        tapeThicknessRatio = 1.0f - (progress * 0.65f)
                    )

                    // Cinta magnética central
                    Canvas(modifier = Modifier.size(width = 60.dp, height = 8.dp)) {
                        drawLine(
                            color = Color(0xFF332018),
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 5f
                        )
                    }

                    // Bobina derecha (con cinta acumulada)
                    CassetteSpool(
                        angle = currentSpoolAngle,
                        tapeThicknessRatio = 0.35f + (progress * 0.65f)
                    )
                }
            }

            // Pie de etiqueta con artista
            Text(
                text = song?.artist ?: "DaVE Player Retro",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CornerScrew(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(Color(0xFF475569))
            .border(1.dp, Color(0xFF1E293B), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp, 1.dp)
                .background(Color(0xFF0F172A))
        )
    }
}

@Composable
private fun CassetteSpool(
    angle: Float,
    tapeThicknessRatio: Float
) {
    Box(
        modifier = Modifier.size(50.dp),
        contentAlignment = Alignment.Center
    ) {
        // Capa exterior de cinta magnética marrón
        Canvas(modifier = Modifier.size((28 + (16 * tapeThicknessRatio)).dp)) {
            drawCircle(
                color = Color(0xFF3B2317) // Color cinta de óxido de hierro
            )
        }

        // Carrete plástico blanco dentado que gira
        Box(
            modifier = Modifier
                .size(24.dp)
                .rotate(angle)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color(0xFFCBD5E1), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                drawCircle(color = Color(0xFF0F172A), radius = radius * 0.4f)
                // Dientes de la rueda dentada
                for (i in 0 until 6) {
                    val a = Math.toRadians((i * 60).toDouble())
                    val x = (size.width / 2) + (radius * 0.65f * Math.cos(a)).toFloat()
                    val y = (size.height / 2) + (radius * 0.65f * Math.sin(a)).toFloat()
                    drawCircle(color = Color(0xFF0F172A), radius = 2.5f, center = Offset(x, y))
                }
            }
        }
    }
}
