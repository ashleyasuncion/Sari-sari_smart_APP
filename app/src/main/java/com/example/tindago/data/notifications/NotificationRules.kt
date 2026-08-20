package com.example.tindago.data.notifications

import com.example.tindago.data.CustomerDebt
import com.example.tindago.data.Product

/**
 * Pure notification rules engine (V2.70 — analysis §5.1.2 / FR-1 / NFR-3).
 * Zero Android dependencies → plain JVM unit tests. Every rule mirrors an
 * EXISTING in-app threshold so notifications and the UI never disagree:
 *   - dueOverdue        = AppViewModel.isStaleOpenDay()
 *   - dueRestock        = Morning-page restock card (daysSinceLastRestock >= 2)
 *   - dueClosing        = dayOpen && !isEodComplete (Closing page state)
 *   - outOfStockItems   = product.quantity <= 0
 *   - quietHours        = 6:00-21:00 (sari-sari store reality)
 */
object NotificationRules {

    /** True when the store is open but the business day started on a previous
     *  calendar day (mirrors isStaleOpenDay; `<` so a clock moved backward never
     *  flags a future day). */
    fun dueOverdue(dayOpen: Boolean, dayDate: String, today: String): Boolean =
        dayOpen && dayDate.isNotBlank() && dayDate < today

    /** Items with zero quantity — direct revenue loss candidates. */
    fun outOfStockItems(products: List<Product>): List<Product> =
        products.filter { it.quantity <= 0 }

    /** Restock overdue when >= 2 days since the last restock (Morning-page rule). */
    fun dueRestock(daysSinceLastRestock: Int): Boolean = daysSinceLastRestock >= 2

    /** Closing reminder: day is open and end-of-day is not finished. */
    fun dueClosing(dayOpen: Boolean, isEodComplete: Boolean): Boolean =
        dayOpen && !isEodComplete

    /** Quiet hours 6:00-21:00 — no notifications outside them. */
    fun quietHoursAllowed(hour: Int): Boolean = hour in 6..21

    /** Effective credit limit for a name: per-customer override wins, else the
     *  global default (0 = no limit) — mirrors getEffectiveCreditLimit(). */
    fun effectiveCreditLimit(debts: List<CustomerDebt>, name: String, defaultLimit: Int): Int {
        val debt = debts.firstOrNull {
            it.customerName.trim().equals(name.trim(), ignoreCase = true)
        }
        val custom = debt?.creditLimit
        return if (custom != null && custom >= 0) custom else defaultLimit
    }

    /** Weekly digest summary: outstanding total, over-limit debtor count, and
     *  60+ day aging count. Generic numbers only — no per-item noise (FR-6). */
    data class DigestSummary(
        val outstandingTotal: Double,
        val overLimitCount: Int,
        val aging60PlusCount: Int
    )

    fun digestSummary(
        debts: List<CustomerDebt>,
        defaultLimit: Int,
        now: Long = System.currentTimeMillis()
    ): DigestSummary {
        val active = debts.filter { it.remainingBalance > 0 }
        val outstanding = active.sumOf { it.remainingBalance }

        // Over-limit debtors: name-grouped totals at-or-above their limit.
        val nameTotals = HashMap<String, Double>()
        active.forEach { d ->
            nameTotals[d.customerName] = (nameTotals[d.customerName] ?: 0.0) + d.remainingBalance
        }
        val overLimit = nameTotals.count { (name, total) ->
            val limit = effectiveCreditLimit(active, name, defaultLimit)
            limit > 0 && total >= limit - 0.005
        }

        val dayMs = 86_400_000L
        val aging60Plus = active.count { (now - it.createdAt) / dayMs >= 60 }

        return DigestSummary(outstanding, overLimit, aging60Plus)
    }
}
