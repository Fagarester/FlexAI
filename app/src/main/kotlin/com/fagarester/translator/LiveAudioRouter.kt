package com.fagarester.translator

import android.content.Context
import android.media.AudioManager

/**
 * ⚠️ Сейчас нигде не вызывается — вынесено из старого MainActivity как есть,
 * на случай если понадобится для будущей функции "живой перевод через Bluetooth".
 * Если фича не планируется — этот файл можно смело удалить.
 */
class LiveAudioRouter(private val context: Context) {
    private var savedSystemVolume = -1
    private var savedNotificationVolume = -1

    fun enable() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            if (savedSystemVolume == -1) {
                savedSystemVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
                savedNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
            }

            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)

            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isBluetoothScoOn = true
            audioManager.startBluetoothSco()
        } catch (e: Exception) {
            // игнор
        }
    }

    fun disable() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isBluetoothScoOn = false
            audioManager.stopBluetoothSco()

            if (savedSystemVolume != -1) {
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, savedSystemVolume, 0)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedNotificationVolume, 0)
                savedSystemVolume = -1
                savedNotificationVolume = -1
            }
        } catch (e: Exception) {
            // игнор
        }
    }
}