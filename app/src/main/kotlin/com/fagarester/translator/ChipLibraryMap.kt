package com.fagarester.translator

import android.os.Build

/**
 * Сопоставляет SOC_MODEL (то, что вернул ChipDetector) с версией Hexagon NPU,
 * от которой зависит, какую именно QnnHtpVXXStub/Skel библиотеку нужно грузить.
 *
 * Добавление нового чипа = добавить одну строку в этот список,
 * при условии что соответствующие libQnnHtpVXXStub.so / libQnnHtpVXXSkel.so
 * уже лежат в app/src/main/jniLibs/arm64-v8a/.
 */
object ChipLibraryMap {

    private val chipToHexagonVersion = mapOf(
        "SM8550" to 73, // Snapdragon 8 Gen 2
        "SM8650" to 75, // Snapdragon 8 Gen 3
        "SM8750" to 79, // Snapdragon 8 Elite
        "SM8850" to 81  // Snapdragon 8 Elite Gen 5
    )

    /** Возвращает имя нужной Stub-библиотеки (без "lib" и ".so") либо null, если чип не в списке. */
    fun getStubLibraryName(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val socModel = Build.SOC_MODEL?.uppercase() ?: return null
        val version = chipToHexagonVersion[socModel] ?: return null
        return "QnnHtpV${version}Stub"
    }

    /** Человекочитаемое имя для логов/диагностики. */
    fun getDetectedChipLabel(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return "Неизвестно (Android < 12)"
        val socModel = Build.SOC_MODEL ?: return "Неизвестно"
        val version = chipToHexagonVersion[socModel.uppercase()]
        return if (version != null) "$socModel (Hexagon V$version)" else "$socModel (нет в списке поддержки)"
    }
}