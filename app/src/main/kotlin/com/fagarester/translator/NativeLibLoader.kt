package com.fagarester.translator

object NativeLibLoader {
    private var loaded = false

    fun preloadQnnLibs() {
        if (loaded) return
        loaded = true

        // Общие библиотеки — одинаковые для всех чипов Qualcomm
        val commonLibs = listOf("QnnSystem", "QnnHtp", "LiteRtDispatch_Qualcomm")
        for (lib in commonLibs) {
            try {
                System.loadLibrary(lib)
            } catch (t: Throwable) {
                // игнор: библиотека недоступна на этом чипе
            }
        }

        // Специфичная под чип Stub-библиотека (версия Hexagon NPU)
        val stubLib = ChipLibraryMap.getStubLibraryName() ?: return

        try {
            System.loadLibrary(stubLib)
        } catch (t: Throwable) {
            // игнор
        }
    }
}
