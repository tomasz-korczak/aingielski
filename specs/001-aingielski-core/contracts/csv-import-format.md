# Contract: Word-set CSV import format

This is the format learners use to prepare word-set files (FR-052 to FR-055, Clarifications
2026-11-18).

## Format

| Aspect | Rule |
|---|---|
| Encoding | UTF-8. An optional BOM is ignored. |
| Header | **None.** Every non-blank line is a word entry. |
| Delimiter | Detected per file. If the first non-blank line contains `;` outside double quotes, the delimiter is `;`; otherwise it is `,`. The whole file uses the detected delimiter. |
| Fields | Exactly 3, in this order: English word, Polish translation, English example sentence |
| Quoting | A field may be enclosed in `"…"`. Inside quotes, the delimiter is literal and `""` stands for `"`. |
| Whitespace | Leading and trailing spaces of each field are trimmed |
| Blank lines | Lines that are empty or whitespace-only are skipped (they still count for line numbering) |
| Line endings | `\n` or `\r\n`. A field cannot contain a line break. |
| Minimum size | At least 10 valid entries |
| Uniqueness | English words must be unique within the file (case-insensitive, normalized) |
| Answer form | The English word field is the **single** accepted answer. `/` and parentheses have no special meaning (`colour/color` must be typed literally). Choose one spelling per entry. |

## Examples

Semicolon-delimited (Polish Excel's default):

```text
apple;jabłko;I eat an apple every day.
cat;kot;The cat is sleeping, as usual.
```

Comma-delimited (quote any field that contains a comma):

```text
apple,jabłko,I eat an apple every day.
cat,kot,"The cat is sleeping, as usual."
```

## Validation result

Validation runs completely before anything is saved. If any error is found, **nothing** is
written. All errors are reported at once; line numbers are 1-based physical lines.

| Condition | Polish message (indicative) |
|---|---|
| Field missing or blank | `Wiersz {n}: brak pola „{tłumaczenie}”.` (several missing fields are listed) |
| More than 3 fields | `Wiersz {n}: za dużo pól (oczekiwano 3).` |
| Unclosed quote | `Wiersz {n}: niezamknięty cudzysłów.` |
| Duplicate word | `Wiersz {n}: słowo „{word}” powtarza się (pierwsze wystąpienie: wiersz {m}).` |
| Fewer than 10 entries | `Zestaw musi zawierać co najmniej 10 słówek (w pliku: {k}).` |
| Empty file | `Plik jest pusty.` |
| Unreadable / not text | `Nie można odczytać pliku.` |

Field names used in messages: `słowo`, `tłumaczenie`, `zdanie przykładowe`.

## Set name and overwrite

- After a file is chosen, the name field is pre-filled with the file name minus `.csv`.
- The name must be non-blank after trimming, at most 50 characters.
- If `normalizeKey(name)` matches an existing set, the app asks:
  `Zestaw „{name}” już istnieje. Zastąpić go?` with the choices **Zastąp** (replace) and
  **Anuluj** (cancel).
  - **Zastąp** replaces the whole set in a single transaction.
  - **Anuluj** aborts the import with no changes.
- Session history is never modified by an import.
