package pl.aingielski.domain.stats

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.floor

/** One finished (completed or aborted) session as shown in statistics. */
data class SessionStat(
    val id: Long,
    val setName: String,
    val startedAt: Long,
    val wordCount: Int,
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
) {
    /** Unrounded success percentage (FR-030 formula). */
    val exactPercent: Double get() = if (wordCount > 0) 100.0 * correct / wordCount else 0.0
}

data class GlobalStats(
    val sessionCount: Int,
    val totalWords: Int,
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
    /** Null when there are no sessions (shown as "—"). */
    val averagePercent: Int?,
)

data class ChartPoint(
    val sessionId: Long,
    val label: String,
    val correct: Int,
    val incorrect: Int,
)

/** Statistics maths (FR-060, FR-061). */
object StatsCalculator {
    const val CHART_SESSIONS = 10
    private val LABEL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")

    fun global(sessions: List<SessionStat>): GlobalStats {
        val correct = sessions.sumOf { it.correct }
        val incorrect = sessions.sumOf { it.incorrect }
        val skipped = sessions.sumOf { it.skipped }
        val average = if (sessions.isEmpty()) {
            null
        } else {
            roundHalfUp(sessions.sumOf { it.exactPercent } / sessions.size)
        }
        return GlobalStats(sessions.size, correct + incorrect + skipped, correct, incorrect, skipped, average)
    }

    /** The newest [CHART_SESSIONS] sessions, oldest first, labelled `dd.MM HH:mm`. */
    fun chart(sessions: List<SessionStat>, zone: ZoneId = ZoneId.systemDefault()): List<ChartPoint> =
        sessions
            .sortedWith(compareByDescending<SessionStat> { it.startedAt }.thenByDescending { it.id })
            .take(CHART_SESSIONS)
            .reversed()
            .map { ChartPoint(it.id, formatLabel(it.startedAt, zone), it.correct, it.incorrect) }

    fun formatLabel(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        LABEL_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun roundHalfUp(value: Double): Int = floor(value + 0.5).toInt()
}
