package com.auraplayer.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.auraplayer.data.model.MediaModel
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

enum class StoryVisualTheme(val title: String, val bgStart: String, val bgEnd: String, val neonColor: String) {
    NEON_CYBER("Cyberpunk", "#0A0F1D", "#3B0764", "#00F0FF"),
    SUNSET_RETRO("Sunset 80s", "#1F1124", "#701A75", "#F43F5E"),
    OLED_MINIMAL("OLED Minimal", "#000000", "#111827", "#10B981"),
    AURORA_PURPLE("Aurora Boreal", "#0B132B", "#1C2541", "#8B5CF6")
}

@Composable
fun StoryCardDialog(
    song: MediaModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var lyricQuote by remember { mutableStateOf("“La música expresa lo que no se puede decir y aquello que no puede callarse.”") }
    var selectedTheme by remember { mutableStateOf(StoryVisualTheme.NEON_CYBER) }
    var includeSpectrumWave by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Story & Reel Maker 9:16", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Genera una tarjeta vertical HD con ondas de audio y tu frase favorita lista para WhatsApp o Reels:",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                // Preview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("🎵 ${song.title}", color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(lyricQuote, color = Color.White, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("— ${song.artist} • DaVE Player", color = Color(0xFFC084FC), fontSize = 12.sp)
                            if (includeSpectrumWave) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    listOf(6, 14, 18, 10, 16, 22, 12, 8).forEach { h ->
                                        Box(
                                            Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF00F0FF))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Theme selector
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StoryVisualTheme.values().forEach { theme ->
                        val isSelected = selectedTheme == theme
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF334155) else Color(0xFF1E293B))
                                .border(1.5.dp, if (isSelected) Color(0xFFEC4899) else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { selectedTheme = theme }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = theme.title.take(8),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = lyricQuote,
                    onValueChange = { lyricQuote = it },
                    label = { Text("Frase, verso o mensaje", color = Color(0xFF94A3B8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFEC4899),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { includeSpectrumWave = !includeSpectrumWave }
                ) {
                    Checkbox(
                        checked = includeSpectrumWave,
                        onCheckedChange = { includeSpectrumWave = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00F0FF))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Incluir ondas reactivas de espectro de audio", color = Color.White, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    shareStoryCard(context, song, lyricQuote, selectedTheme, includeSpectrumWave)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Compartir en WhatsApp / Reels", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))) {
                Text("Cancelar")
            }
        }
    )
}

private fun shareStoryCard(
    context: Context,
    song: MediaModel,
    quote: String,
    theme: StoryVisualTheme,
    drawWave: Boolean
) {
    try {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background Gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(),
                android.graphics.Color.parseColor(theme.bgStart),
                android.graphics.Color.parseColor(theme.bgEnd),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Glass Card
        val cardPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#151B2E")
            alpha = 235
        }
        val cardRect = RectF(100f, 540f, (width - 100).toFloat(), 1420f)
        canvas.drawRoundRect(cardRect, 56f, 56f, cardPaint)

        // Neon border
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = android.graphics.Color.parseColor(theme.neonColor)
        }
        canvas.drawRoundRect(cardRect, 56f, 56f, borderPaint)

        // Title
        val titlePaint = Paint().apply {
            color = android.graphics.Color.parseColor(theme.neonColor)
            textSize = 54f
            isFakeBoldText = true
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("🎵 ${song.title.take(30)}", 160f, 680f, titlePaint)

        // Artist
        val artistPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#A5B4FC")
            textSize = 40f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText(song.artist.take(35), 160f, 755f, artistPaint)

        // Waveform Visualizer on the card
        if (drawWave) {
            val wavePaint = Paint().apply {
                color = android.graphics.Color.parseColor(theme.neonColor)
                style = Paint.Style.FILL
            }
            val numBars = 36
            val barW = 16f
            val startX = 160f
            val spacing = 22f
            val baseH = 860f

            for (i in 0 until numBars) {
                val h = (20f + 70f * (sin(i * 0.45) * 0.5f + 0.5f)).toFloat()
                canvas.drawRoundRect(
                    startX + i * spacing,
                    baseH - h,
                    startX + i * spacing + barW,
                    baseH + h,
                    8f,
                    8f,
                    wavePaint
                )
            }
        }

        // Quote
        val quotePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 44f
            isAntiAlias = true
        }
        val lines = quote.chunked(30)
        var y = if (drawWave) 1000f else 900f
        for (line in lines.take(5)) {
            canvas.drawText(line, 160f, y, quotePaint)
            y += 62f
        }

        val footerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 34f
        }
        canvas.drawText("✨ DaVE Player • Experiencia Audiófila & Stories", 160f, 1340f, footerPaint)

        // Save to cache and share
        val cachePath = File(context.cacheDir, "story_cards")
        cachePath.mkdirs()
        val file = File(cachePath, "story_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir en Historia de WhatsApp / Reels"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al generar tarjeta: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
