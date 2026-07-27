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

/**
 * MORNING CHECK — First of three daily moments.
 * Matches morning.html from the web prototype exactly.
 */
@Composable
fun MorningCheckScreen(
    viewModel: AppViewModel,
    onStartDay: () -> Unit,
    onNavigateToClosing: () -> Unit = {},
    onEditClosing: () -> Unit = {},
    onNavigateToInventory: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRestock: () -> Unit = {},
    onLaunchTutorial: () -> Unit
) {
    val langState = LocalLanguage.current
    val lang = langState.value
    val scrollState = rememberScrollState()

    val products by viewModel.products.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val eodData by viewModel.endOfDayData.collectAsState()
    val highlightState = LocalTutorialHighlightState.current

    val outOfStockItems = products.filter { it.status == StockStatus.OUT_OF_STOCK }
    val lowStockItems = products.filter { it.status == StockStatus.LOW }
    val totalDebt = debts.sumOf { it.remainingBalance }
    val activeDebtors = debts.count { it.remainingBalance > 0 }

    // Yesterday's recap
    val yesterdayEod = eodData?.takeIf { it.date != viewModel.today }
    val yesterdayProfit = yesterdayEod?.profit ?: 0.0
    val hasYesterdayData = yesterdayEod != null

    // Context-aware button state (matching web app renderMorningCheck())
    val isDayOpen = viewModel.dayOpen
    val isDayClosedToday = eodData?.date == viewModel.today && eodData?.finished == true && !viewModel.dayArchived
    val hasSalesToday = viewModel.specificSales.value.any { it.date == viewModel.today }

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
            text = "morningGreeting".t(lang),
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

        // ── Morning cards (matching webapp: 3 cards: stock warning, debt, yesterday) ──

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
                            "Kahapon: \u20B1${String.format("%,.2f", yesterdayProfit)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Gray800
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Yesterday's profit",
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
            isDayOpen -> "\ud83c\udf19 " + "closeStoreBtn".t(lang)
            isDayClosedToday && hasSalesToday -> "\uD83D\uDCDD " + "editClosing".t(lang)
            else -> "\u2714\uFE0F " + "startDay".t(lang)
        }
        val buttonAction = when {
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
                containerColor = if (isDayOpen) Amber600 else Green600,
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
}

@Preview(showBackground = true, name = "Morning Check Screen")
@Composable
fun MorningCheckScreenPreview() {
    SariSariSmartTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MorningCheckScreen(
                viewModel = remember { AppViewModel() },
                onStartDay = {},
                onNavigateToInventory = {},
                onNavigateToDebts = {},
                onNavigateToSettings = {},
                onLaunchTutorial = {}
            )
        }
    }
}
