# Research: AIngielski – Core App

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-11-18

Each entry records the decision, why it was chosen, and the alternatives considered.

## R1. Build toolchain with JDK 23

- **Decision**: Gradle (Kotlin DSL) with a version catalog (`gradle/libs.versions.toml`). The
  Gradle daemon runs on the installed JDK 23, which needs Gradle 8.10 or newer. Kotlin and Java
  compile to JVM target 17, the target recommended for Android Gradle Plugin (AGP) 8.x. Use the
  latest stable Kotlin 2.x, AGP and Gradle available when implementation starts, and pin them in
  the catalog.
- **Rationale**: JDK 23 can run the build but is not used as the bytecode target, because Android
  desugaring and D8 support target 17. A version catalog keeps every dependency in one reviewed
  place, as constitution rule II requires.
- **Alternatives**: Groovy DSL (rejected: the constitution mandates Kotlin DSL). Targeting JVM 23
  (rejected: not supported by the Android toolchain).

## R2. UI framework and design system

- **Decision**: Jetpack Compose (Compose BOM) with Material 3, Navigation Compose, and
  single-activity architecture.
- **Rationale**: Compose is Google's standard toolkit for modern Android UI and is required by
  the constitution. Material 3 brings ready accessibility support (font scaling, 48dp touch
  targets) and a full color-role system for light and dark themes.
- **Alternatives**: XML Views (rejected: legacy, more boilerplate). Compose Multiplatform
  (rejected: cross-platform, banned by rule I).

## R3. Color scheme

- **Decision**: A custom Material 3 scheme generated from seed color **#2B4C9B**, an indigo that
  references the navy of both flags. Secondary is coral red **#D9434F** (the red of the flags),
  tertiary is warm amber **#F2A93B**. Light and dark variants are both defined. Dynamic (wallpaper)
  color is **disabled** so the brand colors and custom icons always match. Quiz result colors are
  separate semantic tokens, tuned per theme for at least 4.5:1 contrast:
  - correct = green **#2E7D32** (dark theme **#81C784**)
  - incorrect = red **#C62828** (dark theme **#E57373**)
  - skipped = orange **#EF6C00** (dark theme **#FFB74D**)

  Results are never shown by color alone: each also has an icon and a text label (rule V).
- **Rationale**: Blue tones suggest trust and focus, which suits learning. The red and amber
  accents add energy and tie in with the US/UK flag icon.
- **Alternatives**: Dynamic color (rejected: it would clash with the fixed-color flag icon and
  tile illustrations). Green-primary "edu" palette (rejected: green is reserved for "correct").

## R4. Icons

- **Decision**: Every icon is hand-authored as a vector in the repository: XML vector drawables
  for the launcher and utility icons, Kotlin `ImageVector`s for the tile illustrations. No icon
  library dependency and no PNG assets.
  - **Launcher**: an adaptive icon. The foreground is a rounded badge split diagonally: Union Jack
    (UK) on the upper-left half, Stars & Stripes (US) on the lower-right half, with a subtle white
    divider. The background is the primary indigo. A monochrome layer supports Android 13+
    themed icons.
  - **Tile "Odsłuchaj słówka"**: headphones with sound waves and a small letter "A" bubble.
  - **Tile "Statystyka"**: a rising line chart over bars with a trend dot.
  - **Tiles in general**: two-tone Kotlin `ImageVector`s whose colors are passed in from the
    current theme, because XML drawables can't follow the app's light and dark colors.
  - **Utility glyphs** (menu, play, replay, delete, back, skip, check, close, settings, words,
    chevron, warning, import): drawn in the 24dp Material Symbols "Rounded" style and tinted via
    `LocalContentColor` so they follow the theme.
- **Rationale**: The user supplies no icons and wants a consistent custom look. Recent Material 3
  releases no longer bundle the icons artifact, and `material-icons-extended` is a large
  dependency. Vector drawables scale to any density and adapt to the theme.
- **Alternatives**: `material-icons-extended` (rejected: size, and a generic look). PNG assets
  (rejected: density buckets, cannot be tinted).

## R5. Speech synthesis and its abstraction

- **Decision**: A Kotlin interface `SpeechEngine` (see
  [contracts/speech-engine.md](./contracts/speech-engine.md)) with suspend functions. `speak()`
  returns only after the utterance finishes or is stopped. It is implemented by
  `AndroidTtsSpeechEngine`, which wraps `android.speech.tts.TextToSpeech`:
  - **Utterance completion**: tracked with `UtteranceProgressListener`
    (`onDone`/`onStop`/`onError`) and bridged to coroutines. This lets the quiz reveal the input
    field only after the first playback finishes (FR-022).
  - **Interruption**: a new `speak()` call uses `QUEUE_FLUSH`, which cancels the playback in
    progress (edge case "playback during playback").
  - **Engine lifetime**: one engine instance for the whole app, created at startup and warmed up so
    the first word starts in under 1 second (SC-002).
  - **Voice selection**: list `tts.voices` and keep voices whose locale matches `en_US` or `en_GB`,
    whose `features` do not contain `KEY_FEATURE_NOT_INSTALLED`, and which do **not** require a
    network connection (offline-first, rule III). If the saved voice is missing, fall back to
    `tts.defaultVoice` when it matches the locale, otherwise the first matching voice.
- **Rationale**: The interface hides every Android TTS type, so a future LLM-based or cloud TTS
  can be plugged in by adding a new implementation to the app container, without touching the UI
  or the domain code.
- **Alternatives**: Calling `TextToSpeech` directly from ViewModels (rejected: not swappable or
  testable). A third-party TTS SDK (rejected: needs the network, violates rule III).

## R6. Missing voices: detecting them and sending the user to download

- **Decision**: An availability check returns one of four states:
  - **Ready**: at least one offline voice exists for the variant.
  - **MissingData**: `isLanguageAvailable(locale)` returns `LANG_MISSING_DATA`, or there are only
    not-installed or network voices.
  - **NotSupported**: the result is `LANG_NOT_SUPPORTED`.
  - **EngineUnavailable**: initialization failed or there is no TTS engine.

  The check runs on the Settings screen and before a quiz starts. When the state is not Ready, a
  Polish dialog explains the problem and offers "Pobierz głosy" (download voices), which launches
  these intents in order, using the first one that resolves:
  1. `TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA`, which opens the engine's own voice-download
     screen.
  2. `"com.android.settings.TTS_SETTINGS"`, the system Text-to-speech settings.
  3. `Settings.ACTION_SETTINGS`.

  When the app returns to the foreground (`ON_RESUME`), availability is checked again.
- **Rationale**: `ACTION_INSTALL_TTS_DATA` is the documented, engine-preferred route. The settings
  fallback covers manufacturer builds that lack it.
- **Alternatives**: Bundling voices (impossible). Opening the Play Store page of Google TTS
  (rejected: assumes a specific engine).

## R7. Persistence

- **Decision**: Room (KSP annotation processing) in the built-in SQLite database for word sets and
  sessions. DataStore Preferences for settings.
  - **Matching keys**: names and words are compared using a stored normalized key:
    `trim → collapse spaces → lowercase(Locale.ROOT)`. This avoids SQLite `NOCASE`, which folds
    ASCII only (it would fail on "Ś" vs "ś").
  - **Session independence**: session tables have **no foreign key** to word sets (FR-057,
    FR-072).
- **Rationale**: This is the constitution's mandated stack. Normalized keys make the
  case-insensitive uniqueness and history matching correct for Polish characters too.
- **Alternatives**: Raw SQLite (rejected: boilerplate). SharedPreferences (rejected: DataStore is
  the current standard).

## R8. Saving quiz results as they happen (FR-028a)

- **Decision**: The session row and all its word rows are inserted at quiz start: the session with
  status `IN_PROGRESS`, the words with result `PENDING` and `played = false`. Each answer or
  skip updates its word row immediately (result, typed answer, `played = true`). Finishing the quiz sets status `COMPLETED`.
  Aborting runs the same routine as recovering a quiz cut off by the system, `finalizeInterrupted`:
  - If the quiz has any `CORRECT` or `INCORRECT` row, it changes `PENDING` rows to `SKIPPED`
    with `played = false` (they stay "new", including a heard-but-unanswered word) and sets status
    `ABORTED`.
  - Otherwise it deletes the session.

  At app start, any session still `IN_PROGRESS` is finalized the same way. If Android recreates
  the quiz screen without its in-memory state (after process death), the screen navigates to Home
  instead of resuming. Statistics only ever read `COMPLETED` and `ABORTED` sessions.
- **Rationale**: Gives the "save like an abort" behaviour chosen in clarification without a resume
  flow.
- **Alternatives**: Keep the quiz in memory and write it at the end (rejected: loses data when the
  app is killed). Full resume (rejected in clarification).

## R9. CSV parsing

- **Decision**: A small in-house parser in the pure-Kotlin domain layer:
  - **Encoding**: UTF-8; a leading BOM is stripped.
  - **Delimiter**: semicolon if the first non-blank line contains `;` outside quotes, otherwise
    comma.
  - **Quoting**: RFC 4180 double quotes (`""` escapes a quote).
  - **Newlines**: none are allowed inside fields. Each physical line is one record.
  - **Lines**: blank lines are skipped. Line numbers are 1-based physical line numbers.
  - **Errors**: all errors are collected and reported together.

  The file is chosen with the Storage Access Framework (`OpenDocument`) for MIME types
  `text/csv`, `text/comma-separated-values`, `text/plain`, `application/vnd.ms-excel` and
  `application/csv`, plus `*/*` as a fallback (some file managers mislabel CSV). No storage
  permission is needed.
- **Rationale**: About 100 lines of easily tested code. It gives exact, Polish-reportable line
  errors, which a general CSV library would make harder.
- **Alternatives**: OpenCSV or kotlin-csv (rejected: an extra dependency, and harder to report
  per-line field errors in the format the spec requires).

## R10. Line chart

- **Decision**: A custom Compose `Canvas` chart. It shows at most 10 points with 2 series
  (correct, incorrect) and has these parts:
  - **Y axis**: integer ticks.
  - **X axis**: labels `dd.MM HH:mm`, rotated if they don't fit.
  - **Legend**: identifies the two lines.
  - **Accessibility**: a content description that summarizes the values.
- **Rationale**: The chart is small and fixed, so a charting library (such as Vico) is not
  justified under rule II.
- **Alternatives**: Vico or MPAndroidChart (rejected: dependency weight for one simple chart).

## R11. Dependency injection and architecture

- **Decision**:
  - **Dependency injection**: manual. An `AppContainer` created in `AIngielskiApp.onCreate()`
    holds the database, repositories, `SpeechEngine` and settings store, and ViewModels get them
    through `viewModelFactory { }` initializers.
  - **Pattern**: MVVM with unidirectional data flow. Each ViewModel exposes one
    `StateFlow<UiState>`.
  - **Domain logic**: kept in a `domain` package with no Android imports: word selection, answer
    checking, statistics, the CSV parser and import validation.
- **Rationale**: With 7 screens, a DI framework (Hilt) is not needed (YAGNI). A pure domain layer
  is what makes rule IV testable on the JVM.
- **Alternatives**: Hilt or Koin (rejected: unnecessary for this size). Multi-module (rejected:
  one module is enough).

## R12. Orientation and system back

- **Decision**:
  - **Orientation**: `android:screenOrientation="sensorPortrait"`, which allows portrait and
    upside-down portrait, never landscape.
  - **Back during a quiz**: `BackHandler(enabled = true)` swallows the back action. If skipping is
    enabled it opens the abort confirmation; otherwise it does nothing (FR-027). The quiz route is
    reached with `popUpTo(Setup)` so the back stack cannot rewind into earlier words.
- **Rationale**: Matches the user's request ("rotation only in vertical if phone is upside down")
  and the spec rules.
- **Alternatives**: `portrait` (rejected: blocks upside-down).

## R13. Testing

- **Decision**:
  - **JVM unit tests** (JUnit 4, kotlinx-coroutines-test) for every domain class and the
    ViewModels, using a `FakeSpeechEngine` and in-memory fake repositories.
  - **Instrumented tests** (AndroidX Test with an in-memory Room database) for DAOs and queries:
    new words, difficult words, statistics, and the import-override transaction.
  - **Compose UI tests** for the critical quiz flow and the "Wyczyść dane" verification dialog.
- **Rationale**: Constitution rule IV mandates unit tests for learning logic. The DAO queries hold
  the "new" and "difficult" rules, so they need real-SQLite tests.
- **Alternatives**: Robolectric for DAOs (acceptable later; on-device tests are more faithful).
