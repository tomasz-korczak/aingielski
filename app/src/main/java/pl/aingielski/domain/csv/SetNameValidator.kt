package pl.aingielski.domain.csv

import pl.aingielski.domain.text.normalizeKey

sealed interface SetNameError {
    data object Blank : SetNameError
    data class TooLong(val max: Int) : SetNameError
}

/** Set-name rules from data-model.md `word_set.name` / `word_set.name_key` (FR-055). */
object SetNameValidator {
    const val MAX_LENGTH = 50

    fun validate(raw: String): SetNameError? {
        val name = raw.trim()
        return when {
            name.isEmpty() -> SetNameError.Blank
            name.length > MAX_LENGTH -> SetNameError.TooLong(MAX_LENGTH)
            else -> null
        }
    }

    fun nameKey(name: String): String = normalizeKey(name)

    fun isSameName(a: String, b: String): Boolean = nameKey(a) == nameKey(b)

    /** Default name suggested after picking a file: the file name without `.csv` (FR-052). */
    fun defaultName(fileName: String?): String {
        if (fileName.isNullOrBlank()) return ""
        val base = if (fileName.endsWith(".csv", ignoreCase = true)) fileName.dropLast(4) else fileName
        return base.trim().take(MAX_LENGTH)
    }
}
