package pl.aingielski.ui.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.aingielski.domain.stats.SessionStat
import pl.aingielski.testing.FakeSessionRepository
import pl.aingielski.testing.MainDispatcherRule
import java.time.ZoneOffset

class ClearDataTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val sessions = FakeSessionRepository().apply {
        // 57 recorded words in total.
        statsFlow.value = listOf(
            SessionStat(1, "A", 1_000, 30, correct = 20, incorrect = 6, skipped = 4),
            SessionStat(2, "B", 2_000, 27, correct = 10, incorrect = 10, skipped = 7),
        )
    }

    private fun create() = StatsViewModel(sessions, ZoneOffset.UTC)

    @Test
    fun wrongNumberShowsErrorAndDeletesNothing() {
        val vm = create()
        assertEquals(57, vm.state.value.global.totalWords)
        vm.openClearDialog()
        vm.onClearInputChange("50")
        vm.confirmClear()
        assertTrue(vm.state.value.clearError)
        assertTrue(vm.state.value.showClearDialog)
        assertEquals(0, sessions.clearCount)
    }

    @Test
    fun correctNumberDeletesAllStatistics() {
        val vm = create()
        vm.openClearDialog()
        vm.onClearInputChange("57")
        vm.confirmClear()
        assertEquals(1, sessions.clearCount)
        assertFalse(vm.state.value.showClearDialog)
        assertFalse(vm.state.value.hasData)
        assertTrue(vm.state.value.cleared)
    }

    @Test
    fun nonNumericInputIsRejected() {
        val vm = create()
        vm.openClearDialog()
        vm.onClearInputChange("abc")
        assertEquals("", vm.state.value.clearInput)
        vm.confirmClear()
        assertTrue(vm.state.value.clearError)
        assertEquals(0, sessions.clearCount)
    }
}
