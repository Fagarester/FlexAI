package com.fagarester.translator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dev.notune.transcribe.LiveSubtitleService
import java.util.Locale

class SpeechHelper(
    private val context: Context,
    private val settingsStore: SettingsStore
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Текст текущего предложения, ещё не завершённого точкой/?/! — копится
    // между вызовами onFinal, пока не наберётся законченное предложение.
    // Живёт на уровне инстанса, чтобы stopListening() мог "дослать" остаток.
    private val pendingChunkText = StringBuilder()
    private var chunkReadyCallback: ((String) -> Unit)? = null

    private fun getOrCreateRecognizer(): SpeechRecognizer {
        var recognizer = speechRecognizer
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
        }
        return recognizer
    }

    fun startListening(
        languageCode: String?,
        onResult: (String) -> Unit,
        onStatusChange: (String) -> Unit,
        onChunkReady: ((String) -> Unit)? = null
    ) {
        if (settingsStore.sttEngine == "notune") {
            startNotuneStreaming(onResult, onStatusChange, onChunkReady)
            return
        }

        mainHandler.post {
            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            if (languageCode != null) {
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            } else {
                val systemLanguage = Locale.getDefault().toLanguageTag()
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, systemLanguage)
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, systemLanguage)
                recognizerIntent.putExtra(
                    RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                    arrayListOf("ru-RU", "en-US")
                )
            }

            val recognizer = getOrCreateRecognizer()

            try {
                recognizer.cancel()
            } catch (e: Exception) {
                // игнор
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    onStatusChange("Слушаю, говорите...")
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    onStatusChange("Обработка речи...")
                }

                override fun onError(error: Int) {
                    onStatusChange("Ошибка или тишина. Ожидание...")
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    }
                    onStatusChange("Конец сессии")
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            try {
                recognizer.startListening(recognizerIntent)
            } catch (e: Exception) {
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
        }
    }

private fun startNotuneStreaming(
        onResult: (String) -> Unit,
        onStatusChange: (String) -> Unit,
        onChunkReady: ((String) -> Unit)?
    ) {
        val committedText = StringBuilder()
        pendingChunkText.clear()
        chunkReadyCallback = onChunkReady

        onStatusChange("Слушаю, говорите...")
        LiveSubtitleService.start(
            context = context,
            onPartial = { partial ->
                mainHandler.post {
                    val combined = if (committedText.isEmpty()) partial else "$committedText $partial"
                    onResult(combined)
                }
            },
            onFinal = { final ->
                mainHandler.post {
                    if (committedText.isNotEmpty()) committedText.append(' ')
                    committedText.append(final)
                    onResult(committedText.toString())

                    // Режим чанков: копим кусок до тех пор, пока где-то внутри
                    // не появится точка/?/! — тогда всё до неё (включительно)
                    // уходит на перевод отдельным законченным куском, а остаток
                    // (если есть) ждёт продолжения следующим onFinal.
                    if (onChunkReady != null && settingsStore.notuneChunkMode) {
                        val trimmedFinal = final.trim()
                        if (trimmedFinal.isNotEmpty()) {
                            if (pendingChunkText.isNotEmpty()) pendingChunkText.append(' ')
                            pendingChunkText.append(trimmedFinal)

                            val text = pendingChunkText.toString()
                            val lastPunctIndex = text.indexOfLast { it == '.' || it == '!' || it == '?' }
                            if (lastPunctIndex >= 0) {
                                val readyPart = text.substring(0, lastPunctIndex + 1).trim()
                                val remainder = text.substring(lastPunctIndex + 1).trim()
                                pendingChunkText.clear()
                                if (remainder.isNotEmpty()) pendingChunkText.append(remainder)
                                if (readyPart.isNotEmpty()) onChunkReady(readyPart)
                            }
                        }
                    }
                }
            }
        )
    }

    fun stopListening() {
        if (settingsStore.sttEngine == "notune") {
            LiveSubtitleService.stop()

            // Если фраза закончилась без точки/?/! — остаток всё равно нужно
            // перевести, иначе он просто потеряется в режиме чанков.
            val remainder = pendingChunkText.toString().trim()
            pendingChunkText.clear()
            if (remainder.isNotEmpty()) {
                chunkReadyCallback?.invoke(remainder)
            }
            chunkReadyCallback = null
            return
        }

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                // игнор
            }
        }
    }

    fun cancelHelper() {
        LiveSubtitleService.stop()
        pendingChunkText.clear()
        chunkReadyCallback = null

        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                // игнор
            }
        }
    }
}