package com.fagarester.translator

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class SettingsStore(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("translator_settings", Context.MODE_PRIVATE)

    var engineMode: String
        get() = prefs.getString("engine_mode", "local") ?: "local"
        set(value) = prefs.edit().putString("engine_mode", value).apply()

    var selectedModelFile: String
        get() = prefs.getString("selected_model_file", "") ?: ""
        set(value) = prefs.edit().putString("selected_model_file", value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    /**
     * Режим чанков для Notune: как только распознавание речи заканчивает
     * предложение (точка/? /!), оно сразу уходит на перевод отдельным куском,
     * не дожидаясь конца всей фразы. Работает только с LiteRT (локальным движком),
     * так как требует потоковой генерации токенов.
     */
    var notuneChunkMode: Boolean
        get() = prefs.getBoolean("notune_chunk_mode", false)
        set(value) = prefs.edit().putBoolean("notune_chunk_mode", value).apply()

    var serverUrl: String
        get() = prefs.getString("server_url", "http://localhost:8000/v1") ?: "http://localhost:8000/v1"
        set(value) = prefs.edit().putString("server_url", value).apply()

    var apiKey: String
        get() = prefs.getString("api_key", "") ?: ""
        set(value) = prefs.edit().putString("api_key", value).apply()

    var sourceTtsEngine: String
        get() = prefs.getString("source_tts_engine", "") ?: ""
        set(value) = prefs.edit().putString("source_tts_engine", value).apply()

    var targetTtsEngine: String
        get() = prefs.getString("target_tts_engine", "") ?: ""
        set(value) = prefs.edit().putString("target_tts_engine", value).apply()

    var sourceLangCode: String
        get() = prefs.getString("source_lang_code", "ru-RU") ?: "ru-RU"
        set(value) = prefs.edit().putString("source_lang_code", value).apply()

    var sourceLangName: String
        get() = prefs.getString("source_lang_name", "Русский") ?: "Русский"
        set(value) = prefs.edit().putString("source_lang_name", value).apply()

    var targetLangCode: String
        get() = prefs.getString("target_lang_code", "en-US") ?: "en-US"
        set(value) = prefs.edit().putString("target_lang_code", value).apply()

    var targetLangName: String
        get() = prefs.getString("target_lang_name", "English") ?: "English"
        set(value) = prefs.edit().putString("target_lang_name", value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    var debounceDelay: Int
        get() = prefs.getInt("debounce_delay", 700)
        set(value) = prefs.edit().putInt("debounce_delay", value).apply()
        
    var ttsSpeedFactor: Int
        get() = prefs.getInt("tts_speed_factor", 100)
        set(value) = prefs.edit().putInt("tts_speed_factor", value).apply()

    var sourceTtsSpeed: Int
        get() = prefs.getInt("source_tts_speed", 115)
        set(value) = prefs.edit().putInt("source_tts_speed", value).apply()

    var targetTtsSpeed: Int
        get() = prefs.getInt("target_tts_speed", 125)
        set(value) = prefs.edit().putInt("target_tts_speed", value).apply()

    var showMetricTotal: Boolean
        get() = prefs.getBoolean("show_metric_total", true)
        set(value) = prefs.edit().putBoolean("show_metric_total", value).apply()

    var showMetricModel: Boolean
        get() = prefs.getBoolean("show_metric_model", true)
        set(value) = prefs.edit().putBoolean("show_metric_model", value).apply()

    var showMetricFrontend: Boolean
        get() = prefs.getBoolean("show_metric_frontend", true)
        set(value) = prefs.edit().putBoolean("show_metric_frontend", value).apply()

    var currentChatId: String?
        get() = prefs.getString("current_chat_id", null)
        set(value) = prefs.edit().putString("current_chat_id", value).apply()

    var activeModelPath: String
        get() = prefs.getString("active_model_path", "") ?: ""
        set(value) = prefs.edit().putString("active_model_path", value).apply()

var sttEngine: String
        get() = prefs.getString("stt_engine", "google") ?: "google"
        set(value) = prefs.edit().putString("stt_engine", value).apply()

    var aiBackend: String
        get() = prefs.getString("ai_backend", "auto") ?: "auto"
        set(value) = prefs.edit().putString("ai_backend", value).apply()

    var aiTopK: Int
        get() = prefs.getInt("ai_top_k", 40)
        set(value) = prefs.edit().putInt("ai_top_k", value).apply()

    var aiTopP: Float
        get() = prefs.getFloat("ai_top_p", 0.9f)
        set(value) = prefs.edit().putFloat("ai_top_p", value).apply()

    var aiTemperature: Float
        get() = prefs.getFloat("ai_temperature", 0.3f)
        set(value) = prefs.edit().putFloat("ai_temperature", value).apply()

    var aiSystemPrompt: String
        get() = prefs.getString("ai_system_prompt", "") ?: ""
        set(value) = prefs.edit().putString("ai_system_prompt", value).apply()

    var aiMaxTokens: Int
        get() = prefs.getInt("ai_max_tokens", 256)
        set(value) = prefs.edit().putInt("ai_max_tokens", value).apply()

    @OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)
    var aiSpeculativeDecoding: Boolean
        get() = prefs.getBoolean("ai_speculative_decoding", true)
        set(value) {
            prefs.edit().putBoolean("ai_speculative_decoding", value).apply()
            try {
                com.google.ai.edge.litertlm.ExperimentalFlags.enableSpeculativeDecoding = value
            } catch (t: Throwable) {
                // игнор: если недоступно в этой версии либы — не критично
            }
        }
        
    fun getModelBackend(path: String): String {
        if (isNpuOnlyModel(path)) return "npu"
        if (isGpuOnlyModel(path)) return "gpu"
        val jsonStr = prefs.getString("model_backends_json", "{}") ?: "{}"
        return try {
            JSONObject(jsonStr).optString(path, "cpu")
        } catch (e: Exception) { "cpu" }
    }

    fun setModelBackend(path: String, backend: String) {
        if (isNpuOnlyModel(path)) return
        if (isGpuOnlyModel(path)) return
        val jsonStr = prefs.getString("model_backends_json", "{}") ?: "{}"
        val obj = try { JSONObject(jsonStr) } catch (e: Exception) { JSONObject() }
        try { obj.put(path, backend) } catch (e: Exception) {}
        prefs.edit().putString("model_backends_json", obj.toString()).apply()
    }

    fun getImportedModels(): List<String> {
        val jsonStr = prefs.getString("imported_models_json", "[]") ?: "[]"
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) list.add(arr.getString(i))
        } catch (e: Exception) { /* игнор */ }
        return list
    }

    fun saveImportedModels(models: List<String>) {
        val arr = JSONArray()
        for (path in models) arr.put(path)
        prefs.edit().putString("imported_models_json", arr.toString()).apply()
    }
}