package pl.aingielski.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.aingielski.domain.model.AppSettings
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.WordSetSummary
import pl.aingielski.domain.quiz.CountError
import pl.aingielski.domain.quiz.PreferredWords
import pl.aingielski.domain.quiz.QuizSetupValidator
import pl.aingielski.domain.quiz.WordSelector
import pl.aingielski.domain.repository.SessionRepository
import pl.aingielski.domain.repository.SettingsRepository
import pl.aingielski.domain.repository.WordSetRepository
import pl.aingielski.domain.text.normalizeKey
import pl.aingielski.speech.SpeechAvailability
import pl.aingielski.speech.SpeechEngine
import pl.aingielski.speech.isReady
import pl.aingielski.ui.navigation.Routes
import kotlin.random.Random

data class SetupUiState(
    val loaded: Boolean = false,
    val sets: List<WordSetSummary> = emptyList(),
    val selectedSetId: Long? = null,
    val countText: String = QuizSetupValidator.DEFAULT_COUNT.toString(),
    val countError: CountError? = null,
    val criterion: SelectionCriterion = SelectionCriterion.RANDOM,
    /** Words of the selected set matching "Tylko nowe" / "Trudne"; null for "Losowe" or while loading. */
    val matchCount: Int? = null,
    val voiceReady: Boolean = false,
    val engineUnavailable: Boolean = false,
    val showVoiceDialog: Boolean = false,
    val starting: Boolean = false,
    val startedSessionId: Long? = null,
) {
    val selectedSet: WordSetSummary? get() = sets.firstOrNull { it.id == selectedSetId }
    val canStart: Boolean get() = selectedSet != null && countError == null && !starting
}

/** Quiz setup (FR-010 – FR-017, FR-021a, FR-032). */
class SetupViewModel(
    savedStateHandle: SavedStateHandle,
    private val wordSets: WordSetRepository,
    private val sessions: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val speech: SpeechEngine,
    private val random: Random = Random.Default,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    private val argSetName: String? = savedStateHandle.get<String>(Routes.ARG_SET_NAME)
    private val argCount: String? = savedStateHandle.get<String>(Routes.ARG_COUNT)
    private val argCriterion: SelectionCriterion? = savedStateHandle.get<String>(Routes.ARG_CRITERION)
        ?.let { value -> SelectionCriterion.entries.firstOrNull { it.name == value } }
    private var argsApplied = false
    private var matchJob: Job? = null

    init {
        viewModelScope.launch {
            wordSets.observeSummaries().collect { sets -> onSetsChanged(sets) }
        }
        viewModelScope.launch {
            combine(settingsRepository.settings, speech.availability) { settings: AppSettings, availability ->
                Triple(
                    availability.isReady(settings.variant),
                    availability is SpeechAvailability.EngineUnavailable,
                    availability is SpeechAvailability.Initializing,
                )
            }.collect { (ready, unavailable, initializing) ->
                _state.update { it.copy(voiceReady = ready || initializing, engineUnavailable = unavailable) }
            }
        }
    }

    private fun onSetsChanged(sets: List<WordSetSummary>) {
        val current = _state.value
        var selected = current.selectedSetId?.takeIf { id -> sets.any { it.id == id } }
        var countText = current.countText
        var criterion = current.criterion
        if (!argsApplied) {
            argsApplied = true
            // "Nowy quiz": preselect the previous set if it still exists, otherwise defaults (FR-032).
            val previous = argSetName?.let { name -> sets.firstOrNull { normalizeKey(it.name) == normalizeKey(name) } }
            if (previous != null) {
                selected = previous.id
                argCount?.let { countText = it }
                argCriterion?.let { criterion = it }
            }
        }
        if (selected == null && sets.size == 1) selected = sets.first().id
        _state.update {
            it.copy(loaded = true, sets = sets, selectedSetId = selected, countText = countText, criterion = criterion)
        }
        revalidate()
        refreshMatchCount()
    }

    fun onSelectSet(id: Long) {
        _state.update { it.copy(selectedSetId = id) }
        revalidate()
        refreshMatchCount()
    }

    fun onCountChange(text: String) {
        _state.update { it.copy(countText = text.filter(Char::isDigit).take(5)) }
        revalidate()
    }

    fun onCriterionChange(criterion: SelectionCriterion) {
        _state.update { it.copy(criterion = criterion) }
        refreshMatchCount()
    }

    fun onVoiceDialogDismiss() {
        _state.update { it.copy(showVoiceDialog = false) }
    }

    fun onNavigated() {
        _state.update { it.copy(startedSessionId = null) }
    }

    fun start() {
        val current = _state.value
        val set = current.selectedSet ?: return
        val count = QuizSetupValidator.check(current.countText, set.wordCount)
        if (count.error != null || count.count == null) {
            _state.update { it.copy(countError = count.error) }
            return
        }
        if (!current.voiceReady || current.engineUnavailable) {
            _state.update { it.copy(showVoiceDialog = true) }
            return
        }
        _state.update { it.copy(starting = true) }
        viewModelScope.launch {
            val entries = wordSets.entries(set.id)
            val preferred = when (current.criterion) {
                SelectionCriterion.RANDOM -> PreferredWords.None
                SelectionCriterion.NEW_ONLY -> PreferredWords.Unordered(sessions.newWordKeys(set.name, entries))
                SelectionCriterion.DIFFICULT -> PreferredWords.Ordered(sessions.difficultWordKeys(set.name, random))
            }
            val words = WordSelector.select(entries, preferred, count.count, random)
            val sessionId = sessions.startSession(set.name, current.criterion, words, clock())
            _state.update { it.copy(starting = false, startedSessionId = sessionId) }
        }
    }

    private fun revalidate() {
        val current = _state.value
        val set = current.selectedSet
        val error = if (set == null) null else QuizSetupValidator.check(current.countText, set.wordCount).error
        _state.update { it.copy(countError = error) }
    }

    private fun refreshMatchCount() {
        matchJob?.cancel()
        val current = _state.value
        val set = current.selectedSet
        if (set == null || current.criterion == SelectionCriterion.RANDOM) {
            _state.update { it.copy(matchCount = null) }
            return
        }
        matchJob = viewModelScope.launch {
            val entries = wordSets.entries(set.id)
            val count = when (current.criterion) {
                SelectionCriterion.NEW_ONLY -> sessions.newWordKeys(set.name, entries).size
                SelectionCriterion.DIFFICULT -> {
                    val keys = entries.mapTo(HashSet()) { normalizeKey(it.word) }
                    sessions.difficultWordKeys(set.name, random).count { it in keys }
                }
                SelectionCriterion.RANDOM -> 0
            }
            _state.update { it.copy(matchCount = count) }
        }
    }
}
