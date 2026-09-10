package com.fagarester.translator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TtsConfigurationCard(
    settingsStore: SettingsStore,
    availableLanguages: List<Pair<String, String>>,
    finalTtsList: List<CustomTtsEngineInfo>,
    host: VoiceHost,
    context: android.content.Context,
    sourceLangName: String,
    targetLangName: String,
    onSourceLangChange: (code: String, name: String) -> Unit,
    onTargetLangChange: (code: String, name: String) -> Unit,
    initialSourceTtsLabel: String,
    initialTargetTtsLabel: String
) {
    var currentSourceTtsLabel by remember(initialSourceTtsLabel) { mutableStateOf(initialSourceTtsLabel) }
    var currentTargetTtsLabel by remember(initialTargetTtsLabel) { mutableStateOf(initialTargetTtsLabel) }

    fun hotReloadEngine(enginePackage: String) {
        host.speechHelper.cancelHelper()
        host.ttsHelper.initializeEngine(context, enginePackage)
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Языковые пары и Движки TTS", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            LanguageSideConfig(
                title = "Исходная конфигурация (Левая сторона)",
                langName = sourceLangName,
                ttsLabel = currentSourceTtsLabel,
                availableLanguages = availableLanguages,
                finalTtsList = finalTtsList,
                initialSpeed = settingsStore.sourceTtsSpeed,
                onLangSelected = { code, name -> onSourceLangChange(code, name) },
                onTtsSelected = { enginePackage ->
                    settingsStore.sourceTtsEngine = enginePackage
                    hotReloadEngine(enginePackage)
                },
                onTtsLabelChange = { currentSourceTtsLabel = it },
                onSpeedChange = { settingsStore.sourceTtsSpeed = it }
            )

            Divider(color = Color.Gray.copy(alpha = 0.2f), thickness = 1.dp)

            LanguageSideConfig(
                title = "Конфигурация перевода (Правая сторона)",
                langName = targetLangName,
                ttsLabel = currentTargetTtsLabel,
                availableLanguages = availableLanguages,
                finalTtsList = finalTtsList,
                initialSpeed = settingsStore.targetTtsSpeed,
                onLangSelected = { code, name -> onTargetLangChange(code, name) },
                onTtsSelected = { enginePackage ->
                    settingsStore.targetTtsEngine = enginePackage
                    hotReloadEngine(enginePackage)
                },
                onTtsLabelChange = { currentTargetTtsLabel = it },
                onSpeedChange = { settingsStore.targetTtsSpeed = it }
            )
        }
    }
}