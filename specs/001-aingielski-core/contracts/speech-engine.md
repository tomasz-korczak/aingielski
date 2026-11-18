# Contract: SpeechEngine

This is the boundary between the app and any text-to-speech backend (research R5 and R6). The UI
and the domain code depend **only** on this interface. Android types (`TextToSpeech`, `Voice`,
`Locale`) MUST NOT appear in it.

## Interface (Kotlin signature, normative)

```kotlin
interface SpeechEngine {
    /** Emits the current readiness; re-evaluated on init, on settings change and on refresh(). */
    val availability: StateFlow<SpeechAvailability>

    /** Re-checks engine and voice availability (e.g. after returning from system settings). */
    suspend fun refresh()

    /** Voices usable offline for the given variant, sorted by display name. */
    suspend fun availableVoices(variant: EnglishVariant): List<VoiceInfo>

    /**
     * Speaks [text] with [voiceId] (or the variant default when null).
     * Cancels any utterance in progress (flush). Suspends until playback completes.
     * Returns Completed, Interrupted (stopped by a newer speak()/stop()) or Failed.
     */
    suspend fun speak(text: String, variant: EnglishVariant, voiceId: String?): SpeakResult

    /** Stops current playback immediately; the pending speak() returns Interrupted. */
    fun stop()

    /** Intent-free description of where the user can fix missing voices. */
    fun voiceInstallAction(): VoiceInstallAction

    /** True when the output stream used for speech is muted (volume 0). */
    fun isOutputMuted(): Boolean
}

enum class EnglishVariant { US, GB }

data class VoiceInfo(val id: String, val displayName: String, val variant: EnglishVariant)

sealed interface SpeechAvailability {
    data object Initializing : SpeechAvailability
    data class Ready(val variants: Set<EnglishVariant>) : SpeechAvailability
    data class MissingVoices(val variants: Set<EnglishVariant>) : SpeechAvailability
    data object EngineUnavailable : SpeechAvailability
}

enum class SpeakResult { Completed, Interrupted, Failed }

sealed interface VoiceInstallAction {
    /** Opaque token resolved by the platform layer (Activity) into an Intent. */
    data class OpenSystemScreen(val token: String) : VoiceInstallAction
    data object None : VoiceInstallAction
}
```

## Behavioural guarantees

| # | Guarantee |
|---|---|
| S1 | `speak()` never returns `Completed` before the audio has finished playing. The quiz relies on this to show the answer field (FR-022). |
| S2 | A call to `speak()` while another call is in progress makes the earlier call return `Interrupted` and starts the new text. |
| S3 | `availableVoices()` returns only voices that are installed **and** do not need a network connection (rule III). |
| S4 | When `voiceId` is unknown or not installed, `speak()` uses the default voice for the variant instead of failing. |
| S5 | `availability` is never `Ready` for a variant that has no usable voice. |
| S6 | Implementations are safe to call from the main thread; blocking work happens internally. |
| S7 | When audio output is interrupted (audio focus lost to a call or another app, or headphones unplugged), playback stops and the pending `speak()` returns `Interrupted`. The engine never resumes or replays by itself (FR-023a). |

## Implementations

| Implementation | Where | Purpose |
|---|---|---|
| `AndroidTtsSpeechEngine` | `app/src/main/.../speech/android/` | Production engine, wraps `android.speech.tts.TextToSpeech` |
| `FakeSpeechEngine` | `app/src/test/.../speech/` | Unit tests: configurable voices and availability; `speak()` completes immediately or on demand |

A future backend (for example LLM or cloud TTS) adds one class and changes one line in
`AppContainer`. Nothing else changes.

## Voice-install routing (platform layer)

The Activity resolves `OpenSystemScreen` by trying these intents in order and launching the first
one that resolves:

1. `TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA`
2. `"com.android.settings.TTS_SETTINGS"`
3. `Settings.ACTION_SETTINGS`

When the Activity resumes, it calls `refresh()`.
