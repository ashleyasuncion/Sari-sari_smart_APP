package com.example.tindago.data.local

import androidx.room.TypeConverter
import com.example.tindago.data.StockStatus

class Converters {
    @TypeConverter
    fun fromStockStatus(status: StockStatus): String = status.name

    @TypeConverter
    fun toStockStatus(value: String): StockStatus = StockStatus.valueOf(value)
}
