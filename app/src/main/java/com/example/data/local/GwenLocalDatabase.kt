package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_offline_notes")
data class LocalOfflineNote(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val category: String,
    val updatedAtMillis: Long
)

@Entity(tableName = "generated_media")
data class GeneratedMediaEntity(
    @PrimaryKey val id: String,
    val prompt: String,
    val filePath: String,
    val modelName: String,
    val isEdit: Boolean,
    val createdAtMillis: Long
)

@Dao
interface LocalOfflineNoteDao {
    @Query("SELECT * FROM local_offline_notes ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<LocalOfflineNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: LocalOfflineNote)

    @Query("DELETE FROM local_offline_notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM local_offline_notes")
    suspend fun clearAll()
}

@Dao
interface GeneratedMediaDao {
    @Query("SELECT * FROM generated_media ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<GeneratedMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: GeneratedMediaEntity)

    @Query("DELETE FROM generated_media WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(
    entities = [LocalOfflineNote::class, GeneratedMediaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GwenLocalDatabase : RoomDatabase() {
    abstract fun offlineNoteDao(): LocalOfflineNoteDao
    abstract fun generatedMediaDao(): GeneratedMediaDao

    companion object {
        @Volatile
        private var INSTANCE: GwenLocalDatabase? = null

        fun getInstance(context: Context): GwenLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GwenLocalDatabase::class.java,
                    "gwen_local_cache.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
