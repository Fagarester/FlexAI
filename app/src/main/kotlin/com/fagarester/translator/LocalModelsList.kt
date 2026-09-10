package com.fagarester.translator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LocalModelsList(
    settingsStore: SettingsStore,
    context: android.content.Context,
    coroutineScope: CoroutineScope,
    importedList: androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    activePathState: MutableState<String>
) {
    Text("Загруженные модели", fontSize = 13.sp, fontWeight = FontWeight.Medium)

    if (importedList.isEmpty()) {
        Text("Список пуст. Выберите файл кнопкой ниже.", fontSize = 12.sp, color = Color.Gray)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        importedList.forEach { modelPath ->
            val isCurrentActive = activePathState.value == modelPath
            val displayName = modelPath.substringAfterLast("/").removeSuffix(".litertlm")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = if (isCurrentActive)
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6D28D9),
                                    lerp(Color(0xFF6D28D9), Color.White, 0.14f)
                                )
                            )
                        else
                            SolidColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .then(
                        if (isCurrentActive)
                            Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            activePathState.value = modelPath
                            settingsStore.activeModelPath = modelPath
                            coroutineScope.launch {
                                LocalEngineManager.preload(
                                    context, modelPath,
                                    settingsStore.getModelBackend(modelPath),
                                    settingsStore.aiMaxTokens
                                )
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isCurrentActive,
                        onClick = {
                            activePathState.value = modelPath
                            settingsStore.activeModelPath = modelPath
                            coroutineScope.launch {
                                LocalEngineManager.preload(
                                    context, modelPath,
                                    settingsStore.getModelBackend(modelPath),
                                    settingsStore.aiMaxTokens
                                )
                            }
                        }
                    )
                    Text(
                        text = displayName,
                        fontSize = 14.sp,
                        fontWeight = if (isCurrentActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrentActive) GradientContentColor else Color.Unspecified
                    )
                }
                IconButton(
                    onClick = {
                        try {
                            val file = java.io.File(modelPath)
                            if (file.exists()) file.delete()
                        } catch (e: Exception) {
                            // игнор
                        }
                        importedList.remove(modelPath)
                        settingsStore.saveImportedModels(importedList.toList())
                        if (activePathState.value == modelPath) {
                            val next = importedList.lastOrNull() ?: ""
                            activePathState.value = next
                            settingsStore.activeModelPath = next
                            if (next.isNotEmpty()) {
                                coroutineScope.launch {
                                    LocalEngineManager.preload(
                                        context, next,
                                        settingsStore.getModelBackend(next),
                                        settingsStore.aiMaxTokens
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("✖", fontSize = 12.sp)
                }
            }
        }
    }
}