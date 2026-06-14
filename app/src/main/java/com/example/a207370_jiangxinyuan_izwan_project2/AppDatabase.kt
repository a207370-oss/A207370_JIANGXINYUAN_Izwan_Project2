package com.example.a207370_jiangxinyuan_izwan_project2

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_items")
    fun getAllItems(): Flow<List<StudyItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: StudyItem)

    @Update
    suspend fun updateItem(item: StudyItem)
}

@Database(entities = [StudyItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ebbinghaus_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}