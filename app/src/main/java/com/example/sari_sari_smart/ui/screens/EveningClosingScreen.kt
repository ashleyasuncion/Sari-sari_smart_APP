package com.example.sari_sari_smart.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.sari_sari_smart.data.StockStatus
import com.example.sari_sari_smart.ui.components.LocalTutorialHighlightState
import com.example.sari_sari_smart.ui.components.tutorialHighlight
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.t
import com.example.sari_sari_smart.ui.theme.*
import com.example.sari_sari_smart.ui.theme.SariSariSmartTheme

/**
 * EVENING CLOSING — Third of three daily moments.
 * Matches closing.html from the web prototype exactly.
 * Sales section: Cash Sales Today (read-only), Cash Counted (input),
 * Cash Difference (auto), Profit from Items Sold (auto), Last Restock info.
 */
@Composable
fun EveningClosingScreen(
    viewModel: AppViewModel,
    onComplete: () -> Unit,
    onBackToDay: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToDebts: () -> Unit,
    onLaunchTutorial: () -> Unit
) {
    val langState = LocalLanguage.current
    val lang = langState.value
    val scrollState = rememberScrollState()
    val highlightState = LocalTutorialHighlightState.current

    val specificSales by viewModel.specificSales.collectAsState()
    val products by viewModel.products.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val lastRestockDate by viewModel.lastRestockDate.collectAsState()

    val today = viewModel.today
    val todaySales = specificSales.filter { it.date == today }
    val recordedSales = viewModel.todayRecordedSales
    // Per-sale profit (sum of each item's (sellingPrice - costPrice) × qty)
    val perSaleProfit = viewModel.todayProfit

    // End-of-day data — used to pre-fill Actual Sales when editing today's closing
    val eodDataForForm by viewModel.endOfDayData.collectAsState()

    // Detect edit mode — check if we're reopening today's finished closing
    val isEditMode = eodDataForForm?.date == today && eodDataForForm?.finished == true && !viewModel.dayArchived

    // Cash Counted input — pre-filled only when editing today's closing.
    // On a fresh day it starts EMPTY (web parity: closing.html begins blank);
    // the legacy fallback to dailyEntry.earnings is removed so a stale
    // persisted value (e.g. a previously entered 1,250,000) can never leak
    // into the Actual Sales field or the closing calculations.
    var actualSales by remember {
        mutableStateOf(
            if (isEditMode) eodDataForForm?.actualSales?.let { if (it > 0) it.toString() else "" } ?: ""
            else ""
        )
    }

    val asVal = actualSales.toDoubleOrNull() ?: 0.0
    val salesDiff = viewModel.getSalesDiff(asVal)
    // Profit: per-sale profit (sum of each item's margin × qty)
    // This matches web app's getTodayProfit() behavior
    val profitTotal = perSaleProfit

    // Weekly snapshot
    val weekSales = viewModel.getWeekSales()
    val topSeller = if (todaySales.isNotEmpty()) {
        todaySales.groupBy { it.description }.maxByOrNull { it.value.sumOf { s -> s.quantity } }?.key ?: "\u2014"
    } else "\u2014"

    // Show day complete overlay
    var showComplete by remember { mutableStateOf(false) }

    if (showComplete) {
        EveningCompleteOverlay(
            recordedSales = recordedSales,
            actualSales = asVal,
            salesDiff = salesDiff,
            profit = profitTotal,
            itemsSoldCount = todaySales.sumOf { it.quantity },
            lowStockCount = products.count { it.status != StockStatus.PLENTY },
            totalDebts = debts.sumOf { it.remainingBalance },
            onEditClosing = {
                // Re-open closing for editing
                showComplete = false
            },
            onPrepareTomorrow = {
                viewModel.completeEndOfDay(actualSales = asVal)
                showComplete = false
                onComplete()
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // ── Header ──
        Spacer(modifier = Modifier.height(16.dp))
        Text("\uD83C\uDF19", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "closingTitle".t(lang),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Gray800
        )
        Text(
            "closingSubtitle".t(lang),
            style = MaterialTheme.typography.bodyLarge,
            color = Gray500,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // ── Sales Section ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .tutorialHighlight("closingEarnings", highlightState),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "closingSectionSales".t(lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray700,
                    modifier = Modifier.padding(bottom = 12.dp)
                )                // Cash Sales Today (read-only) — matching webapp closing.html
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "closingRecordedSales".t(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500
                        )
                        Text(
                            "\u20B1${String.format("%,.2f", recordedSales)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Green600
                        )
                    }

                    HorizontalDivider(color = Gray100, modifier = Modifier.padding(vertical = 8.dp))

                    // Cash Counted (input) — matching webapp closing.html
                    Text(
                        "closingActualSales".t(lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = actualSales,
                        onValueChange = { actualSales = it },
                        leadingIcon = { Text("\u20B1", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Cash Difference (auto) — matching webapp closing.html
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "closingSalesDiff".t(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                        val diffTextColor = when {
                            salesDiff == 0.0 -> Gray500
                            salesDiff > 0 -> Green600
                            else -> Red500
                        }
                        val diffPrefix = if (salesDiff >= 0) "+" else ""
                        Text(
                            "$diffPrefix\u20B1${String.format("%,.2f", salesDiff)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = diffTextColor
                        )
                    }

                    HorizontalDivider(color = Green200, modifier = Modifier.padding(vertical = 8.dp))

                    // Profit from Items Sold (per-sale) — matching web app getTodayProfit()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "closingProfitLabel".t(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Gray800
                        )
                        Text(
                            "\u20B1${String.format("%,.2f", profitTotal)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Green600
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "closingProfitHint".t(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )

                    // Last Restock info (matching webapp closing.html behavior)
                    val daysSinceRestock = viewModel.daysSinceLastRestock
                    if (daysSinceRestock >= 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val restockText = when {
                            daysSinceRestock == 0 -> if (lang == "fil") "Nag-restock ngayong araw!" else "Restocked today!"
                            else -> "restockReminderDays".t(lang).replace("{n}", daysSinceRestock.toString())
                        }
                        Text(
                            restockText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray400
                        )
                    }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Sold Items ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "closingSectionSold".t(lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray700,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (todaySales.isEmpty()) {
                    Text(
                        "noSales".t(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray400,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    todaySales.take(10).forEach { sale ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(sale.description, style = MaterialTheme.typography.bodyMedium, color = Gray800)
                                if (sale.quantity > 1) {
                                    Text("x${sale.quantity}", style = MaterialTheme.typography.bodySmall, color = Gray400)
                                }
                            }
                            Text(
                                "\u20B1${String.format("%,.2f", sale.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Gray800
                            )
                        }
                        if (todaySales.last() != sale) {
                            HorizontalDivider(color = Gray100, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Low Stock ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "closingSectionLowStock".t(lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray700,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val lowItems = products.filter { it.status != StockStatus.PLENTY }.sortedBy { it.quantity }
                if (lowItems.isEmpty()) {
                    Text(
                        "closingLowStockOk".t(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Green600,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    lowItems.take(5).forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(p.name, style = MaterialTheme.typography.bodyMedium, color = Gray800)
                            Text(
                                "${p.quantity} left",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (p.quantity <= 0) Red500 else Amber700,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Outstanding Debts ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .tutorialHighlight("closingUtang", highlightState),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "closingSectionDebts".t(lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray700,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val activeDebts = debts.filter { it.remainingBalance > 0 }
                if (activeDebts.isEmpty()) {
                    Text(
                        "closingNoDebts".t(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Green600,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    activeDebts.take(5).forEach { d ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(d.customerName, style = MaterialTheme.typography.bodyMedium, color = Gray800)
                            Text(
                                "\u20B1${String.format("%,.2f", d.remainingBalance)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Red500,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Weekly Snapshot ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "closingSectionWeekly".t(lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray700,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("closingWeeklyLabel".t(lang), style = MaterialTheme.typography.bodyMedium, color = Gray500)
                    Text(
                        "\u20B1${String.format("%,.2f", weekSales)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray800
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("closingTopSellerLabel".t(lang), style = MaterialTheme.typography.bodyMedium, color = Gray500)
                    Text(
                        topSeller,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray800
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Complete button (edit mode shows "Update Closing" matching webapp behavior) ──
        Button(
            onClick = { showComplete = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .tutorialHighlight("completeDayBtn", highlightState),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green600,
                contentColor = Color.White
            )
        ) {
            Text(
                if (isEditMode) "\u270F\uFE0F ${"updateClosing".t(lang)}" else "\u2714\uFE0F ${"completeDayBtn".t(lang)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Back to Day ──
        OutlinedButton(
            onClick = onBackToDay,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("← ${"backToDayBtn".t(lang)}")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EveningCompleteOverlay(
    recordedSales: Double,
    actualSales: Double,
    salesDiff: Double,
    profit: Double,
    itemsSoldCount: Int,
    lowStockCount: Int,
    totalDebts: Double,
    onEditClosing: () -> Unit = {},
    onPrepareTomorrow: () -> Unit
) {
    val langState = LocalLanguage.current
    val lang = langState.value

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("\uD83C\uDF19", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "dayCompleteTitle".t(lang),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gray800
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "closingRestNote".t(lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray500
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Summary card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Green50)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        OverlayRow("closingRecordedSales".t(lang), "\u20B1${String.format("%,.2f", recordedSales)}", Gray600)
                        OverlayRow("closingActualSales".t(lang), "\u20B1${String.format("%,.2f", actualSales)}", Green600)
                        if (salesDiff != 0.0) {
                            val diffColor = if (salesDiff > 0) Green600 else Red500
                            val diffSign = if (salesDiff > 0) "+" else ""
                            OverlayRow("closingSalesDiff".t(lang), "$diffSign\u20B1${String.format("%,.2f", salesDiff)}", diffColor)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        OverlayRow("closingProfitLabel".t(lang), "\u20B1${String.format("%,.2f", profit)}", Green600, bold = true)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStat(icon = "\uD83D\uDCE6", value = "${itemsSoldCount}", label = "soldLabel".t(lang))
                    MiniStat(icon = "\u26A0\uFE0F", value = "${lowStockCount}", label = "lowStockLabel".t(lang))
                    MiniStat(icon = "\uD83D\uDCB0", value = "\u20B1${String.format("%,.0f", totalDebts)}", label = "debtsLabel".t(lang))
                }

                Spacer(modifier = Modifier.height(16.dp))                    // Edit Closing button — wired to reopenClosing()
                OutlinedButton(
                    onClick = onEditClosing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("\u270F\uFE0F ${"editClosing".t(lang)}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onPrepareTomorrow,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("\uD83C\uDF1E ${"prepareTomorrow".t(lang)}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OverlayRow(label: String, value: String, valueColor: Color, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Gray500)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun MiniStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Gray800
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = Gray500
        )
    }
}

@Preview(showBackground = true, name = "Evening Closing Screen")
@Composable
fun EveningClosingScreenPreview() {
    SariSariSmartTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            EveningClosingScreen(
                viewModel = remember { AppViewModel() },
                onComplete = {},
                onBackToDay = {},
                onNavigateToInventory = {},
                onNavigateToDebts = {},
                onLaunchTutorial = {}
            )
        }
    }
}
