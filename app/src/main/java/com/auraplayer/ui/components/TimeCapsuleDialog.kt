package com.auraplayer.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
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

@Composable
fun TimeCapsuleDialog(
    songs: List<MediaModel>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dave_time_capsule", Context.MODE_PRIVATE) }
    var isCapsuleLocked by remember { mutableStateOf(prefs.getBoolean("is_locked", false)) }
    val unlockDateMs = remember { prefs.getLong("unlock_date", 0L) }
    var selectedDurationDays by remember { mutableIntStateOf(30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cápsula del Tiempo ⏳", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (isCapsuleLocked) {
                    val daysLeft = ((unlockDateMs - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("¡Cápsula Sellada!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Se abrirá en $daysLeft días para revivir tus canciones favoritas.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }
                } else {
                    Text(
                        "Guarda una selección de tus canciones actuales bajo llave para redescubrirlas en el futuro:",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(30 to "1 Mes", 90 to "3 Meses", 365 to "1 Año").forEach { (days, label) ->
                            FilterChip(
                                selected = selectedDurationDays == days,
                                onClick = { selectedDurationDays = days },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFD700).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFFFFD700)
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isCapsuleLocked) {
                Button(
                    onClick = {
                        val target = System.currentTimeMillis() + (selectedDurationDays.toLong() * 24 * 60 * 60 * 1000)
                        prefs.edit()
                            .putBoolean("is_locked", true)
                            .putLong("unlock_date", target)
                            .apply()
                        isCapsuleLocked = true
                        Toast.makeText(context, "⏳ Cápsula sellada por $selectedDurationDays días", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔒 Sellar Cápsula", fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(
                    onClick = {
                        prefs.edit().clear().apply()
                        isCapsuleLocked = false
                        Toast.makeText(context, "Cápsula reabierta", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("Abrir Ahora")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))) {
                Text("Cerrar")
            }
        }
    )
}
