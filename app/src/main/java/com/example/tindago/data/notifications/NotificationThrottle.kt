package com.example.tindago.data.notifications

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pure notification-throttle logic (V2.70 — analysis §5.1.5 / FR-5).
 * Tracks a map of key -> last-notified epoch day so the same event is not
 * re-alerted within its cooldown: once per item per day for stock, once per
 * day for overdue/closing, once per week for the digest.
 *
 * The map is persisted via AppSettings.notifiedKeys as a StringSet of
 * "key=epochDay" entries ([serializeNotifiedKeys] / [parseNotifiedKeys]).
 */
object NotificationThrottle {

    private const val DAY_MS = 86_400_000L

    /** Epoch day (days since epoch) for a YYYY-MM-DD string; 0 when unparseable.
     *  Pinned to UTC so the day boundary is identical on every device/zone. */
    fun epochDay(date: String): Long = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse(date)?.time?.div(DAY_MS) ?: 0L
    } catch (_: Exception) { 0L }

    /** Epoch day for the current local calendar day, derived from the same
     *  UTC-pinned date math as [epochDay] so keys stay comparable everywhere. */
    fun todayEpochDay(): Long = epochDay(
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(java.util.Date())
    )

    /** True when the key has no record or its cooldown (in days) has elapsed. */
    fun shouldNotify(
        lastNotified: Map<String, Long>,
        key: String,
        cooldownDays: Int,
        todayEpochDay: Long = todayEpochDay()
    ): Boolean {
        val last = lastNotified[key] ?: return true
        return todayEpochDay - last >= cooldownDays
    }

    /** Record that a key was notified today (returns the updated map, immutable). */
    fun markNotified(
        lastNotified: Map<String, Long>,
        key: String,
        todayEpochDay: Long = todayEpochDay()
    ): Map<String, Long> = lastNotified + (key to todayEpochDay)

    /** Monday-anchored week key for the once-per-week digest (e.g. "week2627"). */
    fun digestWeekKey(today: String): String {
        // epochDay 0 = 1970-01-01 (a Thursday). Adding 3 shifts to the Monday
        // boundary, so (day + 3) / 7 yields the Monday-start week number.
        val week = (epochDay(today) + 3) / 7
        return "week$week"
    }

    /** Parse the persisted StringSet ("key=epochDay") back into a map. */
    fun parseNotifiedKeys(raw: Set<String>): Map<String, Long> {
        val map = HashMap<String, Long>()
        raw.forEach { entry ->
            val eq = entry.indexOf('=')
            if (eq > 0) {
                val key = entry.substring(0, eq)
                val value = entry.substring(eq + 1).toLongOrNull()
                if (value != null) map[key] = value
            }
        }
        return map
    }

    /** Serialize the throttle map into the persisted StringSet form. */
    fun serializeNotifiedKeys(map: Map<String, Long>): Set<String> =
        map.map { (key, day) -> "$key=$day" }.toSet()
}
