package pl.aingielski.speech

import kotlinx.coroutines.flow.StateFlow
import pl.aingielski.domain.model.EnglishVariant

/**
 * Boundary between the app and any text-to-speech backend (contracts/speech-engine.md).
 * No Android types appear here, so another backend (e.g. LLM-based TTS) can be plugged in.
 */
interface SpeechEngine {
    /** Current readiness; re-evaluated on init, on settings change and on [refresh]. */
    val availability: StateFlow<SpeechAvailability>

    /** Re-checks engine and voice availability (e.g. after returning from system settings). */
    suspend fun refresh()

    /** Voices usable offline for the given variant, sorted by display name. */
    suspend fun availableVoices(variant: EnglishVariant): List<VoiceInfo>

    /**
     * Speaks [text] with [voiceId] (or the variant default when null), cancelling any utterance in
     * progress. Suspends until playback completes.
     */
    suspend fun speak(text: String, variant: EnglishVariant, voiceId: String?): SpeakResult

    /** Stops current playback immediately; the pending [speak] returns [SpeakResult.Interrupted]. */
    fun stop()

    /** Intent-free description of where the user can fix missing voices. */
    fun voiceInstallAction(): VoiceInstallAction

    /** True when the output stream used for speech is muted (volume 0). */
    fun isOutputMuted(): Boolean
}

data class VoiceInfo(val id: String, val displayName: String, val variant: EnglishVariant)

sealed interface SpeechAvailability {
    data object Initializing : SpeechAvailability
    data class Ready(val variants: Set<EnglishVariant>) : SpeechAvailability
    data class MissingVoices(val variants: Set<EnglishVariant>) : SpeechAvailability
    data object EngineUnavailable : SpeechAvailability
}

/** True when [variant] has at least one usable voice. */
fun SpeechAvailability.isReady(variant: EnglishVariant): Boolean =
    this is SpeechAvailability.Ready && variant in variants

enum class SpeakResult { Completed, Interrupted, Failed }

sealed interface VoiceInstallAction {
    /** Opaque token resolved by the platform layer (Activity) into an Intent. */
    data class OpenSystemScreen(val token: String) : VoiceInstallAction
    data object None : VoiceInstallAction
}
