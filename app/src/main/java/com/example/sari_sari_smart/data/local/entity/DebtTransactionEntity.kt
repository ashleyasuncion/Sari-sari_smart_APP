package com.example.sari_sari_smart.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.sari_sari_smart.data.DebtTransaction

@Entity(
    tableName = "debt_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerDebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("debtId")]
)
data class DebtTransactionEntity(
    @PrimaryKey val id: Int,
    val debtId: Int,
    val type: String,
    val description: String? = null,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): DebtTransaction = DebtTransaction(
        id = id,
        debtId = debtId,
        type = type,
        description = description,
        amount = amount,
        timestamp = timestamp
    )

    companion object {
        fun fromDomainModel(tx: DebtTransaction): DebtTransactionEntity = DebtTransactionEntity(
            id = tx.id,
            debtId = tx.debtId,
            type = tx.type,
            description = tx.description,
            amount = tx.amount,
            timestamp = tx.timestamp
        )
    }
}
