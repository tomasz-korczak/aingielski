package pl.aingielski.domain.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetNameValidatorTest {
    @Test
    fun blankNameIsRejected() {
        assertEquals(SetNameError.Blank, SetNameValidator.validate(""))
        assertEquals(SetNameError.Blank, SetNameValidator.validate("   "))
    }

    @Test
    fun nameLongerThanFiftyIsRejectedAfterTrimming() {
        assertNull(SetNameValidator.validate("  " + "a".repeat(50) + "  "))
        assertEquals(SetNameError.TooLong(50), SetNameValidator.validate("a".repeat(51)))
    }

    @Test
    fun duplicatesAreDetectedCaseInsensitivelyIncludingPolishLetters() {
        assertTrue(SetNameValidator.isSameName("Zwierzęta", " zwierzęta "))
        assertTrue(SetNameValidator.isSameName("ŚLIMAKI", "ślimaki"))
        assertFalse(SetNameValidator.isSameName("Zwierzęta", "Zwierzeta"))
    }

    @Test
    fun defaultNameIsFileNameWithoutCsv() {
        assertEquals("zwierzeta_sredniki", SetNameValidator.defaultName("zwierzeta_sredniki.csv"))
        assertEquals("Lista", SetNameValidator.defaultName("Lista.CSV"))
        assertEquals("notes.txt", SetNameValidator.defaultName("notes.txt"))
        assertEquals("", SetNameValidator.defaultName(null))
    }
}
