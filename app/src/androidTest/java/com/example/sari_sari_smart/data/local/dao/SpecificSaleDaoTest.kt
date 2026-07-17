package com.example.sari_sari_smart.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sari_sari_smart.data.local.AppDatabase
import com.example.sari_sari_smart.data.local.entity.SpecificSaleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpecificSaleDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SpecificSaleDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.specificSaleDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetAllSales() = runBlocking {
        val sale = SpecificSaleEntity(1, "2026-07-05", "Test Sale", 100.0, 2, null, 20.0)
        dao.insertSale(sale)

        val allSales = dao.getAllSales().first()
        assertEquals(1, allSales.size)
        assertEquals("Test Sale", allSales[0].description)
        assertEquals(100.0, allSales[0].amount, 0.001)
        assertEquals(2, allSales[0].quantity)
        assertEquals(20.0, allSales[0].profit, 0.001)
    }

    @Test
    fun getAllSales_ReturnsEmptyWhenNoSales() = runBlocking {
        val allSales = dao.getAllSales().first()
        assertTrue(allSales.isEmpty())
    }

    @Test
    fun insertAndGetByDate() = runBlocking {
        val sale = SpecificSaleEntity(1, "2026-07-05", "Test Sale", 100.0)
        dao.insertSale(sale)

        val salesByDate = dao.getSalesByDate("2026-07-05").first()
        assertEquals(1, salesByDate.size)
        assertEquals("Test Sale", salesByDate[0].description)
    }

    @Test
    fun getByDate_ReturnsMultipleSales() = runBlocking {
        val sale1 = SpecificSaleEntity(1, "2026-07-05", "Sale 1", 50.0)
        val sale2 = SpecificSaleEntity(2, "2026-07-05", "Sale 2", 75.0)
        val sale3 = SpecificSaleEntity(3, "2026-07-06", "Sale 3", 100.0)
        dao.insertSale(sale1)
        dao.insertSale(sale2)
        dao.insertSale(sale3)

        val salesOnDate = dao.getSalesByDate("2026-07-05").first()
        assertEquals(2, salesOnDate.size)
    }

    @Test
    fun getByDate_NonExistentDate() = runBlocking {
        val sale = SpecificSaleEntity(1, "2026-07-05", "Test Sale", 100.0)
        dao.insertSale(sale)

        val salesOnDate = dao.getSalesByDate("2026-07-06").first()
        assertTrue(salesOnDate.isEmpty())
    }

    @Test
    fun getSalesSince_ReturnsSalesFromDate() = runBlocking {
        val sale1 = SpecificSaleEntity(1, "2026-07-04", "Old Sale", 50.0)
        val sale2 = SpecificSaleEntity(2, "2026-07-05", "Recent Sale", 75.0)
        val sale3 = SpecificSaleEntity(3, "2026-07-06", "New Sale", 100.0)
        dao.insertSale(sale1)
        dao.insertSale(sale2)
        dao.insertSale(sale3)

        val salesSince = dao.getSalesSince("2026-07-05")
        assertEquals(2, salesSince.size)
        assertTrue(salesSince.all { it.date >= "2026-07-05" })
    }

    @Test
    fun insertSales_BulkInsert() = runBlocking {
        val sales = listOf(
            SpecificSaleEntity(1, "2026-07-05", "Sale 1", 50.0),
            SpecificSaleEntity(2, "2026-07-05", "Sale 2", 75.0),
            SpecificSaleEntity(3, "2026-07-06", "Sale 3", 100.0)
        )
        dao.insertSales(sales)

        val allSales = dao.getAllSales().first()
        assertEquals(3, allSales.size)
    }

    @Test
    fun deleteSalesByDate() = runBlocking {
        val sale1 = SpecificSaleEntity(1, "2026-07-05", "Sale 1", 50.0)
        val sale2 = SpecificSaleEntity(2, "2026-07-06", "Sale 2", 75.0)
        dao.insertSale(sale1)
        dao.insertSale(sale2)

        dao.deleteSalesByDate("2026-07-05")

        val allSales = dao.getAllSales().first()
        assertEquals(1, allSales.size)
        assertEquals("2026-07-06", allSales[0].date)
    }

    @Test
    fun deleteAll() = runBlocking {
        val sales = listOf(
            SpecificSaleEntity(1, "2026-07-05", "Sale 1", 50.0),
            SpecificSaleEntity(2, "2026-07-06", "Sale 2", 75.0)
        )
        dao.insertSales(sales)
        assertEquals(2, dao.getAllSales().first().size)

        dao.deleteAll()
        assertTrue(dao.getAllSales().first().isEmpty())
    }

    @Test
    fun insertReplacesExistingOnConflict() = runBlocking {
        val original = SpecificSaleEntity(1, "2026-07-05", "Original Sale", 50.0)
        dao.insertSale(original)

        val replacement = SpecificSaleEntity(1, "2026-07-05", "Replaced Sale", 100.0, 3, null, 30.0)
        dao.insertSale(replacement)

        val loaded = dao.getAllSales().first()
        assertEquals(1, loaded.size)
        assertEquals("Replaced Sale", loaded[0].description)
        assertEquals(100.0, loaded[0].amount, 0.001)
        assertEquals(3, loaded[0].quantity)
        assertEquals(30.0, loaded[0].profit, 0.001)
    }

    @Test
    fun preservesCustomerNameAndTimestamp() = runBlocking {
        val sale = SpecificSaleEntity(1, "2026-07-05", "Test Sale", 100.0, 1, "Maria", 20.0, 1000L)
        dao.insertSale(sale)

        val loaded = dao.getAllSales().first()
        assertEquals(1, loaded.size)
        assertEquals("Maria", loaded[0].customerName)
        assertEquals(1000L, loaded[0].timestamp)
    }

    @Test
    fun getSalesByDate_OrdersByTimestampDesc() = runBlocking {
        val sale1 = SpecificSaleEntity(1, "2026-07-05", "Older", 50.0, 1, null, 10.0, 100L)
        val sale2 = SpecificSaleEntity(2, "2026-07-05", "Newer", 75.0, 2, null, 15.0, 200L)
        dao.insertSale(sale1)
        dao.insertSale(sale2)

        val salesOnDate = dao.getSalesByDate("2026-07-05").first()
        assertEquals(2, salesOnDate.size)
        // First result should be the one with higher timestamp (newer first)
        assertTrue(salesOnDate[0].timestamp >= salesOnDate[1].timestamp)
    }
}
