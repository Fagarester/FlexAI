package com.fagarester.translator

/**
 * Эвристика для режима "Automatic": пытается понять, на каком из двух выбранных
 * языков (исходном или целевом) сказана фраза.
 *
 * Простая проверка "это кириллица или нет" не различает языки с одинаковым
 * алфавитом (например, русский и украинский — оба кириллические), поэтому для
 * такой пары добавлена проверка по уникальным буквам. Для остальных пар
 * (например, русский-английский) работает прежняя логика: кириллица = исходный,
 * не кириллица = целевой.
 */
object LanguageDetector {

    private val ukrainianOnlyLetters = setOf('і', 'ї', 'є', 'ґ', 'І', 'Ї', 'Є', 'Ґ')
    private val russianOnlyLetters = setOf('ё', 'ъ', 'ы', 'э', 'Ё', 'Ъ', 'Ы', 'Э')

    /** true = фраза похожа на исходный язык, false = на целевой. */
    fun looksLikeSourceLanguage(text: String, sourceLangCode: String, targetLangCode: String): Boolean {
        val isRuUkPair = (sourceLangCode.startsWith("ru") && targetLangCode.startsWith("uk")) ||
            (sourceLangCode.startsWith("uk") && targetLangCode.startsWith("ru"))

        if (isRuUkPair) {
            val hasUkrainianMarker = text.any { it in ukrainianOnlyLetters }
            val hasRussianMarker = text.any { it in russianOnlyLetters }

            if (hasUkrainianMarker && !hasRussianMarker) {
                return sourceLangCode.startsWith("uk")
            }
            if (hasRussianMarker && !hasUkrainianMarker) {
                return sourceLangCode.startsWith("ru")
            }
            // Нет уникальных букв ни одного из языков — используем запасной вариант ниже
        }

        return text.any { it in '\u0400'..'\u04FF' }
    }
}