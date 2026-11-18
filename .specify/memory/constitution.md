# Aingielski Constitution

## Core Principles

### I. Native Android

The app MUST be a native Android application written in Kotlin using the official Android SDK
and Jetpack libraries. Cross-platform frameworks (Flutter, React Native, web views as the main
UI, etc.) MUST NOT be used.

Rationale: a single, well-supported platform stack keeps the project small and maintainable.

### II. Learner-First Simplicity

Every feature MUST serve a concrete English-learning goal (e.g. vocabulary, grammar, listening,
reading, progress tracking). Build the smallest version that delivers that goal; speculative
features, abstractions, and dependencies MUST NOT be added (YAGNI). Any added complexity MUST
be justified in the feature plan.

Rationale: a minimal app ships sooner and is easier to evolve based on real learner needs.

### III. Offline-First & Privacy

Core learning flows (viewing lessons, doing exercises, saving progress) MUST work without a
network connection, with learner progress stored locally on the device. No personal data or
learning history MAY leave the device without explicit user consent. Only permissions strictly
required by a feature MAY be requested.

Rationale: learners study anywhere, and trust depends on respecting their data.

### IV. Testable Learning Logic

Learning logic (exercise evaluation, scoring, progress and repetition scheduling) MUST be kept
separate from UI code and MUST be covered by automated unit tests. A change that breaks
existing tests MUST NOT be merged.

Rationale: incorrect answer checking or lost progress directly harms learning.

### V. Accessible & Localizable UI

All user-facing text MUST live in Android string resources (no hard-coded UI strings). Screens
MUST support system font scaling, provide content descriptions for meaningful images and icons,
and meet minimum 48dp touch targets.

Rationale: learners have diverse needs, and the app's interface language may need to change.

## Technical Constraints

- Language: Kotlin; build system: Gradle (Kotlin DSL) via Android Studio.
- UI: Jetpack Compose with a single-activity architecture.
- Minimum SDK: 26 (Android 8.0); target SDK: the latest level required by Google Play.
- Local persistence: Room (structured data) and DataStore (preferences).
- New third-party dependencies MUST be justified in the feature plan.

## Development Workflow

- Work follows the Spec Kit flow: specify → plan → tasks → implement.
- Each feature plan MUST include a Constitution Check against the principles above.
- Before merge, the project MUST build successfully and all unit tests MUST pass.

## Governance

This constitution supersedes other project practices. Amendments are made by updating this
file via `/speckit-constitution`, with a Sync Impact Report describing the change.

Versioning follows semantic versioning: MAJOR for removed or redefined principles, MINOR for
new principles or sections or materially expanded guidance, PATCH for clarifications and
wording fixes.

Every feature plan and code review MUST verify compliance with these principles; deviations
MUST be documented with a justification in the plan's complexity tracking.

**Version**: 1.0.0 | **Ratified**: 2026-11-18 | **Last Amended**: 2026-11-18
