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
import androidx.core.content.FileProvider
import com.auraplayer.data.model.MediaModel
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

object StoryShareHelper {

    fun shareMusicStory(context: Context, song: MediaModel) {
        try {
            val width = 1080
            val height = 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background Gradient (Deep Cyber Black to Navy)
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        android.graphics.Color.parseColor("#0F172A"),
                        android.graphics.Color.parseColor("#080B14"),
                        android.graphics.Color.parseColor("#05070A")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Outer Neon Glow Ring
            val glowPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 16f
                color = android.graphics.Color.parseColor("#8B5CF6")
                alpha = 90
            }
            val centerDiscX = width / 2f
            val centerDiscY = height * 0.40f
            canvas.drawCircle(centerDiscX, centerDiscY, 320f, glowPaint)

            // Vinyl Base
            val vinylPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#121622")
            }
            canvas.drawCircle(centerDiscX, centerDiscY, 300f, vinylPaint)

            // Concentric vinyl grooves
            val groovePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = android.graphics.Color.parseColor("#334155")
                alpha = 110
            }
            for (r in 140..280 step 35) {
                canvas.drawCircle(centerDiscX, centerDiscY, r.toFloat(), groovePaint)
            }

            // Center Vinyl Label with gradient
            val labelPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    centerDiscX - 110f, centerDiscY - 110f,
                    centerDiscX + 110f, centerDiscY + 110f,
                    intArrayOf(
                        android.graphics.Color.parseColor("#EC4899"),
                        android.graphics.Color.parseColor("#8B5CF6"),
                        android.graphics.Color.parseColor("#38BDF8")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(centerDiscX, centerDiscY, 110f, labelPaint)

            // Center hole
            val holePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#080B14")
            }
            canvas.drawCircle(centerDiscX, centerDiscY, 28f, holePaint)

            // Waveform Audio Bars beneath disc
            val barPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#38BDF8")
                strokeCap = Paint.Cap.ROUND
                strokeWidth = 14f
            }
            val numBars = 32
            val startX = 140f
            val endX = width - 140f
            val barSpacing = (endX - startX) / numBars
            val baseY = height * 0.62f

            for (i in 0 until numBars) {
                val waveFactor = sin(i * 0.38).toFloat() * 0.5f + 0.5f
                val barHeight = 25f + waveFactor * 130f
                val x = startX + i * barSpacing
                canvas.drawLine(x, baseY - barHeight / 2f, x, baseY + barHeight / 2f, barPaint)
            }

            // Song Title Text
            val titlePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 68f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val displayTitle = if (song.title.length > 24) song.title.take(22) + "..." else song.title
            canvas.drawText(displayTitle, width / 2f, height * 0.73f, titlePaint)

            // Artist Text
            val artistPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#94A3B8")
                textSize = 42f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(song.artist, width / 2f, height * 0.78f, artistPaint)

            // App Watermark Badge at bottom
            val badgeBox = RectF(width / 2f - 260f, height * 0.88f, width / 2f + 260f, height * 0.88f + 85f)
            val badgePaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#1E293B")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(badgeBox, 42f, 42f, badgePaint)

            val badgeTextPaint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.parseColor("#00F0FF")
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("🎵 DaVE Player • Hi-Fi", width / 2f, height * 0.88f + 56f, badgeTextPaint)

            // Save Bitmap to cache directory
            val storyFile = File(context.cacheDir, "dave_story.png")
            val fos = FileOutputStream(storyFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            fos.flush()
            fos.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", storyFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Escuchando \"${song.title}\" en DaVE Player 🎶")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir en Estado o Historia"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
