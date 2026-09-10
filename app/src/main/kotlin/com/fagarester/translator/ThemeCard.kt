package com.fagarester.translator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThemeCard(settingsStore: SettingsStore, onThemeChange: (Boolean) -> Unit) {
    var darkTheme by remember { mutableStateOf(settingsStore.darkTheme) }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Тема", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("светлая/темная", fontSize = 16.sp)
                Switch(
                    checked = darkTheme,
                    onCheckedChange = {
                        darkTheme = it
                        settingsStore.darkTheme = it
                        onThemeChange(it)
                    }
                )
            }
        }
    }
}