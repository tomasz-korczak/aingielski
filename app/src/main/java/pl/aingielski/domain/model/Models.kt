package pl.aingielski.domain.model

/** One word entry as used by the domain: English word, Polish translation, English sentence. */
data class WordItem(
    val word: String,
    val translation: String,
    val sentence: String,
)

data class WordSetSummary(
    val id: Long,
    val name: String,
    val wordCount: Int,
)

/** User settings with the defaults from data-model.md › Settings. */
data class AppSettings(
    val variant: EnglishVariant = EnglishVariant.US,
    val voiceId: String? = null,
    val skipEnabled: Boolean = true,
)

/** A word as stored in a session. */
data class SessionWordInfo(
    val position: Int,
    val word: String,
    val translation: String,
    val sentence: String,
    val result: WordResult,
    val typedAnswer: String?,
)

data class SessionInfo(
    val id: Long,
    val setName: String,
    val criterion: SelectionCriterion,
    val wordCount: Int,
    val status: SessionStatus,
)
