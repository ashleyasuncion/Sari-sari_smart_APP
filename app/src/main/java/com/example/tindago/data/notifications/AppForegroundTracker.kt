package com.example.tindago.data.notifications

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground suppression flag (V2.70 — analysis §5.1.6 / FR-9).
 * Set by MainActivity.onStart/onStop. The daily worker skips posting while
 * the app is visible — in-app badges/banners already cover those states, and
 * a notification must never interrupt a sale.
 */
object AppForegroundTracker {
    private val foreground = AtomicBoolean(false)

    var isForeground: Boolean
        get() = foreground.get()
        set(value) = foreground.set(value)
}
