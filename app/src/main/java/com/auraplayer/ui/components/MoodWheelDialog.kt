package com.auraplayer.ui.components

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.model.MediaModel

enum class MoodCategory(
    val title: String,
    val emoji: String,
    val color: Color,
    val description: String
) {
    PARTY("Fiesta & Euforia", "⚡", Color(0xFFF43F5E), "Ritmo acelerado y máxima energía"),
    FOCUS("Focus & Estudio", "🧘", Color(0xFF38BDF8), "Concentración profunda e instrumental"),
    MELANCHOLY("Melancolía & Noche", "🌧️", Color(0xFF818CF8), "Baladas emotivas y desahogo"),
    CHILL("Chill & Relax", "🌴", Color(0xFF10B981), "Vibra suave, acústica y desconexión"),
    GYM("Motivación & Gym", "🔥", Color(0xFFF59E0B), "Puro empuje para entrenar fuerte")
}

/**
 * Diálogo de Rueda de Estados de Ánimo (Mood Wheel).
 * Permite seleccionar tu vibra emocional y genera al instante una cola armónica afín.
 */
@Composable
fun MoodWheelDialog(
    allSongs: List<MediaModel>,
    onMoodSelected: (MoodCategory, List<MediaModel>) -> Unit,
    onDismiss: () -> Unit
) {
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
                            .background(Color(0xFFA855F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Rueda de Emociones",
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Selecciona cómo te sientes ahora y DaVE armará tu lista perfecta al instante:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                MoodCategory.values().forEach { mood ->
                    MoodItemCard(
                        mood = mood,
                        onClick = {
                            val filtered = when (mood) {
                                MoodCategory.PARTY -> allSongs.shuffled().take(15)
                                MoodCategory.FOCUS -> allSongs.filter { it.duration > 180_000L }.shuffled().take(15)
                                MoodCategory.MELANCHOLY -> allSongs.filter { it.duration > 200_000L }.shuffled().take(15)
                                MoodCategory.CHILL -> allSongs.shuffled().take(15)
                                MoodCategory.GYM -> allSongs.filter { it.duration < 240_000L }.shuffled().take(15)
                            }.ifEmpty { allSongs.shuffled().take(15) }

                            onMoodSelected(mood, filtered)
                        }
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun MoodItemCard(
    mood: MoodCategory,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, mood.color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = mood.emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mood.title,
                color = mood.color,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = mood.description,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
    }
}
