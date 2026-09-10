package com.fagarester.translator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsDialog(settingsStore: SettingsStore, modelPath: String, onDismiss: () -> Unit) {
    val forcedNpu = isNpuOnlyModel(modelPath)
    val forcedGpu = isGpuOnlyModel(modelPath)
    var backend by remember { mutableStateOf(settingsStore.getModelBackend(modelPath)) }
    var topK by remember { mutableStateOf(settingsStore.aiTopK.toFloat()) }
    var topP by remember { mutableStateOf(settingsStore.aiTopP) }
    var temperature by remember { mutableStateOf(settingsStore.aiTemperature) }
    var maxTokens by remember { mutableStateOf(settingsStore.aiMaxTokens.toFloat()) }
    var systemPrompt by remember { mutableStateOf(settingsStore.aiSystemPrompt) }
    var speculativeDecoding by remember { mutableStateOf(settingsStore.aiSpeculativeDecoding) }

    val scrollState = rememberScrollState()
    val backendOptions = when {
        forcedNpu -> listOf("npu" to "NPU")
        forcedGpu -> listOf("gpu" to "GPU")
        else -> listOf("cpu" to "CPU", "gpu" to "GPU")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки ИИ") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Устройство вычисления", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    backendOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = backend == value,
                            onClick = { backend = value },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                HorizontalDivider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Лимит токенов", fontSize = 14.sp)
                    Text("${maxTokens.toInt()}", fontWeight = FontWeight.Bold)
                }
                Text(
                    "Меньше = быстрее инициализация и генерация, но короче допустимая фраза",
                    fontSize = 11.sp
                )
                Slider(value = maxTokens, onValueChange = { maxTokens = it }, valueRange = 128f..2048f, steps = 14)

                HorizontalDivider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Top-K", fontSize = 14.sp)
                    Text("${topK.toInt()}", fontWeight = FontWeight.Bold)
                }
                Slider(value = topK, onValueChange = { topK = it }, valueRange = 1f..100f, steps = 98)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Top-P", fontSize = 14.sp)
                    Text(String.format("%.2f", topP), fontWeight = FontWeight.Bold)
                }
                Slider(value = topP, onValueChange = { topP = it }, valueRange = 0.1f..1.0f)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Температура", fontSize = 14.sp)
                    Text(String.format("%.2f", temperature), fontWeight = FontWeight.Bold)
                }
                Slider(value = temperature, onValueChange = { temperature = it }, valueRange = 0f..2f)

                HorizontalDivider()

                Text("Системный промпт", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    placeholder = { Text("You are a professional translator...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Спекулятивный декодинг (MTP)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = speculativeDecoding, onCheckedChange = { speculativeDecoding = it })
                }
                Text(
                    "Движок предсказывает несколько токенов за шаг вместо одного, " +
                    "что заметно ускоряет генерацию (в тестах — на 1–1.5 секунды). " +
                    "На GPU работает гарантированно. На NPU и CPU — экспериментально: " +
                    "может как ускорить, так и не дать эффекта (зависит от версии движка " +
                    "и модели) — если заметишь проблемы именно на этих backend'ах, " +
                    "попробуй выключить здесь.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                settingsStore.setModelBackend(modelPath, backend)
                settingsStore.aiTopK = topK.toInt()
                settingsStore.aiTopP = topP
                settingsStore.aiTemperature = temperature
                settingsStore.aiMaxTokens = maxTokens.toInt()
                settingsStore.aiSystemPrompt = systemPrompt
                settingsStore.aiSpeculativeDecoding = speculativeDecoding
                onDismiss()
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
