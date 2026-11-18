package pl.aingielski.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pl.aingielski.AIngielskiApp
import pl.aingielski.AppContainer
import pl.aingielski.ui.quiz.QuizViewModel
import pl.aingielski.ui.quiz.SetupViewModel
import pl.aingielski.ui.quiz.SummaryViewModel
import pl.aingielski.ui.settings.SettingsViewModel
import pl.aingielski.ui.stats.StatsViewModel
import pl.aingielski.ui.words.WordSetDetailViewModel
import pl.aingielski.ui.words.WordSetsViewModel

/** Builds every ViewModel from the [AppContainer] (manual DI, research R11). */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { WordSetsViewModel(container().wordSetRepository, container().csvFileReader::read) }
        initializer { WordSetDetailViewModel(createSavedStateHandle(), container().wordSetRepository) }
        initializer {
            val c = container()
            SetupViewModel(createSavedStateHandle(), c.wordSetRepository, c.sessionRepository, c.settingsRepository, c.speechEngine)
        }
        initializer {
            val c = container()
            QuizViewModel(createSavedStateHandle(), c.sessionRepository, c.settingsRepository, c.speechEngine)
        }
        initializer { SummaryViewModel(createSavedStateHandle(), container().sessionRepository) }
        initializer { SettingsViewModel(container().settingsRepository, container().speechEngine) }
        initializer { StatsViewModel(container().sessionRepository) }
    }

    private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as AIngielskiApp).container
}
