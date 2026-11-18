package pl.aingielski.domain.quiz

sealed interface CountError {
    data object NotANumber : CountError
    data class TooLow(val min: Int) : CountError
    data class TooHigh(val max: Int) : CountError
}

data class CountCheck(val count: Int?, val error: CountError?)

/** Word-count rules for quiz setup (FR-011, FR-012). */
object QuizSetupValidator {
    const val MIN_WORDS = 10
    const val DEFAULT_COUNT = 10

    fun check(input: String, setSize: Int): CountCheck {
        val value = input.trim().takeIf { it.isNotEmpty() && it.all(Char::isDigit) }?.toIntOrNull()
            ?: return CountCheck(null, CountError.NotANumber)
        return when {
            value < MIN_WORDS -> CountCheck(value, CountError.TooLow(MIN_WORDS))
            value > setSize -> CountCheck(value, CountError.TooHigh(setSize))
            else -> CountCheck(value, null)
        }
    }
}
