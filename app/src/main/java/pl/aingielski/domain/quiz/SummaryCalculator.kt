package pl.aingielski.domain.quiz

import pl.aingielski.domain.model.WordResult

data class SummaryItem(
    val word: String,
    val translation: String,
    val result: WordResult,
    val typedAnswer: String?,
)

data class QuizSummary(
    val correct: Int,
    val incorrect: Int,
    val skipped: Int,
    val percent: Int,
    val items: List<SummaryItem>,
)

/** Quiz summary maths (FR-030). */
object SummaryCalculator {
    /** 100 × correct ÷ total, rounded half-up to a whole number (7 of 12 → 58). */
    fun percent(correct: Int, total: Int): Int {
        if (total <= 0) return 0
        return ((200L * correct + total) / (2L * total)).toInt()
    }

    fun summarize(items: List<SummaryItem>, wordCount: Int = items.size): QuizSummary {
        val correct = items.count { it.result == WordResult.CORRECT }
        val incorrect = items.count { it.result == WordResult.INCORRECT }
        val skipped = items.size - correct - incorrect
        return QuizSummary(correct, incorrect, skipped, percent(correct, wordCount), items)
    }
}
