package com.fagarester.translator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity(), VoiceHost {

    companion object {
        @Volatile
        var isForeground: Boolean = false
    }

    private lateinit var settingsStore: SettingsStore
    override lateinit var speechHelper: SpeechHelper
    override lateinit var ttsHelper: TtsHelper

    override var recognizedTextState = mutableStateOf("")
    override val newChatRequestState = mutableStateOf(0)

    private lateinit var liveAudioRouter: LiveAudioRouter

    private val selectedModelFileState = mutableStateOf("")
    val copyProgressState = mutableStateOf<Float?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Разрешение на микрофон отклонено", Toast.LENGTH_SHORT).show()
        } else if (settingsStore.sttEngine == "notune") {
            dev.notune.transcribe.NotuneWarmup.warmupIfNeeded(this)
        }
    }

    private val pickModelFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importModelFile(it) }
    }

private val pickNotuneModelLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importNotuneModel(it) }
    }

    fun pickNotuneModelFile() {
        pickNotuneModelLauncher.launch(arrayOf("*/*"))
    }

    private fun importNotuneModel(uri: Uri) {
        lifecycleScope.launch {
            val result = NotuneModelImporter.importModel(contentResolver, filesDir, uri)
            result.onSuccess {
                Toast.makeText(this@MainActivity, "Модель notune импортирована", Toast.LENGTH_SHORT).show()
            }.onFailure { t ->
                Toast.makeText(this@MainActivity, "Ошибка импорта: ${t.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    /**
     * Вызывается системой, когда Activity уже запущена (launchMode="singleTask")
     * и её вызывают повторно — например, ассистентом по кнопке питания.
     * В этом случае должен открываться НОВЫЙ чат, а не тот, что был на экране.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_ASSIST) {
            newChatRequestState.value++
        }
    }

    @OptIn(com.google.ai.edge.litertlm.ExperimentalApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)

        settingsStore = SettingsStore(this)

        // Применяем сохранённое значение флага спекулятивного декодинга
        // сразу при старте — до этого он не выставлен движку.
        try {
            com.google.ai.edge.litertlm.ExperimentalFlags.enableSpeculativeDecoding = settingsStore.aiSpeculativeDecoding
        } catch (t: Throwable) {
            // игнор
        }

        speechHelper = SpeechHelper(this, settingsStore)
        ttsHelper = TtsHelper(this, settingsStore) {}
        liveAudioRouter = LiveAudioRouter(this)

        selectedModelFileState.value = settingsStore.selectedModelFile

if (settingsStore.sttEngine == "notune" &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            dev.notune.transcribe.NotuneWarmup.warmupIfNeeded(this)
        }

        if (settingsStore.engineMode == "local" && settingsStore.activeModelPath.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                LocalEngineManager.preload(
                    this@MainActivity,
                    settingsStore.activeModelPath,
                    settingsStore.getModelBackend(settingsStore.activeModelPath),
                    settingsStore.aiMaxTokens
                )
            }
        }

        checkMicrophonePermission()

        val chatRepository = ChatRepository(settingsStore)

        setContent {
            var currentScreen by remember {
                mutableStateOf(if (settingsStore.onboardingCompleted) "translator" else "onboarding_welcome")
            }
            var isDarkTheme by remember { mutableStateOf(settingsStore.darkTheme) }

            // Создаётся один раз и живёт, пока жива Activity — не пересоздаётся
            // при переходе в настройки и обратно (иначе терялся бы текущий чат).
            val viewModel = remember {
                TranslationViewModel(
                    settingsStore = settingsStore,
                    chatRepository = chatRepository,
                    ttsHelper = ttsHelper,
                    appContext = this@MainActivity
                )
            }

            MaterialTheme(
                colorScheme = if (isDarkTheme) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
            ) {
                // Статус-бар под цвет верхней панели, чтобы не было резкого
                // перехода чёрная системная полоса -> светлая панель.
                val statusBarColor = if (isDarkTheme)
                    androidx.compose.material3.darkColorScheme().surface
                else
                    androidx.compose.ui.graphics.Color(0xFFE7DFEC)

                SideEffect {
                    window.statusBarColor = statusBarColor.toArgb()
                    androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                        .isAppearanceLightStatusBars = !isDarkTheme
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isDarkTheme)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else
                                androidx.compose.ui.graphics.lerp(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    androidx.compose.ui.graphics.Color(0xFFE7DFEC),
                                    0.5f
                                ).copy(alpha = 0.65f)
                        )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            "onboarding_welcome" -> {
                                OnboardingWelcomeScreen(
                                    onNext = { currentScreen = "onboarding_download" }
                                )
                            }
                            "onboarding_download" -> {
                                OnboardingDownloadScreen(
                                    filesDir = filesDir,
                                    settingsStore = settingsStore,
                                    selectedModelFileState = selectedModelFileState,
                                    onFinish = {
                                        settingsStore.onboardingCompleted = true
                                        currentScreen = "translator"
                                    }
                                )
                            }
                            "translator" -> {
                                TranslatorScreen(
                                    settingsStore = settingsStore,
                                    viewModel = viewModel,
                                    host = this@MainActivity,
                                    recognizedText = recognizedTextState.value,
                                    onTextConsumed = { recognizedTextState.value = "" },
                                    onNavigateToSettings = { currentScreen = "settings" }
                                )
                            }
                            "settings" -> {
                                SettingsScreen(
                                    settingsStore = settingsStore,
                                    host = this@MainActivity,
                                    selectedModelFileName = selectedModelFileState.value,
                                    onPickModelFile = { pickModelFileLauncher.launch(arrayOf("*/*")) },
                                    onModelFileConsumed = { clearSelectedModelFile() },
                                    copyProgress = copyProgressState.value,
                                    onThemeChange = { isDarkTheme = it },
                                    onBack = {
                                        currentScreen = "translator"
                                    }
                                )
                            }
                        }

                        ModelLoadingOverlay()
                    }
                }
            }
        }
    }
private fun importModelFile(uri: Uri) {
        lifecycleScope.launch {
            ModelFileImporter.copyModelFileToAppStorage(
                contentResolver = contentResolver,
                filesDir = filesDir,
                uri = uri,
                settingsStore = settingsStore,
                selectedModelFileState = selectedModelFileState,
                copyProgressState = copyProgressState,
                onError = { message ->
                    Toast.makeText(this@MainActivity, "Ошибка копирования файла: $message", Toast.LENGTH_LONG).show()
                },
                onSuccess = { name ->
                    Toast.makeText(this@MainActivity, "Модель скопирована: $name", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun clearSelectedModelFile() {
        settingsStore.selectedModelFile = ""
        selectedModelFileState.value = ""
    }

    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
    }

    override fun onPause() {
        super.onPause()
        isForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()
        liveAudioRouter.disable()
        ttsHelper.shutdown()
        speechHelper.stopListening()
    }
}