package com.fagarester.translator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * Кнопка с градиентным фоном — визуальный аналог стандартной Material3
 * Button(), которая градиенты не поддерживает (только сплошной цвет).
 * API максимально похож на Button(), чтобы легко заменять точечно.
 */
/** Светлый цвет текста для наших фиксированных тёмно-фиолетовых кнопок/бабл-фонов —
 *  не завязан на тему, чтобы не терять контраст в светлой теме. */
val GradientContentColor = Color(0xFFE3D6FF)

@Composable
fun GradientButton(
    onClick: () -> Unit,
    brush: Brush,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = GradientContentColor,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(50)
    val disabledBrush = SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    Row(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) brush else disabledBrush)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) contentColor else disabledContentColor
        ) {
            content()
        }
    }
}

/** Стандартный градиент для кнопок — тот же визуальный язык, что и у остальных элементов приложения. */
@Composable
fun defaultButtonBrush(): Brush {
    val base = Color(0xFF4F378B)
    return Brush.linearGradient(colors = listOf(base, lerp(base, Color.White, 0.18f)))
}