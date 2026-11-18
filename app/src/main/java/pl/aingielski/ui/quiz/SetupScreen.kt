package pl.aingielski.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.aingielski.R
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.quiz.CountError
import pl.aingielski.domain.quiz.QuizSetupValidator
import pl.aingielski.ui.AppViewModelProvider
import pl.aingielski.ui.components.BackTopBar
import pl.aingielski.ui.components.VoiceMissingDialog

@Composable
fun SetupScreen(
    onBack: () -> Unit,
    onOpenWords: () -> Unit,
    onOpenVoiceInstall: () -> Unit,
    onQuizStarted: (Long) -> Unit,
    viewModel: SetupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.startedSessionId) {
        val id = state.startedSessionId ?: return@LaunchedEffect
        viewModel.onNavigated()
        onQuizStarted(id)
    }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.setup_title), onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.loaded && state.sets.isEmpty()) {
                Text(stringResource(R.string.setup_no_sets), style = MaterialTheme.typography.bodyLarge)
                OutlinedButton(onClick = onOpenWords) { Text(stringResource(R.string.setup_go_to_words)) }
                return@Column
            }

            Text(stringResource(R.string.setup_choose_set), style = MaterialTheme.typography.titleMedium)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.sets.forEach { set ->
                    val selected = set.id == state.selectedSetId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { viewModel.onSelectSet(set.id) }),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(set.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    pluralStringResource(R.plurals.word_count, set.wordCount, set.wordCount),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            val selectedSet = state.selectedSet
            if (selectedSet != null) {
                val min = QuizSetupValidator.MIN_WORDS
                val max = selectedSet.wordCount
                OutlinedTextField(
                    value = state.countText,
                    onValueChange = viewModel::onCountChange,
                    label = { Text(stringResource(R.string.setup_count_label)) },
                    singleLine = true,
                    isError = state.countError != null,
                    supportingText = {
                        Text(
                            when (val error = state.countError) {
                                null -> stringResource(R.string.setup_count_range, min, max)
                                CountError.NotANumber -> stringResource(R.string.setup_count_not_number, min, max)
                                is CountError.TooLow -> stringResource(R.string.setup_count_too_low, error.min)
                                is CountError.TooHigh -> pluralStringResource(R.plurals.setup_count_too_high, error.max, error.max)
                            },
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(R.string.setup_criterion), style = MaterialTheme.typography.titleMedium)
                Column(Modifier.selectableGroup()) {
                    CriterionOption(SelectionCriterion.RANDOM, R.string.criterion_random, state, viewModel::onCriterionChange)
                    CriterionOption(SelectionCriterion.NEW_ONLY, R.string.criterion_new, state, viewModel::onCriterionChange)
                    CriterionOption(SelectionCriterion.DIFFICULT, R.string.criterion_difficult, state, viewModel::onCriterionChange)
                }
            }

            Button(
                onClick = viewModel::start,
                enabled = state.canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.setup_start))
            }
        }
    }

    if (state.showVoiceDialog) {
        VoiceMissingDialog(
            engineUnavailable = state.engineUnavailable,
            onDownload = onOpenVoiceInstall,
            onDismiss = viewModel::onVoiceDialogDismiss,
        )
    }
}

@Composable
private fun CriterionOption(
    criterion: SelectionCriterion,
    label: Int,
    state: SetupUiState,
    onSelect: (SelectionCriterion) -> Unit,
) {
    val selected = state.criterion == criterion
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(criterion) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
            val match = state.matchCount
            if (selected && criterion != SelectionCriterion.RANDOM && match != null) {
                val requested = state.countText.toIntOrNull() ?: 0
                Text(
                    pluralStringResource(R.plurals.setup_match_count, match, match) +
                        if (match < requested) " " + stringResource(R.string.setup_match_fill) else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
