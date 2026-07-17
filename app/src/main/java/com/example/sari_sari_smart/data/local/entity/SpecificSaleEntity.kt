package com.example.sari_sari_smart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sari_sari_smart.data.SpecificSale

@Entity(tableName = "specific_sales")
data class SpecificSaleEntity(
    @PrimaryKey val id: Int,
    val date: String,
    val description: String,
    val amount: Double,
    val quantity: Int = 1,
    val customerName: String? = null,
    val profit: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): SpecificSale = SpecificSale(
        id = id,
        date = date,
        description = description,
        amount = amount,
        quantity = quantity,
        customerName = customerName,
        profit = profit,
        timestamp = timestamp
    )

    companion object {
        fun fromDomainModel(sale: SpecificSale): SpecificSaleEntity = SpecificSaleEntity(
            id = sale.id,
            date = sale.date,
            description = sale.description,
            amount = sale.amount,
            quantity = sale.quantity,
            customerName = sale.customerName,
            profit = sale.profit,
            timestamp = sale.timestamp
        )
    }
}
