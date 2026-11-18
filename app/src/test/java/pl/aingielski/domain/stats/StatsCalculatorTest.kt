package pl.aingielski.domain.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class StatsCalculatorTest {
    private val zone = ZoneOffset.UTC

    private fun stat(id: Long, startedAt: Long, wordCount: Int, correct: Int, incorrect: Int, skipped: Int) =
        SessionStat(id, "Set", startedAt, wordCount, correct, incorrect, skipped)

    @Test
    fun totalsAreSummedAcrossSessions() {
        val global = StatsCalculator.global(
            listOf(stat(1, 0, 10, 7, 2, 1), stat(2, 1, 12, 3, 4, 5)),
        )
        assertEquals(2, global.sessionCount)
        assertEquals(22, global.totalWords)
        assertEquals(10, global.correct)
        assertEquals(6, global.incorrect)
        assertEquals(6, global.skipped)
    }

    @Test
    fun averageIsMeanOfPerSessionPercentages() {
        val global = StatsCalculator.global(
            listOf(stat(1, 0, 10, 10, 0, 0), stat(2, 1, 50, 25, 25, 0)),
        )
        assertEquals(75, global.averagePercent) // not 35/60 = 58
    }

    @Test
    fun averageUsesUnroundedValuesAndRoundsHalfUp() {
        // 7/12 = 58.33…, 1/8 = 12.5 → mean 35.416… → 35 (rounding first would give (58+13)/2 = 35.5 → 36)
        val global = StatsCalculator.global(listOf(stat(1, 0, 12, 7, 5, 0), stat(2, 1, 8, 1, 7, 0)))
        assertEquals(35, global.averagePercent)
    }

    @Test
    fun noSessionsHasNoAverage() {
        val global = StatsCalculator.global(emptyList())
        assertEquals(0, global.sessionCount)
        assertNull(global.averagePercent)
    }

    @Test
    fun abortedSessionsAreIncluded() {
        // An aborted 12-word session with 3 correct and 9 skipped counts as 25%.
        val global = StatsCalculator.global(listOf(stat(1, 0, 12, 3, 0, 9)))
        assertEquals(25, global.averagePercent)
        assertEquals(12, global.totalWords)
    }

    @Test
    fun chartShowsTenNewestOldestFirstWithLabels() {
        val base = LocalDateTime.of(2026, 11, 18, 9, 5).toInstant(zone).toEpochMilli()
        val sessions = (1..14).map { stat(it.toLong(), base + it * 3_600_000L, 10, it, 10 - it % 10, 0) }
        val chart = StatsCalculator.chart(sessions.shuffled(), zone)
        assertEquals(10, chart.size)
        assertEquals((5L..14L).toList(), chart.map { it.sessionId })
        assertEquals("18.11 14:05", chart.first().label)
    }

    @Test
    fun twentySessionFixtureMatchesHandComputedValues() {
        // SC-006: 20 sessions, mixing completed and aborted ones.
        val sessions = (1..20).map { i ->
            // Every 4th session was aborted: all its unanswered words count as skipped.
            val skipped = if (i % 4 == 0) 10 - i % 10 else 0
            stat(i.toLong(), i * 1000L, 10, correct = i % 10, incorrect = 10 - i % 10 - skipped, skipped = skipped)
        }
        val global = StatsCalculator.global(sessions)
        assertEquals(20, global.sessionCount)
        assertEquals(200, global.totalWords)
        assertEquals(90, global.correct)
        assertEquals(30, global.skipped) // 6 + 2 + 8 + 4 + 10
        assertEquals(80, global.incorrect)
        // Mean of i%10 * 10 over i=1..20 = 45.
        assertEquals(45, global.averagePercent)
        assertEquals((11L..20L).toList(), StatsCalculator.chart(sessions, zone).map { it.sessionId })
    }
}
