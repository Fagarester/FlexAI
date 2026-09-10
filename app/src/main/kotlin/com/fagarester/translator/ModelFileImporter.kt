package com.fagarester.translator

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ModelFileImporter {

    suspend fun copyModelFileToAppStorage(
        contentResolver: ContentResolver,
        filesDir: File,
        uri: Uri,
        settingsStore: SettingsStore,
        selectedModelFileState: MutableState<String>,
        copyProgressState: MutableState<Float?>,
        onError: (String) -> Unit,
        onSuccess: (String) -> Unit
    ) {
        val originalName = getFileName(contentResolver, uri) ?: "model_${System.currentTimeMillis()}.litertlm"
        val destFile = File(filesDir, originalName)
        val totalSize = getFileSize(contentResolver, uri)

        copyProgressState.value = 0f

        try {
            withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        var bytesCopied = 0L
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            bytesCopied += bytesRead
                            if (totalSize > 0) {
                                val progress = (bytesCopied.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f)
                                withContext(Dispatchers.Main) { copyProgressState.value = progress }
                            }
                        }
                    }
                }
            }

            settingsStore.selectedModelFile = destFile.absolutePath
            selectedModelFileState.value = destFile.absolutePath
            copyProgressState.value = null
            onSuccess(originalName)
        } catch (e: Exception) {
            copyProgressState.value = null
            onError(e.message ?: "неизвестная ошибка")
        }
    }

    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun getFileSize(contentResolver: ContentResolver, uri: Uri): Long {
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return size
    }
}