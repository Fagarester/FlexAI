package com.fagarester.translator

/**
 * Определяет, предназначена ли модель ТОЛЬКО для NPU — по наличию суффикса чипа
 * в имени файла (например "_sm8750") — стандарт именования Google для
 * NPU-скомпилированных .litertlm моделей.
 */
private val NPU_SUFFIX_REGEX = Regex("_sm\\d", RegexOption.IGNORE_CASE)

fun isNpuOnlyModel(path: String): Boolean {
    val fileName = path.substringAfterLast("/")
    return NPU_SUFFIX_REGEX.containsMatchIn(fileName)
}

/**
 * Модель gemma-4-E4B-it (скачанная при онбординге или добавленная вручную
 * с тем же именем файла) всегда должна идти на GPU — независимо от того,
 * что сохранено в настройках backend'а.
 */
fun isGpuOnlyModel(path: String): Boolean {
    val fileName = path.substringAfterLast("/")
    return fileName.contains("gemma-4-E4B-it", ignoreCase = true)
}