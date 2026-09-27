package com.auraplayer.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.repository.FavoritesManager
import com.auraplayer.data.repository.PlaylistManager
import com.auraplayer.data.repository.UserManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserProfileDialog(
    userManager: UserManager,
    favoritesManager: FavoritesManager,
    playlistManager: PlaylistManager,
    totalPlaysCount: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val user = userManager.currentUser ?: return

    val favoriteIds = remember { favoritesManager.getFavoriteIds() }
    val playlists = remember { playlistManager.getPlaylists() }

    val lastSyncFormatted = remember(user.lastSyncTime) {
        if (user.lastSyncTime > 0) {
            SimpleDateFormat("d MMM, HH:mm", Locale("es", "ES")).format(Date(user.lastSyncTime))
        } else "Nunca respaldado"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C101D),
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Glowing Avatar Circle
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF38BDF8))
                            )
                        )
                        .border(2.dp, Color(0xFFFFD700), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.avatarEmoji, fontSize = 38.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = user.name,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = user.email,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // VIP Member Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFD700).copy(alpha = 0.15f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFF59E0B))))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = user.memberTier,
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Stats Counter Row (Favoritos, Playlists, Plays)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Favoritos",
                        value = "${favoriteIds.size}",
                        icon = Icons.Default.Favorite,
                        iconColor = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Playlists",
                        value = "${playlists.size}",
                        icon = Icons.Default.QueueMusic,
                        iconColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Escuchas",
                        value = "$totalPlaysCount",
                        icon = Icons.Default.Headphones,
                        iconColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 2. Cloud Backup & Sync Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF151C33))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Respaldo & Sincronización", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Guarda tus canciones favoritas y listas para recuperarlas en cualquier momento:",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Último respaldo: $lastSyncFormatted",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val ok = userManager.backupUserData(favoritesManager.getFavoriteIds(), playlistManager.getPlaylists())
                                    if (ok) {
                                        Toast.makeText(context, "☁️ ¡Respaldo guardado con éxito!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Respaldar", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val restored = userManager.restoreUserData()
                                    if (restored != null) {
                                        favoritesManager.setFavorites(restored.first)
                                        playlistManager.savePlaylists(restored.second)
                                        Toast.makeText(context, "📥 ¡${restored.first.size} favoritos y ${restored.second.size} listas restauradas!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "No hay respaldos previos en esta cuenta", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF38BDF8))))
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restaurar", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Member info text
                Text(
                    text = "Miembro activo desde: ${user.memberSince}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        userManager.logout()
                        Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cerrar Sesión", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text("Cerrar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF151C33))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(title, fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
        }
    }
}
