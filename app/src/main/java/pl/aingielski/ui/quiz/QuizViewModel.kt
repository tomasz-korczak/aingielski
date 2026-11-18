package pl.aingielski.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.aingielski.domain.model.AppSettings
import pl.aingielski.domain.model.SessionWordInfo
import pl.aingielski.domain.model.WordResult
import pl.aingielski.domain.repository.SessionRepository
import pl.aingielski.domain.repository.SettingsRepository
import pl.aingielski.domain.text.AnswerChecker
import pl.aingielski.speech.SpeakResult
import pl.aingielski.speech.SpeechEngine
import pl.aingielski.ui.navigation.Routes

/** Quiz screen phases (contracts/ui-navigation.md › Quiz screen phases). */
enum class QuizPhase { Loading, Playing, Answering, Revealed }

/** Shown only after the answer (or skip); before that the word is never in the UI state (FR-027a). */
data class RevealedWord(
    val result: WordResult,
    val word: String,
    val translation: String,
    val sentence: String,
    val typedAnswer: String?,
)

sealed interface QuizNavigation {
    data class Summary(val sessionId: Long) : QuizNavigation
    data object Home : QuizNavigation
}

data class QuizUiState(
    val phase: QuizPhase = QuizPhase.Loading,
    val index: Int = 0,
    val total: Int = 0,
    val answer: String = "",
    val revealed: RevealedWord? = null,
    val skipEnabled: Boolean = true,
    val playbackFailed: Boolean = false,
    val showMutedHint: Boolean = false,
    val showAbortDialog: Boolean = false,
    val hasAnswered: Boolean = false,
    val navigation: QuizNavigation? = null,
) {
    val isLast: Boolean get() = index == total - 1
    val canSubmit: Boolean get() = phase == QuizPhase.Answering && answer.isNotBlank()
    val canAbort: Boolean get() = skipEnabled && (phase == QuizPhase.Answering || phase == QuizPhase.Revealed)
}

/** Quiz state machine (FR-020 – FR-029, research R8). */
class QuizViewModel(
    savedStateHandle: SavedStateHandle,
    private val sessions: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val speech: SpeechEngine,
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>(Routes.ARG_SESSION_ID) ?: -1L
    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var words: List<SessionWordInfo> = emptyList()
    private var settings = AppSettings()
    private var persistJob: Job? = null

    init {
        if (savedStateHandle.get<Boolean>(KEY_STARTED) == true) {
            // Recreated after the system closed the app: finalize like an abort, never resume (FR-028a).
            viewModelScope.launch {
                sessions.finalizeInterrupted(sessionId)
                _state.update { it.copy(navigation = QuizNavigation.Home) }
            }
        } else {
            savedStateHandle[KEY_STARTED] = true
            viewModelScope.launch {
                // Settings in effect at quiz start apply until the quiz ends (FR-021).
                settings = settingsRepository.settings.first()
                words = sessions.sessionWords(sessionId)
                if (words.isEmpty()) {
                    _state.update { it.copy(navigation = QuizNavigation.Home) }
                    return@launch
                }
                _state.update {
                    it.copy(total = words.size, skipEnabled = settings.skipEnabled, showMutedHint = speech.isOutputMuted())
                }
                present(0)
            }
        }
    }

    private fun present(index: Int) {
        val word = words[index]
        _state.update {
            it.copy(phase = QuizPhase.Playing, index = index, answer = "", revealed = null, playbackFailed = false)
        }
        viewModelScope.launch {
            val result = speech.speak(word.word, settings.variant, settings.voiceId)
            // The answer field appears after the first playback, even if it failed (FR-022).
            _state.update {
                if (it.phase == QuizPhase.Playing && it.index == index) {
                    it.copy(phase = QuizPhase.Answering, playbackFailed = result != SpeakResult.Completed)
                } else {
                    it
                }
            }
        }
    }

    fun replayWord() = play { it.word }

    fun playSentence() = play { it.sentence }

    private fun play(text: (SessionWordInfo) -> String) {
        val current = _state.value
        if (current.phase != QuizPhase.Answering && current.phase != QuizPhase.Revealed) return
        val index = current.index
        viewModelScope.launch {
            val result = speech.speak(text(words[index]), settings.variant, settings.voiceId)
            if (result == SpeakResult.Completed) {
                _state.update { if (it.index == index) it.copy(playbackFailed = false) else it }
            }
        }
    }

    fun onAnswerChange(text: String) {
        _state.update { if (it.phase == QuizPhase.Answering) it.copy(answer = text.take(MAX_ANSWER_LENGTH)) else it }
    }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        val word = words[current.index]
        val result = if (AnswerChecker.isCorrect(current.answer, word.word)) WordResult.CORRECT else WordResult.INCORRECT
        persist(current.index, result, current.answer)
        _state.update {
            it.copy(
                phase = QuizPhase.Revealed,
                revealed = RevealedWord(result, word.word, word.translation, word.sentence, current.answer),
                hasAnswered = true,
                playbackFailed = false,
            )
        }
    }

    /** Skip (FR-024): records SKIPPED and shows the same revealed state; the next word waits for [next]. */
    fun skip() {
        val current = _state.value
        if (!current.skipEnabled || current.phase != QuizPhase.Answering) return
        val word = words[current.index]
        persist(current.index, WordResult.SKIPPED, null)
        _state.update {
            it.copy(
                phase = QuizPhase.Revealed,
                revealed = RevealedWord(WordResult.SKIPPED, word.word, word.translation, word.sentence, null),
                playbackFailed = false,
            )
        }
    }

    fun next() {
        val current = _state.value
        if (current.phase != QuizPhase.Revealed) return
        speech.stop()
        if (current.isLast) {
            viewModelScope.launch {
                persistJob?.join()
                sessions.complete(sessionId)
                _state.update { it.copy(navigation = QuizNavigation.Summary(sessionId)) }
            }
        } else {
            present(current.index + 1)
        }
    }

    /** System back (FR-027): opens the abort confirmation when skipping is enabled, otherwise nothing. */
    fun onBack() = requestAbort()

    fun requestAbort() {
        if (_state.value.canAbort) _state.update { it.copy(showAbortDialog = true) }
    }

    fun dismissAbort() {
        _state.update { it.copy(showAbortDialog = false) }
    }

    fun confirmAbort() {
        speech.stop()
        _state.update { it.copy(showAbortDialog = false) }
        viewModelScope.launch {
            persistJob?.join()
            sessions.finalizeInterrupted(sessionId)
            _state.update { it.copy(navigation = QuizNavigation.Home) }
        }
    }

    /** The screen went to the background: stop playback; nothing replays on return (FR-023a). */
    fun onStop() = speech.stop()

    fun onMutedHintShown() {
        _state.update { it.copy(showMutedHint = false) }
    }

    fun onNavigated() {
        _state.update { it.copy(navigation = null) }
    }

    /** Results are written immediately and in order (FR-028a). */
    private fun persist(position: Int, result: WordResult, typed: String?) {
        val previous = persistJob
        persistJob = viewModelScope.launch {
            previous?.join()
            sessions.recordResult(sessionId, position, result, typed)
        }
    }

    override fun onCleared() {
        speech.stop()
    }

    companion object {
        const val KEY_STARTED = "quiz_started"
        const val MAX_ANSWER_LENGTH = 200
    }
}
