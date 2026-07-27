package com.example.sari_sari_smart.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.sari_sari_smart.data.DebtPayment

@Entity(
    tableName = "debt_payments",
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
data class DebtPaymentEntity(
    @PrimaryKey val id: Int,
    val debtId: Int,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
) {
    fun toDomainModel(): DebtPayment = DebtPayment(
        id = id,
        debtId = debtId,
        amount = amount,
        timestamp = timestamp,
        note = note
    )

    companion object {
        fun fromDomainModel(payment: DebtPayment): DebtPaymentEntity = DebtPaymentEntity(
            id = payment.id,
            debtId = payment.debtId,
            amount = payment.amount,
            timestamp = payment.timestamp,
            note = payment.note
        )
    }
}
