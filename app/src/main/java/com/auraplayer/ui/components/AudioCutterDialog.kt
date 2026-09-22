package com.auraplayer.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

@Composable
fun AudioCutterDialog(
    song: MediaModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val totalSec = remember(song.duration) {
        val s = (song.duration / 1000f).coerceAtLeast(10f)
        s
    }

    var startSec by remember { mutableFloatStateOf(0f) }
    var endSec by remember { mutableFloatStateOf(30f.coerceAtMost(totalSec)) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var isTrimming by remember { mutableStateOf(false) }

    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                previewPlayer?.stop()
                previewPlayer?.release()
                previewPlayer = null
            } catch (_: Exception) {}
        }
    }

    // Monitor preview playback position to stop at endSec
    LaunchedEffect(isPreviewPlaying) {
        while (isActive && isPreviewPlaying) {
            val mp = previewPlayer
            if (mp != null) {
                try {
                    if (mp.currentPosition >= (endSec * 1000).toInt()) {
                        mp.pause()
                        mp.seekTo((startSec * 1000).toInt())
                        isPreviewPlaying = false
                    }
                } catch (_: Exception) {
                    isPreviewPlaying = false
                }
            }
            delay(200)
        }
    }

    fun playPreview() {
        try {
            if (previewPlayer == null) {
                previewPlayer = MediaPlayer().apply {
                    setDataSource(context, song.uri)
                    prepare()
                }
            }
            previewPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    isPreviewPlaying = false
                } else {
                    mp.seekTo((startSec * 1000).toInt())
                    mp.start()
                    isPreviewPlaying = true
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo reproducir vista previa", Toast.LENGTH_SHORT).show()
            isPreviewPlaying = false
        }
    }

    fun checkWriteSettingsPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:" + context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Toast.makeText(context, "Concede permiso para modificar ajustes del sistema", Toast.LENGTH_LONG).show()
                return false
            }
        }
        return true
    }

    fun applyRingtoneType(type: Int, label: String) {
        if (!checkWriteSettingsPermission()) return
        try {
            RingtoneManager.setActualDefaultRingtoneUri(context, type, song.uri)
            Toast.makeText(context, "¡'$label' actualizado con éxito! 🎉", Toast.LENGTH_SHORT).show()
            onDismiss()
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportTrimmedAudio() {
        isTrimming = true
        scope.launch(Dispatchers.IO) {
            try {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DaVE_Cuts")
                if (!dir.exists()) dir.mkdirs()
                val safeName = song.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val outExt = if (song.path.endsWith(".m4a", true)) "m4a" else "mp3"
                val outFile = File(dir, "${safeName}_corte_${startSec.toInt()}s_${endSec.toInt()}s.$outExt")

                trimAudioFile(context, song.uri, outFile, (startSec * 1000).toLong(), (endSec * 1000).toLong())

                android.media.MediaScannerConnection.scanFile(context, arrayOf(outFile.absolutePath), null, null)

                withContext(Dispatchers.Main) {
                    isTrimming = false
                    Toast.makeText(context, "Clip guardado en Música/DaVE_Cuts ✨", Toast.LENGTH_LONG).show()
                    onDismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isTrimming = false
                    Toast.makeText(context, "Error al recortar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isTrimming) onDismiss()
        },
        containerColor = Color(0xFF101422),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cortador & Ringtone",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Song Title & Artist
                Text(
                    text = song.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp
                )
                Text(
                    text = "${song.artist} • Duración: ${formatSec(totalSec)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Time Indicator Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("INICIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(formatSec(startSec), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }

                        IconButton(
                            onClick = { playPreview() },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Preescuchar",
                                tint = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("FIN (${formatSec(endSec - startSec)})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                            Text(formatSec(endSec), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Range Slider
                RangeSlider(
                    value = startSec..endSec,
                    onValueChange = { range ->
                        startSec = range.start
                        endSec = (range.endInclusive).coerceAtLeast(range.start + 3f)
                        previewPlayer?.seekTo((startSec * 1000).toInt())
                    },
                    valueRange = 0f..totalSec,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = startSec == 0f && endSec == 30f.coerceAtMost(totalSec),
                        onClick = {
                            startSec = 0f
                            endSec = 30f.coerceAtMost(totalSec)
                        },
                        label = { Text("0-30s", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                    )
                    FilterChip(
                        selected = startSec == 30f && endSec == 60f.coerceAtMost(totalSec),
                        onClick = {
                            startSec = 30f.coerceAtMost(totalSec - 5f)
                            endSec = 60f.coerceAtMost(totalSec)
                        },
                        label = { Text("30-60s", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                    )
                    FilterChip(
                        selected = startSec == 45f && endSec == 75f.coerceAtMost(totalSec),
                        onClick = {
                            startSec = 45f.coerceAtMost(totalSec - 5f)
                            endSec = 75f.coerceAtMost(totalSec)
                        },
                        label = { Text("Coro", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                    )
                    FilterChip(
                        selected = startSec == 0f && endSec == totalSec,
                        onClick = {
                            startSec = 0f
                            endSec = totalSec
                        },
                        label = { Text("Todo", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { applyRingtoneType(RingtoneManager.TYPE_RINGTONE, "Tono de Llamada") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Establecer como Tono de Llamada", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { applyRingtoneType(RingtoneManager.TYPE_ALARM, "Alarma") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alarma", fontSize = 13.sp)
                        }

                        Button(
                            onClick = { applyRingtoneType(RingtoneManager.TYPE_NOTIFICATION, "Notificación") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Notificación", fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = { exportTrimmedAudio() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isTrimming
                    ) {
                        if (isTrimming) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardando clip...", color = Color.White)
                        } else {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar como nuevo archivo", color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

private fun formatSec(seconds: Float): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format("%02d:%02d", mins, secs)
}

/**
 * Native hardware-accelerated audio trimming using Android MediaExtractor and MediaMuxer.
 */
private fun trimAudioFile(
    context: Context,
    srcUri: Uri,
    dstFile: File,
    startMs: Long,
    endMs: Long
) {
    val extractor = MediaExtractor()
    extractor.setDataSource(context, srcUri, null)

    var trackIndex = -1
    for (i in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(i)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
        if (mime.startsWith("audio/")) {
            trackIndex = i
            break
        }
    }

    if (trackIndex < 0) {
        extractor.release()
        // Fallback: simple copy if format unrecognized
        context.contentResolver.openInputStream(srcUri)?.use { input ->
            dstFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return
    }

    extractor.selectTrack(trackIndex)
    val format = extractor.getTrackFormat(trackIndex)

    val muxer = MediaMuxer(dstFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    val muxerTrack = muxer.addTrack(format)
    muxer.start()

    val bufferSize = 64 * 1024
    val buffer = ByteBuffer.allocate(bufferSize)
    val bufferInfo = MediaCodec.BufferInfo()

    extractor.seekTo(startMs * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

    try {
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break

            val sampleTimeUs = extractor.sampleTime
            if (sampleTimeUs > endMs * 1000) break

            bufferInfo.offset = 0
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = sampleTimeUs - (startMs * 1000)
            bufferInfo.flags = extractor.sampleFlags

            muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
            extractor.advance()
        }
    } finally {
        try {
            muxer.stop()
            muxer.release()
        } catch (_: Exception) {}
        extractor.release()
    }
}
