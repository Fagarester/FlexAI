package dev.notune.transcribe

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import java.io.File

/**
 * Заглушка вместо dev.notune.transcribe.MainActivity — именно она у notune
 * отвечает за реальную загрузку модели. Rust-код при инициализации вызывает
 * у переданного объекта не только getFilesDir() (есть у любого Context),
 * но и свой собственный колбэк onStatusUpdate(String) — поэтому наш объект
 * должен реализовать этот метод сам, обычный ApplicationContext для этого
 * не подходит (NoSuchMethodError).
 */
class MainActivity(base: Context) : ContextWrapper(base) {

    companion object {
        private const val TAG = "NotuneMainActivity"
        @Volatile private var loadTriggered = false

        fun ensureModelLoaded(context: Context) {
            if (loadTriggered) return

            // Если модели ещё нет (не скачана и не импортирована) — initNative()
            // падает нативным крэшем всего процесса, а не Kotlin-исключением,
            // которое можно было бы поймать. Поэтому проверяем ДО вызова.
            val filesDir = context.filesDir
            val markerFile = File(filesDir, "active_model")
            if (!markerFile.exists()) return
            val modelFileName = markerFile.readText().trim()
            if (modelFileName.isEmpty()) return
            val modelFile = File(File(filesDir, "models"), modelFileName)
            if (!modelFile.exists() || modelFile.length() == 0L) return

            loadTriggered = true

            try {
                val wrapper = MainActivity(context.applicationContext)
                initNative(wrapper)
            } catch (t: Throwable) {
                Log.e(TAG, "Сбой initNative", t)
            }
        }

        @JvmStatic private external fun initNative(context: MainActivity)
    }

    // Вызывается из Rust-кода как обязательный колбэк прогресса загрузки
    // модели. Тело оставлено пустым намеренно — метод удалять нельзя,
    // без него будет NoSuchMethodError при вызове из нативного кода.
    fun onStatusUpdate(status: String) {}
}