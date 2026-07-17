package com.example.sari_sari_smart.data

import com.example.sari_sari_smart.ui.localization.t
import java.util.*
import kotlin.math.abs

/**
 * Formats a timestamp as a relative "time ago" string.
 * Uses the bilingual strings already defined in Strings.kt:
 *   justNow → "just now" / "ngayon lang"
 *   minAgo → "{n}m ago" / "{n}m ang nakaraan"
 *   hourAgo → "{n}h ago" / "{n}h ang nakaraan"
 */
fun formatTimeAgo(timestamp: Long, lang: String): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp
    val diffMin = abs(diffMs / 60_000)
    val diffHour = diffMin / 60
    val diffDay = diffHour / 24

    return when {
        diffMin < 1 -> "justNow".t(lang)
        diffMin < 60 -> "minAgo".t(lang).replace("{n}", diffMin.toString())
        diffHour < 24 -> "hourAgo".t(lang).replace("{n}", diffHour.toString())
        diffDay < 7 -> {
            if (diffDay == 1L) "Yesterday" else "${diffDay} days ago"
        }
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM d", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * Returns the current date formatted as yyyy-MM-dd for comparison.
 */
fun todayDateString(): String {
    return java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
