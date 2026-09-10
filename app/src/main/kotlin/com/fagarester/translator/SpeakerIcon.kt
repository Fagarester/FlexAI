package com.fagarester.translator

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun SpeakerIcon(
    isMuted: Boolean,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val baseColor = Color(0xFF7968B0)

        drawSpeakerBase(baseColor)
        drawSpeakerWaves(baseColor)

        if (isMuted) {
            drawLine(
                color = backgroundColor,
                start = Offset(size.width * 0.10f, size.height * 0.10f),
                end = Offset(size.width * 0.90f, size.height * 0.90f),
                strokeWidth = size.width * 0.12f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = baseColor,
                start = Offset(size.width * 0.10f, size.height * 0.10f),
                end = Offset(size.width * 0.90f, size.height * 0.90f),
                strokeWidth = size.width * 0.07f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawSpeakerBase(baseColor: Color) {
    val speakerPath = Path().apply {
        moveTo(size.width * 0.08f, size.height * 0.38f)
        lineTo(size.width * 0.22f, size.height * 0.38f)
        cubicTo(
            size.width * 0.25f, size.height * 0.38f,
            size.width * 0.28f, size.height * 0.35f,
            size.width * 0.30f, size.height * 0.32f
        )
        lineTo(size.width * 0.46f, size.height * 0.12f)
        cubicTo(
            size.width * 0.51f, size.height * 0.06f,
            size.width * 0.52f, size.height * 0.14f,
            size.width * 0.52f, size.height * 0.20f
        )
        lineTo(size.width * 0.52f, size.height * 0.80f)
        cubicTo(
            size.width * 0.52f, size.height * 0.86f,
            size.width * 0.51f, size.height * 0.94f,
            size.width * 0.46f, size.height * 0.88f
        )
        lineTo(size.width * 0.30f, size.height * 0.68f)
        cubicTo(
            size.width * 0.28f, size.height * 0.65f,
            size.width * 0.25f, size.height * 0.62f,
            size.width * 0.22f, size.height * 0.62f
        )
        lineTo(size.width * 0.08f, size.height * 0.62f)
        cubicTo(
            size.width * 0.03f, size.height * 0.62f,
            size.width * 0.02f, size.height * 0.60f,
            size.width * 0.02f, size.height * 0.56f
        )
        lineTo(size.width * 0.02f, size.height * 0.44f)
        cubicTo(
            size.width * 0.02f, size.height * 0.40f,
            size.width * 0.03f, size.height * 0.38f,
            size.width * 0.08f, size.height * 0.38f
        )
        close()
    }
    drawPath(path = speakerPath, color = baseColor)
}

private fun DrawScope.drawSpeakerWaves(baseColor: Color) {
    val strokeStyle = Stroke(width = size.width * 0.07f, cap = StrokeCap.Round)

    val wave1 = Path().apply {
        arcTo(rect = Rect(left = size.width * 0.40f, top = size.height * 0.36f, right = size.width * 0.70f, bottom = size.height * 0.64f), startAngleDegrees = -45f, sweepAngleDegrees = 90f, forceMoveTo = true)
    }
    drawPath(path = wave1, color = baseColor, style = strokeStyle)

    val wave2 = Path().apply {
        arcTo(rect = Rect(left = size.width * 0.30f, top = size.height * 0.24f, right = size.width * 0.86f, bottom = size.height * 0.76f), startAngleDegrees = -55f, sweepAngleDegrees = 110f, forceMoveTo = true)
    }
    drawPath(path = wave2, color = baseColor, style = strokeStyle)

    val wave3 = Path().apply {
        arcTo(rect = Rect(left = size.width * 0.20f, top = size.height * 0.10f, right = size.width * 1.04f, bottom = size.height * 0.90f), startAngleDegrees = -62f, sweepAngleDegrees = 124f, forceMoveTo = true)
    }
    drawPath(path = wave3, color = baseColor, style = strokeStyle)
}