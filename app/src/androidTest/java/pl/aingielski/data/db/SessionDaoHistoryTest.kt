package pl.aingielski.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.aingielski.data.repository.RoomSessionRepository
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.WordResult
import pl.aingielski.testing.inMemoryDatabase
import pl.aingielski.testing.words
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class SessionDaoHistoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomSessionRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repo = RoomSessionRepository(db.sessionDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun playedKeysComeOnlyFromFinishedSessionsOfTheSameSetName() = runTest {
        val finished = repo.startSession("Zwierzęta", SelectionCriterion.RANDOM, words(10), 1L)
        repo.recordResult(finished, 0, WordResult.CORRECT, "word1")
        repo.recordResult(finished, 1, WordResult.SKIPPED, null)
        repo.complete(finished)

        val inProgress = repo.startSession("Zwierzęta", SelectionCriterion.RANDOM, words(10), 2L)
        repo.recordResult(inProgress, 2, WordResult.CORRECT, "word3")

        val otherSet = repo.startSession("Inny", SelectionCriterion.RANDOM, words(10), 3L)
        repo.recordResult(otherSet, 3, WordResult.CORRECT, "word4")
        repo.complete(otherSet)

        assertEquals(setOf("word1", "word2"), db.sessionDao().playedKeys("zwierzęta").toSet())
        // Name matching is case-insensitive through the normalized key.
        assertEquals(8, repo.newWordKeys("ZWIERZĘTA", words(10)).size)
    }

    @Test
    fun abortedUnansweredWordsStayNew() = runTest {
        val id = repo.startSession("Set", SelectionCriterion.RANDOM, words(10), 1L)
        repo.recordResult(id, 0, WordResult.CORRECT, "word1")
        repo.finalizeInterrupted(id)
        assertEquals(listOf("word1"), db.sessionDao().playedKeys("set"))
    }

    @Test
    fun answerCountsGroupByWordAndDriveDifficultOrder() = runTest {
        repeat(3) { round ->
            val id = repo.startSession("Set", SelectionCriterion.RANDOM, words(10), round.toLong())
            // word1: incorrect 3x; word2: incorrect 2x, correct 1x; word3: correct 1x, incorrect 1x (50%).
            repo.recordResult(id, 0, WordResult.INCORRECT, "x")
            repo.recordResult(id, 1, if (round == 0) WordResult.CORRECT else WordResult.INCORRECT, "x")
            if (round < 2) repo.recordResult(id, 2, if (round == 0) WordResult.CORRECT else WordResult.INCORRECT, "x")
            repo.recordResult(id, 3, WordResult.SKIPPED, null)
            repo.complete(id)
        }
        val counts = db.sessionDao().answerCounts("set").associateBy { it.wordKey }
        assertEquals(3, counts.getValue("word1").incorrect)
        assertEquals(1, counts.getValue("word2").correct)
        assertEquals(0, counts.getValue("word4").correct + counts.getValue("word4").incorrect)
        assertEquals(listOf("word1", "word2"), repo.difficultWordKeys("Set", Random(1)))
    }
}
