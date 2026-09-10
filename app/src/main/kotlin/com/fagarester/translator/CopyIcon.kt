package com.fagarester.translator

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Простая одноцветная векторная иконка "копировать" (два перекрывающихся
 * прямоугольника), выдержанная в стиле остальных кастомных иконок проекта
 * (SpeakerIcon, MicrophoneIcon) — вместо цветного emoji "📋".
 */
@Composable
fun CopyIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.11f
        val corner = CornerRadius(size.width * 0.14f, size.height * 0.14f)

        // Задний прямоугольник (верхний левый)
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.06f, size.height * 0.06f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.62f, size.height * 0.62f),
            cornerRadius = corner,
            style = Stroke(width = strokeWidth)
        )

        // Передний прямоугольник (нижний правый, перекрывает задний)
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.34f, size.height * 0.34f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.62f, size.height * 0.62f),
            cornerRadius = corner,
            style = Stroke(width = strokeWidth)
        )
    }
}