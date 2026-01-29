package com.cosmic_struck.stellar.stellar.scantext.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun RectangleFrame(modifier: Modifier = Modifier) {

    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 6.dp.toPx() // Thicker for better visibility
        val cornerLength = 50.dp.toPx()
        val frameColor = Color(0xFF00E5FF) // Neon Cyan

        // Calculate frame dimensions (e.g., 70% of the smaller screen dimension)
        val frameSize = minOf(size.width, size.height) * 0.7f

        val left = (size.width - frameSize) / 2
        val top = (size.height - frameSize) / 2
        val right = left + frameSize
        val bottom = top + frameSize

        // Top-Left Corner
        drawLine(
            color = frameColor,
            start = Offset(left, top),
            end = Offset(left + cornerLength, top),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = frameColor,
            start = Offset(left, top),
            end = Offset(left, top + cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Top-Right Corner
        drawLine(
            color = frameColor,
            start = Offset(right, top),
            end = Offset(right - cornerLength, top),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = frameColor,
            start = Offset(right, top),
            end = Offset(right, top + cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom-Left Corner
        drawLine(
            color = frameColor,
            start = Offset(left, bottom),
            end = Offset(left + cornerLength, bottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = frameColor,
            start = Offset(left, bottom),
            end = Offset(left, bottom - cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom-Right Corner
        drawLine(
            color = frameColor,
            start = Offset(right, bottom),
            end = Offset(right - cornerLength, bottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = frameColor,
            start = Offset(right, bottom),
            end = Offset(right, bottom - cornerLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}