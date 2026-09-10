package dev.notune.transcribe

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ВРЕМЕННЫЙ ТЕСТОВЫЙ ВАРИАНТ.
 *
 * Здесь при каждом start() движок полностью перезагружается через
 * cleanupNative() + initNative() — это специально сделано для замера,
 * сколько миллисекунд занимает полная перезагрузка.
 * Если окажется, что перезагрузка быстрая (условно до ~150-200 мс,
 * незаметно на глаз) — можно оставить такой подход насовсем, это
 * полностью убирает утечку контекста декодера между записями.
 * Если перезагрузка долгая (секунды) — откатываемся на версию без
 * реюза модели (та, что была раньше, с заплаткой от утечки текста).
 */
object LiveSubtitleService {
    private var isNativeLoaded = false
    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private data class SessionCallbacks(
        val onPartial: (String) -> Unit,
        val onFinal: (String) -> Unit
    )

    private var activeSession: SessionCallbacks? = null
    private var activeSessionStopped = false
    private val pendingSessions = ArrayDeque<SessionCallbacks>()
    private val sessionLock = Any()

    init {
        try {
            System.loadLibrary("c++_shared")
            System.loadLibrary("android_transcribe_app")
        } catch (e: UnsatisfiedLinkError) {
            // игнор
        }
    }

    /**
     * Полная перезагрузка движка (только для этого объекта, не трогает
     * MainActivity.ensureModelLoaded — тот отвечает за первичную загрузку
     * весов модели и вызывается один раз при старте приложения).
     */
    @Synchronized
    private fun reloadEngine(context: android.content.Context) {
        MainActivity.ensureModelLoaded(context)

        val t0 = System.currentTimeMillis()
        try {
            cleanupNative()
        } catch (t: Throwable) {
            // игнор
        }
        try {
            initNative(this)
            isNativeLoaded = true
        } catch (t: Throwable) {
            // игнор
        }
    }

    /** Вызывается из NotuneWarmup при старте приложения — просто прогревает большую модель заранее. */
    fun ensureModelLoaded(context: android.content.Context) {
        MainActivity.ensureModelLoaded(context)
    }

    fun start(context: android.content.Context, onPartial: (String) -> Unit, onFinal: (String) -> Unit) {
        if (isRecording.get()) {
            return
        }

        // ТЕСТ: перезагружаем движок при каждом нажатии кнопки записи
        reloadEngine(context)

        val newSession = SessionCallbacks(onPartial, onFinal)
        synchronized(sessionLock) {
            activeSession = newSession
            activeSessionStopped = false
            pendingSessions.clear()
        }

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 16000)

        try {
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, // Даёт заметно точнее распознавание языка/речи, чем MIC (тестировали оба)
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            audioRecord = recorder
            recorder.startRecording()
            isRecording.set(true)

            audioThread = Thread { audioLoop() }.apply { start() }
        } catch (t: Throwable) {
            // игнор
        }
    }

    private fun audioLoop() {
        val chunkSize = 1024
        val shortBuffer = ShortArray(chunkSize)
        val floatBuffer = FloatArray(chunkSize)

        // Программное усиление тихого голоса — работает уже ПОСЛЕ записи,
        // не трогает сам источник звука (VOICE_RECOGNITION остаётся как есть
        // для точности), поэтому не должно портить распознавание языка.
        // Усиливает только тихие куски, плавно, без резких скачков громкости.
        var smoothedGain = 1.0f
        val targetRms = 0.06f
        val maxGain = 6.0f

        while (isRecording.get()) {
            val read = audioRecord?.read(shortBuffer, 0, chunkSize) ?: -1
            if (read > 0) {
                var sumSquares = 0.0
                for (i in 0 until read) {
                    val sample = shortBuffer[i] / 32768.0f
                    floatBuffer[i] = sample
                    sumSquares += (sample * sample).toDouble()
                }

                val rms = kotlin.math.sqrt(sumSquares / read).toFloat()
                if (rms > 0.001f) {
                    val desiredGain = (targetRms / rms).coerceIn(1.0f, maxGain)
                    smoothedGain += (desiredGain - smoothedGain) * 0.2f
                }

                for (i in 0 until read) {
                    floatBuffer[i] = (floatBuffer[i] * smoothedGain).coerceIn(-1.0f, 1.0f)
                }

                try {
                    val chunk = floatBuffer.copyOf(read)
                    pushAudio(chunk, read)
                } catch (t: Throwable) {
                    isRecording.set(false)
                }
            }
        }
    }

    fun stop() {
        if (!isRecording.get()) return
        isRecording.set(false)

        synchronized(sessionLock) {
            activeSessionStopped = true
        }

        try {
            audioThread?.join(1000)
        } catch (e: InterruptedException) { /* игнор */ }
        audioThread = null

        audioRecord?.apply {
            try { stop() } catch (t: Throwable) { /* игнор */ }
            release()
        }
        audioRecord = null
    }

    // Вызывается из Rust-кода
    fun onSubtitleText(text: String, isFinal: Boolean) {
        synchronized(sessionLock) {
            val session = activeSession ?: return
            if (isFinal) {
                session.onFinal(text)
                if (activeSessionStopped) {
                    activeSession = pendingSessions.removeFirstOrNull()
                    activeSessionStopped = false
                }
            } else {
                session.onPartial(text)
            }
        }
    }

    private external fun initNative(service: LiveSubtitleService)
    private external fun cleanupNative()
    private external fun pushAudio(data: FloatArray, length: Int)
}