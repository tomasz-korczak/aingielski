package pl.aingielski.ui.stats

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pl.aingielski.R
import pl.aingielski.data.repository.RoomSessionRepository
import pl.aingielski.data.repository.RoomWordSetRepository
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.WordResult
import pl.aingielski.testing.inMemoryDatabase
import pl.aingielski.testing.words
import pl.aingielski.ui.theme.AIngielskiTheme

@RunWith(AndroidJUnit4::class)
class ClearDataDialogTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = inMemoryDatabase()
    private val sessions = RoomSessionRepository(db.sessionDao())
    private val sets = RoomWordSetRepository(db.wordSetDao())

    @After
    fun tearDown() = db.close()

    @Test
    fun wrongCountKeepsDataAndCorrectCountClearsIt() {
        runBlocking {
            sets.importSet("A", words(10), overwrite = false)
            val id = sessions.startSession("A", SelectionCriterion.RANDOM, words(10), 1L)
            repeat(10) { sessions.recordResult(id, it, WordResult.CORRECT, "x") }
            sessions.complete(id)
        }
        val vm = StatsViewModel(sessions)
        compose.setContent { AIngielskiTheme { StatsScreen(onBack = {}, viewModel = vm) } }

        compose.onNodeWithText(context.getString(R.string.stats_clear)).performClick()
        compose.onNodeWithText(context.getString(R.string.clear_title)).assertIsDisplayed()

        compose.onNodeWithTag(TAG_CLEAR_INPUT).performTextInput("5")
        compose.onNodeWithText(context.getString(R.string.clear_confirm)).performClick()
        compose.onNodeWithText(context.getString(R.string.clear_wrong)).assertIsDisplayed()
        assertEquals(1, runBlocking { sessions.observeSessionStats().first() }.size)

        compose.onNodeWithTag(TAG_CLEAR_INPUT).performTextClearance()
        compose.onNodeWithTag(TAG_CLEAR_INPUT).performTextInput("10")
        compose.onNodeWithText(context.getString(R.string.clear_confirm)).performClick()
        compose.waitUntil(5_000) { !vm.state.value.hasData }

        assertTrue(runBlocking { sessions.observeSessionStats().first() }.isEmpty())
        assertEquals(1, runBlocking { sets.observeSummaries().first() }.size)
        compose.onNodeWithText(context.getString(R.string.stats_no_data)).assertIsDisplayed()
    }
}
