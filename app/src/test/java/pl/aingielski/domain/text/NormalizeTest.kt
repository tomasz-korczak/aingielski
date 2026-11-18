package pl.aingielski.domain.text

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizeTest {
    @Test
    fun trimsLeadingAndTrailingWhitespace() {
        assertEquals("apple", normalizeKey("  apple \t"))
    }

    @Test
    fun collapsesInnerWhitespace() {
        assertEquals("look up", normalizeKey("look    up"))
        assertEquals("look up", normalizeKey("look\t \nup"))
    }

    @Test
    fun lowercasesIncludingPolishLetters() {
        assertEquals("ślimak", normalizeKey("ŚLIMAK"))
        assertEquals("zwierzęta", normalizeKey("ZWIERZĘTA"))
    }

    @Test
    fun keepsPunctuation() {
        assertEquals("don't", normalizeKey("Don't"))
        assertEquals("well-known", normalizeKey("Well-Known"))
        assertEquals("colour/color", normalizeKey("Colour/Color"))
    }
}
