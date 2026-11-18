package pl.aingielski.ui.words

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.aingielski.data.csv.CsvFileContent
import pl.aingielski.domain.csv.ImportResult
import pl.aingielski.domain.csv.SetNameError
import pl.aingielski.domain.csv.SetNameValidator
import pl.aingielski.domain.csv.WordSetImportValidator
import pl.aingielski.domain.model.WordItem
import pl.aingielski.domain.model.WordSetSummary
import pl.aingielski.domain.repository.WordSetRepository

/** Steps of the import flow (FR-052 – FR-055). */
sealed interface ImportStep {
    data object Idle : ImportStep
    data object Reading : ImportStep
    data class NamePrompt(val entries: List<WordItem>, val name: String, val error: SetNameError?) : ImportStep
    data class ConfirmOverwrite(val entries: List<WordItem>, val name: String) : ImportStep
    data class Failed(val result: ImportResult) : ImportStep
}

data class WordSetsUiState(
    val sets: List<WordSetSummary> = emptyList(),
    val loaded: Boolean = false,
    val import: ImportStep = ImportStep.Idle,
    val pendingDelete: WordSetSummary? = null,
    /** Name of a set just imported; the screen shows a confirmation and calls [WordSetsViewModel.onImportMessageShown]. */
    val importedName: String? = null,
)

class WordSetsViewModel(
    private val repository: WordSetRepository,
    private val readFile: suspend (Uri) -> CsvFileContent,
) : ViewModel() {

    private val _state = MutableStateFlow(WordSetsUiState())
    val state: StateFlow<WordSetsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSummaries().collect { sets -> _state.update { it.copy(sets = sets, loaded = true) } }
        }
    }

    fun onFilePicked(uri: Uri) {
        _state.update { it.copy(import = ImportStep.Reading) }
        viewModelScope.launch {
            val content = readFile(uri)
            val result = content.text?.let(WordSetImportValidator::validate) ?: ImportResult.Unreadable
            _state.update {
                it.copy(
                    import = if (result is ImportResult.Success) {
                        val name = SetNameValidator.defaultName(content.displayName)
                        ImportStep.NamePrompt(result.entries, name, null)
                    } else {
                        ImportStep.Failed(result)
                    },
                )
            }
        }
    }

    fun onNameChange(name: String) {
        val step = _state.value.import as? ImportStep.NamePrompt ?: return
        _state.update { it.copy(import = step.copy(name = name, error = null)) }
    }

    fun onNameConfirm() {
        val step = _state.value.import as? ImportStep.NamePrompt ?: return
        val error = SetNameValidator.validate(step.name)
        if (error != null) {
            _state.update { it.copy(import = step.copy(error = error)) }
            return
        }
        viewModelScope.launch {
            val name = step.name.trim()
            if (repository.findByName(name) != null) {
                _state.update { it.copy(import = ImportStep.ConfirmOverwrite(step.entries, name)) }
            } else {
                save(name, step.entries, overwrite = false)
            }
        }
    }

    fun onOverwriteConfirm() {
        val step = _state.value.import as? ImportStep.ConfirmOverwrite ?: return
        viewModelScope.launch { save(step.name, step.entries, overwrite = true) }
    }

    /** Any "Anuluj" during import aborts it with no changes (FR-054, FR-055). */
    fun onImportCancel() {
        _state.update { it.copy(import = ImportStep.Idle) }
    }

    fun onImportMessageShown() {
        _state.update { it.copy(importedName = null) }
    }

    fun onDeleteRequest(set: WordSetSummary) {
        _state.update { it.copy(pendingDelete = set) }
    }

    fun onDeleteCancel() {
        _state.update { it.copy(pendingDelete = null) }
    }

    fun onDeleteConfirm() {
        val set = _state.value.pendingDelete ?: return
        _state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch { repository.delete(set.id) }
    }

    private suspend fun save(name: String, entries: List<WordItem>, overwrite: Boolean) {
        repository.importSet(name, entries, overwrite)
        _state.update { it.copy(import = ImportStep.Idle, importedName = name) }
    }
}
