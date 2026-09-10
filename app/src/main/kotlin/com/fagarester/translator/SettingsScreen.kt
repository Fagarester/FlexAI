package com.fagarester.translator

import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler

data class CustomTtsEngineInfo(val name: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsStore: SettingsStore,
    host: VoiceHost,
    selectedModelFileName: String,
    onPickModelFile: () -> Unit,
    onModelFileConsumed: () -> Unit,
    copyProgress: Float?,
    onThemeChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    BackHandler(onBack = onBack)
    var finalTtsList by remember { mutableStateOf(listOf<CustomTtsEngineInfo>()) }
    var currentSourceTtsLabel by remember { mutableStateOf("Системный по умолчанию") }
    var currentTargetTtsLabel by remember { mutableStateOf("Системный по умолчанию") }

    val availableLanguages = remember { LanguageData.getAvailableLanguages() }

    val importedList = remember {
        val list = mutableStateListOf<String>()
        list.addAll(settingsStore.getImportedModels())
        list
    }

    val activePathState = remember { mutableStateOf(settingsStore.activeModelPath) }

    var sourceLangNameState by remember { mutableStateOf(settingsStore.sourceLangName) }
    var targetLangNameState by remember { mutableStateOf(settingsStore.targetLangName) }
    LaunchedEffect(Unit) {
        try {
            val resolvedList = mutableListOf<CustomTtsEngineInfo>()
            val tempTts = TextToSpeech(context, null)
            tempTts.engines.forEach {
                val displayLabel = if (it.name == "com.google.android.tts") "Google TTS" else it.label
                resolvedList.add(CustomTtsEngineInfo(it.name, displayLabel))
            }
            tempTts.shutdown()

            val pm = context.packageManager
            val commonEngines = listOf(
                "com.github.kewlbear.sherpa_onnx" to "Sherpa ONNX (Kewlbear)",
                "org.sherpa_onnx" to "Sherpa-ONNX Engine",
                "com.github.org.sherpa" to "Sherpa Local TTS",
                "org.kde.sherpa" to "Sherpa TTS (KDE Build)",
                "com.supertonik.tts" to "Supertonik Синтезатор",
                "com.google.android.tts" to "Google TTS"
            )

            commonEngines.forEach { (packageName, humanLabel) ->
                try {
                    pm.getPackageInfo(packageName, 0)
                    if (resolvedList.none { it.name == packageName }) {
                        resolvedList.add(CustomTtsEngineInfo(packageName, humanLabel))
                    }
                } catch (e: PackageManager.NameNotFoundException) { /* движок не установлен */ }
            }

            finalTtsList = resolvedList

            val activeSource = settingsStore.sourceTtsEngine
            val foundSource = finalTtsList.find { it.name == activeSource }
            currentSourceTtsLabel = foundSource?.label ?: if (activeSource.isNotEmpty()) activeSource.substringAfterLast(".") else "Системный по умолчанию"

            val activeTarget = settingsStore.targetTtsEngine
            val foundTarget = finalTtsList.find { it.name == activeTarget }
            currentTargetTtsLabel = foundTarget?.label ?: if (activeTarget.isNotEmpty()) activeTarget.substringAfterLast(".") else "Системный по умолчанию"

        } catch (e: Exception) {
            // игнор
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.clip(
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        bottomStart = 24.dp,
                        bottomEnd = 24.dp
                    )
                ),
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f)
                        androidx.compose.ui.graphics.Color(0xFFE7DFEC)
                    else
                        MaterialTheme.colorScheme.surface,
                    titleContentColor = androidx.compose.ui.graphics.Color(0xFF7968B0),
                    navigationIconContentColor = androidx.compose.ui.graphics.Color(0xFF7968B0),
                    actionIconContentColor = androidx.compose.ui.graphics.Color(0xFF7968B0)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            ModelManagerCard(
                settingsStore = settingsStore,
                selectedModelFileName = selectedModelFileName,
                onPickModelFile = onPickModelFile,
                onModelFileConsumed = onModelFileConsumed,
                coroutineScope = coroutineScope,
                context = context,
                importedList = importedList,
                copyProgress = copyProgress,
                activePathState = activePathState
            )

            SttEngineCard(settingsStore = settingsStore)

            TtsConfigurationCard(
                settingsStore = settingsStore,
                availableLanguages = availableLanguages,
                finalTtsList = finalTtsList,
                host = host,
                context = context,
                sourceLangName = sourceLangNameState,
                targetLangName = targetLangNameState,
                onSourceLangChange = { code, name ->
                    sourceLangNameState = name
                    settingsStore.sourceLangCode = code
                    settingsStore.sourceLangName = name
                },
                onTargetLangChange = { code, name ->
                    targetLangNameState = name
                    settingsStore.targetLangCode = code
                    settingsStore.targetLangName = name
                },
                initialSourceTtsLabel = currentSourceTtsLabel,
                initialTargetTtsLabel = currentTargetTtsLabel
            )

            SettingsSlidersCard(settingsStore = settingsStore, onThemeChange = onThemeChange)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}