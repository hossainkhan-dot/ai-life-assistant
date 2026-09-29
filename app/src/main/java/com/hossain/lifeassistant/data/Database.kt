package com.hossain.lifeassistant.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "diary")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val time: String,
    val content: String,
    val category: String
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    val time: String,
    val priority: String,
    val done: Boolean = false
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    val time: String,
    val repeat: String = "Once"
)

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary ORDER BY date DESC, time DESC")
    fun all(): Flow<List<DiaryEntry>>

    @Insert
    suspend fun insert(e: DiaryEntry): Long

    @Delete
    suspend fun delete(e: DiaryEntry)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY date ASC, time ASC")
    fun all(): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(e: TaskEntity): Long

    @Update
    suspend fun update(e: TaskEntity)

    @Delete
    suspend fun delete(e: TaskEntity)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY date ASC, time ASC")
    fun all(): Flow<List<ReminderEntity>>

    @Insert
    suspend fun insert(e: ReminderEntity): Long

    @Delete
    suspend fun delete(e: ReminderEntity)
}

@Database(
    entities = [DiaryEntry::class, TaskEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "life.db"
                ).build().also { instance = it }
            }
    }
}
