package pl.aingielski.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.aingielski.domain.model.AppSettings
import pl.aingielski.domain.model.EnglishVariant
import pl.aingielski.domain.repository.SettingsRepository

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Settings stored with the keys and defaults from data-model.md › Settings. */
class DataStoreSettingsRepository(context: Context) : SettingsRepository {
    private val store = context.applicationContext.settingsDataStore

    override val settings: Flow<AppSettings> = store.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            variant = prefs[ENGLISH_VARIANT]
                ?.let { value -> EnglishVariant.entries.firstOrNull { it.name == value } }
                ?: defaults.variant,
            voiceId = prefs[VOICE_ID]?.takeIf { it.isNotBlank() },
            skipEnabled = prefs[SKIP_ENABLED] ?: defaults.skipEnabled,
        )
    }

    override suspend fun save(settings: AppSettings) {
        store.edit { prefs ->
            prefs[ENGLISH_VARIANT] = settings.variant.name
            if (settings.voiceId == null) prefs.remove(VOICE_ID) else prefs[VOICE_ID] = settings.voiceId
            prefs[SKIP_ENABLED] = settings.skipEnabled
        }
    }

    private companion object {
        val ENGLISH_VARIANT = stringPreferencesKey("english_variant")
        val VOICE_ID = stringPreferencesKey("voice_id")
        val SKIP_ENABLED = booleanPreferencesKey("skip_enabled")
    }
}
