package com.fagarester.translator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun SettingsSlidersCard(settingsStore: SettingsStore, onThemeChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ThemeCard(settingsStore = settingsStore, onThemeChange = onThemeChange)
        HudMetricsCard(settingsStore = settingsStore)
        VadTimingCard(settingsStore = settingsStore)
        TtsSyncCard(settingsStore = settingsStore)
    }
}