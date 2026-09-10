package com.fagarester.translator

import org.json.JSONObject

object ApiResponseParser {

    fun parseSuccessResponse(responseBody: String, totalLatency: Long): TranslationResult {
        val json = JSONObject(responseBody)
        val content = when {
            json.has("choices") -> {
                val choices = json.getJSONArray("choices")
                if (choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    if (firstChoice.has("message")) {
                        firstChoice.getJSONObject("message").getString("content")
                    } else {
                        firstChoice.getString("text")
                    }
                } else { "" }
            }
            json.has("text") -> json.getString("text")
            json.has("response") -> json.getString("response")
            json.has("content") -> json.getString("content")
            else -> "Ошибка: Неизвестный формат JSON. Тело: $responseBody"
        }

        var modelLatency = 0L
        if (json.has("timings")) {
            val timings = json.getJSONObject("timings")
            val promptMs = timings.optDouble("prompt_ms", 0.0).toLong()
            val predictedMs = timings.optDouble("predicted_ms", 0.0).toLong()
            modelLatency = promptMs + predictedMs
        }

        val frontendLatency = (totalLatency - modelLatency).coerceAtLeast(0L)
        return TranslationResult(content, null, totalLatency, modelLatency, frontendLatency)
    }

    fun buildPingUrl(cleanUrl: String): String {
        return if (cleanUrl.endsWith("chat/completions") || cleanUrl.endsWith("completions")) {
            cleanUrl.substringBefore("/v1") + "/v1/health"
        } else {
            "$cleanUrl/health"
        }
    }
}