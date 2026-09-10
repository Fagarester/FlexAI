package com.fagarester.translator

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object LocalEngineManager {

    private var helper: LiteRtEngineHelper? = null
    private var loadedModelPath: String = ""
    private var loadedMaxTokens: Int = -1
    private var loadedBackend: String = ""
    private val mutex = Mutex()

    val loadingStatus = mutableStateOf<String?>(null)

    private suspend fun ensureLoaded(
        context: Context,
        modelPath: String,
        backend: String,
        maxTokens: Int
    ): Boolean {
        if (helper != null && loadedModelPath == modelPath && loadedMaxTokens == maxTokens && loadedBackend == backend) {
            return true
        }
        helper?.release()
        val newHelper = LiteRtEngineHelper(context.applicationContext)
        val ready = try {
            newHelper.init(modelPath, backend, maxTokens) { progress ->
                loadingStatus.value = progress
            }
        } finally {
            loadingStatus.value = null
        }
        if (!ready) return false
        helper = newHelper
        loadedModelPath = modelPath
        loadedMaxTokens = maxTokens
        loadedBackend = backend
        return true
    }

    /** Заранее инициализирует движок (например, сразу после выбора модели или при запуске приложения). */
    suspend fun preload(context: Context, modelPath: String, backend: String, maxTokens: Int): Boolean = mutex.withLock {
        ensureLoaded(context, modelPath, backend, maxTokens)
    }

    suspend fun translate(
        context: Context,
        modelPath: String,
        text: String,
        targetLanguage: String,
        backend: String = "auto",
        topK: Int = 40,
        topP: Double = 0.9,
        temperature: Double = 0.3,
        maxTokens: Int = 512,
        customSystemPrompt: String? = null,
        onProgress: ((String) -> Unit)? = null
    ): LiteRtEngineHelper.LocalResult = mutex.withLock {
        val ready = ensureLoaded(context, modelPath, backend, maxTokens)
        if (!ready) {
            return@withLock LiteRtEngineHelper.LocalResult(null, "Не удалось инициализировать движок", 0, 0, 0)
        }
        helper!!.generateTranslation(text, targetLanguage, topK, topP, temperature, customSystemPrompt)
    }

    /**
     * Потоковая версия — для режима чанков. Держит mutex на всё время генерации
     * (а не только на момент запуска), чтобы соседний обычный translate() не
     * влез в нативный движок посреди потоковой генерации.
     * onToken вызывается с НАКОПЛЕННЫМ текстом на каждый новый кусок (удобно
     * для прямого показа в UI — не нужно склеивать самим).
     */
    suspend fun translateStream(
        context: Context,
        modelPath: String,
        text: String,
        targetLanguage: String,
        backend: String = "auto",
        topK: Int = 40,
        topP: Double = 0.9,
        temperature: Double = 0.3,
        maxTokens: Int = 512,
        customSystemPrompt: String? = null,
        onToken: suspend (String) -> Unit
    ): LiteRtEngineHelper.LocalResult = mutex.withLock {
        val ready = ensureLoaded(context, modelPath, backend, maxTokens)
        if (!ready) {
            return@withLock LiteRtEngineHelper.LocalResult(null, "Не удалось инициализировать движок", 0, 0, 0)
        }
        val startTime = System.currentTimeMillis()
        val sb = StringBuilder()
        try {
            helper!!.generateTranslationStream(text, targetLanguage, topK, topP, temperature, customSystemPrompt)
                .collect { delta ->
                    sb.append(delta)
                    onToken(sb.toString())
                }
            val elapsed = System.currentTimeMillis() - startTime
            LiteRtEngineHelper.LocalResult(sb.toString(), null, elapsed, elapsed, 0, "LiteRT")
        } catch (t: Throwable) {
            LiteRtEngineHelper.LocalResult(null, t.message ?: "Ошибка потоковой генерации", 0, 0, 0)
        }
    }

    fun release() {
        helper?.release()
        helper = null
        loadedModelPath = ""
        loadedMaxTokens = -1
        loadedBackend = ""
    }
}