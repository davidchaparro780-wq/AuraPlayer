package com.auraplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.AchievementManager

@Composable
fun AchievementsDialog(
    achievementManager: AchievementManager,
    onDismiss: () -> Unit
) {
    val level = remember { achievementManager.listenerLevel }
    val title = remember { achievementManager.levelTitle }
    val exp = remember { achievementManager.totalExp }
    val achievements = remember { achievementManager.getAchievements() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nivel & Logros 🏆", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                // Player Level Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nivel $level", color = Color(0xFF00F0FF), fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(title, color = Color(0xFFFFD700), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ((exp % 100) / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Color(0xFF00F0FF),
                            trackColor = Color(0xFF334155)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${exp % 100} / 100 EXP para el siguiente nivel", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                Text("Tus Medallas:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                // List of achievements
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                ) {
                    items(achievements) { a ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (a.isUnlocked) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF1E293B),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(a.icon, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(a.title, color = if (a.isUnlocked) Color(0xFF10B981) else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (a.isUnlocked) {
                                        Text("  ✓", color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    }
                                }
                                Text(a.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                if (!a.isUnlocked) {
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { a.progress },
                                        modifier = Modifier.fillMaxWidth().height(3.dp),
                                        color = Color(0xFF38BDF8),
                                        trackColor = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Genial", fontWeight = FontWeight.Bold)
            }
        }
    )
}
