package com.example.sari_sari_smart.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sari_sari_smart.data.*
import com.example.sari_sari_smart.ui.localization.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
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

    private val _debtTransactions = MutableStateFlow<List<DebtTransaction>>(emptyList())
    val debtTransactions: StateFlow<List<DebtTransaction>> = _debtTransactions.asStateFlow()

    private val _endOfDayData = MutableStateFlow<EndOfDayData?>(null)
    val endOfDayData: StateFlow<EndOfDayData?> = _endOfDayData.asStateFlow()

    private val _reportPeriod = MutableStateFlow("day")
    val reportPeriod: StateFlow<String> = _reportPeriod.asStateFlow()

    private val _currentDate = MutableStateFlow(dateNow())
    /** Observable current date (yyyy-MM-dd). Kept in sync with the device clock at
     *  each midnight and on app resume, so date-dependent UI (Morning overdue
     *  banner, Day/Closing entry guards, headers) recomputes in REAL time instead
     *  of freezing at the value captured when it first composed. */
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val today: String
        get() = if (devDateOverride.isNotBlank()) devDateOverride else _currentDate.value

    private fun dateNow(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    /** Re-sync [currentDate] with the device clock (wired to app ON_RESUME). */
    fun refreshCurrentDate() {
        _currentDate.value = dateNow()
    }

    /** Dev-only temporary date override (YYYY-MM-DD).
     *  In-memory only — NEVER persisted to AppSettings/Room, so it resets on
     *  app restart and never affects real data permanently. */
    var devDateOverride: String by mutableStateOf("")

    /** Snapshot of the real business day taken when a dev override is applied,
     *  so clearing the override restores the exact pre-test state.
     *  In-memory only — never persisted, matching the override's lifetime. */
    private data class DayStateSnapshot(
        val dayOpen: Boolean,
        val dayDate: String,
        val dayArchived: Boolean,
        val sales: List<SpecificSale>,
        val dailyEntry: DailyEntry?,
        val endOfDayData: EndOfDayData?
    )

    private var devDaySnapshot: DayStateSnapshot? = null

    /** Set a temporary dev date override. Returns false if the format is invalid. */
    fun setDevDateOverride(date: String): Boolean {
        val d = date.trim()
        if (d.isEmpty()) {
            // Empty input clears the override and restores the pre-test business state.
            devDateOverride = ""
            restoreDevDaySnapshot()
            return true
        }
        if (!d.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return false
        // Reject impossible dates (e.g. 2026-99-99) via non-lenient parse
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fmt.isLenient = false
        try { fmt.parse(d) } catch (e: Exception) { return false }
        // Snapshot the real business day BEFORE applying the override so clearing
        // it restores the exact pre-test state (no sales/day-state loss).
        if (devDaySnapshot == null) {
            devDaySnapshot = DayStateSnapshot(
                dayOpen = dayOpen,
                dayDate = dayDate,
                dayArchived = dayArchived,
                sales = _specificSales.value.toList(),
                dailyEntry = _dailyEntry.value,
                endOfDayData = _endOfDayData.value
            )
        }
        devDateOverride = d
        // If the perceived date moved past the open day, the Morning page now
        // SURFACES the stale day (overdue banner + close-stale button) instead
        // of auto-archiving it (web v2.35 parity).
        return true
    }

    fun clearDevDateOverride() {
        devDateOverride = ""
        restoreDevDaySnapshot()
    }

    /** Dev-only temporary HOUR override (0-23) for testing the time-based greeting.
     *  In-memory only — NEVER persisted, matching devDateOverride's lifetime. */
    var devTimeOverride: Int? by mutableStateOf(null)

    /** Time-of-day greeting key (web greetingForTime parity): devTimeOverride wins,
     *  otherwise the real device hour. h<12 → morning, h<18 → afternoon, else evening. */
    fun greetingForTimeKey(): String {
        val h = devTimeOverride ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            h < 12 -> "greetingMorning"
            h < 18 -> "greetingAfternoon"
            else -> "greetingEvening"
        }
    }

    /** Set a temporary dev time override from a raw string (mirrors setDevDateOverride).
     *  Blank = clear; non-numeric or out-of-range 0-23 = invalid (returns false). */
    fun setDevTimeOverride(input: String): Boolean {
        val t = input.trim()
        if (t.isEmpty()) { devTimeOverride = null; return true }   // blank = clear
        val hour = t.toIntOrNull() ?: return false                 // garbage = invalid
        if (hour < 0 || hour > 23) return false                    // out of range = invalid
        devTimeOverride = hour
        return true
    }

    fun clearDevTimeOverride() {
        devTimeOverride = null
    }

    /** Restores the exact pre-override business day (flags, date, sales, earnings, EOD).
     *  Sales are REPLACED (not merged) so any sales recorded during the test —
     *  dated to the override date — are purged on clear, with no test data
     *  leaking into real records (web v2.34 captureDevSnapshot parity). */
    private fun restoreDevDaySnapshot() {
        val snap = devDaySnapshot ?: return
        devDaySnapshot = null
        _specificSales.value = snap.sales.toList()
        dayOpen = snap.dayOpen
        dayDate = snap.dayDate
        dayArchived = snap.dayArchived
        _dailyEntry.value = snap.dailyEntry
        _endOfDayData.value = snap.endOfDayData
        persistDayState()
    }

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
    // ── AppSettings persistence for day state ───────────────────────────
    private var appSettings: AppSettings? = null

    /**
     * Initialize day state from persisted AppSettings.
     * Call this after ViewModel creation so dayOpen/dayDate/dayArchived
     * survive app restart.
     */
    fun initAppSettings(settings: AppSettings) {
        appSettings = settings
        dayOpen = settings.dayOpen
        dayDate = settings.dayDate
        dayArchived = settings.dayArchived
    }

    /** Save current day state to AppSettings so it survives app restart */
    fun persistDayState() {
        appSettings?.let {
            it.dayOpen = dayOpen
            it.dayDate = dayDate
            it.dayArchived = dayArchived
        }
    }

    /** Whether the business day is currently open */
    var dayOpen: Boolean by mutableStateOf(false)

    /** The calendar date (yyyy-MM-dd) this business day started on */
    var dayDate: String by mutableStateOf("")

    /** Whether today's sales data has been archived to history */
    var dayArchived: Boolean by mutableStateOf(false)

    /** Start the business day — sets dayOpen, dayDate, clears archive flag.
     *  Also clears the manual daily entry so a fresh day starts clean
     *  (web parity: startDay() resets todayExpenses/todayEarnings to 0).
     *  Prevents a stale DailyEntry.earnings from a previous session (e.g. a
     *  previously entered 1,250,000) from leaking into the Closing page's
     *  Actual Sales input or the Day mode stat.
     */
    fun openDay() {
        dayOpen = true
        dayDate = today
        dayArchived = false
        _dailyEntry.value = null
        persistDayState()
    }

    // ── Overdue store workflow (web v2.35 parity) ──────────────────────
    // A store left open across business days is now SURFACED on the Morning
    // page (amber banner + "Review Last Day's Sales" modal) instead of being
    // silently auto-archived. Archiving only happens on explicit user action
    // via closeStaleDayAndStartToday().

    /** True when the store is open but the business day it started on (dayDate)
     *  is strictly BEFORE today. Uses `<` (not `!=`) so a device clock moved
     *  backward never flags a "future" day as overdue (web isStaleOpenDay()). */
    fun isStaleOpenDay(): Boolean =
        dayOpen && dayDate.isNotBlank() && dayDate < today

    /** Whole calendar days the current open day has been open (0 = opened today).
     *  Uses the dev-override-aware `today` so an override simulates the date
     *  (web getDaysOpen()). */
    fun getDaysOpen(): Int {
        if (dayDate.isBlank()) return 0
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val then = fmt.parse(dayDate) ?: return 0
            val now = fmt.parse(today) ?: Date()
            val days = ((now.time - then.time) / (1000L * 60 * 60 * 24)).toInt()
            days.coerceAtLeast(0)
        } catch (_: Exception) { 0 }
    }

    /** Close the previous (stale) day and start a fresh day for today.
     *  Saves the previous day's sales into history — nothing is lost.
     *  The UI shows a confirm dialog first when a dev date override is active
     *  (archiving during an override writes to REAL persisted history). */
    fun closeStaleDayAndStartToday() {
        if (!isStaleOpenDay()) return
        archiveDaySales()
        // Clear the previous day's manual earnings so the fresh day starts clean
        // (web parity: closeStaleDayAndStartToday resets expenses/earnings to 0).
        _dailyEntry.value = null
        dayDate = today
        dayArchived = false
        dayOpen = true
        persistDayState()
    }

    val isEodComplete: Boolean
        get() = _endOfDayData.value?.finished == true

    private var _productIdCounter = 10
    private var _saleIdCounter = 3
    private var _debtIdCounter = 4
    private var _paymentIdCounter = 10
    private var _debtTxIdCounter = 100
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
            // Stale open days are no longer auto-archived here — they are
            // surfaced on the Morning page (overdue banner, web v2.35 parity).
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
            _debtTransactions.collect { list ->
                list.forEach { repo.saveDebtTransaction(it) }
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
        val debtTxs = repo.getAllDebtTransactions().first()
        if (debtTxs.isNotEmpty()) {
            _debtTransactions.value = debtTxs
            val maxTxId = debtTxs.maxOfOrNull { it.id } ?: 100
            if (maxTxId > _debtTxIdCounter) _debtTxIdCounter = maxTxId
        }
        // Web loadState parity: backfill a permanent initial ledger row for any
        // loaded debt that has none (pre-ledger data). Without this, a legacy
        // debt that later receives ONE new ledger entry would lose its initial
        // row and its running balance would silently stop reconciling.
        if (debts.isNotEmpty()) {
            val existingIds = _debtTransactions.value.map { it.debtId }.toSet()
            val missing = debts.filter { it.id !in existingIds }
            if (missing.isNotEmpty()) {
                val newTxs = _debtTransactions.value.toMutableList()
                missing.forEach { debt ->
                    _debtTxIdCounter++
                    newTxs.add(DebtTransaction(
                        id = _debtTxIdCounter,
                        debtId = debt.id,
                        type = "debt",
                        description = null, // rendered as the localized initial-debt label
                        amount = debt.amount,
                        timestamp = debt.createdAt
                    ))
                }
                _debtTransactions.value = newTxs
            }
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
        repo.saveDebtTransactions(_debtTransactions.value)
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

    fun addDebt(debt: CustomerDebt): CustomerDebt {
        _debtIdCounter++
        val debtWithId = debt.copy(id = _debtIdCounter)
        val updated = _debts.value.toMutableList()
        updated.add(0, debtWithId)
        _debts.value = updated
        return debtWithId
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

    /** Record a debt-balance increase in the ledger (web transactions[] parity). */
    fun addDebtTransaction(debtId: Int, type: String, description: String, amount: Double, timestamp: Long = System.currentTimeMillis()) {
        _debtTxIdCounter++
        _debtTransactions.value = _debtTransactions.value + DebtTransaction(
            id = _debtTxIdCounter,
            debtId = debtId,
            type = type,
            description = description,
            amount = amount,
            timestamp = timestamp
        )
    }

    /** Ledger entries for one debt, chronological (web transactions[]). */
    fun getDebtTransactionsForDebt(debtId: Int): List<DebtTransaction> =
        _debtTransactions.value.filter { it.debtId == debtId }.sortedBy { it.timestamp }

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
        persistDayState()
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
            persistDayState()
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
        persistDayState()
    }

    fun resetTodaySales() {
        _dailyEntry.value = null
        _specificSales.value = _specificSales.value.filter { it.date != today }
        _endOfDayData.value = null
    }

    /**
     * Start a fresh business day from the Dev Panel.
     * Archives current day's sales to history, resets today's in-memory data,
     * and initializes a pre-opening state so the Morning page shows "Start the Day".
     */
    fun startNewDay() {
        archiveDaySales()       // Copy today's sales to history
        resetTodaySales()        // Clear daily entry, specific sales, and EOD data
        dayOpen = false
        dayDate = today          // Set to today so morning page can detect a fresh day
        dayArchived = true       // Sales were archived → "Edit Closing" condition won't match
        persistDayState()
    }

    fun resetAllData() {
        _products.value = emptyList()
        _dailyEntry.value = null
        _specificSales.value = emptyList()
        _debts.value = emptyList()
        _payments.value = emptyList()
        _debtTransactions.value = emptyList()
        _endOfDayData.value = null
        _productIdCounter = 10
        _saleIdCounter = 3
        _debtIdCounter = 4
        _paymentIdCounter = 10
        _debtTxIdCounter = 100
        dayOpen = false
        dayDate = ""
        dayArchived = false
        devDateOverride = "" // factory reset also clears the temporary dev override
        devDaySnapshot = null // and its pre-test snapshot
        devTimeOverride = null // ...and the temporary dev time override
        persistDayState()
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
        // WEB PARITY: a fresh day / clean slate starts with NO financial data.
        // Only sample products are seeded — no fake sales, debts, payments, or
        // daily entries — so Day Mode / Closing show P0.00 / 0 items until real
        // sales are recorded. (Fake transactions used to be auto-seeded here,
        // causing phantom P120 / 10-item sales to appear on a clean launch.)
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
            put("debtTransactions", org.json.JSONArray(_debtTransactions.value.map { tx ->
                JSONObject().apply {
                    put("id", tx.id); put("debtId", tx.debtId); put("type", tx.type)
                    put("description", tx.description ?: org.json.JSONObject.NULL)
                    put("amount", tx.amount); put("timestamp", tx.timestamp)
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
        // Debt Transactions
        if (obj.has("debtTransactions")) {
            val arr = obj.getJSONArray("debtTransactions")
            val txs = mutableListOf<DebtTransaction>()
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                txs.add(DebtTransaction(
                    id = t.optInt("id", _debtTxIdCounter + i + 1),
                    debtId = t.optInt("debtId", 0),
                    type = t.optString("type", "debt"),
                    description = if (t.isNull("description")) null else t.optString("description", null),
                    amount = t.optDouble("amount", 0.0),
                    timestamp = t.optLong("timestamp", System.currentTimeMillis())
                ))
            }
            if (txs.isNotEmpty()) {
                _debtTransactions.value = txs
                val maxTxId = txs.maxOfOrNull { it.id } ?: 100
                if (maxTxId > _debtTxIdCounter) _debtTxIdCounter = maxTxId
            }
        }
        // Web loadState parity: backfill an initial ledger row for imported debts
        // that have none, so history + running balance stay consistent.
        val importedDebts = _debts.value
        val ledgerIds = _debtTransactions.value.map { it.debtId }.toSet()
        val missing = importedDebts.filter { it.id !in ledgerIds }
        if (missing.isNotEmpty()) {
            val newTxs = _debtTransactions.value.toMutableList()
            missing.forEach { debt ->
                _debtTxIdCounter++
                newTxs.add(DebtTransaction(
                    id = _debtTxIdCounter,
                    debtId = debt.id,
                    type = "debt",
                    description = null,
                    amount = debt.amount,
                    timestamp = debt.createdAt
                ))
            }
            _debtTransactions.value = newTxs
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

        // If customer, create debt + record ledger entry (web saveSale parity)
        if (customerName != null) {
            addToDebtBalance(_debts.value[0].id, amount)
            addDebtTransaction(_debts.value[0].id, "debt", product.name, amount)
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
        val newTxs = _debtTransactions.value.toMutableList()
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
            // Initial ledger entry (web: initial transactions[] row)
            _debtTxIdCounter++
            newTxs.add(DebtTransaction(
                id = _debtTxIdCounter,
                debtId = debtId,
                type = "debt",
                description = "Test debt",
                amount = amount,
                timestamp = now - (i + 1) * day
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
        _debtTransactions.value = newTxs
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
                    _debtTransactions.value = emptyList()
                }
                "eod" -> _endOfDayData.value = null
            }
        }
    }

    init {
        // Don't seed data here anymore — it's now done in initPersistence()
        // if no persisted data exists.

        // Observable-date ticker: fires at each midnight so any UI depending on
        // `today` (overdue banner, day guards, headers) recomposes in real time
        // even while the app process stays alive across calendar days.
        viewModelScope.launch {
            while (true) {
                val now = Calendar.getInstance()
                val nextMidnight = Calendar.getInstance().apply {
                    timeInMillis = now.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                delay((nextMidnight.timeInMillis - now.timeInMillis).coerceAtLeast(1000L))
                _currentDate.value = dateNow()
            }
        }
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
