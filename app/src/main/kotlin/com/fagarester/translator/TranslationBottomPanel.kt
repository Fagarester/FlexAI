package com.fagarester.translator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun TranslationBottomPanel(
    activeRecordMode: String?,
    host: VoiceHost,
    chatMessages: List<ChatMessage>,
    currentEditingBubbleId: String?,
    onModeChange: (String?) -> Unit,
    onEditingIdChange: (String?) -> Unit,
    settingsStore: SettingsStore,
    onChunkReady: (String) -> Unit
) {
    val sideBase = Color(0xFF4A4458)
    val sideBrush = Brush.linearGradient(colors = listOf(sideBase, lerp(sideBase, Color.White, 0.25f)))

    val centerBase = Color(0xFF6750A4)
    val centerBrush = Brush.linearGradient(colors = listOf(centerBase, lerp(centerBase, Color.White, 0.22f)))

    val panelColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
        Color(0xFFE7DFEC)
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = panelColor,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MicButtonSlot(
            mode = "source",
            label = settingsStore.sourceLangName.substringBefore(" ("),
            languageCode = settingsStore.sourceLangCode,
            buttonSize = 54.dp,
            iconSize = 24.dp,
            shape = MaterialTheme.shapes.small,
            inactiveBrush = sideBrush,
            activeRecordMode = activeRecordMode,
            host = host,
            chatMessages = chatMessages,
            currentEditingBubbleId = currentEditingBubbleId,
            resolveSpeakLangCode = { settingsStore.targetLangCode },
            onModeChange = onModeChange,
            onEditingIdChange = onEditingIdChange,
            onChunkReady = onChunkReady
        )

        MicButtonSlot(
            mode = "auto",
            label = "Automatic",
            languageCode = null,
            buttonSize = 72.dp,
            iconSize = 34.dp,
            shape = RoundedCornerShape(50),
            inactiveBrush = centerBrush,
            activeRecordMode = activeRecordMode,
            host = host,
            chatMessages = chatMessages,
            currentEditingBubbleId = currentEditingBubbleId,
           resolveSpeakLangCode = { msg ->
                val isSourceLang = LanguageDetector.looksLikeSourceLanguage(msg.originalText, settingsStore.sourceLangCode, settingsStore.targetLangCode)
                if (isSourceLang) settingsStore.targetLangCode else settingsStore.sourceLangCode
            },
            onModeChange = onModeChange,
            onEditingIdChange = onEditingIdChange,
            onChunkReady = onChunkReady
        )

        MicButtonSlot(
            mode = "target",
            label = settingsStore.targetLangName.substringBefore(" ("),
            languageCode = settingsStore.targetLangCode,
            buttonSize = 54.dp,
            iconSize = 24.dp,
            shape = MaterialTheme.shapes.small,
            inactiveBrush = sideBrush,
            activeRecordMode = activeRecordMode,
            host = host,
            chatMessages = chatMessages,
            currentEditingBubbleId = currentEditingBubbleId,
            resolveSpeakLangCode = { settingsStore.sourceLangCode },
            onModeChange = onModeChange,
            onEditingIdChange = onEditingIdChange,
            onChunkReady = onChunkReady
        )
    }
}