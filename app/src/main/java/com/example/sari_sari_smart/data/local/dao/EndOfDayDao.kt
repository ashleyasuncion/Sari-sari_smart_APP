package com.example.sari_sari_smart.data.local.dao

import androidx.room.*
import com.example.sari_sari_smart.data.local.entity.EndOfDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EndOfDayDao {
    @Query("SELECT * FROM end_of_day_data ORDER BY date DESC LIMIT 1")
    fun getLatest(): Flow<EndOfDayEntity?>

    @Query("SELECT * FROM end_of_day_data WHERE date = :date")
    suspend fun getByDate(date: String): EndOfDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: EndOfDayEntity)

    @Query("DELETE FROM end_of_day_data WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM end_of_day_data")
    suspend fun deleteAll()
}
