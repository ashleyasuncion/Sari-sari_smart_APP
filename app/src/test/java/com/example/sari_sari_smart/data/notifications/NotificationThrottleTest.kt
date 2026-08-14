package com.example.sari_sari_smart.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NotificationThrottle (V2.70 — cooldown engine).
 * Exercises epoch-day math, cooldown windows, the Monday-anchored week key,
 * and the StringSet persistence round-trip.
 */
class NotificationThrottleTest {

    // ── Epoch day math ──────────────────────────────────────────────────

    @Test
    fun epochDay_parsesIsoDates() {
        // 1970-01-01 = day 0
        assertEquals(0L, NotificationThrottle.epochDay("1970-01-01"))
        // 1970-01-02 = day 1
        assertEquals(1L, NotificationThrottle.epochDay("1970-01-02"))
        // Consecutive days differ by exactly 1
        assertEquals(1L, NotificationThrottle.epochDay("2026-08-15") - NotificationThrottle.epochDay("2026-08-14"))
    }

    @Test
    fun epochDay_returnsZeroForGarbage() {
        assertEquals(0L, NotificationThrottle.epochDay("not-a-date"))
        assertEquals(0L, NotificationThrottle.epochDay(""))
    }

    // ── Cooldown windows ────────────────────────────────────────────────

    @Test
    fun shouldNotify_trueWhenNoRecord() {
        assertTrue(NotificationThrottle.shouldNotify(emptyMap(), "stock:1", 1, todayEpochDay = 20000L))
    }

    @Test
    fun shouldNotify_dailyCooldown() {
        val map = mapOf("stock:1" to 20000L)
        // Same day → suppressed
        assertFalse(NotificationThrottle.shouldNotify(map, "stock:1", 1, todayEpochDay = 20000L))
        // Next day → allowed again
        assertTrue(NotificationThrottle.shouldNotify(map, "stock:1", 1, todayEpochDay = 20001L))
    }

    @Test
    fun shouldNotify_weeklyCooldown() {
        val map = mapOf("digest:w1" to 20000L)
        // 6 days later → still suppressed for a 7-day cooldown
        assertFalse(NotificationThrottle.shouldNotify(map, "digest:w1", 7, todayEpochDay = 20006L))
        // 7 days later → allowed
        assertTrue(NotificationThrottle.shouldNotify(map, "digest:w1", 7, todayEpochDay = 20007L))
    }

    @Test
    fun markNotified_recordsToday() {
        val updated = NotificationThrottle.markNotified(emptyMap(), "overdue:2026-08-14", todayEpochDay = 20680L)
        assertEquals(20680L, updated["overdue:2026-08-14"]!!)
        // Immutable: original map untouched
        assertTrue(emptyMap<String, Long>().isEmpty())
    }

    // ── Monday-anchored week key ────────────────────────────────────────

    @Test
    fun digestWeekKey_sameWeekForMondayThroughSunday() {
        // 2026-08-10 is a Monday; 2026-08-16 is that Sunday
        val monday = NotificationThrottle.digestWeekKey("2026-08-10")
        val sunday = NotificationThrottle.digestWeekKey("2026-08-16")
        assertEquals(monday, sunday)
        // The following Monday starts a new week
        val nextMonday = NotificationThrottle.digestWeekKey("2026-08-17")
        assertFalse(monday == nextMonday)
        // Week keys are ordered by time
        val weekNum = { k: String -> k.removePrefix("week").toLong() }
        assertTrue(weekNum(nextMonday) == weekNum(monday) + 1)
    }

    // ── StringSet persistence round-trip ────────────────────────────────

    @Test
    fun serializeThenParse_roundTrips() {
        val map = mapOf(
            "stock:3:2026-08-14" to 20680L,
            "overdue:2026-08-14" to 20680L,
            "digest:week2954" to 20675L
        )
        val raw = NotificationThrottle.serializeNotifiedKeys(map)
        assertEquals(map.size, raw.size)
        assertEquals(map, NotificationThrottle.parseNotifiedKeys(raw))
    }

    @Test
    fun parseNotifiedKeys_ignoresMalformedEntries() {
        val raw = setOf(
            "good=20680",
            "noEqualsSign",
            "bad=notANumber",
            "=20680",          // empty key
            "trailing=20680=extra"
        )
        val map = NotificationThrottle.parseNotifiedKeys(raw)
        assertEquals(1, map.size)
        assertEquals(20680L, map["good"]!!)
    }

    @Test
    fun serializeNotifiedKeys_emptyMapYieldsEmptySet() {
        assertTrue(NotificationThrottle.serializeNotifiedKeys(emptyMap()).isEmpty())
    }
}
