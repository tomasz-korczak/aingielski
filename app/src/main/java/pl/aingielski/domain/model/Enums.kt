package pl.aingielski.domain.model

/** English pronunciation variant (data-model.md › Settings `english_variant`). */
enum class EnglishVariant { US, GB }

/** Quiz word-selection criterion (data-model.md `quiz_session.criterion`). */
enum class SelectionCriterion { RANDOM, NEW_ONLY, DIFFICULT }

/** Result of one quiz word (data-model.md `session_word.result`). */
enum class WordResult { PENDING, CORRECT, INCORRECT, SKIPPED }

/** Lifecycle of a quiz session (data-model.md `quiz_session.status`). */
enum class SessionStatus { IN_PROGRESS, COMPLETED, ABORTED }
