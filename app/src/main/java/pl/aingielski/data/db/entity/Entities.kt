package pl.aingielski.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.SessionStatus
import pl.aingielski.domain.model.WordResult

/** data-model.md `word_set`. */
@Entity(
    tableName = "word_set",
    indices = [Index(value = ["name_key"], unique = true)],
)
data class WordSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "name_key") val nameKey: String,
    @ColumnInfo(name = "imported_at") val importedAt: Long,
)

/** data-model.md `word_entry`. */
@Entity(
    tableName = "word_entry",
    foreignKeys = [
        ForeignKey(
            entity = WordSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["set_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["set_id"]),
        Index(value = ["set_id", "word_key"], unique = true),
    ],
)
data class WordEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "set_id") val setId: Long,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "word") val word: String,
    @ColumnInfo(name = "word_key") val wordKey: String,
    @ColumnInfo(name = "translation") val translation: String,
    @ColumnInfo(name = "sentence") val sentence: String,
)

/** data-model.md `quiz_session`. Deliberately no foreign key to `word_set` (FR-057, FR-072). */
@Entity(
    tableName = "quiz_session",
    indices = [Index(value = ["set_name_key"]), Index(value = ["started_at"])],
)
data class QuizSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "set_name") val setName: String,
    @ColumnInfo(name = "set_name_key") val setNameKey: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "criterion") val criterion: SelectionCriterion,
    @ColumnInfo(name = "word_count") val wordCount: Int,
    @ColumnInfo(name = "status") val status: SessionStatus,
)

/** data-model.md `session_word`. */
@Entity(
    tableName = "session_word",
    foreignKeys = [
        ForeignKey(
            entity = QuizSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["word_key"]),
        Index(value = ["session_id", "word_key"], unique = true),
    ],
)
data class SessionWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "word") val word: String,
    @ColumnInfo(name = "word_key") val wordKey: String,
    @ColumnInfo(name = "translation") val translation: String,
    @ColumnInfo(name = "sentence") val sentence: String,
    @ColumnInfo(name = "result") val result: WordResult,
    @ColumnInfo(name = "typed_answer") val typedAnswer: String?,
    @ColumnInfo(name = "played") val played: Boolean,
)
