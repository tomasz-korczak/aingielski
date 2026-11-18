# Data Model: AIngielski – Core App

**Feature**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

The app stores data in two places:

- **Room database** (`aingielski.db`): the word-set tables and the session tables. There is no
  link between these two groups (FR-057, FR-072).
- **DataStore Preferences** (`settings`): user settings.

## Normalization rule (shared)

`normalizeKey(s) = s.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)`

This key is used for:

- uniqueness of set names (case-insensitive)
- uniqueness of words within a set
- matching history for "new" and "difficult" words (by `setNameKey` + `wordKey`)

Answer checking uses the same normalization on both sides (FR-025). Punctuation is never
removed.

---

## Word sets

### `word_set`

| Field | Type | Rules |
|---|---|---|
| `id` | Long, PK, auto | |
| `name` | String | Trimmed. Not blank. At most 50 characters. Shown exactly as entered. |
| `name_key` | String, **UNIQUE** | `normalizeKey(name)` |
| `imported_at` | Long (epoch ms) | Set on import and on overwrite |

### `word_entry`

| Field | Type | Rules |
|---|---|---|
| `id` | Long, PK, auto | |
| `set_id` | Long, FK → `word_set.id`, **ON DELETE CASCADE**, indexed | |
| `position` | Int | Order as in the file (0-based) |
| `word` | String | English word or phrase. Trimmed. Not blank. |
| `word_key` | String | `normalizeKey(word)`. **UNIQUE together with `set_id`**. |
| `translation` | String | Polish. Trimmed. Not blank. |
| `sentence` | String | English example sentence. Trimmed. Not blank. |

**Set-level rules**:

- A set has **at least 10** entries (FR-054a). This is enforced by import validation before any
  write.
- **Overwrite** happens in one transaction: delete the old `word_set` row (its entries cascade),
  then insert the new set and its entries. No session row is touched.
- **Delete**: deletes the `word_set` row (entries cascade). Sessions stay.

---

## Sessions

### `quiz_session`

| Field | Type | Rules |
|---|---|---|
| `id` | Long, PK, auto | |
| `set_name` | String | Copy of the set name at quiz start, kept for display. **Not a foreign key.** |
| `set_name_key` | String, indexed | `normalizeKey(set_name)`, used for history matching |
| `started_at` | Long (epoch ms), indexed | When the quiz started |
| `criterion` | String enum | `RANDOM` \| `NEW_ONLY` \| `DIFFICULT` |
| `word_count` | Int | Number of words selected (10 to set size) |
| `status` | String enum | `IN_PROGRESS` \| `COMPLETED` \| `ABORTED` |

### `session_word`

| Field | Type | Rules |
|---|---|---|
| `id` | Long, PK, auto | |
| `session_id` | Long, FK → `quiz_session.id`, **ON DELETE CASCADE**, indexed | |
| `position` | Int | 0-based order in which words are presented |
| `word` | String | Copy of the English word |
| `word_key` | String, indexed | `normalizeKey(word)` |
| `translation` | String | Copy |
| `sentence` | String | Copy, so the session is self-contained |
| `result` | String enum | `PENDING` \| `CORRECT` \| `INCORRECT` \| `SKIPPED` |
| `typed_answer` | String? | The raw text the learner typed. Null if skipped or pending. |
| `played` | Boolean | Set to true when the learner completes the word (answer or "Pomiń"). Stays false for words left `PENDING`, including a heard-but-unanswered word at abort/interruption. |

Each `(session_id, word_key)` pair is unique, so a word appears only once per session (FR-017).

### Session states

```text
             start quiz (insert session + N PENDING words)
                         │
                         ▼
                   IN_PROGRESS ──(each answer/skip/playback: update one session_word row)
                    │         │
   last word done   │         │  abort (user) / app start finds stale IN_PROGRESS /
                    ▼         ▼  quiz screen recreated without state
               COMPLETED    finalizeInterrupted():
                              any CORRECT|INCORRECT? ── yes ─▶ PENDING→SKIPPED, played = false
                                                               status = ABORTED
                                                      ── no ──▶ DELETE session (cascade)
```

- `COMPLETED` and `ABORTED` are final states. Statistics read **only** these two.
- **Clear statistics** (FR-063): `DELETE FROM quiz_session`. Rows in `session_word` cascade.
  Word-set tables are untouched.

---

## Derived queries (domain rules)

All history queries consider only sessions whose `status` is in (`COMPLETED`, `ABORTED`) and
whose `set_name_key` equals the key of the selected set.

| Query | Definition |
|---|---|
| **Played keys** | Distinct `word_key` with `played = 1` in matching sessions |
| **New words** (FR-014) | Entries of the set whose `word_key` is not among the played keys |
| **Difficult words** (FR-015) | Group matching `session_word` rows by `word_key`. Let `c` = number of CORRECT rows and `i` = number of INCORRECT rows. Difficult when `c + i > 0` and `i / (c + i) ≥ 0.51`. SKIPPED rows are ignored. |
| **Session success %** | `100 × correct / word_count`, displayed rounded half-up to a whole number |
| **Global totals** (FR-060) | Number of sessions. Sums of correct, incorrect and skipped. Average = mean of the **unrounded** per-session success % values, displayed rounded half-up (0 sessions → shown as "—"). |
| **Chart** (FR-061) | The 10 newest sessions by `started_at`, shown oldest to newest, with their correct and incorrect counts |
| **Clear-data verification** | The typed number must equal the sum of correct, incorrect and skipped across all sessions |

**Word selection** (FR-016, FR-017) is a pure function:

```text
selectWords(entries, criterionKeys?, count, random)
  preferred = entries filtered by criterionKeys, shuffled
  rest      = remaining entries, shuffled
  result    = (preferred + rest).take(count), shuffled again
```

---

## Settings (DataStore Preferences)

| Key | Type | Default | Notes |
|---|---|---|---|
| `english_variant` | String enum `US` \| `GB` | `US` | Maps to locale `en-US` or `en-GB` |
| `voice_id` | String? | null (the engine's default voice for the variant) | Opaque id from `SpeechEngine.availableVoices()` |
| `skip_enabled` | Boolean | `true` | Controls the skip and abort options |

**Validation**: if `voice_id` does not belong to the current variant, or is no longer installed,
the app falls back to the default voice for the variant (FR-041).

---

## Domain (in-memory) models

These are plain Kotlin types used by the ViewModels; they are not persisted.

- `WordSetSummary(id, name, wordCount)`
- `WordItem(word, translation, sentence)`
- `QuizSetup(setId, count, criterion)`, with validation result `CountError.TooLow(min=10)` or
  `CountError.TooHigh(max)`
- `QuizState`: current index, total, the current `WordItem`, and a phase
  (`Playing` → `Answering` → `Revealed(correct: Boolean?)`)
- `QuizSummary(correct, incorrect, skipped, percent, items: List<SummaryItem>)`
- `ImportResult`: one of
  - `Success(entries)`
  - `Invalid(errors: List<LineError(line, missing: Set<Field>, reason)>)`
  - `TooFew(count)`
  - `Empty`
  - `Unreadable`
