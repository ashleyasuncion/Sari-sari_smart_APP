package com.example.sari_sari_smart.ui.screens

import com.example.sari_sari_smart.data.CustomerDebt
import com.example.sari_sari_smart.data.SpecificSale
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AppViewModel computed properties and methods.
 * Tests the closing restructuring logic: todayRecordedSales, getSalesDiff,
 * completeEndOfDay.
 */
class AppViewModelTest {

    private lateinit var viewModel: AppViewModel

    // AppViewModel's init launches a midnight ticker on Dispatchers.Main, which
    // does not exist in JVM unit tests — provide a test Main dispatcher.
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AppViewModel()
        // Clear any seed data
        viewModel.resetAllData()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun todayRecordedSales_returnsZero_whenNoSales() {
        assertEquals(0.0, viewModel.todayRecordedSales, 0.001)
    }

    @Test
    fun todayRecordedSales_excludesUtangSales() {
        val today = viewModel.today
        // Add a cash sale
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Item A", amount = 100.0, quantity = 1, customerName = null)
        )
        // Add a utang sale
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Item B", amount = 50.0, quantity = 1, customerName = "Juan")
        )
        // Should only count cash sales (100.0)
        assertEquals(100.0, viewModel.todayRecordedSales, 0.001)
    }

    @Test
    fun todayRecordedSales_countsOnlyTodaySales() {
        val today = viewModel.today
        val yesterday = "2026-01-01" // Different date
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Today Item", amount = 75.0, quantity = 1, customerName = null)
        )
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = yesterday, description = "Old Item", amount = 30.0, quantity = 1, customerName = null)
        )
        assertEquals(75.0, viewModel.todayRecordedSales, 0.001)
    }

    @Test
    fun getSalesDiff_returnsPositive_whenActualExceedsRecorded() {
        // No sales today, so recorded sales = 0
        // Actual sales = 500
        val diff = viewModel.getSalesDiff(500.0)
        assertEquals(500.0, diff, 0.001)
    }

    @Test
    fun getSalesDiff_returnsNegative_whenRecordedExceedsActual() {
        val today = viewModel.today
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Item", amount = 200.0, quantity = 1, customerName = null)
        )
        // Recorded = 200, Actual = 150
        val diff = viewModel.getSalesDiff(150.0)
        assertEquals(-50.0, diff, 0.001)
    }

    @Test
    fun getSalesDiff_returnsZero_whenEqual() {
        val today = viewModel.today
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Item", amount = 100.0, quantity = 1, customerName = null)
        )
        assertEquals(0.0, viewModel.getSalesDiff(100.0), 0.001)
    }

    @Test
    fun completeEndOfDay_storesNewFieldsCorrectly() {
        val today = viewModel.today
        // Add a cash sale = recorded sales (per-sale profit 50)
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Item", amount = 150.0, quantity = 1, customerName = null, profit = 50.0)
        )
        // Complete day with actual sales = 200
        viewModel.completeEndOfDay(actualSales = 200.0)

        val eod = viewModel.endOfDayData.value
        assertNotNull(eod)
        eod!!.let {
            assertEquals(today, it.date)
            assertEquals(150.0, it.recordedSales, 0.001)  // 150 cash sale
            assertEquals(200.0, it.actualSales, 0.001)
            assertEquals(50.0, it.salesDiff, 0.001)       // 200 - 150
            assertEquals(50.0, it.profit, 0.001)          // per-sale profit (todayProfit parity)
            assertTrue(it.finished)
        }
    }

    @Test
    fun completeEndOfDay_worksWithDefaults() {
        // With no daily entry, defaults to zeros
        viewModel.completeEndOfDay()
        val eod = viewModel.endOfDayData.value
        assertNotNull(eod)
        assertEquals(0.0, eod!!.recordedSales, 0.001)
        assertEquals(0.0, eod.actualSales, 0.001)
        assertEquals(0.0, eod.salesDiff, 0.001)
        assertEquals(0.0, eod.profit, 0.001)
        assertTrue(eod.finished)
    }

    @Test
    fun completeEndOfDay_withUtangSalesOnly_hasZeroRecordedSales() {
        val today = viewModel.today
        // Add only utang sales (per-sale profit 40)
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Utang Item", amount = 200.0, quantity = 1, customerName = "Pedro", profit = 40.0)
        )
        // recorded = 0 (no cash sales), actual = 250
        viewModel.completeEndOfDay(actualSales = 250.0)

        val eod = viewModel.endOfDayData.value
        assertNotNull(eod)
        assertEquals(0.0, eod!!.recordedSales, 0.001)     // No cash sales
        assertEquals(250.0, eod.actualSales, 0.001)
        assertEquals(250.0, eod.salesDiff, 0.001)          // 250 - 0
        assertEquals(40.0, eod.profit, 0.001)              // per-sale profit (todayProfit parity)
    }

    @Test
    fun completeEndOfDay_updatesExternallyVisibleState() {
        val today = viewModel.today
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Cash Sale", amount = 80.0, quantity = 1, customerName = null)
        )
        viewModel.completeEndOfDay(actualSales = 100.0)

        // Verify the state is externally visible via isEodComplete
        assertTrue(viewModel.isEodComplete)

        // Verify eodData is properly stored
        val eod = viewModel.endOfDayData.value
        assertNotNull(eod)
        assertEquals(80.0, eod!!.recordedSales, 0.001)
    }

    // ── Debt transaction ledger (web transactions[] parity) ──────────────

    @Test
    fun addDebt_returnsDebtWithAssignedId() {
        val debt = viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Aling Nena", amount = 100.0, remainingBalance = 100.0)
        )
        assertTrue(debt.id > 0)
        assertEquals(100.0, viewModel.getDebtById(debt.id)?.remainingBalance ?: 0.0, 0.001)
    }

    @Test
    fun addDebtTransaction_recordsLedgerEntry() {
        val debt = viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Mang Kanor", amount = 50.0, remainingBalance = 50.0)
        )
        viewModel.addDebtTransaction(debt.id, "debt", "Mantika", 50.0)
        val txs = viewModel.getDebtTransactionsForDebt(debt.id)
        assertEquals(1, txs.size)
        assertEquals("Mantika", txs[0].description)
        assertEquals("debt", txs[0].type)
        assertEquals(50.0, txs[0].amount, 0.001)
    }

    @Test
    fun getDebtTransactionsForDebt_filtersByDebt() {
        val d1 = viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "A", amount = 10.0, remainingBalance = 10.0)
        )
        val d2 = viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "B", amount = 20.0, remainingBalance = 20.0)
        )
        viewModel.addDebtTransaction(d1.id, "debt", "Manual", 10.0)
        viewModel.addDebtTransaction(d2.id, "debt", "Manual", 20.0)
        assertEquals(1, viewModel.getDebtTransactionsForDebt(d1.id).size)
        assertEquals(1, viewModel.getDebtTransactionsForDebt(d2.id).size)
    }

    // ── Reports engine (web v2.55 parity) ────────────────────────────────

    @Test
    fun computeReportStats_day_aggregatesTodayAndComparesToYesterday() {
        assertTrue(viewModel.setDevDateOverride("2026-08-09"))
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = "2026-08-09", description = "Today", amount = 100.0, quantity = 1, customerName = null)
        )
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = "2026-08-08", description = "Yesterday", amount = 50.0, quantity = 1, customerName = null)
        )
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = "2026-08-07", description = "Older", amount = 30.0, quantity = 1, customerName = null)
        )

        val stats = viewModel.computeReportStats("day")
        assertEquals(1, stats.sales.size)
        assertEquals(100.0, stats.sales.sumOf { it.amount }, 0.001)
        assertEquals(50.0, stats.prevSalesTotal, 0.001)
    }

    @Test
    fun computeReportStats_week_includesLast7DaysAndOlderFallsToPrev() {
        assertTrue(viewModel.setDevDateOverride("2026-08-09"))
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = "2026-08-09", description = "A", amount = 100.0, quantity = 1, customerName = null)
        )
        // 08-03 is exactly 6 days before 08-09 → still inside the week window
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = "2026-08-03", description = "B", amount = 50.0, quantity = 1, customerName = null)
        )
        // 08-02 is 7 days before → falls into the PREVIOUS week window
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = "2026-08-02", description = "C", amount = 30.0, quantity = 1, customerName = null)
        )

        val stats = viewModel.computeReportStats("week")
        assertEquals(2, stats.sales.size)
        assertEquals(150.0, stats.sales.sumOf { it.amount }, 0.001)
        assertEquals(30.0, stats.prevSalesTotal, 0.001)
    }

    @Test
    fun computeReportStats_splitsCashVsUtangSales() {
        assertTrue(viewModel.setDevDateOverride("2026-08-09"))
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = "2026-08-09", description = "Cash", amount = 100.0, quantity = 1, customerName = null)
        )
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = "2026-08-09", description = "Utang", amount = 40.0, quantity = 1, customerName = "Juan")
        )

        val stats = viewModel.computeReportStats("day")
        assertEquals(100.0, stats.sales.filter { it.customerName == null }.sumOf { it.amount }, 0.001)
        assertEquals(40.0, stats.sales.filter { it.customerName != null }.sumOf { it.amount }, 0.001)
    }

    @Test
    fun computeReportStats_agingBuckets_byDebtAge() {
        val dayMs = 24L * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "New", amount = 100.0, remainingBalance = 100.0, createdAt = now - 10 * dayMs)
        )
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Mid", amount = 200.0, remainingBalance = 200.0, createdAt = now - 45 * dayMs)
        )
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Old", amount = 300.0, remainingBalance = 300.0, createdAt = now - 70 * dayMs)
        )

        val stats = viewModel.computeReportStats("day")
        assertEquals(600.0, stats.outstandingUtang, 0.001)
        assertEquals(3, stats.activeDebtors)
        assertEquals(100.0, stats.aging[0].amount, 0.001)
        assertEquals(200.0, stats.aging[1].amount, 0.001)
        assertEquals(300.0, stats.aging[2].amount, 0.001)
        assertEquals(1, stats.aging[0].count)
        assertEquals(1, stats.aging[1].count)
        assertEquals(1, stats.aging[2].count)
    }

    @Test
    fun computeReportStats_countsPaymentsCollectedThisPeriod() {
        assertTrue(viewModel.setDevDateOverride("2026-08-09"))
        val debt = viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Aling Nena", amount = 100.0, remainingBalance = 50.0)
        )
        // Partial payment recorded now → inside the day window
        viewModel.recordDebtPayment(debt.id, 25.0)

        val stats = viewModel.computeReportStats("day")
        assertEquals(25.0, stats.collectedThisPeriod, 0.001)
        // Payment of 25 reduces the balance from 50 → 25
        assertEquals(25.0, stats.outstandingUtang, 0.001)
        assertEquals(1, stats.activeDebtors)
    }

    @Test
    fun exportReportCsv_containsPeriodSummaryAndTransactions() {
        assertTrue(viewModel.setDevDateOverride("2026-08-09"))
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = "2026-08-09", description = "Bread", amount = 100.0, quantity = 2, profit = 20.0, customerName = null)
        )
        val csv = viewModel.exportReportCsv("day")
        // Locale-robust assertions: String.format uses the JVM default locale,
        // so build the expected decimals explicitly (Locale.US) instead of
        // hardcoding "100.00" (which breaks on comma-decimal locales).
        val us = { v: Double -> String.format(Locale.US, "%.2f", v) }
        assertTrue(csv.contains("Sari-Sari Smart - Report (Day)"))
        assertTrue(csv.contains("Total Sales,${us(100.0)}"))
        assertTrue(csv.contains("Items Sold,2"))
        assertTrue(csv.contains("Bread,2,${us(100.0)},${us(20.0)},Cash"))
    }

    // ── Credit-limit engine (web v2.56/v2.57 parity) ────────────────────

    @Test
    fun getDefaultCreditLimit_fallsBackTo500() {
        assertEquals(500, viewModel.getDefaultCreditLimit())
    }

    @Test
    fun getEffectiveCreditLimit_usesDefaultWhenNoCustom() {
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Aling Nena", amount = 480.0, remainingBalance = 480.0)
        )
        assertEquals(500, viewModel.getEffectiveCreditLimit("Aling Nena"))
    }

    @Test
    fun getEffectiveCreditLimit_usesCustomWhenSet() {
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Aling Nena", amount = 480.0, remainingBalance = 480.0, creditLimit = 1000)
        )
        assertEquals(1000, viewModel.getEffectiveCreditLimit("Aling Nena"))
    }

    @Test
    fun getEffectiveCreditLimit_zeroMeansNoLimit() {
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Aling Nena", amount = 480.0, remainingBalance = 480.0, creditLimit = 0)
        )
        assertEquals(0, viewModel.getEffectiveCreditLimit("Aling Nena"))
    }

    @Test
    fun getCreditStatus_blocksAtOrAboveLimit() {
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Aling Nena", amount = 480.0, remainingBalance = 480.0)
        )
        // 480 + 20 = 500 → exactly AT the limit → blocked (v2.57)
        val atLimit = viewModel.getCreditStatus("Aling Nena", 20.0)
        assertTrue(atLimit.overLimit)
        assertTrue(atLimit.atLimit)
        assertFalse(atLimit.nearLimit)
        // 480 + 50 = 530 → over
        val over = viewModel.getCreditStatus("Aling Nena", 50.0)
        assertTrue(over.overLimit)
        assertFalse(over.atLimit)
        // 480 + 10 = 490 → below limit but >= 80% (400) → near
        val near = viewModel.getCreditStatus("Aling Nena", 10.0)
        assertFalse(near.overLimit)
        assertTrue(near.nearLimit)
    }

    @Test
    fun getCreditStatus_floatDriftCannotSlipAtLimitThrough() {
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Float Case", amount = 450.10, remainingBalance = 450.10)
        )
        // 450.10 + 49.90 = 499.99999999999994 in IEEE754 → must still be blocked
        val cs = viewModel.getCreditStatus("Float Case", 49.90)
        assertTrue(cs.overLimit)
    }

    @Test
    fun getCreditStatus_noLimitAllowsAnyAmount() {
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Unlimited", amount = 2000.0, remainingBalance = 2000.0, creditLimit = 0)
        )
        val cs = viewModel.getCreditStatus("Unlimited", 500.0)
        assertFalse(cs.overLimit)
        assertFalse(cs.nearLimit)
    }

    @Test
    fun getCreditStatus_sumsAcrossMultipleDebtsForSameName() {
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Aling Nena", amount = 300.0, remainingBalance = 300.0)
        )
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Aling Nena", amount = 200.0, remainingBalance = 200.0)
        )
        val cs = viewModel.getCreditStatus("Aling Nena", 0.0)
        assertEquals(500.0, cs.balance, 0.001)
        assertTrue(cs.overLimit)
    }

    @Test
    fun updateDebtCreditLimit_setsAndClearsCustomLimit() {
        val debt = viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Mang Kanor", amount = 200.0, remainingBalance = 200.0)
        )
        viewModel.updateDebtCreditLimit(debt.id, 750)
        assertEquals(750, viewModel.getEffectiveCreditLimit("Mang Kanor"))
        // Clear back to default
        viewModel.updateDebtCreditLimit(debt.id, null)
        assertEquals(500, viewModel.getEffectiveCreditLimit("Mang Kanor"))
    }

    @Test
    fun getOverLimitDebtorCount_groupsByNameAtOrAboveLimit() {
        // Nena: 300 + 200 = 500 → exactly AT limit → counts
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Aling Nena", amount = 300.0, remainingBalance = 300.0)
        )
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Aling Nena", amount = 200.0, remainingBalance = 200.0)
        )
        // Kanor: 400 → below limit, not over
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Mang Kanor", amount = 400.0, remainingBalance = 400.0)
        )
        // Pedro: settled → ignored
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Pedro", amount = 600.0, remainingBalance = 0.0)
        )
        // Liza: 600 but no limit (creditLimit = 0) → not counted
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Liza", amount = 600.0, remainingBalance = 600.0, creditLimit = 0)
        )
        assertEquals(1, viewModel.getOverLimitDebtorCount())
    }
}
