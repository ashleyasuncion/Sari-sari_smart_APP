package com.example.sari_sari_smart.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.sari_sari_smart.data.formatTimeAgo
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
 * DAY MODE — Second of three daily moments.
 * Matches day.html from the web prototype exactly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayModeScreen(
    viewModel: AppViewModel,
    onCloseStore: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onOpenSaleSheet: () -> Unit,
    onLaunchTutorial: () -> Unit
) {
    val langState = LocalLanguage.current
    val lang = langState.value
    val scrollState = rememberScrollState()
    val highlightState = LocalTutorialHighlightState.current

    val specificSales by viewModel.specificSales.collectAsState()
    val debts by viewModel.debts.collectAsState()
    // Observable current date — recomposes this screen on midnight rollover / resume
    // so the header date and today-based stats stay correct in real time.
    val currentDate by viewModel.currentDate.collectAsState()

    val today = viewModel.today
    val todaySales = specificSales.filter { it.date == today }
    // Cash Sales Today = actual recorded cash sales (web parity: the web's
    // getTodayEarnings() sums only cash sales, never a seeded/manual DailyEntry.
    // A stale DailyEntry.earnings (e.g. the old 1,250 sample seed) must not
    // override real sales or show money before any sale is recorded).
    val todayEarnings = viewModel.todayRecordedSales
    val todayUtangTotal = viewModel.specificSales.value.filter { it.date == today && it.customerName != null }.sumOf { it.amount }

    val dateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
    // Display the (possibly dev-overridden) app date so the header matches simulated day
    val todayFormatted = remember(currentDate, viewModel.today) {
        val parsed = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(viewModel.today)
        } catch (e: Exception) { null }
        dateFormat.format(parsed ?: Date())
    }

    // Collapsible transactions state
    var transactionsExpanded by remember { mutableStateOf(true) }

    // Provide scroll state for tutorial auto-scroll
    CompositionLocalProvider(LocalScreenScrollState provides scrollState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // ── Header ──
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "dayModeTitle".t(lang),
                    style = MaterialTheme.typography.titleMedium,
                    color = Gray500,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    todayFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray400,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Stats grid ──
        // Fixed-height Row (150dp) gives every card equal height.
        // Box(contentAlignment = Center) inside each card reliably
        // centers content vertically — no more IntrinsicSize.Min issues.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .tutorialHighlight("dayStatsGrid", highlightState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DayStatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = "\uD83D\uDCB5",
                iconBg = Green100,
                label = "recordedSalesToday".t(lang),
                value = "\u20B1${String.format("%,.2f", todayEarnings)}"
            )
            DayStatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = "\uD83D\uDCE6",
                iconBg = Blue50,
                label = "itemsSold".t(lang),
                value = "${todaySales.sumOf { it.quantity }}"
            )
            DayStatCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = "\uD83D\uDCB0",
                iconBg = Amber100,
                label = "debtToday".t(lang),
                value = "\u20B1${String.format("%,.2f", todayUtangTotal)}"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Collapsible Transaction Feed ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .tutorialHighlight("dayTxFeed", highlightState),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column {
                // Header (clickable to toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { transactionsExpanded = !transactionsExpanded }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "dayTransactionsLabel".t(lang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Gray700
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        if (transactionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Gray400,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Transaction list (collapsible)
                AnimatedVisibility(
                    visible = transactionsExpanded,
                    enter = expandVertically(expandFrom = Alignment.Top),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        if (todaySales.isEmpty()) {
                            Text(
                                "noTransactions".t(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray400,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            todaySales.take(20).forEach { sale ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (sale.customerName != null) Amber100 else Green100
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text(
                                                if (sale.customerName != null) "\uD83D\uDCB0" else "\uD83D\uDCB5",
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            sale.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Gray800
                                        )
                                        Row {
                                            if (sale.customerName != null) {
                                                Text(
                                                    sale.customerName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Amber700
                                                )
                                                Text(" \u2022 ", style = MaterialTheme.typography.bodySmall, color = Gray300)
                                            }
                                            Text(
                                                formatTimeAgo(sale.timestamp, lang),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Gray400
                                            )
                                            if (sale.quantity > 1) {
                                                Text(" \u2022 x${sale.quantity}", style = MaterialTheme.typography.bodySmall, color = Gray400)
                                            }
                                        }
                                    }
                                    Text(
                                        "\u20B1${String.format("%,.2f", sale.amount)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
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
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Close Store button ──
        OutlinedButton(
            onClick = onCloseStore,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Amber700)
        ) {
            Text(
                "\uD83C\uDF19 ${"closeStore".t(lang)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
    }
}

@Composable
private fun DayStatCard(
    modifier: Modifier = Modifier,
    icon: String,
    iconBg: Color,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Box fills the entire Card height (150dp from Row),
        // then contentAlignment = Center vertically centers the Column.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = iconBg
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(icon, fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Gray500,
                    textAlign = TextAlign.Center
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gray800,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Day Mode Screen")
@Composable
fun DayModeScreenPreview() {
    SariSariSmartTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DayModeScreen(
                viewModel = remember { AppViewModel() },
                onCloseStore = {},
                onNavigateToInventory = {},
                onOpenSaleSheet = {},
                onLaunchTutorial = {}
            )
        }
    }
}
