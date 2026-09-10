package com.fagarester.translator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HudMetricsCard(settingsStore: SettingsStore) {
    var showTotal by remember { mutableStateOf(settingsStore.showMetricTotal) }
    var showModel by remember { mutableStateOf(settingsStore.showMetricModel) }
    var showFrontend by remember { mutableStateOf(settingsStore.showMetricFrontend) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Мониторинг и HUD баблов", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("Выберите метрики скорости, которые будут отображаться под сообщениями:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showTotal, onCheckedChange = { showTotal = it; settingsStore.showMetricTotal = it })
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Общее время цикла (Total)", fontSize = 16.sp)
                    Text("Полное время прохождения от микрофона до ответа сервера", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showModel, onCheckedChange = { showModel = it; settingsStore.showMetricModel = it })
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Время размышления ИИ (Model)", fontSize = 16.sp)
                    Text("Чистые миллисекунды генерации OlliteRT на NPU кристалла", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showFrontend, onCheckedChange = { showFrontend = it; settingsStore.showMetricFrontend = it })
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Задержка интерфейса (Frontend)", fontSize = 16.sp)
                    Text("Время работы OkHttp сокетов и системных потоков Android", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}