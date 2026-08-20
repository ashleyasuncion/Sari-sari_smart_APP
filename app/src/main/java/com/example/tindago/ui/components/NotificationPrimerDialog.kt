package com.example.tindago.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.example.tindago.ui.localization.LocalLanguage
import com.example.tindago.ui.localization.t

/**
 * Contextual permission primer (V2.70 — analysis §5.3.1): explains WHY the app
 * wants to notify BEFORE the OS dialog appears, at a high-intent moment (first
 * "Start the Day" tap) — never on first launch. Accepting leads to the
 * POST_NOTIFICATIONS request; "Not now" dismisses quietly (in-app alerts
 * continue to work either way).
 */
@Composable
fun NotificationPrimerDialog(
    onAllow: () -> Unit,
    onNotNow: () -> Unit
) {
    val lang = LocalLanguage.current.value
    AlertDialog(
        onDismissRequest = onNotNow,
        title = { Text("notifPrimerTitle".t(lang), fontWeight = FontWeight.Bold) },
        text = { Text("notifPrimerBody".t(lang)) },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text("notifAllow".t(lang), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onNotNow) {
                Text("notifNotNow".t(lang))
            }
        }
    )
}
