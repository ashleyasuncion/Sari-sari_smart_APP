package com.example.sari_sari_smart.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sari_sari_smart.data.local.AppDatabase
import com.example.sari_sari_smart.data.local.entity.EndOfDayEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndOfDayDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: EndOfDayDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.endOfDayDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetLatest() = runBlocking {
        val data = EndOfDayEntity("2026-07-05", 1500.0, true, true, true)
        dao.insert(data)

        val loaded = dao.getLatest().first()
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals("2026-07-05", it.date)
            assertEquals(1500.0, it.cashInDrawer, 0.001)
            assertTrue(it.stockCheckDone)
            assertTrue(it.debtPaymentsDone)
            assertTrue(it.finished)
        }
    }

    @Test
    fun getLatest_ReturnsMostRecent() = runBlocking {
        val older = EndOfDayEntity("2026-07-04", 1200.0, true, true, true)
        val newer = EndOfDayEntity("2026-07-05", 1500.0, true, true, true)
        dao.insert(older)
        dao.insert(newer)

        val loaded = dao.getLatest().first()
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals("2026-07-05", it.date)
            assertEquals(1500.0, it.cashInDrawer, 0.001)
        }
    }

    @Test
    fun getLatest_ReturnsNullWhenEmpty() = runBlocking {
        val loaded = dao.getLatest().first()
        assertNull(loaded)
    }

    @Test
    fun insertAndGetByDate() = runBlocking {
        val data = EndOfDayEntity("2026-07-05", 1500.0, true, true, true)
        dao.insert(data)

        val loaded = dao.getByDate("2026-07-05")
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals("2026-07-05", it.date)
            assertEquals(1500.0, it.cashInDrawer, 0.001)
        }
    }

    @Test
    fun getByDate_NonExistentDate() = runBlocking {
        val data = EndOfDayEntity("2026-07-05", 1500.0, true, true, true)
        dao.insert(data)

        val loaded = dao.getByDate("2026-07-06")
        assertNull(loaded)
    }

    @Test
    fun insertReplacesExistingOnConflict() = runBlocking {
        val original = EndOfDayEntity("2026-07-05", 1500.0, true, false, false)
        dao.insert(original)

        val replacement = EndOfDayEntity("2026-07-05", 2000.0, true, true, true)
        dao.insert(replacement)

        val loaded = dao.getByDate("2026-07-05")
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals(2000.0, it.cashInDrawer, 0.001)
            assertTrue(it.finished)
        }
    }

    @Test
    fun deleteByDate() = runBlocking {
        val data = EndOfDayEntity("2026-07-05", 1500.0, true, true, true)
        dao.insert(data)
        assertNotNull(dao.getByDate("2026-07-05"))

        dao.deleteByDate("2026-07-05")
        assertNull(dao.getByDate("2026-07-05"))
    }

    @Test
    fun deleteByDate_OnlyRemovesSpecifiedDate() = runBlocking {
        val data1 = EndOfDayEntity("2026-07-04", 1200.0, true, true, true)
        val data2 = EndOfDayEntity("2026-07-05", 1500.0, true, true, true)
        dao.insert(data1)
        dao.insert(data2)

        dao.deleteByDate("2026-07-04")

        assertNull(dao.getByDate("2026-07-04"))
        assertNotNull(dao.getByDate("2026-07-05"))
    }

    @Test
    fun deleteAll() = runBlocking {
        val data1 = EndOfDayEntity("2026-07-04", 1200.0, true, true, true)
        val data2 = EndOfDayEntity("2026-07-05", 1500.0, true, true, true)
        dao.insert(data1)
        dao.insert(data2)
        assertNotNull(dao.getLatest().first())

        dao.deleteAll()
        assertNull(dao.getLatest().first())
    }

    @Test
    fun preservesBooleanFlags() = runBlocking {
        val data = EndOfDayEntity("2026-07-05", 1500.0, true, false, true)
        dao.insert(data)

        val loaded = dao.getByDate("2026-07-05")
        assertNotNull(loaded)
        loaded!!.let {
            assertTrue(it.stockCheckDone)
            assertFalse(it.debtPaymentsDone)
            assertTrue(it.finished)
        }
    }

    @Test
    fun defaultValuesForNewEntry() = runBlocking {
        val data = EndOfDayEntity("2026-07-05")
        dao.insert(data)

        val loaded = dao.getByDate("2026-07-05")
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals(0.0, it.cashInDrawer, 0.001)
            assertFalse(it.stockCheckDone)
            assertFalse(it.debtPaymentsDone)
            assertFalse(it.finished)
        }
    }
}
