# Implementation Plan: AIngielski – Core App

**Branch**: `001-aingielski-core` | **Date**: 2026-11-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-aingielski-core/spec.md`

## Summary

AIngielski is a native Android app for Polish speakers learning English. It is portrait-only, has
a single user and no accounts, and its interface is entirely in Polish. It delivers:

- **Listen-and-write quiz**: words and example sentences are spoken by text-to-speech, and the
  learner types what they heard.
- **CSV word sets**: imported from files and stored on the device.
- **Settings**: English variant, voice, and word skipping.
- **Statistics**: totals, a line chart, session details, and a protected clear action.

The app is built in Kotlin with Jetpack Compose and Material 3, using MVVM and a single Activity:

- **Persistence**: Room for word sets and sessions, which are kept independent of each other;
  DataStore for settings.
- **Speech**: Android text-to-speech behind a swappable `SpeechEngine` interface. The app detects
  missing offline voices and sends the user to the system's voice-download screen.
- **Learning logic**: answer checking, word selection, statistics and CSV parsing live in a pure
  Kotlin `domain` package covered by JVM unit tests.
- **Visuals**: all icons, including the US/UK flag launcher icon, are custom vector drawables
  colored by a custom indigo, coral and amber Material 3 scheme.

## Technical Context

**Language/Version**: Kotlin 2.x, latest stable, pinned in the version catalog. Bytecode targets
JVM 17. The build runs on the installed JDK 23, which requires Gradle 8.10 or newer (research R1).

**Primary Dependencies** (each justified per constitution rule II):

| Dependency | Why |
|---|---|
| Compose BOM: ui, foundation, material3 | UI toolkit mandated by the constitution (R2) |
| activity-compose | Hosts Compose in the single Activity, file picker, BackHandler |
| navigation-compose | Screen routing with back-stack control for the quiz (R12) |
| lifecycle-viewmodel-compose, lifecycle-runtime-compose | MVVM with lifecycle-aware state |
| room-runtime, room-ktx, room-compiler (KSP) | Database mandated by the constitution (R7) |
| datastore-preferences | Settings storage mandated by the constitution (R7) |
| kotlinx-coroutines-android | Asynchronous text-to-speech and database work |
| Test: junit 4, kotlinx-coroutines-test, androidx test (core, runner, ext-junit), room-testing, compose ui-test-junit4 | Tests required by rule IV (R13) |

Features deliberately built without a library: the chart (R10), the CSV parser (R9),
dependency injection (R11) and icons (R4).

**Storage**: Room/SQLite (`aingielski.db`) for word sets and sessions; DataStore Preferences for
settings. See [data-model.md](./data-model.md).

**Testing**: JUnit 4 JVM unit tests for the domain package and ViewModels, with a fake speech
engine and fake repositories. Instrumented tests for Room DAOs and for the critical Compose flows
(quiz, clear dialog).

**Target Platform**: Android 8.0+ (minSdk 26). compileSdk and targetSdk are 36, the level
required by Google Play. Orientation is `sensorPortrait`.

**Project Type**: mobile-app: a single native Android application module.

**Performance Goals**:

- Word playback starts within 1 second of being presented (SC-002).
- Importing a 500-line CSV takes under 5 seconds (SC-004).
- Scrolling and transitions run at 60 fps.
- Cold start takes under 2 seconds on a mid-range device.

**Constraints**:

- Fully offline: no `INTERNET` permission, and only offline voices are offered.
- No runtime permissions: files are read through the Storage Access Framework.
- Polish-only string resources.
- Layouts work at 200% font scale.

**Scale/Scope**:

- 1 user and 8 routes (7 screens plus the quiz summary).
- Word sets of roughly 10 to 2,000 entries.
- Up to a few thousand sessions, so every statistics query is a simple indexed SQL query.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Pre-research | Post-design |
|---|---|---|---|
| I. Native Android | Kotlin + Android SDK/Jetpack, no cross-platform | ✅ Kotlin, Compose, Room, DataStore | ✅ No cross-platform library; the TTS abstraction is Kotlin-only |
| II. Learner-First Simplicity | Every feature serves a learning goal; minimal dependencies; complexity justified | ✅ Scope = spec only | ✅ One module, manual DI, in-house chart and CSV parser; every dependency justified above |
| III. Offline-First & Privacy | Core flows offline, data local, minimal permissions | ✅ | ✅ No INTERNET permission, offline voices only (contract S3), no runtime permissions, all data on the device |
| IV. Testable Learning Logic | Logic separate from UI, unit tested | ✅ | ✅ `domain` package has no Android imports; DAO queries tested on a device |
| V. Accessible & Localizable UI | Strings in resources, font scaling, content descriptions, 48dp | ✅ | ✅ Codified in [contracts/ui-navigation.md](./contracts/ui-navigation.md) under Accessibility |
| Technical Constraints | Kotlin DSL, Compose single-activity, minSdk 26, Room + DataStore | ✅ | ✅ |
| Development Workflow | Build + unit tests pass before merge | ✅ | ✅ Commands in [quickstart.md](./quickstart.md) |

**Result**: PASS, with no violations. Complexity Tracking is not needed.

## Project Structure

### Documentation (this feature)

```text
specs/001-aingielski-core/
├── plan.md              # This file
├── research.md          # Phase 0: decisions R1–R13
├── data-model.md        # Phase 1: Room schema, states, derived queries, settings
├── quickstart.md        # Phase 1: build/test commands + validation scenarios V1–V8
├── contracts/
│   ├── speech-engine.md       # SpeechEngine interface + guarantees
│   ├── csv-import-format.md   # Import file format + validation messages
│   └── ui-navigation.md       # Routes, quiz phases, accessibility rules
├── checklists/requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks; not created here)
```

### Source Code (repository root)

```text
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/libs.versions.toml
gradle/wrapper/…
samples/                                   # Sample CSV files for manual validation (quickstart)
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml            # sensorPortrait, no permissions, label @string/app_name
    │   ├── java/pl/aingielski/
    │   │   ├── AIngielskiApp.kt           # Application: creates AppContainer, finalizes stale sessions
    │   │   ├── AppContainer.kt            # Manual DI: db, repositories, SpeechEngine, settings
    │   │   ├── MainActivity.kt            # Single activity, theme, NavHost, voice-install routing
    │   │   ├── domain/                    # Pure Kotlin – no android.* imports
    │   │   │   ├── model/                 # WordItem, QuizSetup, QuizState, QuizSummary, enums
    │   │   │   ├── text/Normalize.kt      # normalizeKey, AnswerChecker
    │   │   │   ├── csv/                   # CsvParser, WordSetImportValidator, ImportResult
    │   │   │   ├── quiz/                  # WordSelector, QuizSetupValidator, SummaryCalculator
    │   │   │   ├── stats/                 # StatsCalculator (totals, average, chart points)
    │   │   │   └── repository/            # WordSetRepository, SessionRepository, SettingsRepository interfaces
    │   │   ├── data/
    │   │   │   ├── db/                    # AppDatabase, entities, DAOs, converters
    │   │   │   ├── repository/            # Room/DataStore implementations
    │   │   │   └── settings/              # DataStore settings store
    │   │   ├── speech/
    │   │   │   ├── SpeechEngine.kt        # Interface + types (contract)
    │   │   │   └── android/AndroidTtsSpeechEngine.kt
    │   │   └── ui/
    │   │       ├── theme/                 # Color.kt (scheme + result colors), Theme.kt, Type.kt
    │   │       ├── navigation/AppNavHost.kt
    │   │       ├── components/            # Tile, ResultChip, PlayButton, VoiceMissingDialog, LineChart
    │   │       ├── home/                  # HomeScreen + drawer
    │   │       ├── settings/              # SettingsScreen, SettingsViewModel
    │   │       ├── words/                 # WordSetsScreen, WordSetDetailScreen, ImportDialog, ViewModels
    │   │       ├── quiz/                  # SetupScreen, QuizScreen, SummaryScreen, ViewModels
    │   │       └── stats/                 # StatsScreen, ClearDataDialog, StatsViewModel
    │   └── res/
    │       ├── values/strings.xml         # All Polish UI text
    │       ├── values/colors.xml, themes.xml
    │       ├── drawable/                  # ic_* vector icons (utility glyphs + tile illustrations)
    │       ├── drawable/ic_launcher_foreground.xml, ic_launcher_monochrome.xml
    │       └── mipmap-anydpi-v26/ic_launcher.xml, ic_launcher_round.xml
    ├── test/java/pl/aingielski/           # JVM unit tests: domain/*, ui/*ViewModelTest, FakeSpeechEngine
    └── androidTest/java/pl/aingielski/    # Room DAO tests, Compose UI tests (quiz flow, clear dialog)
```

**Structure decision**: one Android application module (`app`) with packages arranged by layer.
The `domain` package holds all learning logic and is kept free of Android imports so it can be
unit tested on the JVM (rule IV). The `speech` package isolates text-to-speech behind the
contract, so a future backend can replace it. There is no separate backend or API project; the
app is offline and single-user.

## Key design decisions (summary of research)

| Area | Decision | Ref |
|---|---|---|
| Speech | `SpeechEngine` interface; `speak()` suspends until playback finishes; flush on re-speak | R5, [contract](./contracts/speech-engine.md) |
| Missing voices | Detect `LANG_MISSING_DATA`, not-installed or network-only voices → Polish dialog → `ACTION_INSTALL_TTS_DATA`, then the TTS settings screen, then the main settings screen; re-check on resume | R6 |
| Quiz durability | Session and word rows written at start and updated per event; abort, stale sessions and process death all go through `finalizeInterrupted()` | R8 |
| History matching | Normalized `set_name_key` and `word_key` (Unicode lowercase); no foreign key from sessions to sets | R7 |
| CSV | In-house parser; auto `;`/`,` delimiter; no header; all errors collected; nothing saved on error | R9, [contract](./contracts/csv-import-format.md) |
| Colors | Seed indigo #2B4C9B, coral #D9434F, amber #F2A93B; light and dark themes; dynamic color off; semantic result colors | R3 |
| Icons | Hand-authored vector drawables; adaptive launcher icon (diagonal US/UK flag) with monochrome layer; two-tone tile icons | R4 |
| Chart | Custom Compose Canvas, 10 points, 2 series | R10 |
| Orientation/back | `sensorPortrait`; the quiz back action is intercepted and opens the abort dialog only when skipping is on | R12 |

## Complexity Tracking

No constitution violations, so nothing needs justifying.
