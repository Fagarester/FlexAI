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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope

@Composable
fun ModelManagerCard(
    settingsStore: SettingsStore,
    selectedModelFileName: String,
    onPickModelFile: () -> Unit,
    onModelFileConsumed: () -> Unit,
    coroutineScope: CoroutineScope,
    context: android.content.Context,
    importedList: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    activePathState: MutableState<String>,
    copyProgress: Float?
) {
    var engineModeInput by remember { mutableStateOf(settingsStore.engineMode) }
    val tabIndex = when (engineModeInput) {
        "local" -> 0
        "server" -> 1
        else -> 2
    }
    var showAiSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedModelFileName) {
        if (selectedModelFileName.isNotEmpty() && !importedList.contains(selectedModelFileName)) {
            importedList.add(selectedModelFileName)
            settingsStore.saveImportedModels(importedList.toList())
            if (activePathState.value.isEmpty()) {
                activePathState.value = selectedModelFileName
                settingsStore.activeModelPath = selectedModelFileName
            }
            onModelFileConsumed()
        }
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Настройка ИИ", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            BoxWithConstraints(
                modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp))
                .padding(3.dp)
            ) {
                val segmentWidth = maxWidth / 3
                val indicatorOffset by animateDpAsState(
                    targetValue = segmentWidth * tabIndex,
                    label = "indicatorOffset"
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
                        .clickable { engineModeInput = "local"; settingsStore.engineMode = "local" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "LiteRT",
                            fontSize = 13.sp,
                            color = animateColorAsState(
                                targetValue = if (tabIndex == 0) GradientContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "textColorLocal"
                            ).value
                        )
                    }
                    Box(
                        modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { engineModeInput = "server"; settingsStore.engineMode = "server" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "AI API",
                            fontSize = 13.sp,
                            color = animateColorAsState(
                                targetValue = if (tabIndex == 1) GradientContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "textColorApi"
                            ).value
                        )
                    }
                    Box(
                        modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { engineModeInput = "geniex"; settingsStore.engineMode = "geniex" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "GenieX",
                            fontSize = 13.sp,
                            color = animateColorAsState(
                                targetValue = if (tabIndex == 2) GradientContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "textColorGeniex"
                            ).value
                        )
                    }
                }
            }

            if (tabIndex == 0) {
                LocalModelsList(
                    settingsStore = settingsStore,
                    context = context,
                    coroutineScope = coroutineScope,
                    importedList = importedList,
                    activePathState = activePathState
                )

                Spacer(modifier = Modifier.height(4.dp))

                GradientButton(
                    onClick = { onPickModelFile() },
                    brush = defaultButtonBrush(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Загрузить новую модель")
                }

                if (copyProgress != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        LinearProgressIndicator(progress = copyProgress, modifier = Modifier.fillMaxWidth())
                        Text(
                            "Копирование: ${(copyProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                ModelDiagnosticSection(
                    settingsStore = settingsStore,
                    context = context,
                    coroutineScope = coroutineScope,
                    activePathState = activePathState,
                    onOpenAiSettings = { showAiSettingsDialog = true }
                )
            } else if (tabIndex == 1) {
                ServerConfigSection(settingsStore = settingsStore, coroutineScope = coroutineScope)
            } else {
                Text(
                    "GenieX (Qualcomm) — экспериментальная поддержка в разработке. " +
                        "Требует отдельного набора моделей, скомпилированных под Qualcomm AI Hub, " +
                        "и не совместим с текущими файлами LiteRT/GGUF.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showAiSettingsDialog && activePathState.value.isNotEmpty()) {
        AiSettingsDialog(
            settingsStore = settingsStore,
            modelPath = activePathState.value,
            onDismiss = { showAiSettingsDialog = false }
        )
    }
}
