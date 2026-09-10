package com.fagarester.translator

import android.os.Build

object ChipDetector {

    /**
     * Возвращает лучшую доступную информацию о чипе устройства.
     * Build.SOC_MODEL / Build.SOC_MANUFACTURER доступны только с Android 12 (API 31)+.
     * На более старых версиях используем резервные системные поля.
     */
    fun getChipInfo(): String {
        val sb = StringBuilder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            sb.append("SOC_MODEL: ${Build.SOC_MODEL}\n")
            sb.append("SOC_MANUFACTURER: ${Build.SOC_MANUFACTURER}\n")
        } else {
            sb.append("SOC_MODEL: недоступно (Android < 12)\n")
        }

        sb.append("HARDWARE: ${Build.HARDWARE}\n")
        sb.append("BOARD: ${Build.BOARD}\n")
        sb.append("DEVICE: ${Build.DEVICE}\n")
        sb.append("MODEL: ${Build.MODEL}\n")
        sb.append("MANUFACTURER: ${Build.MANUFACTURER}\n")
        sb.append("SUPPORTED_ABIS: ${Build.SUPPORTED_ABIS.joinToString(", ")}\n")

        return sb.toString()
    }
}