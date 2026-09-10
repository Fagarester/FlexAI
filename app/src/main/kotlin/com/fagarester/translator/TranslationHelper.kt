package com.fagarester.translator

import android.content.Context

object TranslationHelper {

    suspend fun translate(
        settingsStore: SettingsStore,
        context: Context,
        text: String,
        targetLanguage: String,
        onProgress: ((String) -> Unit)? = null
    ): TranslationResult {
        if (settingsStore.engineMode == "geniex") {
            return TranslationResult(null, "GenieX ещё не реализован в приложении. Выберите LiteRT или AI API в настройках.", 0, 0, 0)
        }

        if (settingsStore.engineMode == "local") {
            val modelPath = settingsStore.activeModelPath
            if (modelPath.isEmpty()) {
                return TranslationResult(null, "Локальная модель не выбрана. Выберите модель в настройках.", 0, 0, 0)
            }
            val localResult = LocalEngineManager.translate(
                context = context,
                modelPath = modelPath,
                text = text,
                targetLanguage = targetLanguage,
                backend = settingsStore.getModelBackend(modelPath),
                topK = settingsStore.aiTopK,
                topP = settingsStore.aiTopP.toDouble(),
                temperature = settingsStore.aiTemperature.toDouble(),
                maxTokens = settingsStore.aiMaxTokens,
                customSystemPrompt = settingsStore.aiSystemPrompt.ifEmpty { null },
                onProgress = onProgress
            )
            return TranslationResult(
                localResult.content,
                localResult.error,
                localResult.totalLatency,
                localResult.modelLatency,
                localResult.frontendLatency,
                localResult.backend
            )
        }

        val apiClient = ApiClient(settingsStore.serverUrl, settingsStore.apiKey)
        val apiResult = apiClient.translate(text, targetLanguage)
        return apiResult.copy(backend = "API")
    }
}