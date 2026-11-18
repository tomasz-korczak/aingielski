package pl.aingielski.speech.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import pl.aingielski.domain.model.EnglishVariant
import pl.aingielski.speech.SpeakResult
import pl.aingielski.speech.SpeechAvailability
import pl.aingielski.speech.SpeechEngine
import pl.aingielski.speech.VoiceInfo
import pl.aingielski.speech.VoiceInstallAction
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Production [SpeechEngine] wrapping Android [TextToSpeech] (research R5–R6, guarantees S1–S7).
 * One instance lives for the whole app so the first word plays quickly (SC-002).
 */
class AndroidTtsSpeechEngine(context: Context) : SpeechEngine {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _availability = MutableStateFlow<SpeechAvailability>(SpeechAvailability.Initializing)
    override val availability: StateFlow<SpeechAvailability> = _availability.asStateFlow()

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ready = CompletableDeferred<Boolean>()
    @Volatile private var engineReady = false
    @Volatile private var voiceCache: Map<EnglishVariant, List<Voice>> = emptyMap()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<SpeakResult>>()
    private var focusRequest: AudioFocusRequest? = null

    private val audioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit
        override fun onDone(utteranceId: String?) = finish(utteranceId, SpeakResult.Completed)

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = finish(utteranceId, SpeakResult.Failed)

        override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId, SpeakResult.Failed)

        override fun onStop(utteranceId: String?, interrupted: Boolean) =
            finish(utteranceId, SpeakResult.Interrupted)
    }

    /** Headphones unplugged (S7). */
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) stop()
        }
    }

    /** A call or another app took the audio (S7). Ducking for notifications does not stop playback. */
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stop()
    }

    init {
        ContextCompat.registerReceiver(
            appContext,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        mainHandler.post { createEngine() }
    }

    private fun createEngine() {
        val deferred = CompletableDeferred<Boolean>()
        ready = deferred
        engineReady = false
        _availability.value = SpeechAvailability.Initializing
        var engine: TextToSpeech? = null
        engine = TextToSpeech(appContext) { status ->
            val created = engine
            if (status == TextToSpeech.SUCCESS && created != null) {
                created.setOnUtteranceProgressListener(progressListener)
                created.setAudioAttributes(audioAttributes)
                evaluate(created)
                engineReady = true
                deferred.complete(true)
            } else {
                _availability.value = SpeechAvailability.EngineUnavailable
                deferred.complete(false)
            }
        }
        tts = engine
    }

    private fun evaluate(engine: TextToSpeech) {
        val cache = EnglishVariant.entries.associateWith { usableVoices(engine, it) }
        voiceCache = cache
        val readyVariants = EnglishVariant.entries.filter { variant ->
            cache[variant].orEmpty().isNotEmpty() || (engine.voices.isNullOrEmpty() && languageInstalled(engine, variant))
        }.toSet()
        _availability.value = if (readyVariants.isNotEmpty()) {
            SpeechAvailability.Ready(readyVariants)
        } else {
            SpeechAvailability.MissingVoices(EnglishVariant.entries.toSet())
        }
    }

    override suspend fun refresh() {
        withContext(Dispatchers.Main) {
            val engine = tts
            if (engine != null && engineReady) evaluate(engine)
            val current = _availability.value
            val allReady = current is SpeechAvailability.Ready && current.variants.size == EnglishVariant.entries.size
            if (!allReady && pending.isEmpty()) {
                // Voices installed meanwhile are often visible only to a fresh engine instance.
                interruptPending()
                tts?.shutdown()
                createEngine()
            }
        }
        ready.await()
    }

    override suspend fun availableVoices(variant: EnglishVariant): List<VoiceInfo> {
        if (!ready.await()) return emptyList()
        return voiceCache[variant].orEmpty().map { VoiceInfo(it.name, it.name, variant) }
    }

    override suspend fun speak(text: String, variant: EnglishVariant, voiceId: String?): SpeakResult {
        if (!ready.await()) return SpeakResult.Failed
        val engine = tts ?: return SpeakResult.Failed
        interruptPending()
        val voice = chooseVoice(engine, variant, voiceId)
        if (voice != null) engine.voice = voice else engine.language = locale(variant)

        val id = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<SpeakResult>()
        pending[id] = deferred
        requestFocus()
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) {
            pending.remove(id)
            abandonFocusIfIdle()
            return SpeakResult.Failed
        }
        return try {
            deferred.await()
        } catch (e: CancellationException) {
            if (pending.containsKey(id)) engine.stop()
            throw e
        } finally {
            pending.remove(id)
            abandonFocusIfIdle()
        }
    }

    override fun stop() {
        tts?.stop()
        interruptPending()
    }

    override fun voiceInstallAction(): VoiceInstallAction = VoiceInstallAction.OpenSystemScreen(INSTALL_TOKEN)

    override fun isOutputMuted(): Boolean = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0

    private fun finish(utteranceId: String?, result: SpeakResult) {
        if (utteranceId != null) pending[utteranceId]?.complete(result)
    }

    private fun interruptPending() {
        pending.values.forEach { it.complete(SpeakResult.Interrupted) }
    }

    private fun chooseVoice(engine: TextToSpeech, variant: EnglishVariant, voiceId: String?): Voice? {
        val usable = voiceCache[variant].orEmpty()
        return usable.firstOrNull { it.name == voiceId }
            ?: engine.defaultVoice?.let { default -> usable.firstOrNull { it.name == default.name } }
            ?: usable.firstOrNull()
    }

    private fun usableVoices(engine: TextToSpeech, variant: EnglishVariant): List<Voice> =
        try {
            engine.voices.orEmpty()
                .filter { voice ->
                    matches(voice.locale, variant) &&
                        !voice.isNetworkConnectionRequired &&
                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty()
                }
                .sortedBy { it.name }
        } catch (_: Exception) {
            emptyList()
        }

    private fun languageInstalled(engine: TextToSpeech, variant: EnglishVariant): Boolean {
        val result = engine.isLanguageAvailable(locale(variant))
        return result == TextToSpeech.LANG_COUNTRY_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }

    private fun matches(locale: Locale?, variant: EnglishVariant): Boolean {
        if (locale == null) return false
        val language = locale.language.lowercase(Locale.ROOT)
        val country = locale.country.uppercase(Locale.ROOT)
        if (language != "en" && language != "eng") return false
        return when (variant) {
            EnglishVariant.US -> country == "US" || country == "USA"
            EnglishVariant.GB -> country == "GB" || country == "GBR"
        }
    }

    private fun locale(variant: EnglishVariant): Locale = when (variant) {
        EnglishVariant.US -> Locale.US
        EnglishVariant.GB -> Locale.UK
    }

    private fun requestFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener(focusListener, mainHandler)
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonFocusIfIdle() {
        if (pending.isNotEmpty()) return
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    companion object {
        const val INSTALL_TOKEN = "tts_install"
    }
}
