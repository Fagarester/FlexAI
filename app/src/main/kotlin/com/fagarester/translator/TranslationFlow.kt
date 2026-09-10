package com.fagarester.translator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Режим чанков (Notune): вызывается на каждое законченное предложение
 * (определяется в SpeechHelper по точке/?/!) отдельно, независимо от
 * остальной фразы. Переводит именно этот кусок потоково (виден "печатающийся"
 * перевод) и ДОПИСЫВАЕТ результат к уже переведённым кускам этого бабла —
 * готовые куски не переводятся заново.
 *
 * Текст оригинала (серая строка) НЕ трогает — этим занимается обычный
 * processRecognizedText() через onResult, чтобы не было двух источников
 * правды для одного и того же поля и мигания.
 */
fun TranslationViewModel.processChunk(
    chunkText: String,
    coroutineScope: CoroutineScope,
    isTtsEnabled: Boolean
) {
    val trimmed = chunkText.trim()
    if (trimmed.isEmpty()) return

    if (currentChatId == null) {
        val newSessionId = UUID.randomUUID().toString()
        val newSession = ChatSession(id = newSessionId, title = trimmed)
        sessions.add(newSession)
        chatRepository.saveChatSessions(sessions)
        currentChatId = newSessionId
        settingsStore.currentChatId = newSessionId
    }

    val bubbleId: String
    val isFromRight: Boolean
    val targetBubbleId = currentEditingBubbleId

    if (targetBubbleId == null) {
        // Первый чанк новой фразы — бабла ещё нет, создаём (originalText
        // тут ставим как стартовое значение, дальше его подхватит и будет
        // обновлять processRecognizedText через обычный onResult).
        isFromRight = when (activeRecordMode) {
            "source" -> true
            "target" -> false
            else -> LanguageDetector.looksLikeSourceLanguage(trimmed, settingsStore.sourceLangCode, settingsStore.targetLangCode)
        }
        isCurrentSessionRight = isFromRight

        val newBubble = ChatMessage(
            id = UUID.randomUUID().toString(),
            chatId = currentChatId!!,
            originalText = trimmed,
            translatedText = "",
            isFromRight = isFromRight
        )
        bubbleId = newBubble.id
        currentEditingBubbleId = bubbleId
        allMessages.add(newBubble)
        chatRepository.saveAllMessages(allMessages)
    } else {
        bubbleId = targetBubbleId
        isFromRight = isCurrentSessionRight ?: true
    }

    val targetLanguage = if (isFromRight) settingsStore.targetLangName else settingsStore.sourceLangName
    val modelPath = settingsStore.activeModelPath

    if (modelPath.isEmpty()) {
        val index = allMessages.indexOfFirst { it.id == bubbleId }
        if (index != -1) {
            allMessages[index] = allMessages[index].copy(translatedText = "Ошибка: локальная модель не выбрана")
            chatRepository.saveAllMessages(allMessages)
            listUpdateTrigger++
        }
        return
    }

    // То, что уже переведено по предыдущим чанкам этого бабла — новый чанк
    // дописывается ПОСЛЕ, не затирая готовое.
    val basePrefix = allMessages.find { it.id == bubbleId }?.translatedText ?: ""

    coroutineScope.launch {
        isLoading = true

        val result = LocalEngineManager.translateStream(
            context = appContext,
            modelPath = modelPath,
            text = trimmed,
            targetLanguage = targetLanguage,
            backend = settingsStore.getModelBackend(modelPath),
            topK = settingsStore.aiTopK,
            topP = settingsStore.aiTopP.toDouble(),
            temperature = settingsStore.aiTemperature.toDouble(),
            maxTokens = settingsStore.aiMaxTokens,
            customSystemPrompt = settingsStore.aiSystemPrompt.ifEmpty { null }
        ) { partialText ->
            val index = allMessages.indexOfFirst { it.id == bubbleId }
            if (index != -1) {
                val combined = if (basePrefix.isBlank()) partialText else "$basePrefix $partialText"
                allMessages[index] = allMessages[index].copy(translatedText = combined)
                listUpdateTrigger++
            }
        }

        isLoading = false

        val index = allMessages.indexOfFirst { it.id == bubbleId }
        if (index != -1) {
            if (result.content != null) {
                val finalCombined = if (basePrefix.isBlank()) result.content else "$basePrefix ${result.content}"
                allMessages[index] = allMessages[index].copy(
                    translatedText = finalCombined,
                    metricTotal = result.totalLatency,
                    metricModel = result.modelLatency,
                    metricFrontend = result.frontendLatency,
                    metricBackend = result.backend
                )
                chatRepository.saveAllMessages(allMessages)
                listUpdateTrigger++

                if (isTtsEnabled && activeRecordMode == null) {
                    val speakLangCode = if (isFromRight) settingsStore.targetLangCode else settingsStore.sourceLangCode
                    ttsHelper.speak(result.content, speakLangCode)
                }
            } else {
                val errorCombined = if (basePrefix.isBlank()) "Ошибка: ${result.error}" else "$basePrefix\nОшибка: ${result.error}"
                allMessages[index] = allMessages[index].copy(translatedText = errorCombined)
                chatRepository.saveAllMessages(allMessages)
                listUpdateTrigger++
            }
        }
    }
}

/**
 * Повторная отправка уже существующего бабла на перевод заново
 * (кнопка "повторить" под баблом). Берёт оригинальный текст как есть,
 * заново переводит и обновляет тот же бабл.
 */
fun TranslationViewModel.retranslateMessage(id: String, coroutineScope: CoroutineScope) {
    val message = allMessages.find { it.id == id } ?: return

    coroutineScope.launch {
        isLoading = true
        currentEditingBubbleId = id

        val targetLanguage = if (message.isFromRight) settingsStore.targetLangName else settingsStore.sourceLangName
        val resultObj = TranslationHelper.translate(settingsStore, appContext, message.originalText, targetLanguage)

        isLoading = false
        currentEditingBubbleId = null

        val index = allMessages.indexOfFirst { it.id == id }
        if (index != -1) {
            allMessages[index] = if (resultObj.content != null) {
                allMessages[index].copy(
                    translatedText = resultObj.content,
                    metricTotal = resultObj.totalLatency,
                    metricModel = resultObj.modelLatency,
                    metricFrontend = resultObj.frontendLatency,
                    metricBackend = resultObj.backend
                )
            } else {
                allMessages[index].copy(translatedText = "Ошибка: ${resultObj.error}")
            }
            chatRepository.saveAllMessages(allMessages)
            listUpdateTrigger++
        }
    }
}

/**
 * Логика "подождать паузу после речи → отправить на перевод → записать результат".
 * Вынесена из TranslationViewModel отдельным расширением, чтобы не раздувать сам класс.
 */
fun TranslationViewModel.scheduleTranslation(
    coroutineScope: CoroutineScope,
    currentInput: String,
    isTtsEnabled: Boolean
) {
    // Запоминаем ID пузыря и сторону СРАЗУ, до паузы — иначе если пользователь
    // быстро нажмёт "стоп" (что сбрасывает currentEditingBubbleId для следующей
    // записи) раньше, чем истечёт пауза debounce, перевод для уже показанного
    // текста тихо потеряется — пузырь останется висеть без перевода навсегда.
    val finalBubbleId = currentEditingBubbleId ?: return
    val finalBubbleSide = isCurrentSessionRight ?: true

    debounceJob?.cancel()
    debounceJob = coroutineScope.launch {
        delay(settingsStore.debounceDelay.toLong())
        isLoading = true
        val targetLanguage = if (finalBubbleSide) {
            settingsStore.targetLangName
        } else {
            settingsStore.sourceLangName
        }

        val resultObj = TranslationHelper.translate(settingsStore, appContext, currentInput, targetLanguage)

        isLoading = false
        val textToSpeak = resultObj.content
        if (textToSpeak != null) {
            val index = allMessages.indexOfFirst { it.id == finalBubbleId }
            if (index != -1) {
                allMessages[index] = allMessages[index].copy(
                    translatedText = textToSpeak,
                    isFromRight = finalBubbleSide,
                    metricTotal = resultObj.totalLatency,
                    metricModel = resultObj.modelLatency,
                    metricFrontend = resultObj.frontendLatency,
                    metricBackend = resultObj.backend
                )
                chatRepository.saveAllMessages(allMessages)
                listUpdateTrigger++

                if (isTtsEnabled && activeRecordMode == null) {
                    val speakLangCode = if (finalBubbleSide) settingsStore.targetLangCode else settingsStore.sourceLangCode
                    ttsHelper.speak(textToSpeak, speakLangCode)
                }
            }
        } else if (resultObj.error != null) {
            val index = allMessages.indexOfFirst { it.id == finalBubbleId }
            if (index != -1) {
                allMessages[index] = allMessages[index].copy(
                    translatedText = "Ошибка: ${resultObj.error}",
                    isFromRight = finalBubbleSide
                )
                chatRepository.saveAllMessages(allMessages)
                listUpdateTrigger++
            }
        }
    }
}