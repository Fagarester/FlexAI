package com.fagarester.translator

import android.content.Context
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import java.io.File

class LiteRtEngineHelper(private val context: Context) {

    companion object {
        init { NativeLibLoader.preloadQnnLibs() }
    }

    private var engine: Engine? = null

    var activeBackendName: String = ""
        private set

    val isInitialized: Boolean get() = engine != null

    data class LocalResult(
        val content: String?,
        val error: String?,
        val totalLatency: Long,
        val modelLatency: Long,
        val frontendLatency: Long,
        val backend: String = ""
    )

    suspend fun init(
        path: String,
        preferredBackend: String = "auto",
        maxTokens: Int = 512,
        onProgress: ((String) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || file.length() == 0L) {
            onProgress?.invoke("Ошибка: файл модели не найден")
            return@withContext false
        }

        release()

        try {
            val libDir = context.applicationInfo.nativeLibraryDir
            android.system.Os.setenv("ADSP_LIBRARY_PATH", libDir, true)
        } catch (t: Throwable) {
            // игнор: путь ADSP не критичен для CPU/GPU backend
        }

        val cacheFolder = File(context.cacheDir, "litert_npu_cache").apply { mkdirs() }

        val backendsToTry: List<Pair<String, Backend>> = when (preferredBackend) {
            "npu" -> listOf("NPU" to Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir))
            "gpu" -> listOf("GPU" to Backend.GPU())
            "cpu" -> listOf("CPU" to Backend.CPU())
            else -> listOf(
                "NPU" to Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir),
                "CPU" to Backend.CPU(),
            )
        }

        for ((name, backend) in backendsToTry) {
            try {
                onProgress?.invoke("Инициализация на $name...")
                val cfg = EngineConfig(
                    modelPath = path,
                    backend = backend,
                    cacheDir = cacheFolder.absolutePath,
                    maxNumTokens = maxTokens,
                )
                val eng = Engine(cfg)
                eng.initialize()
                engine = eng
                activeBackendName = name
                onProgress?.invoke("Готово ($name)")
                return@withContext true
            } catch (t: Throwable) {
                // пробуем следующий backend
            }
        }
        onProgress?.invoke("Не удалось инициализировать движок")
        false
    }

    suspend fun generateTranslation(
        text: String,
        targetLanguage: String,
        topK: Int = 40,
        topP: Double = 0.9,
        temperature: Double = 0.3,
        customSystemPrompt: String? = null
    ): LocalResult {
        val startTime = System.currentTimeMillis()
        val eng = engine
        if (eng == null) {
            return LocalResult(null, "Движок не инициализирован.", 0, 0, 0)
        }

        return try {
            val systemPrompt = customSystemPrompt
    ?: "You are a professional, native-level translator. Translate the user's text into $targetLanguage. CRITICAL RULES: 1. Translate contextually, not word-for-word. Adapt idioms and metaphors to sound natural. 2. Maintain the original tone and style. 3. Keep original formatting and punctuation. 4. Output ONLY the pure final translation. Strictly NO intros, explanations, notes, or extra text."

            val convConfig = ConversationConfig(
                systemInstruction = Contents.of(systemPrompt),
                samplerConfig = SamplerConfig(topK = topK, topP = topP, temperature = temperature)
            )

            val conv = withContext(Dispatchers.IO) { eng.createConversation(convConfig) }
            val message = withContext(Dispatchers.IO) { conv.sendMessage(text) }
            conv.close()

            val resultText = message.toString().trim()
            if (resultText.isEmpty()) {
                return LocalResult(null, "Пустой ответ от модели", 0, 0, 0, activeBackendName)
            }

            val latency = System.currentTimeMillis() - startTime
            LocalResult(
                content = resultText,
                error = null,
                totalLatency = latency,
                modelLatency = (latency * 0.95).toLong(),
                frontendLatency = (latency * 0.05).toLong(),
                backend = activeBackendName
            )
   } catch (t: kotlinx.coroutines.CancellationException) {
            throw t
        } catch (t: Throwable) {
            LocalResult(null, "Сбой инференса: ${t.localizedMessage}", 0, 0, 0, activeBackendName)
        }
    }

    /**
     * Потоковая версия перевода — для режима чанков в Notune. Отдаёт куски
     * текста по мере генерации (каждый эмит — новый кусок, а не весь текст
     * заново), чтобы можно было показывать "печатающийся" перевод в баблe.
     *
     * Возвращает поток SDK напрямую (без собственной обёртки flow{}) —
     * так рекомендует официальная документация LiteRT-LM. Закрытие Conversation
     * повешено на .onCompletion{}, которое сработает ровно в момент завершения
     * ЭТОГО ЖЕ потока (успех/ошибка/отмена) — раньше был свой flow{}-билдер
     * поверх потока SDK с ручным try/finally, и закрытие могло срабатывать
     * не в той точке, вызывая нативный краш при завершении.
     */
    fun generateTranslationStream(
        text: String,
        targetLanguage: String,
        topK: Int = 40,
        topP: Double = 0.9,
        temperature: Double = 0.3,
        customSystemPrompt: String? = null
    ): Flow<String> {
        val eng = engine ?: throw IllegalStateException("Движок не инициализирован.")

        val systemPrompt = customSystemPrompt
            ?: "You are a professional, native-level translator. Translate the user's text into $targetLanguage. CRITICAL RULES: 1. Translate contextually, not word-for-word. Adapt idioms and metaphors to sound natural. 2. Maintain the original tone and style. 3. Keep original formatting and punctuation. 4. Output ONLY the pure final translation. Strictly NO intros, explanations, notes, or extra text."

        val convConfig = ConversationConfig(
            systemInstruction = Contents.of(systemPrompt),
            samplerConfig = SamplerConfig(topK = topK, topP = topP, temperature = temperature)
        )

        val conv = eng.createConversation(convConfig)

        return conv.sendMessageAsync(text)
            .map { message -> message.toString() }
            .onCompletion { conv.close() }
            .flowOn(Dispatchers.IO)
    }

    fun release() {
        try {
            engine?.close()
        } catch (e: Exception) {
            // игнор
        } finally {
            engine = null
            activeBackendName = ""
        }
    }
}