package com.auraplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.EqualizerManager
import com.auraplayer.data.repository.PlaylistManager

@Composable
fun EqualizerScreen(
    playlistManager: PlaylistManager,
    currentAccent: String,
    onAccentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val eqManager = remember { EqualizerManager.instance }
    var isEqEnabled by remember { mutableStateOf(eqManager.isEnabled) }
    var selectedPreset by remember { mutableIntStateOf(0) }
    var selectedProfile by remember { mutableIntStateOf(0) }
    val presets = listOf(
        "Aura Flat", "Cyber Bass", "Vocal Glow", "Neon Pop",
        "Rock Velvet", "Lo-Fi Lounge", "Electronic", "Audiófilo Hi-Fi"
    )

    val deviceProfiles = listOf(
        "🎧 In-Ear Earbuds",
        "🦻 Over-Ear Bass",
        "🚗 Car Audio",
        "🎙️ Studio Flat",
        "🔊 Altavoz Móvil"
    )

    val bandFrequencies = EqualizerManager.TEN_BAND_FREQUENCIES
    val bandLevels = remember {
        mutableStateListOf<Float>().apply {
            for (i in 0 until 10) add(eqManager.bandLevels[i])
        }
    }

    var bassBoost by remember { mutableFloatStateOf(eqManager.bassStrength / 10f) }
    var virtualizer by remember { mutableFloatStateOf(eqManager.virtualizerStrength / 10f) }
    var loudness by remember { mutableFloatStateOf(eqManager.loudnessGain / 10f) }
    var crossfadeSeconds by remember { mutableIntStateOf(playlistManager.getCrossfadeSeconds()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Master Toggle Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aura Studio Effects",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isEqEnabled) "DSP 32-bit Float • 10 Bandas Activo" else "Procesamiento desactivado",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEqEnabled,
                    onCheckedChange = {
                        isEqEnabled = it
                        eqManager.setEnabled(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Headphone & Output Acoustic Device Profiles
        Text(
            text = "PERFILES ACÚSTICOS DE DISPOSITIVO (DOLBY / POWERAMP)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(deviceProfiles) { index, profile ->
                FilterChip(
                    selected = selectedProfile == index,
                    onClick = {
                        selectedProfile = index
                        when (index) {
                            0 -> { // In-Ear
                                eqManager.applyPreset(3) // Neon Pop
                                virtualizer = 60f
                                bassBoost = 45f
                            }
                            1 -> { // Over-Ear Bass
                                eqManager.applyPreset(1) // Cyber Bass
                                bassBoost = 85f
                                virtualizer = 50f
                            }
                            2 -> { // Car Audio
                                eqManager.applyPreset(4) // Rock
                                loudness = 40f
                                virtualizer = 30f
                            }
                            3 -> { // Studio Flat
                                eqManager.applyPreset(0) // Flat
                                bassBoost = 0f
                                virtualizer = 0f
                                loudness = 0f
                            }
                            4 -> { // Phone Speaker
                                eqManager.applyPreset(2) // Vocal
                                loudness = 80f
                                bassBoost = 20f
                            }
                        }
                        for (i in 0 until 10) bandLevels[i] = eqManager.bandLevels[i]
                        eqManager.setBassBoostStrength((bassBoost * 10).toInt().toShort())
                        eqManager.setVirtualizerStrength((virtualizer * 10).toInt().toShort())
                        eqManager.setLoudnessGain((loudness * 10).toInt())
                    },
                    label = { Text(profile, fontSize = 12.sp) },
                    enabled = isEqEnabled,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Presets Chips
        Text(
            text = "PRESETS DE ESTUDIO",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(presets) { index, preset ->
                FilterChip(
                    selected = selectedPreset == index,
                    onClick = {
                        selectedPreset = index
                        eqManager.applyPreset(index)
                        for (i in 0 until 10) {
                            bandLevels[i] = eqManager.bandLevels[i]
                        }
                        bassBoost = eqManager.bassStrength / 10f
                        virtualizer = eqManager.virtualizerStrength / 10f
                        loudness = eqManager.loudnessGain / 10f
                    },
                    label = { Text(preset) },
                    enabled = isEqEnabled,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 10-Band Graphic Equalizer
        Text(
            text = "ECUALIZADOR GRÁFICO PROFESIONAL (10 BANDAS)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                bandFrequencies.forEachIndexed { index, freq ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = freq,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(62.dp)
                        )
                        Slider(
                            value = bandLevels[index],
                            onValueChange = { newVal ->
                                bandLevels[index] = newVal
                                eqManager.setBandLevel(index, newVal)
                            },
                            valueRange = -12f..12f,
                            enabled = isEqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${if (bandLevels[index] > 0) "+" else ""}${bandLevels[index].toInt()} dB",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(48.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sound Enhancement Rack (Bass Boost, 3D Virtualizer, Loudness)
        Text(
            text = "RACK DE MEJORA ACÚSTICA",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Bass Boost
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Refuerzo Dinámico de Graves (Bass)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "${bassBoost.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    Slider(
                        value = bassBoost,
                        onValueChange = {
                            bassBoost = it
                            eqManager.setBassBoostStrength((it * 10).toInt().toShort())
                        },
                        valueRange = 0f..100f,
                        enabled = isEqEnabled,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary, activeTrackColor = MaterialTheme.colorScheme.secondary)
                    )
                }

                // 3D Virtualizer
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.SurroundSound, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Sonido Envolvente 3D (Virtualizer)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text(text = "${virtualizer.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Slider(
                        value = virtualizer,
                        onValueChange = {
                            virtualizer = it
                            eqManager.setVirtualizerStrength((it * 10).toInt().toShort())
                        },
                        valueRange = 0f..100f,
                        enabled = isEqEnabled,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.tertiary, activeTrackColor = MaterialTheme.colorScheme.tertiary)
                    )
                }

                // Loudness Boost
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Claridad y Ganancia (Loudness Boost)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Text(text = "${loudness.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }
                    Slider(
                        value = loudness,
                        onValueChange = {
                            loudness = it
                            eqManager.setLoudnessGain((it * 10).toInt())
                        },
                        valueRange = 0f..100f,
                        enabled = isEqEnabled,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFF59E0B), activeTrackColor = Color(0xFFF59E0B))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // DJ Crossfade Engine
        Text(
            text = "MOTOR DJ CROSSFADE & GAPLESS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Fundido Cruzado (Crossfade)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (crossfadeSeconds == 0) "Desactivado (Reproducción directa)" else "Transición suave de ${crossfadeSeconds}s entre canciones",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (crossfadeSeconds == 0) "OFF" else "${crossfadeSeconds}s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = crossfadeSeconds.toFloat(),
                    onValueChange = {
                        val sec = it.toInt()
                        crossfadeSeconds = sec
                        playlistManager.setCrossfadeSeconds(sec)
                    },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // ReplayGain Normalization
                var isReplayGain by remember { mutableStateOf(playlistManager.isReplayGainEnabled()) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Normalización de Volumen (ReplayGain)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Ajusta todas las pistas a un volumen uniforme sin saturación",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isReplayGain,
                        onCheckedChange = {
                            isReplayGain = it
                            playlistManager.setReplayGainEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Autoplay / Radio Mode
                var isAutoplay by remember { mutableStateOf(playlistManager.isAutoplayEnabled()) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Autoplay / Modo Radio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Continúa reproduciendo temas similares al terminar la lista",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAutoplay,
                        onCheckedChange = {
                            isAutoplay = it
                            playlistManager.setAutoplayEnabled(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cyber Theme Accent Picker (BlackPlayer Style)
        Text(
            text = "PERSONALIZACIÓN VISUAL (BLACKPLAYER STYLE)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Acento Neón Cyberpunk", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(14.dp))

                val accents = listOf(
                    Triple("PURPLE", "Velvet Purple", Color(0xFFA855F7)),
                    Triple("CYAN", "Matrix Cyan", Color(0xFF06B6D4)),
                    Triple("MAGENTA", "Neon Magenta", Color(0xFFF43F5E)),
                    Triple("GREEN", "Toxic Green", Color(0xFF10B981)),
                    Triple("GOLD", "Solar Gold", Color(0xFFF59E0B))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    accents.forEach { (key, label, color) ->
                        val isSelected = currentAccent.equals(key, ignoreCase = true)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    playlistManager.setThemeAccent(key)
                                    onAccentChange(key)
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = key,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
