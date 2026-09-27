package com.auraplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.AchievementManager
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MusicQuizDialog(
    songs: List<MediaModel>,
    achievementManager: AchievementManager,
    onPlaySnippet: (MediaModel, Long) -> Unit,
    onStopSnippet: () -> Unit,
    onDismiss: () -> Unit
) {
    if (songs.size < 4) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF0C101D),
            title = { Text("DaVE Quiz", color = Color.White) },
            text = { Text("Necesitas al menos 4 canciones en tu teléfono para jugar a la trivia.", color = Color(0xFF94A3B8)) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido", color = Color(0xFF00F0FF)) } }
        )
        return
    }

    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(1) }
    var currentSong by remember { mutableStateOf(songs.random()) }
    var options by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var timeLeft by remember { mutableIntStateOf(10) }

    fun nextRound() {
        val picked = songs.random()
        currentSong = picked
        val distractors = songs.filter { it.id != picked.id }.shuffled().take(3).map { it.title }
        options = (distractors + picked.title).shuffled()
        selectedOption = null
        timeLeft = 10

        // Play 4s snippet starting at 25% of duration
        val startMs = (picked.duration * 0.25).toLong().coerceAtLeast(0L)
        onPlaySnippet(picked, startMs)
    }

    LaunchedEffect(round) {
        nextRound()
    }

    // Countdown Timer
    LaunchedEffect(round, selectedOption) {
        if (selectedOption == null) {
            while (timeLeft > 0 && selectedOption == null) {
                delay(1000)
                timeLeft--
            }
            if (selectedOption == null && timeLeft == 0) {
                // Time's up
                selectedOption = ""
                streak = 0
                onStopSnippet()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { onStopSnippet() }
    }

    AlertDialog(
        onDismissRequest = {
            onStopSnippet()
            onDismiss()
        },
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFFE040FB), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DaVE Quiz 🎵", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
                }
                Text("⭐ $score pts", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Timer & Streak Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⏳ Tiempo: ${timeLeft}s", color = if (timeLeft <= 3) Color(0xFFEF4444) else Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                    if (streak > 1) {
                        Text("🔥 Racha: x$streak", color = Color(0xFFFF6600), fontWeight = FontWeight.ExtraBold)
                    }
                }

                LinearProgressIndicator(
                    progress = { timeLeft / 10f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (timeLeft <= 3) Color(0xFFEF4444) else Color(0xFF00F0FF),
                    trackColor = Color(0xFF1E293B)
                )

                Text(
                    "¿Qué canción está sonando en este fragmento?",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                // 4 Options
                options.forEach { opt ->
                    val isCorrect = opt == currentSong.title
                    val isChosen = selectedOption == opt
                    val bgColor by animateColorAsState(
                        when {
                            selectedOption == null -> Color(0xFF1A2035)
                            isCorrect -> Color(0xFF059669)
                            isChosen -> Color(0xFFDC2626)
                            else -> Color(0xFF1A2035)
                        }, label = "opt_bg"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgColor, RoundedCornerShape(14.dp))
                            .clickable(enabled = selectedOption == null) {
                                selectedOption = opt
                                onStopSnippet()
                                if (opt == currentSong.title) {
                                    score += 10 + (streak * 2)
                                    streak++
                                    achievementManager.recordQuizWin(score)
                                } else {
                                    streak = 0
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Text(
                            text = opt,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedOption != null) {
                Button(
                    onClick = { round++ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Siguiente Canción ➔", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onStopSnippet()
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))
            ) {
                Text("Salir")
            }
        }
    )
}
