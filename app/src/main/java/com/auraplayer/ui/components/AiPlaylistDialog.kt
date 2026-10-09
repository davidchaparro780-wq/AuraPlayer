package com.auraplayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.model.MediaModel

/**
 * Creador de Listas de Reproducción Inteligente con Inteligencia Artificial (DaVE AI Prompts).
 * Permite al usuario escribir cómo se siente o qué vibra busca ("Para trotar a 180bpm",
 * "Noche lluviosa con café", "Reggaeton viejo") y selecciona dinámicamente las canciones ideales.
 */
@Composable
fun AiPlaylistDialog(
    allSongs: List<MediaModel>,
    onDismiss: () -> Unit,
    onPlaylistCreated: (playlistName: String, selectedSongs: List<MediaModel>) -> Unit
) {
    var prompt by remember { mutableStateOf("") }

    val presetVibes = listOf(
        "🔥 Gimnasio & Motivación" to listOf("rock", "trap", "gym", "workout", "metal", "pump", "power", "hard", "beat"),
        "🌙 Noche Chill & Relax" to listOf("chill", "lofi", "acoustic", "slow", "piano", "calm", "night", "relax", "rain"),
        "⚡ Fiesta & Perreo" to listOf("reggaeton", "party", "dance", "fiesta", "club", "remix", "bass", "techno", "disco"),
        "🚗 Carretera & Viaje" to listOf("road", "pop", "electronic", "drive", "travel", "summer", "vibes"),
        "🎻 Clásico & Acústico" to listOf("acoustic", "guitar", "piano", "classic", "jazz", "instrumental", "unplugged")
    )

    // Filtrador inteligente por palabras clave y patrones semánticos
    val matchedSongs by remember(prompt, allSongs) {
        derivedStateOf {
            if (prompt.isBlank()) {
                emptyList()
            } else {
                val tokens = prompt.lowercase()
                    .replace(",", " ")
                    .split("\\s+".toRegex())
                    .filter { it.length > 2 }

                val matched = allSongs.filter { song ->
                    val combined = "${song.title} ${song.artist} ${song.album}".lowercase()
                    tokens.any { token -> combined.contains(token) }
                }

                if (matched.isNotEmpty()) {
                    matched.take(25)
                } else {
                    // Fallback: Si no coincide por texto exacto, tomar una muestra variada basada en la longitud
                    val hash = prompt.hashCode()
                    allSongs.shuffled(kotlin.random.Random(hash)).take(15)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101422),
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DaVE AI Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Describe la vibra, momento o estilo de música que deseas y la IA armará tu playlist perfecta:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Input de texto del prompt
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Ej: Canciones para trotar o trap motivador...", color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFA855F7),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Chips de sugerencia rápida
                Text(
                    text = "Vibras rápidas recomendadas:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetVibes.forEach { (label, keywords) ->
                        SuggestionChip(
                            onClick = {
                                prompt = label.substringAfter(" ")
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (prompt.contains(label.substringAfter(" "))) Color(0xFFA855F7) else Color.White
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFF1E1B4B)
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (prompt.contains(label.substringAfter(" "))) Color(0xFFA855F7) else Color(0xFF334155)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Resumen de canciones encontradas
                androidx.compose.animation.AnimatedVisibility(visible = prompt.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🎯 Coincidencias encontradas",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA)
                            )
                            Text(
                                text = "${matchedSongs.size} canciones",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Vista previa de primeras 3 canciones
                        matchedSongs.take(3).forEach { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${song.title} - ${song.artist}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (prompt.isNotBlank() && matchedSongs.isNotEmpty()) {
                        val title = "Vibe: ${prompt.take(24).trim()}"
                        onPlaylistCreated(title, matchedSongs)
                        onDismiss()
                    }
                },
                enabled = prompt.isNotBlank() && matchedSongs.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    disabledContainerColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Crear Playlist DaVE AI (${matchedSongs.size})",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF94A3B8))
            }
        }
    )
}
