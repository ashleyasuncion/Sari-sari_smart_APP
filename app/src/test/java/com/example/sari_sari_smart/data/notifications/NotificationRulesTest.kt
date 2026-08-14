package com.example.sari_sari_smart.data.notifications

import com.example.sari_sari_smart.data.CustomerDebt
import com.example.sari_sari_smart.data.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NotificationRules (V2.70 — pure rules engine).
 * Every rule mirrors an existing in-app threshold, so the tests pin the
 * notification behavior to the UI behavior (no divergence).
 */
class NotificationRulesTest {

    // ── Overdue store (mirrors isStaleOpenDay) ──────────────────────────

    @Test
    fun dueOverdue_trueWhenOpenOnPreviousDay() {
        assertTrue(NotificationRules.dueOverdue(dayOpen = true, dayDate = "2026-08-13", today = "2026-08-14"))
    }

    @Test
    fun dueOverdue_falseWhenClosedOrSameDayOrFuture() {
        assertFalse(NotificationRules.dueOverdue(dayOpen = false, dayDate = "2026-08-13", today = "2026-08-14"))
        assertFalse(NotificationRules.dueOverdue(dayOpen = true, dayDate = "2026-08-14", today = "2026-08-14"))
        // Clock moved backward must never flag a future day
        assertFalse(NotificationRules.dueOverdue(dayOpen = true, dayDate = "2026-08-15", today = "2026-08-14"))
    }

    @Test
    fun dueOverdue_falseWhenDateBlank() {
        assertFalse(NotificationRules.dueOverdue(dayOpen = true, dayDate = "", today = "2026-08-14"))
    }

    // ── Out of stock (mirrors stock status) ─────────────────────────────

    @Test
    fun outOfStockItems_returnsOnlyZeroQuantity() {
        val products = listOf(
            Product(id = 1, name = "Asin", quantity = 0, costPrice = 10.0, sellingPrice = 15.0),
            Product(id = 2, name = "Toyo", quantity = -2, costPrice = 12.0, sellingPrice = 18.0),
            Product(id = 3, name = "Canned Tuna", quantity = 30, costPrice = 18.0, sellingPrice = 25.0),
            Product(id = 4, name = "Sabon", quantity = 5, costPrice = 10.0, sellingPrice = 16.0)
        )
        val out = NotificationRules.outOfStockItems(products)
        assertEquals(listOf(1, 2), out.map { it.id })
    }

    @Test
    fun outOfStockItems_emptyWhenAllInStock() {
        val products = listOf(
            Product(id = 1, name = "A", quantity = 1, costPrice = 1.0, sellingPrice = 2.0),
            Product(id = 2, name = "B", quantity = 5, costPrice = 1.0, sellingPrice = 2.0)
        )
        assertTrue(NotificationRules.outOfStockItems(products).isEmpty())
    }

    // ── Restock due (mirrors the Morning-page restock card) ─────────────

    @Test
    fun dueRestock_trueAtTwoDays() {
        assertTrue(NotificationRules.dueRestock(2))
        assertTrue(NotificationRules.dueRestock(5))
    }

    @Test
    fun dueRestock_falseBeforeTwoDays() {
        assertFalse(NotificationRules.dueRestock(0))
        assertFalse(NotificationRules.dueRestock(1))
    }

    // ── Closing reminder ────────────────────────────────────────────────

    @Test
    fun dueClosing_trueOnlyWhenOpenAndNotFinished() {
        assertTrue(NotificationRules.dueClosing(dayOpen = true, isEodComplete = false))
        assertFalse(NotificationRules.dueClosing(dayOpen = false, isEodComplete = false))
        assertFalse(NotificationRules.dueClosing(dayOpen = true, isEodComplete = true))
    }

    // ── Quiet hours (6:00–21:00) ────────────────────────────────────────

    @Test
    fun quietHoursAllowed_bounds() {
        assertTrue(NotificationRules.quietHoursAllowed(6))
        assertTrue(NotificationRules.quietHoursAllowed(12))
        assertTrue(NotificationRules.quietHoursAllowed(21))
        assertFalse(NotificationRules.quietHoursAllowed(5))
        assertFalse(NotificationRules.quietHoursAllowed(22))
    }

    // ── Effective credit limit (mirrors getEffectiveCreditLimit) ────────

    private val debts = listOf(
        CustomerDebt(id = 1, customerName = "Aling Nena", amount = 300.0, remainingBalance = 300.0, creditLimit = 1000),
        CustomerDebt(id = 2, customerName = "Mang Kanor", amount = 200.0, remainingBalance = 200.0, creditLimit = 0),
        CustomerDebt(id = 3, customerName = "Pedro", amount = 100.0, remainingBalance = 100.0)
    )

    @Test
    fun effectiveCreditLimit_customWinsAndCaseInsensitive() {
        assertEquals(1000, NotificationRules.effectiveCreditLimit(debts, "aling nena", 500))
        assertEquals(1000, NotificationRules.effectiveCreditLimit(debts, "ALING NENA", 500))
        // 0 = no limit, kept
        assertEquals(0, NotificationRules.effectiveCreditLimit(debts, "Mang Kanor", 500))
        // Unknown name → global default
        assertEquals(500, NotificationRules.effectiveCreditLimit(debts, "Liza", 500))
    }

    // ── Weekly digest summary ───────────────────────────────────────────

    @Test
    fun digestSummary_totalsActiveBalancesOnly() {
        val dayMs = 86_400_000L
        val now = System.currentTimeMillis()
        val ds = NotificationRules.digestSummary(
            listOf(
                CustomerDebt(id = 1, customerName = "A", amount = 100.0, remainingBalance = 100.0, createdAt = now - 10 * dayMs),
                CustomerDebt(id = 2, customerName = "B", amount = 50.0, remainingBalance = 50.0, createdAt = now - 70 * dayMs),
                // settled → excluded
                CustomerDebt(id = 3, customerName = "C", amount = 500.0, remainingBalance = 0.0, createdAt = now - 80 * dayMs)
            ),
            defaultLimit = 500,
            now = now
        )
        assertEquals(150.0, ds.outstandingTotal, 0.001)
        assertEquals(0, ds.overLimitCount)
        assertEquals(1, ds.aging60PlusCount)
    }

    @Test
    fun digestSummary_countsOverLimitDebtorsByName() {
        val dayMs = 86_400_000L
        val now = System.currentTimeMillis()
        // Nena: 300 + 200 = 500 → AT limit → counts
        // Kanor: 400 → under limit → not counted
        // Liza: 600 but creditLimit 0 → no limit → not counted
        val ds = NotificationRules.digestSummary(
            listOf(
                CustomerDebt(id = 1, customerName = "Aling Nena", amount = 300.0, remainingBalance = 300.0, createdAt = now - 5 * dayMs),
                CustomerDebt(id = 2, customerName = "Aling Nena", amount = 200.0, remainingBalance = 200.0, createdAt = now - 5 * dayMs),
                CustomerDebt(id = 3, customerName = "Mang Kanor", amount = 400.0, remainingBalance = 400.0, createdAt = now - 5 * dayMs),
                CustomerDebt(id = 4, customerName = "Liza", amount = 600.0, remainingBalance = 600.0, creditLimit = 0, createdAt = now - 5 * dayMs)
            ),
            defaultLimit = 500,
            now = now
        )
        assertEquals(1500.0, ds.outstandingTotal, 0.001)
        assertEquals(1, ds.overLimitCount)
    }
}
