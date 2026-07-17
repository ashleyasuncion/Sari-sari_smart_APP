package com.example.sari_sari_smart.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.LocalTextScale
import com.example.sari_sari_smart.ui.localization.t

/**
 * A Text composable that applies the user's text size scale factor.
 * Uses the Material theme's typography by default, scaled by the user's preference.
 */
@Composable
fun ScaledText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    i18nKey: String? = null
) {
    val langState = LocalLanguage.current
    val scale by LocalTextScale.current
    val lang = langState.value

    val displayText = if (i18nKey != null) i18nKey.t(lang) else text

    Text(
        text = displayText,
        modifier = modifier,
        style = style.copy(fontSize = style.fontSize * scale)
    )
}

/**
 * Helper function to get a localized string
 */
@Composable
fun rememberT(key: String): String {
    val langState = LocalLanguage.current
    return remember(key, langState.value) { key.t(langState.value) }
}
