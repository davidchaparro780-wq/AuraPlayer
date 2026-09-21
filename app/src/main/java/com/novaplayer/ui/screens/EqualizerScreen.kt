package com.novaplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun EqualizerScreen(
    modifier: Modifier = Modifier
) {
    var isEqEnabled by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableIntStateOf(0) }
    val presets = listOf("Normal", "Rock", "Pop", "Jazz", "Graves +", "Vocal", "Clásica")
    
    // Default 5-band EQ levels (-10dB to +10dB)
    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val bandLevels = remember { mutableStateListOf(0f, 2f, -1f, 3f, 1f) }
    var bassBoost by remember { mutableStateOf(40f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Master Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ecualizador y Efectos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEqEnabled) "Efectos activos" else "Desactivado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEqEnabled,
                    onCheckedChange = { isEqEnabled = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Presets Chips
        Text(
            text = "PRESETS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(presets) { index, preset ->
                FilterChip(
                    selected = selectedPreset == index,
                    onClick = {
                        selectedPreset = index
                        // Update band levels based on preset
                        when (index) {
                            0 -> for (i in bandLevels.indices) bandLevels[i] = 0f
                            1 -> { bandLevels[0] = 5f; bandLevels[1] = 3f; bandLevels[2] = -1f; bandLevels[3] = 3f; bandLevels[4] = 5f }
                            2 -> { bandLevels[0] = -1f; bandLevels[1] = 2f; bandLevels[2] = 5f; bandLevels[3] = 1f; bandLevels[4] = -2f }
                            3 -> { bandLevels[0] = 4f; bandLevels[1] = 2f; bandLevels[2] = -2f; bandLevels[3] = 2f; bandLevels[4] = 4f }
                            4 -> { bandLevels[0] = 8f; bandLevels[1] = 6f; bandLevels[2] = 2f; bandLevels[3] = 0f; bandLevels[4] = 0f }
                            5 -> { bandLevels[0] = -2f; bandLevels[1] = 0f; bandLevels[2] = 6f; bandLevels[3] = 2f; bandLevels[4] = -1f }
                            6 -> { bandLevels[0] = 4f; bandLevels[1] = 3f; bandLevels[2] = -2f; bandLevels[3] = 4f; bandLevels[4] = 3f }
                        }
                    },
                    label = { Text(preset) },
                    enabled = isEqEnabled
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bands Sliders
        Text(
            text = "BANDAS DE FRECUENCIA",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                bandFrequencies.forEachIndexed { index, freq ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = freq,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Slider(
                            value = bandLevels[index],
                            onValueChange = { bandLevels[index] = it },
                            valueRange = -10f..10f,
                            enabled = isEqEnabled,
                            modifier = Modifier.weight(3f)
                        )
                        Text(
                            text = "${bandLevels[index].toInt()} dB",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bass Boost Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Refuerzo de Graves (Bass Boost)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${bassBoost.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = bassBoost,
                    onValueChange = { bassBoost = it },
                    valueRange = 0f..100f,
                    enabled = isEqEnabled
                )
            }
        }
    }
}
