package com.auraplayer.ui.components

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.PartyLinkManager
import com.auraplayer.data.model.MediaModel

/**
 * Diálogo de Silent Disco / Party Link (Sincronización Multi-Teléfono por Wi-Fi).
 */
@Composable
fun PartyLinkDialog(
    partyManager: PartyLinkManager,
    currentMedia: MediaModel?,
    currentPosMs: Long,
    isPlaying: Boolean,
    onSyncPlay: (title: String, posMs: Long, isPlaying: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                            .background(Color(0xFF8B5CF6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Silent Disco (Party Link)",
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Sincroniza múltiples teléfonos en la misma red Wi-Fi para reproducir la misma canción al milisegundo exacto como un sistema de altavoces conectado.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                if (partyManager.isHosting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "👑 TRANSMITIENDO COMO ANFITRIÓN",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "IP Local: ${partyManager.hostIp} • Puerto: 8844",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else if (partyManager.isJoined) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "🎧 CONECTADO A LA SALA: Sincronizando playback...",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (partyManager.isHosting) {
                                partyManager.stopAll()
                                Toast.makeText(context, "Transmisión detenida", Toast.LENGTH_SHORT).show()
                            } else {
                                partyManager.startHosting(
                                    scope = scope,
                                    currentTitle = currentMedia?.title ?: "Música",
                                    currentPosMs = currentPosMs,
                                    isPlaying = isPlaying
                                )
                                Toast.makeText(context, "Sala creada como Anfitrión", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (partyManager.isHosting) Color(0xFFEF4444) else Color(0xFF8B5CF6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (partyManager.isHosting) "Parar Sala" else "Crear Sala",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (partyManager.isJoined) {
                                partyManager.stopAll()
                                Toast.makeText(context, "Desconectado de la sala", Toast.LENGTH_SHORT).show()
                            } else {
                                partyManager.joinParty(scope = scope) { title, pos, playing ->
                                    onSyncPlay(title, pos, playing)
                                }
                                Toast.makeText(context, "Escuchando salas en la red...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (partyManager.isJoined) Color(0xFFEF4444) else Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (partyManager.isJoined) "Desconectar" else "Unirse",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
