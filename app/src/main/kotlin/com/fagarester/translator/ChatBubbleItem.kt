package com.fagarester.translator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    isGenerating: Boolean,
    onBubbleClick: () -> Unit,
    onRetranslate: () -> Unit,
    onDelete: () -> Unit,
    settingsStore: SettingsStore
) {
    val alignment = if (message.isFromRight) Alignment.CenterEnd else Alignment.CenterStart
    val clipboardManager = LocalClipboardManager.current

    // Фиксированные базовые оттенки (не завязаны на тему) — но степень
    // осветления разная: в тёмной теме бабл темнее, в светлой — светлее,
    // чтобы гармонировать с фоном каждой темы.
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bubbleBrush = if (message.isFromRight) {
        val base = Color(0xFF4D3F73)
        if (isLightTheme)
            androidx.compose.ui.graphics.SolidColor(lerp(base, Color.White, 0.3f))
        else
            Brush.linearGradient(colors = listOf(base, lerp(base, Color.White, 0.14f)))
    } else {
        val base = Color(0xFF4F378B)
        if (isLightTheme)
            androidx.compose.ui.graphics.SolidColor(lerp(base, Color.White, 0.3f))
        else
            Brush.linearGradient(colors = listOf(base, lerp(base, Color.White, 0.28f)))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (message.isFromRight) Alignment.End else Alignment.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .wrapContentWidth(align = if (message.isFromRight) Alignment.End else Alignment.Start)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromRight) 16.dp else 4.dp,
                        bottomEnd = if (message.isFromRight) 4.dp else 16.dp
                    )
                )
                .background(bubbleBrush)
                .clickable { onBubbleClick() }
                .padding(14.dp)
        ) {
            Text(
                text = message.originalText,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isGenerating && message.translatedText.isEmpty()) {
                Text(
                    text = "AI переводит...",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            } else {
                Text(
                    text = message.translatedText,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            if (message.translatedText.isNotEmpty() &&
                !message.translatedText.startsWith("Ошибка") &&
                (settingsStore.showMetricTotal || settingsStore.showMetricModel || settingsStore.showMetricFrontend)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hudElements = mutableListOf<String>()

                    if (settingsStore.showMetricTotal) {
                        hudElements.add("Total: ${message.metricTotal} ms")
                    }
                    if (settingsStore.showMetricModel) {
                        hudElements.add("Model: ${message.metricModel} ms")
                    }
                    if (settingsStore.showMetricFrontend) {
                        hudElements.add("Frontend: ${message.metricFrontend} ms")
                    }
                    if (message.metricBackend.isNotEmpty()) {
                        hudElements.add(message.metricBackend)
                    }

                    Text(
                        text = hudElements.joinToString("   |   "),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    clipboardManager.setText(
                        AnnotatedString("${message.originalText}\n${message.translatedText}")
                    )
                },
                modifier = Modifier.size(32.dp)
            ) {
                CopyIcon(
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onRetranslate,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Перевести заново",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}