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
        _reportPeriod.value = settings.reportPeriod
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
                    // New-product price uses the configured default markup from
                    // Settings (falls back to 20% when unset), matching the
                    // Add Stock markup helper.
                    sellingPrice = item.costPerUnit * (1 + (appSettings?.defaultMarkup ?: 20) / 100.0),
                    // Same for the low-stock alert threshold.
                    lowStockThreshold = appSettings?.lowStockThreshold ?: 5
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

    /** Web v2.59 parity: identity fields (category/brand/unit/packageSize) are
     *  persisted on both the add and update paths so edits keep them intact. */
    fun addOrUpdateProduct(
        name: String,
        qty: Int,
        costPrice: Double,
        sellingPrice: Double,
        lowStockThreshold: Int = 5,
        category: String = "",
        brand: String = "",
        unit: String = "piece",
        packageSize: String = ""
    ): Product {
        val existing = _products.value.find { it.name.equals(name, ignoreCase = true) }
        return if (existing != null) {
            val updated = _products.value.toMutableList()
            val index = updated.indexOfFirst { it.id == existing.id }
            updated[index] = existing.copy(
                quantity = existing.quantity + qty,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                lowStockThreshold = lowStockThreshold,
                category = category,
                brand = brand,
                unit = unit,
                packageSize = packageSize
            )
            _products.value = updated
            updated[index]
        } else {
            _productIdCounter++
            val newProduct = Product(
                _productIdCounter, name, qty, costPrice, sellingPrice,
                unit = unit, lowStockThreshold = lowStockThreshold,
                category = category, brand = brand, packageSize = packageSize
            )
            _products.value = _products.value + newProduct
            newProduct
        }
    }

    fun deleteProduct(productId: Int) {
        _products.value = _products.value.filter { it.id != productId }
    }

    /** Web v2.59 parity: product search covers ALL identity fields — name,
     *  category (key + EN/FIL labels), brand, unit (key + labels), and package
     *  size — so an owner can find products by category (e.g. "condiments" /
     *  "pampalasa"), brand, or size, not just by name. */
    fun searchProducts(query: String): List<Product> {
        if (query.isBlank()) return _products.value
        val q = query.lowercase()
        return _products.value.filter { p ->
            val hay = buildString {
                append(p.name.lowercase())
                if (p.category.isNotBlank()) {
                    append(' ').append(p.category.lowercase())
                    append(' ').append(com.example.sari_sari_smart.ui.localization.Strings.productCategoryLabel(p.category, "en").lowercase())
                    append(' ').append(com.example.sari_sari_smart.ui.localization.Strings.productCategoryLabel(p.category, "fil").lowercase())
                }
                if (p.brand.isNotBlank()) append(' ').append(p.brand.lowercase())
                if (p.unit.isNotBlank()) {
                    append(' ').append(p.unit.lowercase())
                    append(' ').append(com.example.sari_sari_smart.ui.localization.Strings.productUnitLabel(p.unit, "en").lowercase())
                    append(' ').append(com.example.sari_sari_smart.ui.localization.Strings.productUnitLabel(p.unit, "fil").lowercase())
                }
                if (p.packageSize.isNotBlank()) append(' ').append(p.packageSize.lowercase())
            }
            hay.contains(q)
        }
    }

    // ── Product identity helpers (web v2.59 parity) ────────────────────────

    /** Distinct brands already used by products — for Add Stock suggestions
     *  (web getUsedBrands parity). */
    fun getUsedBrands(): List<String> =
        _products.value.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()

    /** Distinct package sizes already used by products — for Add Stock
     *  suggestions (web getUsedPackageSizes parity). */
    fun getUsedPackageSizes(): List<String> =
        _products.value.map { it.packageSize }.filter { it.isNotBlank() }.distinct().sorted()

    /** Filter products by a category key (web v2.59 parity). Empty filter or
     *  "" returns ALL products (uncategorized products only match "all"). */
    fun getProductsByCategory(category: String): List<Product> {
        if (category.isBlank()) return _products.value
        return _products.value.filter { it.category == category }
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
        appSettings?.reportPeriod = period
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

    // ── Multi-item checkout (web v2.63/v2.64 parity) ──────────────────────
    // The Day page's Sell action now opens a standalone checkout screen that
    // builds a CART of multiple products, then completes them as ONE
    // transaction: a shared transactionId on every sale row, a single debt
    // entry per credit purchase, per-line ledger entries, one stock deduction
    // pass, and one cart total used for the credit-limit gate.

    /** One line of the in-progress sale cart. */
    data class CartLine(
        val productId: Int,
        val name: String,
        val brand: String,
        val unit: String,
        val packageSize: String,
        val sellingPrice: Double,
        val qty: Int
    ) {
        val subtotal: Double get() = sellingPrice * qty
    }

    private val _saleCart = MutableStateFlow<List<CartLine>>(emptyList())
    /** Items currently in the checkout cart. */
    val saleCart: StateFlow<List<CartLine>> = _saleCart.asStateFlow()

    private val _salePayment = MutableStateFlow("cash")
    /** "cash" or "credit" — the checkout payment method (web setSalePayment parity). */
    val salePayment: StateFlow<String> = _salePayment.asStateFlow()

    /** Sum of all cart lines (₱). */
    fun getCartTotal(): Double = _saleCart.value.sumOf { it.subtotal }

    /** Number of items across all cart lines (for the count badge). */
    fun getCartLineCount(): Int = _saleCart.value.sumOf { it.qty }

    fun setSalePayment(payment: String) {
        if (payment == "cash" || payment == "credit") _salePayment.value = payment
    }

    /** Add a product to the cart. Same product merges; qty is clamped to stock
     *  (web addToCart parity). Returns true when the item was added/merged. */
    fun addToCart(product: Product, qty: Int): Boolean {
        if (product.quantity <= 0 || qty <= 0) return false
        val current = _saleCart.value.toMutableList()
        val idx = current.indexOfFirst { it.productId == product.id }
        val clamped = qty.coerceAtMost(product.quantity)
        if (idx >= 0) {
            val line = current[idx]
            current[idx] = line.copy(qty = (line.qty + clamped).coerceAtMost(product.quantity))
        } else {
            current.add(
                CartLine(
                    productId = product.id,
                    name = product.name,
                    brand = product.brand,
                    unit = product.unit,
                    packageSize = product.packageSize,
                    sellingPrice = product.sellingPrice,
                    qty = clamped
                )
            )
        }
        _saleCart.value = current
        return true
    }

    /** Adjust a line's qty by [delta] (web cartAdjustQty parity). */
    fun cartAdjustQty(productId: Int, delta: Int) {
        val product = getProductById(productId)
        val current = _saleCart.value.toMutableList()
        val idx = current.indexOfFirst { it.productId == productId }
        if (idx < 0) return
        val line = current[idx]
        val max = product?.quantity?.coerceAtLeast(line.qty) ?: Int.MAX_VALUE
        val newQty = (line.qty + delta).coerceIn(1, max)
        current[idx] = line.copy(qty = newQty)
        _saleCart.value = current
    }

    /** Set a line's qty directly (web cartSetQty parity, clamped to stock). */
    fun cartSetQty(productId: Int, qty: Int) {
        if (qty < 1) return
        val product = getProductById(productId)
        val current = _saleCart.value.toMutableList()
        val idx = current.indexOfFirst { it.productId == productId }
        if (idx < 0) return
        val line = current[idx]
        val max = product?.quantity?.coerceAtLeast(line.qty) ?: Int.MAX_VALUE
        current[idx] = line.copy(qty = qty.coerceAtMost(max))
        _saleCart.value = current
    }

    /** Remove one line from the cart (web cartRemoveLine parity). */
    fun cartRemoveLine(productId: Int) {
        _saleCart.value = _saleCart.value.filter { it.productId != productId }
    }

    fun clearCart() {
        _saleCart.value = emptyList()
    }

    /**
     * Complete the whole cart as ONE transaction (web completeSale parity).
     *
     * - Every sale row shares the same [transactionId] and carries the payment
     *   method, so the Day feed / reports can group items of one purchase.
     * - A credit purchase creates ONE debt entry for the transaction total with
     *   PER-LINE ledger entries (web: one debt per transaction, per-item rows).
     * - Stock is deducted per line.
     * - The credit-limit gate checks the CART TOTAL, not a single item.
     *
     * Returns false (and does nothing) when the cart is empty, a credit sale
     * has no customer name, or the credit limit blocks the sale without [force].
     */
    fun completeSale(customerName: String = "", force: Boolean = false): Boolean {
        val lines = _saleCart.value
        if (lines.isEmpty()) return false
        val total = getCartTotal()
        val isCredit = _salePayment.value == "credit"
        if (isCredit && customerName.isBlank()) return false
        if (isCredit && !force) {
            val cs = getCreditStatus(customerName, total)
            if (cs.overLimit) return false
        }

        // Shared transaction id so all lines read as one purchase.
        val transactionId = System.currentTimeMillis()
        lines.forEach { line ->
            val sale = SpecificSale(
                id = 0, // auto-assigned
                date = today,
                description = line.name,
                amount = line.subtotal,
                quantity = line.qty,
                customerName = if (isCredit) customerName else null,
                profit = (line.sellingPrice - (getProductById(line.productId)?.costPrice ?: 0.0)) * line.qty,
                transactionId = transactionId,
                paymentMethod = if (isCredit) "credit" else "cash"
            )
            addSpecificSale(sale)
            deductStock(line.productId, line.qty)
        }

        // One debt entry per transaction; per-line ledger entries (web parity).
        if (isCredit) {
            val existingDebt = getDebtForName(customerName)
            if (existingDebt != null) {
                addToDebtBalance(existingDebt.id, total)
                lines.forEach { line ->
                    addDebtTransaction(existingDebt.id, "debt", line.name, line.subtotal)
                }
            } else {
                val newDebt = addDebt(
                    CustomerDebt(
                        id = 0,
                        customerName = customerName,
                        amount = total,
                        remainingBalance = total
                    )
                )
                lines.forEach { line ->
                    addDebtTransaction(newDebt.id, "debt", line.name, line.subtotal)
                }
            }
        }

        clearCart()
        return true
    }

    // ── Credit-limit engine (web v2.56/v2.57 parity) ────────────────────

    /** Global default credit limit (₱). 0 = no limit. Falls back to 500. */
    fun getDefaultCreditLimit(): Int = appSettings?.defaultCreditLimit ?: 500

    /** The debt record for a customer name — active (balance > 0) first, else
     *  a settled record, else null. Mirrors web getDebtForName(). */
    fun getDebtForName(name: String): CustomerDebt? {
        if (name.isBlank()) return null
        val lower = name.trim().lowercase()
        var settled: CustomerDebt? = null
        _debts.value.forEach { d ->
            if (d.customerName.trim().lowercase() == lower) {
                if (d.remainingBalance > 0) return d
                if (settled == null) settled = d
            }
        }
        return settled
    }

    /** Effective limit for a customer: per-customer override wins, else global
     *  default (0 = no limit). Mirrors web getEffectiveCreditLimit(). */
    fun getEffectiveCreditLimit(name: String): Int {
        val debt = getDebtForName(name)
        val custom = debt?.creditLimit
        return if (custom != null && custom >= 0) custom else getDefaultCreditLimit()
    }

    /** Credit status for a name given a prospective purchase. At-or-above the
     *  limit blocks a credit sale; near-limit warns at >=80% (web v2.57: at-or-above
     *  with a half-cent epsilon so float drift can't slip an at-limit sale through). */
    fun getCreditStatus(name: String, prospective: Double): CreditStatus {
        val limit = getEffectiveCreditLimit(name)
        val lower = name.trim().lowercase()
        val balance = _debts.value
            .filter { it.customerName.trim().lowercase() == lower && it.remainingBalance > 0 }
            .sumOf { it.remainingBalance }
        val total = balance + prospective
        val atLimit = limit > 0 && Math.abs(total - limit) < 0.005
        val overLimit = limit > 0 && total >= limit - 0.005
        val nearLimit = limit > 0 && !overLimit && total >= limit * 0.8
        return CreditStatus(
            limit = limit,
            balance = balance,
            total = total,
            overLimit = overLimit,
            atLimit = atLimit,
            nearLimit = nearLimit
        )
    }

    /** Set (or clear) a customer's per-customer credit limit. null = use default. */
    fun updateDebtCreditLimit(debtId: Int, limit: Int?) {
        val updated = _debts.value.toMutableList()
        val index = updated.indexOfFirst { it.id == debtId }
        if (index >= 0) {
            updated[index] = updated[index].copy(creditLimit = limit)
            _debts.value = updated
        }
    }

    /** Number of customers whose TOTAL outstanding balance is at-or-above their
     *  effective credit limit (name-grouped across multiple debt records, web
     *  v2.56/v2.57 parity). 0 when a limit is 0 (no limit). */
    fun getOverLimitDebtorCount(): Int {
        val nameTotals = mutableMapOf<String, Double>()
        _debts.value.filter { it.remainingBalance > 0 }.forEach { d ->
            nameTotals[d.customerName] = (nameTotals[d.customerName] ?: 0.0) + d.remainingBalance
        }
        return nameTotals.count { (name, total) ->
            val limit = getEffectiveCreditLimit(name)
            limit > 0 && total >= limit - 0.005
        }
    }

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
    fun completeEndOfDay(actualSales: Double = _dailyEntry.value?.earnings ?: 0.0) {
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
        _reportPeriod.value = "day" // web parity: resetData() also resets the persisted period
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
    /** Web v2.59 parity: 17 sample products matching the web prototype's
     *  getSampleProducts() — all 9 categories covered, a brand/no-brand mix
     *  (10/7), and lowStockThreshold set on 6 (the rest exercise the global
     *  Settings fallback). Only products are seeded — no fake sales, debts,
     *  payments, or daily entries (web parity: a clean slate has no financial
     *  data). */
    fun seedSampleData() {
        fun p(id: Int, name: String, cat: String, brand: String, unit: String, pkg: String, qty: Int, cost: Double, sell: Double, threshold: Int? = null) =
            Product(
                id = id, name = name, quantity = qty, costPrice = cost, sellingPrice = sell,
                unit = unit, lowStockThreshold = threshold ?: 5,
                category = cat, brand = brand, packageSize = pkg
            )
        _products.value = listOf(
            p(1, "Bigas", "food", "", "kg", "1kg", 20, 45.0, 55.0, 10),
            p(2, "Mantika", "dry_goods", "", "L", "1L", 3, 22.0, 30.0),
            p(3, "Asin", "condiments", "", "sachet", "", 0, 10.0, 15.0),
            p(4, "Canned Tuna", "canned", "Ligo", "can", "155g", 30, 18.0, 25.0, 12),
            p(5, "Instant Noodles", "food", "Lucky Me", "pack", "60g", 8, 10.0, 15.0),
            p(6, "Kape 3in1", "beverages", "Nescaf\u00e9", "sachet", "", 50, 5.0, 8.0, 20),
            p(7, "Asukal", "food", "", "kg", "1kg", 10, 50.0, 65.0),
            p(8, "Gatas Powder", "beverages", "Bear Brand", "sachet", "25g", 6, 28.0, 38.0, 10),
            p(9, "Sardinas", "canned", "555", "can", "155g", 25, 15.0, 22.0),
            p(10, "Shampoo Sachet", "personal_care", "Sunsilk", "sachet", "", 100, 3.0, 5.0),
            p(11, "Sabon", "personal_care", "Safeguard", "piece", "", 2, 10.0, 16.0, 5),
            p(12, "Toyo", "condiments", "Silver Swan", "bottle", "350mL", 15, 12.0, 18.0),
            p(13, "Chichirya", "snacks", "Jack 'n Jill", "pack", "90g", 40, 8.0, 12.0),
            p(14, "Detergent", "household", "Surf", "sachet", "50g", 35, 6.0, 10.0, 15),
            p(15, "Lighter", "other", "", "piece", "", 24, 7.0, 12.0),
            p(16, "Pisi (Bamboo Ties)", "dry_goods", "", "bundle", "25 pcs", 12, 20.0, 30.0),
            p(17, "Itlog", "food", "", "dozen", "", 4, 90.0, 115.0)
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
                    // v2.59 parity: identity fields round-trip with the data
                    put("category", p.category); put("brand", p.brand)
                    put("packageSize", p.packageSize)
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
                    put("transactionId", s.transactionId)
                    if (s.paymentMethod != null) put("paymentMethod", s.paymentMethod) else put("paymentMethod", org.json.JSONObject.NULL)
                }
            }))
            put("debts", org.json.JSONArray(_debts.value.map { d ->
                JSONObject().apply {
                    put("id", d.id); put("customerName", d.customerName)
                    put("amount", d.amount); put("remainingBalance", d.remainingBalance)
                    put("createdAt", d.createdAt)
                    if (d.creditLimit != null) put("creditLimit", d.creditLimit)
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
                    lowStockThreshold = p.optInt("lowStockThreshold", 5),
                    category = p.optString("category", ""),
                    brand = p.optString("brand", ""),
                    packageSize = p.optString("packageSize", "")
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
                    profit = s.optDouble("profit", 0.0),
                    transactionId = s.optLong("transactionId", 0),
                    paymentMethod = if (s.isNull("paymentMethod")) null else s.optString("paymentMethod", null)
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
                    createdAt = d.optLong("createdAt", System.currentTimeMillis()),
                    creditLimit = if (d.has("creditLimit") && !d.isNull("creditLimit")) {
                        d.optInt("creditLimit", -1).takeIf { it >= 0 }
                    } else null
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

        // Products section (v2.59: identity columns — category, brand, unit, package size)
        sb.appendLine("=== PRODUCTS ===")
        sb.appendLine("ID,Name,Category,Brand,Unit,Package Size,Quantity,Cost Price,Selling Price,Markup,Status")
        _products.value.forEach { p ->
            val margin = if (p.costPrice > 0) String.format("%.1f%%", ((p.sellingPrice - p.costPrice) / p.costPrice) * 100) else "N/A"
            sb.appendLine("${p.id},${p.name},${p.category},${p.brand},${p.unit},${p.packageSize},${p.quantity},${p.costPrice},${p.sellingPrice},$margin,${p.status}")
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

    // ── Reports engine (web v2.55 parity) ────────────────────────────────
    /**
     * Compute everything the Reports screen needs for a period: current-window
     * sales, previous-window comparison totals, utang/receivables summary with
     * aging buckets, and cash collected within the period. Anchored to the
     * (possibly dev-overridden) [today].
     */
    fun computeReportStats(period: String): ReportStats {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = try { fmt.parse(today) ?: Date() } catch (_: Exception) { Date() }
        val dayMs = 24L * 60 * 60 * 1000
        val curLen = when (period) {
            "week" -> 7
            "month" -> 30
            else -> 1
        }
        val curStart = fmt.format(Date(todayDate.time - (curLen - 1) * dayMs))
        val prevEnd = fmt.format(Date(todayDate.time - curLen * dayMs))
        val prevStart = fmt.format(Date(todayDate.time - (2 * curLen - 1) * dayMs))

        val curSales = _specificSales.value.filter { it.date >= curStart && it.date <= today }
        val prevSales = _specificSales.value.filter { it.date >= prevStart && it.date <= prevEnd }
        val activeDebts = _debts.value.filter { it.remainingBalance > 0 }

        // Cash collected within the period: any payment recorded at/after the period start
        val curStartMs = fmt.parse(curStart)?.time ?: 0L
        val collected = _payments.value.filter { it.timestamp >= curStartMs }.sumOf { it.amount }

        // Aging buckets (0-30 / 31-60 / 60+ days) by debt creation date
        val nowMs = System.currentTimeMillis()
        val aging = MutableList(3) { AgingBucket(0.0, 0) }
        activeDebts.forEach { d ->
            val ageDays = ((nowMs - d.createdAt) / dayMs).toInt().coerceAtLeast(0)
            val idx = if (ageDays >= 60) 2 else if (ageDays >= 30) 1 else 0
            val b = aging[idx]
            aging[idx] = AgingBucket(b.amount + d.remainingBalance, b.count + 1)
        }

        return ReportStats(
            period = period,
            sales = curSales,
            prevSalesTotal = prevSales.sumOf { it.amount },
            prevProfitTotal = prevSales.sumOf { it.profit },
            outstandingUtang = activeDebts.sumOf { it.remainingBalance },
            activeDebtors = activeDebts.size,
            collectedThisPeriod = collected,
            aging = aging
        )
    }

    /** Period-scoped CSV report (web exportCurrentReport parity). */
    fun exportReportCsv(period: String): String {
        val st = computeReportStats(period)
        val periodLabel = when (period) {
            "week" -> "Week"
            "month" -> "Month"
            else -> "Day"
        }
        val sb = StringBuilder()
        sb.appendLine("Sari-Sari Smart - Report ($periodLabel)")
        sb.appendLine("Period,$today")
        sb.appendLine("Total Sales,${String.format("%.2f", st.sales.sumOf { it.amount })}")
        sb.appendLine("Total Profit,${String.format("%.2f", st.sales.sumOf { it.profit })}")
        sb.appendLine("Items Sold,${st.sales.sumOf { it.quantity }}")
        sb.appendLine("Transactions,${st.sales.size}")
        sb.appendLine("Cash Sales,${String.format("%.2f", st.sales.filter { it.customerName == null }.sumOf { it.amount })}")
        sb.appendLine("Utang Sales,${String.format("%.2f", st.sales.filter { it.customerName != null }.sumOf { it.amount })}")
        sb.appendLine("vs Previous Sales,${String.format("%.2f", st.prevSalesTotal)}")
        sb.appendLine("Outstanding Utang,${String.format("%.2f", st.outstandingUtang)}")
        sb.appendLine("Active Debtors,${st.activeDebtors}")
        sb.appendLine("Collected This Period,${String.format("%.2f", st.collectedThisPeriod)}")
        sb.appendLine("Aging 0-30 days,${String.format("%.2f", st.aging[0].amount)} (${st.aging[0].count})")
        sb.appendLine("Aging 31-60 days,${String.format("%.2f", st.aging[1].amount)} (${st.aging[1].count})")
        sb.appendLine("Aging 60+ days,${String.format("%.2f", st.aging[2].amount)} (${st.aging[2].count})")
        sb.appendLine()
        sb.appendLine("=== TRANSACTIONS ===")
        sb.appendLine("Date,Description,Quantity,Amount,Profit,Customer")
        st.sales.sortedByDescending { it.timestamp }.forEach { s ->
            sb.appendLine("${s.date},${s.description},${s.quantity},${String.format("%.2f", s.amount)},${String.format("%.2f", s.profit)},${s.customerName ?: "Cash"}")
        }
        sb.appendLine()
        sb.appendLine("=== LOW STOCK ===")
        sb.appendLine("Name,Quantity,Status")
        _products.value.filter { it.status != StockStatus.PLENTY }.sortedBy { it.quantity }.forEach { p ->
            val status = if (p.quantity <= 0) "Out of stock" else "Low"
            sb.appendLine("${p.name},${p.quantity},$status")
        }
        return sb.toString()
    }
}

/** Credit status for a customer given a prospective purchase (web getCreditStatus parity). */
data class CreditStatus(
    val limit: Int,
    val balance: Double,
    val total: Double,
    val overLimit: Boolean,
    val atLimit: Boolean,
    val nearLimit: Boolean
)

/** Aggregated report stats for one period (web computeReportStats parity). */
data class ReportStats(
    val period: String,
    val sales: List<SpecificSale>,
    val prevSalesTotal: Double,
    val prevProfitTotal: Double,
    val outstandingUtang: Double,
    val activeDebtors: Int,
    val collectedThisPeriod: Double,
    val aging: List<AgingBucket>
)

/** One aging bucket (0-30 / 31-60 / 60+ days) — amount outstanding + debtor count. */
data class AgingBucket(
    val amount: Double,
    val count: Int
)
