package pl.aingielski.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pl.aingielski.R
import pl.aingielski.ui.components.AppIcon

/** Warning + typed total word count before deleting all statistics (FR-063). */
@Composable
fun ClearDataDialog(
    input: String,
    showError: Boolean,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIcon(R.drawable.ic_warning, contentDescription = null) },
        title = { Text(stringResource(R.string.clear_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.clear_message))
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(R.string.clear_input_label)) },
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.clear_wrong)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAG_CLEAR_INPUT),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = input.isNotEmpty()) {
                Text(stringResource(R.string.clear_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

const val TAG_CLEAR_INPUT = "clear_input"
