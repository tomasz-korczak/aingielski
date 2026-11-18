package pl.aingielski.testing

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pl.aingielski.data.db.AppDatabase
import pl.aingielski.domain.model.EnglishVariant
import pl.aingielski.domain.model.WordItem
import pl.aingielski.speech.SpeakResult
import pl.aingielski.speech.SpeechAvailability
import pl.aingielski.speech.SpeechEngine
import pl.aingielski.speech.VoiceInfo
import pl.aingielski.speech.VoiceInstallAction

fun inMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

fun words(count: Int, prefix: String = "word") = (1..count).map { WordItem("$prefix$it", "słowo$it", "Sentence $it.") }

/** Speech engine for instrumented tests: completes every utterance immediately. */
class InstantSpeechEngine : SpeechEngine {
    override val availability: StateFlow<SpeechAvailability> =
        MutableStateFlow(SpeechAvailability.Ready(EnglishVariant.entries.toSet()))
    val spoken = mutableListOf<String>()
    private var current: CompletableDeferred<SpeakResult>? = null

    override suspend fun refresh() = Unit
    override suspend fun availableVoices(variant: EnglishVariant): List<VoiceInfo> = emptyList()
    override suspend fun speak(text: String, variant: EnglishVariant, voiceId: String?): SpeakResult {
        spoken += text
        return SpeakResult.Completed
    }
    override fun stop() {
        current?.complete(SpeakResult.Interrupted)
    }
    override fun voiceInstallAction(): VoiceInstallAction = VoiceInstallAction.None
    override fun isOutputMuted(): Boolean = false
}
