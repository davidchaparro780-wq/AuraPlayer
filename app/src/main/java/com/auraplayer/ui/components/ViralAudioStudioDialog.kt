package com.auraplayer.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.AuraHaptic
import com.auraplayer.audio.EqualizerManager
import kotlin.math.pow

/**
 * Estudio de Efectos Virales en Vivo: Slowed+Reverb, Sped Up, 8D Espacial y Lo-Fi.
 */
@Composable
fun ViralAudioStudioDialog(
    initialSpeed: Float = 1.0f,
    initialPitch: Float = 1.0f,
    onApplyParams: (speed: Float, pitch: Float) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val eqManager = remember { EqualizerManager.instance }

    var currentSpeed by remember { mutableFloatStateOf(initialSpeed) }
    var currentSemitones by remember { mutableIntStateOf(0) }
    var activePreset by remember { mutableFloatStateOf(1f) } // 0: Slowed, 1: Normal, 2: SpedUp, 3: 8D, 4: LoFi

    fun applyPlayback(speed: Float, semitones: Int) {
        currentSpeed = speed
        currentSemitones = semitones
        val pitchMultiplier = (2.0.pow(semitones / 12.0)).toFloat()
        onApplyParams(speed, pitchMultiplier)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C1022),
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Estudio Viral FX 🎛️",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Slowed, Sped Up, 8D Audio & Lo-Fi",
                            color = Color(0xFFE879F9),
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Aplica efectos virales en tiempo real a cualquier canción:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )

                // Grid de Presets Rápidos
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 🌙 Slowed + Reverb
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E1B4B))
                            .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable {
                                AuraHaptic.click(view)
                                eqManager.setSpatial8DEnabled(true)
                                applyPlayback(0.85f, -2)
                                Toast.makeText(context, "🌙 Modo Slowed + Reverb Activo", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Nightlight, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("Slowed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("+ Reverb", color = Color(0xFFA78BFA), fontSize = 10.sp)
                        }
                    }

                    // ⚡ Sped Up
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF431407))
                            .border(1.dp, Color(0xFFF97316).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable {
                                AuraHaptic.click(view)
                                eqManager.setSpatial8DEnabled(false)
                                applyPlayback(1.25f, 3)
                                Toast.makeText(context, "⚡ Modo Sped Up & Nightcore Activo", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFFFB923C), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("Sped Up", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Nightcore", color = Color(0xFFFB923C), fontSize = 10.sp)
                        }
                    }

                    // 🌀 8D Audio
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF064E3B))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable {
                                AuraHaptic.click(view)
                                eqManager.setSpatial8DEnabled(true)
                                applyPlayback(1.0f, 0)
                                Toast.makeText(context, "🌀 Audio 8D Espacial Activo (Ponte audífonos)", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("8D Audio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("360° Surround", color = Color(0xFF34D399), fontSize = 10.sp)
                        }
                    }
                }

                // Fila 2: Lo-Fi y Restablecer
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // ☕ Lo-Fi
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF2E1065))
                            .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable {
                                AuraHaptic.click(view)
                                eqManager.setSpatial8DEnabled(false)
                                applyPlayback(0.92f, -1)
                                Toast.makeText(context, "☕ Modo Lo-Fi Chill Activo", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Coffee, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Lo-Fi Chill Beats", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    // 🔄 Original / Reset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF475569), RoundedCornerShape(14.dp))
                            .clickable {
                                AuraHaptic.click(view)
                                eqManager.setSpatial8DEnabled(false)
                                applyPlayback(1.0f, 0)
                                Toast.makeText(context, "Pista original restaurada", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Original (1.0x)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Sliders Manuales de Precisión
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13182E)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Velocidad de Reproducción", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(String.format("%.2fx", currentSpeed), color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = currentSpeed,
                            onValueChange = { sp ->
                                applyPlayback(sp, currentSemitones)
                            },
                            valueRange = 0.5f..1.75f,
                            steps = 24,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF00F0FF), activeTrackColor = Color(0xFF00F0FF))
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tono Musical (Pitch)", color = Color(0xFFE879F9), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(if (currentSemitones > 0) "+$currentSemitones st" else "$currentSemitones st", color = Color(0xFFE879F9), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = currentSemitones.toFloat(),
                            onValueChange = { st ->
                                applyPlayback(currentSpeed, st.toInt())
                            },
                            valueRange = -6f..6f,
                            steps = 11,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFE879F9), activeTrackColor = Color(0xFFE879F9))
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
