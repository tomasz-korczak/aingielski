package pl.aingielski.domain.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSetImportValidatorTest {
    private fun lines(count: Int, prefix: String = "word") =
        (1..count).joinToString("\n") { "$prefix$it;słowo$it;Sentence number $it." }

    @Test
    fun validFileReturnsTrimmedEntriesInFileOrder() {
        val result = WordSetImportValidator.validate(lines(12))
        assertTrue(result is ImportResult.Success)
        val entries = (result as ImportResult.Success).entries
        assertEquals(12, entries.size)
        assertEquals("word1", entries.first().word)
        assertEquals("słowo12", entries.last().translation)
    }

    @Test
    fun missingSentenceReportsLineAndField() {
        val text = lines(6) + "\nbroken;zepsuty;\n" + lines(5, "more")
        val result = WordSetImportValidator.validate(text) as ImportResult.Invalid
        assertEquals(listOf(LineError.MissingFields(7, listOf(CsvField.SENTENCE))), result.errors)
    }

    @Test
    fun missingSeveralFieldsAreListedTogether() {
        val text = lines(10) + "\nonlyword"
        val result = WordSetImportValidator.validate(text) as ImportResult.Invalid
        assertEquals(LineError.MissingFields(11, listOf(CsvField.TRANSLATION, CsvField.SENTENCE)), result.errors.single())
    }

    @Test
    fun tooManyFieldsIsInvalid() {
        val text = "a,b,c,d\n" + (2..11).joinToString("\n") { "w$it,t$it,s$it" }
        val result = WordSetImportValidator.validate(text) as ImportResult.Invalid
        assertEquals(LineError.TooManyFields(1, 4), result.errors.single())
    }

    @Test
    fun duplicateWordReportsBothLines() {
        val text = lines(10) + "\nWORD3;inne;Another sentence."
        val result = WordSetImportValidator.validate(text) as ImportResult.Invalid
        assertEquals(LineError.DuplicateWord(11, "WORD3", 3), result.errors.single())
    }

    @Test
    fun fewerThanTenEntriesIsRejected() {
        assertEquals(ImportResult.TooFew(8), WordSetImportValidator.validate(lines(8)))
    }

    @Test
    fun emptyFileIsRejected() {
        assertEquals(ImportResult.Empty, WordSetImportValidator.validate(""))
        assertEquals(ImportResult.Empty, WordSetImportValidator.validate("\n   \n"))
    }

    @Test
    fun allErrorsAreCollectedInLineOrder() {
        val text = listOf("a;b;", "c;;d", "e;f;g;h", "i;j;\"open").joinToString("\n")
        val result = WordSetImportValidator.validate(text) as ImportResult.Invalid
        assertEquals(listOf(1, 2, 3, 4), result.errors.map { it.line })
    }

    @Test
    fun slashIsKeptLiterallyAsSingleAnswer() {
        val text = "colour/color;kolor;What colour is it?\n" + lines(9)
        val result = WordSetImportValidator.validate(text) as ImportResult.Success
        assertEquals("colour/color", result.entries.first().word)
    }

    @Test
    fun headerLikeFirstLineIsImportedAsEntry() {
        val text = "word;translation;sentence\n" + lines(9, "w")
        val result = WordSetImportValidator.validate(text) as ImportResult.Success
        assertEquals("word", result.entries.first().word)
    }
}
