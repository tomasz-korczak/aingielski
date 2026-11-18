# Quickstart & Validation Guide: AIngielski – Core App

This guide covers how to build, test and check end to end that the feature works. For the
design, see [data-model.md](./data-model.md) and [contracts/](./contracts/).

## Prerequisites

- **JDK 23**, set in `JAVA_HOME` (runs the Gradle daemon; the code compiles to JVM 17).
- **Android SDK** with platform 36 and build tools. Android Studio (latest stable) is
  recommended.
- **A device or emulator** running Android 8.0 (API 26) or newer, with a TTS engine installed
  (for example Speech Services by Google). To test the "missing voices" flow, use one device with
  English voices installed and one without.
- **Sample files** (added during implementation under `samples/`):
  - `zwierzeta_sredniki.csv`: 25 entries, delimited with `;`
  - `podstawy_przecinki.csv`: 12 entries, delimited with `,`, with quoted sentences
  - `bledny_wiersz7.csv`: line 7 has no sentence
  - `za_maly.csv`: 8 entries
  - `duplikat.csv`: a repeated word

## Build & test (repository root, Windows)

```powershell
.\gradlew.bat assembleDebug                 # builds app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat testDebugUnitTest             # JVM unit tests (domain + ViewModels)
.\gradlew.bat connectedDebugAndroidTest     # Room DAO + Compose UI tests (device required)
.\gradlew.bat lint                          # Android lint, must report no errors
.\gradlew.bat installDebug                  # install on the connected device
```

The merge gate (constitution, Development Workflow) is that `assembleDebug` and
`testDebugUnitTest` both pass.

## Validation scenarios

Each scenario maps to a user story (US) in [spec.md](./spec.md).

### V1: Branding & shell

1. Install the app and launch it.
   - The launcher shows the "AIngielski" label and the US/UK flag icon.
   - On Android 13+ with themed icons on, the monochrome icon is used.
2. Rotate the phone.
   - Upside-down portrait works.
   - Landscape is ignored.
3. Open the burger menu: it shows "Ustawienia" and "Słówka".
4. Check the home screen: it shows the "Odsłuchaj słówka" and "Statystyka" tiles with their
   icons.

### V2: Import & browse (US1)

1. Go to Słówka → Importuj and choose `zwierzeta_sredniki.csv`.
   - The name is pre-filled as "zwierzeta_sredniki".
   - Change it to "Zwierzęta" and save. The list shows "Zwierzęta – 25".
2. Import `podstawy_przecinki.csv`.
   - The quoted commas are kept in the sentences.
3. Import `bledny_wiersz7.csv`.
   - The error mentions "Wiersz 7" and "zdanie przykładowe".
   - No new set appears.
4. Import `za_maly.csv`: it is rejected with the "co najmniej 10" message.
5. Import `duplikat.csv`: it is rejected, naming both line numbers.
6. Import any valid file under the name "zwierzęta" (lowercase).
   - The overwrite prompt appears.
   - Anuluj leaves the set unchanged; Zastąp replaces its content.
7. Tap a set: the table opens, and back returns to Słówka.

### V3: Quiz core flow (US2)

1. Open Ustawienia, enable skipping, and save.
2. Tap Odsłuchaj słówka and select "Zwierzęta".
   - The range shown is "10–25".
3. Enter 8, then 30.
   - Each shows an error, and Start stays disabled.
4. Enter 12, keep "Losowe", and start.
   - The screen shows "1 / 12" and the word plays.
   - No word text is visible, and the input field appears only after playback ends.
5. Use "Odtwórz ponownie" and "Odtwórz zdanie".
   - Both can be played repeatedly, and pressing one during playback interrupts it.
6. Type the word in different letter case with extra spaces and tap Sprawdź.
   - The result is "Dobrze!" with the translation shown.
   - "Pomiń" is hidden, "Dalej" is visible, and both play buttons still work.
7. Answer one word wrongly. The result is "Źle", with the correct word and the translation shown.
8. Skip one word. It is recorded as skipped, and the word and translation are revealed.
9. Press system back. The abort dialog appears; cancel it.
10. Finish all words. The summary shows:
    - the counts and the percentage (= correct ÷ 12)
    - the word list colored green / red / orange, with icons
    - "Twoja odpowiedź" on incorrect rows
11. Tap "Nowy quiz": it opens the setup screen.

### V4: Abort & interruption (US2, FR-028/028a)

1. Start a 10-word quiz, answer 3 words, then tap Przerwij quiz and confirm.
   - In Statystyka, the session shows 3 answered and 7 skipped.
2. Start a quiz, skip 2 words without answering, and abort.
   - No new session appears in Statystyka.
3. Start a quiz, answer 2 words, then swipe the app away from recents and relaunch.
   - The app opens on Home, not in the quiz.
   - Statistics contain the session: 2 answered, the rest skipped.
4. Disable skipping in Ustawienia and start a quiz.
   - There is no Pomiń or Przerwij quiz, and system back does nothing.

### V5: Targeted selection (US3)

1. Complete one 10-word quiz on a set with 25 words.
2. Start a 15-word quiz with "Tylko nowe".
   - The 15 new words are selected first, and the rest are random. Check via the summary list.
3. Get a word wrong twice and right once across sessions.
   - "Trudne" includes it.
   - A word with one right and one wrong answer is excluded.

### V6: Settings & voices (US4)

1. Select Brytyjski, choose a voice, preview it, disable skipping, and tap Zapisz.
   - Restart the app: the values persist.
2. Change the values and tap Anuluj. Nothing changes.
3. On a device without English voices, open Ustawienia.
   - A "brak głosów" message appears with "Pobierz głosy".
   - "Pobierz głosy" opens the engine's install screen (or the TTS settings).
   - After installing a voice and returning, the voice list refreshes automatically.

### V7: Statistics & clearing (US5, US6)

1. With 11 or more sessions:
   - The totals match the per-session sums.
   - The average is the mean of the per-session percentages.
   - The chart shows the 10 newest sessions, oldest on the left, labelled `dd.MM HH:mm`.
2. Expand "Szczegóły sesji". The rows are newest first, each with the set name, date/time and
   counts.
3. Delete the word set "Zwierzęta" in Słówka. Statystyka is unchanged.
4. Tap Wyczyść dane and enter the wrong total.
   - An error is shown and the data stays.
5. Enter the correct total.
   - The statistics are empty and the chart shows "Brak danych".
   - The word sets remain.

### V8: Offline & accessibility

1. Turn on airplane mode and repeat V2 and V3. Everything works.
2. Set the system font to its largest size and visit every screen.
   - Nothing is clipped, and every screen scrolls.
3. Turn on TalkBack and use Home and Quiz.
   - Every icon button is announced in Polish.
