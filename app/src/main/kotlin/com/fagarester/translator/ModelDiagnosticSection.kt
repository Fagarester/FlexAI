package com.fagarester.translator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ModelDiagnosticSection(
    settingsStore: SettingsStore,
    context: android.content.Context,
    coroutineScope: CoroutineScope,
    activePathState: MutableState<String>,
    onOpenAiSettings: () -> Unit
) {
    var diagnosticReport by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    var isDiagnosticRunning by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GradientButton(
            onClick = onOpenAiSettings,
            brush = defaultButtonBrush(),
            modifier = Modifier.weight(1f),
            enabled = activePathState.value.isNotEmpty()
        ) {
            Text("Настройки")
        }

        GradientButton(
            onClick = {
                if (activePathState.value.isNotEmpty()) {
                    isDiagnosticRunning = true
                    diagnosticReport = emptyList()
                    progressText = "Запуск..."
                    coroutineScope.launch {
                        val helper = LiteRtEngineHelper(context)
                        val success = helper.init(
                            activePathState.value,
                            settingsStore.getModelBackend(activePathState.value)
                        ) { progress -> progressText = progress }
                        diagnosticReport = if (success) {
                            listOf("Движок успешно инициализирован" to true)
                        } else {
                            listOf("Не удалось инициализировать движок" to false)
                        }
                        helper.release()
                        isDiagnosticRunning = false
                    }
                }
            },
            brush = defaultButtonBrush(),
            enabled = activePathState.value.isNotEmpty() && !isDiagnosticRunning,
            modifier = Modifier.weight(1f)
        ) {
            Text(if (isDiagnosticRunning) "..." else "Проверить")
        }
    }

    if (isDiagnosticRunning && progressText.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(progressText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }

    if (diagnosticReport.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            diagnosticReport.forEach { (stepText, isPassed) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isPassed) "✅" else "❌", modifier = Modifier.padding(end = 6.dp))
                    Text(
                        text = stepText,
                        fontSize = 12.sp,
                        color = if (isPassed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        fontWeight = if (isPassed) FontWeight.Normal else FontWeight.Bold
                    )
                }
            }
        }
    }
}