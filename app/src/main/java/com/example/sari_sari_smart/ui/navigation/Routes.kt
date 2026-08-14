package com.example.sari_sari_smart.ui.navigation

/**
 * Navigation route constants for the Three Moment architecture.
 *
 * Moments (tab screens, shown in bottom nav):
 *   MORNING — Morning Check: stock warnings, debt summary, "Start Day"
 *   DAY     — Day Mode: live stats, transaction feed, sale/payment sheets
 *   CLOSING — Evening Closing: expenses+earnings, sold items, weekly snapshot
 *
 * Support screens (accessed from header buttons, back navigation):
 *   INVENTORY, DEBTS, ADD_STOCK, NEW_DEBT, CUSTOMER_DEBT_DETAIL,
 *   RECORD_PAYMENT, PRODUCT_DETAIL, SETTINGS, HELP
 */
object Routes {
    // ── App flow ─────────────────────────────────────────
    const val SPLASH = "splash"
    const val SETUP = "setup"

    // ── Three Moments (tab screens) ──────────────────────
    const val MORNING = "morning"
    const val DAY = "day"
    const val CLOSING = "closing"

    // ── Standalone checkout page (web v2.64 parity — the sale sheet overlay
    //    was replaced by a dedicated checkout screen) ─────────────────────
    const val CHECKOUT = "checkout"

    // ── Support screens ──────────────────────────────────
    const val INVENTORY = "inventory"
    const val DEBTS = "debts"
    const val RESTOCK = "restock"
    const val ADD_STOCK = "add_stock/{productId}"
    const val NEW_DEBT = "new_debt"
    const val CUSTOMER_DEBT_DETAIL = "customer_debt_detail/{debtId}"
    const val RECORD_PAYMENT = "record_payment/{debtId}"
    const val PRODUCT_DETAIL = "product_detail/{productId}"
    const val REPORTS = "reports"
    const val EXPENSES = "expenses"
    const val SETTINGS = "settings"
    const val HELP = "help"

    // ── Parameterized route helpers ──────────────────────
    fun addStock(productId: Int = -1) = "add_stock/$productId"
    fun productDetail(productId: Int) = "product_detail/$productId"
    fun customerDebtDetail(debtId: Int) = "customer_debt_detail/$debtId"
    fun recordPayment(debtId: Int) = "record_payment/$debtId"
}
