package com.fagarester.translator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MicrophoneIcon(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF8A80B4)
) {
    Canvas(modifier = modifier.size(40.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.08f

        val micWidth = w * 0.24f
        val micHeight = h * 0.45f
        val micLeft = (w - micWidth) / 2
        val micTop = h * 0.18f

        drawRoundRect(
            color = color,
            topLeft = Offset(micLeft, micTop),
            size = Size(micWidth, micHeight),
            cornerRadius = CornerRadius(micWidth / 2, micWidth / 2)
        )

        val cupRadius = w * 0.22f
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w / 2 - cupRadius, h * 0.28f),
            size = Size(cupRadius * 2, cupRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        drawLine(
            color = color,
            start = Offset(w / 2, h * 0.73f),
            end = Offset(w / 2, h * 0.83f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.35f, h * 0.83f),
            end = Offset(w * 0.65f, h * 0.83f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}