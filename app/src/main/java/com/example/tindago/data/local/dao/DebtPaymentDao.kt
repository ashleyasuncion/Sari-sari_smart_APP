package com.example.tindago.data.local.dao

import androidx.room.*
import com.example.tindago.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtPaymentDao {
    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY timestamp ASC")
    fun getPaymentsByDebtId(debtId: Int): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY timestamp ASC")
    suspend fun getPaymentsByDebtIdList(debtId: Int): List<DebtPaymentEntity>

    @Query("SELECT * FROM debt_payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<DebtPaymentEntity>>

    @Query("SELECT MAX(timestamp) FROM debt_payments WHERE debtId = :debtId")
    suspend fun getLatestPaymentTimestamp(debtId: Int): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: DebtPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<DebtPaymentEntity>)

    @Query("DELETE FROM debt_payments WHERE debtId = :debtId")
    suspend fun deletePaymentsByDebtId(debtId: Int)

    @Query("DELETE FROM debt_payments")
    suspend fun deleteAll()
}
