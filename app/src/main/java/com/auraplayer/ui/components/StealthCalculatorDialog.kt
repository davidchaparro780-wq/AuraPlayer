package com.auraplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.auraplayer.audio.AuraHaptic

/**
 * Modo Camuflaje: Calculadora Secreta (Stealth Panic Calculator).
 * Transforma instantáneamente la pantalla en una calculadora estándar funcional.
 * Si el usuario introduce la clave PIN ("1234=" o "0000="), la app regresa silenciosamente al reproductor.
 */
@Composable
fun StealthCalculatorDialog(
    onUnlock: () -> Unit
) {
    val view = LocalView.current
    var displayText by remember { mutableStateOf("0") }
    var expressionHistory by remember { mutableStateOf("") }
    var inputSequence by remember { mutableStateOf("") }

    fun onKeyClick(key: String) {
        AuraHaptic.tick(view)
        inputSequence += key

        // Claves maestras para desbloquear
        if (inputSequence.endsWith("1234=") || inputSequence.endsWith("0000=") || inputSequence.endsWith("9999=")) {
            AuraHaptic.heavy(view)
            onUnlock()
            return
        }

        when (key) {
            "C" -> {
                displayText = "0"
                expressionHistory = ""
                inputSequence = ""
            }
            "+", "-", "×", "÷" -> {
                expressionHistory = "$displayText $key"
                displayText = "0"
            }
            "=" -> {
                try {
                    val parts = expressionHistory.split(" ")
                    if (parts.size >= 2) {
                        val num1 = parts[0].toDoubleOrNull() ?: 0.0
                        val op = parts[1]
                        val num2 = displayText.toDoubleOrNull() ?: 0.0
                        val res = when (op) {
                            "+" -> num1 + num2
                            "-" -> num1 - num2
                            "×" -> num1 * num2
                            "÷" -> if (num2 != 0.0) num1 / num2 else 0.0
                            else -> num2
                        }
                        expressionHistory = "$expressionHistory $displayText ="
                        displayText = if (res % 1.0 == 0.0) res.toLong().toString() else String.format("%.2f", res)
                    }
                } catch (_: Exception) {
                    displayText = "0"
                }
            }
            "+/-" -> {
                if (displayText != "0") {
                    displayText = if (displayText.startsWith("-")) displayText.drop(1) else "-$displayText"
                }
            }
            "%" -> {
                val num = displayText.toDoubleOrNull() ?: 0.0
                displayText = (num / 100.0).toString()
            }
            "." -> {
                if (!displayText.contains(".")) {
                    displayText += "."
                }
            }
            else -> {
                if (displayText == "0") displayText = key
                else if (displayText.length < 10) displayText += key
            }
        }
    }

    Dialog(
        onDismissRequest = {}, // Bloqueado hasta ingresar PIN
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Pantalla LCD de la calculadora
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 40.dp, bottom = 20.dp, end = 12.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expressionHistory,
                        color = Color(0xFF64748B),
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayText,
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }

                // Teclado numérico estándar
                val buttons = listOf(
                    listOf("C", "+/-", "%", "÷"),
                    listOf("7", "8", "9", "×"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "=", "")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    buttons.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { btn ->
                                if (btn.isNotEmpty()) {
                                    val isOp = btn in listOf("÷", "×", "-", "+", "=")
                                    val isSpecial = btn in listOf("C", "+/-", "%")
                                    val bg = when {
                                        btn == "=" -> Color(0xFF38BDF8)
                                        isOp -> Color(0xFF8B5CF6)
                                        isSpecial -> Color(0xFF334155)
                                        else -> Color(0xFF1E293B)
                                    }
                                    val textColor = when {
                                        btn == "=" -> Color(0xFF0F172A)
                                        isOp -> Color.White
                                        isSpecial -> Color(0xFF38BDF8)
                                        else -> Color.White
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(if (btn == "0") 2.1f else 1f)
                                            .aspectRatio(if (btn == "0") 2.1f else 1f)
                                            .clip(CircleShape)
                                            .background(bg)
                                            .clickable { onKeyClick(btn) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = btn,
                                            color = textColor,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
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
}
