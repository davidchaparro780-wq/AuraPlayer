package com.auraplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.BinauralMode
import com.auraplayer.audio.BinauralNoiseManager

@Composable
fun BinauralNoiseDialog(
    onDismiss: () -> Unit
) {
    val manager = remember { BinauralNoiseManager.instance }
    var activeMode by remember { mutableStateOf(manager.currentMode) }
    var volume by remember { mutableFloatStateOf(manager.volume) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ondas Binaurales & Ruido 🧠", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Frecuencias que suenan junto a tu música para estudiar, concentrarte o dormir:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                // Mode Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BinauralMode.values().forEach { mode ->
                        val isSelected = activeMode == mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected) Color(0xFF00F0FF).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    activeMode = mode
                                    manager.setMode(mode)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    mode.title,
                                    color = if (isSelected) Color(0xFF00F0FF) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(mode.desc, color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    activeMode = mode
                                    manager.setMode(mode)
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00F0FF))
                            )
                        }
                    }
                }

                if (activeMode != BinauralMode.OFF) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Volumen de las ondas", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text("${(volume * 100).toInt()}%", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = volume,
                            onValueChange = {
                                volume = it
                                manager.volume = it
                            },
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00F0FF), activeTrackColor = Color(0xFF00F0FF))
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
        }
    )
}
