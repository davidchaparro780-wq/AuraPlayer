package com.auraplayer.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CleaningServices
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
fun BatchMetadataDialog(
    songs: List<MediaModel>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val messySongs = remember(songs) {
        songs.filter { song ->
            val t = song.title.lowercase()
            t.contains("y2mate") || t.contains("official video") || t.contains("video oficial") ||
            t.contains("lyrics") || t.contains("128kbps") || t.contains("320kbps") || t.contains(".mp3")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF00F0FF), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Limpiador Inteligente 🪄", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Elimina textos basura de descargas como '[Official Video]', 'y2mate' y números de pista:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                if (messySongs.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(16.dp)) {
                        Box(Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("✨ ¡Tu biblioteca está impecable! No hay nombres con texto basura.", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    Text("Detectadas ${messySongs.size} canciones con nombres desordenados:", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        items(messySongs.take(20)) { s ->
                            val cleanTitle = cleanSongTitle(s.title)
                            Column(Modifier.fillMaxWidth().background(Color(0xFF1E293B), RoundedCornerShape(10.dp)).padding(10.dp)) {
                                Text("❌ ${s.title}", color = Color(0xFFEF4444), fontSize = 11.sp, maxLines = 1)
                                Text("➔ $cleanTitle", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (messySongs.isNotEmpty()) {
                Button(
                    onClick = {
                        Toast.makeText(context, "🧹 ${messySongs.size} títulos estilizados con éxito", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Limpiar Todo", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black), shape = RoundedCornerShape(12.dp)) {
                    Text("Listo", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))) {
                Text("Cancelar")
            }
        }
    )
}

fun cleanSongTitle(title: String): String {
    return title
        .replace(Regex("(?i)\\[official video\\]|\\(official video\\)"), "")
        .replace(Regex("(?i)\\[video oficial\\]|\\(video oficial\\)"), "")
        .replace(Regex("(?i)\\[lyrics\\]|\\(lyrics\\)|\\(letra\\)"), "")
        .replace(Regex("(?i)y2mate\\.com\\s*-\\s*"), "")
        .replace(Regex("(?i)\\.mp3|\\.m4a|\\.wav|\\.flac"), "")
        .replace(Regex("(?i)128kbps|320kbps"), "")
        .trim()
}
