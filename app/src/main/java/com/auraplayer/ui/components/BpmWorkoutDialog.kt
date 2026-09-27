package com.auraplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.BpmDetector
import com.auraplayer.data.model.MediaModel

@Composable
fun BpmWorkoutDialog(
    songs: List<MediaModel>,
    onPlayPlaylist: (List<MediaModel>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val groups = remember { BpmDetector.classifyLibrary(songs, context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Playlists por Ritmo BPM 🏃", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "DaVE analizó el tempo (BPM) de tus pistas y las organizó para tus entrenamientos:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    items(groups) { group ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (group.songs.isNotEmpty()) {
                                        onPlayPlaylist(group.songs)
                                        onDismiss()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(group.icon, fontSize = 28.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(group.category, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${group.minBpm}–${group.maxBpm} BPM • ${group.songs.size} canciones", color = Color(0xFF00F0FF), fontSize = 12.sp)
                                        Text(group.description, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (group.songs.isNotEmpty()) {
                                            onPlayPlaylist(group.songs)
                                            onDismiss()
                                        }
                                    },
                                    enabled = group.songs.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("▶ Iniciar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))) {
                Text("Cerrar")
            }
        }
    )
}
