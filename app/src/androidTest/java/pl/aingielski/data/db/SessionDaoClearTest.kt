package pl.aingielski.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class SessionDaoClearTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = inMemoryDatabase()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun deleteAllSessionsCascadesWordsAndKeepsWordSets() = runTest {
        val sets = RoomWordSetRepository(db.wordSetDao())
        val sessions = RoomSessionRepository(db.sessionDao())
        sets.importSet("A", words(10), overwrite = false)
        val id = sessions.startSession("A", SelectionCriterion.RANDOM, words(10), 1L)
        sessions.recordResult(id, 0, WordResult.CORRECT, "x")
        sessions.complete(id)

        sessions.clearAll()

        assertTrue(sessions.observeSessionStats().first().isEmpty())
        assertTrue(db.sessionDao().sessionWords(id).isEmpty())
        assertEquals(10, sets.observeSummaries().first().single().wordCount)
    }
}
