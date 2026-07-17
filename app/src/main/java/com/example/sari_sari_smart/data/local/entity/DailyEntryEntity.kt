package com.example.sari_sari_smart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sari_sari_smart.data.DailyEntry

@Entity(tableName = "daily_entries")
data class DailyEntryEntity(
    @PrimaryKey val date: String,
    val stockExpenses: Double,
    val earnings: Double
) {
    fun toDomainModel(): DailyEntry = DailyEntry(
        date = date,
        stockExpenses = stockExpenses,
        earnings = earnings
    )

    companion object {
        fun fromDomainModel(entry: DailyEntry): DailyEntryEntity = DailyEntryEntity(
            date = entry.date,
            stockExpenses = entry.stockExpenses,
            earnings = entry.earnings
        )
    }
}
