package com.fagarester.translator

import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsHelper(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val onInitComplete: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var currentActiveEngine = ""

    // Очередь текста на случай, если движок ещё не успел загрузиться
    private var pendingText = ""
    private var pendingLocale: Locale? = null
    private var pendingSpeedFactor = 100

    init {
        initializeEngine(context, settingsStore.sourceTtsEngine)
    }

    fun initializeEngine(context: Context, enginePackage: String) {
        try {
            if (tts != null && currentActiveEngine == enginePackage) return

            shutdown()
            isReady = false
            currentActiveEngine = enginePackage

            var targetPackage = enginePackage
            if (targetPackage.isEmpty()) {
                val pm = context.packageManager
                val localEngines = listOf(
                    "com.github.kewlbear.sherpa_onnx",
                    "org.sherpa_onnx",
                    "com.github.org.sherpa",
                    "org.kde.sherpa",
                    "com.supertonik.tts"
                )
                for (packageName in localEngines) {
                    try {
                        pm.getPackageInfo(packageName, 0)
                        targetPackage = packageName
                        break
                    } catch (e: PackageManager.NameNotFoundException) { /* пробуем следующий */ }
                }
            }

            tts = if (targetPackage.isNotEmpty()) {
                TextToSpeech(context, this, targetPackage)
            } else {
                TextToSpeech(context, this)
            }
        } catch (e: Exception) {
            tts = TextToSpeech(context, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isReady = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {}
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {}
            })

            // Если движок поднялся, а в очереди есть пропущенный текст — озвучиваем его сразу
            if (pendingText.isNotEmpty() && pendingLocale != null) {
                try {
                    speakInternal(pendingText, pendingLocale!!, pendingSpeedFactor)
                } catch (e: Exception) {
                    // игнор
                } finally {
                    pendingText = ""
                    pendingLocale = null
                }
            }

            onInitComplete(true)
        } else {
            isReady = false
            onInitComplete(false)
        }
    }

    fun speak(text: String, langCode: String) {
        try {
            val requiredEngine: String
            val speedFactor: Int

            if (langCode == settingsStore.sourceLangCode) {
                requiredEngine = settingsStore.sourceTtsEngine
                speedFactor = settingsStore.sourceTtsSpeed
            } else {
                requiredEngine = settingsStore.targetTtsEngine
                speedFactor = settingsStore.targetTtsSpeed
            }

            val locale = Locale.forLanguageTag(langCode)

            if (currentActiveEngine != requiredEngine) {
                pendingText = text
                pendingLocale = locale
                pendingSpeedFactor = speedFactor
                initializeEngine(context, requiredEngine)
                return
            }

            if (!isReady || tts == null || text.isEmpty()) return

            speakInternal(text, locale, speedFactor)
        } catch (e: Exception) {
            // игнор
        }
    }

    private fun speakInternal(text: String, locale: Locale, speedFactor: Int) {
        tts?.language = locale
        val speedValue = speedFactor.toFloat() / 100f
        if (speedValue in 0.5f..2.5f) {
            tts?.setSpeechRate(speedValue)
        }
        val params = android.os.Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "msg_id")
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "msg_id")
    }

    fun stop() {
        try { if (isReady) tts?.stop() } catch (e: Exception) { /* игнор */ }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isReady = false
        } catch (e: Exception) {
            // игнор
        }
    }
}