package com.auraplayer.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.auraplayer.audio.AuraHaptic
import com.auraplayer.audio.RealtimeVisualizerManager
import com.auraplayer.audio.StemMixerManager

/**
 * Diálogo de Estudio Mezclador de Stems en Vivo (Live AI Stem Mixer Dialog).
 * Permite aislar o mutear Vocales (Karaoke/Acapella), Batería, Bajo y Melodía con faders independientes.
 */
@Composable
fun StemMixerDialog(
    isPlaying: Boolean,
    stemManager: StemMixerManager,
    onDismiss: () -> Unit
) {
    val view = LocalView.current

    var isEnabled by remember { mutableStateOf(stemManager.isEnabled) }
    var vocalGain by remember { mutableFloatStateOf(stemManager.vocalGain) }
    var drumsGain by remember { mutableFloatStateOf(stemManager.drumsGain) }
    var bassGain by remember { mutableFloatStateOf(stemManager.bassGain) }
    var instrumentsGain by remember { mutableFloatStateOf(stemManager.instrumentsGain) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6), Color(0xFFEC4899))
                    ),
                    RoundedCornerShape(26.dp)
                ),
            color = Color(0xFF0C101D)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Cabecera del Estudio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AI Stem Mixer Studio",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Aislador de Pistas en Tiempo Real",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Switch de activación y Reset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161E31))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = {
                                AuraHaptic.click(view)
                                isEnabled = it
                                stemManager.isEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF38BDF8),
                                checkedTrackColor = Color(0xFF38BDF8).copy(alpha = 0.4f)
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isEnabled) "MEZCLADOR ACTIVO" else "MODO ESTÁNDAR",
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) Color(0xFF38BDF8) else Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            AuraHaptic.click(view)
                            stemManager.resetToDefault()
                            isEnabled = false
                            vocalGain = 1.0f
                            drumsGain = 1.0f
                            bassGain = 1.0f
                            instrumentsGain = 1.0f
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reiniciar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets Rápidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PresetChip(
                        title = "🎤 Karaoke",
                        subtitle = "Sin Voz",
                        accentColor = Color(0xFFEC4899),
                        onClick = {
                            AuraHaptic.heavy(view)
                            stemManager.setKaraokePreset()
                            isEnabled = true
                            vocalGain = stemManager.vocalGain
                            drumsGain = stemManager.drumsGain
                            bassGain = stemManager.bassGain
                            instrumentsGain = stemManager.instrumentsGain
                        },
                        modifier = Modifier.weight(1f)
                    )

                    PresetChip(
                        title = "🎙️ Acapella",
                        subtitle = "Solo Voz",
                        accentColor = Color(0xFF38BDF8),
                        onClick = {
                            AuraHaptic.heavy(view)
                            stemManager.setAcapellaPreset()
                            isEnabled = true
                            vocalGain = stemManager.vocalGain
                            drumsGain = stemManager.drumsGain
                            bassGain = stemManager.bassGain
                            instrumentsGain = stemManager.instrumentsGain
                        },
                        modifier = Modifier.weight(1f)
                    )

                    PresetChip(
                        title = "🔊 808 Bass",
                        subtitle = "Bajo Club",
                        accentColor = Color(0xFF10B981),
                        onClick = {
                            AuraHaptic.heavy(view)
                            stemManager.setBassBoostedPreset()
                            isEnabled = true
                            vocalGain = stemManager.vocalGain
                            drumsGain = stemManager.drumsGain
                            bassGain = stemManager.bassGain
                            instrumentsGain = stemManager.instrumentsGain
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4 Faders de Estudio
                StemFaderRow(
                    name = "Vocales / Voz",
                    icon = Icons.Default.Mic,
                    value = vocalGain,
                    accentColor = Color(0xFFEC4899),
                    bandEnergy = if (isPlaying && isEnabled) RealtimeVisualizerManager.instance.getBand(16, 32, isPlaying) else 0.05f,
                    onValueChange = {
                        vocalGain = it
                        stemManager.vocalGain = it
                        if (!isEnabled) {
                            isEnabled = true
                            stemManager.isEnabled = true
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                StemFaderRow(
                    name = "Batería / Drums",
                    icon = Icons.Default.GraphicEq,
                    value = drumsGain,
                    accentColor = Color(0xFFF59E0B),
                    bandEnergy = if (isPlaying && isEnabled) RealtimeVisualizerManager.instance.getBand(24, 32, isPlaying) else 0.05f,
                    onValueChange = {
                        drumsGain = it
                        stemManager.drumsGain = it
                        if (!isEnabled) {
                            isEnabled = true
                            stemManager.isEnabled = true
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                StemFaderRow(
                    name = "Línea de Bajo / Sub",
                    icon = Icons.Default.Speaker,
                    value = bassGain,
                    accentColor = Color(0xFF10B981),
                    bandEnergy = if (isPlaying && isEnabled) RealtimeVisualizerManager.instance.getBassEnergy(isPlaying) else 0.05f,
                    onValueChange = {
                        bassGain = it
                        stemManager.bassGain = it
                        if (!isEnabled) {
                            isEnabled = true
                            stemManager.isEnabled = true
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                StemFaderRow(
                    name = "Melodía & Sintetizadores",
                    icon = Icons.Default.MusicNote,
                    value = instrumentsGain,
                    accentColor = Color(0xFF38BDF8),
                    bandEnergy = if (isPlaying && isEnabled) RealtimeVisualizerManager.instance.getBand(8, 32, isPlaying) else 0.05f,
                    onValueChange = {
                        instrumentsGain = it
                        stemManager.instrumentsGain = it
                        if (!isEnabled) {
                            isEnabled = true
                            stemManager.isEnabled = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 12.sp
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun StemFaderRow(
    name: String,
    icon: ImageVector,
    value: Float,
    accentColor: Color,
    bandEnergy: Float,
    onValueChange: (Float) -> Unit
) {
    val animatedVu by animateFloatAsState(targetValue = bandEnergy.coerceIn(0.05f, 1.0f), label = "vu")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131A2B))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }

            // VU Meter indicador de nivel
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedVu)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(accentColor)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(value * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 12.sp
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..2f,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(0xFF1E293B)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
