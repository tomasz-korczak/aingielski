package pl.aingielski.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.aingielski.R
import pl.aingielski.domain.model.WordResult
import pl.aingielski.ui.AppViewModelProvider
import pl.aingielski.ui.components.AppIcon
import pl.aingielski.ui.components.NoSuggestionsTextField
import pl.aingielski.ui.components.PlayButton
import pl.aingielski.ui.components.ResultBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onFinished: (Long) -> Unit,
    onExitToHome: () -> Unit,
    viewModel: QuizViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val mutedText = stringResource(R.string.quiz_muted)

    // There is no way back to a previous word (FR-027).
    BackHandler(enabled = true) { viewModel.onBack() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onStop() }

    LaunchedEffect(state.navigation) {
        when (val nav = state.navigation) {
            is QuizNavigation.Summary -> onFinished(nav.sessionId)
            QuizNavigation.Home -> onExitToHome()
            null -> return@LaunchedEffect
        }
        viewModel.onNavigated()
    }
    LaunchedEffect(state.showMutedHint) {
        if (state.showMutedHint) {
            snackbar.showSnackbar(mutedText)
            viewModel.onMutedHintShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.quiz_title)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.total > 0) {
                val progressDescription = stringResource(R.string.quiz_progress_cd, state.index + 1, state.total)
                LinearProgressIndicator(
                    progress = { (state.index + 1).toFloat() / state.total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.quiz_progress, state.index + 1, state.total),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { contentDescription = progressDescription },
                )
            }

            when (state.phase) {
                QuizPhase.Loading -> CircularProgressIndicator()
                QuizPhase.Playing -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(min = 120.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text(stringResource(R.string.quiz_listening), style = MaterialTheme.typography.titleLarge)
                }
                QuizPhase.Answering -> AnsweringContent(state, viewModel)
                QuizPhase.Revealed -> state.revealed?.let { RevealedContent(it) }
            }

            if (state.phase == QuizPhase.Answering || state.phase == QuizPhase.Revealed) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PlayButton(R.drawable.ic_replay, stringResource(R.string.quiz_replay), viewModel::replayWord)
                    PlayButton(R.drawable.ic_sentence, stringResource(R.string.quiz_play_sentence), viewModel::playSentence)
                }
            }

            if (state.phase == QuizPhase.Revealed) {
                Button(
                    onClick = viewModel::next,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                ) {
                    Text(stringResource(if (state.isLast) R.string.quiz_finish else R.string.quiz_next))
                }
            }

            if (state.canAbort) {
                TextButton(onClick = viewModel::requestAbort, modifier = Modifier.heightIn(min = 48.dp)) {
                    AppIcon(R.drawable.ic_stop, contentDescription = null)
                    Text(stringResource(R.string.quiz_abort), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    if (state.showAbortDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAbort,
            icon = { AppIcon(R.drawable.ic_warning, contentDescription = null) },
            title = { Text(stringResource(R.string.abort_title)) },
            text = {
                Text(stringResource(if (state.hasAnswered) R.string.abort_message_saved else R.string.abort_message_nothing))
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmAbort) {
                    Text(stringResource(R.string.abort_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAbort) { Text(stringResource(R.string.abort_continue)) }
            },
        )
    }
}

@Composable
private fun AnsweringContent(state: QuizUiState, viewModel: QuizViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        if (state.playbackFailed) {
            Text(
                stringResource(R.string.quiz_playback_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        NoSuggestionsTextField(
            value = state.answer,
            onValueChange = viewModel::onAnswerChange,
            onDone = viewModel::submit,
            label = stringResource(R.string.quiz_answer_hint),
            maxLength = QuizViewModel.MAX_ANSWER_LENGTH,
            focusKey = state.index,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.quiz_check)) }
            if (state.skipEnabled) {
                OutlinedButton(
                    onClick = viewModel::skip,
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    AppIcon(R.drawable.ic_skip, contentDescription = null)
                    Text(stringResource(R.string.quiz_skip), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** Word, translation and sentence are composed only here, after the reveal (FR-026, FR-027a). */
@Composable
private fun RevealedContent(revealed: RevealedWord) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ResultBadge(revealed.result)
        if (revealed.result != WordResult.CORRECT) {
            Text(
                stringResource(R.string.quiz_correct_word, revealed.word),
                style = MaterialTheme.typography.headlineSmall,
            )
            revealed.typedAnswer?.let {
                Text(stringResource(R.string.summary_your_answer, it), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Text(revealed.word, style = MaterialTheme.typography.headlineSmall)
        }
        Text(stringResource(R.string.quiz_translation, revealed.translation), style = MaterialTheme.typography.titleMedium)
        Text(
            revealed.sentence,
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
