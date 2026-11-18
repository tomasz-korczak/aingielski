package pl.aingielski.domain.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.aingielski.domain.model.WordItem
import pl.aingielski.domain.model.WordResult
import kotlin.random.Random

class QuizSetupValidatorTest {
    @Test
    fun acceptsRangeFromTenToSetSize() {
        assertNull(QuizSetupValidator.check("10", 25).error)
        assertNull(QuizSetupValidator.check("25", 25).error)
        assertEquals(17, QuizSetupValidator.check("17", 25).count)
    }

    @Test
    fun rejectsOutOfRange() {
        assertEquals(CountError.TooLow(10), QuizSetupValidator.check("8", 25).error)
        assertEquals(CountError.TooHigh(25), QuizSetupValidator.check("30", 25).error)
    }

    @Test
    fun rejectsEmptyOrNonNumeric() {
        assertEquals(CountError.NotANumber, QuizSetupValidator.check("", 25).error)
        assertEquals(CountError.NotANumber, QuizSetupValidator.check("1a", 25).error)
        assertEquals(CountError.NotANumber, QuizSetupValidator.check("-5", 25).error)
    }

    @Test
    fun setOfExactlyTenAllowsOnlyTen() {
        assertNull(QuizSetupValidator.check("10", 10).error)
        assertEquals(CountError.TooHigh(10), QuizSetupValidator.check("11", 10).error)
    }

    @Test
    fun defaultCountIsValidForEverySavedSet() {
        for (size in 10..40) assertNull(QuizSetupValidator.check(QuizSetupValidator.DEFAULT_COUNT.toString(), size).error)
    }
}

class WordSelectorTest {
    private val entries = (1..20).map { WordItem("w$it", "t$it", "s$it") }

    @Test
    fun returnsExactlyCountDistinctWords() {
        val selected = WordSelector.select(entries, PreferredWords.None, 12, Random(1))
        assertEquals(12, selected.size)
        assertEquals(12, selected.map { it.word }.toSet().size)
    }

    @Test
    fun takesPreferredFirstAndFillsFromTheRest() {
        val preferred = setOf("w1", "w2", "w3", "w4")
        val selected = WordSelector.select(entries, PreferredWords.Unordered(preferred), 10, Random(2))
        assertTrue(selected.map { it.word }.containsAll(preferred))
        assertEquals(10, selected.size)
    }

    @Test
    fun orderedPreferenceTakesTheFirstOnesWhenMoreThanNeeded() {
        val hardestFirst = (1..15).map { "w$it" }
        val selected = WordSelector.select(entries, PreferredWords.Ordered(hardestFirst), 10, Random(3))
        assertEquals((1..10).map { "w$it" }.toSet(), selected.map { it.word }.toSet())
    }

    @Test
    fun unorderedPreferencePicksRandomlyWhenMoreThanNeeded() {
        val preferred = (1..15).map { "w$it" }.toSet()
        val picks = (1..20).map { seed ->
            WordSelector.select(entries, PreferredWords.Unordered(preferred), 10, Random(seed)).map { it.word }.toSet()
        }
        assertTrue(picks.all { it.all { word -> word in preferred } })
        assertTrue(picks.toSet().size > 1)
    }

    @Test
    fun shufflesTheFinalOrder() {
        val ordered = (1..10).map { "w$it" }
        val orders = (1..10).map { seed ->
            WordSelector.select(entries, PreferredWords.Ordered(ordered), 10, Random(seed)).map { it.word }
        }
        assertTrue(orders.any { it != ordered })
    }

    @Test
    fun emptyPreferenceIsPurelyRandom() {
        val a = WordSelector.select(entries, PreferredWords.Unordered(emptySet()), 10, Random(5))
        val b = WordSelector.select(entries, PreferredWords.None, 10, Random(5))
        assertEquals(10, a.size)
        assertEquals(10, b.size)
    }

    @Test
    fun preferredKeysAreMatchedCaseInsensitively() {
        val mixed = listOf(WordItem("Apple", "jabłko", "s")) + entries
        val selected = WordSelector.select(mixed, PreferredWords.Ordered(listOf("apple")), 10, Random(1))
        assertTrue(selected.any { it.word == "Apple" })
    }
}

class SummaryCalculatorTest {
    private fun item(result: WordResult) = SummaryItem("w", "t", result, null)

    @Test
    fun roundsHalfUp() {
        assertEquals(58, SummaryCalculator.percent(7, 12))
        assertEquals(50, SummaryCalculator.percent(5, 10))
        assertEquals(17, SummaryCalculator.percent(1, 6)) // 16.67
        assertEquals(13, SummaryCalculator.percent(1, 8)) // 12.5 rounds up
        assertEquals(0, SummaryCalculator.percent(0, 0))
    }

    @Test
    fun countsResults() {
        val items = List(7) { item(WordResult.CORRECT) } + List(3) { item(WordResult.INCORRECT) } +
            List(2) { item(WordResult.SKIPPED) }
        val summary = SummaryCalculator.summarize(items)
        assertEquals(7, summary.correct)
        assertEquals(3, summary.incorrect)
        assertEquals(2, summary.skipped)
        assertEquals(58, summary.percent)
    }

    @Test
    fun abortedSessionCountsAutoSkippedWordsInTheDenominator() {
        val items = List(3) { item(WordResult.CORRECT) } + List(9) { item(WordResult.SKIPPED) }
        assertEquals(25, SummaryCalculator.summarize(items).percent)
    }
}

class DifficultyRuleTest {
    @Test
    fun thresholdIsAtLeastFiftyOnePercent() {
        assertTrue(DifficultyRule.isDifficult(correct = 1, incorrect = 2)) // 67%
        assertFalse(DifficultyRule.isDifficult(correct = 1, incorrect = 1)) // 50%
        assertTrue(DifficultyRule.isDifficult(correct = 49, incorrect = 51)) // exactly 51%
        assertFalse(DifficultyRule.isDifficult(correct = 101, incorrect = 99)) // 49.5%
        assertFalse(DifficultyRule.isDifficult(correct = 0, incorrect = 0))
    }

    @Test
    fun ordersHardestFirst() {
        val counts = listOf(
            AnswerCounts("a", correct = 1, incorrect = 2), // 67%
            AnswerCounts("b", correct = 0, incorrect = 3), // 100%
            AnswerCounts("c", correct = 1, incorrect = 3), // 75%
            AnswerCounts("d", correct = 2, incorrect = 0), // not difficult
        )
        assertEquals(listOf("b", "c", "a"), DifficultyRule.difficultKeys(counts, Random(1)))
    }

    @Test
    fun tiesAreShuffled() {
        val counts = (1..6).map { AnswerCounts("w$it", correct = 0, incorrect = 1) }
        val orders = (1..10).map { DifficultyRule.difficultKeys(counts, Random(it)) }.toSet()
        assertTrue(orders.size > 1)
    }
}
