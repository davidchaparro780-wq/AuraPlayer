package com.auraplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.EqualizerManager

@Composable
fun QuickEqDialog(
    onDismiss: () -> Unit
) {
    val eq = remember { EqualizerManager.instance }

    // Read current values
    var bassBoost by remember { mutableFloatStateOf(eq.bassStrength.toFloat()) }
    var virtualizer by remember { mutableFloatStateOf(eq.virtualizerStrength.toFloat()) }
    var loudness by remember { mutableFloatStateOf(eq.loudnessGain.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Equalizer, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ecualizador Rápido 🎚️", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Bass Boost
                EqSliderRow(
                    label = "Graves & Sub-Bajos 🔊",
                    value = bassBoost,
                    valueRange = 0f..1000f,
                    color = Color(0xFF8B5CF6),
                    onValueChange = {
                        bassBoost = it
                        eq.setBassBoostStrength(it.toInt().toShort())
                    }
                )

                // 2. Virtualizer / Soundstage
                EqSliderRow(
                    label = "Espacio Sonoro 3D 🌌",
                    value = virtualizer,
                    valueRange = 0f..1000f,
                    color = Color(0xFF00F0FF),
                    onValueChange = {
                        virtualizer = it
                        eq.setVirtualizerStrength(it.toInt().toShort())
                    }
                )

                // 3. Loudness Boost
                EqSliderRow(
                    label = "Potencia / Brillo ⚡",
                    value = loudness,
                    valueRange = 0f..1000f,
                    color = Color(0xFFFFD700),
                    onValueChange = {
                        loudness = it
                        eq.setLoudnessGain(it.toInt())
                    }
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        bassBoost = 0f
                        virtualizer = 0f
                        loudness = 0f
                        eq.setBassBoostStrength(0)
                        eq.setVirtualizerStrength(0)
                        eq.setLoudnessGain(0)
                    }
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reiniciar", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("Listo", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    )
}

@Composable
private fun EqSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            val percent = ((value / valueRange.endInclusive) * 100).toInt()
            Text("$percent%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
