package com.example.sari_sari_smart.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sari_sari_smart.data.local.AppDatabase
import com.example.sari_sari_smart.data.local.entity.CustomerDebtEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerDebtDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CustomerDebtDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.customerDebtDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetDebtById() = runBlocking {
        val debt = CustomerDebtEntity(1, "Aling Maria", 150.0, 150.0, "Yesterday")
        dao.insertDebt(debt)

        val loaded = dao.getDebtById(1)
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals("Aling Maria", it.customerName)
            assertEquals(150.0, it.amount, 0.001)
            assertEquals(150.0, it.remainingBalance, 0.001)
            assertEquals("Yesterday", it.lastActivity)
        }
    }

    @Test
    fun insertAndGetAllDebts() = runBlocking {
        val debt1 = CustomerDebtEntity(1, "Aling Maria", 150.0, 150.0, "Yesterday")
        val debt2 = CustomerDebtEntity(2, "Mang Jose", 75.0, 40.0, "3 days ago")
        dao.insertDebt(debt1)
        dao.insertDebt(debt2)

        val allDebts = dao.getAllDebts().first()
        assertEquals(2, allDebts.size)
        assertTrue(allDebts.any { it.customerName == "Aling Maria" })
        assertTrue(allDebts.any { it.customerName == "Mang Jose" })
    }

    @Test
    fun getAllDebts_Empty() = runBlocking {
        val allDebts = dao.getAllDebts().first()
        assertTrue(allDebts.isEmpty())
    }

    @Test
    fun getDebtById_NonExistent() = runBlocking {
        val loaded = dao.getDebtById(999)
        assertNull(loaded)
    }

    @Test
    fun updateDebt() = runBlocking {
        val debt = CustomerDebtEntity(1, "Aling Maria", 150.0, 150.0, "Yesterday")
        dao.insertDebt(debt)

        val updated = debt.copy(remainingBalance = 100.0, lastActivity = "Today")
        dao.updateDebt(updated)

        val loaded = dao.getDebtById(1)
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals(100.0, it.remainingBalance, 0.001)
            assertEquals("Today", it.lastActivity)
        }
    }

    @Test
    fun updateDebt_PartialPayment() = runBlocking {
        val debt = CustomerDebtEntity(1, "Mang Jose", 75.0, 75.0, "Today")
        dao.insertDebt(debt)

        val afterPayment = debt.copy(remainingBalance = 50.0, lastActivity = "Today")
        dao.updateDebt(afterPayment)

        val loaded = dao.getDebtById(1)
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals(50.0, it.remainingBalance, 0.001)
        }
    }

    @Test
    fun updateDebt_FullySettled() = runBlocking {
        val debt = CustomerDebtEntity(1, "Bryan", 50.0, 50.0, "Last week")
        dao.insertDebt(debt)

        val settled = debt.copy(remainingBalance = 0.0, lastActivity = "Settled")
        dao.updateDebt(settled)

        val loaded = dao.getDebtById(1)
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals(0.0, it.remainingBalance, 0.001)
            assertEquals("Settled", it.lastActivity)
        }
    }

    @Test
    fun deleteDebt() = runBlocking {
        val debt = CustomerDebtEntity(1, "Aling Maria", 150.0, 150.0, "Yesterday")
        dao.insertDebt(debt)
        assertNotNull(dao.getDebtById(1))

        dao.deleteDebt(1)
        assertNull(dao.getDebtById(1))
    }

    @Test
    fun deleteAll() = runBlocking {
        val debts = listOf(
            CustomerDebtEntity(1, "Aling Maria", 150.0, 150.0, "Yesterday"),
            CustomerDebtEntity(2, "Mang Jose", 75.0, 40.0, "3 days ago")
        )
        for (d in debts) dao.insertDebt(d)
        assertEquals(2, dao.getAllDebts().first().size)

        dao.deleteAll()
        assertTrue(dao.getAllDebts().first().isEmpty())
    }

    @Test
    fun insertDebts_BulkInsert() = runBlocking {
        val debts = listOf(
            CustomerDebtEntity(1, "Aling Maria", 150.0, 150.0, "Yesterday"),
            CustomerDebtEntity(2, "Mang Jose", 75.0, 40.0, "3 days ago"),
            CustomerDebtEntity(3, "Kathryn", 200.0, 200.0, "Today")
        )
        dao.insertDebts(debts)

        val allDebts = dao.getAllDebts().first()
        assertEquals(3, allDebts.size)
    }

    @Test
    fun getUsedCustomerNames() = runBlocking {
        val debts = listOf(
            CustomerDebtEntity(1, "Aling Maria", 150.0, 150.0, "Yesterday"),
            CustomerDebtEntity(2, "Mang Jose", 75.0, 40.0, "3 days ago"),
            CustomerDebtEntity(3, "Aling Maria", 50.0, 50.0, "Today")
        )
        for (d in debts) dao.insertDebt(d)

        val names = dao.getUsedCustomerNames()
        assertEquals(2, names.size)
        assertTrue(names.contains("Aling Maria"))
        assertTrue(names.contains("Mang Jose"))
    }

    @Test
    fun getUsedCustomerNames_Empty() = runBlocking {
        val names = dao.getUsedCustomerNames()
        assertTrue(names.isEmpty())
    }
}
