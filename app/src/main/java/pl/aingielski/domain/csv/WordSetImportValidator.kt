package pl.aingielski.domain.csv

import pl.aingielski.domain.model.WordItem
import pl.aingielski.domain.text.normalizeKey

enum class CsvField { WORD, TRANSLATION, SENTENCE }

sealed interface LineError {
    val line: Int

    data class MissingFields(override val line: Int, val fields: List<CsvField>) : LineError
    data class TooManyFields(override val line: Int, val count: Int) : LineError
    data class UnclosedQuote(override val line: Int) : LineError
    data class DuplicateWord(override val line: Int, val word: String, val firstLine: Int) : LineError
}

/** Outcome of validating an import file (data-model.md › Domain models `ImportResult`). */
sealed interface ImportResult {
    data class Success(val entries: List<WordItem>) : ImportResult
    data class Invalid(val errors: List<LineError>) : ImportResult
    data class TooFew(val count: Int) : ImportResult
    data object Empty : ImportResult
    data object Unreadable : ImportResult
}

/**
 * Validates a whole file before anything is saved (FR-053, FR-054, FR-054a). All errors are
 * collected and reported together, ordered by line number.
 */
object WordSetImportValidator {
    const val MIN_WORDS = 10
    private const val FIELD_COUNT = 3

    fun validate(text: String): ImportResult {
        val parsed = CsvParser.parse(text)
        if (parsed.records.isEmpty() && parsed.unclosedQuoteLines.isEmpty()) return ImportResult.Empty

        val errors = mutableListOf<LineError>()
        parsed.unclosedQuoteLines.forEach { errors += LineError.UnclosedQuote(it) }

        val entries = mutableListOf<WordItem>()
        val firstLineByKey = mutableMapOf<String, Int>()
        for (record in parsed.records) {
            val fields = record.fields
            if (fields.size > FIELD_COUNT) {
                errors += LineError.TooManyFields(record.line, fields.size)
                continue
            }
            val missing = CsvField.entries.filterIndexed { index, _ -> fields.getOrNull(index).isNullOrBlank() }
            if (missing.isNotEmpty()) {
                errors += LineError.MissingFields(record.line, missing)
                continue
            }
            val key = normalizeKey(fields[0])
            val firstLine = firstLineByKey[key]
            if (firstLine != null) {
                errors += LineError.DuplicateWord(record.line, fields[0], firstLine)
                continue
            }
            firstLineByKey[key] = record.line
            entries += WordItem(word = fields[0], translation = fields[1], sentence = fields[2])
        }

        return when {
            errors.isNotEmpty() -> ImportResult.Invalid(errors.sortedBy { it.line })
            entries.size < MIN_WORDS -> ImportResult.TooFew(entries.size)
            else -> ImportResult.Success(entries)
        }
    }
}
