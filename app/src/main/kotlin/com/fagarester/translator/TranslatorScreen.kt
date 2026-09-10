package com.fagarester.translator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    settingsStore: SettingsStore,
    viewModel: TranslationViewModel,
    host: VoiceHost,
    recognizedText: String,
    onTextConsumed: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

    var isGlobalTtsEnabled by remember { mutableStateOf(true) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteDialogText by remember { mutableStateOf("") }

    val filteredMessages = remember(viewModel.allMessages, viewModel.currentChatId, viewModel.listUpdateTrigger) {
        viewModel.allMessages.filter { it.chatId == viewModel.currentChatId }
    }

    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotEmpty()) {
            viewModel.processRecognizedText(recognizedText, coroutineScope, isGlobalTtsEnabled)
            onTextConsumed()
        }
    }

    // Повторный вызов через ассистента (кнопка питания) — всегда новый чат,
    // а не продолжение того, что уже было открыто.
    LaunchedEffect(host.newChatRequestState.value) {
        if (host.newChatRequestState.value > 0) {
            viewModel.createNewSession()
        }
    }

    var isFirstScroll by remember { mutableStateOf(true) }
    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            if (isFirstScroll) {
                listState.scrollToItem(filteredMessages.size - 1)
                isFirstScroll = false
            } else {
                kotlinx.coroutines.delay(100)
                listState.animateScrollToItem(filteredMessages.size - 1)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatHistoryDrawer(
                sessions = viewModel.sessions,
                currentChatId = viewModel.currentChatId,
                onSessionSelected = { id ->
                    viewModel.currentChatId = id
                    settingsStore.currentChatId = id
                    coroutineScope.launch { drawerState.close() }
                },
                onNewSession = {
                    viewModel.createNewSession()
                    coroutineScope.launch { drawerState.close() }
                },
                onDeleteSession = { viewModel.deleteSession(it) },
                onClearAllHistory = {
                    viewModel.clearAllHistory()
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
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
                    title = { Text("AI Переводчик", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню истории")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                pasteDialogText = ""
                                showPasteDialog = true
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Вставить текст для перевода"
                            )
                        }
                        IconButton(
                            onClick = {
                                isGlobalTtsEnabled = !isGlobalTtsEnabled
                                if (!isGlobalTtsEnabled) host.ttsHelper.stop()
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            SpeakerIcon(
                                isMuted = !isGlobalTtsEnabled,
                                backgroundColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = { onNavigateToSettings() }) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки")
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
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                BubblesTranslateContent(
                    currentChatMessages = filteredMessages,
                    allMessages = viewModel.allMessages,
                    listState = listState,
                    isLoading = viewModel.isLoading,
                    currentEditingBubbleId = viewModel.currentEditingBubbleId,
                    activeRecordMode = viewModel.activeRecordMode,
                    host = host,
                    onModeChange = { viewModel.activeRecordMode = it },
                    onEditingIdChange = { viewModel.currentEditingBubbleId = it },
                    onRetranslate = { id -> viewModel.retranslateMessage(id, coroutineScope) },
                    onDeleteMessage = { id -> viewModel.deleteMessage(id) },
                    onChunkReady = { chunkText -> viewModel.processChunk(chunkText, coroutineScope, isGlobalTtsEnabled) },
                    settingsStore = settingsStore
                )
            }
        }
    }

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Вставить текст для перевода") },
            text = {
                OutlinedTextField(
                    value = pasteDialogText,
                    onValueChange = { pasteDialogText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    placeholder = { Text("Введите или вставьте текст...") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val text = pasteDialogText.trim()
                        showPasteDialog = false
                        if (text.isNotEmpty()) {
                            // Сброс, как перед стартом новой записи с микрофона (startRecording()):
                            // иначе вставка текста может "доклеиться" к предыдущему баблу
                            // вместо создания нового.
                            viewModel.currentEditingBubbleId = null
                            viewModel.isCurrentSessionRight = null
                            viewModel.processRecognizedText(text, coroutineScope, isGlobalTtsEnabled)
                        }
                    }
                ) {
                    Text("Отправить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}