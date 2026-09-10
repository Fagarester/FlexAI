package com.fagarester.translator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TtsSyncCard(settingsStore: SettingsStore) {
    var ttsSpeedFactor by remember { mutableStateOf(settingsStore.ttsSpeedFactor.toFloat()) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Синхронизация конвейера TTS", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Удержание кнопки (мс на символ)", fontSize = 15.sp)
                Text("${ttsSpeedFactor.toInt()} мс", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = ttsSpeedFactor, onValueChange = { ttsSpeedFactor = it; settingsStore.ttsSpeedFactor = it.toInt() }, valueRange = 50f..250f, steps = 20)
        }
    }
}