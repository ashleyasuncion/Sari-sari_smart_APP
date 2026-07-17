package com.example.sari_sari_smart.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sari_sari_smart.data.local.AppDatabase
import com.example.sari_sari_smart.data.local.entity.DailyEntryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyEntryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DailyEntryDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.dailyEntryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetLatestEntry() = runBlocking {
        val entry = DailyEntryEntity("2026-07-05", 500.0, 1200.0)
        dao.insertEntry(entry)

        val loaded = dao.getLatestEntry().first()
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals("2026-07-05", it.date)
            assertEquals(500.0, it.stockExpenses, 0.001)
            assertEquals(1200.0, it.earnings, 0.001)
        }
    }

    @Test
    fun getLatestEntry_ReturnsMostRecent() = runBlocking {
        val older = DailyEntryEntity("2026-07-04", 300.0, 800.0)
        val newer = DailyEntryEntity("2026-07-05", 500.0, 1200.0)
        dao.insertEntry(older)
        dao.insertEntry(newer)

        val loaded = dao.getLatestEntry().first()
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals("2026-07-05", it.date)
            assertEquals(1200.0, it.earnings, 0.001)
        }
    }

    @Test
    fun getLatestEntry_ReturnsNullWhenEmpty() = runBlocking {
        val loaded = dao.getLatestEntry().first()
        assertNull(loaded)
    }

    @Test
    fun insertAndGetEntryByDate() = runBlocking {
        val entry = DailyEntryEntity("2026-07-05", 500.0, 1200.0)
        dao.insertEntry(entry)

        val loaded = dao.getEntryByDate("2026-07-05")
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals("2026-07-05", it.date)
            assertEquals(500.0, it.stockExpenses, 0.001)
            assertEquals(1200.0, it.earnings, 0.001)
        }
    }

    @Test
    fun getEntryByDate_NonExistentDate() = runBlocking {
        val entry = DailyEntryEntity("2026-07-05", 500.0, 1200.0)
        dao.insertEntry(entry)

        val loaded = dao.getEntryByDate("2026-07-06")
        assertNull(loaded)
    }

    @Test
    fun deleteEntry() = runBlocking {
        val entry = DailyEntryEntity("2026-07-05", 500.0, 1200.0)
        dao.insertEntry(entry)
        assertNotNull(dao.getEntryByDate("2026-07-05"))

        dao.deleteEntry("2026-07-05")
        assertNull(dao.getEntryByDate("2026-07-05"))
    }

    @Test
    fun deleteAll() = runBlocking {
        val entries = listOf(
            DailyEntryEntity("2026-07-04", 300.0, 800.0),
            DailyEntryEntity("2026-07-05", 500.0, 1200.0)
        )
        for (entry in entries) dao.insertEntry(entry)
        assertNotNull(dao.getLatestEntry().first())

        dao.deleteAll()
        assertNull(dao.getLatestEntry().first())
    }

    @Test
    fun insertReplacesExistingOnConflict() = runBlocking {
        val original = DailyEntryEntity("2026-07-05", 500.0, 1200.0)
        dao.insertEntry(original)

        val replacement = DailyEntryEntity("2026-07-05", 600.0, 1500.0)
        dao.insertEntry(replacement)

        val loaded = dao.getEntryByDate("2026-07-05")
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals(600.0, it.stockExpenses, 0.001)
            assertEquals(1500.0, it.earnings, 0.001)
        }
    }

    @Test
    fun getEntryByDate_PreservesAllFields() = runBlocking {
        val entry = DailyEntryEntity("2026-07-05", 500.0, 1200.0)
        dao.insertEntry(entry)

        val loaded = dao.getEntryByDate("2026-07-05")
        assertNotNull(loaded)
        loaded!!.let {
            assertEquals("2026-07-05", it.date)
            assertEquals(500.0, it.stockExpenses, 0.001)
            assertEquals(1200.0, it.earnings, 0.001)
        }
    }
}
