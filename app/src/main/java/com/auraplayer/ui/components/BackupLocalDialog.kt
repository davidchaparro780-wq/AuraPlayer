package com.auraplayer.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auraplayer.data.repository.BackupManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupLocalDialog(
    backupManager: BackupManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var lastExportPath by remember { mutableStateOf<String?>(null) }

    val isBusy = isExporting || isImporting

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            errorText = null
            isExporting = true
            scope.launch {
                val result = backupManager.exportBackup()
                if (result.isSuccess) {
                    val file = result.getOrNull()
                    if (file == null) {
                        errorText = "El archivo de copia no está disponible"
                    } else {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { output ->
                                file.inputStream().use { input ->
                                    input.copyTo(output)
                                }
                            }
                            lastExportPath = file.absolutePath
                            Toast.makeText(
                                context,
                                "Copia guardada en ${file.absolutePath}",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                            errorText = "No se pudo escribir el archivo en el destino elegido"
                        }
                    }
                } else {
                    val error = result.exceptionOrNull()
                    errorText = error?.message ?: "No se pudo crear la copia de seguridad"
                }
                isExporting = false
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            errorText = null
            isImporting = true
            scope.launch {
                val result = backupManager.importBackup(uri)
                if (result.isSuccess) {
                    val summary = result.getOrNull()
                    if (summary == null) {
                        errorText = "No se pudo leer el resumen de la copia"
                    } else {
                        lastExportPath = null
                        Toast.makeText(context, summary.toMessage(), Toast.LENGTH_LONG).show()
                    }
                } else {
                    val error = result.exceptionOrNull()
                    errorText = error?.message ?: "No se pudo restaurar la copia de seguridad"
                }
                isImporting = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isBusy) {
                onDismiss()
            }
        },
        containerColor = Color(0xFF0D1222),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Copia de seguridad local",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Guarda o restaura tus listas, favoritos y ajustes en un archivo de tu teléfono.",
                    color = Color(0xFFB8C4D9),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (!isBusy) {
                            errorText = null
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            exportLauncher.launch("aura_backup_$stamp.json")
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E5BFF),
                        disabledContainerColor = Color(0xFF1A2036)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Exportando...",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Exportar copia",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (!isBusy) {
                            errorText = null
                            importLauncher.launch(arrayOf("application/json"))
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1F2A44),
                        disabledContainerColor = Color(0xFF1A2036)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Importando...",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "Importar copia",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                val error = errorText
                if (error != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = error,
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp
                    )
                }

                if (error == null && lastExportPath != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Última copia: $lastExportPath",
                        color = Color(0xFF7C8AA5),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isBusy
                ) {
                    Text(
                        text = "Cerrar",
                        color = Color(0xFF8FA6FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}
