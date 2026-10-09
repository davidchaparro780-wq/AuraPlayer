package com.auraplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.AudioQualityInspector
import com.auraplayer.audio.AudioQualityReport
import com.auraplayer.data.model.MediaModel

/**
 * Diálogo de Inspector de Calidad Real de Audio (Fake Hi-Res Detector).
 */
@Composable
fun AudioQualityDialog(
    song: MediaModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var report by remember { mutableStateOf<AudioQualityReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(song.id) {
        isLoading = true
        report = AudioQualityInspector.inspectQuality(context, song)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
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
                            .background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Inspector de Calidad",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF94A3B8))
                }
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            } else if (report != null) {
                val rep = report!!
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Badge destacado
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (rep.isHiResGenuine) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF3B82F6).copy(alpha = 0.2f))
                            .border(1.dp, if (rep.isHiResGenuine) Color(0xFF10B981) else Color(0xFF3B82F6), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rep.badgeLabel,
                            fontWeight = FontWeight.Bold,
                            color = if (rep.isHiResGenuine) Color(0xFF10B981) else Color(0xFF60A5FA),
                            fontSize = 15.sp
                        )
                    }

                    // Fila Formato & Bitrate
                    InfoRow(label = "Formato de Audio", value = rep.format)
                    InfoRow(label = "Tasa de Bits (Bitrate)", value = "${rep.bitrateKbps} kbps")
                    InfoRow(label = "Frecuencia de Muestreo", value = "${rep.sampleRateHz} Hz (${rep.sampleRateHz / 1000f} kHz)")
                    InfoRow(label = "Canales de Audio", value = if (rep.channels == 2) "Estéreo (2 canales)" else "${rep.channels} canales")

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 ${rep.technicalVerdict}",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Entendido", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
