package pl.aingielski.domain.text

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerCheckerTest {
    @Test
    fun ignoresLetterCase() {
        assertTrue(AnswerChecker.isCorrect("apple", "Apple"))
        assertTrue(AnswerChecker.isCorrect("APPLE", "apple"))
        assertTrue(AnswerChecker.isCorrect("café", "Café"))
    }

    @Test
    fun ignoresSurroundingAndRepeatedSpaces() {
        assertTrue(AnswerChecker.isCorrect("  look   up ", "look up"))
    }

    @Test
    fun punctuationMustMatchExactly() {
        assertFalse(AnswerChecker.isCorrect("dont", "don't"))
        assertFalse(AnswerChecker.isCorrect("well known", "well-known"))
        assertTrue(AnswerChecker.isCorrect("Don't", "don't"))
    }

    @Test
    fun slashHasNoSpecialMeaning() {
        assertFalse(AnswerChecker.isCorrect("colour", "colour/color"))
        assertTrue(AnswerChecker.isCorrect("colour/color", "Colour/Color"))
    }

    @Test
    fun spellingFollowsTheSetRegardlessOfVariant() {
        assertFalse(AnswerChecker.isCorrect("colour", "color"))
    }
}
