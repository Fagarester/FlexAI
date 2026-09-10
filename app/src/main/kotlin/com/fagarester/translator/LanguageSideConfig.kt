package com.fagarester.translator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Один параметризованный блок конфигурации стороны (исходная/целевая).
 * Заменяет два почти одинаковых блока из старого TtsConfigurationCard.
 */
@Composable
fun LanguageSideConfig(
    title: String,
    langName: String,
    ttsLabel: String,
    availableLanguages: List<Pair<String, String>>,
    finalTtsList: List<CustomTtsEngineInfo>,
    initialSpeed: Int,
    onLangSelected: (code: String, name: String) -> Unit,
    onTtsSelected: (enginePackage: String) -> Unit,
    onTtsLabelChange: (String) -> Unit,
    onSpeedChange: (Int) -> Unit
) {
    var langDropdownExpanded by remember { mutableStateOf(false) }
    var ttsDropdownExpanded by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(initialSpeed.toFloat()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                GradientButton(
                    onClick = { langDropdownExpanded = true },
                    brush = defaultButtonBrush(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(langName, fontSize = 14.sp, maxLines = 1)
                }
                DropdownMenu(expanded = langDropdownExpanded, onDismissRequest = { langDropdownExpanded = false }) {
                    availableLanguages.forEach { (code, name) ->
                        DropdownMenuItem(text = { Text(name) }, onClick = {
                            onLangSelected(code, name)
                            langDropdownExpanded = false
                        })
                    }
                }
            }

            Box(modifier = Modifier.weight(1.2f)) {
                GradientButton(
                    onClick = { ttsDropdownExpanded = true },
                    brush = defaultButtonBrush(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(ttsLabel, fontSize = 14.sp, maxLines = 1)
                }
                DropdownMenu(expanded = ttsDropdownExpanded, onDismissRequest = { ttsDropdownExpanded = false }) {
                    DropdownMenuItem(text = { Text("Системный по умолчанию") }, onClick = {
                        onTtsLabelChange("Системный по умолчанию")
                        ttsDropdownExpanded = false
                        onTtsSelected("")
                    })
                    finalTtsList.forEach { engine ->
                        DropdownMenuItem(text = { Text(engine.label) }, onClick = {
                            onTtsLabelChange(engine.label)
                            ttsDropdownExpanded = false
                            onTtsSelected(engine.name)
                        })
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text("Скорость: ${speed.toInt()}%", fontSize = 11.sp, color = Color.Gray)
            Slider(
                value = speed,
                onValueChange = { speed = it; onSpeedChange(it.toInt()) },
                valueRange = 50f..250f,
                modifier = Modifier.height(24.dp)
            )
        }
    }
}