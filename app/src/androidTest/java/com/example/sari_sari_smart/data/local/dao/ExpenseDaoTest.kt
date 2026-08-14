package com.example.sari_sari_smart.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sari_sari_smart.data.local.AppDatabase
import com.example.sari_sari_smart.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ExpenseDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.expenseDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetAllExpenses() = runBlocking {
        val e1 = ExpenseEntity(1, "2026-08-09", "utilities", 150.5, "electric bill", 100L)
        val e2 = ExpenseEntity(2, "2026-08-10", "rent", 300.0, "", 200L)
        dao.insert(e1)
        dao.insert(e2)

        val all = dao.getAllExpenses().first()
        assertEquals(2, all.size)
        // Ordered date DESC → e2 (08-10) first
        assertEquals(2, all[0].id)
        assertEquals(1, all[1].id)
        assertEquals("electric bill", all[1].note)
    }

    @Test
    fun getExpensesByDate_filtersByDate() = runBlocking {
        dao.insert(ExpenseEntity(1, "2026-08-09", "utilities", 100.0, "", 100L))
        dao.insert(ExpenseEntity(2, "2026-08-10", "rent", 200.0, "", 200L))

        val day9 = dao.getExpensesByDate("2026-08-09")
        assertEquals(1, day9.size)
        assertEquals(100.0, day9[0].amount, 0.001)
    }

    @Test
    fun deleteById_removesOnlyThatExpense() = runBlocking {
        dao.insert(ExpenseEntity(1, "2026-08-09", "utilities", 100.0, "", 100L))
        dao.insert(ExpenseEntity(2, "2026-08-10", "rent", 200.0, "", 200L))
        dao.deleteById(1)

        val all = dao.getAllExpenses().first()
        assertEquals(1, all.size)
        assertEquals(2, all[0].id)
    }

    @Test
    fun deleteAll_clearsLog() = runBlocking {
        dao.insert(ExpenseEntity(1, "2026-08-09", "utilities", 100.0, "", 100L))
        dao.deleteAll()
        assertTrue(dao.getAllExpenses().first().isEmpty())
    }
}
