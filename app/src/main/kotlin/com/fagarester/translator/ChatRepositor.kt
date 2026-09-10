package com.fagarester.translator

import org.json.JSONArray
import org.json.JSONObject

class ChatRepository(private val settingsStore: SettingsStore) {
    private val prefs get() = settingsStore.prefs

    fun getChatSessions(): List<ChatSession> {
        val jsonStr = prefs.getString("chat_sessions_json", "[]") ?: "[]"
        val list = mutableListOf<ChatSession>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(ChatSession(id = obj.getString("id"), title = obj.getString("title")))
            }
        } catch (e: Exception) { /* игнор */ }
        return list
    }

    fun saveChatSessions(sessions: List<ChatSession>) {
        val arr = JSONArray()
        for (s in sessions) {
            arr.put(JSONObject().apply { put("id", s.id); put("title", s.title) })
        }
        prefs.edit().putString("chat_sessions_json", arr.toString()).apply()
    }

    fun getAllMessages(): List<ChatMessage> {
        val jsonStr = prefs.getString("all_messages_json", "[]") ?: "[]"
        val list = mutableListOf<ChatMessage>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = obj.getString("id"),
                        chatId = obj.getString("chatId"),
                        originalText = obj.getString("originalText"),
                        translatedText = obj.getString("translatedText"),
                        isFromRight = obj.getBoolean("isFromRight"),
                        metricTotal = obj.optLong("metricTotal", 0L),
                        metricModel = obj.optLong("metricModel", 0L),
                        metricFrontend = obj.optLong("metricFrontend", 0L),
                        metricBackend = obj.optString("metricBackend", "")
                    )
                )
            }
        } catch (e: Exception) { /* игнор */ }
        return list
    }

    fun saveAllMessages(messages: List<ChatMessage>) {
        val arr = JSONArray()
        for (m in messages) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("chatId", m.chatId)
                put("originalText", m.originalText)
                put("translatedText", m.translatedText)
                put("isFromRight", m.isFromRight)
                put("metricTotal", m.metricTotal)
                put("metricModel", m.metricModel)
                put("metricFrontend", m.metricFrontend)
                put("metricBackend", m.metricBackend)
            }
            arr.put(obj)
        }
        prefs.edit().putString("all_messages_json", arr.toString()).apply()
    }
}