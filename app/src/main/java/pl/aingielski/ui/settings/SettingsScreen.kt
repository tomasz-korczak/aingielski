package pl.aingielski.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.aingielski.R
import pl.aingielski.domain.model.EnglishVariant
import pl.aingielski.ui.AppViewModelProvider
import pl.aingielski.ui.components.AppIcon
import pl.aingielski.ui.components.BackTopBar
import pl.aingielski.ui.components.PlayButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDone: () -> Unit,
    onOpenVoiceInstall: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    // System back behaves like "Anuluj" (contract › Routes).
    BackHandler { viewModel.cancel() }
    val sample = stringResource(R.string.preview_sample)

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.settings_title), viewModel::cancel) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = viewModel::cancel,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = viewModel::save,
                    enabled = state.loaded,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.save)) }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.settings_variant), style = MaterialTheme.typography.titleMedium)
            val variants = listOf(EnglishVariant.US to R.string.variant_us, EnglishVariant.GB to R.string.variant_gb)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                variants.forEachIndexed { index, (variant, label) ->
                    SegmentedButton(
                        selected = state.draft.variant == variant,
                        onClick = { viewModel.onVariantChange(variant) },
                        shape = SegmentedButtonDefaults.itemShape(index, variants.size),
                    ) { Text(stringResource(label)) }
                }
            }

            Text(stringResource(R.string.settings_voice), style = MaterialTheme.typography.titleMedium)
            if (state.voicesMissing) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(R.drawable.ic_warning, contentDescription = null)
                            Text(
                                stringResource(
                                    if (state.engineUnavailable) R.string.voice_engine_unavailable else R.string.settings_voices_missing,
                                ),
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                        Button(onClick = onOpenVoiceInstall) { Text(stringResource(R.string.voice_download)) }
                    }
                }
            } else {
                Column(Modifier.selectableGroup()) {
                    VoiceRow(
                        label = stringResource(R.string.settings_voice_default),
                        detail = null,
                        selected = state.draft.voiceId == null,
                        onSelect = { viewModel.onVoiceChange(null) },
                    )
                    state.voices.forEachIndexed { index, voice ->
                        VoiceRow(
                            label = stringResource(R.string.settings_voice_item, index + 1),
                            detail = voice.displayName,
                            selected = state.draft.voiceId == voice.id,
                            onSelect = { viewModel.onVoiceChange(voice.id) },
                        )
                    }
                }
                if (state.voices.isEmpty() && state.loaded) {
                    Text(stringResource(R.string.settings_no_voices), style = MaterialTheme.typography.bodyMedium)
                }
                PlayButton(R.drawable.ic_play, stringResource(R.string.settings_preview), { viewModel.preview(sample) })
            }

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(
                        value = state.draft.skipEnabled,
                        role = Role.Switch,
                        onValueChange = viewModel::onSkipChange,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_skip), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_skip_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.draft.skipEnabled, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun VoiceRow(label: String, detail: String?, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
