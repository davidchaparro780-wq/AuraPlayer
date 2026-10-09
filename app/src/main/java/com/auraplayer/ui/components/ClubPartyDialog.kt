package com.auraplayer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.auraplayer.audio.AuraHaptic
import com.auraplayer.audio.FlashlightBeatManager
import com.auraplayer.audio.RealtimeVisualizerManager

/**
 * Modo Club & Linterna Estroboscópica de Fiesta (Club Party Strobe Hub).
 * Sincroniza el flash LED trasero de la cámara del teléfono y el perímetro de la pantalla
 * con los picos acústicos de la canción para convertir cualquier habitación en una discoteca.
 */
@Composable
fun ClubPartyDialog(
    isPlaying: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val flashManager = remember { FlashlightBeatManager(context) }
    var isFlashActive by remember { mutableStateOf(flashManager.isEnabled) }

    DisposableEffect(Unit) {
        onDispose {
            flashManager.stop()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "party_rgb")
    val rgbHue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rgbHue"
    )

    val bassEnergy = if (isPlaying) RealtimeVisualizerManager.instance.getBassEnergy(isPlaying) else 0.1f
    val dynamicColor = Color.hsl(rgbHue, 0.9f, 0.55f)
    val flashAlpha = if (isPlaying) (bassEnergy * 1.3f).coerceIn(0.2f, 1.0f) else 0.2f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                dynamicColor.copy(alpha = flashAlpha * 0.45f),
                                Color(0xFF070B16),
                                Color.Black
                            )
                        )
                    )
            ) {
                // Perímetro estroboscópico de fiesta
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .border(
                            width = (6.dp * flashAlpha),
                            color = dynamicColor.copy(alpha = flashAlpha),
                            shape = RoundedCornerShape(24.dp)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cabecera
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Nightlife,
                                contentDescription = null,
                                tint = dynamicColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MODO CLUB STROBE",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                    }

                    // Orbe de Ritmo Central
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size((160.dp * (0.85f + bassEnergy * 0.4f)).coerceIn(140.dp, 220.dp))
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color.White.copy(alpha = flashAlpha),
                                            dynamicColor,
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(60.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = if (isFlashActive) "⚡ FLASH TRASERO DISPARANDO AL RITMO" else "PULSA PARA ACTIVAR FLASH LED",
                            fontWeight = FontWeight.Bold,
                            color = if (isFlashActive) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Controles Inferiores
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131A2B))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Linterna LED de Cámara",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Destellos sincronizados con los drops",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            Switch(
                                checked = isFlashActive,
                                onCheckedChange = {
                                    AuraHaptic.heavy(view)
                                    isFlashActive = flashManager.toggle()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFBBF24),
                                    checkedTrackColor = Color(0xFFFBBF24).copy(alpha = 0.4f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = dynamicColor
                            )
                        ) {
                            Text(
                                text = "Volver al Reproductor",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
