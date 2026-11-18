package pl.aingielski.domain.csv

/** One physical line of the file, split into fields. [line] is 1-based. */
data class CsvRecord(val line: Int, val fields: List<String>)

data class CsvParseResult(
    val delimiter: Char,
    val records: List<CsvRecord>,
    val unclosedQuoteLines: List<Int>,
)

/**
 * Minimal CSV reader for word-set files (contracts/csv-import-format.md, research R9):
 * UTF-8 BOM stripped, delimiter `;` or `,` detected from the first non-blank line, RFC 4180
 * quoting within a line (`""` is a literal quote), no line breaks inside fields, blank lines
 * skipped while keeping physical line numbers.
 */
object CsvParser {
    private val BOM = Char(0xFEFF)
    private val LINE_BREAK = Regex("\r\n|\n|\r")

    fun parse(text: String): CsvParseResult {
        val lines = text.removePrefix(BOM.toString()).split(LINE_BREAK)
        val firstContent = lines.firstOrNull { it.isNotBlank() }
        val delimiter = if (firstContent != null && hasUnquotedSemicolon(firstContent)) ';' else ','
        val records = mutableListOf<CsvRecord>()
        val unclosed = mutableListOf<Int>()
        lines.forEachIndexed { index, line ->
            if (line.isBlank()) return@forEachIndexed
            val lineNumber = index + 1
            val parsed = splitLine(line, delimiter)
            if (parsed == null) unclosed += lineNumber else records += CsvRecord(lineNumber, parsed)
        }
        return CsvParseResult(delimiter, records, unclosed)
    }

    private fun hasUnquotedSemicolon(line: String): Boolean {
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ';' && !inQuotes -> return true
            }
        }
        return false
    }

    /** Returns the trimmed fields, or null when a quoted field is never closed. */
    private fun splitLine(line: String, delimiter: Char): List<String>? {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    current.append(c)
                }
            } else {
                when {
                    c == delimiter -> {
                        fields += current.toString().trim()
                        current.clear()
                    }
                    c == '"' && current.isBlank() -> {
                        current.clear()
                        inQuotes = true
                    }
                    else -> current.append(c)
                }
            }
            i++
        }
        if (inQuotes) return null
        fields += current.toString().trim()
        return fields
    }
}
