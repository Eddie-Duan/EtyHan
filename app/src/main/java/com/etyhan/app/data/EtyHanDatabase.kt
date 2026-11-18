package com.etyhan.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * 历史 / 词库。`payload` 存整份 Analysis 的 JSON：
 * 模型字段变化时不需要迁移表结构，展示层容错即可。
 */
@Entity(
    tableName = "analyses",
    indices = [Index(value = ["queryNorm"], unique = true)],
)
data class AnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val queryNorm: String,
    val payload: String,
    val provider: String,
    val model: String,
    val tokensIn: Int,
    val tokensOut: Int,
    val elapsedMs: Long,
    val favorite: Boolean = false,
    val hits: Int = 1,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface AnalysisDao {

    @Query("SELECT * FROM analyses ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses WHERE favorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<AnalysisEntity>>

    @Query(
        "SELECT * FROM analyses WHERE query LIKE '%' || :q || '%' OR payload LIKE '%' || :q || '%' " +
            "ORDER BY updatedAt DESC LIMIT 200"
    )
    fun search(q: String): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analyses WHERE queryNorm = :norm LIMIT 1")
    suspend fun findByNorm(norm: String): AnalysisEntity?

    @Query("SELECT * FROM analyses WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): AnalysisEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AnalysisEntity): Long

    @Query("UPDATE analyses SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("DELETE FROM analyses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM analyses")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM analyses")
    suspend fun count(): Int
}

@Database(entities = [AnalysisEntity::class], version = 1, exportSchema = false)
abstract class EtyHanDatabase : RoomDatabase() {

    abstract fun analysisDao(): AnalysisDao

    companion object {
        fun build(context: Context): EtyHanDatabase =
            Room.databaseBuilder(context, EtyHanDatabase::class.java, "etyhan.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
