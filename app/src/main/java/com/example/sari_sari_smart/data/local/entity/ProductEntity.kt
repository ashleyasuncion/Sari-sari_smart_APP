package com.example.sari_sari_smart.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sari_sari_smart.data.Product
import com.example.sari_sari_smart.data.StockStatus

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val quantity: Int,
    val costPrice: Double,
    val sellingPrice: Double,
    val unit: String = "piece",
    val lowStockThreshold: Int = 5
) {
    val status: StockStatus get() = when {
        quantity <= 0 -> StockStatus.OUT_OF_STOCK
        quantity <= lowStockThreshold -> StockStatus.LOW
        else -> StockStatus.PLENTY
    }

    fun toDomainModel(): Product = Product(
        id = id,
        name = name,
        quantity = quantity,
        costPrice = costPrice,
        sellingPrice = sellingPrice,
        unit = unit,
        lowStockThreshold = lowStockThreshold
    )

    companion object {
        fun fromDomainModel(product: Product): ProductEntity = ProductEntity(
            id = product.id,
            name = product.name,
            quantity = product.quantity,
            costPrice = product.costPrice,
            sellingPrice = product.sellingPrice,
            unit = product.unit,
            lowStockThreshold = product.lowStockThreshold
        )
    }
}
