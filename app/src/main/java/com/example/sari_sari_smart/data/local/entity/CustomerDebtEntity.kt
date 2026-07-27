package com.example.sari_sari_smart.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sari_sari_smart.data.CustomerDebt

@Entity(tableName = "customer_debts")
data class CustomerDebtEntity(
    @PrimaryKey val id: Int,
    val customerName: String,
    val amount: Double,
    val remainingBalance: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): CustomerDebt = CustomerDebt(
        id = id,
        customerName = customerName,
        amount = amount,
        remainingBalance = remainingBalance,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(debt: CustomerDebt): CustomerDebtEntity = CustomerDebtEntity(
            id = debt.id,
            customerName = debt.customerName,
            amount = debt.amount,
            remainingBalance = debt.remainingBalance,
            createdAt = debt.createdAt
        )
    }
}
