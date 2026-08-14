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

    // ── Web v2.59 parity: units, brands, and categories ─────────────────────

    @Test
    fun addOrUpdateProduct_persistsIdentityFields() {
        viewModel.addOrUpdateProduct(
            "Canned Tuna", 10, 18.0, 25.0,
            category = "canned", brand = "Ligo", unit = "can", packageSize = "155g"
        )
        val p = viewModel.products.value.first()
        assertEquals("canned", p.category)
        assertEquals("Ligo", p.brand)
        assertEquals("can", p.unit)
        assertEquals("155g", p.packageSize)
    }

    @Test
    fun addOrUpdateProduct_updatePathKeepsIdentityFields() {
        viewModel.addOrUpdateProduct(
            "Canned Tuna", 10, 18.0, 25.0,
            category = "canned", brand = "Ligo", unit = "can", packageSize = "155g"
        )
        // Same name → merge path (existing product updated, qty added)
        viewModel.addOrUpdateProduct(
            "Canned Tuna", 5, 20.0, 28.0,
            category = "canned", brand = "Century", unit = "can", packageSize = "155g"
        )
        val p = viewModel.products.value.first()
        assertEquals(15, p.quantity)
        assertEquals("Century", p.brand) // identity updated on the merge path too
        assertEquals(20.0, p.costPrice, 0.001)
    }

    @Test
    fun searchProducts_matchesCategoryLabel_enAndFil() {
        viewModel.addOrUpdateProduct(
            "Asin", 10, 10.0, 15.0, category = "condiments", unit = "sachet"
        )
        // English label
        assertTrue(viewModel.searchProducts("condiments").isNotEmpty())
        // Filipino label
        assertTrue(viewModel.searchProducts("pampalasa").isNotEmpty())
    }

    @Test
    fun searchProducts_matchesBrand() {
        viewModel.addOrUpdateProduct(
            "Canned Tuna", 10, 18.0, 25.0, category = "canned", brand = "Ligo", packageSize = "155g"
        )
        assertTrue(viewModel.searchProducts("ligo").isNotEmpty())
        assertTrue(viewModel.searchProducts("155g").isNotEmpty())
    }

    @Test
    fun searchProducts_matchesUnitKeyAndLabel() {
        viewModel.addOrUpdateProduct(
            "Toyo", 15, 12.0, 18.0, category = "condiments", unit = "bottle", packageSize = "350mL"
        )
        // Unit key
        assertTrue(viewModel.searchProducts("bottle").isNotEmpty())
        // Unit EN label is same as key for bottle; Filipino label
        assertTrue(viewModel.searchProducts("bote").isNotEmpty())
    }

    @Test
    fun searchProducts_emptyQueryReturnsAll() {
        viewModel.addOrUpdateProduct("Item A", 1, 1.0, 2.0)
        viewModel.addOrUpdateProduct("Item B", 1, 1.0, 2.0)
        assertEquals(2, viewModel.searchProducts("").size)
    }

    @Test
    fun getUsedBrands_returnsDistinctSortedBrands() {
        viewModel.addOrUpdateProduct("A", 1, 1.0, 2.0, brand = "Zebra")
        viewModel.addOrUpdateProduct("B", 1, 1.0, 2.0, brand = "Alpha")
        viewModel.addOrUpdateProduct("C", 1, 1.0, 2.0, brand = "Zebra") // duplicate
        viewModel.addOrUpdateProduct("D", 1, 1.0, 2.0, brand = "")      // blank ignored
        assertEquals(listOf("Alpha", "Zebra"), viewModel.getUsedBrands())
    }

    @Test
    fun getUsedPackageSizes_returnsDistinctSortedSizes() {
        viewModel.addOrUpdateProduct("A", 1, 1.0, 2.0, packageSize = "155g")
        viewModel.addOrUpdateProduct("B", 1, 1.0, 2.0, packageSize = "1L")
        viewModel.addOrUpdateProduct("C", 1, 1.0, 2.0, packageSize = "155g")
        viewModel.addOrUpdateProduct("D", 1, 1.0, 2.0, packageSize = "")
        assertEquals(listOf("155g", "1L"), viewModel.getUsedPackageSizes())
    }

    @Test
    fun getProductsByCategory_filtersAndAllReturnsEverything() {
        viewModel.addOrUpdateProduct("A", 1, 1.0, 2.0, category = "canned")
        viewModel.addOrUpdateProduct("B", 1, 1.0, 2.0, category = "food")
        viewModel.addOrUpdateProduct("C", 1, 1.0, 2.0, category = "")
        assertEquals(3, viewModel.getProductsByCategory("").size)
        assertEquals(1, viewModel.getProductsByCategory("canned").size)
        assertEquals(0, viewModel.getProductsByCategory("condiments").size)
    }

    @Test
    fun productSubline_returnsEmptyWhenNoBrand() {
        val p = com.example.sari_sari_smart.data.Product(
            id = 1, name = "Tosino", quantity = 100, costPrice = 10.0, sellingPrice = 15.0,
            category = "food", unit = "g", packageSize = "10"
        )
        // Brand empty → no subline; package size must NEVER leak into the brand slot
        assertEquals("", com.example.sari_sari_smart.ui.localization.Strings.productSubline(p, "en"))
    }

    @Test
    fun productSubline_rendersBrandWithSize() {
        val p = com.example.sari_sari_smart.data.Product(
            id = 1, name = "Canned Tuna", quantity = 30, costPrice = 18.0, sellingPrice = 25.0,
            category = "canned", brand = "Ligo", unit = "can", packageSize = "155g"
        )
        assertEquals("Ligo \u00b7 155g", com.example.sari_sari_smart.ui.localization.Strings.productSubline(p, "en"))
    }

    @Test
    fun productSubline_fallsBackToUnitLabelWhenNoSize() {
        val p = com.example.sari_sari_smart.data.Product(
            id = 1, name = "Toyo", quantity = 15, costPrice = 12.0, sellingPrice = 18.0,
            category = "condiments", brand = "Silver Swan", unit = "bottle", packageSize = ""
        )
        assertEquals("Silver Swan \u00b7 bottle", com.example.sari_sari_smart.ui.localization.Strings.productSubline(p, "en"))
    }

    @Test
    fun productSubline_omitsUnitWhenPiece() {
        val p = com.example.sari_sari_smart.data.Product(
            id = 1, name = "Sabon", quantity = 2, costPrice = 10.0, sellingPrice = 16.0,
            category = "personal_care", brand = "Safeguard", unit = "piece", packageSize = ""
        )
        assertEquals("Safeguard", com.example.sari_sari_smart.ui.localization.Strings.productSubline(p, "en"))
    }

    @Test
    fun seedSampleData_has17ProductsWithIdentityFields() {
        viewModel.seedSampleData()
        val products = viewModel.products.value
        assertEquals(17, products.size)
        // All 9 categories covered
        val categories = products.map { it.category }.filter { it.isNotBlank() }.toSet()
        assertEquals(9, categories.size)
        // Brand/no-brand mix present
        val withBrand = products.count { it.brand.isNotBlank() }
        assertTrue(withBrand > 0)
        assertTrue(products.size - withBrand > 0)
        // Canned Tuna has a full identity
        val tuna = products.first { it.name == "Canned Tuna" }
        assertEquals("Ligo", tuna.brand)
        assertEquals("can", tuna.unit)
        assertEquals("155g", tuna.packageSize)
        // Low-stock threshold set on SOME, omitted on others (global fallback)
        val withThreshold = products.count { it.lowStockThreshold != 5 }
        assertTrue(withThreshold in 1 until products.size)
    }

    // ── Multi-item checkout cart (web v2.63/v2.64 parity) ────────────────

    @Test
    fun addToCart_addsNewLine() {
        viewModel.addOrUpdateProduct("Canned Tuna", 30, 18.0, 25.0)
        val p = viewModel.products.value.first()
        assertTrue(viewModel.addToCart(p, 2))
        assertEquals(1, viewModel.saleCart.value.size)
        assertEquals(2, viewModel.saleCart.value[0].qty)
        assertEquals(50.0, viewModel.getCartTotal(), 0.001)
    }

    @Test
    fun addToCart_mergesSameProductAndClampsToStock() {
        viewModel.addOrUpdateProduct("Canned Tuna", 5, 18.0, 25.0)
        val p = viewModel.products.value.first()
        viewModel.addToCart(p, 3)
        viewModel.addToCart(p, 3) // 3+3 = 6 > stock 5 → clamped to 5
        assertEquals(1, viewModel.saleCart.value.size)
        assertEquals(5, viewModel.saleCart.value[0].qty)
        assertEquals(125.0, viewModel.getCartTotal(), 0.001)
    }

    @Test
    fun addToCart_rejectsOutOfStock() {
        viewModel.addOrUpdateProduct("Asin", 0, 10.0, 15.0)
        val p = viewModel.products.value.first()
        assertFalse(viewModel.addToCart(p, 1))
        assertTrue(viewModel.saleCart.value.isEmpty())
    }

    @Test
    fun cartAdjustQty_and_RemoveLine() {
        viewModel.addOrUpdateProduct("A", 10, 1.0, 2.0)
        viewModel.addOrUpdateProduct("B", 10, 1.0, 3.0)
        val a = viewModel.products.value.first { it.name == "A" }
        val b = viewModel.products.value.first { it.name == "B" }
        viewModel.addToCart(a, 2)
        viewModel.addToCart(b, 1)
        viewModel.cartAdjustQty(a.id, 1)
        assertEquals(3, viewModel.saleCart.value.first { it.productId == a.id }.qty)
        viewModel.cartAdjustQty(a.id, -1)
        assertEquals(2, viewModel.saleCart.value.first { it.productId == a.id }.qty)
        viewModel.cartSetQty(b.id, 4)
        assertEquals(4, viewModel.saleCart.value.first { it.productId == b.id }.qty)
        viewModel.cartRemoveLine(a.id)
        assertEquals(1, viewModel.saleCart.value.size)
        assertTrue(viewModel.saleCart.value.none { it.productId == a.id })
    }

    @Test
    fun completeSale_cash_recordsAllLinesWithSharedTransactionId() {
        viewModel.addOrUpdateProduct("A", 10, 1.0, 2.0)
        viewModel.addOrUpdateProduct("B", 10, 1.0, 3.0)
        val a = viewModel.products.value.first { it.name == "A" }
        val b = viewModel.products.value.first { it.name == "B" }
        viewModel.addToCart(a, 2)
        viewModel.addToCart(b, 3)
        viewModel.setSalePayment("cash")

        assertTrue(viewModel.completeSale())
        assertEquals(2, viewModel.specificSales.value.size)
        val txIds = viewModel.specificSales.value.map { it.transactionId }.distinct()
        assertEquals(1, txIds.size) // one shared transaction
        assertTrue(txIds[0] > 0)
        assertTrue(viewModel.specificSales.value.all { it.paymentMethod == "cash" })
        assertTrue(viewModel.specificSales.value.all { it.customerName == null })
        // Stock deducted
        assertEquals(8, viewModel.getProductById(a.id)?.quantity)
        assertEquals(7, viewModel.getProductById(b.id)?.quantity)
        // Cart cleared
        assertTrue(viewModel.saleCart.value.isEmpty())
    }

    @Test
    fun completeSale_credit_createsSingleDebtWithPerLineLedger() {
        viewModel.addOrUpdateProduct("A", 10, 1.0, 2.0)
        viewModel.addOrUpdateProduct("B", 10, 1.0, 3.0)
        val a = viewModel.products.value.first { it.name == "A" }
        val b = viewModel.products.value.first { it.name == "B" }
        viewModel.addToCart(a, 2) // 4.00
        viewModel.addToCart(b, 3) // 9.00 → total 13.00
        viewModel.setSalePayment("credit")

        assertTrue(viewModel.completeSale(customerName = "Juan"))
        // ONE debt record for the whole transaction
        val debt = viewModel.getDebtForName("Juan")
        assertNotNull(debt)
        assertEquals(13.0, debt!!.remainingBalance, 0.001)
        // Per-line ledger entries (one per cart line)
        val txs = viewModel.getDebtTransactionsForDebt(debt.id)
        assertEquals(2, txs.size)
        assertEquals("A", txs[0].description)
        assertEquals(4.0, txs[0].amount, 0.001)
        assertEquals("B", txs[1].description)
        assertEquals(9.0, txs[1].amount, 0.001)
        // Sales rows carry payment method + customer
        assertTrue(viewModel.specificSales.value.all { it.paymentMethod == "credit" })
        assertTrue(viewModel.specificSales.value.all { it.customerName == "Juan" })
    }

    @Test
    fun completeSale_credit_requiresCustomer() {
        viewModel.addOrUpdateProduct("A", 10, 1.0, 2.0)
        viewModel.addToCart(viewModel.products.value.first(), 1)
        viewModel.setSalePayment("credit")
        assertFalse(viewModel.completeSale(customerName = ""))
        assertTrue(viewModel.saleCart.value.isNotEmpty()) // cart kept
        assertTrue(viewModel.specificSales.value.isEmpty())
    }

    @Test
    fun completeSale_credit_blocksAtLimitUnlessForced() {
        // Juan already at 490 with default limit 500
        viewModel.addDebt(
            CustomerDebt(id = 0, customerName = "Juan", amount = 490.0, remainingBalance = 490.0)
        )
        viewModel.addOrUpdateProduct("A", 10, 1.0, 2.0)
        viewModel.addToCart(viewModel.products.value.first(), 10) // 20.00 → 510 > 500
        viewModel.setSalePayment("credit")

        assertFalse(viewModel.completeSale(customerName = "Juan"))
        assertTrue(viewModel.saleCart.value.isNotEmpty())
        // Force (allow anyway) succeeds
        assertTrue(viewModel.completeSale(customerName = "Juan", force = true))
        assertTrue(viewModel.saleCart.value.isEmpty())
    }

    @Test
    fun completeSale_emptyCart_returnsFalse() {
        assertFalse(viewModel.completeSale())
    }

    @Test
    fun salePayment_defaultsToCash() {
        assertEquals("cash", viewModel.salePayment.value)
        viewModel.setSalePayment("credit")
        assertEquals("credit", viewModel.salePayment.value)
        viewModel.setSalePayment("garbage")
        assertEquals("credit", viewModel.salePayment.value) // invalid ignored
    }

    @Test
    fun categoryAndUnitLabels_areLocalized() {
        assertEquals("Condiments", com.example.sari_sari_smart.ui.localization.Strings.productCategoryLabel("condiments", "en"))
        assertEquals("Pampalasa", com.example.sari_sari_smart.ui.localization.Strings.productCategoryLabel("condiments", "fil"))
        assertEquals("piece", com.example.sari_sari_smart.ui.localization.Strings.productUnitLabel("piece", "en"))
        assertEquals("piraso", com.example.sari_sari_smart.ui.localization.Strings.productUnitLabel("piece", "fil"))
        assertEquals("kahon", com.example.sari_sari_smart.ui.localization.Strings.productUnitLabel("box", "fil"))
    }

    // ── Expense Log (web V2.71 parity) ───────────────────────────────────

    @Test
    fun todayExpensesTotal_returnsZero_whenNoExpenses() {
        assertEquals(0.0, viewModel.todayExpensesTotal, 0.001)
        assertEquals(0.0, viewModel.todayNetProfit, 0.001)
    }

    @Test
    fun addExpense_appendsAndTotalsToday() {
        val today = viewModel.today
        val e1 = viewModel.addExpense(date = today, category = "utilities", amount = 150.5, note = "electric bill")
        assertNotNull(e1)
        assertTrue(e1!!.id > 0)
        assertEquals(today, e1.date)
        assertEquals("utilities", e1.category)
        assertEquals(150.5, e1.amount, 0.001)
        assertEquals("electric bill", e1.note)
        assertEquals(1, viewModel.expenses.value.size)
        assertEquals(150.5, viewModel.todayExpensesTotal, 0.001)

        viewModel.addExpense(date = today, category = "rent", amount = 300.0)
        assertEquals(450.5, viewModel.todayExpensesTotal, 0.001)
    }

    @Test
    fun addExpense_validatesAmountAndCategory() {
        // Amount <= 0 → rejected
        assertNull(viewModel.addExpense(date = viewModel.today, category = "rent", amount = 0.0))
        assertNull(viewModel.addExpense(date = viewModel.today, category = "rent", amount = -5.0))
        // Blank category → rejected
        assertNull(viewModel.addExpense(date = viewModel.today, category = "", amount = 100.0))
        assertEquals(0, viewModel.expenses.value.size)
    }

    @Test
    fun addExpense_defaultsBlankDateToToday() {
        val e = viewModel.addExpense(date = "", category = "supplies", amount = 25.0)
        assertNotNull(e)
        assertEquals(viewModel.today, e!!.date)
    }

    @Test
    fun getExpensesTotalFor_filtersByExactDate() {
        viewModel.addExpense(date = "2026-08-09", category = "utilities", amount = 100.0)
        viewModel.addExpense(date = "2026-08-10", category = "rent", amount = 200.0)
        assertEquals(100.0, viewModel.getExpensesTotalFor("2026-08-09"), 0.001)
        assertEquals(200.0, viewModel.getExpensesTotalFor("2026-08-10"), 0.001)
        assertEquals(0.0, viewModel.getExpensesTotalFor("2026-08-11"), 0.001)
    }

    @Test
    fun getPeriodExpensesTotal_sumsOnOrAfterStartDate() {
        viewModel.addExpense(date = "2026-08-01", category = "utilities", amount = 100.0)
        viewModel.addExpense(date = "2026-08-05", category = "rent", amount = 200.0)
        viewModel.addExpense(date = "2026-07-31", category = "other", amount = 50.0)
        assertEquals(300.0, viewModel.getPeriodExpensesTotal("2026-08-01"), 0.001)
        assertEquals(50.0, viewModel.getPeriodExpensesTotal("2026-07-01") - 300.0, 0.001)
    }

    @Test
    fun todayNetProfit_equalsGrossProfitMinusExpenses() {
        val today = viewModel.today
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Item", amount = 200.0, quantity = 1, customerName = null, profit = 80.0)
        )
        viewModel.addExpense(date = today, category = "utilities", amount = 30.0)
        assertEquals(80.0, viewModel.todayProfit, 0.001)
        assertEquals(30.0, viewModel.todayExpensesTotal, 0.001)
        assertEquals(50.0, viewModel.todayNetProfit, 0.001)
    }

    @Test
    fun todayNetProfit_canBeNegative() {
        val today = viewModel.today
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Item", amount = 100.0, quantity = 1, customerName = null, profit = 20.0)
        )
        viewModel.addExpense(date = today, category = "transport", amount = 50.0)
        assertEquals(-30.0, viewModel.todayNetProfit, 0.001) // never clamped
    }

    @Test
    fun deleteExpense_recalculatesTotalsFromLog() {
        val today = viewModel.today
        val e1 = viewModel.addExpense(date = today, category = "utilities", amount = 100.0)
        viewModel.addExpense(date = today, category = "rent", amount = 200.0)
        assertEquals(300.0, viewModel.todayExpensesTotal, 0.001)
        viewModel.deleteExpense(e1!!.id)
        assertEquals(1, viewModel.expenses.value.size)
        assertEquals(200.0, viewModel.todayExpensesTotal, 0.001)
    }

    @Test
    fun completeEndOfDay_storesExpensesAndNetProfit() {
        val today = viewModel.today
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = today, description = "Item", amount = 150.0, quantity = 1, customerName = null, profit = 50.0)
        )
        viewModel.addExpense(date = today, category = "utilities", amount = 20.0)
        viewModel.addExpense(date = today, category = "transport", amount = 10.0)
        viewModel.completeEndOfDay(actualSales = 150.0)

        val eod = viewModel.endOfDayData.value
        assertNotNull(eod)
        assertEquals(50.0, eod!!.profit, 0.001)      // gross (unchanged)
        assertEquals(30.0, eod.expenses, 0.001)      // today's expense total
        assertEquals(20.0, eod.netProfit, 0.001)     // 50 - 30
    }

    @Test
    fun computeReportStats_day_includesExpensesAndNetProfit() {
        assertTrue(viewModel.setDevDateOverride("2026-08-09"))
        viewModel.addSpecificSale(
            SpecificSale(id = 0, date = "2026-08-09", description = "Today", amount = 100.0, quantity = 1, customerName = null, profit = 40.0)
        )
        viewModel.addExpense(date = "2026-08-09", category = "utilities", amount = 25.0)
        // An expense outside the day window must not leak into the day total
        viewModel.addExpense(date = "2026-08-01", category = "rent", amount = 500.0)

        val stats = viewModel.computeReportStats("day")
        assertEquals(25.0, stats.expenses, 0.001)
        assertEquals(15.0, stats.netProfit, 0.001)   // 40 - 25
        assertEquals(0.0, stats.prevNetProfit, 0.001)
    }

    @Test
    fun expenseCategoryLabels_areLocalized() {
        assertEquals("Utilities", com.example.sari_sari_smart.ui.localization.Strings.expenseCategoryLabel("utilities", "en"))
        assertEquals("Kuryente at Tubig", com.example.sari_sari_smart.ui.localization.Strings.expenseCategoryLabel("utilities", "fil"))
        assertEquals("Permits & Fees", com.example.sari_sari_smart.ui.localization.Strings.expenseCategoryLabel("permits", "en"))
        assertEquals("Lisensya / Bayarin", com.example.sari_sari_smart.ui.localization.Strings.expenseCategoryLabel("permits", "fil"))
        assertEquals("Iba pa", com.example.sari_sari_smart.ui.localization.Strings.expenseCategoryLabel("bogus", "fil"))
    }
}

