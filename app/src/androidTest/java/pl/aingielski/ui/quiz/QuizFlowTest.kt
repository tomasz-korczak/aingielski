package pl.aingielski.ui.quiz

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.aingielski.R
import pl.aingielski.data.db.AppDatabase
import pl.aingielski.data.repository.RoomSessionRepository
import pl.aingielski.domain.model.AppSettings
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.SessionStatus
import pl.aingielski.domain.model.WordResult
import pl.aingielski.domain.repository.SettingsRepository
import pl.aingielski.testing.InstantSpeechEngine
import pl.aingielski.testing.inMemoryDatabase
import pl.aingielski.testing.words
import pl.aingielski.ui.navigation.Routes
import pl.aingielski.ui.theme.AIngielskiTheme

@RunWith(AndroidJUnit4::class)
class QuizFlowTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db: AppDatabase = inMemoryDatabase()
    private val sessions = RoomSessionRepository(db.sessionDao())
    private val settings = object : SettingsRepository {
        override val settings: Flow<AppSettings> = MutableStateFlow(AppSettings())
        override suspend fun save(settings: AppSettings) = Unit
    }

    @After
    fun tearDown() = db.close()

    private fun text(id: Int) = context.getString(id)

    private fun waitForAnswering(vm: QuizViewModel) {
        compose.waitUntil(5_000) { vm.state.value.phase == QuizPhase.Answering }
    }

    @Test
    fun answerCorrectlyIncorrectlySkipAndFinish() {
        val id = runBlocking { sessions.startSession("Set", SelectionCriterion.RANDOM, words(10), 1L) }
        val vm = QuizViewModel(SavedStateHandle(mapOf(Routes.ARG_SESSION_ID to id)), sessions, settings, InstantSpeechEngine())
        var finishedId: Long? = null
        compose.setContent {
            AIngielskiTheme { QuizScreen(onFinished = { finishedId = it }, onExitToHome = {}, viewModel = vm) }
        }

        // Word 1: hidden before the reveal, then correct.
        waitForAnswering(vm)
        compose.onNodeWithText(text(R.string.quiz_progress).format(1, 10)).assertIsDisplayed()
        compose.onNodeWithText("Sentence 1.").assertDoesNotExist()
        compose.onNodeWithText("słowo1", substring = true).assertDoesNotExist()
        compose.runOnIdle { vm.onAnswerChange("Word1") }
        compose.onNodeWithText(text(R.string.quiz_check)).performClick()
        compose.onNodeWithText(text(R.string.result_correct)).assertIsDisplayed()
        compose.onNodeWithText("Sentence 1.").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.quiz_next)).performClick()

        // Word 2: incorrect.
        waitForAnswering(vm)
        compose.runOnIdle { vm.onAnswerChange("wrong") }
        compose.onNodeWithText(text(R.string.quiz_check)).performClick()
        compose.onNodeWithText(text(R.string.result_incorrect)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.quiz_next)).performClick()

        // Word 3: skipped.
        waitForAnswering(vm)
        compose.onNodeWithText(text(R.string.quiz_skip)).performClick()
        compose.onNodeWithText(text(R.string.result_skipped)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.quiz_next)).performClick()

        // Words 4..10: correct.
        for (n in 4..10) {
            waitForAnswering(vm)
            compose.runOnIdle { vm.onAnswerChange("word$n") }
            compose.onNodeWithText(text(R.string.quiz_check)).performClick()
            compose.onNodeWithText(text(if (n == 10) R.string.quiz_finish else R.string.quiz_next)).performClick()
        }

        compose.waitUntil(5_000) { finishedId != null }
        assertEquals(id, finishedId)
        val rows = runBlocking { sessions.sessionWords(id) }
        assertEquals(8, rows.count { it.result == WordResult.CORRECT })
        assertEquals(1, rows.count { it.result == WordResult.INCORRECT })
        assertEquals(1, rows.count { it.result == WordResult.SKIPPED })
        assertEquals(SessionStatus.COMPLETED, runBlocking { sessions.session(id) }!!.status)
    }

    @Test
    fun summaryShowsCountsPercentageAndTypedAnswer() {
        val id = runBlocking {
            val sid = sessions.startSession("Set", SelectionCriterion.RANDOM, words(12), 1L)
            repeat(7) { sessions.recordResult(sid, it, WordResult.CORRECT, "word${it + 1}") }
            repeat(3) { sessions.recordResult(sid, 7 + it, WordResult.INCORRECT, "oops${it + 8}") }
            repeat(2) { sessions.recordResult(sid, 10 + it, WordResult.SKIPPED, null) }
            sessions.complete(sid)
            sid
        }
        val vm = SummaryViewModel(SavedStateHandle(mapOf(Routes.ARG_SESSION_ID to id)), sessions)
        compose.setContent { AIngielskiTheme { SummaryScreen(onHome = {}, onNewQuiz = { _, _, _ -> }, viewModel = vm) } }

        compose.onNodeWithText(text(R.string.summary_percent).format(58)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.summary_correct).format(7)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.summary_incorrect).format(3)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.summary_skipped).format(2)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.summary_your_answer).format("oops8"), substring = true).assertExists()
    }
}
