package com.auraplayer.ui.components

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.model.MediaModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneMakerDialog(
    song: MediaModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val totalSec = (song.duration / 1000).toFloat().coerceAtLeast(30f)
    var startSec by remember { mutableFloatStateOf(0f) }
    var endSec by remember { mutableFloatStateOf(30f.coerceAtMost(totalSec)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Creador de Tonos ✂️", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Selecciona el fragmento de ${song.title} para tu Tono de Llamada o Alarma:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Inicio: ${startSec.toInt()}s", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            Text("Fin: ${endSec.toInt()}s", color = Color(0xFFEC4899), fontWeight = FontWeight.Bold)
                            Text("Duración: ${(endSec - startSec).toInt()}s", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                        }

                        RangeSlider(
                            value = startSec..endSec,
                            onValueChange = { range ->
                                startSec = range.start
                                endSec = range.endInclusive
                            },
                            valueRange = 0f..totalSec,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00F0FF),
                                activeTrackColor = Color(0xFF00F0FF),
                                inactiveTrackColor = Color(0xFF334155)
                            )
                        )
                    }
                }

                Text(
                    "💡 Podrás establecerlo como Tono de Llamada oficial de tu teléfono.",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        RingtoneManager.setActualDefaultRingtoneUri(
                            context,
                            RingtoneManager.TYPE_RINGTONE,
                            song.uri
                        )
                        Toast.makeText(context, "🔔 Tono de llamada actualizado a ${song.title}", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Se requiere permiso de Ajustes de Sistema para cambiar tono", Toast.LENGTH_LONG).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Fijar como Tono", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))) {
                Text("Cancelar")
            }
        }
    )
}
