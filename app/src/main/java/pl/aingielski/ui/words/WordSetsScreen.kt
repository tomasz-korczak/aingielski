package pl.aingielski.ui.words

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.aingielski.R
import pl.aingielski.ui.AppViewModelProvider
import pl.aingielski.ui.components.AppIcon
import pl.aingielski.ui.components.BackTopBar

// MIME types offered by the file picker (research R9); the wildcard covers mislabelled CSV files.
private val CSV_MIME_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/csv",
    "application/vnd.ms-excel",
    "*/*",
)

@Composable
fun WordSetsScreen(
    onBack: () -> Unit,
    onOpenSet: (Long) -> Unit,
    viewModel: WordSetsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val importedMessage = state.importedName?.let { stringResource(R.string.words_imported, it) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onFilePicked(uri)
    }

    LaunchedEffect(importedMessage) {
        val message = importedMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.onImportMessageShown()
    }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.words_title), onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { picker.launch(CSV_MIME_TYPES) },
                icon = { AppIcon(R.drawable.ic_file_import, contentDescription = null) },
                text = { Text(stringResource(R.string.words_import)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.loaded && state.sets.isEmpty()) {
                Text(
                    stringResource(R.string.words_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(state.sets, key = { it.id }) { set ->
                    ListItem(
                        headlineContent = { Text(set.name) },
                        supportingContent = {
                            Text(pluralStringResource(R.plurals.word_count, set.wordCount, set.wordCount))
                        },
                        leadingContent = { AppIcon(R.drawable.ic_words, contentDescription = null) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.onDeleteRequest(set) }) {
                                AppIcon(
                                    R.drawable.ic_delete,
                                    contentDescription = stringResource(R.string.words_delete_cd, set.name),
                                )
                            }
                        },
                        modifier = Modifier.clickable { onOpenSet(set.id) },
                    )
                    HorizontalDivider()
                }
            }
            if (state.import == ImportStep.Reading) {
                val readingText = stringResource(R.string.words_reading)
                CircularProgressIndicator(
                    Modifier
                        .align(Alignment.Center)
                        .semantics { contentDescription = readingText },
                )
            }
        }
    }

    when (val step = state.import) {
        is ImportStep.NamePrompt -> SetNameDialog(
            step = step,
            onNameChange = viewModel::onNameChange,
            onConfirm = viewModel::onNameConfirm,
            onCancel = viewModel::onImportCancel,
        )
        is ImportStep.ConfirmOverwrite -> OverwriteDialog(
            name = step.name,
            onConfirm = viewModel::onOverwriteConfirm,
            onCancel = viewModel::onImportCancel,
        )
        is ImportStep.Failed -> ImportErrorDialog(step.result, onDismiss = viewModel::onImportCancel)
        ImportStep.Idle, ImportStep.Reading -> Unit
    }

    state.pendingDelete?.let { set ->
        AlertDialog(
            onDismissRequest = viewModel::onDeleteCancel,
            icon = { AppIcon(R.drawable.ic_delete, contentDescription = null) },
            title = { Text(stringResource(R.string.words_delete_title)) },
            text = { Text(stringResource(R.string.words_delete_message, set.name)) },
            confirmButton = {
                TextButton(onClick = viewModel::onDeleteConfirm) {
                    Text(stringResource(R.string.words_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::onDeleteCancel) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
