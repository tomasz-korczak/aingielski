# Feature Specification: AIngielski – Core App (Listen & Write, Word Sets, Statistics)

**Feature Branch**: `001-aingielski-core` (no git repository yet; directory name only)

**Created**: 2026-11-18

**Status**: Draft

**Input**: User description: "Native Android app for learning English, Polish UI, title 'AIngielski'.
Main screen with burger menu (Ustawienia, Słówka) and tiles ('Odsłuchaj słówka', 'Statystyka').
Listen-and-write quiz with setup (word set, word count 10..set size, criteria: random / new /
difficult), per-word playback of word and example sentence, answer check, skip/abort rules,
summary. Settings (English variant, voice, word skipping). Word management (CSV import with
validation, override prompt, view, delete). Statistics (global totals, last-10-sessions line chart,
foldable session details, protected clear). Word sets and session stats persisted; deleting a word
set never affects session stats." (full original description retained in conversation history)

## Clarifications

### Session 2026-11-18

- Q: Which CSV format should import accept? → A: Delimiter auto-detected (semicolon or comma),
  no header row; every non-blank line is a word entry.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Import and browse word sets (Priority: P1)

The learner prepares a CSV file with English words, Polish translations and English example
sentences, imports it into the app under a chosen name, browses its content, and can delete sets
they no longer need.

**Why this priority**: Without word sets the app has nothing to teach; every other feature
depends on at least one imported set.

**Independent Test**: Import a valid CSV file, confirm it appears in "Słówka" with its word count,
open it to see the table, delete it. Import an invalid file and confirm the import is rejected
with the offending line number.

**Acceptance Scenarios**:

1. **Given** the "Słówka" screen, **When** the learner picks a valid CSV file and enters a new
   unique name, **Then** the set is saved and listed with its name and word count.
2. **Given** a CSV file where line 7 lacks the example sentence, **When** the learner imports it,
   **Then** the import is fully aborted, nothing is saved, and an error states that line 7 is
   invalid and which field is missing.
3. **Given** an existing set "Zwierzęta", **When** the learner imports a file under the name
   "Zwierzęta", **Then** the app asks whether to overwrite it; confirming replaces the whole set
   content, declining aborts the import and leaves the existing set unchanged.
4. **Given** the list of sets, **When** the learner taps a set, **Then** a new screen shows a table
   (word, translation, example sentence) with only a back action returning to "Słówka".
5. **Given** the list of sets, **When** the learner taps the trash icon of a set and confirms,
   **Then** the set is removed from the list and all previously recorded session statistics remain
   unchanged.

---

### User Story 2 - Listen-and-write quiz (Priority: P1)

The learner opens "Odsłuchaj słówka", chooses a word set and a word count, and goes through the
quiz: each word is spoken, the learner types what they heard, sees whether it was correct together
with the Polish translation, and moves on. At the end they see a summary of the session.

**Why this priority**: This is the core learning activity and the main reason the app exists.

**Independent Test**: With one imported set of at least 10 words, run a 10-word quiz with default
(random) criteria, answer some correctly, some incorrectly, skip one, and verify the per-word
feedback, the progress counter, and the final summary (counts, percentage, colored word list).

**Acceptance Scenarios**:

1. **Given** the quiz setup, **When** the learner opens it, **Then** every available word set is
   listed with its word count and the allowed word-count range (10 – set size) is displayed for the
   selected set.
2. **Given** a selected set of 25 words, **When** the learner enters 8 or 30 as the word count,
   **Then** a clear error explains the allowed range (10–25) and the quiz cannot be started.
3. **Given** a started quiz of 12 words, **When** the first word is presented, **Then** the screen
   shows "1 / 12", the word is spoken automatically, and the word, its translation and its example
   sentence are not visible.
4. **Given** the first playback has finished, **When** the input field appears, **Then** the
   learner can type the answer, replay the word any number of times ("Odtwórz ponownie") and play
   the example sentence any number of times ("Odtwórz zdanie").
5. **Given** the played word is "Apple", **When** the learner submits "apple", **Then** the answer
   is marked correct (case-insensitive) and the Polish translation is shown.
6. **Given** an answer was submitted, **When** the feedback is shown, **Then** a "Dalej" (next)
   button appears, the skip button is hidden, and both play buttons remain available.
7. **Given** word skipping is enabled in settings, **When** a word is presented, **Then** a "Pomiń"
   (skip) button is visible; pressing it records the word as skipped and reveals the correct
   word and translation before moving on.
8. **Given** all words have been answered or skipped, **When** the last word is completed,
   **Then** a summary shows correct / incorrect / skipped counts, the success percentage for this
   quiz, and every quiz word colored green (correct), red (incorrect) or orange (skipped), with
   "Wróć do ekranu głównego" and "Nowy quiz" actions.
9. **Given** a quiz is in progress, **When** the learner uses the system back gesture/button,
   **Then** the quiz does not navigate back to any previous word or screen.
10. **Given** skipping is enabled and the learner has answered 3 of 12 words, **When** they abort
   the quiz and confirm, **Then** the session is saved with the 3 results and the remaining 9 words
   recorded as skipped, and the learner returns to the main screen.
11. **Given** skipping is enabled and no word has been answered yet, **When** the learner aborts,
   **Then** no session is saved.

---

### User Story 3 - Targeted word selection: new and difficult words (Priority: P2)

In the quiz setup the learner can choose "only new words" (never played before for this set name)
or "difficult words" (words with an error ratio of at least 51% in previous sessions of this set
name) instead of the default random selection.

**Why this priority**: Adds learning efficiency on top of the core quiz but is not required for a
usable first version.

**Independent Test**: Run a quiz on a set, then start a new quiz with "only new words" and verify
none of the previously played words are chosen while enough new words exist; get some words wrong
and verify "difficult words" selects them first.

**Acceptance Scenarios**:

1. **Given** the quiz setup, **When** it opens, **Then** "Losowe" (random) is selected by default
   and exactly one criterion can be selected at a time.
2. **Given** a set where 4 words were never played and the learner requests 10 new words,
   **When** the quiz starts, **Then** it contains those 4 new words plus 6 other words chosen at
   random from the set, with no duplicates.
3. **Given** word "cat" was answered incorrectly 2 times and correctly 1 time in sessions of the
   same set name, **When** "difficult words" is chosen, **Then** "cat" (error ratio 67%) is eligible;
   a word with 1 correct and 1 incorrect answer (50%) is not.

---

### User Story 4 - Settings (Priority: P2)

The learner chooses the English pronunciation variant (American / British), the voice used for
speech, and whether skipping words is allowed, then saves or cancels.

**Why this priority**: Personalizes listening, but sensible defaults let the quiz work without it.

**Independent Test**: Change all three settings, save, restart the app and verify they persist;
change them again and cancel, verifying nothing changed.

**Acceptance Scenarios**:

1. **Given** the settings screen, **When** the learner selects "Brytyjski", picks a voice from the
   list of voices available on the device for that variant, disables skipping, and taps "Zapisz",
   **Then** the settings are stored, survive app restarts, and the main screen is shown.
2. **Given** changed but unsaved settings, **When** the learner taps "Anuluj", **Then** the
   previously saved settings remain and the main screen is shown.
3. **Given** no voice for the selected variant is installed on the device, **When** the settings
   screen is shown, **Then** the app explains this in Polish and offers to open the device's
   speech settings.

---

### User Story 5 - Statistics overview (Priority: P2)

The learner opens "Statystyka" to see overall progress, a chart of recent sessions and the details
of every past session.

**Why this priority**: Motivates the learner and makes progress visible; not required to learn.

**Independent Test**: After completing several quizzes, open the screen and verify totals,
average, chart points and the per-session list against the sessions actually played.

**Acceptance Scenarios**:

1. **Given** past sessions exist, **When** the screen opens, **Then** it shows the total session
   count, total word count with the split into correct / incorrect / skipped, and the average
   success percentage across all sessions.
2. **Given** 14 sessions exist, **When** the chart is shown, **Then** it displays the 10 most recent
   sessions in chronological order, labelled by date and hour, with one line for correct and one for
   incorrect answers.
3. **Given** the foldable "session details" section, **When** the learner expands it, **Then** each
   session shows word set name, start date/time and correct / incorrect / skipped counts, newest
   first.
4. **Given** a word set was deleted, **When** the statistics are shown, **Then** sessions of that set
   are still listed with the original set name and counts.
5. **Given** no sessions exist, **When** the screen opens, **Then** totals show zero and the chart
   area shows a Polish "no data yet" message.

---

### User Story 6 - Clear statistics with verification (Priority: P3)

The learner can delete all statistics after a clear warning and after typing the total number of
recorded words as confirmation.

**Why this priority**: Rarely used maintenance action.

**Independent Test**: With recorded sessions totalling 57 words, attempt to clear typing 50 (rejected,
nothing deleted), then typing 57 (all statistics removed).

**Acceptance Scenarios**:

1. **Given** statistics totalling 57 words, **When** the learner taps "Wyczyść dane", **Then** a
   warning states that all statistics will be permanently deleted and asks for the total word count.
2. **Given** the warning, **When** the learner enters a number other than 57, **Then** an error is
   shown and nothing is deleted.
3. **Given** the warning, **When** the learner enters 57 and confirms, **Then** all sessions are
   deleted, word sets remain untouched, and the screen shows the empty state.

---

### Edge Cases

- No word sets imported: quiz setup shows a Polish message with a shortcut to "Słówka"; the quiz
  cannot start.
- A word set has fewer than 10 words: it is listed in quiz setup but cannot be selected, with an
  explanation that at least 10 words are required.
- Empty or whitespace-only answer: the answer cannot be submitted.
- Leading/trailing spaces in the answer are ignored; repeated inner spaces are treated as one.
- Speech engine unavailable or fails: the learner sees a Polish message with a shortcut to device
  speech settings; the quiz cannot start (or, if mid-quiz, the learner may retry playback).
- Playback is requested while another playback is in progress: the current playback stops and the
  new one starts.
- App moved to background mid-quiz and resumed: the quiz continues at the same word and state.
- App process terminated mid-quiz: the unfinished session is discarded (not saved).
- Skipping disabled: no skip and no abort option; the quiz can only be left by finishing it.
- CSV file empty or containing only blank lines: import rejected with a Polish message.
- CSV line with more than three fields (e.g. an unquoted comma inside a sentence in a
  comma-delimited file): treated as invalid, reported with its line number.
- A file that starts with a header-like line (e.g. "word;translation;sentence"): no header is
  expected, so that line is imported as an ordinary entry.
- CSV file contains the same English word twice (case-insensitive): import rejected, naming both
  line numbers.
- CSV file is not valid text or cannot be read: import rejected with a Polish message.
- Set name empty or whitespace-only: the name cannot be confirmed.
- Overwriting a set that has session history: history stays; it continues to match words by set
  name and English word.
- Both play buttons stay usable after the answer is shown, including on the last word.

## Requirements *(mandatory)*

### Functional Requirements

**General & main screen**

- **FR-001**: The app MUST display all interface text in Polish.
- **FR-002**: The app name shown to users MUST be exactly "AIngielski" (capital A and I).
- **FR-003**: The app MUST have a modern launcher icon depicting a combined US and UK flag.
- **FR-004**: The app MUST work in portrait orientation only.
- **FR-005**: The main screen MUST provide a burger menu with "Ustawienia" (settings) and
  "Słówka" (word management).
- **FR-006**: The main screen MUST show function tiles, each with a title and a modern icon:
  "Odsłuchaj słówka" (headphones with sound waves icon) and "Statystyka" (chart icon).

**Quiz setup**

- **FR-010**: The quiz setup MUST list all word sets with their word counts and allow selecting one.
- **FR-011**: The setup MUST display the allowed word-count range for the selected set: minimum 10,
  maximum equal to the set's word count.
- **FR-012**: The setup MUST reject counts below 10 or above the set size with a clear Polish error
  message stating the allowed range, and MUST NOT start the quiz in that case.
- **FR-013**: The setup MUST offer exactly one selection criterion at a time: "Losowe" (random,
  default), "Tylko nowe" (only new), "Trudne" (difficult).
- **FR-014**: "New" words MUST be words of the selected set never played in any past session
  recorded under the same set name (matched by English word, case-insensitive).
- **FR-015**: "Difficult" words MUST be words whose error ratio — incorrect ÷ (correct + incorrect)
  across past sessions recorded under the same set name — is at least 51%. Skipped results are
  excluded from the ratio; words with no correct or incorrect results are not difficult.
- **FR-016**: When fewer words meet the chosen criterion than requested, the remainder MUST be
  filled with randomly chosen words from the rest of the set.
- **FR-017**: Each word MUST appear at most once per session; the order of words MUST be random.

**Quiz flow**

- **FR-020**: The quiz screen MUST show progress as "current / total" (starting at 1).
- **FR-021**: Each word MUST be spoken automatically when presented, using the voice and English
  variant from settings, without revealing the word, its translation or its example sentence.
- **FR-022**: The answer input MUST appear after the first automatic playback finishes.
- **FR-023**: The learner MUST be able to replay the word and play its example sentence an
  unlimited number of times, via buttons with play icons and captions "Odtwórz ponownie" and
  "Odtwórz zdanie", before and after answering.
- **FR-024**: When word skipping is enabled, a "Pomiń" button MUST be shown until the word is
  answered; skipping records the word as skipped and shows the correct word and translation.
- **FR-025**: Submitted answers MUST be compared with the played word case-insensitively, ignoring
  leading/trailing whitespace and collapsing repeated inner spaces.
- **FR-026**: After an answer, the app MUST show whether it was correct, the correct word (when
  incorrect) and the Polish translation; then show "Dalej", hide "Pomiń", and keep the play buttons.
- **FR-027**: The quiz MUST NOT offer any way to return to a previous word; the system back action
  MUST NOT leave or rewind the quiz.
- **FR-028**: When skipping is enabled, the learner MUST be able to abort the quiz ("Przerwij quiz")
  after confirming. If at least one word was answered (correct or incorrect), the session MUST be
  saved with all not-yet-completed words recorded as skipped and marked as not played; otherwise no
  session is saved.
- **FR-029**: The result (correct / incorrect / skipped) of every word MUST be recorded for the
  session.

**Quiz summary**

- **FR-030**: After the last word, the app MUST show the counts of correct, incorrect and skipped
  words and the success percentage = correct ÷ number of words in this quiz × 100, rounded to a
  whole number.
- **FR-031**: The summary MUST list every quiz word colored green (correct), red (incorrect) or
  orange (skipped); incorrect entries MUST also show what the learner typed.
- **FR-032**: The summary MUST offer "Wróć do ekranu głównego" (main screen) and "Nowy quiz"
  (opens the quiz setup).

**Settings**

- **FR-040**: Settings MUST include English variant (Amerykański / Brytyjski; default American),
  voice (list of voices installed on the device for the chosen variant; default: device default
  voice for that variant), and word skipping (Tak / Nie; default Tak).
- **FR-041**: Changing the variant MUST refresh the voice list; a voice not matching the new variant
  MUST be replaced by the default voice for that variant.
- **FR-042**: The learner SHOULD be able to preview the selected voice with a sample word.
- **FR-043**: "Zapisz" MUST persist settings and return to the main screen; "Anuluj" MUST discard
  changes and return to the main screen. Saved settings MUST persist until changed.

**Word management**

- **FR-050**: The "Słówka" screen MUST list saved word sets with name and word count, each with a
  delete action shown as a trash icon; deletion MUST require confirmation.
- **FR-051**: Tapping a set MUST open a screen showing its content as a table (word, translation,
  example sentence) with only a back action returning to "Słówka".
- **FR-052**: The learner MUST be able to import a word set from a CSV file chosen from the device;
  after choosing the file the app MUST ask for a set name, pre-filled with the file name without
  extension.
- **FR-053**: Each CSV line MUST contain three non-empty fields: English word, Polish translation,
  English example sentence. The file has no header row — every non-blank line is a word entry.
  The delimiter MUST be detected automatically per file: semicolon (;) if the first non-blank
  line contains a semicolon outside double quotes, otherwise comma (,); the detected delimiter
  applies to the whole file.
- **FR-054**: If any line is invalid, the import MUST be aborted entirely, saving nothing, and the
  app MUST show the line number(s) and the missing field(s).
- **FR-055**: Set names MUST be non-empty and unique (case-insensitive, trimmed). If the name
  already exists, the app MUST ask whether to overwrite; confirming MUST replace the entire content
  of that set, declining MUST abort the import with no changes.
- **FR-056**: The app MUST NOT offer in-app editing of word sets; content changes happen only via
  import.
- **FR-057**: Deleting or overwriting a word set MUST NOT modify or delete any session statistics.

**Statistics**

- **FR-060**: The statistics screen MUST show the total session count, the total number of recorded
  words split into correct / incorrect / skipped, and the average success percentage = mean of the
  per-session success percentages across all sessions.
- **FR-061**: The screen MUST show a line chart of the 10 most recent sessions (fewer if fewer exist)
  in chronological order, x-axis labelled with session start date and hour, with two lines: correct
  and incorrect counts.
- **FR-062**: The screen MUST include a foldable section (collapsed by default) listing every
  session, newest first, with word set name, start date/time and correct / incorrect / skipped
  counts.
- **FR-063**: "Wyczyść dane" MUST show a clear warning that all statistics will be permanently
  deleted and require the learner to type the total recorded word count; a wrong value MUST show an
  error and abort; the correct value MUST delete all sessions and nothing else.
- **FR-064**: Statistics can be removed only through FR-063.

**Data**

- **FR-070**: The app MUST store word sets on the device: unique name and a list of entries (word,
  translation, example sentence).
- **FR-071**: The app MUST store each session on the device: word set name, start date and time, the
  words selected for the session (with their translations, independent of the word set), and for
  each word its result (correct / incorrect / skipped), what the learner typed, and whether it was
  actually played.
- **FR-072**: Session data MUST be self-contained so that it remains complete after the word set is
  deleted or overwritten.

### Key Entities

- **Word set**: A named collection of words imported from one CSV file. Name is unique
  (case-insensitive). Contains many word entries.
- **Word entry**: English word, Polish translation, English example sentence; belongs to one set;
  the English word is unique within its set (case-insensitive).
- **Session (quiz)**: One quiz run. Word set name (stored as text, not a link), start date/time,
  selection criterion, ordered list of session words.
- **Session word**: English word and translation as used in the session, result (correct /
  incorrect / skipped), learner's typed answer (if any), and a played flag.
- **Settings**: English variant, selected voice, word-skipping flag.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A learner with an imported set can start a 10-word quiz from the main screen in under
  30 seconds and 5 taps or fewer.
- **SC-002**: Each word starts playing within 1 second of being presented, in 95% of cases.
- **SC-003**: 100% of answers differing from the played word only in letter case or surrounding
  spaces are judged correct.
- **SC-004**: Importing a valid 500-word CSV file completes in under 5 seconds; 100% of invalid
  files are rejected with the correct line number and no partial data saved.
- **SC-005**: After deleting any word set, 100% of previously recorded sessions and their totals
  remain identical on the statistics screen.
- **SC-006**: Statistics totals, averages and chart values match the recorded sessions exactly
  (verified on a set of at least 20 test sessions).
- **SC-007**: All core functions (import, quiz, settings, statistics) work with no network
  connection.
- **SC-008**: No English or untranslated interface text appears anywhere except learning content
  (words and example sentences).

## Assumptions

- Target users are Polish speakers learning English; a single learner per device, no accounts.
- Speech is produced by the device's built-in text-to-speech; voices come from those installed on
  the device. If none is available for the chosen variant, the learner is directed to device
  settings.
- CSV files are UTF-8 encoded (Polish characters supported); fields containing the delimiter are
  enclosed in double quotes; blank lines are ignored.
- Duplicate English words in one file make the file invalid (keeps "new" and "difficult" tracking
  unambiguous).
- Statistics link to words by set name + English word (case-insensitive); importing a set under a
  name used earlier reuses the history recorded under that name.
- "Played" means the word was spoken at least once in the session; aborted-unplayed words count as
  skipped in totals but do not make a word "not new".
- An unfinished quiz is not persisted if the app process is terminated; backgrounding keeps state.
- Exact Polish wording of buttons and messages given here is indicative and may be refined, as
  long as it remains Polish and unambiguous.
- Only the two tiles listed are in scope; further learning modes are future features.
