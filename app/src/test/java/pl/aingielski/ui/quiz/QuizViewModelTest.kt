package pl.aingielski.ui.quiz

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.aingielski.domain.model.AppSettings
import pl.aingielski.domain.model.SessionStatus
import pl.aingielski.domain.model.WordResult
import pl.aingielski.speech.SpeakResult
import pl.aingielski.testing.FakeSessionRepository
import pl.aingielski.testing.FakeSettingsRepository
import pl.aingielski.testing.FakeSpeechEngine
import pl.aingielski.testing.MainDispatcherRule
import pl.aingielski.testing.sampleWords
import pl.aingielski.ui.navigation.Routes

class QuizViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val sessions = FakeSessionRepository()
    private val speech = FakeSpeechEngine()

    private fun create(
        words: Int = 10,
        skipEnabled: Boolean = true,
        restored: Boolean = false,
    ): Pair<QuizViewModel, Long> {
        val id = sessions.seed(sampleWords(words))
        val handle = SavedStateHandle(
            buildMap {
                put(Routes.ARG_SESSION_ID, id)
                if (restored) put(QuizViewModel.KEY_STARTED, true)
            },
        )
        val vm = QuizViewModel(handle, sessions, FakeSettingsRepository(AppSettings(skipEnabled = skipEnabled)), speech)
        return vm to id
    }

    @Test
    fun answerFieldAppearsOnlyAfterFirstPlaybackCompletes() {
        speech.autoComplete = false
        val (vm, _) = create()
        assertEquals(QuizPhase.Playing, vm.state.value.phase)
        assertEquals("word1", speech.spoken.single())
        speech.completeCurrent()
        assertEquals(QuizPhase.Answering, vm.state.value.phase)
        assertFalse(vm.state.value.playbackFailed)
    }

    @Test
    fun wordIsNotInStateBeforeReveal() {
        val (vm, _) = create()
        assertEquals(QuizPhase.Answering, vm.state.value.phase)
        assertNull(vm.state.value.revealed)
        assertEquals(1, vm.state.value.index + 1)
        assertEquals(10, vm.state.value.total)
    }

    @Test
    fun failedOrInterruptedFirstPlaybackStillShowsAnswerFieldWithMessage() {
        speech.nextResult = SpeakResult.Failed
        val (vm, _) = create()
        assertEquals(QuizPhase.Answering, vm.state.value.phase)
        assertTrue(vm.state.value.playbackFailed)

        speech.nextResult = SpeakResult.Completed
        speech.autoComplete = false
        val (vm2, _) = create()
        speech.simulateInterruption()
        assertEquals(QuizPhase.Answering, vm2.state.value.phase)
        assertTrue(vm2.state.value.playbackFailed)
    }

    @Test
    fun skipIsOfferedOnlyWhileAnsweringAndWhenEnabled() {
        speech.autoComplete = false
        val (vm, id) = create()
        vm.skip() // still Playing: ignored
        assertEquals(QuizPhase.Playing, vm.state.value.phase)
        speech.completeCurrent()
        vm.skip()
        assertEquals(QuizPhase.Revealed, vm.state.value.phase)
        assertEquals(WordResult.SKIPPED, sessions.sessions[id]!!.words[0].result)

        speech.autoComplete = true
        val (noSkip, noSkipId) = create(skipEnabled = false)
        noSkip.skip()
        assertEquals(QuizPhase.Answering, noSkip.state.value.phase)
        assertEquals(WordResult.PENDING, sessions.sessions[noSkipId]!!.words[0].result)
    }

    @Test
    fun skipRevealsAndWaitsForNext() {
        val (vm, _) = create()
        vm.skip()
        val revealed = vm.state.value.revealed
        assertNotNull(revealed)
        assertEquals("word1", revealed!!.word)
        assertEquals("Sentence 1.", revealed.sentence)
        assertEquals(0, vm.state.value.index)
        vm.next()
        assertEquals(1, vm.state.value.index)
    }

    @Test
    fun blankAnswerCannotBeSubmitted() {
        val (vm, _) = create()
        vm.onAnswerChange("   ")
        vm.submit()
        assertEquals(QuizPhase.Answering, vm.state.value.phase)
    }

    @Test
    fun revealShowsCorrectnessWordTranslationAndSentence() {
        val (vm, id) = create()
        vm.onAnswerChange(" WORD1 ")
        vm.submit()
        val revealed = vm.state.value.revealed!!
        assertEquals(WordResult.CORRECT, revealed.result)
        assertEquals("słowo1", revealed.translation)
        assertEquals("Sentence 1.", revealed.sentence)
        // Saved immediately (FR-028a).
        assertEquals(WordResult.CORRECT, sessions.sessions[id]!!.words[0].result)
        assertEquals(" WORD1 ", sessions.sessions[id]!!.words[0].typedAnswer)

        vm.next()
        vm.onAnswerChange("wrong")
        vm.submit()
        assertEquals(WordResult.INCORRECT, vm.state.value.revealed!!.result)
    }

    @Test
    fun playButtonsWorkWhileAnsweringAndAfterReveal() {
        val (vm, _) = create()
        vm.replayWord()
        vm.playSentence()
        vm.onAnswerChange("word1")
        vm.submit()
        vm.replayWord()
        vm.playSentence()
        assertEquals(listOf("word1", "word1", "Sentence 1.", "word1", "Sentence 1."), speech.spoken)
    }

    @Test
    fun lastWordLeadsToSummaryAndCompletesSession() {
        val (vm, id) = create(words = 10)
        repeat(10) {
            vm.onAnswerChange("word${it + 1}")
            vm.submit()
            vm.next()
        }
        assertEquals(QuizNavigation.Summary(id), vm.state.value.navigation)
        assertEquals(SessionStatus.COMPLETED, sessions.sessions[id]!!.info.status)
    }

    @Test
    fun abortWithAnsweredWordsSavesSessionAndSkipsTheRest() {
        val (vm, id) = create(words = 12)
        repeat(3) {
            vm.onAnswerChange("word${it + 1}")
            vm.submit()
            vm.next()
        }
        vm.requestAbort()
        assertTrue(vm.state.value.showAbortDialog)
        assertTrue(vm.state.value.hasAnswered)
        vm.confirmAbort()
        val stored = sessions.sessions[id]!!
        assertEquals(SessionStatus.ABORTED, stored.info.status)
        assertEquals(9, stored.words.count { it.result == WordResult.SKIPPED })
        // The word on screen (heard, unanswered) stays "new" (FR-028).
        assertFalse(stored.played[3])
        assertEquals(QuizNavigation.Home, vm.state.value.navigation)
    }

    @Test
    fun abortWithoutAnswersSavesNothing() {
        val (vm, id) = create()
        vm.skip()
        vm.next()
        vm.requestAbort()
        assertFalse(vm.state.value.hasAnswered)
        vm.confirmAbort()
        assertNull(sessions.sessions[id])
    }

    @Test
    fun backIsIgnoredWhenSkippingIsDisabled() {
        val (vm, _) = create(skipEnabled = false)
        vm.onBack()
        assertFalse(vm.state.value.showAbortDialog)
        assertFalse(vm.state.value.canAbort)
    }

    @Test
    fun backOpensAbortDialogWhenSkippingIsEnabled() {
        val (vm, _) = create()
        vm.onBack()
        assertTrue(vm.state.value.showAbortDialog)
    }

    @Test
    fun mutedOutputShowsHint() {
        speech.muted = true
        val (vm, _) = create()
        assertTrue(vm.state.value.showMutedHint)
        vm.onMutedHintShown()
        assertFalse(vm.state.value.showMutedHint)
    }

    @Test
    fun stoppingTheScreenStopsPlaybackAndNothingReplays() {
        val (vm, _) = create()
        val spokenBefore = speech.spoken.size
        vm.onStop()
        assertEquals(1, speech.stopCount)
        assertEquals(spokenBefore, speech.spoken.size)
    }

    @Test
    fun recreatedAfterProcessDeathFinalizesAndGoesHome() {
        val (vm, id) = create(restored = true)
        assertEquals(QuizNavigation.Home, vm.state.value.navigation)
        assertEquals(1, sessions.finalizeCalls)
        assertNull(sessions.sessions[id]) // no answers: deleted
        assertTrue(speech.spoken.isEmpty())
    }
}
