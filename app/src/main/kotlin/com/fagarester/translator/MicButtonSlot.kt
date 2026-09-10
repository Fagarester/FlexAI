package com.fagarester.translator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MicButtonSlot(
    mode: String,
    label: String,
    languageCode: String?,
    buttonSize: Dp,
    iconSize: Dp,
    shape: Shape,
    inactiveBrush: Brush,
    activeRecordMode: String?,
    host: VoiceHost,
    chatMessages: List<ChatMessage>,
    currentEditingBubbleId: String?,
    resolveSpeakLangCode: (ChatMessage) -> String,
    onModeChange: (String?) -> Unit,
    onEditingIdChange: (String?) -> Unit,
    onChunkReady: (String) -> Unit
) {
    val isActive = activeRecordMode == mode

    fun speakEditedBubbleIfAny() {
        chatMessages.find { it.id == currentEditingBubbleId }?.let {
            if (it.translatedText.isNotEmpty()) {
                host.ttsHelper.speak(it.translatedText, resolveSpeakLangCode(it))
            }
        }
    }

    fun stopRecording() {
        host.speechHelper.stopListening()
        onModeChange(null)
        speakEditedBubbleIfAny()
        // onEditingIdChange(null) сюда сознательно НЕ ставим: у notune-движка
        // колбэк с распознанным текстом может прийти уже ПОСЛЕ нажатия "стоп"
        // (обработка ещё летит в фоне) — если обнулить id сразу, такой
        // запоздалый кусок текста создаст лишний, дублирующий пузырь вместо
        // дополнения уже существующего. currentEditingBubbleId и так надёжно
        // сбрасывается в startRecording() перед началом следующей записи.
    }

    fun startRecording() {
        host.ttsHelper.stop()
        onEditingIdChange(null)
        onModeChange(mode)
        host.speechHelper.startListening(
            languageCode = languageCode,
            onResult = { text ->
                host.recognizedTextState.value = text
            },
            onStatusChange = { status ->
              if (status.startsWith("Ошибка") || status.startsWith("Нажмите") || status.startsWith("Обработка") || status.startsWith("Конец")) {
                    if (status.startsWith("Обработка") && activeRecordMode == mode) {
                        speakEditedBubbleIfAny()
                    }
                    onModeChange(null)
                }
            },
            onChunkReady = onChunkReady
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(shape)
                .background(
                    if (isActive) SolidColor(MaterialTheme.colorScheme.error) else inactiveBrush
                )
                .clickable { if (isActive) stopRecording() else startRecording() },
            contentAlignment = Alignment.Center
        ) {
            MicrophoneIcon(modifier = Modifier.size(iconSize), color = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = if (mode == "auto") 13.sp else 12.sp,
            color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}