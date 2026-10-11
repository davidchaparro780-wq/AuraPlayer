package com.auraplayer.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.AuraHaptic
import com.auraplayer.audio.BoosterProfile
import com.auraplayer.audio.VolumeBoosterManager

@Composable
fun VolumeBoosterDialog(
    onDismiss: () -> Unit
) {
    val booster = VolumeBoosterManager.instance
    val boostPercent by booster.boostPercent.collectAsState()
    val activeProfile by booster.activeProfile.collectAsState()
    val view = LocalView.current

    val boostColor by animateColorAsState(
        targetValue = when {
            boostPercent > 175 -> Color(0xFFEF4444) // Intense Red
            boostPercent > 140 -> Color(0xFFF97316) // Energetic Orange
            boostPercent > 115 -> Color(0xFFEAB308) // Warm Yellow
            else -> Color(0xFF10B981)              // Clean Emerald
        },
        label = "boostColor"
    )

    val boostGradient = Brush.horizontalGradient(
        listOf(boostColor, Color(0xFFA855F7))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
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
                            .background(boostColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = boostColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Super Booster 200%",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Amplificación DSP Nativa",
                            color = Color(0xFF94A3B8),
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicador visual gigante de porcentaje
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(3.dp, boostColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$boostPercent%",
                            color = boostColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (boostPercent == 100) "Normal" else "+${boostPercent - 100}% extra",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Slider suave de 100% a 200%
                Slider(
                    value = boostPercent.toFloat(),
                    onValueChange = { newValue ->
                        val target = newValue.toInt()
                        if (target != boostPercent) {
                            if (target % 10 == 0) AuraHaptic.click(view)
                            booster.setBoostPercent(target)
                        }
                    },
                    valueRange = 100f..200f,
                    colors = SliderDefaults.colors(
                        thumbColor = boostColor,
                        activeTrackColor = boostColor,
                        inactiveTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("100% Normal", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text("150%", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text("200% MAX", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de Perfiles Rápidos
                Text(
                    text = "PERFILES RÁPIDOS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                val profiles = listOf(
                    BoosterProfile.NORMAL,
                    BoosterProfile.BALANCED,
                    BoosterProfile.HIGH,
                    BoosterProfile.SUPER_MAX
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    profiles.forEach { prof ->
                        val isSelected = activeProfile == prof
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) boostColor else Color(0xFF1E293B))
                                .clickable {
                                    AuraHaptic.click(view)
                                    booster.applyProfile(prof)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${prof.boostPercent}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botones de efectos especiales: Cine 3D y Modo Calle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cine 3D Surround
                    val isCinema = activeProfile == BoosterProfile.CINEMA_3D
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCinema) Color(0xFF8B5CF6) else Color(0xFF1E293B))
                            .border(1.dp, if (isCinema) Color(0xFFA78BFA) else Color(0xFF334155), RoundedCornerShape(12.dp))
                            .clickable {
                                AuraHaptic.click(view)
                                booster.applyProfile(BoosterProfile.CINEMA_3D)
                            }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🍿 Cine 3D",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCinema) Color.White else Color(0xFFCBD5E1)
                        )
                    }

                    // Modo Calle
                    val isStreet = activeProfile == BoosterProfile.STREET_LOUD
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isStreet) Color(0xFFEA580C) else Color(0xFF1E293B))
                            .border(1.dp, if (isStreet) Color(0xFFFB923C) else Color(0xFF334155), RoundedCornerShape(12.dp))
                            .clickable {
                                AuraHaptic.click(view)
                                booster.applyProfile(BoosterProfile.STREET_LOUD)
                            }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📢 Modo Calle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStreet) Color.White else Color(0xFFCBD5E1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Badge de protección
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Con limitador dinámico para proteger altavoces y oídos.",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        },
        confirmButton = {}
    )
}
