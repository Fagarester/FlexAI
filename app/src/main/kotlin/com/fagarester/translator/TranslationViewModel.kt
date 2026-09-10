package com.fagarester.translator

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import java.util.UUID

class TranslationViewModel(
    internal val settingsStore: SettingsStore,
    internal val chatRepository: ChatRepository,
    internal val ttsHelper: TtsHelper,
    internal val appContext: Context
) {
    val sessions = mutableStateListOf<ChatSession>().apply { addAll(chatRepository.getChatSessions()) }
    val allMessages = mutableStateListOf<ChatMessage>().apply { addAll(chatRepository.getAllMessages()) }

    var currentChatId by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)

    var activeRecordMode by mutableStateOf<String?>(null)
    var currentEditingBubbleId by mutableStateOf<String?>(null)
    var isCurrentSessionRight by mutableStateOf<Boolean?>(null)

    var listUpdateTrigger by mutableStateOf(0)
    internal var debounceJob: kotlinx.coroutines.Job? = null

    fun processRecognizedText(recognizedText: String, coroutineScope: kotlinx.coroutines.CoroutineScope, isTtsEnabled: Boolean) {
        if (recognizedText.isEmpty()) return

        
        if (recognizedText.contains("Конец сессии") || recognizedText.startsWith("Обработка") || recognizedText.startsWith("Ошибка")) {
            currentEditingBubbleId = null
            isCurrentSessionRight = null
            return
        }

        val currentInput = recognizedText

        if (currentChatId == null) {
            val newSessionId = UUID.randomUUID().toString()
            val newSession = ChatSession(id = newSessionId, title = currentInput)
            sessions.add(newSession)
            chatRepository.saveChatSessions(sessions)
            currentChatId = newSessionId
            settingsStore.currentChatId = newSessionId
        }

        val targetBubbleId = currentEditingBubbleId

        
        if (targetBubbleId == null) {
           val isFromRight = when (activeRecordMode) {
                "source" -> true
                "target" -> false
                else -> LanguageDetector.looksLikeSourceLanguage(currentInput, settingsStore.sourceLangCode, settingsStore.targetLangCode)
            }

            isCurrentSessionRight = isFromRight

            val newBubble = ChatMessage(
                id = UUID.randomUUID().toString(),
                chatId = currentChatId!!,
                originalText = currentInput,
                translatedText = "",
                isFromRight = isFromRight,
                metricTotal = 0L,
                metricModel = 0L,
                metricFrontend = 0L
            )
            currentEditingBubbleId = newBubble.id
            allMessages.add(newBubble)
            chatRepository.saveAllMessages(allMessages)
        } else {
            val index = allMessages.indexOfFirst { it.id == targetBubbleId }
            if (index != -1) {
                val fixedSide = isCurrentSessionRight ?: true
                allMessages[index] = allMessages[index].copy(
                    originalText = currentInput,
                    isFromRight = fixedSide
                )
                chatRepository.saveAllMessages(allMessages)
                listUpdateTrigger++
            }
        }

        // В режиме чанков перевод делает processChunk() по каждому законченному
        // предложению отдельно — обычный debounce-перевод тут не нужен и будет
        // мешать (двойной перевод одновременно).
        if (!isChunkModeActive()) {
            scheduleTranslation(coroutineScope, currentInput, isTtsEnabled)
        }
    }

    /**
     * true, если сейчас активен режим чанков — тогда обычный (не-потоковый)
     * debounce-перевод в scheduleTranslation() запускать не нужно, чтобы не
     * было двойного перевода одновременно с processChunk().
     */
    internal fun isChunkModeActive(): Boolean =
        settingsStore.sttEngine == "notune" && settingsStore.notuneChunkMode

    fun createNewSession() {
        currentChatId = null
        settingsStore.currentChatId = null
        currentEditingBubbleId = null
        isCurrentSessionRight = null
    }

    fun deleteSession(id: String) {
        sessions.removeAll { it.id == id }
        allMessages.removeAll { it.chatId == id }
        chatRepository.saveChatSessions(sessions)
        chatRepository.saveAllMessages(allMessages)
        if (currentChatId == id) {
            currentChatId = sessions.lastOrNull()?.id
            settingsStore.currentChatId = currentChatId
        }
    }

    fun deleteMessage(id: String) {
        allMessages.removeAll { it.id == id }
        chatRepository.saveAllMessages(allMessages)
        listUpdateTrigger++
    }

    fun clearAllHistory() {
        sessions.clear()
        allMessages.clear()
        currentChatId = null
        settingsStore.currentChatId = null
        chatRepository.saveChatSessions(sessions)
        chatRepository.saveAllMessages(allMessages)
    }
}