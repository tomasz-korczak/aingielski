package pl.aingielski.ui.words

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import pl.aingielski.R
import pl.aingielski.domain.csv.CsvField
import pl.aingielski.domain.csv.ImportResult
import pl.aingielski.domain.csv.LineError
import pl.aingielski.domain.csv.SetNameError
import pl.aingielski.domain.csv.SetNameValidator
import pl.aingielski.ui.components.AppIcon

@Composable
fun SetNameDialog(
    step: ImportStep.NamePrompt,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val error = step.error ?: SetNameValidator.validate(step.name).takeIf { step.name.isNotEmpty() }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.import_name_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pluralStringResource(R.plurals.import_entries_found, step.entries.size, step.entries.size))
                OutlinedTextField(
                    value = step.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.import_name_label)) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(setNameErrorText(it)) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = SetNameValidator.validate(step.name) == null) {
                Text(stringResource(R.string.import_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun OverwriteDialog(name: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { AppIcon(R.drawable.ic_warning, contentDescription = null) },
        title = { Text(stringResource(R.string.import_overwrite_title)) },
        text = { Text(stringResource(R.string.import_overwrite_message, name)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.import_overwrite_confirm)) } },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ImportErrorDialog(result: ImportResult, onDismiss: () -> Unit) {
    val messages: List<String> = when (result) {
        is ImportResult.Invalid -> result.errors.map { lineErrorText(it) }
        is ImportResult.TooFew -> listOf(stringResource(R.string.import_error_too_few, result.count))
        ImportResult.Empty -> listOf(stringResource(R.string.import_error_empty))
        ImportResult.Unreadable -> listOf(stringResource(R.string.import_error_unreadable))
        is ImportResult.Success -> emptyList()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIcon(R.drawable.ic_warning, contentDescription = null) },
        title = { Text(stringResource(R.string.import_error_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.import_error_intro), style = MaterialTheme.typography.bodyMedium)
                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(messages) { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}

@Composable
private fun setNameErrorText(error: SetNameError): String = when (error) {
    SetNameError.Blank -> stringResource(R.string.import_name_blank)
    is SetNameError.TooLong -> stringResource(R.string.import_name_too_long, error.max)
}

@Composable
private fun lineErrorText(error: LineError): String = when (error) {
    is LineError.MissingFields -> {
        val names = error.fields.map { fieldName(it) }
        stringResource(R.string.import_error_missing, error.line, names.joinToString(stringResource(R.string.field_separator)))
    }
    is LineError.TooManyFields -> stringResource(R.string.import_error_too_many, error.line)
    is LineError.UnclosedQuote -> stringResource(R.string.import_error_unclosed, error.line)
    is LineError.DuplicateWord -> stringResource(R.string.import_error_duplicate, error.line, error.word, error.firstLine)
}

@Composable
private fun fieldName(field: CsvField): String = stringResource(
    when (field) {
        CsvField.WORD -> R.string.field_word
        CsvField.TRANSLATION -> R.string.field_translation
        CsvField.SENTENCE -> R.string.field_sentence
    },
)
