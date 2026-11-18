package pl.aingielski.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.aingielski.data.db.entity.WordSetEntity
import pl.aingielski.data.repository.RoomSessionRepository
import pl.aingielski.data.repository.RoomWordSetRepository
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.testing.inMemoryDatabase
import pl.aingielski.testing.words

@RunWith(AndroidJUnit4::class)
class WordSetDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: RoomWordSetRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repo = RoomWordSetRepository(db.wordSetDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertedSetIsListedWithWordCountAndEntriesInFileOrder() = runTest {
        repo.importSet("Zwierzęta", words(25), overwrite = false)
        val summary = repo.observeSummaries().first().single()
        assertEquals("Zwierzęta", summary.name)
        assertEquals(25, summary.wordCount)
        assertEquals(words(25).map { it.word }, repo.entries(summary.id).map { it.word })
    }

    @Test
    fun nameKeyIsUnique() = runTest {
        repo.importSet("Zwierzęta", words(10), overwrite = false)
        try {
            db.wordSetDao().insertSet(WordSetEntity(name = "zwierzęta", nameKey = "zwierzęta", importedAt = 0), emptyList())
            fail("Expected a unique-constraint violation")
        } catch (_: SQLiteConstraintException) {
            // expected
        }
    }

    @Test
    fun replaceSetReplacesAllEntries() = runTest {
        repo.importSet("Zwierzęta", words(25), overwrite = false)
        repo.importSet("zwierzęta", words(12, "new"), overwrite = true)
        val summary = repo.observeSummaries().first().single()
        assertEquals("zwierzęta", summary.name)
        assertEquals(12, summary.wordCount)
        assertEquals("new1", repo.entries(summary.id).first().word)
    }

    @Test
    fun deletingSetCascadesEntriesButKeepsSessions() = runTest {
        repo.importSet("Zwierzęta", words(10), overwrite = false)
        val id = repo.findByName("Zwierzęta")!!.id
        val sessions = RoomSessionRepository(db.sessionDao())
        val sessionId = sessions.startSession("Zwierzęta", SelectionCriterion.RANDOM, words(10), 1L)

        repo.delete(id)

        assertNull(repo.findByName("Zwierzęta"))
        assertEquals(0, repo.entries(id).size)
        assertNotNull(sessions.session(sessionId))
        assertEquals(10, sessions.sessionWords(sessionId).size)
    }
}
