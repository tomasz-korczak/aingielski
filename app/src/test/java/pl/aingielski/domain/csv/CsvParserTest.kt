package pl.aingielski.domain.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvParserTest {
    @Test
    fun detectsSemicolonWhenFirstLineHasOne() {
        val result = CsvParser.parse("apple;jabłko;I eat an apple, every day.\ncat;kot;The cat sleeps.")
        assertEquals(';', result.delimiter)
        assertEquals(listOf("apple", "jabłko", "I eat an apple, every day."), result.records[0].fields)
    }

    @Test
    fun usesCommaWhenFirstLineHasNoUnquotedSemicolon() {
        val result = CsvParser.parse("apple,jabłko,\"I eat; an apple.\"\ncat,kot,The cat sleeps.")
        assertEquals(',', result.delimiter)
        assertEquals(listOf("apple", "jabłko", "I eat; an apple."), result.records[0].fields)
    }

    @Test
    fun supportsRfc4180Quoting() {
        val result = CsvParser.parse("say,mówić,\"He said \"\"hi\"\", then left.\"")
        assertEquals(listOf("say", "mówić", "He said \"hi\", then left."), result.records[0].fields)
    }

    @Test
    fun stripsBomAndHandlesCrLf() {
        val result = CsvParser.parse(Char(0xFEFF) + "apple;jabłko;Sentence one.\r\ncat;kot;Sentence two.\r\n")
        assertEquals(2, result.records.size)
        assertEquals("apple", result.records[0].fields[0])
        assertEquals("Sentence two.", result.records[1].fields[2])
    }

    @Test
    fun skipsBlankLinesButKeepsPhysicalLineNumbers() {
        val result = CsvParser.parse("\n  \na;b;c\n\nd;e;f")
        assertEquals(listOf(3, 5), result.records.map { it.line })
    }

    @Test
    fun reportsUnclosedQuote() {
        val result = CsvParser.parse("a;b;c\nd;e;\"never closed")
        assertEquals(listOf(2), result.unclosedQuoteLines)
        assertEquals(1, result.records.size)
    }

    @Test
    fun keepsExtraFieldsSoValidatorCanReportThem() {
        val result = CsvParser.parse("a,b,c,d")
        assertEquals(4, result.records[0].fields.size)
    }

    @Test
    fun trimsFields() {
        val result = CsvParser.parse("  apple ;  jabłko ; Sentence.  ")
        assertEquals(listOf("apple", "jabłko", "Sentence."), result.records[0].fields)
    }
}
