package pl.aingielski.domain.quiz

import kotlin.random.Random

data class AnswerCounts(val wordKey: String, val correct: Int, val incorrect: Int)

/** "Difficult word" rule (FR-015) and hardest-first ordering (FR-016). */
object DifficultyRule {
    private const val THRESHOLD_PERCENT = 51

    /** Exact comparison without rounding: incorrect × 100 ≥ 51 × (correct + incorrect). */
    fun isDifficult(correct: Int, incorrect: Int): Boolean {
        val answered = correct + incorrect
        return answered > 0 && incorrect.toLong() * 100 >= THRESHOLD_PERCENT.toLong() * answered
    }

    /** Difficult keys ordered by error ratio, highest first; ties are shuffled with [random]. */
    fun difficultKeys(counts: List<AnswerCounts>, random: Random): List<String> =
        counts
            .filter { isDifficult(it.correct, it.incorrect) }
            .shuffled(random)
            .sortedWith { a, b ->
                // Compare a.incorrect / a.total with b.incorrect / b.total without division.
                val left = b.incorrect.toLong() * (a.correct + a.incorrect)
                val right = a.incorrect.toLong() * (b.correct + b.incorrect)
                left.compareTo(right)
            }
            .map { it.wordKey }
}
