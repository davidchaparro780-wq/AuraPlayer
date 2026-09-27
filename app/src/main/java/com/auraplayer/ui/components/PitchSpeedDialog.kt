package com.auraplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import kotlin.math.pow

@Composable
fun PitchSpeedDialog(
    initialSpeed: Float = 1.0f,
    initialPitch: Float = 1.0f,
    onApply: (speed: Float, pitch: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var semitones by remember { mutableIntStateOf(0) }
    var speed by remember { mutableFloatStateOf(initialSpeed) }

    fun updatePlayback() {
        val pitchMultiplier = (2.0.pow(semitones / 12.0)).toFloat()
        onApply(speed, pitchMultiplier)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tono & Velocidad 🎚️", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Adapta el tono para cantar o practicar, o cambia la velocidad de la pista:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                // Pitch Semitones Slider
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cambio de Tono", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(if (semitones > 0) "+$semitones semitonos" else "$semitones semitonos", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = semitones.toFloat(),
                            onValueChange = {
                                semitones = it.toInt()
                                updatePlayback()
                            },
                            valueRange = -6f..6f,
                            steps = 11,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                        )
                    }
                }

                // Speed Slider
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Velocidad de Reproducción", color = Color(0xFFEC4899), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(String.format(java.util.Locale.US, "%.2fx", speed), color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = speed,
                            onValueChange = {
                                speed = (Math.round(it * 20) / 20f).coerceIn(0.5f, 2.0f)
                                updatePlayback()
                            },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFEC4899), activeTrackColor = Color(0xFFEC4899))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Listo", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    semitones = 0
                    speed = 1.0f
                    onApply(1.0f, 1.0f)
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
            ) {
                Text("Restablecer")
            }
        }
    )
}
