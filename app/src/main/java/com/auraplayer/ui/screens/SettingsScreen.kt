package com.auraplayer.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.audio.AuraHaptic
import com.auraplayer.audio.CrossfadeManager
import com.auraplayer.data.repository.PlaylistManager
import com.auraplayer.data.repository.SettingsManager
import com.auraplayer.data.repository.VaultManager

@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    onRescanLibrary: () -> Unit,
    onCheckUpdate: () -> Unit,
    isCheckingUpdate: Boolean = false,
    appVersion: String = "2.6.0"
) {
    val context = LocalContext.current
    val settings = remember { SettingsManager.getInstance(context) }
    val playlistManager = remember { PlaylistManager.getInstance(context) }
    val crossfadeManager = remember { CrossfadeManager.getInstance(context) }
    val vaultManager = remember { VaultManager(context) }

    var currentCrossfadeSec by remember { mutableIntStateOf(playlistManager.getCrossfadeSeconds()) }
    var showCrossfadeDialog by remember { mutableStateOf(false) }
    var isBiometricVaultEnabled by remember { mutableStateOf(vaultManager.isBiometricEnabled()) }

    var showShortAudioDialog by remember { mutableStateOf(false) }
    var showSensitivityDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A12))
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        AuraHaptic.click(null)
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF131A2A))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ajustes & Confort",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personaliza tu experiencia DaVE Player",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v$appVersion",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Audio & Reproducción
                item {
                    SettingsSectionHeader(title = "AUDIO & REPRODUCCIÓN", icon = Icons.Default.Audiotrack)
                }

                item {
                    SettingsActionCard(
                        title = "Fundido Cruzado (Crossfade)",
                        subtitle = if (currentCrossfadeSec == 0) "Desactivado (Reproducción directa)" else "Transición suave de ${currentCrossfadeSec}s entre canciones",
                        icon = Icons.Default.GraphicEq,
                        actionText = if (currentCrossfadeSec == 0) "OFF" else "${currentCrossfadeSec}s",
                        onClick = {
                            AuraHaptic.click(null)
                            showCrossfadeDialog = true
                        }
                    )
                }

                item {
                    SettingsToggleCard(
                        title = "Pausa y reanudación suave (Fade In/Out)",
                        subtitle = "Sube y baja el volumen gradualmente sin cortes bruscos",
                        icon = Icons.Default.VolumeUp,
                        checked = settings.fadePlayback,
                        onCheckedChange = {
                            AuraHaptic.click(null)
                            settings.setFadePlaybackEnabled(it)
                        }
                    )
                }

                item {
                    SettingsToggleCard(
                        title = "Pausar al desconectar audífonos",
                        subtitle = "Detiene la música al desconectar auriculares o Bluetooth",
                        icon = Icons.Default.Headphones,
                        checked = settings.pauseOnHeadphonesDisconnect,
                        onCheckedChange = {
                            AuraHaptic.click(null)
                            settings.setPauseOnHeadphonesDisconnectEnabled(it)
                        }
                    )
                }

                item {
                    SettingsToggleCard(
                        title = "Reanudar al conectar audífonos",
                        subtitle = "Continúa reproduciendo automáticamente al conectar audífonos",
                        icon = Icons.Default.TouchApp,
                        checked = settings.resumeOnHeadphonesConnect,
                        onCheckedChange = {
                            AuraHaptic.click(null)
                            settings.setResumeOnHeadphonesConnectEnabled(it)
                        }
                    )
                }

                // Section 2: Filtro de Biblioteca
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsSectionHeader(title = "BIBLIOTECA & LIMPIEZA", icon = Icons.Default.GraphicEq)
                }

                item {
                    SettingsActionCard(
                        title = "Ocultar audios cortos (Notas de voz)",
                        subtitle = when (settings.hideShortAudioDurationSec) {
                            0 -> "Desactivado (mostrar todos los audios)"
                            else -> "Ocultar audios de menos de ${settings.hideShortAudioDurationSec} segundos"
                        },
                        icon = Icons.Default.Settings,
                        actionText = if (settings.hideShortAudioDurationSec == 0) "Desactivado" else "${settings.hideShortAudioDurationSec}s",
                        onClick = {
                            AuraHaptic.click(null)
                            showShortAudioDialog = true
                        }
                    )
                }

                item {
                    SettingsActionCard(
                        title = "Re-escanear biblioteca musical",
                        subtitle = "Busca nuevas canciones descargadas o añadidas al dispositivo",
                        icon = Icons.Default.Refresh,
                        actionText = "Escanear",
                        onClick = {
                            AuraHaptic.heavy(null)
                            onRescanLibrary()
                        }
                    )
                }

                // Section 3: Gestos & Pantalla
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsSectionHeader(title = "GESTOS & PANTALLA", icon = Icons.Default.TouchApp)
                }

                item {
                    SettingsToggleCard(
                        title = "Vibración Háptica",
                        subtitle = "Respuesta táctil suave en botones, deslizamiento y listas",
                        icon = Icons.Default.Vibration,
                        checked = settings.hapticFeedbackEnabled,
                        onCheckedChange = {
                            AuraHaptic.click(null)
                            settings.setHapticEnabled(it)
                        }
                    )
                }

                item {
                    SettingsToggleCard(
                        title = "Mantener pantalla encendida en reproductor",
                        subtitle = "Evita que la pantalla se bloquee mientras miras el reproductor",
                        icon = Icons.Default.ScreenLockPortrait,
                        checked = settings.keepScreenOnInPlayer,
                        onCheckedChange = {
                            AuraHaptic.click(null)
                            settings.setKeepScreenOnInPlayerEnabled(it)
                        }
                    )
                }

                // Section 4: Visual & Visualizador
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsSectionHeader(title = "INTERFAZ & VISUALIZADOR", icon = Icons.Default.ColorLens)
                }

                item {
                    SettingsToggleCard(
                        title = "Fondo dinámico adaptativo",
                        subtitle = "Extrae los colores de la carátula para ambientar el reproductor",
                        icon = Icons.Default.ColorLens,
                        checked = settings.dynamicPaletteEnabled,
                        onCheckedChange = {
                            AuraHaptic.click(null)
                            settings.setDynamicPalette(it)
                        }
                    )
                }

                item {
                    SettingsActionCard(
                        title = "Sensibilidad del visualizador de audio",
                        subtitle = when (settings.visualizerSensitivity) {
                            0.6f -> "Suave (Reacción moderada)"
                            1.5f -> "Intensa (Máxima potencia)"
                            else -> "Normal (Equilibrada)"
                        },
                        icon = Icons.Default.GraphicEq,
                        actionText = when (settings.visualizerSensitivity) {
                            0.6f -> "Suave"
                            1.5f -> "Intensa"
                            else -> "Normal"
                        },
                        onClick = {
                            AuraHaptic.click(null)
                            showSensitivityDialog = true
                        }
                    )
                }

                // Section 5: Seguridad & Bóveda
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsSectionHeader(title = "SEGURIDAD & BÓVEDA", icon = Icons.Default.Security)
                }

                item {
                    SettingsToggleCard(
                        title = "Desbloqueo Biométrico (Huella Digital)",
                        subtitle = "Acceso instantáneo a tu bóveda con lector de huella o biometría",
                        icon = Icons.Default.Fingerprint,
                        checked = isBiometricVaultEnabled,
                        onCheckedChange = {
                            AuraHaptic.click(null)
                            vaultManager.setBiometricEnabled(it)
                            isBiometricVaultEnabled = it
                            Toast.makeText(
                                context,
                                if (it) "Huella digital habilitada para la Bóveda" else "Huella digital deshabilitada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                item {
                    SettingsActionCard(
                        title = "Bóveda Señuelo (Decoy PIN)",
                        subtitle = "El PIN 0000 abre una bóveda camuflada vacía para máxima privacidad",
                        icon = Icons.Default.Shield,
                        actionText = "PIN: 0000",
                        onClick = {
                            AuraHaptic.click(null)
                            Toast.makeText(
                                context,
                                "Si alguien te obliga a abrir la bóveda, introduce '0000' para mostrarla completamente vacía.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }

                // Section 6: Notificaciones & Actualizaciones
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsSectionHeader(title = "NOTIFICACIONES & ACTUALIZACIONES", icon = Icons.Default.Notifications)
                }

                item {
                    SettingsToggleCard(
                        title = "Notificaciones de nuevas versiones",
                        subtitle = "Recibe una alerta en el celular cuando salga una nueva actualización de DaVE",
                        icon = Icons.Default.Notifications,
                        checked = settings.notifyUpdates,
                        onCheckedChange = {
                            AuraHaptic.click(null)
                            settings.setNotifyUpdatesEnabled(it)
                        }
                    )
                }

                item {
                    SettingsActionCard(
                        title = "Buscar actualizaciones ahora",
                        subtitle = "Comprobar si hay una nueva versión disponible en GitHub",
                        icon = Icons.Default.SystemUpdate,
                        actionText = if (isCheckingUpdate) "Buscando..." else "Comprobar",
                        isLoading = isCheckingUpdate,
                        onClick = {
                            AuraHaptic.heavy(null)
                            onCheckUpdate()
                        }
                    )
                }

                // Section 6: Acerca de DaVE Player
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsSectionHeader(title = "ACERCA DE", icon = Icons.Default.Info)
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101626)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "D",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "DaVE Player Ultra",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Versión $appVersion • Desarrollado con 💙 por David",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Reproductor de música de alta fidelidad con visualizador de audio FFT en tiempo real, síntesis acústica de relajación y motor sensorial táctil.",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Modal para seleccionar tiempo de filtro de audios cortos
    if (showShortAudioDialog) {
        val options = listOf(
            0 to "Desactivado (mostrar todo)",
            30 to "Ocultar audios < 30 segundos (Recomendado)",
            45 to "Ocultar audios < 45 segundos",
            60 to "Ocultar audios < 60 segundos",
            90 to "Ocultar audios < 90 segundos"
        )

        AlertDialog(
            onDismissRequest = { showShortAudioDialog = false },
            title = {
                Text(
                    text = "🧹 Ocultar audios cortos",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Filtra notas de voz de WhatsApp, audios de mensajería y tonos breves para dejar tu biblioteca musical limpia:",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    options.forEach { (sec, label) ->
                        val isSelected = settings.hideShortAudioDurationSec == sec
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.15f) else Color(0xFF131A2A))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    AuraHaptic.click(null)
                                    settings.setHideShortAudioSec(sec)
                                    showShortAudioDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShortAudioDialog = false }) {
                    Text("Cerrar", color = Color(0xFF38BDF8))
                }
            },
            containerColor = Color(0xFF101626),
            tonalElevation = 8.dp
        )
    }

    // Modal para seleccionar sensibilidad del visualizador
    if (showSensitivityDialog) {
        val options = listOf(
            0.6f to "🌊 Suave (Movimiento relajado)",
            1.0f to "⚡ Normal (Respuesta equilibrada)",
            1.5f to "🔥 Intensa (Máxima potencia reactiva)"
        )

        AlertDialog(
            onDismissRequest = { showSensitivityDialog = false },
            title = {
                Text(
                    text = "🌊 Sensibilidad del Visualizador",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ajusta la intensidad y elevación con la que las ondas y barras reaccionan al audio en tiempo real:",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    options.forEach { (value, label) ->
                        val isSelected = settings.visualizerSensitivity == value
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.15f) else Color(0xFF131A2A))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    AuraHaptic.click(null)
                                    settings.setVisualizerSensitivityValue(value)
                                    showSensitivityDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSensitivityDialog = false }) {
                    Text("Cerrar", color = Color(0xFF38BDF8))
                }
            },
            containerColor = Color(0xFF101626),
            tonalElevation = 8.dp
        )
    }

    // Modal para seleccionar tiempo de Crossfade (Fundido Cruzado)
    if (showCrossfadeDialog) {
        val crossfadeOptions = listOf(
            0 to "🚫 Desactivado (Reproducción directa)",
            2 to "🎵 2 segundos (Transición suave)",
            4 to "✨ 4 segundos (Recomendado)",
            6 to "🔥 6 segundos (Estilo DJ)",
            8 to "🌊 8 segundos (Fundido profundo)",
            10 to "🎧 10 segundos (Máxima transición)"
        )

        AlertDialog(
            onDismissRequest = { showCrossfadeDialog = false },
            title = {
                Text(
                    text = "🎛️ Fundido Cruzado (Crossfade)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Elige cuántos segundos se mezclarán el final de la canción actual con el inicio de la siguiente:",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    crossfadeOptions.forEach { (seconds, label) ->
                        val isSelected = currentCrossfadeSec == seconds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.15f) else Color(0xFF131A2A))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    AuraHaptic.click(null)
                                    currentCrossfadeSec = seconds
                                    playlistManager.setCrossfadeSeconds(seconds)
                                    crossfadeManager.crossfadeSeconds = seconds.coerceIn(1, 10)
                                    crossfadeManager.isCrossfadeEnabled = seconds > 0
                                    showCrossfadeDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCrossfadeDialog = false }) {
                    Text("Cerrar", color = Color(0xFF38BDF8))
                }
            },
            containerColor = Color(0xFF101626),
            tonalElevation = 8.dp
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = Color(0xFF38BDF8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101626)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF38BDF8),
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFF1E293B)
                )
            )
        }
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionText: String,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101626)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF38BDF8),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = actionText,
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
