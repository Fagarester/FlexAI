package com.fagarester.translator

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File

private const val PARAKEET_URL =
    "https://huggingface.co/handy-computer/parakeet-tdt-0.6b-v3-gguf/resolve/main/parakeet-tdt-0.6b-v3-Q4_K_M.gguf"
private const val PARAKEET_FILE_NAME = "parakeet-tdt-0.6b-v3-Q4_K_M.gguf"

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SttEngineCard(settingsStore: SettingsStore) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var sttEngine by remember { mutableStateOf(settingsStore.sttEngine) }
    val tabIndex = if (sttEngine == "google") 0 else 1

    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Распознавание речи (STT)", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp))
                    .padding(3.dp)
            ) {
                val segmentWidth = maxWidth / 2
                val indicatorOffset by animateDpAsState(
                    targetValue = if (tabIndex == 0) 0.dp else segmentWidth,
                    label = "sttIndicatorOffset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(segmentWidth)
                        .height(32.dp)
                        .background(defaultButtonBrush(), shape = RoundedCornerShape(18.dp))
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                sttEngine = "google"
                                settingsStore.sttEngine = "google"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Google",
                            fontSize = 14.sp,
                            color = animateColorAsState(
                                targetValue = if (tabIndex == 0) GradientContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "sttTextColorGoogle"
                            ).value
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                sttEngine = "notune"
                                settingsStore.sttEngine = "notune"

                                val markerFile = File(context.filesDir, "active_model")
                                val modelExists = markerFile.exists() &&
                                    markerFile.readText().trim().isNotEmpty() &&
                                    File(File(context.filesDir, "models"), markerFile.readText().trim()).exists()

                                if (modelExists) {
                                    dev.notune.transcribe.NotuneWarmup.warmupIfNeeded(context)
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Сначала скачай или импортируй модель Notune ниже",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Notune",
                            fontSize = 14.sp,
                            color = animateColorAsState(
                                targetValue = if (tabIndex == 1) GradientContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "sttTextColorNotune"
                            ).value
                        )
                    }
                }
            }

            Text(
                text = if (sttEngine == "google")
                    "Google — системный распознаватель речи. Работает через облако, требует подключения к интернету, но обычно точнее и лучше держит тихую или невнятную речь.Работает и офлайн если заранее прогружены языковые модели"
                else
                    "Notune — офлайн-движок (Parakeet_0.6b_tdt_v3_q4). Работает полностью на устройстве без интернета, но нужна заранее импортированная модель либо скачать.",
                fontSize = 12.sp,
                color = Color.Gray
            )

            if (tabIndex == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GradientButton(
                        onClick = { (context as? MainActivity)?.pickNotuneModelFile() },
                        brush = defaultButtonBrush(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Импорт", fontSize = 13.sp)
                    }

                    GradientButton(
                        onClick = {
                            downloadError = null
                            coroutineScope.launch {
                                downloadProgress = 0f
                                val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
                                val destFile = File(modelsDir, PARAKEET_FILE_NAME)
                                val result = ModelDownloader.download(PARAKEET_URL, destFile) { p ->
                                    downloadProgress = p
                                }
                                downloadProgress = null
                                result.onSuccess {
                                    try {
                                        File(context.filesDir, "active_model").writeText(PARAKEET_FILE_NAME)
                                    } catch (t: Throwable) { /* игнор */ }
                                }.onFailure { e ->
                                    downloadError = e.message ?: "Ошибка загрузки"
                                }
                            }
                        },
                        brush = defaultButtonBrush(),
                        modifier = Modifier.weight(1f),
                        enabled = downloadProgress == null
                    ) {
                        Text("Скачать", fontSize = 13.sp)
                    }
                }

                if (downloadProgress != null) {
                    LinearProgressIndicator(
                        progress = downloadProgress!!,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${(downloadProgress!! * 100).toInt()}%", fontSize = 12.sp)
                }

                if (downloadError != null) {
                    Text("⚠️ $downloadError", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                var chunkMode by remember { mutableStateOf(settingsStore.notuneChunkMode) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            chunkMode = !chunkMode
                            settingsStore.notuneChunkMode = chunkMode
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Режим чанков перевод", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Переводить каждое законченное предложение сразу, не дожидаясь конца всей фразы. Только для LiteRT.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = chunkMode,
                        onCheckedChange = {
                            chunkMode = it
                            settingsStore.notuneChunkMode = it
                        }
                    )
                }
            }
        }
    }
}
