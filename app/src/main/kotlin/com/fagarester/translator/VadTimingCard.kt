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
fun VadTimingCard(settingsStore: SettingsStore) {
    var debounceDelay by remember { mutableStateOf(settingsStore.debounceDelay.toFloat()) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Паузы и дебаунс (VAD)", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Пауза завершения фразы", fontSize = 15.sp)
                Text("${debounceDelay.toInt()} мс", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = debounceDelay, onValueChange = { debounceDelay = it; settingsStore.debounceDelay = it.toInt() }, valueRange = 300f..3000f, steps = 26)
        }
    }
}