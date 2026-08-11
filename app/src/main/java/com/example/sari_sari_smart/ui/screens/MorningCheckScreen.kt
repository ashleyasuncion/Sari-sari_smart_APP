package com.example.sari_sari_smart.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.sari_sari_smart.data.StockStatus
import com.example.sari_sari_smart.ui.components.LocalScreenScrollState
import com.example.sari_sari_smart.ui.components.LocalTutorialHighlightState
import com.example.sari_sari_smart.ui.components.tutorialHighlight
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.t
import com.example.sari_sari_smart.ui.theme.*
import com.example.sari_sari_smart.ui.theme.SariSariSmartTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * MORNING CHECK — First of three daily moments.
 * Matches morning.html from the web prototype exactly.
 */
@Composable
fun MorningCheckScreen(
    viewModel: AppViewModel,
    ownerName: String,
    onStartDay: () -> Unit,
    onNavigateToClosing: () -> Unit = {},
    onEditClosing: () -> Unit = {},
    onCloseStaleDayAndStartToday: () -> Unit = {},
    onNavigateToInventory: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRestock: () -> Unit = {},
    onLaunchTutorial: () -> Unit
) {
    val langState = LocalLanguage.current
    val lang = langState.value
    val scrollState = rememberScrollState()

    // Stale open days are NO LONGER auto-archived here (web v2.35 parity) — they
    // are surfaced via the amber overdue banner below so the owner decides.

    val products by viewModel.products.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val eodData by viewModel.endOfDayData.collectAsState()
    val specificSales by viewModel.specificSales.collectAsState()
    // Observable current date — forces this screen to recompose when the real day
    // advances (midnight ticker / app resume), so the overdue banner and other
    // today-dependent values recompute in real time instead of freezing.
    val currentDate by viewModel.currentDate.collectAsState()
    val highlightState = LocalTutorialHighlightState.current

    val outOfStockItems = products.filter { it.status == StockStatus.OUT_OF_STOCK }
    val lowStockItems = products.filter { it.status == StockStatus.LOW }
    val totalDebt = debts.sumOf { it.remainingBalance }
    val activeDebtors = debts.count { it.remainingBalance > 0 }

    // Yesterday's recap
    val yesterdayEod = eodData?.takeIf { it.date != viewModel.today }
    val yesterdayEarnings = yesterdayEod?.actualSales ?: 0.0
    val hasYesterdayData = yesterdayEod != null

    // Context-aware button state (matching web app renderMorningCheck())
    val isDayOpen = viewModel.dayOpen
    val isDayClosedToday = eodData?.date == viewModel.today && eodData?.finished == true && !viewModel.dayArchived
    val hasSalesToday = specificSales.any { it.date == viewModel.today }

    // ── Overdue store state (store left open across business days — web v2.35) ──
    // Re-evaluated on every recomposition; `currentDate` above guarantees the screen
    // recomposes when the date changes, so this flips to true in real time.
    val isStaleOpen = viewModel.isStaleOpenDay()
    val staleDaySales = if (viewModel.dayDate.isBlank()) emptyList()
        else specificSales.filter { it.date == viewModel.dayDate }
    val staleDaysOpen = viewModel.getDaysOpen()
    val openedDateFormatted = remember(viewModel.dayDate) {
        try {
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(viewModel.dayDate) ?: Date())
        } catch (_: Exception) { viewModel.dayDate }
    }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showDevConfirmDialog by remember { mutableStateOf(false) }

    /** Proceed to close the stale day — with a warning dialog if a dev date
     *  override is active (archiving writes to REAL persisted history). */
    fun requestCloseStaleDay() {
        showReviewDialog = false
        if (viewModel.devDateOverride.isNotBlank()) {
            showDevConfirmDialog = true
        } else {
            onCloseStaleDayAndStartToday()
        }
    }

    // Provide scroll state for tutorial auto-scroll
    CompositionLocalProvider(LocalScreenScrollState provides scrollState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Morning greeting (matching webapp: morning-icon, morning-greeting, morning-subtitle) ──
        Spacer(modifier = Modifier.height(16.dp))
        // SVG-style sun icon (matching morning.html morning-icon)
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2600\uFE0F", fontSize = 48.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = viewModel.greetingForTimeKey().t(lang) + ", " + ownerName + " \uD83D\uDC4B",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Gray800
        )
        Text(
            text = "morningSubtitle".t(lang),
            style = MaterialTheme.typography.bodyLarge,
            color = Gray500,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // ── Morning cards (matching webapp: overdue banner + stock warning, debt, yesterday) ──

        // Overdue store banner (store left open across business days — web v2.35)
        if (isStaleOpen) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .tutorialHighlight("morningOverdueCard", highlightState),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Amber50),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Amber100
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("\u26A0\uFE0F", fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "\u26A0\uFE0F " + "overdueTitle".t(lang),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Gray800
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "overdueDesc".t(lang)
                                    .replace("{date}", openedDateFormatted)
                                    .replace("{n}", staleDaysOpen.toString()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray500
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showReviewDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber700)
                    ) {
                        Text(
                            "overdueReview".t(lang),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Stock warnings card (matches morning.html warning-bg card)
        Card(
            onClick = onNavigateToInventory,
            modifier = Modifier
                .fillMaxWidth()
                .tutorialHighlight("morningStockCard", highlightState),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (outOfStockItems.isNotEmpty() || lowStockItems.isNotEmpty())
                        Red50 else Green50
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            if (outOfStockItems.isNotEmpty() || lowStockItems.isNotEmpty())
                                "\u26A0\uFE0F" else "\u2705",
                            fontSize = 22.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (outOfStockItems.isEmpty() && lowStockItems.isEmpty())
                            "\u2705 Lahat ng stock \u2014 okay"
                        else
                            "\u26A0\uFE0F ${outOfStockItems.size} out, ${lowStockItems.size} running low",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray800
                    )
                    if (outOfStockItems.isEmpty() && lowStockItems.isEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "No items running low",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        val allConcerned = outOfStockItems + lowStockItems
                        Column {
                            allConcerned.forEach { item ->
                                val isOut = item.status == StockStatus.OUT_OF_STOCK
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        if (isOut) "\uD83D\uDD34" else "\u26A0\uFE0F",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Gray800,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val statusLabel = if (isOut) {
                                        if (lang == "fil") "\u2014 Walang stock" else "\u2014 No stock"
                                    } else {
                                        if (lang == "fil") "\u2014 ${item.quantity} na lang" else "\u2014 ${item.quantity} left"
                                    }
                                    Text(
                                        statusLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOut) MaterialTheme.colorScheme.error else Gray500
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Debt summary card (matches morning.html debt-bg card)
        Card(
            onClick = onNavigateToDebts,
            modifier = Modifier
                .fillMaxWidth()
                .tutorialHighlight("morningDebtCard", highlightState),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Amber50
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("\uD83D\uDCB0", fontSize = 22.sp)
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "\uD83D\uDCB0 Utang: \u20B1${String.format("%,.2f", totalDebt)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray800
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "$activeDebtors customers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray500
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Yesterday summary card (matches morning.html summary-bg card, hidden when no data)
        if (hasYesterdayData) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Green50
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("\u2705", fontSize = 22.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "yesterday".t(lang).replace("{amount}", "\u20B1${String.format("%,.2f", yesterdayEarnings)}"),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Gray800
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "yesterdayDesc".t(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Restock reminder card (matches morning.html #morningRestockCard)
        val daysSinceRestock = viewModel.daysSinceLastRestock
        if (daysSinceRestock >= 2) {
            val restockDesc = "restockReminderDays".t(lang)
                .replace("{n}", daysSinceRestock.toString())
            Card(
                onClick = onNavigateToRestock, // Navigate to Restock Day screen
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Amber50
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("\uD83D\uDE9A", fontSize = 22.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "\uD83D\uDE9A " + "restockReminder".t(lang),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Gray800
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            restockDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Context-aware button (matching web app renderMorningCheck()):
        //     Day open → "Close Store" → navigate to closing
        //     Day closed today, not archived → "Edit Today's Closing" → reopen
        //     Default → "Start the Day" → startDay
        val buttonLabel = when {
            isStaleOpen -> "overdueCloseStart".t(lang)
            isDayOpen -> "\ud83c\udf19 " + "closeStoreBtn".t(lang)
            isDayClosedToday && hasSalesToday -> "\uD83D\uDCDD " + "editClosing".t(lang)
            else -> "\u2714\uFE0F " + "startDay".t(lang)
        }
        val buttonAction: () -> Unit = when {
            isStaleOpen -> ::requestCloseStaleDay
            isDayOpen -> onNavigateToClosing
            isDayClosedToday && hasSalesToday -> onEditClosing
            else -> onStartDay
        }
        Button(
            onClick = buttonAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .tutorialHighlight("startDayBtn", highlightState),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isStaleOpen || isDayOpen) Amber600 else Green600,
                contentColor = Color.White
            )
        ) {
            Text(
                buttonLabel,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
    }

    // ── Overdue review dialog: previous day's sales + total (web overdueReviewOverlay) ──
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("overdueReviewTitle".t(lang), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (staleDaySales.isEmpty()) {
                        Text(
                            "overdueReviewEmpty".t(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // No inner weight/scroll: M3 AlertDialog's own Column is
                        // already scrollable, so a weighted child would crash.
                        Column {
                            staleDaySales.forEach { sale ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${sale.description} \u00d7 ${sale.quantity}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Gray800,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "\u20B1${String.format("%,.2f", sale.amount)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Gray800
                                    )
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "overdueReviewTotal".t(lang),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Gray800,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "\u20B1${String.format("%,.2f", staleDaySales.sumOf { it.amount })}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Green600
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { requestCloseStaleDay() }) {
                    Text("overdueCloseStart".t(lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) {
                    Text("cancel".t(lang))
                }
            }
        )
    }

    // ── Dev-override confirm: archiving writes to REAL persisted history (web overdueDevConfirm) ──
    if (showDevConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDevConfirmDialog = false },
            title = { Text("\u26A0\uFE0F " + "overdueTitle".t(lang), fontWeight = FontWeight.Bold) },
            text = { Text("overdueDevConfirm".t(lang)) },
            confirmButton = {
                TextButton(onClick = {
                    showDevConfirmDialog = false
                    onCloseStaleDayAndStartToday()
                }) {
                    Text("ok".t(lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDevConfirmDialog = false }) {
                    Text("cancel".t(lang))
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "Morning Check Screen")
@Composable
fun MorningCheckScreenPreview() {
    SariSariSmartTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MorningCheckScreen(
                viewModel = remember { AppViewModel() },
                ownerName = "May-ari",
                onStartDay = {},
                onNavigateToInventory = {},
                onNavigateToDebts = {},
                onNavigateToSettings = {},
                onLaunchTutorial = {}
            )
        }
    }
}
