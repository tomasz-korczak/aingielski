package pl.aingielski.testing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pl.aingielski.domain.model.AppSettings
import pl.aingielski.domain.model.EnglishVariant
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.SessionInfo
import pl.aingielski.domain.model.SessionStatus
import pl.aingielski.domain.model.SessionWordInfo
import pl.aingielski.domain.model.WordItem
import pl.aingielski.domain.model.WordResult
import pl.aingielski.domain.repository.SessionRepository
import pl.aingielski.domain.repository.SettingsRepository
import pl.aingielski.domain.stats.SessionStat
import pl.aingielski.speech.SpeakResult
import pl.aingielski.speech.SpeechAvailability
import pl.aingielski.speech.SpeechEngine
import pl.aingielski.speech.VoiceInfo
import pl.aingielski.speech.VoiceInstallAction
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Test double for [SpeechEngine] (contract guarantees S1, S2, S7). */
class FakeSpeechEngine : SpeechEngine {
    val availabilityFlow = MutableStateFlow<SpeechAvailability>(
        SpeechAvailability.Ready(EnglishVariant.entries.toSet()),
    )
    override val availability: StateFlow<SpeechAvailability> = availabilityFlow

    var voices: Map<EnglishVariant, List<VoiceInfo>> = mapOf(
        EnglishVariant.US to listOf(VoiceInfo("us-1", "us-1", EnglishVariant.US), VoiceInfo("us-2", "us-2", EnglishVariant.US)),
        EnglishVariant.GB to listOf(VoiceInfo("gb-1", "gb-1", EnglishVariant.GB)),
    )

    /** When true, [speak] returns [nextResult] immediately; otherwise it waits for [completeCurrent]. */
    var autoComplete = true
    var nextResult = SpeakResult.Completed
    var muted = false
    val spoken = mutableListOf<String>()
    var stopCount = 0
        private set
    private var current: CompletableDeferred<SpeakResult>? = null

    override suspend fun refresh() = Unit

    override suspend fun availableVoices(variant: EnglishVariant): List<VoiceInfo> = voices[variant].orEmpty()

    override suspend fun speak(text: String, variant: EnglishVariant, voiceId: String?): SpeakResult {
        current?.complete(SpeakResult.Interrupted)
        spoken += text
        if (autoComplete) return nextResult
        val deferred = CompletableDeferred<SpeakResult>()
        current = deferred
        return deferred.await()
    }

    fun completeCurrent(result: SpeakResult = SpeakResult.Completed) {
        current?.complete(result)
        current = null
    }

    /** A phone call / headphones unplugged: playback stops (S7). */
    fun simulateInterruption() = stop()

    override fun stop() {
        stopCount++
        current?.complete(SpeakResult.Interrupted)
        current = null
    }

    override fun voiceInstallAction(): VoiceInstallAction = VoiceInstallAction.OpenSystemScreen("test")

    override fun isOutputMuted(): Boolean = muted
}

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    val flow = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = flow
    var saveCount = 0
        private set

    override suspend fun save(settings: AppSettings) {
        saveCount++
        flow.value = settings
    }
}

/** In-memory [SessionRepository] following the same rules as the Room implementation. */
class FakeSessionRepository : SessionRepository {
    data class StoredSession(
        val info: SessionInfo,
        val startedAt: Long,
        val words: MutableList<SessionWordInfo>,
        val played: MutableList<Boolean>,
    )

    val sessions = linkedMapOf<Long, StoredSession>()
    private var nextId = 1L
    var finalizeCalls = 0
        private set
    val statsFlow = MutableStateFlow<List<SessionStat>>(emptyList())
    var clearCount = 0
        private set

    fun seed(words: List<WordItem>, setName: String = "Zestaw"): Long {
        val id = nextId++
        sessions[id] = StoredSession(
            SessionInfo(id, setName, SelectionCriterion.RANDOM, words.size, SessionStatus.IN_PROGRESS),
            0L,
            words.mapIndexed { i, w -> SessionWordInfo(i, w.word, w.translation, w.sentence, WordResult.PENDING, null) }
                .toMutableList(),
            MutableList(words.size) { false },
        )
        return id
    }

    override suspend fun startSession(
        setName: String,
        criterion: SelectionCriterion,
        words: List<WordItem>,
        startedAt: Long,
    ): Long = seed(words, setName)

    override suspend fun session(sessionId: Long): SessionInfo? = sessions[sessionId]?.info

    override suspend fun sessionWords(sessionId: Long): List<SessionWordInfo> = sessions[sessionId]?.words.orEmpty()

    override suspend fun recordResult(sessionId: Long, position: Int, result: WordResult, typedAnswer: String?) {
        val s = sessions[sessionId] ?: return
        s.words[position] = s.words[position].copy(result = result, typedAnswer = typedAnswer)
        s.played[position] = true
    }

    override suspend fun complete(sessionId: Long) {
        val s = sessions[sessionId] ?: return
        if (s.info.status == SessionStatus.IN_PROGRESS) {
            sessions[sessionId] = s.copy(info = s.info.copy(status = SessionStatus.COMPLETED))
        }
    }

    override suspend fun finalizeInterrupted(sessionId: Long) {
        finalizeCalls++
        val s = sessions[sessionId] ?: return
        if (s.info.status != SessionStatus.IN_PROGRESS) return
        val answered = s.words.any { it.result == WordResult.CORRECT || it.result == WordResult.INCORRECT }
        if (!answered) {
            sessions.remove(sessionId)
            return
        }
        s.words.indices.forEach { i ->
            if (s.words[i].result == WordResult.PENDING) {
                s.words[i] = s.words[i].copy(result = WordResult.SKIPPED)
                s.played[i] = false
            }
        }
        sessions[sessionId] = s.copy(info = s.info.copy(status = SessionStatus.ABORTED))
    }

    override suspend fun finalizeAllStale() {
        sessions.keys.toList().forEach { finalizeInterrupted(it) }
    }

    override suspend fun newWordKeys(setName: String, entries: List<WordItem>): Set<String> =
        entries.map { it.word.lowercase() }.toSet()

    override suspend fun difficultWordKeys(setName: String, random: Random): List<String> = emptyList()

    override fun observeSessionStats(): Flow<List<SessionStat>> = statsFlow.map { it }

    override suspend fun clearAll() {
        clearCount++
        statsFlow.value = emptyList()
        sessions.clear()
    }
}

fun sampleWords(count: Int) = (1..count).map { WordItem("word$it", "słowo$it", "Sentence $it.") }
