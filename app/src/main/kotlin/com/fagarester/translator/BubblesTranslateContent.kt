package com.fagarester.translator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BubblesTranslateContent(
    currentChatMessages: List<ChatMessage>,
    allMessages: List<ChatMessage>,
    listState: LazyListState,
    isLoading: Boolean,
    currentEditingBubbleId: String?,
    activeRecordMode: String?,
    host: VoiceHost,
    onModeChange: (String?) -> Unit,
    onEditingIdChange: (String?) -> Unit,
    onRetranslate: (String) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onChunkReady: (String) -> Unit,
    settingsStore: SettingsStore
) {
    Column(modifier = Modifier.fillMaxSize()) {
LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(currentChatMessages, key = { it.id }) { message ->
                ChatBubbleItem(
                    message = message,
                    isGenerating = isLoading && message.id == currentEditingBubbleId,
                    onBubbleClick = {
                        val speakLangCode = if (message.isFromRight) settingsStore.targetLangCode else settingsStore.sourceLangCode
                        host.ttsHelper.speak(message.translatedText, speakLangCode)
                    },
                    onRetranslate = { onRetranslate(message.id) },
                    onDelete = { onDeleteMessage(message.id) },
                    settingsStore = settingsStore
                )
            }

            if (isLoading && currentEditingBubbleId == null) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            }
        }


        TranslationBottomPanel(
            activeRecordMode = activeRecordMode,
            host = host,
            chatMessages = allMessages,
            currentEditingBubbleId = currentEditingBubbleId,
            onModeChange = onModeChange,
            onEditingIdChange = onEditingIdChange,
            settingsStore = settingsStore,
            onChunkReady = onChunkReady
        )
    }
}