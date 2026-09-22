package com.auraplayer.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auraplayer.data.model.MediaModel
import com.auraplayer.data.repository.VaultItem
import com.auraplayer.data.repository.VaultManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    vaultManager: VaultManager,
    onBack: () -> Unit,
    onPlayHiddenVideo: (MediaModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Smooth Back Navigation: intercepts hardware/gesture back to return without quitting app
    BackHandler {
        onBack()
    }

    var isUnlocked by remember { mutableStateOf(false) }
    var isPinSet by remember { mutableStateOf(vaultManager.isPinSet()) }

    // PIN Setup / Entry state
    var enteredPin by remember { mutableStateOf("") }
    var setupFirstPin by remember { mutableStateOf("") }
    var setupStep by remember { mutableIntStateOf(0) } // 0: Enter Pin or Setup Step 1, 1: Confirm Setup Pin
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Restore media with PIN confirmation state
    var itemToRestoreWithPin by remember { mutableStateOf<VaultItem?>(null) }
    var restorePinEntered by remember { mutableStateOf("") }
    var restorePinError by remember { mutableStateOf<String?>(null) }

    // Vault contents state
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Videos, 1: Photos
    var vaultItems by remember { mutableStateOf<List<VaultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var selectedItemForMenu by remember { mutableStateOf<VaultItem?>(null) }
    var previewPhotoItem by remember { mutableStateOf<VaultItem?>(null) }

    fun refreshItems() {
        scope.launch {
            isLoading = true
            vaultItems = vaultManager.getVaultItems()
            isLoading = false
        }
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            refreshItems()
        }
    }

    var pendingVaultItemToDelete by remember { mutableStateOf<Pair<File, Uri?>?>(null) }
    val vaultDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pending = pendingVaultItemToDelete
        pendingVaultItemToDelete = null
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "🔒 Archivo ocultado de la galería y protegido en Bóveda", Toast.LENGTH_SHORT).show()
            refreshItems()
        } else {
            pending?.second?.let { uri ->
                val realPath = vaultManager.resolveRealPathFromUri(uri)
                if (realPath != null) vaultManager.unmarkPathAsHidden(realPath)
            }
            vaultManager.cleanVaultFile(pending?.first)
            Toast.makeText(context, "Cancelado: el archivo permanece en tu galería", Toast.LENGTH_SHORT).show()
        }
    }

    // Media Pickers to hide directly from Vault screen
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                val realPath = vaultManager.resolveRealPathFromUri(uri)
                if (realPath != null) {
                    vaultManager.markPathAsHidden(realPath)
                }
                val copiedVaultFile = vaultManager.copyMediaToVault(sourcePath = realPath, sourceUri = uri, isVideo = true)
                if (copiedVaultFile == null) {
                    if (realPath != null) vaultManager.unmarkPathAsHidden(realPath)
                    isLoading = false
                    Toast.makeText(context, "Error al copiar archivo a la Bóveda", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val deleted = vaultManager.deleteOriginalMedia(context, filePath = realPath, uri = uri, isVideo = true)
                isLoading = false
                if (deleted) {
                    Toast.makeText(context, "🔒 Video ocultado de la galería y protegido en Bóveda", Toast.LENGTH_SHORT).show()
                    refreshItems()
                } else {
                    val pendingIntent = vaultManager.getDeleteRequestPendingIntent(context, uri = uri, filePath = realPath, isVideo = true)
                    if (pendingIntent != null) {
                        pendingVaultItemToDelete = Pair(copiedVaultFile, uri)
                        vaultDeleteLauncher.launch(
                            androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    } else {
                        Toast.makeText(context, "Para borrar de la galería, activa el permiso de archivos", Toast.LENGTH_LONG).show()
                        vaultManager.openAllFilesAccessSettings(context)
                        refreshItems()
                    }
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                val realPath = vaultManager.resolveRealPathFromUri(uri)
                if (realPath != null) {
                    vaultManager.markPathAsHidden(realPath)
                }
                val copiedVaultFile = vaultManager.copyMediaToVault(sourcePath = realPath, sourceUri = uri, isVideo = false)
                if (copiedVaultFile == null) {
                    if (realPath != null) vaultManager.unmarkPathAsHidden(realPath)
                    isLoading = false
                    Toast.makeText(context, "Error al copiar foto a la Bóveda", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val deleted = vaultManager.deleteOriginalMedia(context, filePath = realPath, uri = uri, isVideo = false)
                isLoading = false
                if (deleted) {
                    Toast.makeText(context, "🔒 Foto ocultada de la galería y protegida en Bóveda", Toast.LENGTH_SHORT).show()
                    refreshItems()
                } else {
                    val pendingIntent = vaultManager.getDeleteRequestPendingIntent(context, uri = uri, filePath = realPath, isVideo = false)
                    if (pendingIntent != null) {
                        pendingVaultItemToDelete = Pair(copiedVaultFile, uri)
                        vaultDeleteLauncher.launch(
                            androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    } else {
                        Toast.makeText(context, "Para borrar de la galería, activa el permiso de archivos", Toast.LENGTH_LONG).show()
                        vaultManager.openAllFilesAccessSettings(context)
                        refreshItems()
                    }
                }
            }
        }
    }

    if (!isUnlocked) {
        // PIN LOCK / SETUP SCREEN
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF080B14))
                    )
                )
                .statusBarsPadding()
        ) {
            // Top Bar with Back Button and Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bóveda Privada",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // PIN Lock / Setup Content Centered in Remaining Space
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Glowing Shield / Lock Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFF38BDF8))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Bóveda Privada DaVE",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (!isPinSet) {
                        if (setupStep == 0) "Crea una clave de 4 dígitos para proteger tus fotos y videos"
                        else "Confirma tu clave de 4 dígitos"
                    } else {
                        "Ingresa tu clave de 4 dígitos para desbloquear"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(30.dp))

                // 4 PIN Dots Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = enteredPin.length > i
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) Color(0xFF38BDF8)
                                    else Color(0xFF1E293B)
                                )
                                .border(
                                    1.5.dp,
                                    if (isFilled) Color(0xFF8B5CF6) else Color(0xFF475569),
                                    CircleShape
                                )
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Numeric Keypad (0-9 + Backspace)
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "DEL")
                    )

                    keyRows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { digit ->
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (digit in listOf("C", "DEL")) Color(0xFF1E1B4B).copy(alpha = 0.5f)
                                            else Color(0xFF13182E)
                                        )
                                        .border(
                                            1.dp,
                                            Color(0xFF8B5CF6).copy(alpha = 0.35f),
                                            CircleShape
                                        )
                                        .clickable {
                                            when (digit) {
                                                "C" -> {
                                                    enteredPin = ""
                                                    errorMessage = null
                                                }
                                                "DEL" -> {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                        errorMessage = null
                                                    }
                                                }
                                                else -> {
                                                    if (enteredPin.length < 4) {
                                                        enteredPin += digit
                                                        errorMessage = null
                                                        if (enteredPin.length == 4) {
                                                            // Evaluate PIN
                                                            if (!isPinSet) {
                                                                if (setupStep == 0) {
                                                                    setupFirstPin = enteredPin
                                                                    setupStep = 1
                                                                    enteredPin = ""
                                                                } else {
                                                                    if (enteredPin == setupFirstPin) {
                                                                        vaultManager.setPin(enteredPin)
                                                                        isPinSet = true
                                                                        isUnlocked = true
                                                                        Toast.makeText(context, "Clave configurada exitosamente", Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        errorMessage = "Las claves no coinciden. Intenta de nuevo."
                                                                        setupStep = 0
                                                                        setupFirstPin = ""
                                                                        enteredPin = ""
                                                                    }
                                                                }
                                                            } else {
                                                                if (vaultManager.verifyPin(enteredPin)) {
                                                                    isUnlocked = true
                                                                } else {
                                                                    errorMessage = "Clave incorrecta"
                                                                    enteredPin = ""
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (digit == "DEL") {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Borrar",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Text(
                                            text = digit,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} else {
        // UNLOCKED VAULT GALLERY
        val hiddenVideos = remember(vaultItems) { vaultItems.filter { it.isVideo } }
        val hiddenPhotos = remember(vaultItems) { vaultItems.filter { !it.isVideo } }

        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bóveda Privada",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Lock Button to re-lock immediately
                    IconButton(
                        onClick = {
                            isUnlocked = false
                            enteredPin = ""
                            Toast.makeText(context, "Bóveda bloqueada", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Bloquear", tint = Color(0xFFEC4899))
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) {
                            videoPickerLauncher.launch("video/*")
                        } else {
                            photoPickerLauncher.launch("image/*")
                        }
                    },
                    containerColor = Color(0xFF8B5CF6),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedTab == 0) "+ Ocultar Video" else "+ Ocultar Foto",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Tabs: Videos vs Photos
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF13182E),
                    contentColor = Color(0xFF38BDF8)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "🎬 Videos Ocultos (${hiddenVideos.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "🖼️ Fotos Ocultas (${hiddenPhotos.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                // Storage Permission banner if not granted on Android 11+
                if (!vaultManager.hasAllFilesAccess()) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clickable { vaultManager.openAllFilesAccessSettings(context) },
                        color = Color(0xFF1E1B4B),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Permiso de Archivos Recomendado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Toca aquí para permitir que DaVE elimine automáticamente los videos de tu galería principal de Android.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    val currentList = if (selectedTab == 0) hiddenVideos else hiddenPhotos

                    if (currentList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Default.Videocam else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6).copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (selectedTab == 0) "No tienes videos ocultos aún." else "No tienes fotos ocultas aún.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Usa el botón + para ocultar archivos de tu galería",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(currentList, key = { it.id }) { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF13182E))
                                        .border(
                                            1.dp,
                                            Color(0xFF8B5CF6).copy(alpha = 0.3f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            if (item.isVideo) {
                                                // Play in Video Player
                                                val media = MediaModel(
                                                    id = item.id.hashCode().toLong(),
                                                    title = item.name,
                                                    artist = "Bóveda Oculta",
                                                    album = "Privado",
                                                    duration = 0,
                                                    uri = Uri.fromFile(item.file),
                                                    isVideo = true,
                                                    path = item.file.absolutePath
                                                )
                                                onPlayHiddenVideo(media)
                                            } else {
                                                previewPhotoItem = item
                                            }
                                        }
                                ) {
                                    if (item.isVideo) {
                                        // Video preview
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            VideoThumbnail(
                                                video = MediaModel(
                                                    id = item.id.hashCode().toLong(),
                                                    title = item.name,
                                                    artist = "",
                                                    album = "",
                                                    duration = 0,
                                                    uri = Uri.fromFile(item.file),
                                                    path = item.file.absolutePath,
                                                    isVideo = true
                                                ),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.6f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Reproducir",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // Photo preview
                                        AsyncImage(
                                            model = item.file,
                                            contentDescription = item.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Item Options Button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .clickable { selectedItemForMenu = item },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = "Opciones",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Title banner at bottom
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.75f))
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Item Action Dialog (Restore / Delete)
    if (selectedItemForMenu != null) {
        val item = selectedItemForMenu!!
        AlertDialog(
            onDismissRequest = { selectedItemForMenu = null },
            title = { Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(
                        onClick = {
                            itemToRestoreWithPin = item
                            restorePinEntered = ""
                            restorePinError = null
                            selectedItemForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🔓 Desocultar (Restaurar a Galería)", color = Color.White)
                        }
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                val ok = vaultManager.deletePermanently(item)
                                if (ok) {
                                    Toast.makeText(context, "Eliminado definitivamente", Toast.LENGTH_SHORT).show()
                                    refreshItems()
                                }
                                selectedItemForMenu = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🗑️ Eliminar definitivamente", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedItemForMenu = null }) {
                    Text("Cerrar")
                }
            },
            containerColor = Color(0xFF13182E),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // PIN Confirmation Dialog to Restore Media back to Public Gallery
    if (itemToRestoreWithPin != null) {
        val item = itemToRestoreWithPin!!
        AlertDialog(
            onDismissRequest = { itemToRestoreWithPin = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar con Clave", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ingresa tu clave de 4 dígitos para restaurar \"${item.name}\" a tu galería principal:",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // PIN Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < restorePinEntered.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) Color(0xFF38BDF8)
                                        else Color(0xFF334155)
                                    )
                                    .border(
                                        1.dp,
                                        if (isFilled) Color(0xFF38BDF8) else Color(0xFF64748B),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    if (restorePinError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = restorePinError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mini Keypad
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "⌫")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        keys.forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                row.forEach { key ->
                                    Button(
                                        onClick = {
                                            restorePinError = null
                                            when (key) {
                                                "C" -> restorePinEntered = ""
                                                "⌫" -> if (restorePinEntered.isNotEmpty()) restorePinEntered = restorePinEntered.dropLast(1)
                                                else -> {
                                                    if (restorePinEntered.length < 4) {
                                                        restorePinEntered += key
                                                        if (restorePinEntered.length == 4) {
                                                            if (vaultManager.verifyPin(restorePinEntered)) {
                                                                scope.launch {
                                                                    isLoading = true
                                                                    val ok = vaultManager.restoreMedia(item)
                                                                    isLoading = false
                                                                    if (ok) {
                                                                        Toast.makeText(context, "✅ Archivo restaurado en tu galería", Toast.LENGTH_SHORT).show()
                                                                        refreshItems()
                                                                    } else {
                                                                        Toast.makeText(context, "Error al restaurar archivo", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                    itemToRestoreWithPin = null
                                                                }
                                                            } else {
                                                                restorePinError = "Clave incorrecta"
                                                                restorePinEntered = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(key, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { itemToRestoreWithPin = null }) {
                    Text("Cancelar", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF13182E),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Fullscreen Photo Viewer Dialog
    if (previewPhotoItem != null) {
        AlertDialog(
            onDismissRequest = { previewPhotoItem = null },
            confirmButton = {},
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = previewPhotoItem!!.file,
                        contentDescription = previewPhotoItem!!.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            containerColor = Color.Black,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
