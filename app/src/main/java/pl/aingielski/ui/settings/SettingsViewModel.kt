package pl.aingielski.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.aingielski.domain.model.AppSettings
import pl.aingielski.domain.model.EnglishVariant
import pl.aingielski.domain.repository.SettingsRepository
import pl.aingielski.speech.SpeechAvailability
import pl.aingielski.speech.SpeechEngine
import pl.aingielski.speech.VoiceInfo
import pl.aingielski.speech.isReady

data class SettingsUiState(
    val loaded: Boolean = false,
    val draft: AppSettings = AppSettings(),
    val voices: List<VoiceInfo> = emptyList(),
    val voicesMissing: Boolean = false,
    val engineUnavailable: Boolean = false,
    val done: Boolean = false,
)

/** Settings with an editable draft: "Zapisz" persists it, "Anuluj" discards it (FR-040 – FR-043). */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val speech: SpeechEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repository.settings.first()
            _state.update { it.copy(loaded = true, draft = saved) }
            reloadVoices()
        }
        viewModelScope.launch {
            // Voices may appear after the user installs them in system settings (R6).
            speech.availability.collect { reloadVoices() }
        }
    }

    fun onVariantChange(variant: EnglishVariant) {
        _state.update { it.copy(draft = it.draft.copy(variant = variant)) }
        viewModelScope.launch { reloadVoices() }
    }

    fun onVoiceChange(voiceId: String?) {
        _state.update { it.copy(draft = it.draft.copy(voiceId = voiceId)) }
    }

    fun onSkipChange(enabled: Boolean) {
        _state.update { it.copy(draft = it.draft.copy(skipEnabled = enabled)) }
    }

    /** Speaks a sample word with the selected voice (FR-042). */
    fun preview(sample: String) {
        val draft = _state.value.draft
        viewModelScope.launch { speech.speak(sample, draft.variant, draft.voiceId) }
    }

    fun save() {
        val draft = _state.value.draft
        viewModelScope.launch {
            speech.stop()
            repository.save(draft)
            _state.update { it.copy(done = true) }
        }
    }

    fun cancel() {
        speech.stop()
        _state.update { it.copy(done = true) }
    }

    private suspend fun reloadVoices() {
        if (!_state.value.loaded) return
        val variant = _state.value.draft.variant
        val voices = speech.availableVoices(variant)
        val availability = speech.availability.value
        _state.update { current ->
            if (current.draft.variant != variant) return@update current
            // A voice that does not belong to the variant falls back to the default (FR-041).
            val voiceId = current.draft.voiceId?.takeIf { id -> voices.any { it.id == id } }
            current.copy(
                draft = current.draft.copy(voiceId = voiceId),
                voices = voices,
                voicesMissing = availability !is SpeechAvailability.Initializing && !availability.isReady(variant),
                engineUnavailable = availability is SpeechAvailability.EngineUnavailable,
            )
        }
    }

    override fun onCleared() {
        speech.stop()
    }
}
