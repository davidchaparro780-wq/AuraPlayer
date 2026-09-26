package com.auraplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DiagnosticResult(
    val duplicates: List<Pair<MediaModel, MediaModel>>,
    val noCover: List<MediaModel>,
    val corruptFiles: List<MediaModel>,
    val incompleteMetadata: List<MediaModel>,
    val totalScanned: Int
)

@Composable
fun LibraryDiagnosticDialog(
    songs: List<MediaModel>,
    onDeleteFiles: (List<MediaModel>) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<DiagnosticResult?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        isScanning = true
        result = withContext(Dispatchers.IO) { runDiagnostic(songs) }
        isScanning = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxHeight(0.9f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = null,
                    tint = Color(0xFF00F0FF), modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text("🧰 Diagnóstico", fontWeight = FontWeight.ExtraBold,
                    color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            if (isScanning) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(color = Color(0xFF00F0FF))
                    Spacer(Modifier.height(12.dp))
                    Text("Analizando ${songs.size} canciones...", color = Color(0xFF94A3B8))
                }
            } else {
                val r = result ?: return@AlertDialog
                Column {
                    // Summary cards
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiagCard("${r.duplicates.size}", "Duplicados", Color(0xFFFF6584), Modifier.weight(1f))
                        DiagCard("${r.noCover.size}", "Sin carátula", Color(0xFFFFD700), Modifier.weight(1f))
                        DiagCard("${r.corruptFiles.size}", "Corruptos", Color(0xFFFF4444), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    val goodCount = r.totalScanned - r.duplicates.size - r.corruptFiles.size
                    Text("✅ $goodCount de ${r.totalScanned} archivos están perfectos",
                        color = Color(0xFF00C853), fontSize = 12.sp)

                    Spacer(Modifier.height(12.dp))

                    // Tabs
                    val tabs = listOf("Duplicados", "Sin carátula", "Corruptos")
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF1A2035),
                        contentColor = Color(0xFF00F0FF),
                        edgePadding = 0.dp
                    ) {
                        tabs.forEachIndexed { i, t ->
                            Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                                Text(t, modifier = Modifier.padding(8.dp),
                                    color = if (selectedTab == i) Color(0xFF00F0FF) else Color(0xFF64748B),
                                    fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val listItems: List<MediaModel> = when (selectedTab) {
                        0 -> r.duplicates.flatMap { listOf(it.first, it.second) }.distinct()
                        1 -> r.noCover
                        else -> r.corruptFiles
                    }

                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        if (listItems.isEmpty()) {
                            item {
                                Text("✅ Nada encontrado aquí", color = Color(0xFF00C853),
                                    modifier = Modifier.padding(16.dp))
                            }
                        }
                        items(listItems.take(30)) { song ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(8.dp).background(Color(0xFFFF6584), CircleShape))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.title, color = Color.White, fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text(song.artist, color = Color(0xFF64748B), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth()) {
                result?.let { r ->
                    if (r.duplicates.isNotEmpty() || r.corruptFiles.isNotEmpty()) {
                        Button(
                            onClick = {
                                val toDelete = (r.corruptFiles + r.duplicates.map { it.second }).distinct()
                                onDeleteFiles(toDelete)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🗑 Limpiar archivos problemáticos",
                                color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar", color = Color(0xFF64748B))
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun DiagCard(count: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2035)),
        shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(label, color = Color(0xFF94A3B8), fontSize = 10.sp)
        }
    }
}

private fun runDiagnostic(songs: List<MediaModel>): DiagnosticResult {
    val duplicates = mutableListOf<Pair<MediaModel, MediaModel>>()
    val noCover = mutableListOf<MediaModel>()
    val corruptFiles = mutableListOf<MediaModel>()

    val seen = mutableMapOf<String, MediaModel>()
    songs.forEach { song ->
        // Duplicate detection: same title + artist (case insensitive)
        val key = "${song.title.lowercase().trim()}|${song.artist.lowercase().trim()}"
        if (seen.containsKey(key)) {
            duplicates.add(Pair(seen[key]!!, song))
        } else {
            seen[key] = song
        }

        // Corrupt: file doesn't exist or is 0 bytes
        try {
            val f = File(song.path)
            if (!f.exists() || f.length() == 0L) corruptFiles.add(song)
        } catch (_: Exception) { corruptFiles.add(song) }

        // No cover: albumArtUri is null/empty
        if (song.albumArtUri.isNullOrBlank()) noCover.add(song)
    }

    return DiagnosticResult(
        duplicates = duplicates,
        noCover = noCover,
        corruptFiles = corruptFiles,
        incompleteMetadata = emptyList(),
        totalScanned = songs.size
    )
}
