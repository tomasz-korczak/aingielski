package pl.aingielski.ui.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.aingielski.R
import pl.aingielski.domain.stats.SessionStat
import pl.aingielski.domain.stats.StatsCalculator
import pl.aingielski.ui.AppViewModelProvider
import pl.aingielski.ui.components.AppIcon
import pl.aingielski.ui.components.BackTopBar
import pl.aingielski.ui.components.LineChart

@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val clearedText = stringResource(R.string.stats_cleared)
    LaunchedEffect(state.cleared) {
        if (state.cleared) {
            snackbar.showSnackbar(clearedText)
            viewModel.onClearedMessageShown()
        }
    }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.stats_title), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TotalsCard(state) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.stats_chart_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                        LineChart(state.chart)
                    }
                }
            }
            if (state.hasData) {
                item { DetailsHeader(state.detailsExpanded, viewModel::toggleDetails) }
                if (state.detailsExpanded) {
                    items(state.sessions, key = { it.id }) { SessionRow(it) }
                }
                item {
                    OutlinedButton(
                        onClick = viewModel::openClearDialog,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(top = 8.dp),
                    ) {
                        AppIcon(R.drawable.ic_delete, contentDescription = null)
                        Text(stringResource(R.string.stats_clear), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (state.showClearDialog) {
        ClearDataDialog(
            input = state.clearInput,
            showError = state.clearError,
            onInputChange = viewModel::onClearInputChange,
            onConfirm = viewModel::confirmClear,
            onDismiss = viewModel::dismissClearDialog,
        )
    }
}

@Composable
private fun TotalsCard(state: StatsUiState) {
    val global = state.global
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatValue(stringResource(R.string.stats_sessions), global.sessionCount.toString())
                StatValue(stringResource(R.string.stats_words), global.totalWords.toString())
                StatValue(
                    stringResource(R.string.stats_average),
                    global.averagePercent?.let { stringResource(R.string.stats_percent, it) }
                        ?: stringResource(R.string.stats_no_value),
                )
            }
            Text(
                stringResource(R.string.stats_session_counts, global.correct, global.incorrect, global.skipped),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun StatValue(label: String, value: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun DetailsHeader(expanded: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val stateText = stringResource(if (expanded) R.string.collapse else R.string.expand)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics(mergeDescendants = true) {
                heading()
                stateDescription = stateText
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.stats_details), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        AppIcon(R.drawable.ic_chevron_down, contentDescription = null, modifier = Modifier.rotate(rotation))
    }
    HorizontalDivider()
}

@Composable
private fun SessionRow(session: SessionStat) {
    Column(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = 8.dp),
    ) {
        Text(session.setName, style = MaterialTheme.typography.titleMedium)
        Text(StatsCalculator.formatLabel(session.startedAt), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(R.string.stats_session_counts, session.correct, session.incorrect, session.skipped),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}
