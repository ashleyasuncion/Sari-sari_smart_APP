package com.example.sari_sari_smart.data.local.dao

import androidx.room.*
import com.example.sari_sari_smart.data.local.entity.RestockLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestockLogDao {
    @Query("SELECT * FROM restock_log ORDER BY date DESC")
    fun getAll(): Flow<List<RestockLogEntity>>

    @Query("SELECT * FROM restock_log ORDER BY date DESC LIMIT 1")
    fun getLatest(): Flow<RestockLogEntity?>

    @Query("SELECT * FROM restock_log WHERE date = :date")
    suspend fun getByDate(date: String): RestockLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RestockLogEntity)

    @Query("DELETE FROM restock_log")
    suspend fun deleteAll()

    @Query("DELETE FROM restock_log WHERE id = :id")
    suspend fun deleteById(id: String)
}
