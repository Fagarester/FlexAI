package com.fagarester.translator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File

private const val GEMMA_URL =
    "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm"
private const val PARAKEET_URL =
    "https://huggingface.co/handy-computer/parakeet-tdt-0.6b-v3-gguf/resolve/main/parakeet-tdt-0.6b-v3-Q4_K_M.gguf"
private const val PARAKEET_FILE_NAME = "parakeet-tdt-0.6b-v3-Q4_K_M.gguf"

@Composable
fun OnboardingDownloadScreen(
    filesDir: File,
    settingsStore: SettingsStore,
    selectedModelFileState: MutableState<String>,
    onFinish: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var gemmaProgress by remember { mutableStateOf<Float?>(null) }
    var gemmaDone by remember { mutableStateOf(false) }
    var gemmaError by remember { mutableStateOf<String?>(null) }

    var parakeetProgress by remember { mutableStateOf<Float?>(null) }
    var parakeetDone by remember { mutableStateOf(false) }
    var parakeetError by remember { mutableStateOf<String?>(null) }

    val anyStarted = gemmaProgress != null || gemmaDone || parakeetProgress != null || parakeetDone

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Скачать модели", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GradientContentColor)
            Text(
                "Нужны для офлайн-режима. Можно скачать сейчас или позже вручную " +
                    "в Настройках → AI Local. Файлы большие, желательно Wi-Fi.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            DownloadModelCard(
                title = "Gemma 4 E4B-it",
                description = "Модель ИИ, которая делает сам перевод",
                progress = gemmaProgress,
                done = gemmaDone,
                error = gemmaError,
                onDownload = {
                    gemmaError = null
                    coroutineScope.launch {
                        gemmaProgress = 0f
                        val destFile = File(filesDir, "gemma-4-E4B-it.litertlm")
                        val result = ModelDownloader.download(GEMMA_URL, destFile) { p ->
                            gemmaProgress = p
                        }
                        gemmaProgress = null
                        result.onSuccess { file ->
                            settingsStore.selectedModelFile = file.absolutePath
                            selectedModelFileState.value = file.absolutePath
                            settingsStore.engineMode = "local"
                            gemmaDone = true
                        }.onFailure { e ->
                            gemmaError = e.message ?: "Ошибка загрузки"
                        }
                    }
                }
            )

            DownloadModelCard(
                title = "Parakeet TDT (STT)",
                description = "Модель распознавания речи для офлайн-режима (notune)",
                progress = parakeetProgress,
                done = parakeetDone,
                error = parakeetError,
                onDownload = {
                    parakeetError = null
                    coroutineScope.launch {
                        parakeetProgress = 0f
                        val modelsDir = File(filesDir, "models").apply { mkdirs() }
                        val destFile = File(modelsDir, PARAKEET_FILE_NAME)
                        val result = ModelDownloader.download(PARAKEET_URL, destFile) { p ->
                            parakeetProgress = p
                        }
                        parakeetProgress = null
                        result.onSuccess {
                            try {
                                File(filesDir, "active_model").writeText(PARAKEET_FILE_NAME)
                            } catch (t: Throwable) { /* игнор */ }
                            parakeetDone = true
                        }.onFailure { e ->
                            parakeetError = e.message ?: "Ошибка загрузки"
                        }
                    }
                }
            )
        }

        GradientButton(
            onClick = onFinish,
            brush = defaultButtonBrush(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (anyStarted) "Далее" else "Пропустить")
        }
    }
}

@Composable
private fun DownloadModelCard(
    title: String,
    description: String,
    progress: Float?,
    done: Boolean,
    error: String?,
    onDownload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

        when {
            done -> {
                Text("✅ Скачано", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            progress != null -> {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${(progress * 100).toInt()}%", fontSize = 12.sp)
            }
            else -> {
                if (error != null) {
                    Text("⚠️ $error", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
                GradientButton(
                    onClick = onDownload,
                    brush = defaultButtonBrush(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Скачать")
                }
            }
        }
    }
}