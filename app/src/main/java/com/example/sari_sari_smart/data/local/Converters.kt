package com.example.sari_sari_smart.data.local

import androidx.room.TypeConverter
import com.example.sari_sari_smart.data.StockStatus

class Converters {
    @TypeConverter
    fun fromStockStatus(status: StockStatus): String = status.name

    @TypeConverter
    fun toStockStatus(value: String): StockStatus = StockStatus.valueOf(value)
}
