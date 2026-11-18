package pl.aingielski

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import pl.aingielski.data.csv.CsvFileReader
import pl.aingielski.data.db.AppDatabase
import pl.aingielski.data.repository.RoomSessionRepository
import pl.aingielski.data.repository.RoomWordSetRepository
import pl.aingielski.data.settings.DataStoreSettingsRepository
import pl.aingielski.domain.repository.SessionRepository
import pl.aingielski.domain.repository.SettingsRepository
import pl.aingielski.domain.repository.WordSetRepository
import pl.aingielski.speech.SpeechEngine
import pl.aingielski.speech.android.AndroidTtsSpeechEngine

/**
 * Manual dependency injection (research R11). To switch speech backends (e.g. an LLM-based TTS),
 * change only [speechEngine].
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }
    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository(appContext) }
    val speechEngine: SpeechEngine by lazy { AndroidTtsSpeechEngine(appContext) }
    val wordSetRepository: WordSetRepository by lazy { RoomWordSetRepository(database.wordSetDao()) }
    val sessionRepository: SessionRepository by lazy { RoomSessionRepository(database.sessionDao()) }
    val csvFileReader: CsvFileReader by lazy { CsvFileReader(appContext) }
}
