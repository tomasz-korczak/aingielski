package pl.aingielski.ui.words

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.aingielski.R
import pl.aingielski.domain.model.WordItem
import pl.aingielski.domain.repository.WordSetRepository
import pl.aingielski.ui.AppViewModelProvider
import pl.aingielski.ui.components.BackTopBar
import pl.aingielski.ui.navigation.Routes

data class WordSetDetailUiState(val name: String = "", val entries: List<WordItem> = emptyList())

class WordSetDetailViewModel(
    savedStateHandle: SavedStateHandle,
    repository: WordSetRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WordSetDetailUiState())
    val state: StateFlow<WordSetDetailUiState> = _state.asStateFlow()

    init {
        val id = savedStateHandle.get<Long>(Routes.ARG_ID) ?: -1L
        viewModelScope.launch {
            val summary = repository.summary(id)
            _state.value = WordSetDetailUiState(summary?.name.orEmpty(), repository.entries(id))
        }
    }
}

/** Read-only table of a set's content with only a back action (FR-051, FR-056). */
@Composable
fun WordSetDetailScreen(
    onBack: () -> Unit,
    viewModel: WordSetDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { BackTopBar(state.name, onBack) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                TableRow(
                    stringResource(R.string.col_word),
                    stringResource(R.string.col_translation),
                    stringResource(R.string.col_sentence),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .semantics { heading() },
                )
                HorizontalDivider()
            }
            itemsIndexed(state.entries) { _, entry ->
                TableRow(entry.word, entry.translation, entry.sentence, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TableRow(word: String, translation: String, sentence: String, style: TextStyle, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(word, style = style, modifier = Modifier.weight(1f))
        Text(translation, style = style, modifier = Modifier.weight(1f))
        Text(sentence, style = style, modifier = Modifier.weight(1.6f))
    }
}
