package com.fagarester.translator

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object NotuneModelImporter {

    /**
     * Копирует выбранный .gguf файл в filesDir/models/ и записывает
     * файл-маркер active_model — это ровно тот механизм, который
     * нативный Rust-код notune ищет сам при старте.
     */
    suspend fun importModel(
        contentResolver: ContentResolver,
        filesDir: File,
        uri: Uri,
        modelFileName: String = "parakeet-tdt-0.6b-v3-Q4_K_M.gguf"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val modelsDir = File(filesDir, "models").apply { mkdirs() }
            val destFile = File(modelsDir, modelFileName)

            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 1024 * 1024)
                }
            } ?: return@withContext Result.failure(Exception("Не удалось открыть выбранный файл"))

            val markerFile = File(filesDir, "active_model")
            markerFile.writeText(modelFileName)

            Result.success(destFile)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}