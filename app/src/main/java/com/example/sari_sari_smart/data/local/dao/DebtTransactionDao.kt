package com.example.sari_sari_smart.data.local.dao

import androidx.room.*
import com.example.sari_sari_smart.data.local.entity.DebtTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtTransactionDao {
    @Query("SELECT * FROM debt_transactions WHERE debtId = :debtId ORDER BY timestamp ASC")
    fun getTransactionsByDebtId(debtId: Int): Flow<List<DebtTransactionEntity>>

    @Query("SELECT * FROM debt_transactions ORDER BY timestamp ASC")
    fun getAllTransactions(): Flow<List<DebtTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: DebtTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(txs: List<DebtTransactionEntity>)

    @Query("DELETE FROM debt_transactions WHERE debtId = :debtId")
    suspend fun deleteTransactionsByDebtId(debtId: Int)

    @Query("DELETE FROM debt_transactions")
    suspend fun deleteAll()
}
