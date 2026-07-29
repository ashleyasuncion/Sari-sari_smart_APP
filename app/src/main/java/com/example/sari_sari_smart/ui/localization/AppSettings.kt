package com.example.sari_sari_smart.ui.localization

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

val LocalLanguage = compositionLocalOf { mutableStateOf("en") }
val LocalTextScale = compositionLocalOf { mutableStateOf(1f) }

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("sss_prefs", Context.MODE_PRIVATE)

    var language: String
        get() = prefs.getString("language", "en") ?: "en"
        set(value) = prefs.edit().putString("language", value).apply()

    var textSize: String
        get() = prefs.getString("text_size", "standard") ?: "standard"
        set(value) = prefs.edit().putString("text_size", value).apply()

    var storeName: String
        get() = prefs.getString("store_name", "My Store") ?: "My Store"
        set(value) = prefs.edit().putString("store_name", value).apply()

    var ownerName: String
        get() = prefs.getString("owner_name", "Owner") ?: "Owner"
        set(value) = prefs.edit().putString("owner_name", value).apply()

    var hasCompletedSetup: Boolean
        get() = prefs.getBoolean("has_completed_setup", false)
        set(value) = prefs.edit().putBoolean("has_completed_setup", value).apply()

    var hasCompletedTutorial: Boolean
        get() = prefs.getBoolean("has_completed_tutorial", false)
        set(value) = prefs.edit().putBoolean("has_completed_tutorial", value).apply()

    /** Launch counter for tutorial — auto-starts on every launch, no skip on first-ever launch */
    var launchCount: Int
        get() = prefs.getInt("launch_count", 0)
        set(value) = prefs.edit().putInt("launch_count", value).apply()

    var defaultMarkup: Int
        get() = prefs.getInt("default_markup", 20)
        set(value) = prefs.edit().putInt("default_markup", value).apply()

    fun getTextScaleFactor(): Float = when (textSize) {
        "standard" -> 1.0f
        "large" -> 1.125f
        else -> 1.25f
    }

    var dayOpen: Boolean
        get() = prefs.getBoolean("day_open", false)
        set(value) = prefs.edit().putBoolean("day_open", value).apply()

    var dayDate: String
        get() = prefs.getString("day_date", "") ?: ""
        set(value) = prefs.edit().putString("day_date", value).apply()

    var dayArchived: Boolean
        get() = prefs.getBoolean("day_archived", false)
        set(value) = prefs.edit().putBoolean("day_archived", value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

fun String.t(lang: String): String = Strings.get(this, lang)
