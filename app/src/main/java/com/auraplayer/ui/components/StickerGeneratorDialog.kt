package com.auraplayer.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.auraplayer.data.model.MediaModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun StickerGeneratorDialog(
    song: MediaModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }

    val emojis = listOf("🎵", "🔥", "🎧", "💎", "⚡", "🌟", "🎶", "❤️")
    var selectedEmoji by remember { mutableStateOf("🎵") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEmotions, contentDescription = null,
                    tint = Color(0xFF00F0FF), modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text("💬 Generar Sticker", fontWeight = FontWeight.ExtraBold,
                    color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Crea un sticker 512×512 con la\ncanción actual para compartir por WhatsApp",
                    color = Color(0xFF94A3B8), fontSize = 13.sp)

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2035)),
                    shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("🎵 ${song.title}", color = Color.White,
                            fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text(song.artist, color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 1)
                    }
                }

                Text("Elige un emoji:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojis.forEach { emoji ->
                        FilterChip(
                            selected = selectedEmoji == emoji,
                            onClick = { selectedEmoji = emoji },
                            label = { Text(emoji, fontSize = 18.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6C63FF),
                                containerColor = Color(0xFF1A2035)
                            )
                        )
                    }
                }

                if (isDone) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF00C853).copy(0.2f)),
                        shape = RoundedCornerShape(12.dp)) {
                        Text("✅ ¡Sticker generado! Escoge la app para compartir.",
                            color = Color(0xFF00C853), modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        scope.launch {
                            isGenerating = true
                            val uri = withContext(Dispatchers.IO) {
                                generateStickerBitmap(context, song, selectedEmoji)
                            }
                            if (uri != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir Sticker"))
                                isDone = true
                            }
                            isGenerating = false
                        }
                    },
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isGenerating) "Generando… ⏳" else "📤 Crear y Compartir",
                        color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            }
        },
        dismissButton = {}
    )
}

private fun generateStickerBitmap(context: Context, song: MediaModel, emoji: String): Uri? {
    return try {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(),
                intArrayOf(0xFF0C101D.toInt(), 0xFF1A0A2E.toInt(), 0xFF0D1B2A.toInt()),
                null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // Album art from path if available
        val artBitmap = try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(song.path)
            val art = retriever.embeddedPicture
            retriever.release()
            if (art != null) BitmapFactory.decodeByteArray(art, 0, art.size) else null
        } catch (_: Exception) { null }

        if (artBitmap != null) {
            val scaled = Bitmap.createScaledBitmap(artBitmap, size, size, true)
            val artPaint = Paint().apply { alpha = 80 }
            canvas.drawBitmap(scaled, 0f, 0f, artPaint)
        }

        // Dark overlay for text readability
        val overlayPaint = Paint().apply {
            color = 0xCC000000.toInt()
        }
        canvas.drawRect(0f, size * 0.55f, size.toFloat(), size.toFloat(), overlayPaint)

        // Neon border
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = 0xFF00F0FF.toInt()
            isAntiAlias = true
        }
        canvas.drawRect(4f, 4f, size - 4f, size - 4f, borderPaint)

        // Emoji
        val emojiPaint = Paint().apply { textSize = 120f; isAntiAlias = true }
        canvas.drawText(emoji, size * 0.05f, size * 0.5f, emojiPaint)

        // Song title
        val titlePaint = Paint().apply {
            color = android.graphics.Color.WHITE; textSize = 38f
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
        }
        val titleText = if (song.title.length > 18) song.title.take(18) + "…" else song.title
        canvas.drawText(titleText, 24f, size * 0.68f, titlePaint)

        // Artist
        val artistPaint = Paint().apply { color = 0xFF94A3B8.toInt(); textSize = 28f; isAntiAlias = true }
        val artistText = if (song.artist.length > 22) song.artist.take(22) + "…" else song.artist
        canvas.drawText(artistText, 24f, size * 0.79f, artistPaint)

        // DaVE watermark
        val wmPaint = Paint().apply { color = 0xFF6C63FF.toInt(); textSize = 22f; isAntiAlias = true }
        canvas.drawText("DaVE Player 🎵", 24f, size * 0.94f, wmPaint)

        // Save to cache
        val file = File(context.cacheDir, "sticker_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
