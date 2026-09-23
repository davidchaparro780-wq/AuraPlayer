package com.auraplayer.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

object DjSynthSoundPlayer {

    fun playAirhorn() {
        thread {
            try {
                val sampleRate = 44100
                for (honk in 0..2) {
                    val durationMs = if (honk == 2) 320 else 170
                    val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                    val buffer = ShortArray(numSamples)

                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val envelope = if (i < 400) i / 400.0 else (numSamples - i).toDouble() / numSamples
                        val sample = (
                            sin(2 * PI * 466.16 * t) * 0.4 +
                            sin(2 * PI * 622.25 * t) * 0.35 +
                            sin(2 * PI * 698.46 * t) * 0.25
                        ) * envelope
                        buffer[i] = (sample * Short.MAX_VALUE * 0.85).toInt().toShort()
                    }
                    playBuffer(buffer, sampleRate)
                    Thread.sleep(if (honk == 2) 40 else 60)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playScratch() {
        thread {
            try {
                val sampleRate = 44100
                val durationMs = 280
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = 220.0 + 900.0 * sin(progress * PI * 3)
                    val sample = sin(2 * PI * freq * (i.toDouble() / sampleRate)) * (1.0 - progress)
                    buffer[i] = (sample * Short.MAX_VALUE * 0.7).toInt().toShort()
                }
                playBuffer(buffer, sampleRate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playBassDrop() {
        thread {
            try {
                val sampleRate = 44100
                val durationMs = 600
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = 140.0 * (1.0 - progress * 0.72)
                    val envelope = (1.0 - progress)
                    val sample = sin(2 * PI * freq * (i.toDouble() / sampleRate)) * envelope
                    buffer[i] = (sample * Short.MAX_VALUE * 0.9).toInt().toShort()
                }
                playBuffer(buffer, sampleRate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playApplause() {
        thread {
            try {
                val sampleRate = 44100
                val durationMs = 500
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                val random = java.util.Random()

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val envelope = sin(progress * PI)
                    val whiteNoise = (random.nextDouble() * 2.0 - 1.0)
                    buffer[i] = (whiteNoise * envelope * Short.MAX_VALUE * 0.5).toInt().toShort()
                }
                playBuffer(buffer, sampleRate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playBuffer(buffer: ShortArray, sampleRate: Int) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        Thread.sleep((buffer.size * 1000L) / sampleRate + 30)
        audioTrack.release()
    }
}

data class SoundEffectItem(
    val title: String,
    val subtitle: String,
    val icon: String,
    val gradient: List<Color>,
    val onPlay: () -> Unit
)

@Composable
fun SoundboardDialog(
    onDismiss: () -> Unit
) {
    val soundEffects = listOf(
        SoundEffectItem(
            title = "Airhorn DJ",
            subtitle = "Bocina de reggaetón",
            icon = "🚨",
            gradient = listOf(Color(0xFFFF0055), Color(0xFFFF5252)),
            onPlay = { DjSynthSoundPlayer.playAirhorn() }
        ),
        SoundEffectItem(
            title = "Vinilo Scratch",
            subtitle = "Giro de disco",
            icon = "💽",
            gradient = listOf(Color(0xFF00F0FF), Color(0xFF38BDF8)),
            onPlay = { DjSynthSoundPlayer.playScratch() }
        ),
        SoundEffectItem(
            title = "808 Bass Boom",
            subtitle = "Bajo profundo",
            icon = "💣",
            gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
            onPlay = { DjSynthSoundPlayer.playBassDrop() }
        ),
        SoundEffectItem(
            title = "Aplausos",
            subtitle = "Ovación de fiesta",
            icon = "👏",
            gradient = listOf(Color(0xFFFFD700), Color(0xFFFF9100)),
            onPlay = { DjSynthSoundPlayer.playApplause() }
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101422),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFFFF0055),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DJ Party Soundboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White.copy(alpha = 0.7f))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Toca cualquier efecto para disparar sonidos en vivo sobre tu música:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(soundEffects) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(item.gradient))
                                .clickable { item.onPlay() }
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = item.icon,
                                    fontSize = 32.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
