package pl.aingielski.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.aingielski.domain.repository.SessionRepository
import pl.aingielski.domain.stats.ChartPoint
import pl.aingielski.domain.stats.GlobalStats
import pl.aingielski.domain.stats.SessionStat
import pl.aingielski.domain.stats.StatsCalculator
import java.time.ZoneId

data class StatsUiState(
    val loaded: Boolean = false,
    val global: GlobalStats = StatsCalculator.global(emptyList()),
    val chart: List<ChartPoint> = emptyList(),
    /** Newest first (FR-062). */
    val sessions: List<SessionStat> = emptyList(),
    val detailsExpanded: Boolean = false,
    val showClearDialog: Boolean = false,
    val clearInput: String = "",
    val clearError: Boolean = false,
    val cleared: Boolean = false,
) {
    val hasData: Boolean get() = sessions.isNotEmpty()
}

/** Statistics (FR-060 – FR-062) and the protected clear action (FR-063). */
class StatsViewModel(
    private val sessions: SessionRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessions.observeSessionStats().collect { list ->
                _state.update {
                    it.copy(
                        loaded = true,
                        global = StatsCalculator.global(list),
                        chart = StatsCalculator.chart(list, zone),
                        sessions = list.sortedWith(compareByDescending<SessionStat> { s -> s.startedAt }.thenByDescending { s -> s.id }),
                    )
                }
            }
        }
    }

    fun toggleDetails() {
        _state.update { it.copy(detailsExpanded = !it.detailsExpanded) }
    }

    fun openClearDialog() {
        _state.update { it.copy(showClearDialog = true, clearInput = "", clearError = false) }
    }

    fun onClearInputChange(text: String) {
        _state.update { it.copy(clearInput = text.filter(Char::isDigit).take(9), clearError = false) }
    }

    fun dismissClearDialog() {
        _state.update { it.copy(showClearDialog = false, clearInput = "", clearError = false) }
    }

    /** Deletes all statistics only when the typed number equals the total recorded word count. */
    fun confirmClear() {
        val current = _state.value
        val typed = current.clearInput.toIntOrNull()
        if (typed == null || typed != current.global.totalWords) {
            _state.update { it.copy(clearError = true) }
            return
        }
        viewModelScope.launch {
            sessions.clearAll()
            _state.update { it.copy(showClearDialog = false, clearInput = "", clearError = false, cleared = true) }
        }
    }

    fun onClearedMessageShown() {
        _state.update { it.copy(cleared = false) }
    }
}
