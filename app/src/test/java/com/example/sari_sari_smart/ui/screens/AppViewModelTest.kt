package com.example.sari_sari_smart.ui.screens

import com.example.sari_sari_smart.data.CustomerDebt
import com.example.sari_sari_smart.data.SpecificSale
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
 * getClosingProfit, completeEndOfDay.
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
    fun getClosingProfit_returnsDifferenceBetweenActualAndCost() {
        val profit = viewModel.getClosingProfit(500.0, 300.0)
        assertEquals(200.0, profit, 0.001)
    }

    @Test
    fun getClosingProfit_returnsNegative_whenCostExceedsActual() {
        val profit = viewModel.getClosingProfit(200.0, 350.0)
        assertEquals(-150.0, profit, 0.001)
    }

    @Test
    fun getClosingProfit_returnsZero_whenEqual() {
        val profit = viewModel.getClosingProfit(300.0, 300.0)
        assertEquals(0.0, profit, 0.001)
    }

    @Test
    fun completeEndOfDay_storesNewFieldsCorrectly() {
        val today = viewModel.today
        // Add a cash sale = recorded sales (per-sale profit 50)
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Item", amount = 150.0, quantity = 1, customerName = null, profit = 50.0)
        )
        // Complete day with actual sales = 200, cost of goods = 80
        viewModel.completeEndOfDay(actualSales = 200.0, costOfGoods = 80.0)

        val eod = viewModel.endOfDayData.value
        assertNotNull(eod)
        eod!!.let {
            assertEquals(today, it.date)
            assertEquals(150.0, it.recordedSales, 0.001)  // 150 cash sale
            assertEquals(200.0, it.actualSales, 0.001)
            assertEquals(50.0, it.salesDiff, 0.001)       // 200 - 150
            assertEquals(80.0, it.costOfGoods, 0.001)
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
        assertEquals(0.0, eod.costOfGoods, 0.001)
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
        // recorded = 0 (no cash sales), actual = 250, cost = 100
        viewModel.completeEndOfDay(actualSales = 250.0, costOfGoods = 100.0)

        val eod = viewModel.endOfDayData.value
        assertNotNull(eod)
        assertEquals(0.0, eod!!.recordedSales, 0.001)     // No cash sales
        assertEquals(250.0, eod.actualSales, 0.001)
        assertEquals(250.0, eod.salesDiff, 0.001)          // 250 - 0
        assertEquals(100.0, eod.costOfGoods, 0.001)
        assertEquals(40.0, eod.profit, 0.001)              // per-sale profit (todayProfit parity)
    }

    @Test
    fun completeEndOfDay_updatesExternallyVisibleState() {
        val today = viewModel.today
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Cash Sale", amount = 80.0, quantity = 1, customerName = null)
        )
        viewModel.completeEndOfDay(actualSales = 100.0, costOfGoods = 40.0)

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
}
