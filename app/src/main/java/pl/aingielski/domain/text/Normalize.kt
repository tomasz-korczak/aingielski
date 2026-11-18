package pl.aingielski.domain.text

import java.util.Locale

private val WHITESPACE = Regex("\\s+")

/**
 * Normalization rule shared by set names, word keys and answer checking (data-model.md):
 * trim, collapse inner whitespace, lowercase independently of the device language.
 * Punctuation is never removed.
 */
fun normalizeKey(value: String): String =
    value.trim().replace(WHITESPACE, " ").lowercase(Locale.ROOT)

/** Answer checking (FR-025, FR-025a): case- and whitespace-insensitive, otherwise exact. */
object AnswerChecker {
    fun isCorrect(answer: String, expected: String): Boolean =
        normalizeKey(answer) == normalizeKey(expected)
}
