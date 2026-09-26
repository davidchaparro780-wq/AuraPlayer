package com.auraplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.MusicAlarm
import com.auraplayer.audio.MusicAlarmManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSetupDialog(
    alarmManager: MusicAlarmManager,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(7) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var alarmLabel by remember { mutableStateOf("Despertar con música 🎵") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null,
                    tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("🌅 Alarma Musical", fontWeight = FontWeight.ExtraBold,
                    color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                Text("DaVE te despertará suavemente subiendo\nel volumen de tu música durante 5 minutos.",
                    color = Color(0xFF94A3B8), fontSize = 13.sp)

                // Hour picker
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2035)),
                    shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("⏰ Hora", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()) {
                            // Hour
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                                    Text("▲", color = Color(0xFF00F0FF), fontSize = 20.sp)
                                }
                                Text(String.format("%02d", selectedHour),
                                    color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                                IconButton(onClick = { selectedHour = (selectedHour - 1 + 24) % 24 }) {
                                    Text("▼", color = Color(0xFF00F0FF), fontSize = 20.sp)
                                }
                            }
                            Text(":", color = Color(0xFFFFD700), fontSize = 40.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp))
                            // Minute
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { selectedMinute = (selectedMinute + 5) % 60 }) {
                                    Text("▲", color = Color(0xFF00F0FF), fontSize = 20.sp)
                                }
                                Text(String.format("%02d", selectedMinute),
                                    color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                                IconButton(onClick = { selectedMinute = (selectedMinute - 5 + 60) % 60 }) {
                                    Text("▼", color = Color(0xFF00F0FF), fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }

                // Label
                OutlinedTextField(
                    value = alarmLabel,
                    onValueChange = { alarmLabel = it },
                    label = { Text("Etiqueta", color = Color(0xFF94A3B8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00F0FF),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Preview
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF6C63FF).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null,
                            tint = Color(0xFF6C63FF), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sonará a las ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                            color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val alarm = MusicAlarm(
                            id = System.currentTimeMillis().toInt() and 0xFFFF,
                            hour = selectedHour,
                            minute = selectedMinute,
                            label = alarmLabel
                        )
                        alarmManager.setAlarm(alarm)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⏰ Activar Alarma", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            }
        },
        dismissButton = {}
    )
}
