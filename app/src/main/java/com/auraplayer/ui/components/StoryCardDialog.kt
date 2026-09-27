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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
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
import java.io.File
import java.io.FileOutputStream

@Composable
fun StoryCardDialog(
    song: MediaModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var lyricQuote by remember { mutableStateOf("“La música expresa lo que no se puede decir y aquello que no puede callarse.”") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tarjeta para Stories 9:16", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Genera una imagen vertical estética para tus historias de Instagram o WhatsApp.",
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
                        Text("— ${song.artist} • DaVE Player", color = Color(0xFFC084FC), fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = lyricQuote,
                    onValueChange = { lyricQuote = it },
                    label = { Text("Frase o verso favorito", color = Color(0xFF94A3B8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFEC4899),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    shareStoryCard(context, song, lyricQuote)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Compartir Historia", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8))) {
                Text("Cancelar")
            }
        }
    )
}

private fun shareStoryCard(context: Context, song: MediaModel, quote: String) {
    try {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background Gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(),
                android.graphics.Color.parseColor("#0F172A"),
                android.graphics.Color.parseColor("#3B0764"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Glass Card
        val cardPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1E1B4B")
            alpha = 230
        }
        val cardRect = RectF(100f, 600f, (width - 100).toFloat(), 1350f)
        canvas.drawRoundRect(cardRect, 48f, 48f, cardPaint)

        // Neon border
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = android.graphics.Color.parseColor("#EC4899")
        }
        canvas.drawRoundRect(cardRect, 48f, 48f, borderPaint)

        // Texts
        val titlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#00F0FF")
            textSize = 52f
            isFakeBoldText = true
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("🎵 ${song.title.take(30)}", 160f, 740f, titlePaint)

        val artistPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#A5B4FC")
            textSize = 38f
        }
        canvas.drawText(song.artist.take(35), 160f, 810f, artistPaint)

        val quotePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 44f
            isAntiAlias = true
        }
        val lines = quote.chunked(32)
        var y = 940f
        for (line in lines.take(4)) {
            canvas.drawText(line, 160f, y, quotePaint)
            y += 60f
        }

        val footerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#64748B")
            textSize = 36f
        }
        canvas.drawText("DaVE Player • Audio en Alta Fidelidad", 160f, 1260f, footerPaint)

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
        context.startActivity(Intent.createChooser(shareIntent, "Compartir en Historia"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al generar tarjeta: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
