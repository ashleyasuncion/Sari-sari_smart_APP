package com.example.tindago.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tindago.data.Expense

/**
 * Room entity for the expense log (web V2.71 parity — ExpenseTracking analysis §10.1).
 * Each row = one store expense: date, category, amount, optional note.
 */
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: Int,
    val date: String,
    val category: String,
    val amount: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): Expense = Expense(
        id = id,
        date = date,
        category = category,
        amount = amount,
        note = note,
        timestamp = timestamp
    )

    companion object {
        fun fromDomainModel(expense: Expense): ExpenseEntity = ExpenseEntity(
            id = expense.id,
            date = expense.date,
            category = expense.category,
            amount = expense.amount,
            note = expense.note,
            timestamp = expense.timestamp
        )
    }
}
