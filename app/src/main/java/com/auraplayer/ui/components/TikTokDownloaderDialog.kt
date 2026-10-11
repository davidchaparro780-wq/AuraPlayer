package com.auraplayer.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auraplayer.audio.AuraHaptic
import com.auraplayer.data.repository.TikTokDownloader
import com.auraplayer.data.repository.TikTokVideoDetails
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.launch

/**
 * Diálogo interactivo para descargar videos de TikTok sin marca de agua con un solo link.
 */
@Composable
fun TikTokDownloaderDialog(
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onDownloadCompleted: (Uri, Boolean) -> Unit = { _, _ -> },
    onPlayDownloadedVideo: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var urlInput by remember { mutableStateOf(initialUrl) }
    var isResolving by remember { mutableStateOf(false) }
    var resolvedDetails by remember { mutableStateOf<TikTokVideoDetails?>(null) }
    var resolveError by remember { mutableStateOf<String?>(null) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var completedUri by remember { mutableStateOf<Uri?>(null) }
    var wasVideoDownloaded by remember { mutableStateOf(true) }

    val tiktokCyan = Color(0xFF00F2FE)
    val tiktokPink = Color(0xFFFE2C55)
    val tiktokGradient = Brush.horizontalGradient(listOf(tiktokCyan, tiktokPink))

    fun resolveLink(targetUrl: String) {
        val clean = targetUrl.trim()
        if (clean.isBlank()) {
            Toast.makeText(context, "Pega un enlace de TikTok primero", Toast.LENGTH_SHORT).show()
            return
        }
        keyboardController?.hide()
        isResolving = true
        resolveError = null
        resolvedDetails = null
        completedUri = null

        scope.launch {
            val result = TikTokDownloader.resolveTikTok(clean)
            isResolving = false
            result.onSuccess { details ->
                resolvedDetails = details
            }.onFailure { err ->
                resolveError = err.message ?: "No se pudo extraer el video. Verifica el enlace."
            }
        }
    }

    // Auto-detección del portapapeles si initialUrl está vacío
    LaunchedEffect(Unit) {
        if (initialUrl.isNotBlank()) {
            resolveLink(initialUrl)
        } else {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
            if (!clip.isNullOrBlank() && TikTokDownloader.isTikTokUrl(clip)) {
                urlInput = clip.trim()
                resolveLink(clip.trim())
            }
        }
    }

    fun startDownload(asAudioOnly: Boolean) {
        val details = resolvedDetails ?: return
        val downloadUrl = if (asAudioOnly) {
            details.audioUrl ?: details.videoNoWatermarkUrl
        } else {
            details.videoNoWatermarkUrl
        }

        if (downloadUrl.isBlank()) {
            Toast.makeText(context, "Enlace de descarga no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        isDownloading = true
        downloadProgress = 0
        wasVideoDownloaded = !asAudioOnly

        scope.launch {
            val result = TikTokDownloader.downloadMedia(
                context = context,
                downloadUrl = downloadUrl,
                details = details,
                asAudioOnly = asAudioOnly,
                onProgress = { pct ->
                    downloadProgress = pct
                }
            )

            isDownloading = false
            result.onSuccess { uri ->
                completedUri = uri
                Toast.makeText(
                    context,
                    if (asAudioOnly) "🎵 Audio guardado en Música" else "🎬 Video sin marca guardado en Videos",
                    Toast.LENGTH_LONG
                ).show()
                onDownloadCompleted(uri, !asAudioOnly)
            }.onFailure { err ->
                Toast.makeText(context, "Error al descargar: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        containerColor = Color(0xFF0F1424),
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(tiktokGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "TikTok Sin Marca",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Descargador HD Libre de Agua",
                            color = tiktokCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(onClick = onDismiss, enabled = !isDownloading) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Input de URL con botón pegar
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = {
                        Text("Pega el enlace de TikTok aquí...", fontSize = 12.sp, color = Color(0xFF64748B))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tiktokCyan,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFF161E36),
                        unfocusedContainerColor = Color(0xFF161E36),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 2,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (urlInput.isNotBlank()) {
                                IconButton(onClick = { urlInput = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Borrar",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                                    if (!clip.isNullOrBlank()) {
                                        urlInput = clip.trim()
                                        resolveLink(clip.trim())
                                    } else {
                                        Toast.makeText(context, "Portapapeles vacío", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Pegar",
                                    tint = tiktokPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { resolveLink(urlInput) })
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Botón Resolver si no está resuelto
                if (resolvedDetails == null && !isResolving) {
                    Button(
                        onClick = { resolveLink(urlInput) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        enabled = urlInput.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analizar Video", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Indicador de resolución
                if (isResolving) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = tiktokCyan,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Extrayendo enlace limpio sin marca...",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Error
                if (resolveError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = resolveError ?: "", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                    }
                }

                // Tarjeta de video resuelto
                AnimatedVisibility(
                    visible = resolvedDetails != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    resolvedDetails?.let { details ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF13182E))
                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cover thumbnail
                                if (details.coverUrl.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black)
                                    ) {
                                        AsyncImage(
                                            model = details.coverUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(72.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF00F2FE).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "✨ SIN MARCA DE AGUA",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = details.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${details.username} (${details.author})",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Estado de descarga en progreso
                            if (isDownloading) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (wasVideoDownloaded) "Descargando video HD..." else "Descargando audio...",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = "$downloadProgress%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tiktokCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { downloadProgress / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = tiktokCyan,
                                        trackColor = Color(0xFF1E293B)
                                    )
                                }
                            } else if (completedUri != null) {
                                // Descarga completada con éxito
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "¡Descargado con éxito!",
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (wasVideoDownloaded && onPlayDownloadedVideo != null) {
                                        Button(
                                            onClick = {
                                                completedUri?.let { uri ->
                                                    onPlayDownloadedVideo(uri)
                                                    onDismiss()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                        ) {
                                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Reproducir Video Ahora", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                // Botones de descarga
                                Column {
                                    // Botón Video Sin Marca
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(tiktokGradient)
                                            .clickable {
                                                AuraHaptic.click(view)
                                                startDownload(asAudioOnly = false)
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.VideoFile,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Descargar Video Sin Marca (HD)",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Botón Solo Audio MP3
                                    if (!details.audioUrl.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF1E293B))
                                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                                                .clickable {
                                                    AuraHaptic.click(view)
                                                    startDownload(asAudioOnly = true)
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = tiktokCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Descargar Solo Audio (MP3)",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
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
        },
        confirmButton = {}
    )
}
