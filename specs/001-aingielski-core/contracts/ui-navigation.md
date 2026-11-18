# Contract: Screens, navigation and UI rules

The app is a single Activity using Navigation Compose. It runs in `sensorPortrait` orientation
(portrait or upside-down portrait). All text comes from `res/values/strings.xml` and is Polish
only.

## Routes

| Route | Screen | Entered from | Leaves to |
|---|---|---|---|
| `home` (start) | Home: top bar "AIngielski", burger menu, 2 tiles | app launch | `setup`, `stats`, `settings`, `words` |
| `settings` | Ustawienia | drawer | `home` (Zapisz / Anuluj / back = Anuluj) |
| `words` | Słówka: set list, import button | drawer, empty-state link in `setup` | `wordSet/{id}`, `home` |
| `wordSet/{id}` | Set content table (word, translation, sentence) | `words` | `words` (back only) |
| `setup?setName={name}&count={n}&criterion={c}` (all arguments optional) | Quiz setup | tile "Odsłuchaj słówka" (no arguments); "Nowy quiz" (previous set name, count, criterion; FR-032) | `quiz/{sessionId}` (popUpTo `setup` inclusive), `home` |
| `quiz/{sessionId}` | Quiz | `setup` | `summary/{sessionId}`, `home` (after abort) |
| `summary/{sessionId}` | Quiz summary | `quiz` (popUpTo `home`) | `home`, `setup` |
| `stats` | Statystyka | tile "Statystyka" | `home` |

## Home

- **Drawer items**: "Ustawienia" and "Słówka", each with its custom icon.
- **Tiles**: laid out in a 2-column grid. Each tile is a card with a 64dp two-tone illustration
  and a title, and is fully clickable (minimum 48dp target).

## Quiz screen phases (FR-020 to FR-028a)

| Phase | Visible | Hidden |
|---|---|---|
| `Playing` (first automatic playback) | Progress `n / N`, "Słuchaj…" indicator | Word, translation, sentence, input field |
| `Answering` | Progress, input field + "Sprawdź", "Odtwórz ponownie", "Odtwórz zdanie", "Pomiń" (only if skipping is on), "Przerwij quiz" (only if skipping is on) | Word, translation, sentence |
| `Revealed` | Result badge (icon + "Dobrze!", "Źle" or "Pominięto"), correct word (when wrong or skipped), translation, example sentence text, both play buttons, "Dalej" (or "Zakończ" on the last word), "Przerwij quiz" (if skipping is on) | "Pomiń", input field (read-only echo of the answer) |

- "Sprawdź" and the keyboard's IME "Done" action are disabled while the input is blank.
- The input field turns off autocorrect, suggestions and auto-capitalization (FR-022a) and
  accepts up to 200 characters (FR-022b).
- If the first automatic playback fails or is interrupted, `Answering` is entered anyway with the
  message "Nie udało się odtworzyć słowa. Użyj „Odtwórz ponownie”." (FR-022).
- Playback stops when the screen goes to the background (`ON_STOP`); nothing replays on return
  (FR-023a).
- At quiz start, if media volume is muted, a snackbar shows "Głośność multimediów jest wyciszona"
  (FR-021b).
- Abort confirmation dialog (FR-028): title "Przerwać quiz?"; text "Odpowiedzi zostaną zapisane,
  pozostałe słówka zostaną oznaczone jako pominięte." when at least one word was answered,
  otherwise "Nic nie zostanie zapisane."; buttons "Przerwij" and "Kontynuuj".
- The word, translation and sentence are not in the composition (not merely invisible) before
  `Revealed`, so screen readers cannot read them; the result badge is a polite live region
  (FR-027a).
- System back: when skipping is on, it opens the abort confirmation; when skipping is off, it
  does nothing.

## Summary

- **Header**: the success percentage, plus the three counts, each shown with an icon and a
  colored chip.
- **List**: one row per word, in quiz order. Each row has a result color, a result icon, the word
  and its translation. Incorrect rows also show "Twoja odpowiedź: …" (your answer).
- **Actions**: "Wróć do ekranu głównego" (back to the main screen) and "Nowy quiz" (new quiz).

## Statystyka

The screen shows these sections, top to bottom:

1. **Totals card**: sessions, words (correct / incorrect / skipped), average %.
2. **Line chart**: "Ostatnie 10 sesji" (last 10 sessions).
3. **Foldable list**: "Szczegóły sesji" (session details), collapsed by default.
4. **Destructive button**: "Wyczyść dane".

**Clear dialog**: shows a warning icon and text, a numeric field "Wpisz łączną liczbę słówek",
and the buttons "Usuń wszystko" and "Anuluj". If the number is wrong, an inline error is shown
and nothing is deleted.

## Accessibility (rule V)

- Every icon has a `contentDescription` (or a `null` description when the icon is decorative and
  sits next to text).
- Results are conveyed by color **and** icon **and** text.
- Layouts scroll and do not clip at 200% system font scale.
- Touch targets are at least 48dp.
