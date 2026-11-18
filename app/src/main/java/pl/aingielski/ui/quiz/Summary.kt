package pl.aingielski.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.WordResult
import pl.aingielski.domain.quiz.QuizSummary
import pl.aingielski.domain.quiz.SummaryCalculator
import pl.aingielski.domain.quiz.SummaryItem
import pl.aingielski.domain.repository.SessionRepository
import pl.aingielski.ui.AppViewModelProvider
import pl.aingielski.ui.navigation.Routes
import pl.aingielski.ui.theme.LocalResultColors

data class SummaryUiState(
    val summary: QuizSummary? = null,
    val setName: String = "",
    val wordCount: Int = 0,
    val criterion: SelectionCriterion = SelectionCriterion.RANDOM,
)

class SummaryViewModel(
    savedStateHandle: SavedStateHandle,
    sessions: SessionRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SummaryUiState())
    val state: StateFlow<SummaryUiState> = _state.asStateFlow()

    init {
        val id = savedStateHandle.get<Long>(Routes.ARG_SESSION_ID) ?: -1L
        viewModelScope.launch {
            val info = sessions.session(id)
            val items = sessions.sessionWords(id).map { SummaryItem(it.word, it.translation, it.result, it.typedAnswer) }
            _state.value = SummaryUiState(
                summary = SummaryCalculator.summarize(items, info?.wordCount ?: items.size),
                setName = info?.setName.orEmpty(),
                wordCount = info?.wordCount ?: items.size,
                criterion = info?.criterion ?: SelectionCriterion.RANDOM,
            )
        }
    }
}

/** End-of-quiz summary (FR-030 – FR-032). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onHome: () -> Unit,
    onNewQuiz: (setName: String, count: Int, criterion: SelectionCriterion) -> Unit,
    viewModel: SummaryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val summary = state.summary
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.summary_title)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (summary != null) {
                item {
                    val percentDescription = stringResource(R.string.summary_percent_cd, summary.percent)
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.summary_percent, summary.percent),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { contentDescription = percentDescription },
                        )
                        Spacer(Modifier.size(8.dp))
                        val colors = LocalResultColors.current
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CountChip(R.drawable.ic_check_circle, colors.correct, stringResource(R.string.summary_correct, summary.correct))
                            CountChip(R.drawable.ic_cancel, colors.incorrect, stringResource(R.string.summary_incorrect, summary.incorrect))
                            CountChip(R.drawable.ic_skip, colors.skipped, stringResource(R.string.summary_skipped, summary.skipped))
                        }
                    }
                }
                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                items(summary.items) { SummaryRow(it) }
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { onNewQuiz(state.setName, state.wordCount, state.criterion) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                        ) { Text(stringResource(R.string.summary_new_quiz)) }
                        OutlinedButton(
                            onClick = onHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp),
                        ) { Text(stringResource(R.string.summary_home)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountChip(icon: Int, color: Color, label: String) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SummaryRow(item: SummaryItem) {
    val colors = LocalResultColors.current
    val (color, icon, label) = when (item.result) {
        WordResult.CORRECT -> Triple(colors.correct, R.drawable.ic_check_circle, R.string.result_correct)
        WordResult.INCORRECT -> Triple(colors.incorrect, R.drawable.ic_cancel, R.string.result_incorrect)
        else -> Triple(colors.skipped, R.drawable.ic_skip, R.string.result_skipped)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = stringResource(label), tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.word, style = MaterialTheme.typography.titleMedium)
            Text(item.translation, style = MaterialTheme.typography.bodyMedium)
            if (item.result == WordResult.INCORRECT && item.typedAnswer != null) {
                Text(
                    stringResource(R.string.summary_your_answer, item.typedAnswer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
