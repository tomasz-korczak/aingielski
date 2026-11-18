package pl.aingielski.data.db

import androidx.room.TypeConverter
import pl.aingielski.domain.model.SelectionCriterion
import pl.aingielski.domain.model.SessionStatus
import pl.aingielski.domain.model.WordResult

/** Enums are stored by name so raw SQL in the DAOs can compare against literals like 'COMPLETED'. */
class Converters {
    @TypeConverter fun criterionToString(value: SelectionCriterion): String = value.name
    @TypeConverter fun stringToCriterion(value: String): SelectionCriterion = SelectionCriterion.valueOf(value)

    @TypeConverter fun statusToString(value: SessionStatus): String = value.name
    @TypeConverter fun stringToStatus(value: String): SessionStatus = SessionStatus.valueOf(value)

    @TypeConverter fun resultToString(value: WordResult): String = value.name
    @TypeConverter fun stringToResult(value: String): WordResult = WordResult.valueOf(value)
}
