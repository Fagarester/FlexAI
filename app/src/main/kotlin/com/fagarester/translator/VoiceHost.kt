package com.fagarester.translator

import androidx.compose.runtime.MutableState

/**
 * Общий контракт для экранов-хостов голосового ввода (MainActivity, AssistantActivity).
 * Устраняет жёсткую привязку UI-компонентов к конкретному классу MainActivity:
 * раньше AssistantActivity была вынуждена создавать "заглушку" MainActivity(),
 * из-за чего результат распознавания речи никогда не доходил до экрана ассистента.
 */
interface VoiceHost {
    var recognizedTextState: MutableState<String>
    val newChatRequestState: MutableState<Int>
    val speechHelper: SpeechHelper
    val ttsHelper: TtsHelper
}