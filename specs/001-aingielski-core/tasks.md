---

description: "Task list for AIngielski – Core App"
---

# Tasks: AIngielski – Core App

**Input**: Design documents from `specs/001-aingielski-core/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (speech-engine.md,
csv-import-format.md, ui-navigation.md), quickstart.md

**Tests**: Included. Constitution principle IV makes unit tests mandatory for learning logic
(answer checking, word selection, scoring, statistics, CSV validation). Plan R13 adds DAO tests
and Compose UI tests for the critical flows. Test tasks come before the implementation tasks
they cover and should fail first.

**Organization**: Tasks are grouped by user story (US1–US6 from spec.md) so each story can be
built and verified on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on unfinished tasks)
- **[Story]**: User story label (US1–US6). Setup, Foundational and Polish tasks have none.

## Path Conventions

This is a single Android module (plan.md › Project Structure). The aliases below are used in
task descriptions:

| Alias | Path |
|---|---|
| `{main}` | `app/src/main/java/pl/aingielski` |
| `{res}` | `app/src/main/res` |
| `{test}` | `app/src/test/java/pl/aingielski` |
| `{atest}` | `app/src/androidTest/java/pl/aingielski` |

All UI text goes into `{res}/values/strings.xml` in Polish (constitution V). Each task that adds
screens also adds its strings there.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Gradle project that builds an empty app on JDK 23.

- [ ] T001 Create the Gradle wrapper (Gradle ≥ 8.10, so it runs on JDK 23 per research R1), `settings.gradle.kts` (rootProject "AIngielski", include `:app`, google/mavenCentral repositories), root `build.gradle.kts` (plugins declared with `apply false`) and `gradle.properties` (`android.useAndroidX=true`, `kotlin.code.style=official`, JVM args) at the repository root
- [ ] T002 Create `gradle/libs.versions.toml`. Pin the latest stable versions of AGP, Kotlin 2.x, the KSP plugin, the Kotlin Compose compiler plugin, and every library listed in plan.md › Technical Context › Primary Dependencies (including test libraries).
- [ ] T003 Create `app/build.gradle.kts` with these settings:
  - namespace and applicationId `pl.aingielski`; minSdk 26; compileSdk and targetSdk 36
  - Java and Kotlin target 17
  - Compose enabled and KSP applied
  - Room schema export to `app/schemas` via `room.schemaLocation`
  - `testInstrumentationRunner` set to `androidx.test.runner.AndroidJUnitRunner`
  - all dependencies taken from the catalog
- [ ] T004 [P] Create `app/src/main/AndroidManifest.xml` with:
  - an `application` element with label `@string/app_name` only. The theme, icon and application-class attributes are added later by T011, T014 and T025, so that T008 can build without them.
  - a single `MainActivity` with `android:screenOrientation="sensorPortrait"` and `windowSoftInputMode="adjustResize"`
  - **no** `<uses-permission>` entries (plan Constraints, R12)
  - a `<queries>` entry for the `android.speech.tts.engine.INSTALL_TTS_DATA` and `com.android.settings.TTS_SETTINGS` intents (R6)
- [ ] T005 [P] Create the root `.gitignore` for Android/Gradle. It ignores `.gradle/`, `build/`, `local.properties`, `.idea/` (except shared run configurations), `*.iml`, `captures/` and `.kotlin/`. Merge with any existing content rather than overwrite it.
- [ ] T006 [P] Create `{res}/values/strings.xml` with `app_name` = `AIngielski` (exact casing, FR-002) and the shared strings: navigation labels "Ustawienia", "Słówka", "Odsłuchaj słówka", "Statystyka"; generic "Wstecz", "Anuluj", "Zapisz", "OK"
- [ ] T007 [P] Create the Android lint config `app/lint.xml` (missing translations and hard-coded text treated as errors) and `.editorconfig` (Kotlin official style) at the repository root
- [ ] T008 Run `.\gradlew.bat assembleDebug` with a placeholder `{main}/MainActivity.kt` that shows an empty Compose `Text`. This proves the toolchain works on JDK 23.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Theme, icons, shell navigation, database schema, settings store, speech engine and
DI. All stories depend on these.

**⚠️ CRITICAL**: No user story work can start until this phase is complete.

### Theme & icons

- [ ] T009 [P] Create `{main}/ui/theme/Color.kt`. Build the light and dark Material 3 `ColorScheme`s from seed #2B4C9B, with secondary #D9434F and tertiary #F2A93B (tonal roles generated with Material Theme Builder). Add a `ResultColors` holder (correct, incorrect, skipped) with the light and dark values from research R3, exposed through `LocalResultColors`.
- [ ] T010 [P] Create `{main}/ui/theme/Type.kt`, a Material 3 `Typography` using sp units only, so system font scaling is respected.
- [ ] T011 Create `{main}/ui/theme/Theme.kt` as `AIngielskiTheme`. It picks light or dark from `isSystemInDarkTheme()`, has dynamic color **disabled** (R3), and provides `LocalResultColors`. Also create `{res}/values/themes.xml` (`Theme.AIngielski`, NoActionBar, edge-to-edge) and `{res}/values-night/themes.xml`. Add `android:theme="@style/Theme.AIngielski"` to `<application>` in `app/src/main/AndroidManifest.xml`.
- [ ] T012 [P] Hand-author 24dp utility vector icons in the Material Symbols "Rounded" style (R4). Each uses `?attr/colorControlNormal`-compatible tinting and its own file under `{res}/drawable/`: `ic_menu.xml`, `ic_play.xml`, `ic_replay.xml`, `ic_sentence.xml`, `ic_delete.xml`, `ic_arrow_back.xml`, `ic_skip.xml`, `ic_check_circle.xml`, `ic_cancel.xml`, `ic_close.xml`, `ic_settings.xml`, `ic_words.xml`, `ic_chevron_down.xml`, `ic_warning.xml`, `ic_file_import.xml`, `ic_stop.xml`
- [ ] T013 [P] Create the two-tone 64dp tile illustrations as Kotlin `ImageVector` builders in `{main}/ui/icons/TileIcons.kt` (R4):
  - `listenTileIcon(primary: Color, accent: Color)`: headphones, sound waves and an "A" speech bubble
  - `statsTileIcon(primary: Color, accent: Color)`: a rising line over bars, with a trend dot
  - Wrap each in a `@Composable` accessor that takes `MaterialTheme.colorScheme.primary` and `MaterialTheme.colorScheme.secondary` and is cached with `remember(colors)`, so the icons follow light and dark mode.
  - Add a `@Preview` showing both icons in light and dark mode.
- [ ] T014 [P] Create the adaptive launcher icon (FR-003, R4):
  - `{res}/drawable/ic_launcher_foreground.xml`: a rounded badge split diagonally, Union Jack top-left and Stars & Stripes bottom-right, with a white divider, inside the 66dp safe zone
  - `{res}/drawable/ic_launcher_monochrome.xml`: a single-color version for themed icons
  - `{res}/values/ic_launcher_background.xml`: color #2B4C9B
  - `{res}/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`, each with background, foreground and monochrome layers
  - add `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` to `<application>` in `app/src/main/AndroidManifest.xml`

### Domain core

- [ ] T015 [P] Create the domain enums and value types in `{main}/domain/model/Enums.kt`: `EnglishVariant {US, GB}`, `SelectionCriterion {RANDOM, NEW_ONLY, DIFFICULT}`, `WordResult {PENDING, CORRECT, INCORRECT, SKIPPED}` and `SessionStatus {IN_PROGRESS, COMPLETED, ABORTED}`. These follow data-model.md `quiz_session.criterion`, `session_word.result` and `quiz_session.status`. Also create `WordItem(word, translation, sentence)` in `{main}/domain/model/WordItem.kt`. No Android imports anywhere in `domain/`.
- [ ] T016 [P] Write the unit test `{test}/domain/text/NormalizeTest.kt` for `normalizeKey`. Cases: trimming, collapsing inner whitespace, lowercasing with `Locale.ROOT` including Polish letters ("ŚLIMAK" → "ślimak"), and punctuation preserved ("Don't" → "don't").
- [ ] T017 Implement `normalizeKey()` in `{main}/domain/text/Normalize.kt` per data-model.md › Normalization rule, so that T016 passes.

### Database schema

- [ ] T018 [P] Create the Room entities in `{main}/data/db/entity/` (`WordSetEntity.kt`, `WordEntryEntity.kt`, `QuizSessionEntity.kt`, `SessionWordEntity.kt`), matching data-model.md exactly:
  - `word_set.id`, `word_set.name`, `word_set.name_key` (unique index), `word_set.imported_at`
  - `word_entry.id`, `word_entry.set_id` (FK to `word_set.id`, ON DELETE CASCADE, indexed), `word_entry.position`, `word_entry.word`, `word_entry.word_key` (unique together with `set_id`), `word_entry.translation`, `word_entry.sentence`
  - `quiz_session.id`, `quiz_session.set_name`, `quiz_session.set_name_key` (indexed, **no FK**), `quiz_session.started_at` (indexed), `quiz_session.criterion`, `quiz_session.word_count`, `quiz_session.status`
  - `session_word.id`, `session_word.session_id` (FK to `quiz_session.id`, ON DELETE CASCADE, indexed), `session_word.position`, `session_word.word`, `session_word.word_key` (indexed; unique together with `session_id`), `session_word.translation`, `session_word.sentence`, `session_word.result`, `session_word.typed_answer` (nullable), `session_word.played`
- [ ] T019 Create `{main}/data/db/Converters.kt` (enum ↔ String for the T015 enums) and `{main}/data/db/AppDatabase.kt`. The database is `@Database` version 1 named `aingielski.db`, has `exportSchema = true`, and lists the four entities. Leave DAO accessors as abstract functions to be added by the stories (empty `WordSetDao` and `SessionDao` interfaces in `{main}/data/db/dao/` for now).

### Settings store

- [ ] T020 [P] Create `{main}/domain/repository/SettingsRepository.kt`, an interface with `settings: Flow<AppSettings>` and `suspend fun save(AppSettings)`. Create `AppSettings(variant, voiceId, skipEnabled)` in `{main}/domain/model/AppSettings.kt`, using the defaults from data-model.md Settings `english_variant`, `voice_id` and `skip_enabled`.
- [ ] T021 Implement `{main}/data/settings/DataStoreSettingsRepository.kt` using DataStore Preferences in the file `settings`. Use the keys and defaults exactly as in data-model.md Settings `english_variant`, `voice_id` and `skip_enabled`. An unknown or missing value falls back to the default.

### Speech engine (contract)

- [ ] T022 [P] Create `{main}/speech/SpeechEngine.kt` with the interface and types **exactly** as in contracts/speech-engine.md: `SpeechEngine`, `VoiceInfo`, `SpeechAvailability`, `SpeakResult` and `VoiceInstallAction`. Reuse `EnglishVariant` from the domain. No Android imports.
- [ ] T023 [P] Create `{test}/speech/FakeSpeechEngine.kt`, a test double. Its voices and availability can be configured. `speak()` either completes immediately or suspends until a test calls `completeCurrent()`. It records spoken texts and supports `stop()` returning `Interrupted`, satisfying guarantees S1–S2 for tests.
- [ ] T024 Implement `{main}/speech/android/AndroidTtsSpeechEngine.kt`, wrapping `TextToSpeech` per research R5–R6 and guarantees S1–S6:
  - a single instance, initialized and warmed up at creation
  - `speak()` bridges `UtteranceProgressListener` to coroutines using `QUEUE_FLUSH`
  - an earlier, still-pending call returns `Interrupted`
  - `availableVoices()` filters by `en_US`/`en_GB`, excludes voices with `KEY_FEATURE_NOT_INSTALLED` and voices that need the network
  - `availability` is computed from `isLanguageAvailable` per variant
  - falls back to the default voice when the given id is unknown
  - `voiceInstallAction()` returns `OpenSystemScreen("tts_install")`

### App shell & DI

- [ ] T025 Create `{main}/AppContainer.kt` (manual DI per R11). It lazily holds `AppDatabase`, `SettingsRepository` and `SpeechEngine`, and has placeholders that the stories fill with their repositories. Create `{main}/AIngielskiApp.kt` (an `Application` that creates the `AppContainer` in `onCreate`). Add a `{main}/ui/AppViewModelProvider.kt` helper to build ViewModels via `viewModelFactory { initializer { … } }`. Add `android:name=".AIngielskiApp"` to `<application>` in `app/src/main/AndroidManifest.xml`.
- [ ] T026 Create `{main}/ui/navigation/AppNavHost.kt` with the routes from contracts/ui-navigation.md (`home`, `settings`, `words`, `wordSet/{id}`, `setup`, `quiz/{sessionId}`, `summary/{sessionId}`, `stats`). Point each non-home route at a placeholder composable for now.
- [ ] T027 Create `{main}/ui/home/HomeScreen.kt` (FR-005, FR-006, contract › Home):
  - a top app bar titled "AIngielski" with a burger icon (`ic_menu`) that opens a `ModalNavigationDrawer`
  - drawer items "Ustawienia" (`ic_settings`) and "Słówka" (`ic_words`)
  - a 2-column grid of `Tile` cards: "Odsłuchaj słówka" (`listenTileIcon`, T013) → `setup`, and "Statystyka" (`statsTileIcon`, T013) → `stats`
  - a reusable `{main}/ui/components/Tile.kt` (whole card clickable, at least 48dp target, merged semantics)
- [ ] T028 Create `{main}/ui/components/VoiceMissingDialog.kt`, a Polish explanation dialog with a "Pobierz głosy" button. Update `{main}/MainActivity.kt` (from the T008 placeholder) to:
  - host `AIngielskiTheme` and `AppNavHost`
  - resolve `VoiceInstallAction.OpenSystemScreen` by trying `ACTION_INSTALL_TTS_DATA`, then `com.android.settings.TTS_SETTINGS`, then `Settings.ACTION_SETTINGS` (R6), using the first that resolves
  - call `speechEngine.refresh()` on `ON_RESUME`
  - pass an `onOpenVoiceInstall: () -> Unit` callback from `MainActivity` into `AppNavHost`, which hands it to `SetupScreen` and `SettingsScreen`. ViewModels only expose that voices are missing; they never hold an Activity reference or launch intents.

**Checkpoint**: The app launches with the launcher icon, the drawer and two tiles leading to
placeholder screens. The theme works in light and dark mode. `testDebugUnitTest` is green.

---

## Phase 3: User Story 1 – Import and browse word sets (Priority: P1) 🎯 MVP

**Goal**: Import CSV word sets (auto `;`/`,`, no header, at least 10 entries, no duplicates,
all-or-nothing), name them, overwrite with confirmation, browse them as a table, and delete them
without touching statistics.

**Independent Test**: quickstart.md › V2.

### Tests for User Story 1 ⚠️ (write first, must fail)

- [ ] T029 [P] [US1] Write the unit tests `{test}/domain/csv/CsvParserTest.kt`. Cases:
  - delimiter detection: `;` when the first non-blank line has it outside quotes, otherwise `,`
  - RFC 4180 quoting, including `""` and a quoted delimiter
  - BOM stripping and `\r\n` line endings
  - blank lines skipped while physical line numbers are kept
  - an unclosed quote and more than 3 fields reported per line
- [ ] T030 [P] [US1] Write the unit tests `{test}/domain/csv/WordSetImportValidatorTest.kt`, one per row of the contracts/csv-import-format.md › Validation result table:
  - missing or blank field, with field names in the message
  - too many fields
  - duplicate word, reporting both line numbers
  - fewer than 10 entries
  - empty file
  - all errors collected together
  - a valid file returns `Success` with trimmed entries in file order
  - `colour/color` kept literally as a single answer (FR-025)
- [ ] T031 [P] [US1] Write the unit tests `{test}/domain/csv/SetNameValidatorTest.kt` for the data-model.md `word_set.name` rules: trimmed, not blank, at most 50 characters. Duplicates are detected via `word_set.name_key` (case-insensitive, Polish letters included).
- [ ] T032 [P] [US1] Write the instrumented tests `{atest}/data/db/WordSetDaoTest.kt` on in-memory Room. Cases:
  - insert a set with entries; list summaries with word counts
  - entries come back in `word_entry.position` order
  - the unique `word_set.name_key` is enforced
  - `replaceSet` replaces all entries in one transaction
  - deleting a set cascades to `word_entry` and leaves `quiz_session`/`session_word` rows untouched (FR-057)

### Implementation for User Story 1

- [ ] T033 [P] [US1] Implement `{main}/domain/csv/CsvParser.kt` per contracts/csv-import-format.md › Format and research R9, so that T029 passes. It is pure Kotlin and returns raw records with their physical line numbers, or per-line parse errors.
- [ ] T034 [US1] Implement `{main}/domain/csv/WordSetImportValidator.kt` and `{main}/domain/csv/ImportResult.kt` (`Success`, `Invalid(List<LineError>)`, `TooFew`, `Empty`, `Unreadable`, per data-model.md › Domain models), so that T030 passes. Rules:
  - non-blank `word_entry.word`, `word_entry.translation` and `word_entry.sentence`
  - unique `word_entry.word_key` within the file
  - a set needs at least 10 entries (data-model.md › Set-level rules)
  - `word_entry.position` taken from file order
- [ ] T035 [P] [US1] Implement `{main}/domain/csv/SetNameValidator.kt` per data-model.md `word_set.name` and `word_set.name_key`, so that T031 passes. It also provides the default name from the file name minus `.csv` (FR-052).
- [ ] T036 [US1] Implement `{main}/data/db/dao/WordSetDao.kt`:
  - `observeSummaries()` (id, name, count, ordered by name)
  - `findByNameKey(word_set.name_key)`
  - `entries(setId)` ordered by `word_entry.position`
  - `@Transaction insertSet(set, entries)` and `@Transaction replaceSet(nameKey, set, entries)`, which deletes the old set and inserts the new one with a new `word_set.imported_at`
  - `deleteSet(id)`

  Register the DAO in `AppDatabase`. T032 must pass.
- [ ] T037 [US1] Create the interface `{main}/domain/repository/WordSetRepository.kt` and its implementation `{main}/data/repository/RoomWordSetRepository.kt`. Methods: list summaries (`WordSetSummary` from data-model.md › Domain models), get entries, `existsByName`, `importSet(name, entries, overwrite)` and `delete`. Map domain entries to `word_entry.word_key` via `normalizeKey`. Wire it into `AppContainer`.
- [ ] T038 [US1] Implement `{main}/data/csv/CsvFileReader.kt`. It reads a SAF `Uri` through `ContentResolver` as UTF-8 text on `Dispatchers.IO`. Unreadable or binary content gives `ImportResult.Unreadable`, and it also returns the display name for the default set name.
- [ ] T039 [US1] Implement `{main}/ui/words/WordSetsViewModel.kt` with one `StateFlow<WordSetsUiState>`:
  - the set list
  - the import flow: file picked → read and validate → name prompt (pre-filled) → when the name exists, overwrite confirmation → save, or show errors
  - delete with confirmation
  - "Anuluj" on any import step aborts with no changes (FR-054, FR-055)
- [ ] T040 [US1] Implement `{main}/ui/words/WordSetsScreen.kt`:
  - a list of sets showing name and word count, each with a trash icon button (`ic_delete`, contentDescription "Usuń zestaw {name}") and a delete confirmation dialog
  - an "Importuj" extended FAB (`ic_file_import`) that launches `ActivityResultContracts.OpenDocument` with the MIME types from research R9
  - an empty state
  - a back arrow to `home`
- [ ] T041 [P] [US1] Implement the import dialogs in `{main}/ui/words/ImportDialogs.kt`:
  - `SetNameDialog`: text field, validation errors, "Importuj" disabled while the name is invalid
  - `OverwriteDialog`: `Zestaw „{name}” już istnieje. Zastąpić go?`, with "Zastąp" and "Anuluj"
  - `ImportErrorDialog`: a scrollable list of Polish line errors, using the message formats in contracts/csv-import-format.md
- [ ] T042 [P] [US1] Implement `{main}/ui/words/WordSetDetailViewModel.kt` and `{main}/ui/words/WordSetDetailScreen.kt` (FR-051). The screen has a top bar with the set name and only a back action to `words`. Its body is a table with header "Słowo | Tłumaczenie | Zdanie przykładowe" in a `LazyColumn`; text wraps, and the table stays readable at 200% font scale.
- [ ] T043 [US1] Wire `words` and `wordSet/{id}` into `{main}/ui/navigation/AppNavHost.kt` and the "Słówka" drawer item in `HomeScreen`. Add all US1 strings to `{res}/values/strings.xml`.
- [ ] T044 [P] [US1] Add the sample files from quickstart.md › Prerequisites under `samples/`: `zwierzeta_sredniki.csv` (25 entries, `;`), `podstawy_przecinki.csv` (12 entries, `,`, quoted sentences with commas), `bledny_wiersz7.csv`, `za_maly.csv` (8 entries) and `duplikat.csv`.

**Checkpoint**: quickstart V2 passes. Sets can be imported, overwritten, viewed and deleted.

---

## Phase 4: User Story 2 – Listen-and-write quiz (Priority: P1)

**Goal**: Quiz setup (set, count 10 to set size, random selection), the per-word flow (auto-play
→ input → check → reveal → next), skip and abort, saving results as they happen,
recovery after the app is closed, and the summary.

**Independent Test**: quickstart.md › V3 and V4.

**Depends on**: Foundational. A word set is needed at runtime, which US1 provides (or tests can
seed one).

### Tests for User Story 2 ⚠️ (write first, must fail)

- [ ] T045 [P] [US2] Write the unit tests `{test}/domain/text/AnswerCheckerTest.kt` (FR-025, FR-025a). Cases:
  - ignores case and leading/trailing spaces, and collapses inner spaces
  - apostrophes, hyphens and `/` must match exactly
  - "Apple" vs "apple" is correct; "dont" vs "don't" is incorrect
- [ ] T046 [P] [US2] Write the unit tests `{test}/domain/quiz/QuizSetupValidatorTest.kt` (FR-011, FR-012). Cases: range 10..setSize; empty or non-numeric input; `TooLow(10)` and `TooHigh(max)`; a set of exactly 10 allows only 10; the default value of 10 is valid for every saved set (all have at least 10 words, FR-054a).
- [ ] T047 [P] [US2] Write the unit tests `{test}/domain/quiz/WordSelectorTest.kt` (FR-016, FR-017). With a seeded `Random`:
  - returns exactly `count` distinct words
  - takes preferred keys first and fills from the rest
  - shuffles the final order
  - an empty preferred set gives a purely random selection
- [ ] T048 [P] [US2] Write the unit tests `{test}/domain/quiz/SummaryCalculatorTest.kt` (FR-030). Cases: counts; percent = round(100 × correct / word_count) with half-up rounding (7 of 12 → 58); aborted sessions count auto-skipped words in the denominator.
- [ ] T049 [P] [US2] Write the instrumented tests `{atest}/data/db/SessionDaoQuizTest.kt`. Cases:
  - creating a session inserts the `quiz_session` row (`quiz_session.status` = IN_PROGRESS) and N `session_word` rows (`session_word.result` = PENDING, `session_word.played` = false)
  - recording an answer updates `session_word.result`, `session_word.typed_answer` and `session_word.played`
  - `finalizeInterrupted` with at least one answered word turns PENDING into SKIPPED, sets `played` = false, and sets the status to ABORTED
  - `finalizeInterrupted` with no answered word deletes the session (cascade)
  - completing a quiz sets COMPLETED
  - calling `finalizeInterrupted` on a session that is already ABORTED, COMPLETED or deleted changes nothing
- [ ] T050 [P] [US2] Write the unit tests `{test}/ui/quiz/QuizViewModelTest.kt` with `FakeSpeechEngine` and fake repositories. Cases:
  - phases Playing → Answering only after the first `speak()` completes (FR-022)
  - skip is offered only in Answering when skipping is enabled (FR-024)
  - submit is blocked for blank input
  - Revealed shows correctness, the correct word and the translation (FR-026)
  - replay and sentence playback stay available in Answering and Revealed
  - each result is saved immediately (FR-028a)
  - abort, with or without answered words (FR-028)
  - back is ignored when skipping is disabled (FR-027)
  - "Dalej" on the last word leads to the summary

### Implementation for User Story 2

- [ ] T051 [P] [US2] Implement `{main}/domain/text/AnswerChecker.kt` using `normalizeKey` on both sides, so that T045 passes.
- [ ] T052 [P] [US2] Implement `{main}/domain/quiz/QuizSetupValidator.kt`, returning `CountError.TooLow`/`TooHigh`/`NotANumber` (data-model.md › Domain models `QuizSetup`), so that T046 passes.
- [ ] T053 [P] [US2] Implement `{main}/domain/quiz/WordSelector.kt` (`selectWords(entries, preferredKeys, count, random)`, per data-model.md › Word selection), so that T047 passes.
- [ ] T054 [P] [US2] Implement `{main}/domain/quiz/SummaryCalculator.kt` and `{main}/domain/model/QuizSummary.kt` (`QuizSummary`, `SummaryItem` with word, translation, result and typed answer), so that T048 passes.
- [ ] T055 [US2] Implement `{main}/data/db/dao/SessionDao.kt` with the quiz operations and register it in `AppDatabase`:
  - `@Transaction createSession(session, words)`: writes `quiz_session.set_name`, `quiz_session.set_name_key`, `quiz_session.started_at`, `quiz_session.criterion`, `quiz_session.word_count`, `quiz_session.status`, and `session_word.position`, `session_word.word`, `session_word.word_key`, `session_word.translation`, `session_word.sentence` copied from the set
  - `recordResult(sessionId, position, result, typedAnswer)`: sets `session_word.result`, `session_word.typed_answer` and `session_word.played` = true
  - `sessionWords(sessionId)`
  - `markCompleted(sessionId)`
  - `@Transaction finalizeInterrupted(sessionId)`, per data-model.md › Session states. It does nothing unless the session exists and its `quiz_session.status` is IN_PROGRESS, so startup recovery (T057) and quiz-screen recovery (T060) can both call it safely.
  - `staleInProgressIds()`

  T049 must pass.
- [ ] T056 [US2] Create the interface `{main}/domain/repository/SessionRepository.kt` and its implementation `{main}/data/repository/RoomSessionRepository.kt`, with the quiz methods `startSession`, `recordResult`, `complete`, `finalizeInterrupted`, `finalizeAllStale` and `summary(sessionId)`. Wire it into `AppContainer`.
- [ ] T057 [US2] In `{main}/AIngielskiApp.kt`, call `sessionRepository.finalizeAllStale()` on startup on an application coroutine scope, before any statistics read (FR-028a, R8).
- [ ] T058 [US2] Implement `{main}/ui/quiz/SetupViewModel.kt`:
  - the word-set list with counts
  - the selected set and the allowed range text "10–{max}"
  - count input pre-filled with 10 (FR-011), kept when the selected set changes and re-validated against the new range, with validation errors in Polish
  - speech availability: when not `Ready`, show `VoiceMissingDialog` and block starting
  - start: choose words via `WordSelector` (no preferred keys yet; RANDOM), then `startSession`, then navigate to `quiz/{sessionId}`
- [ ] T059 [US2] Implement `{main}/ui/quiz/SetupScreen.kt`:
  - a set list as selectable cards showing name and word count
  - a numeric count field (initially "10") with the range hint and inline error
  - a "Rozpocznij" button, disabled while invalid
  - an empty state with a link to "Słówka" when no sets exist
- [ ] T060 [US2] Implement `{main}/ui/quiz/QuizViewModel.kt`, a state machine following contracts/ui-navigation.md › Quiz screen phases:
  - **Settings snapshot**: read once at quiz start.
  - **Each word**: auto `speak(word)`. Playing → Answering only when the result is `Completed`. If the result is `Failed`, still move to Answering and show a retry message.
  - **Playback buttons**: `replayWord()` and `playSentence()`.
  - **Answers**: `submit(text)` uses `AnswerChecker` and saves via `recordResult`. `skip()` records SKIPPED and reveals.
  - **Navigation**: `next()`; on the last word, `complete()` then emit navigation to the summary.
  - **Abort**: `abort()` → `finalizeInterrupted`, then navigate home.
  - **Recovery**: if created without in-memory state (process death), finalize the session and navigate home (R8).
- [ ] T061 [P] [US2] Implement the components:
  - `{main}/ui/components/PlayButton.kt`: icon plus caption, `ic_replay` for "Odtwórz ponownie" and `ic_sentence` for "Odtwórz zdanie"
  - `{main}/ui/components/ResultBadge.kt`: icon, text ("Dobrze!" or "Źle") and result color from `LocalResultColors`, so meaning never depends on color alone
  - `{main}/ui/components/NoSuggestionsTextField.kt`: a plain-text answer field (the typed text is always visible; nothing is masked). It is a Compose `AndroidView` wrapping `EditText` with:
    - `inputType = TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | TYPE_TEXT_FLAG_NO_SUGGESTIONS`. The "visible password" variation is only a hint that makes Gboard and Samsung Keyboard turn off suggestions and autocorrect (FR-022a); the field is not a password and must never show dots. Add a code comment saying so.
    - `importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS` and `imeOptions = IME_ACTION_DONE`
    - the value and change callback wired to the screen state, an `enabled` parameter, an `onDone` callback, and Material 3 styling (outline, label, colors) from the theme
    - a Polish `contentDescription` of "Wpisz usłyszane słowo" (type the word you heard)
- [ ] T062 [US2] Implement `{main}/ui/quiz/QuizScreen.kt`:
  - progress "n / N" with a `LinearProgressIndicator`
  - a "Słuchaj…" indicator while Playing; no word, translation or sentence shown before Revealed
  - the answer input as `NoSuggestionsTextField` (T061), a clear-text field that turns off autocorrect, suggestions and auto-capitalization (FR-022a). The keyboard's Done action submits and does nothing while the field is blank.
  - "Sprawdź"
  - "Pomiń" and "Przerwij quiz" only when skipping is enabled
  - Revealed: `ResultBadge`, the correct word when wrong or skipped, the translation, and "Dalej" (or "Zakończ" on the last word)
  - an abort confirmation dialog
  - `BackHandler` that opens the abort dialog when skipping is enabled and otherwise consumes back (FR-027)
- [ ] T063 [US2] Implement `{main}/ui/quiz/SummaryViewModel.kt` and `{main}/ui/quiz/SummaryScreen.kt` (FR-030–FR-032, contract › Summary):
  - a header with the percentage and three count chips (icon + color + label)
  - a `LazyColumn` of words in quiz order with result color and icon, the word and its translation, and "Twoja odpowiedź: …" on incorrect rows
  - buttons "Wróć do ekranu głównego" (to `home`) and "Nowy quiz" (to `setup`)
- [ ] T064 [US2] Wire `setup`, `quiz/{sessionId}` (navigated with `popUpTo("setup") { inclusive = true }`) and `summary/{sessionId}` (with `popUpTo("home")`) in `{main}/ui/navigation/AppNavHost.kt`. Connect the "Odsłuchaj słówka" tile. Add all US2 strings to `{res}/values/strings.xml`.
- [ ] T065 [P] [US2] Write the Compose UI test `{atest}/ui/quiz/QuizFlowTest.kt` with an in-memory database, `FakeSpeechEngine` and a seeded 10-word set. It answers correctly, answers incorrectly and skips, checks that the word is hidden before reveal, and checks the summary counts and percentage.

**Checkpoint**: quickstart V3 and V4 pass. The MVP (US1 + US2) is usable end to end.

---

## Phase 5: User Story 3 – Targeted word selection: new & difficult (Priority: P2)

**Goal**: "Tylko nowe" and "Trudne" criteria, matched by set name and English word, with a random
top-up when too few words qualify.

**Independent Test**: quickstart.md › V5.

### Tests for User Story 3 ⚠️

- [ ] T066 [P] [US3] Write the unit tests `{test}/domain/quiz/DifficultyRuleTest.kt` (FR-015). Cases: 2 incorrect and 1 correct → difficult (67%); 1 and 1 → not difficult (50%); skips ignored; no answers → not difficult; the threshold is compared as `incorrect * 100 >= 51 * (correct + incorrect)` to avoid floating-point errors.
- [ ] T067 [P] [US3] Write the instrumented tests `{atest}/data/db/SessionDaoHistoryTest.kt`. Cases:
  - `playedKeys(setNameKey)` returns only `session_word.word_key` where `session_word.played` = true, from sessions whose `quiz_session.status` is COMPLETED or ABORTED and whose `quiz_session.set_name_key` matches
  - IN_PROGRESS sessions are excluded
  - `answerCounts(setNameKey)` groups by `word_key`, counting CORRECT and INCORRECT

### Implementation for User Story 3

- [ ] T068 [P] [US3] Implement `{main}/domain/quiz/DifficultyRule.kt`, which turns `(wordKey, correct, incorrect)` counts into a set of difficult keys, so that T066 passes.
- [ ] T069 [US3] Add `playedKeys(setNameKey)` and `answerCounts(setNameKey)` queries to `{main}/data/db/dao/SessionDao.kt`, per data-model.md › Derived queries, so that T067 passes. Expose them as `SessionRepository.newWordKeys(setName, entries)` and `difficultWordKeys(setName)` in `{main}/data/repository/RoomSessionRepository.kt`.
- [ ] T070 [US3] Extend `{main}/ui/quiz/SetupViewModel.kt` with the selected `SelectionCriterion` (default RANDOM). On start it computes the preferred keys (new or difficult) and passes them to `WordSelector`, stores `quiz_session.criterion` and shows the count of matching words for the selected set.
- [ ] T071 [US3] Add a single-choice criterion group ("Losowe", "Tylko nowe", "Trudne"; RadioButtons with `selectableGroup` semantics) to `{main}/ui/quiz/SetupScreen.kt`. Each option shows a hint with the count of matching words, and when fewer match than requested, a note that the rest will be random. Add the strings.

**Checkpoint**: quickstart V5 passes, and US1 and US2 still pass.

---

## Phase 6: User Story 4 – Settings (Priority: P2)

**Goal**: English variant, voice (offline, installed, with preview) and skipping toggle, with
Zapisz/Anuluj and a voice-download route when voices are missing.

**Independent Test**: quickstart.md › V6.

### Tests for User Story 4 ⚠️

- [ ] T072 [P] [US4] Write the unit tests `{test}/ui/settings/SettingsViewModelTest.kt` with `FakeSpeechEngine` and a fake `SettingsRepository`. Cases:
  - loads saved values
  - changing the variant reloads voices and resets a voice that doesn't match to null (FR-041)
  - Zapisz persists `english_variant`, `voice_id` and `skip_enabled` (data-model.md Settings)
  - Anuluj discards changes
  - `MissingVoices` exposes the install action

### Implementation for User Story 4

- [ ] T073 [US4] Implement `{main}/ui/settings/SettingsViewModel.kt`:
  - draft state copied from `SettingsRepository`
  - the voice list from `SpeechEngine.availableVoices(variant)`
  - `preview()` speaks the sample word "Hello" (FR-042)
  - save and cancel
  - follows `SpeechEngine.availability`
- [ ] T074 [US4] Implement `{main}/ui/settings/SettingsScreen.kt`:
  - a variant `SingleChoiceSegmentedButtonRow` ("Amerykański", "Brytyjski")
  - a voice selection list with names, the selected one marked, and a "Odsłuchaj próbkę" button
  - a "Pomijanie słówek" `Switch` row
  - a missing-voices card with "Pobierz głosy" that triggers `VoiceInstallAction`
  - bottom buttons "Anuluj" and "Zapisz"
  - system back behaves like Anuluj
- [ ] T075 [US4] Wire the `settings` route in `{main}/ui/navigation/AppNavHost.kt` and the "Ustawienia" drawer item. Add the strings.

**Checkpoint**: quickstart V6 passes. The quiz uses the saved variant, voice and skipping setting.

---

## Phase 7: User Story 5 – Statistics overview (Priority: P2)

**Goal**: Global totals, the average of per-session percentages, a line chart of the last 10
sessions, and a foldable list of session details. All of it survives word-set deletion.

**Independent Test**: quickstart.md › V7, steps 1–3.

### Tests for User Story 5 ⚠️

- [ ] T076 [P] [US5] Write the unit tests `{test}/domain/stats/StatsCalculatorTest.kt` (FR-060, FR-061). Cases:
  - totals summed across sessions
  - average = mean of per-session percentages (10-word quiz at 100% and 50-word quiz at 50% → 75%)
  - 0 sessions → no average
  - chart = the 10 newest sessions in chronological order with labels `dd.MM HH:mm`
  - aborted sessions included
  - the average is computed from unrounded per-session values and displayed rounded half-up (FR-060)
  - a 20-session fixture (SC-006) mixing completed and aborted sessions, with totals, average and chart points checked against hand-computed values
- [ ] T077 [P] [US5] Write the instrumented tests `{atest}/data/db/SessionDaoStatsTest.kt`. `sessionSummaries()` returns one row per COMPLETED or ABORTED session (`quiz_session.status`), with `quiz_session.set_name`, `quiz_session.started_at`, `quiz_session.word_count` and counts by `session_word.result`, newest first. IN_PROGRESS sessions are excluded. Results are unchanged after the matching `word_set` is deleted.

### Implementation for User Story 5

- [ ] T078 [P] [US5] Implement `{main}/domain/stats/StatsCalculator.kt` and `{main}/domain/model/Stats.kt` (`SessionStat`, `GlobalStats`, `ChartPoint`), so that T076 passes.
- [ ] T079 [US5] Add a `sessionSummaries()` Flow query to `{main}/data/db/dao/SessionDao.kt` and `observeSessionStats()` to `SessionRepository`/`RoomSessionRepository`, so that T077 passes.
- [ ] T080 [P] [US5] Implement `{main}/ui/components/LineChart.kt` as a Compose `Canvas` chart (R10):
  - two series with the result colors for correct and incorrect
  - integer Y ticks, and X labels that rotate when crowded
  - a legend
  - a merged `contentDescription` summarizing the values
  - a "Brak danych" empty state
- [ ] T081 [US5] Implement `{main}/ui/stats/StatsViewModel.kt` and `{main}/ui/stats/StatsScreen.kt`, scrollable, with:
  - a totals card: sessions, words (correct, incorrect, skipped) and average %, shown as "—" when there are no sessions
  - a "Ostatnie 10 sesji" chart
  - a "Szczegóły sesji" expandable section, collapsed by default (`ic_chevron_down` rotates), with rows of set name, date and time, and correct/incorrect/skipped
  - a back arrow
- [ ] T082 [US5] Wire the `stats` route in `{main}/ui/navigation/AppNavHost.kt` and the "Statystyka" tile. Add the strings.

**Checkpoint**: quickstart V7 steps 1–3 pass.

---

## Phase 8: User Story 6 – Clear statistics with verification (Priority: P3)

**Goal**: Delete all sessions only after a warning and after the user types the exact total word
count. Word sets are untouched.

**Independent Test**: quickstart.md › V7, steps 4–5.

### Tests for User Story 6 ⚠️

- [ ] T083 [P] [US6] Write the unit tests `{test}/ui/stats/ClearDataTest.kt` against `StatsViewModel`. A wrong number shows an error and deletes nothing. The correct total (the sum of correct, incorrect and skipped) calls `clearAll`. Non-numeric input is rejected.
- [ ] T084 [P] [US6] Write the instrumented test `{atest}/data/db/SessionDaoClearTest.kt`. `deleteAllSessions()` removes all `quiz_session` rows and cascades `session_word`, while `word_set` and `word_entry` rows remain.

### Implementation for User Story 6

- [ ] T085 [US6] Add `deleteAllSessions()` to `{main}/data/db/dao/SessionDao.kt` and `clearAll()` to `SessionRepository`/`RoomSessionRepository`, so that T084 passes.
- [ ] T086 [US6] Implement `{main}/ui/stats/ClearDataDialog.kt` and the clear flow in `StatsViewModel`:
  - a `ic_warning` icon and the Polish text "Wszystkie statystyki zostaną trwale usunięte."
  - a numeric field "Wpisz łączną liczbę słówek"
  - an inline error on mismatch
  - buttons "Usuń wszystko" (error color) and "Anuluj"
  - a destructive "Wyczyść dane" button on `StatsScreen`, hidden when there is no data

  T083 must pass.
- [ ] T087 [P] [US6] Write the Compose UI test `{atest}/ui/stats/ClearDataDialogTest.kt`, covering the wrong-count and correct-count paths.

**Checkpoint**: All six stories work independently and together.

---

## Phase 9: Polish & Cross-Cutting Concerns

- [ ] T088 [P] Accessibility pass across `{main}/ui/**` (constitution V, contract › Accessibility):
  - every icon button has a Polish `contentDescription`; decorative icons have `null`
  - touch targets are at least 48dp
  - every screen scrolls at 200% font scale
  - TalkBack does not announce the hidden quiz word during Playing or Answering
  - the quiz answer field (`NoSuggestionsTextField`) shows typed text in clear, shows no suggestions on Gboard and Samsung Keyboard, triggers no autofill "save password" prompt, and is announced by TalkBack as a normal editable field that reads back the typed text
- [ ] T089 [P] Visual consistency check: every utility `ic_*` drawable is recolored by the theme when shown, the tile icons in `{main}/ui/icons/TileIcons.kt` match the R3 palette in light and dark mode, and the launcher icon matches R3 (adjust `{res}/drawable/*` or `TileIcons.kt` where needed).
- [ ] T090 [P] Make sure `{res}/values/strings.xml` holds every UI string, with no hard-coded text in Kotlin (`.\gradlew.bat lint` reports no `HardcodedText` or `SetTextI18n`). Use plurals where counts appear.
- [ ] T091 Performance check for SC-002 and SC-004. Measure the delay before the first word plays with the warm engine from `AppContainer` startup (target: under 1 s), and the import time for a generated 500-line CSV (target: under 5 s). Also measure cold start (plan target: under 2 s on a mid-range device) and check that lists and screen transitions show no visible jank (plan target: 60 fps; inspect with the Profile GPU Rendering overlay). Record the results in `specs/001-aingielski-core/quickstart.md` › V8 notes.
- [ ] T092 [P] Configure the release build in `app/build.gradle.kts` (R8 `isMinifyEnabled = true`, `app/proguard-rules.pro` keeping the Room-generated classes if needed) and verify that `.\gradlew.bat assembleRelease` succeeds.
- [ ] T093 Run `.\gradlew.bat testDebugUnitTest lint` and `connectedDebugAndroidTest`, then work through quickstart.md › V1–V8 on a device or emulator, offline included. Include a timed SC-001 check: from the home screen with one imported set, start a 10-word quiz with the default settings in 5 taps or fewer and under 30 s. Fix any deviations.
- [ ] T094 [P] Remove the Sync Impact Report HTML comment from `.specify/memory/constitution.md` before the first commit (it is temporary review material).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies. T008 needs T001–T004 and T006, and must not depend on any Phase 2 resource.
- **Foundational (Phase 2)**: depends on Setup and **blocks all stories**.
- **US1 (Phase 3)** and **US2 (Phase 4)**: both depend only on Foundational. At runtime US2 needs
  a word set (from US1, or seeded in tests).
- **US3 (Phase 5)**: depends on US2 (it extends `SetupViewModel`/`SetupScreen` and
  `SessionDao`).
- **US4 (Phase 6)**: depends on Foundational only (settings store and speech engine already
  exist).
- **US5 (Phase 7)**: depends on US2's `SessionDao` and `SessionRepository` (T055, T056).
- **US6 (Phase 8)**: depends on US5 (`StatsScreen`, `StatsViewModel`).
- **Polish (Phase 9)**: depends on all the stories that are being delivered.

```text
Setup ─▶ Foundational ─┬─▶ US1 ──────────────┐
                       ├─▶ US2 ─┬─▶ US3      ├─▶ Polish
                       │        └─▶ US5 ─▶ US6
                       └─▶ US4 ──────────────┘
```

### Within each story

Tests come first and fail. Then the domain logic (pure Kotlin), then the DAO and repository,
then the ViewModel, then the screen, then navigation wiring and strings.

### Shared-file sequencing (not [P])

- `AppDatabase.kt`: T019 → T036 → T055
- `SessionDao.kt`: T055 → T069 → T079 → T085
- `SetupViewModel.kt`/`SetupScreen.kt`: T058/T059 → T070/T071
- `AppNavHost.kt`: T026 → T043 → T064 → T075 → T082
- `StatsViewModel.kt`: T081 → T086

---

## Parallel Examples

### Phase 2 (after T011)

```text
T012 utility icons  |  T013 tile illustrations  |  T014 launcher icon
T015 domain enums   |  T016 NormalizeTest       |  T018 Room entities
T020 SettingsRepository interface  |  T022 SpeechEngine interface  |  T023 FakeSpeechEngine
```

### User Story 1

```text
T029 CsvParserTest | T030 WordSetImportValidatorTest | T031 SetNameValidatorTest | T032 WordSetDaoTest
then: T033 CsvParser | T035 SetNameValidator | T041 ImportDialogs | T042 WordSetDetail | T044 samples
```

### User Story 2

```text
T045 AnswerCheckerTest | T046 QuizSetupValidatorTest | T047 WordSelectorTest | T048 SummaryCalculatorTest | T049 SessionDaoQuizTest | T050 QuizViewModelTest
then: T051 AnswerChecker | T052 QuizSetupValidator | T053 WordSelector | T054 SummaryCalculator | T061 PlayButton/ResultBadge
```

### Across stories (after Foundational)

```text
Developer A: US1 (T029–T044)    Developer B: US2 (T045–T065)    Developer C: US4 (T072–T075)
```

---

## Implementation Strategy

### MVP first (US1 + US2)

1. Phase 1 Setup, then Phase 2 Foundational.
2. Phase 3 (US1): import a real CSV. **Validate with V2.**
3. Phase 4 (US2): run a quiz on that set. **Validate with V3 and V4.**
4. Stop here for a demo: this is the smallest version that teaches something.

### Incremental delivery

5. US4 Settings, so you can pick the British voice and turn skipping off (V6).
6. US3 Targeted selection (V5).
7. US5 Statistics (V7, steps 1–3), then US6 Clear (V7, steps 4–5).
8. Polish (Phase 9), then the full quickstart run.

### Merge gate (constitution › Development Workflow)

After each task group, `.\gradlew.bat assembleDebug testDebugUnitTest` must pass.

---

## Notes

- `[P]` means a different file with no dependency on unfinished tasks.
- Every constrained field in data-model.md is cited in at least one task: T018 (all
  fields), plus T021, T031, T034–T037, T049, T055, T067, T069, T070, T072, T077 and T084.
- Open lower-risk review items in `checklists/quiz.md` (audio interruptions, muted volume,
  choosing among more difficult words than requested) are handled by the defaults in T060, T053
  and T070. Revisit them if the reviewer decides otherwise.
- Commit after each task or logical group, and stop at any checkpoint to validate.
