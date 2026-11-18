package pl.aingielski.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import pl.aingielski.data.db.entity.QuizSessionEntity
import pl.aingielski.data.db.entity.SessionWordEntity
import pl.aingielski.domain.model.SessionStatus
import pl.aingielski.domain.model.WordResult

data class AnswerCountRow(
    val wordKey: String,
    val correct: Int,
    val incorrect: Int,
)

data class SessionStatRow(
    val id: Long,
    val setName: String,
    val startedAt: Long,
    val wordCount: Int,
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
)

@Dao
abstract class SessionDao {

    // --- Quiz operations (US2) ---------------------------------------------------------------

    @Insert
    protected abstract suspend fun insertSessionRow(session: QuizSessionEntity): Long

    @Insert
    protected abstract suspend fun insertWordRows(words: List<SessionWordEntity>)

    /** Writes the session and all its words (PENDING, not played) at quiz start (research R8). */
    @Transaction
    open suspend fun createSession(session: QuizSessionEntity, words: List<SessionWordEntity>): Long {
        val id = insertSessionRow(session)
        insertWordRows(words.map { it.copy(id = 0, sessionId = id) })
        return id
    }

    @Query(
        """
        UPDATE session_word SET result = :result, typed_answer = :typedAnswer, played = 1
        WHERE session_id = :sessionId AND position = :position
        """,
    )
    abstract suspend fun recordResult(sessionId: Long, position: Int, result: WordResult, typedAnswer: String?)

    @Query("SELECT * FROM session_word WHERE session_id = :sessionId ORDER BY position")
    abstract suspend fun sessionWords(sessionId: Long): List<SessionWordEntity>

    @Query("SELECT * FROM quiz_session WHERE id = :sessionId LIMIT 1")
    abstract suspend fun session(sessionId: Long): QuizSessionEntity?

    @Query("UPDATE quiz_session SET status = 'COMPLETED' WHERE id = :sessionId AND status = 'IN_PROGRESS'")
    abstract suspend fun markCompleted(sessionId: Long)

    @Query("SELECT COUNT(*) FROM session_word WHERE session_id = :sessionId AND result IN ('CORRECT', 'INCORRECT')")
    protected abstract suspend fun answeredCount(sessionId: Long): Int

    @Query("UPDATE session_word SET result = 'SKIPPED', played = 0 WHERE session_id = :sessionId AND result = 'PENDING'")
    protected abstract suspend fun skipPending(sessionId: Long)

    @Query("UPDATE quiz_session SET status = 'ABORTED' WHERE id = :sessionId")
    protected abstract suspend fun markAborted(sessionId: Long)

    @Query("DELETE FROM quiz_session WHERE id = :sessionId")
    protected abstract suspend fun deleteSession(sessionId: Long)

    /**
     * Abort / interruption rule (FR-028, FR-028a, data-model.md › Session states). A no-op unless the
     * session exists and is still IN_PROGRESS, so it is safe to call more than once.
     */
    @Transaction
    open suspend fun finalizeInterrupted(sessionId: Long) {
        val session = session(sessionId) ?: return
        if (session.status != SessionStatus.IN_PROGRESS) return
        if (answeredCount(sessionId) > 0) {
            skipPending(sessionId)
            markAborted(sessionId)
        } else {
            deleteSession(sessionId)
        }
    }

    @Query("SELECT id FROM quiz_session WHERE status = 'IN_PROGRESS'")
    abstract suspend fun staleInProgressIds(): List<Long>

    // --- History (US3) -----------------------------------------------------------------------

    @Query(
        """
        SELECT DISTINCT w.word_key FROM session_word w
        JOIN quiz_session s ON s.id = w.session_id
        WHERE s.set_name_key = :setNameKey AND s.status IN ('COMPLETED', 'ABORTED') AND w.played = 1
        """,
    )
    abstract suspend fun playedKeys(setNameKey: String): List<String>

    @Query(
        """
        SELECT w.word_key AS wordKey,
               SUM(CASE WHEN w.result = 'CORRECT' THEN 1 ELSE 0 END) AS correct,
               SUM(CASE WHEN w.result = 'INCORRECT' THEN 1 ELSE 0 END) AS incorrect
        FROM session_word w
        JOIN quiz_session s ON s.id = w.session_id
        WHERE s.set_name_key = :setNameKey AND s.status IN ('COMPLETED', 'ABORTED')
        GROUP BY w.word_key
        """,
    )
    abstract suspend fun answerCounts(setNameKey: String): List<AnswerCountRow>

    // --- Statistics (US5) --------------------------------------------------------------------

    @Query(
        """
        SELECT s.id AS id, s.set_name AS setName, s.started_at AS startedAt, s.word_count AS wordCount,
               SUM(CASE WHEN w.result = 'CORRECT' THEN 1 ELSE 0 END) AS correct,
               SUM(CASE WHEN w.result = 'INCORRECT' THEN 1 ELSE 0 END) AS incorrect,
               SUM(CASE WHEN w.result IN ('SKIPPED', 'PENDING') THEN 1 ELSE 0 END) AS skipped
        FROM quiz_session s
        JOIN session_word w ON w.session_id = s.id
        WHERE s.status IN ('COMPLETED', 'ABORTED')
        GROUP BY s.id
        ORDER BY s.started_at DESC, s.id DESC
        """,
    )
    abstract fun sessionSummaries(): Flow<List<SessionStatRow>>

    // --- Clearing (US6) ----------------------------------------------------------------------

    /** Removes every session; `session_word` rows cascade. Word sets are untouched (FR-063). */
    @Query("DELETE FROM quiz_session")
    abstract suspend fun deleteAllSessions()
}
