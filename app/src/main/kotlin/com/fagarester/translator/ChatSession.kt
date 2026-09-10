package com.fagarester.translator

import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val originalText: String,
    val translatedText: String,
    val isFromRight: Boolean,
    val metricTotal: Long = 0L,
    val metricModel: Long = 0L,
    val metricFrontend: Long = 0L,
    val metricBackend: String = ""
)

data class TranslationResult(
    val content: String?,
    val error: String?,
    val totalLatency: Long = 0L,
    val modelLatency: Long = 0L,
    val frontendLatency: Long = 0L,
    val backend: String = ""
)