package com.fagarester.translator

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ServerConfigSection(settingsStore: SettingsStore, coroutineScope: CoroutineScope) {
    var serverUrlInput by remember { mutableStateOf(settingsStore.serverUrl) }
    var apiKeyInput by remember { mutableStateOf(settingsStore.apiKey) }
    var serverStatus by remember { mutableStateOf("Статус: не проверялся") }

    OutlinedTextField(
        value = serverUrlInput,
        onValueChange = { serverUrlInput = it },
        label = { Text("Адрес сервера (URL)") },
        placeholder = { Text("http://localhost:8000/v1", color = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    OutlinedTextField(
        value = apiKeyInput,
        onValueChange = { apiKeyInput = it },
        label = { Text("API Ключ сервера") },
        placeholder = { Text("Вставьте ваш токен ollite-...", color = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    GradientButton(
        onClick = {
            serverStatus = "💾 Проверка связи..."
            val finalUrl = if (serverUrlInput.trim().isEmpty()) "http://localhost:8000/v1" else serverUrlInput.trim()
            val finalKey = apiKeyInput.trim()
            settingsStore.serverUrl = finalUrl
            settingsStore.apiKey = finalKey

            coroutineScope.launch {
                val apiClient = ApiClient(finalUrl, finalKey)
                val result = apiClient.checkPing()
                serverStatus = "✅ Сохранено и проверено!\n$result"
            }
        },
        brush = defaultButtonBrush(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Подключить и сохранить", fontWeight = FontWeight.Bold)
    }
    Text(
        text = serverStatus,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}