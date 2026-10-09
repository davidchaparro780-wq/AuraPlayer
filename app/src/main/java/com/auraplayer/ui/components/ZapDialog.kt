package com.auraplayer.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.ZapPreviewManager

/**
 * Diálogo de Control del Modo ZAP (DJ Escucha Rápida).
 */
@Composable
fun ZapDialog(
    zapManager: ZapPreviewManager,
    isCurrentlyActive: Boolean,
    onStartZap: (Int) -> Unit,
    onStopZap: () -> Unit,
    onDismiss: () -> Unit
) {
    var durationSec by remember { mutableFloatStateOf(zapManager.previewDurationSeconds.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111827),
        shape = RoundedCornerShape(24.dp),
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
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Modo ZAP (DJ)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF9CA3AF))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Salta automáticamente por tu biblioteca reproduciendo solo los mejores segundos de cada canción para descubrir temas al instante.",
                    color = Color(0xFF9CA3AF),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Duración por canción:", color = Color.White, fontSize = 14.sp)
                    Text(
                        text = "${durationSec.toInt()} segundos",
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Slider(
                    value = durationSec,
                    onValueChange = { durationSec = it },
                    valueRange = 8f..25f,
                    steps = 16,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFF59E0B),
                        activeTrackColor = Color(0xFFF59E0B),
                        inactiveTrackColor = Color(0xFF374151)
                    )
                )

                if (isCurrentlyActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ ZAP ACTIVO: Saltando de tema en tema...",
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    zapManager.previewDurationSeconds = durationSec.toInt()
                    if (isCurrentlyActive) {
                        onStopZap()
                    } else {
                        onStartZap(durationSec.toInt())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentlyActive) Color(0xFFEF4444) else Color(0xFFF59E0B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isCurrentlyActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isCurrentlyActive) Color.White else Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isCurrentlyActive) "Detener ZAP" else "Activar ZAP",
                    color = if (isCurrentlyActive) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
