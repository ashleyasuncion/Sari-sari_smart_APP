package com.example.tindago.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tindago.data.CustomerDebt

@Entity(tableName = "customer_debts")
data class CustomerDebtEntity(
    @PrimaryKey val id: Int,
    val customerName: String,
    val amount: Double,
    val remainingBalance: Double,
    val createdAt: Long = System.currentTimeMillis(),
    /** Per-customer credit limit; null = uses global default (web v2.56 parity). */
    val creditLimit: Int? = null
) {
    fun toDomainModel(): CustomerDebt = CustomerDebt(
        id = id,
        customerName = customerName,
        amount = amount,
        remainingBalance = remainingBalance,
        createdAt = createdAt,
        creditLimit = creditLimit
    )

    companion object {
        fun fromDomainModel(debt: CustomerDebt): CustomerDebtEntity = CustomerDebtEntity(
            id = debt.id,
            customerName = debt.customerName,
            amount = debt.amount,
            remainingBalance = debt.remainingBalance,
            createdAt = debt.createdAt,
            creditLimit = debt.creditLimit
        )
    }
}
