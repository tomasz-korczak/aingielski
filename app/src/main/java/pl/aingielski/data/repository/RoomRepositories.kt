package pl.aingielski.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.aingielski.data.db.dao.SessionDao
import pl.aingielski.data.db.dao.WordSetDao
import pl.aingielski.data.db.entity.QuizSessionEntity
import pl.aingielski.data.db.entity.SessionWordEntity
import pl.aingielski.data.db.entity.WordEntryEntity
import pl.aingielski.data.db.entity.WordSetEntity
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.SessionInfo
import pl.aingielski.domain.model.SessionStatus
import pl.aingielski.domain.model.SessionWordInfo
import pl.aingielski.domain.model.WordItem
import pl.aingielski.domain.model.WordResult
import pl.aingielski.domain.model.WordSetSummary
import pl.aingielski.domain.quiz.AnswerCounts
import pl.aingielski.domain.quiz.DifficultyRule
import pl.aingielski.domain.repository.SessionRepository
import pl.aingielski.domain.repository.WordSetRepository
import pl.aingielski.domain.stats.SessionStat
import pl.aingielski.domain.text.normalizeKey
import kotlin.random.Random

class RoomWordSetRepository(
    private val dao: WordSetDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : WordSetRepository {

    override fun observeSummaries(): Flow<List<WordSetSummary>> =
        dao.observeSummaries().map { rows -> rows.map { WordSetSummary(it.id, it.name, it.wordCount) } }

    override suspend fun summary(setId: Long): WordSetSummary? {
        val set = dao.findById(setId) ?: return null
        return WordSetSummary(set.id, set.name, dao.entries(setId).size)
    }

    override suspend fun findByName(name: String): WordSetSummary? {
        val set = dao.findByNameKey(normalizeKey(name)) ?: return null
        return WordSetSummary(set.id, set.name, dao.entries(set.id).size)
    }

    override suspend fun entries(setId: Long): List<WordItem> =
        dao.entries(setId).map { WordItem(it.word, it.translation, it.sentence) }

    override suspend fun importSet(name: String, entries: List<WordItem>, overwrite: Boolean) {
        val trimmed = name.trim()
        val set = WordSetEntity(name = trimmed, nameKey = normalizeKey(trimmed), importedAt = clock())
        val rows = entries.mapIndexed { index, item ->
            WordEntryEntity(
                setId = 0,
                position = index,
                word = item.word,
                wordKey = normalizeKey(item.word),
                translation = item.translation,
                sentence = item.sentence,
            )
        }
        if (overwrite) dao.replaceSet(set, rows) else dao.insertSet(set, rows)
    }

    override suspend fun delete(setId: Long) = dao.deleteSet(setId)
}

class RoomSessionRepository(private val dao: SessionDao) : SessionRepository {

    override suspend fun startSession(
        setName: String,
        criterion: SelectionCriterion,
        words: List<WordItem>,
        startedAt: Long,
    ): Long {
        val session = QuizSessionEntity(
            setName = setName,
            setNameKey = normalizeKey(setName),
            startedAt = startedAt,
            criterion = criterion,
            wordCount = words.size,
            status = SessionStatus.IN_PROGRESS,
        )
        val rows = words.mapIndexed { index, item ->
            SessionWordEntity(
                sessionId = 0,
                position = index,
                word = item.word,
                wordKey = normalizeKey(item.word),
                translation = item.translation,
                sentence = item.sentence,
                result = WordResult.PENDING,
                typedAnswer = null,
                played = false,
            )
        }
        return dao.createSession(session, rows)
    }

    override suspend fun session(sessionId: Long): SessionInfo? =
        dao.session(sessionId)?.let { SessionInfo(it.id, it.setName, it.criterion, it.wordCount, it.status) }

    override suspend fun sessionWords(sessionId: Long): List<SessionWordInfo> =
        dao.sessionWords(sessionId).map {
            SessionWordInfo(it.position, it.word, it.translation, it.sentence, it.result, it.typedAnswer)
        }

    override suspend fun recordResult(sessionId: Long, position: Int, result: WordResult, typedAnswer: String?) =
        dao.recordResult(sessionId, position, result, typedAnswer)

    override suspend fun complete(sessionId: Long) = dao.markCompleted(sessionId)

    override suspend fun finalizeInterrupted(sessionId: Long) = dao.finalizeInterrupted(sessionId)

    override suspend fun finalizeAllStale() {
        dao.staleInProgressIds().forEach { dao.finalizeInterrupted(it) }
    }

    override suspend fun newWordKeys(setName: String, entries: List<WordItem>): Set<String> {
        val played = dao.playedKeys(normalizeKey(setName)).toHashSet()
        return entries.map { normalizeKey(it.word) }.filterTo(LinkedHashSet()) { it !in played }
    }

    override suspend fun difficultWordKeys(setName: String, random: Random): List<String> =
        DifficultyRule.difficultKeys(
            dao.answerCounts(normalizeKey(setName)).map { AnswerCounts(it.wordKey, it.correct, it.incorrect) },
            random,
        )

    override fun observeSessionStats(): Flow<List<SessionStat>> =
        dao.sessionSummaries().map { rows ->
            rows.map { SessionStat(it.id, it.setName, it.startedAt, it.wordCount, it.correct, it.incorrect, it.skipped) }
        }

    override suspend fun clearAll() = dao.deleteAllSessions()
}
