package com.auraplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TournamentStage { SETUP, BRACKET, WINNER }

@Composable
fun SongTournamentDialog(
    songs: List<MediaModel>,
    onPlaySong: (MediaModel) -> Unit,
    onDismiss: () -> Unit
) {
    var stage by remember { mutableStateOf(TournamentStage.SETUP) }
    var bracket by remember { mutableStateOf<List<MediaModel>>(emptyList()) }
    var currentPair by remember { mutableStateOf<Pair<MediaModel, MediaModel>?>(null) }
    var winners by remember { mutableStateOf<List<MediaModel>>(emptyList()) }
    var winner by remember { mutableStateOf<MediaModel?>(null) }
    var roundNum by remember { mutableIntStateOf(1) }
    var matchNum by remember { mutableIntStateOf(1) }
    var totalMatches by remember { mutableIntStateOf(1) }
    var previewTimer by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun startTournament(size: Int) {
        val shuffled = songs.shuffled().take(size)
        bracket = shuffled
        winners = emptyList()
        roundNum = 1
        matchNum = 1
        totalMatches = size / 2
        currentPair = if (shuffled.size >= 2) Pair(shuffled[0], shuffled[1]) else null
        stage = TournamentStage.BRACKET
    }

    fun vote(chosen: MediaModel) {
        val newWinners = winners + chosen
        val remaining = bracket.drop(matchNum * 2)
        if (remaining.size >= 2) {
            winners = newWinners
            currentPair = Pair(remaining[0], remaining[1])
            matchNum++
        } else if (remaining.size == 1) {
            // odd one out advances automatically
            val nextRound = newWinners + remaining
            if (nextRound.size == 1) {
                winner = nextRound[0]; stage = TournamentStage.WINNER
            } else {
                bracket = nextRound; winners = emptyList()
                matchNum = 1; totalMatches = nextRound.size / 2
                roundNum++
                currentPair = Pair(nextRound[0], nextRound[1])
            }
        } else {
            // Round done
            if (newWinners.size == 1) {
                winner = newWinners[0]; stage = TournamentStage.WINNER
            } else {
                bracket = newWinners; winners = emptyList()
                matchNum = 1; totalMatches = newWinners.size / 2
                roundNum++
                currentPair = Pair(newWinners[0], newWinners[1])
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null,
                    tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("🏆 Torneo de Canciones", fontWeight = FontWeight.ExtraBold,
                    color = Color.White, fontSize = 19.sp)
            }
        },
        text = {
            when (stage) {
                TournamentStage.SETUP -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Elige cuántas canciones entran al torneo.\nVotarás por tu favorita en cada duelo hasta coronar a LA CAMPEONA.",
                            color = Color(0xFF94A3B8), fontSize = 13.sp)

                        listOf(8, 16, 32).forEach { size ->
                            val available = songs.size >= size
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = available) { startTournament(size) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (available) Color(0xFF1A2035) else Color(0xFF111827)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (size == 8) "⚡" else if (size == 16) "🔥" else "🌋",
                                        fontSize = 28.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("$size canciones", color = if (available) Color.White else Color(0xFF374151),
                                            fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${size/2 + size/4 + 1} rondas", color = if (available) Color(0xFF94A3B8) else Color(0xFF374151),
                                            fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    if (!available) Text("Necesitas $size canciones", color = Color(0xFF374151), fontSize = 11.sp)
                                    else Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF6C63FF))
                                }
                            }
                        }
                    }
                }

                TournamentStage.BRACKET -> {
                    val pair = currentPair ?: return@AlertDialog
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ronda $roundNum • Duelo $matchNum de $totalMatches",
                            color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))

                        // Song A
                        TournamentSongCard(pair.first, "A", Color(0xFF6C63FF)) { vote(pair.first) }

                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            HorizontalDivider(color = Color(0xFF1E293B))
                            Text("  VS  ", color = Color(0xFFFFD700),
                                fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                                modifier = Modifier.background(Color(0xFF0C101D)).padding(horizontal = 12.dp))
                        }
                        Spacer(Modifier.height(8.dp))

                        // Song B
                        TournamentSongCard(pair.second, "B", Color(0xFFFF6584)) { vote(pair.second) }
                    }
                }

                TournamentStage.WINNER -> {
                    val w = winner ?: return@AlertDialog
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 64.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("¡CAMPEONA ABSOLUTA!", color = Color(0xFFFFD700),
                            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2035)),
                            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(Modifier.background(
                                Brush.horizontalGradient(listOf(Color(0xFFFFD700).copy(0.2f), Color(0xFF1A2035)))
                            )) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(w.title, color = Color.White,
                                        fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                    Spacer(Modifier.height(2.dp))
                                    Text(w.artist, color = Color(0xFFFFD700), fontSize = 14.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Esta es tu canción favorita oficial del mes 🎖️",
                            color = Color(0xFF94A3B8), textAlign = TextAlign.Center, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth()) {
                if (stage == TournamentStage.WINNER) {
                    val w = winner
                    if (w != null) {
                        Button(
                            onClick = { onPlaySong(w); onDismiss() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("▶ Reproducir la campeona", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(if (stage == TournamentStage.WINNER) "Cerrar" else "Cancelar", color = Color(0xFF64748B))
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun TournamentSongCard(song: MediaModel, label: String, accentColor: Color, onVote: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            scope.launch {
                scale.animateTo(0.95f, tween(80))
                scale.animateTo(1f, tween(120))
                onVote()
            }
        }.scale(scale.value),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2035)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(accentColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, maxLines = 1)
                Text(song.artist, color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 1)
            }
            Icon(Icons.Default.CheckCircle, contentDescription = "Votar",
                tint = accentColor, modifier = Modifier.size(24.dp))
        }
    }
}
