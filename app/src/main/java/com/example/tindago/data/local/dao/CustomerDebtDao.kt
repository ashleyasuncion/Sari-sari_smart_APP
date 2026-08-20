package com.example.tindago.data.local.dao

import androidx.room.*
import com.example.tindago.data.local.entity.CustomerDebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDebtDao {
    @Query("SELECT * FROM customer_debts ORDER BY id DESC")
    fun getAllDebts(): Flow<List<CustomerDebtEntity>>

    @Query("SELECT * FROM customer_debts WHERE id = :id")
    suspend fun getDebtById(id: Int): CustomerDebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: CustomerDebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<CustomerDebtEntity>)

    @Update
    suspend fun updateDebt(debt: CustomerDebtEntity)

    @Query("DELETE FROM customer_debts WHERE id = :id")
    suspend fun deleteDebt(id: Int)

    @Query("DELETE FROM customer_debts")
    suspend fun deleteAll()

    @Query("SELECT DISTINCT customerName FROM customer_debts")
    suspend fun getUsedCustomerNames(): List<String>
}
