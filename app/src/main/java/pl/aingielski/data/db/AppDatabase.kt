package pl.aingielski.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pl.aingielski.data.db.dao.SessionDao
import pl.aingielski.data.db.dao.WordSetDao
import pl.aingielski.data.db.entity.QuizSessionEntity
import pl.aingielski.data.db.entity.SessionWordEntity
import pl.aingielski.data.db.entity.WordEntryEntity
import pl.aingielski.data.db.entity.WordSetEntity

@Database(
    entities = [WordSetEntity::class, WordEntryEntity::class, QuizSessionEntity::class, SessionWordEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordSetDao(): WordSetDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val NAME = "aingielski.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME).build()
    }
}
