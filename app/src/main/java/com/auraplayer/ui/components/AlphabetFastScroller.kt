package com.auraplayer.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AlphabetFastScroller(
    letters: List<Char> = listOf('#') + ('A'..'Z').toList(),
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var selectedLetter by remember { mutableStateOf<Char?>(null) }
    var columnHeight by remember { mutableFloatStateOf(0f) }

    fun updateLetterAt(y: Float) {
        if (columnHeight <= 0f || letters.isEmpty()) return
        val clampedY = y.coerceIn(0f, columnHeight)
        val index = ((clampedY / columnHeight) * letters.size).toInt().coerceIn(0, letters.size - 1)
        val letter = letters[index]
        if (selectedLetter != letter) {
            selectedLetter = letter
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onLetterSelected(letter)
        }
    }

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating Letter Preview Bubble
        AnimatedVisibility(
            visible = selectedLetter != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.padding(end = 36.dp)
        ) {
            selectedLetter?.let { letter ->
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Slim Alphabet Rail
        Column(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.65f))
                .onGloballyPositioned { coordinates ->
                    columnHeight = coordinates.size.height.toFloat()
                }
                .pointerInput(letters) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            updateLetterAt(offset.y)
                        },
                        onDragEnd = {
                            selectedLetter = null
                        },
                        onDragCancel = {
                            selectedLetter = null
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            updateLetterAt(change.position.y)
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            letters.forEach { char ->
                val isCurrent = selectedLetter == char
                Text(
                    text = char.toString(),
                    fontSize = 9.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isCurrent) Color(0xFF38BDF8) else Color(0xFF94A3B8).copy(alpha = 0.8f)
                )
            }
        }
    }
}
