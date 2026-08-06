package com.example.sari_sari_smart.data

/**
 * Data models matching the web prototype (git/Sari-sari_smart/)
 */
data class Product(
    val id: Int,
    val name: String,
    val quantity: Int,
    val costPrice: Double,
    val sellingPrice: Double,
    val unit: String = "piece",
    val lowStockThreshold: Int = 5
) {
    val status: StockStatus get() = when {
        quantity <= 0 -> StockStatus.OUT_OF_STOCK
        quantity <= lowStockThreshold -> StockStatus.LOW
        else -> StockStatus.PLENTY
    }
}

enum class StockStatus { PLENTY, LOW, OUT_OF_STOCK }

data class DailyEntry(
    val date: String,
    val stockExpenses: Double,
    val earnings: Double
) {
    val grossProfit: Double get() = earnings - stockExpenses
}

data class SpecificSale(
    val id: Int,
    val date: String,
    val description: String,
    val amount: Double,
    val quantity: Int = 1,
    val customerName: String? = null,
    val profit: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomerDebt(
    val id: Int,
    val customerName: String,
    val amount: Double,
    val remainingBalance: Double,
    val createdAt: Long = System.currentTimeMillis()
)

data class EndOfDayData(
    val date: String,
    val cashInDrawer: Double = 0.0,
    val stockCheckDone: Boolean = false,
    val debtPaymentsDone: Boolean = false,
    val finished: Boolean = false,
    val recordedSales: Double = 0.0,
    val actualSales: Double = 0.0,
    val salesDiff: Double = 0.0,
    val costOfGoods: Double = 0.0,
    val profit: Double = 0.0
)

data class DebtPayment(
    val id: Int,
    val debtId: Int,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)

/**
 * Per-debt transaction ledger — mirrors the web's `debt.transactions[]` array.
 * Every debt-balance increase (utang sale, manual add) becomes a row so the
 * debt history can show individual entries with descriptions and dates,
 * exactly like the web prototype. Payments live in [DebtPayment].
 */
data class DebtTransaction(
    val id: Int,
    val debtId: Int,
    val type: String,            // "debt" = added to balance
    val description: String?,    // product name (utang sale) or "Manual"
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

// ── Restock Day Models ─────────────────────────────────────────────────

data class RestockLogEntry(
    val id: String,
    val date: String,
    val items: List<PurchaseEntry>,
    val totalCost: Double
)

data class RestockTempState(
    val step: Int = 1,
    val corrections: List<Correction> = emptyList(),
    val purchases: List<PurchaseEntry> = emptyList()
)

data class Correction(
    val productId: String? = null,
    val productEntityId: Int = 0,
    val oldQty: Int = 0,
    val newQty: Int = 0
)

data class PurchaseEntry(
    val productId: String? = null,
    val productEntityId: Int = 0,
    val productName: String,
    val costPerUnit: Double,
    val qtyAdded: Int,
    val totalCost: Double
)
