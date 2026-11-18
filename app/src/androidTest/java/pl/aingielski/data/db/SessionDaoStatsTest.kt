package pl.aingielski.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.aingielski.data.repository.RoomSessionRepository
import pl.aingielski.data.repository.RoomWordSetRepository
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.WordResult
import pl.aingielski.testing.inMemoryDatabase
import pl.aingielski.testing.words

@RunWith(AndroidJUnit4::class)
class SessionDaoStatsTest {
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
    fun summariesCountResultsPerFinishedSessionNewestFirst() = runTest {
        val older = repo.startSession("A", SelectionCriterion.RANDOM, words(10), 1_000L)
        repeat(7) { repo.recordResult(older, it, WordResult.CORRECT, "x") }
        repeat(2) { repo.recordResult(older, 7 + it, WordResult.INCORRECT, "x") }
        repo.recordResult(older, 9, WordResult.SKIPPED, null)
        repo.complete(older)

        val newer = repo.startSession("B", SelectionCriterion.RANDOM, words(12), 2_000L)
        repo.recordResult(newer, 0, WordResult.CORRECT, "x")
        repo.finalizeInterrupted(newer) // aborted: 11 skipped

        repo.startSession("C", SelectionCriterion.RANDOM, words(10), 3_000L) // in progress: excluded

        val stats = repo.observeSessionStats().first()
        assertEquals(listOf("B", "A"), stats.map { it.setName })
        assertEquals(Triple(1, 0, 11), stats[0].let { Triple(it.correct, it.incorrect, it.skipped) })
        assertEquals(Triple(7, 2, 1), stats[1].let { Triple(it.correct, it.incorrect, it.skipped) })
        assertEquals(12, stats[0].wordCount)
    }

    @Test
    fun statisticsAreUnchangedAfterDeletingTheWordSet() = runTest {
        val sets = RoomWordSetRepository(db.wordSetDao())
        sets.importSet("A", words(10), overwrite = false)
        val id = repo.startSession("A", SelectionCriterion.RANDOM, words(10), 1L)
        repeat(10) { repo.recordResult(id, it, WordResult.CORRECT, "x") }
        repo.complete(id)
        val before = repo.observeSessionStats().first()

        sets.delete(sets.findByName("A")!!.id)

        assertEquals(before, repo.observeSessionStats().first())
    }
}
