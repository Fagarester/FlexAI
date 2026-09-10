package com.fagarester.translator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiClient(private val serverUrl: String, private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val cleanUrl: String = serverUrl.trim().removeSuffix("/")
    private val cleanKey: String = apiKey.trim()

    private val finalTargetUrl: String = if (cleanUrl.endsWith("chat/completions") ||
        cleanUrl.endsWith("completions") || cleanUrl.endsWith("generate")
    ) {
        cleanUrl
    } else {
        "$cleanUrl/chat/completions"
    }

    suspend fun translate(text: String, targetLanguage: String): TranslationResult = withContext(Dispatchers.IO) {
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", "[Translate to $targetLanguage]: $text")
            })
        }

        val body = JSONObject().apply {
            put("messages", messages)
            put("stream", false)
            put("prompt", "[Translate to $targetLanguage]: $text")
            put("max_tokens", 256)
        }

        val requestBody = RequestBody.create(null, body.toString())
        val startTime = System.currentTimeMillis()
        val request = buildRequest(finalTargetUrl, requestBody, includeBearer = true)

        try {
            val response = client.newCall(request).execute()
            val totalLatency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""

            if (response.code == 401 || response.code == 403 || responseBody.contains("unauthorized")) {
                return@withContext retryWithoutBearer(finalTargetUrl, requestBody, startTime)
            }

            return@withContext ApiResponseParser.parseSuccessResponse(responseBody, totalLatency)
        } catch (e: IOException) {
            return@withContext TranslationResult(null, e.message)
        } catch (e: Exception) {
            return@withContext TranslationResult(null, "Ошибка разбора: ${e.message}")
        }
    }

    private fun retryWithoutBearer(url: String, requestBody: RequestBody, startTime: Long): TranslationResult {
        val retryRequest = buildRequest(url, requestBody, includeBearer = false)
        return try {
            val response = client.newCall(retryRequest).execute()
            val totalLatency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string() ?: ""
            ApiResponseParser.parseSuccessResponse(responseBody, totalLatency)
        } catch (e: Exception) {
            TranslationResult(null, "Ошибка аварийного контура: ${e.message}")
        }
    }

    private fun buildRequest(url: String, requestBody: RequestBody, includeBearer: Boolean): Request {
        return Request.Builder().apply {
            url(url)
            if (includeBearer) {
                addHeader("Authorization", "Bearer $cleanKey")
            } else {
                addHeader("Authorization", cleanKey)
            }
            addHeader("X-API-Key", cleanKey)
            addHeader("api-key", cleanKey)
            addHeader("Content-Type", "application/json; charset=utf-8")
            post(requestBody)
        }.build()
    }

    suspend fun checkPing(): String = withContext(Dispatchers.IO) {
        val pingUrl = ApiResponseParser.buildPingUrl(cleanUrl)
        val request = Request.Builder().url(pingUrl).get().build()
        return@withContext try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "Пустое тело ответа"
            if (response.code == 200) {
                "✅ Успешно! Сервер ответил Код 200. Ответ: $body"
            } else {
                "⚠️ Сервер ответил, но с кодом ошибки: ${response.code}. Ответ: $body"
            }
        } catch (e: IOException) {
            "❌ Ошибка сети: ${e.message ?: "Не удалось подключиться"}"
        }
    }
}