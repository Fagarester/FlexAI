package dev.notune.transcribe

object NotuneWarmup {
    fun warmupIfNeeded(context: android.content.Context) {
        Thread {
            LiveSubtitleService.ensureModelLoaded(context)
        }.start()
    }
}