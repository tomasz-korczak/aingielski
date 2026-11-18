package pl.aingielski.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.aingielski.data.repository.RoomSessionRepository
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.SessionStatus
import pl.aingielski.domain.model.WordResult
import pl.aingielski.testing.inMemoryDatabase
import pl.aingielski.testing.words

@RunWith(AndroidJUnit4::class)
class SessionDaoQuizTest {
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
    fun createSessionWritesPendingUnplayedWords() = runTest {
        val id = repo.startSession("Set", SelectionCriterion.NEW_ONLY, words(12), 42L)
        val session = db.sessionDao().session(id)!!
        assertEquals(SessionStatus.IN_PROGRESS, session.status)
        assertEquals(SelectionCriterion.NEW_ONLY, session.criterion)
        assertEquals(12, session.wordCount)
        val rows = db.sessionDao().sessionWords(id)
        assertTrue(rows.all { it.result == WordResult.PENDING && !it.played && it.typedAnswer == null })
    }

    @Test
    fun recordResultUpdatesRowImmediately() = runTest {
        val id = repo.startSession("Set", SelectionCriterion.RANDOM, words(10), 1L)
        repo.recordResult(id, 0, WordResult.INCORRECT, "wrod1")
        val row = db.sessionDao().sessionWords(id).first()
        assertEquals(WordResult.INCORRECT, row.result)
        assertEquals("wrod1", row.typedAnswer)
        assertTrue(row.played)
    }

    @Test
    fun finalizeWithAnswersSkipsPendingAsNotPlayedAndAborts() = runTest {
        val id = repo.startSession("Set", SelectionCriterion.RANDOM, words(12), 1L)
        repeat(3) { repo.recordResult(id, it, WordResult.CORRECT, "word${it + 1}") }
        repo.finalizeInterrupted(id)
        assertEquals(SessionStatus.ABORTED, db.sessionDao().session(id)!!.status)
        val rows = db.sessionDao().sessionWords(id)
        assertEquals(9, rows.count { it.result == WordResult.SKIPPED && !it.played })
    }

    @Test
    fun finalizeWithoutAnswersDeletesSession() = runTest {
        val id = repo.startSession("Set", SelectionCriterion.RANDOM, words(10), 1L)
        repo.recordResult(id, 0, WordResult.SKIPPED, null)
        repo.finalizeInterrupted(id)
        assertNull(db.sessionDao().session(id))
        assertTrue(db.sessionDao().sessionWords(id).isEmpty())
    }

    @Test
    fun completeMarksCompleted() = runTest {
        val id = repo.startSession("Set", SelectionCriterion.RANDOM, words(10), 1L)
        repo.complete(id)
        assertEquals(SessionStatus.COMPLETED, db.sessionDao().session(id)!!.status)
    }

    @Test
    fun finalizeIsANoOpForFinishedOrMissingSessions() = runTest {
        val completed = repo.startSession("Set", SelectionCriterion.RANDOM, words(10), 1L)
        repo.recordResult(completed, 0, WordResult.CORRECT, "word1")
        repo.complete(completed)
        repo.finalizeInterrupted(completed)
        assertEquals(SessionStatus.COMPLETED, db.sessionDao().session(completed)!!.status)
        assertEquals(9, db.sessionDao().sessionWords(completed).count { it.result == WordResult.PENDING })

        val aborted = repo.startSession("Set", SelectionCriterion.RANDOM, words(10), 2L)
        repo.recordResult(aborted, 0, WordResult.CORRECT, "word1")
        repo.finalizeInterrupted(aborted)
        repo.finalizeInterrupted(aborted)
        assertEquals(SessionStatus.ABORTED, db.sessionDao().session(aborted)!!.status)

        repo.finalizeInterrupted(9_999L) // missing: nothing happens
    }

    @Test
    fun finalizeAllStaleHandlesEveryInProgressSession() = runTest {
        val a = repo.startSession("Set", SelectionCriterion.RANDOM, words(10), 1L)
        val b = repo.startSession("Set", SelectionCriterion.RANDOM, words(10), 2L)
        repo.recordResult(a, 0, WordResult.CORRECT, "word1")
        repo.finalizeAllStale()
        assertEquals(SessionStatus.ABORTED, db.sessionDao().session(a)!!.status)
        assertNull(db.sessionDao().session(b))
    }
}
