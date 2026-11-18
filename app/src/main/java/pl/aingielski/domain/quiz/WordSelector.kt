package pl.aingielski.domain.quiz

import pl.aingielski.domain.model.WordItem
import pl.aingielski.domain.text.normalizeKey
import kotlin.random.Random

/** Words to prefer when selecting quiz words (FR-016). */
sealed interface PreferredWords {
    data object None : PreferredWords

    /** Any order; when more qualify than needed, they are picked at random ("Tylko nowe"). */
    data class Unordered(val keys: Set<String>) : PreferredWords

    /** Priority order, first = most preferred ("Trudne", hardest first). */
    data class Ordered(val keys: List<String>) : PreferredWords
}

/** Pure word selection (data-model.md › Word selection, FR-016, FR-017). */
object WordSelector {
    fun select(entries: List<WordItem>, preferred: PreferredWords, count: Int, random: Random): List<WordItem> {
        val byKey = LinkedHashMap<String, WordItem>()
        entries.forEach { byKey.putIfAbsent(normalizeKey(it.word), it) }

        val preferredItems: List<WordItem> = when (preferred) {
            PreferredWords.None -> emptyList()
            is PreferredWords.Unordered -> byKey.filterKeys { it in preferred.keys }.values.shuffled(random)
            is PreferredWords.Ordered -> preferred.keys.distinct().mapNotNull { byKey[it] }
        }
        val preferredKeys = preferredItems.mapTo(HashSet()) { normalizeKey(it.word) }
        val rest = byKey.filterKeys { it !in preferredKeys }.values.shuffled(random)

        return (preferredItems + rest).take(count).shuffled(random)
    }
}
