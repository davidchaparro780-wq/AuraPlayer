package com.auraplayer.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.model.MediaModel
import com.auraplayer.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "BatchCoverFetcherDialog"

/**
 * Descargador Masivo de Carátulas Oficiales en Segundo Plano.
 * Escanea canciones sin imagen y obtiene portadas HD de alta resolución automáticamente.
 */
@Composable
fun BatchCoverFetcherDialog(
    songsWithoutCover: List<MediaModel>,
    onFetchSingleCover: suspend (MediaModel) -> Unit,
    onFinished: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var processedCount by remember { mutableIntStateOf(0) }
    val totalCount = songsWithoutCover.size
    var currentSongName by remember { mutableStateOf("") }
    var isComplete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
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
                            .background(Color(0xFF06B6D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Descargador de Carátulas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                if (!isDownloading) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (totalCount == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF059669)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "¡Todas tus canciones tienen carátula!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else if (!isDownloading && !isComplete) {
                    Text(
                        text = "Se encontraron $totalCount canciones sin carátula en tu biblioteca musical.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "DaVE Player buscará y descargará las portadas oficiales en alta definición automáticamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                } else if (isDownloading) {
                    Text(
                        text = "Descargando carátulas oficiales...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSongName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF38BDF8),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (totalCount > 0) (processedCount.toFloat() / totalCount) else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF06B6D4),
                        trackColor = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$processedCount de $totalCount canciones procesadas",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    Text(
                        text = "¡Descarga masiva completada exitosamente!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                }
            }
        },
        confirmButton = {
            if (totalCount > 0 && !isDownloading && !isComplete) {
                Button(
                    onClick = {
                        isDownloading = true
                        scope.launch(Dispatchers.IO) {
                            songsWithoutCover.forEachIndexed { idx, song ->
                                withContext(Dispatchers.Main) {
                                    processedCount = idx + 1
                                    currentSongName = "${song.title} - ${song.artist}"
                                }
                                try {
                                    onFetchSingleCover(song)
                                } catch (e: Exception) {
                                    AppLog.e(TAG, "No se pudo descargar la carátula de la canción", e)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                isDownloading = false
                                isComplete = true
                                onFinished()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Descargar Todas ($totalCount)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else if (!isDownloading) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cerrar", color = Color.White)
                }
            }
        },
        dismissButton = {
            if (!isDownloading && totalCount > 0 && !isComplete) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            }
        }
    )
}
