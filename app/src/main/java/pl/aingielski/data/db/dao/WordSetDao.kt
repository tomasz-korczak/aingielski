package pl.aingielski.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import pl.aingielski.data.db.entity.WordEntryEntity
import pl.aingielski.data.db.entity.WordSetEntity

data class WordSetSummaryRow(
    val id: Long,
    val name: String,
    val wordCount: Int,
)

@Dao
abstract class WordSetDao {
    @Query(
        """
        SELECT s.id AS id, s.name AS name, COUNT(e.id) AS wordCount
        FROM word_set s LEFT JOIN word_entry e ON e.set_id = s.id
        GROUP BY s.id
        ORDER BY s.name_key
        """,
    )
    abstract fun observeSummaries(): Flow<List<WordSetSummaryRow>>

    @Query("SELECT * FROM word_set WHERE name_key = :nameKey LIMIT 1")
    abstract suspend fun findByNameKey(nameKey: String): WordSetEntity?

    @Query("SELECT * FROM word_set WHERE id = :id LIMIT 1")
    abstract suspend fun findById(id: Long): WordSetEntity?

    @Query("SELECT * FROM word_entry WHERE set_id = :setId ORDER BY position")
    abstract suspend fun entries(setId: Long): List<WordEntryEntity>

    @Insert
    protected abstract suspend fun insertSetRow(set: WordSetEntity): Long

    @Insert
    protected abstract suspend fun insertEntryRows(entries: List<WordEntryEntity>)

    @Query("DELETE FROM word_set WHERE id = :id")
    abstract suspend fun deleteSet(id: Long)

    @Query("DELETE FROM word_set WHERE name_key = :nameKey")
    protected abstract suspend fun deleteByNameKey(nameKey: String)

    /** Inserts a set and its entries; the entries' `set_id` is replaced by the new set id. */
    @Transaction
    open suspend fun insertSet(set: WordSetEntity, entries: List<WordEntryEntity>): Long {
        val id = insertSetRow(set)
        insertEntryRows(entries.map { it.copy(id = 0, setId = id) })
        return id
    }

    /** Replaces the whole set with the same `name_key` in one transaction (FR-055). */
    @Transaction
    open suspend fun replaceSet(set: WordSetEntity, entries: List<WordEntryEntity>): Long {
        deleteByNameKey(set.nameKey)
        return insertSet(set, entries)
    }
}
