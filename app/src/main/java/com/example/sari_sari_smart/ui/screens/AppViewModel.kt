package com.example.sari_sari_smart.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sari_sari_smart.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Shared ViewModel for the app — holds in-memory state with Room database persistence.
 * Phase 4: Database-backed persistence via AppRepository.
 */
class AppViewModel : ViewModel() {

    // ── State ──────────────────────────────────────────────────────────
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _dailyEntry = MutableStateFlow<DailyEntry?>(null)
    val dailyEntry: StateFlow<DailyEntry?> = _dailyEntry.asStateFlow()

    private val _specificSales = MutableStateFlow<List<SpecificSale>>(emptyList())
    val specificSales: StateFlow<List<SpecificSale>> = _specificSales.asStateFlow()

    private val _debts = MutableStateFlow<List<CustomerDebt>>(emptyList())
    val debts: StateFlow<List<CustomerDebt>> = _debts.asStateFlow()

    private val _payments = MutableStateFlow<List<DebtPayment>>(emptyList())
    val payments: StateFlow<List<DebtPayment>> = _payments.asStateFlow()

    private val _endOfDayData = MutableStateFlow<EndOfDayData?>(null)
    val endOfDayData: StateFlow<EndOfDayData?> = _endOfDayData.asStateFlow()

    private val _reportPeriod = MutableStateFlow("day")
    val reportPeriod: StateFlow<String> = _reportPeriod.asStateFlow()

    val today: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ── Computed helpers ────────────────────────────────────────────────
    val totalOutstandingDebts: Double
        get() = _debts.value.sumOf { it.remainingBalance }

    val activeDebtorCount: Int
        get() = _debts.value.count { it.remainingBalance > 0 }

    /** Compute last activity string from createdAt timestamp and latest payment */
    fun getLastActivity(debt: CustomerDebt): String {
        val paymentsForDebt = _payments.value.filter { it.debtId == debt.id }
        val latestPaymentTime = paymentsForDebt.maxOfOrNull { it.timestamp }
        val latestTime = maxOf(latestPaymentTime ?: debt.createdAt, debt.createdAt)
        val now = System.currentTimeMillis()
        val diff = now - latestTime
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86_400_000 -> "${diff / 3600_000}h ago"
            diff < 172_800_000 -> "Yesterday"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                sdf.format(java.util.Date(latestTime))
            }
        }
    }

    val lowStockCount: Int
        get() = _products.value.count { it.status == StockStatus.LOW }

    val outOfStockCount: Int
        get() = _products.value.count { it.status == StockStatus.OUT_OF_STOCK }

    val todaySpecificSalesTotal: Double
        get() = _specificSales.value.filter { it.date == today }.sumOf { it.amount }

    /** Total cash sales today (non-utang) — this is the Recorded Sales Today value */
    val todayRecordedSales: Double
        get() = _specificSales.value.filter { it.date == today && it.customerName == null }.sumOf { it.amount }

    /** Total profit from today's specific sales (per-sale: (sellingPrice - costPrice) × qty) */
    val todayProfit: Double
        get() = _specificSales.value.filter { it.date == today }.sumOf { it.profit }

    /** Difference between Actual Sales and Recorded Sales */
    fun getSalesDiff(actualSales: Double): Double = actualSales - todayRecordedSales

    /** Profit = Actual Sales - Cost of Goods */
    fun getClosingProfit(actualSales: Double, costOfGoods: Double): Double = actualSales - costOfGoods

    // ── EOD Editability State ──────────────────────────────────────────
    /** Whether the business day is currently open */
    var dayOpen: Boolean = false

    /** The calendar date (yyyy-MM-dd) this business day started on */
    var dayDate: String = ""

    /** Whether today's sales data has been archived to history */
    var dayArchived: Boolean = false

    /** Start the business day — sets dayOpen, dayDate, clears archive flag */
    fun openDay() {
        dayOpen = true
        dayDate = today
        dayArchived = false
    }

    val isEodComplete: Boolean
        get() = _endOfDayData.value?.finished == true

    private var _productIdCounter = 10
    private var _saleIdCounter = 3
    private var _debtIdCounter = 4
    private var _paymentIdCounter = 10
    private var _tipRotationIndex = 0
    private var _restockIdCounter = 0

    // ── Restock Day State ──────────────────────────────────────────────
    private val _restockTemp = MutableStateFlow(RestockTempState())
    val restockTemp: StateFlow<RestockTempState> = _restockTemp.asStateFlow()

    private val _lastRestockDate = MutableStateFlow<String?>(null)
    val lastRestockDate: StateFlow<String?> = _lastRestockDate.asStateFlow()

    private val _restockLog = MutableStateFlow<List<RestockLogEntry>>(emptyList())
    val restockLog: StateFlow<List<RestockLogEntry>> = _restockLog.asStateFlow()

    val daysSinceLastRestock: Int get() {
        val date = _lastRestockDate.value ?: return -1
        return try {
            val then = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date) ?: return -1
            val diff = System.currentTimeMillis() - then.time
            (diff / (1000L * 60 * 60 * 24)).toInt()
        } catch (_: Exception) { -1 }
    }

    fun clearRestockData() {
        _lastRestockDate.value = null
        _restockLog.value = emptyList()
        _restockTemp.value = RestockTempState()
    }

    fun setRestockDateToday() {
        _lastRestockDate.value = today
    }

    fun viewRestockLogCount(): String = "${_restockLog.value.size} restock(s) on record"

    fun applyCorrection(correction: Correction) {
        val current = _restockTemp.value.corrections.toMutableList()
        val idx = current.indexOfFirst {
            (correction.productId != null && it.productId == correction.productId) ||
            (correction.productEntityId > 0 && it.productEntityId == correction.productEntityId)
        }
        if (idx >= 0) current[idx] = correction else current.add(correction)
        _restockTemp.value = _restockTemp.value.copy(corrections = current)
    }

    fun addPurchaseToTemp(purchase: PurchaseEntry) {
        val current = _restockTemp.value.purchases.toMutableList()
        current.add(purchase)
        _restockTemp.value = _restockTemp.value.copy(purchases = current)
    }

    fun removePurchaseFromTemp(index: Int) {
        val current = _restockTemp.value.purchases.toMutableList()
        if (index in current.indices) current.removeAt(index)
        _restockTemp.value = _restockTemp.value.copy(purchases = current)
    }

    fun setRestockStep(step: Int) {
        _restockTemp.value = _restockTemp.value.copy(step = step)
    }

    fun applyCorrectionsToProducts() {
        val corrections = _restockTemp.value.corrections
        val updated = _products.value.toMutableList()
        corrections.forEach { c ->
            if (c.productEntityId > 0) {
                val idx = updated.indexOfFirst { it.id == c.productEntityId }
                if (idx >= 0) updated[idx] = updated[idx].copy(quantity = c.newQty)
            }
        }
        _products.value = updated
    }

    fun completeRestock() {
        // Apply purchases to product quantities
        val updated = _products.value.toMutableList()
        val purchases = _restockTemp.value.purchases
        purchases.forEach { item ->
            if (item.productEntityId > 0) {
                val idx = updated.indexOfFirst { it.id == item.productEntityId }
                if (idx >= 0) {
                    updated[idx] = updated[idx].copy(
                        quantity = updated[idx].quantity + item.qtyAdded
                    )
                }
            } else {
                // New product
                _productIdCounter++
                updated.add(Product(
                    id = _productIdCounter,
                    name = item.productName,
                    quantity = item.qtyAdded,
                    costPrice = item.costPerUnit,
                    sellingPrice = item.costPerUnit * 1.2
                ))
            }
        }
        _products.value = updated

        // Create restock log entry
        _restockIdCounter++
        val entry = RestockLogEntry(
            id = "restock_$_restockIdCounter",
            date = today,
            items = purchases,
            totalCost = purchases.sumOf { it.totalCost }
        )
        _restockLog.value = _restockLog.value + entry
        _lastRestockDate.value = today

        // Reset temp state
        _restockTemp.value = RestockTempState()

        // Persist to Room (auto-saved via StateFlow.collect)
        viewModelScope.launch {
            repository?.saveRestockLog(entry)
        }
    }

    fun cancelRestock() {
        // Revert corrections already applied
        val corrections = _restockTemp.value.corrections
        val updated = _products.value.toMutableList()
        corrections.forEach { c ->
            if (c.productEntityId > 0) {
                val idx = updated.indexOfFirst { it.id == c.productEntityId }
                if (idx >= 0) updated[idx] = updated[idx].copy(quantity = c.oldQty)
            }
        }
        _products.value = updated
        _restockTemp.value = RestockTempState()
    }

    // ── Database persistence (Phase 4) ─────────────────────────────────
    private var repository: AppRepository? = null

    /**
     * Initialize Room database — loads saved data and sets up auto-save.
     * Uses `first()` for one-shot initial load, then observes state changes for persistence.
     */
    fun initRepository(repo: AppRepository) {
        repository = repo

        // Phase 1: Load initial data (one-shot)
        viewModelScope.launch {
            val loaded = doInitialLoad(repo)
            if (!loaded) {
                seedSampleData()
                persistAllToRepo(repo)
            }
        }

        // Phase 2: Auto-save every state change to Room
        viewModelScope.launch {
            _products.collect { list ->
                list.forEach { repo.saveProduct(it) }
            }
        }
        viewModelScope.launch {
            _dailyEntry.collect { v ->
                v?.let { repo.saveDailyEntry(it) }
            }
        }
        viewModelScope.launch {
            _specificSales.collect { list ->
                list.forEach { repo.saveSpecificSale(it) }
            }
        }
        viewModelScope.launch {
            _debts.collect { list ->
                list.forEach { repo.saveDebt(it) }
            }
        }
        viewModelScope.launch {
            _payments.collect { list ->
                list.forEach { repo.savePayment(it) }
            }
        }
        viewModelScope.launch {
            _endOfDayData.collect { v ->
                v?.let { repo.saveEndOfDayData(it) }
            }
        }
    }

    /**
     * One-shot initial load from Room using first() to get initial Flow emissions.
     */
    private suspend fun doInitialLoad(repo: AppRepository): Boolean {
        val products = repo.getAllProducts().first()
        if (products.isNotEmpty()) {
            _products.value = products
            // Restore product ID counter from max existing ID
            val maxId = products.maxOfOrNull { it.id } ?: 10
            if (maxId > _productIdCounter) _productIdCounter = maxId
        }
        val entry = repo.getLatestDailyEntry().first()
        if (entry != null) {
            _dailyEntry.value = entry
        }
        val sales = repo.getAllSpecificSales().first()
        if (sales.isNotEmpty()) {
            _specificSales.value = sales
            val maxSaleId = sales.maxOfOrNull { it.id } ?: 3
            if (maxSaleId > _saleIdCounter) _saleIdCounter = maxSaleId
        }
        val debts = repo.getAllDebts().first()
        if (debts.isNotEmpty()) {
            _debts.value = debts
            val maxDebtId = debts.maxOfOrNull { it.id } ?: 4
            if (maxDebtId > _debtIdCounter) _debtIdCounter = maxDebtId
        }
        val payments = repo.getAllPayments().first()
        if (payments.isNotEmpty()) {
            _payments.value = payments
            val maxPaymentId = payments.maxOfOrNull { it.id } ?: 10
            if (maxPaymentId > _paymentIdCounter) _paymentIdCounter = maxPaymentId
        }
        val eod = repo.getLatestEndOfDayData().first()
        if (eod != null) _endOfDayData.value = eod

        // Load latest restock log to restore last restock date (survives app restart)
        val restockLog = repo.getLatestRestockLog().first()
        if (restockLog != null) {
            _lastRestockDate.value = restockLog.date
            _restockLog.value = listOf(restockLog) + _restockLog.value.filter { it.id != restockLog.id }
        }

        return products.isNotEmpty() || debts.isNotEmpty() || payments.isNotEmpty() || sales.isNotEmpty() || entry != null || eod != null
    }

    private suspend fun persistAllToRepo(repo: AppRepository) {
        repo.saveProducts(_products.value)
        _dailyEntry.value?.let { repo.saveDailyEntry(it) }
        repo.saveSpecificSales(_specificSales.value)
        repo.saveDebts(_debts.value)
        repo.savePayments(_payments.value)
        _endOfDayData.value?.let { repo.saveEndOfDayData(it) }
    }

    /** True if data was loaded from persistence (not just seed data) */
    val hasPersistedData: Boolean get() = _products.value.isNotEmpty()

    // ── Stock Management ────────────────────────────────────────────────
    fun getProductById(id: Int): Product? = _products.value.find { it.id == id }

    fun deductStock(productId: Int, qty: Int) {
        val updated = _products.value.toMutableList()
        val index = updated.indexOfFirst { it.id == productId }
        if (index >= 0) {
            val p = updated[index]
            updated[index] = p.copy(quantity = (p.quantity - qty).coerceAtLeast(0))
            _products.value = updated
        }
    }

    fun updateProductStatus(productId: Int, newQty: Int) {
        val updated = _products.value.toMutableList()
        val index = updated.indexOfFirst { it.id == productId }
        if (index >= 0) {
            updated[index] = updated[index].copy(quantity = newQty.coerceAtLeast(0))
            _products.value = updated
        }
    }

    fun addOrUpdateProduct(name: String, qty: Int, costPrice: Double, sellingPrice: Double, lowStockThreshold: Int = 5): Product {
        val existing = _products.value.find { it.name.equals(name, ignoreCase = true) }
        return if (existing != null) {
            val updated = _products.value.toMutableList()
            val index = updated.indexOfFirst { it.id == existing.id }
            updated[index] = existing.copy(
                quantity = existing.quantity + qty,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                lowStockThreshold = lowStockThreshold
            )
            _products.value = updated
            updated[index]
        } else {
            _productIdCounter++
            val newProduct = Product(_productIdCounter, name, qty, costPrice, sellingPrice, lowStockThreshold = lowStockThreshold)
            _products.value = _products.value + newProduct
            newProduct
        }
    }

    fun deleteProduct(productId: Int) {
        _products.value = _products.value.filter { it.id != productId }
    }

    fun searchProducts(query: String): List<Product> {
        if (query.isBlank()) return _products.value
        return _products.value.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun getFilteredProducts(filter: String): List<Product> {
        return when (filter) {
            "plenty" -> _products.value.filter { it.status == StockStatus.PLENTY }
            "low" -> _products.value.filter { it.status == StockStatus.LOW }
            "out" -> _products.value.filter { it.status == StockStatus.OUT_OF_STOCK }
            else -> _products.value
        }
    }

    // ── Actions ─────────────────────────────────────────────────────────
    fun setReportPeriod(period: String) {
        _reportPeriod.value = period
    }

    fun recordDailyEntry(stockExpenses: Double, earnings: Double) {
        _dailyEntry.value = DailyEntry(
            date = today,
            stockExpenses = stockExpenses,
            earnings = earnings
        )
    }

    fun getPaymentsForDebt(debtId: Int): List<DebtPayment> =
        _payments.value.filter { it.debtId == debtId }.sortedBy { it.timestamp }

    fun getPaymentsForDebtFlow(debtId: Int) =
        repository?.getPaymentsByDebtId(debtId)

    fun addSpecificSale(sale: SpecificSale) {
        _saleIdCounter++
        val saleWithId = sale.copy(id = _saleIdCounter)
        val updated = _specificSales.value.toMutableList()
        updated.add(0, saleWithId)
        _specificSales.value = updated
    }

    fun addToDebtBalance(debtId: Int, amount: Double) {
        val updated = _debts.value.toMutableList()
        val index = updated.indexOfFirst { it.id == debtId }
        if (index >= 0) {
            val debt = updated[index]
            updated[index] = debt.copy(
                amount = debt.amount + amount,
                remainingBalance = debt.remainingBalance + amount
            )
            _debts.value = updated
        }
    }

    fun addDebt(debt: CustomerDebt) {
        _debtIdCounter++
        val debtWithId = debt.copy(id = _debtIdCounter)
        val updated = _debts.value.toMutableList()
        updated.add(0, debtWithId)
        _debts.value = updated
    }

    fun getDebtById(id: Int): CustomerDebt? = _debts.value.find { it.id == id }

    fun getUsedCustomerNames(): List<String> = _debts.value.map { it.customerName }.distinct()

    fun recordDebtPayment(debtId: Int, amount: Double, note: String? = null) {
        val updated = _debts.value.toMutableList()
        val index = updated.indexOfFirst { it.id == debtId }
        if (index >= 0) {
            val debt = updated[index]
            updated[index] = debt.copy(
                remainingBalance = debt.remainingBalance - amount
            )
            _debts.value = updated
        }
        // Create a payment record
        _paymentIdCounter++
        val payment = DebtPayment(
            id = _paymentIdCounter,
            debtId = debtId,
            amount = amount,
            timestamp = System.currentTimeMillis(),
            note = note
        )
        _payments.value = _payments.value + payment
    }

    /**
     * Complete the end-of-day closing.
     * Sets dayOpen = false but keeps dayArchived = false so data is still editable.
     * Overwrites today's history entry if one already exists.
     */
    fun completeEndOfDay(actualSales: Double = _dailyEntry.value?.earnings ?: 0.0,
                         costOfGoods: Double = _dailyEntry.value?.stockExpenses ?: 0.0) {
        val recordedSales = todayRecordedSales
        val salesDiff = actualSales - recordedSales
        val profit = todayProfit // Use per-sale profit (matching web app getTodayProfit())
        _endOfDayData.value = EndOfDayData(
            date = today,
            cashInDrawer = actualSales,
            stockCheckDone = true,
            debtPaymentsDone = true,
            finished = true,
            recordedSales = recordedSales,
            actualSales = actualSales,
            salesDiff = salesDiff,
            costOfGoods = costOfGoods,
            profit = profit
        )
        dayOpen = false
        dayArchived = false // Keep data available for editing
        dayDate = today
    }

    /**
     * Re-open closing for editing.
     * Restores today's expenses and earnings from the EOD history entry,
     * then sets dayOpen = true so the user can edit closing values.
     */
    fun reopenClosing() {
        val eod = _endOfDayData.value
        if (eod != null && eod.date == today) {
            // Restore actual sales from saved EOD data
            // The closing screen will pre-fill from this state
            dayDate = today
            dayOpen = true
            dayArchived = false
        }
    }

    /**
     * Archive today's sales to the history entry before starting a new day.
     * Copies today's specific sales into a structured snapshot and clears
     * them from the active sales list.
     */
    fun archiveDaySales() {
        if (dayDate.isBlank()) return
        val salesForDate = _specificSales.value.filter { it.date == dayDate }
        if (salesForDate.isNotEmpty()) {
            val existingEod = _endOfDayData.value
            if (existingEod != null && existingEod.date == dayDate) {
                // Archive into existing EOD entry
                val archivedProfit = salesForDate.sumOf { it.profit }
                val archivedSalesTotal = salesForDate.sumOf { it.amount }
                _endOfDayData.value = existingEod.copy(
                    recordedSales = existingEod.recordedSales.coerceAtLeast(archivedSalesTotal),
                    profit = existingEod.profit.coerceAtLeast(archivedProfit)
                )
            }
            // Remove archived sales from active list
            _specificSales.value = _specificSales.value.filter { it.date != dayDate }
        }
        dayArchived = true
    }

    fun resetTodaySales() {
        _dailyEntry.value = null
        _specificSales.value = _specificSales.value.filter { it.date != today }
        _endOfDayData.value = null
    }

    fun resetAllData() {
        _products.value = emptyList()
        _dailyEntry.value = null
        _specificSales.value = emptyList()
        _debts.value = emptyList()
        _payments.value = emptyList()
        _endOfDayData.value = null
        _productIdCounter = 10
        _saleIdCounter = 3
        _debtIdCounter = 4
        _paymentIdCounter = 10
    }

    // ── Business Tip Logic (mirrors web prototype 6-priority system + enhancements) ──
    data class BusinessTip(val message: String, val priority: Int)

    /**
     * Rotating generic tips for when everything is caught up.
     * Matches the web prototype's rotating tip system.
     */
    private val rotatingTips = listOf(
        "💡 Tip: Buying in bulk usually gets you a 10-20% discount from suppliers. Save more by stocking up on fast-moving items!",
        "💡 Tip: Check your inventory every morning to know what's running low before your customers ask.",
        "💡 Tip: Offer small discounts for cash payments instead of utang. This improves your cash flow!",
        "💡 Tip: Keep a notebook of which products sell fastest. Focus your restocking budget on those items.",
        "💡 Tip: Review your weekly profit trends every Monday. This helps you spot which products earn the most.",
        "💡 Tip: Set aside 20% of your daily earnings for savings. This builds a safety net for emergencies.",
    )

    fun getBusinessTip(): BusinessTip {
        val outOfStock = _products.value.filter { it.status == StockStatus.OUT_OF_STOCK }
        val lowStock = _products.value.filter { it.status == StockStatus.LOW }
        val hasDailyEntry = _dailyEntry.value?.date == today
        val debtsExist = totalOutstandingDebts > 0
        val eodDone = isEodComplete
        val todaySales = _specificSales.value.filter { it.date == today }

        // Priority 1: Out-of-stock items
        if (outOfStock.isNotEmpty()) {
            val names = outOfStock.take(3).joinToString(", ") { it.name }
            val suffix = if (outOfStock.size > 3) " +${outOfStock.size - 3} more" else ""
            return BusinessTip("⚠ Out of stock: $names$suffix. Restock immediately!", 1)
        }

        // Priority 2: Low-stock items
        if (lowStock.isNotEmpty()) {
            val names = lowStock.take(3).joinToString(", ") { "${it.name} (${it.quantity} left)" }
            val suffix = if (lowStock.size > 3) " +${lowStock.size - 3} more" else ""
            return BusinessTip("📦 Running low: $names$suffix. Consider restocking soon.", 2)
        }

        // Priority 3: Sales not recorded today
        if (!hasDailyEntry) {
            // Include pending profit awareness
            val todayUtangSales = todaySales.filter { it.customerName != null }
            val todayUtangTotal = todaySales.sumOf { it.amount } - todaySales.filter { it.customerName == null }.sumOf { it.amount }
            if (todaySales.isNotEmpty() && todayUtangTotal > 0) {
                return BusinessTip("📝 You have ₱${String.format("%,.2f", todayUtangTotal)} in utang sales today — record your daily earnings to see your real cash profit!", 3)
            }
            return BusinessTip("📝 Sales not recorded today. Tap the Earnings card to record your daily sales.", 3)
        }

        // Priority 4: Outstanding debts
        if (debtsExist) {
            return BusinessTip("📌 You have $activeDebtorCount debtor(s) with a total of ₱${String.format("%,.2f", totalOutstandingDebts)} outstanding.", 4)
        }

        // Priority 5: Sales recorded but EOD not done
        if (hasDailyEntry && !eodDone) {
            // Add pending profit awareness when profit is constrained by utang
            val todayUtangSales = todaySales.filter { it.customerName != null }
            val todayUtangTotal = todayUtangSales.sumOf { it.amount }
            val dailyEarnings = _dailyEntry.value?.earnings ?: 0.0
            if (dailyEarnings > 0 && todayUtangTotal > 0) {
                val utangPercentage = (todayUtangTotal / dailyEarnings) * 100
                if (utangPercentage >= 50) {
                    return BusinessTip("⚠ Over 50% of your earnings (₱${String.format("%,.2f", todayUtangTotal)}) is still in utang. Record payments to free up your cash flow.", 5)
                } else if (utangPercentage >= 20) {
                    return BusinessTip("📌 About ${String.format("%.0f", utangPercentage)}% of your earnings is still in utang. Follow up with debtors to keep cash flowing.", 5)
                }
            }
            return BusinessTip("🏁 Sales recorded! Complete your End-of-Day closing to finalize.", 5)
        }

        // Priority 6: All caught up — show rotating tip
        // Sales trend analysis: compare today's specific sales to 7-day average
        val todaySaleTotal = todaySales.sumOf { it.amount }
        val sevenDaysAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
        val weekSales = _specificSales.value.filter { it.date >= sevenDaysAgo }
        val weekSaleCount = weekSales.size.coerceAtLeast(1)
        val avgSaleTotal = weekSales.sumOf { it.amount } / weekSaleCount * todaySales.size.coerceAtLeast(1)

        // Only show trend when there are enough data points (3+ sales in the last 7 days)
        if (todaySaleTotal > 0 && weekSales.size >= 3 && avgSaleTotal > 0) {
            val diffPercent = ((todaySaleTotal - avgSaleTotal) / avgSaleTotal * 100).toInt()
            if (diffPercent > 10) {
                val prefix = if (_tipRotationIndex % 3 == 0) "📈" else "🌟"
                _tipRotationIndex = (_tipRotationIndex + 1) % rotatingTips.size
                return BusinessTip("$prefix Your sales are $diffPercent% higher than your recent average! Great momentum today. 🎉", 6)
            } else if (diffPercent < -10) {
                _tipRotationIndex = (_tipRotationIndex + 1) % rotatingTips.size
                return BusinessTip("📊 Your sales today are ${-diffPercent}% lower than your recent average. Check if you're missing any items.", 6)
            }
        }

        // Show rotating generic tip
        val tip = rotatingTips[_tipRotationIndex % rotatingTips.size]
        _tipRotationIndex = (_tipRotationIndex + 1) % rotatingTips.size
        return BusinessTip(tip, 6)
    }

    // ── Seed demo data ──────────────────────────────────────────────────
    fun seedSampleData() {
        _products.value = listOf(
            Product(1, "Cigarettes (Marvel)", 20, 12.0, 15.0),
            Product(2, "Canned Sardines", 15, 15.0, 20.0),
            Product(3, "Instant Noodles", 8, 8.0, 12.0),
            Product(4, "Cooking Oil (500ml)", 3, 30.0, 42.0),
            Product(5, "Rice (1kg)", 0, 45.0, 55.0),
            Product(6, "Sugar (1kg)", 4, 55.0, 68.0),
            Product(7, "Coffee 3in1 (10pk)", 12, 25.0, 35.0),
            Product(8, "Milk Powder (400g)", 2, 80.0, 105.0),
            Product(9, "Shampoo Sachet (50pcs)", 30, 5.0, 7.0),
            Product(10, "Candy Jar (approx 50pcs)", 25, 20.0, 30.0)
        )
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        _debts.value = listOf(
            CustomerDebt(1, "Aling Maria", 150.0, 150.0, now - day),
            CustomerDebt(2, "Mang Jose", 75.0, 40.0, now - 3 * day),
            CustomerDebt(3, "Kathryn", 200.0, 200.0, now),
            CustomerDebt(4, "Bryan", 50.0, 0.0, now - 7 * day)
        )
        // Record a payment for Bryan (who is settled)
        _payments.value = listOf(
            DebtPayment(1, 4, 50.0, now - 7 * day + 3600_000, "Fully paid")
        )
        _paymentIdCounter = 1
        // Record today's daily entry + specific sales for demo
        _dailyEntry.value = DailyEntry(today, 850.0, 1250.0)
        _specificSales.value = listOf(
            SpecificSale(1, today, "Instant Noodles (10 pcs)", 120.0, 10, null, 40.0),
            SpecificSale(2, today, "Cigarettes (5 packs)", 75.0, 5, "Kathryn", 15.0),
            SpecificSale(3, today, "Cooking Oil", 42.0, 1, null, 12.0)
        )
    }

    // ── Dev Panel Actions (Phase 1, adaptation_plan2) ────────────────────

    fun getRawStateJson(): JSONObject {
        return JSONObject().apply {
            put("products", org.json.JSONArray(_products.value.map { p ->
                JSONObject().apply {
                    put("id", p.id); put("name", p.name)
                    put("quantity", p.quantity); put("costPrice", p.costPrice)
                    put("sellingPrice", p.sellingPrice); put("unit", p.unit)
                    put("lowStockThreshold", p.lowStockThreshold)
                }
            }))
            put("dailyEntry", _dailyEntry.value?.let { de ->
                JSONObject().apply {
                    put("date", de.date); put("stockExpenses", de.stockExpenses)
                    put("earnings", de.earnings)
                }
            } ?: org.json.JSONObject.NULL)
            put("specificSales", org.json.JSONArray(_specificSales.value.map { s ->
                JSONObject().apply {
                    put("id", s.id); put("date", s.date); put("description", s.description)
                    put("amount", s.amount); put("quantity", s.quantity)
                    if (s.customerName != null) put("customerName", s.customerName) else put("customerName", org.json.JSONObject.NULL)
                    put("profit", s.profit)
                }
            }))
            put("debts", org.json.JSONArray(_debts.value.map { d ->
                JSONObject().apply {
                    put("id", d.id); put("customerName", d.customerName)
                    put("amount", d.amount); put("remainingBalance", d.remainingBalance)
                    put("createdAt", d.createdAt)
                }
            }))
            put("payments", org.json.JSONArray(_payments.value.map { p ->
                JSONObject().apply {
                    put("id", p.id); put("debtId", p.debtId); put("amount", p.amount)
                    put("timestamp", p.timestamp); put("note", p.note ?: org.json.JSONObject.NULL)
                }
            }))
            put("lowStockCount", lowStockCount)
            put("outOfStockCount", outOfStockCount)
            put("totalOutstandingDebts", totalOutstandingDebts)
        }
    }

    fun importData(obj: JSONObject) {
        // Products
        if (obj.has("products")) {
            val arr = obj.getJSONArray("products")
            val products = mutableListOf<Product>()
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                products.add(Product(
                    id = p.optInt("id", _productIdCounter + i + 1),
                    name = p.getString("name"),
                    quantity = p.optInt("quantity", 0),
                    costPrice = p.optDouble("costPrice", 0.0),
                    sellingPrice = p.optDouble("sellingPrice", 0.0),
                    unit = p.optString("unit", "piece"),
                    lowStockThreshold = p.optInt("lowStockThreshold", 5)
                ))
            }
            if (products.isNotEmpty()) _products.value = products
        }
        // Daily Entry
        if (obj.has("dailyEntry") && !obj.isNull("dailyEntry")) {
            val de = obj.getJSONObject("dailyEntry")
            _dailyEntry.value = DailyEntry(
                date = de.optString("date", today),
                stockExpenses = de.optDouble("stockExpenses", 0.0),
                earnings = de.optDouble("earnings", 0.0)
            )
        }
        // Specific Sales
        if (obj.has("specificSales")) {
            val arr = obj.getJSONArray("specificSales")
            val sales = mutableListOf<SpecificSale>()
            for (i in 0 until arr.length()) {
                val s = arr.getJSONObject(i)
                val customerName = if (s.isNull("customerName")) null else s.optString("customerName", null)
                sales.add(SpecificSale(
                    id = s.optInt("id", _saleIdCounter + i + 1),
                    date = s.optString("date", today),
                    description = s.optString("description", ""),
                    amount = s.optDouble("amount", 0.0),
                    quantity = s.optInt("quantity", 1),
                    customerName = customerName,
                    profit = s.optDouble("profit", 0.0)
                ))
            }
            if (sales.isNotEmpty()) _specificSales.value = sales
        }
        // Debts
        if (obj.has("debts")) {
            val arr = obj.getJSONArray("debts")
            val debts = mutableListOf<CustomerDebt>()
            for (i in 0 until arr.length()) {
                val d = arr.getJSONObject(i)
                debts.add(CustomerDebt(
                    id = d.optInt("id", _debtIdCounter + i + 1),
                    customerName = d.optString("customerName", ""),
                    amount = d.optDouble("amount", 0.0),
                    remainingBalance = d.optDouble("remainingBalance", 0.0),
                    createdAt = d.optLong("createdAt", System.currentTimeMillis())
                ))
            }
            if (debts.isNotEmpty()) _debts.value = debts
        }
        // Payments
        if (obj.has("payments")) {
            val arr = obj.getJSONArray("payments")
            val payments = mutableListOf<DebtPayment>()
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                payments.add(DebtPayment(
                    id = p.optInt("id", _paymentIdCounter + i + 1),
                    debtId = p.optInt("debtId", 0),
                    amount = p.optDouble("amount", 0.0),
                    timestamp = p.optLong("timestamp", System.currentTimeMillis()),
                    note = p.optString("note", null)
                ))
            }
            if (payments.isNotEmpty()) _payments.value = payments
        }
    }

    fun generateTestSale() {
        val prods = _products.value
        if (prods.isEmpty()) {
            // Auto-seed if no products exist
            seedSampleData()
            generateTestSale()
            return
        }
        val rand = java.util.Random()
        val product = prods[rand.nextInt(prods.size)]
        val qty = rand.nextInt(5) + 1
        val hasCustomer = rand.nextBoolean()
        val customerName = if (hasCustomer && _debts.value.isNotEmpty()) {
            _debts.value[rand.nextInt(_debts.value.size)].customerName
        } else null

        val amount = product.sellingPrice * qty
        val profit = (product.sellingPrice - product.costPrice) * qty

        _saleIdCounter++
        val sale = SpecificSale(
            id = _saleIdCounter,
            date = today,
            description = "${product.name} (test)",
            amount = amount,
            quantity = qty,
            customerName = customerName,
            profit = profit
        )
        val updated = _specificSales.value.toMutableList()
        updated.add(0, sale)
        _specificSales.value = updated

        // Deduct stock
        deductStock(product.id, qty)

        // If customer, create debt
        if (customerName != null) {
            addToDebtBalance(_debts.value[0].id, amount)
        }
    }

    fun generateTestDebts(): Int {
        val rand = java.util.Random()
        val names = listOf("Aling Nena", "Mang Kanor", "Teresa", "Bong", "Liza", "Rolly", "Elena", "Pedro")
        val count = rand.nextInt(4) + 2 // 2-5 debts
        val now = System.currentTimeMillis()
        val day = 86_400_000L

        val newDebts = _debts.value.toMutableList()
        val newPayments = _payments.value.toMutableList()
        for (i in 0 until count) {
            _debtIdCounter++
            val name = names[rand.nextInt(names.size)]
            val amount = (rand.nextInt(46) + 5) * 10.0 // 50-500 in steps of 10
            val isSettled = i == 0 && rand.nextBoolean()
            val debtId = _debtIdCounter
            newDebts.add(CustomerDebt(
                id = debtId,
                customerName = name,
                amount = amount,
                remainingBalance = if (isSettled) 0.0 else amount,
                createdAt = now - (i + 1) * day
            ))
            if (isSettled) {
                _paymentIdCounter++
                newPayments.add(DebtPayment(
                    id = _paymentIdCounter,
                    debtId = debtId,
                    amount = amount,
                    timestamp = now - i * day + 3600_000,
                    note = "Fully paid"
                ))
            }
        }
        _debts.value = newDebts
        _payments.value = newPayments
        return count
    }

    fun bulkAddItems(): Int {
        val rand = java.util.Random()
        val names = listOf(
            "Biscuit Pack", "Candle Pack", "Toothpaste", "Soap", "Shampoo 200ml",
            "Candy Pack", "Chips", "Juice Pack", "Noodles Cup", "Canned Corned Beef",
            "Coffee Ground", "Milk 1L", "Bread (Pan de Sal)", "Eggs (per piece)", "Sardines Hot"
        )
        val newProducts = _products.value.toMutableList()
        var added = 0

        names.forEach { name ->
            val costPrice = (rand.nextInt(20) + 5) * 1.0 // 5-25
            val markup = 1.2 + rand.nextDouble() * 0.3 // 1.2x to 1.5x
            val sellingPrice = (costPrice * markup).let { Math.round(it * 10.0) / 10.0 }
            val qty = rand.nextInt(96) + 5 // 5-100

            _productIdCounter++
            newProducts.add(Product(_productIdCounter, name, qty, costPrice, sellingPrice))
            added++
        }
        _products.value = newProducts
        return added
    }

    fun clearAllInventory() {
        _products.value = emptyList()
    }

    fun clearSelectedData(types: List<String>) {
        types.forEach { type ->
            when (type) {
                "products" -> _products.value = emptyList()
                "sales" -> {
                    _dailyEntry.value = null
                    _specificSales.value = emptyList()
                }
                "debts" -> {
                    _debts.value = emptyList()
                    _payments.value = emptyList()
                }
                "eod" -> _endOfDayData.value = null
            }
        }
    }

    init {
        // Don't seed data here anymore — it's now done in initPersistence()
        // if no persisted data exists.
    }

    // ── CSV Export ────────────────────────────────────────────────────
    fun exportCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("Sari-Sari Smart - Data Export")
        sb.appendLine("Exported: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
        sb.appendLine()

        // Products section
        sb.appendLine("=== PRODUCTS ===")
        sb.appendLine("ID,Name,Quantity,Cost Price,Selling Price,Profit Margin,Status")
        _products.value.forEach { p ->
            val margin = if (p.costPrice > 0) String.format("%.1f%%", ((p.sellingPrice - p.costPrice) / p.costPrice) * 100) else "N/A"
            sb.appendLine("${p.id},${p.name},${p.quantity},${p.costPrice},${p.sellingPrice},$margin,${p.status}")
        }
        sb.appendLine()

        // Sales section
        sb.appendLine("=== TODAY'S SALES ===")
        sb.appendLine("ID,Date,Description,Quantity,Amount,Profit,Customer")
        val todaySales = _specificSales.value.filter { it.date == today }
        todaySales.forEach { s ->
            sb.appendLine("${s.id},${s.date},${s.description},${s.quantity},${s.amount},${s.profit},${s.customerName ?: "Cash"}")
        }
        sb.appendLine()

        // Weekly summary
        sb.appendLine("=== WEEKLY SUMMARY ===")
        val sevenDaysAgo = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
        val weekSales = _specificSales.value.filter { it.date >= sevenDaysAgo }
        sb.appendLine("Total Sales (7 days),${String.format("%.2f", weekSales.sumOf { it.amount })}")
        sb.appendLine("Total Profit (7 days),${String.format("%.2f", weekSales.sumOf { it.profit })}")
        sb.appendLine("Items Sold (7 days),${weekSales.sumOf { it.quantity }}")
        sb.appendLine()

        // Debts section
        sb.appendLine("=== OUTSTANDING DEBTS ===")
        sb.appendLine("Customer,Total Amount,Remaining Balance,Last Activity")
        _debts.value.filter { it.remainingBalance > 0 }.forEach { d ->
            val lastAct = getLastActivity(d)
            sb.appendLine("${d.customerName},${d.amount},${d.remainingBalance},$lastAct")
        }
        sb.appendLine()

        // Inventory status
        sb.appendLine("=== INVENTORY STATUS ===")
        sb.appendLine("Status,Count")
        sb.appendLine("Out of Stock,${outOfStockCount}")
        sb.appendLine("Low Stock,${lowStockCount}")
        sb.appendLine("Plenty,${_products.value.size - outOfStockCount - lowStockCount}")

        return sb.toString()
    }

    fun getLowStockItems(): List<Product> {
        return _products.value.filter { it.status != StockStatus.PLENTY }
            .sortedBy { it.quantity }
    }

    // ── Weekly Snapshot (Phase 3b) ────────────────────────────────────
    /** Total sales from specific sales in the last 7 days */
    fun getWeekSales(): Double {
        val sevenDaysAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
        return _specificSales.value
            .filter { it.date >= sevenDaysAgo }
            .sumOf { it.amount }
    }

    /** Estimated weekly earnings (daily entries + specific sales for last 7 days) */
    fun getWeekEarnings(): Double {
        val sevenDaysAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000))
        val specificTotal = _specificSales.value
            .filter { it.date >= sevenDaysAgo }
            .sumOf { it.amount }
        val dailyTotal = _dailyEntry.value?.earnings ?: 0.0
        return specificTotal + dailyTotal
    }

    /** Estimated weekly profit */
    fun getWeekProfit(): Double {
        return getWeekEarnings() * 0.15 // rough estimate: 15% margin on total
    }
}
