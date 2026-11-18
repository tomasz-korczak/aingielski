package pl.aingielski.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.aingielski.domain.model.AppSettings
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.SessionInfo
import pl.aingielski.domain.model.SessionWordInfo
import pl.aingielski.domain.model.WordItem
import pl.aingielski.domain.model.WordResult
import pl.aingielski.domain.model.WordSetSummary
import pl.aingielski.domain.stats.SessionStat
import kotlin.random.Random

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun save(settings: AppSettings)
}

interface WordSetRepository {
    fun observeSummaries(): Flow<List<WordSetSummary>>
    suspend fun summary(setId: Long): WordSetSummary?
    suspend fun findByName(name: String): WordSetSummary?
    suspend fun entries(setId: Long): List<WordItem>

    /** Saves a new set, or replaces the set with the same name when [overwrite] is true. */
    suspend fun importSet(name: String, entries: List<WordItem>, overwrite: Boolean)
    suspend fun delete(setId: Long)
}

interface SessionRepository {
    suspend fun startSession(
        setName: String,
        criterion: SelectionCriterion,
        words: List<WordItem>,
        startedAt: Long,
    ): Long

    suspend fun session(sessionId: Long): SessionInfo?
    suspend fun sessionWords(sessionId: Long): List<SessionWordInfo>
    suspend fun recordResult(sessionId: Long, position: Int, result: WordResult, typedAnswer: String?)
    suspend fun complete(sessionId: Long)
    suspend fun finalizeInterrupted(sessionId: Long)
    suspend fun finalizeAllStale()

    /** Keys of [entries] never played in a finished session of this set name (FR-014). */
    suspend fun newWordKeys(setName: String, entries: List<WordItem>): Set<String>

    /** Difficult keys of this set name, hardest first (FR-015, FR-016). */
    suspend fun difficultWordKeys(setName: String, random: Random): List<String>

    fun observeSessionStats(): Flow<List<SessionStat>>
    suspend fun clearAll()
}
