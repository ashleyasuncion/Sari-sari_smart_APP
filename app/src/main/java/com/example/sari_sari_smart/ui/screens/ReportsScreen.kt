package com.example.sari_sari_smart.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.sari_sari_smart.data.StockStatus
import com.example.sari_sari_smart.data.formatTimeAgo
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.t
import com.example.sari_sari_smart.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * REPORTS SCREEN — Shows period-based sales summary with bar chart.
 * Replicates the web prototype's Reports page with Day/Week/Month toggles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val langState = LocalLanguage.current
    val lang = langState.value
    val scrollState = rememberScrollState()

    val specificSales by viewModel.specificSales.collectAsState()
    val products by viewModel.products.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val reportPeriod by viewModel.reportPeriod.collectAsState()

    var selectedPeriod by remember { mutableStateOf(reportPeriod) }

    // Filter sales by period — anchored to the (possibly overridden) app date
    val todayStr = viewModel.today
    val now = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(todayStr) ?: Date()
    } catch (e: Exception) { Date() }
    val weekAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .format(Date(now.time - 7L * 24 * 60 * 60 * 1000))
    val monthAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .format(Date(now.time - 30L * 24 * 60 * 60 * 1000))

    val filteredSales = when (selectedPeriod) {
        "day" -> specificSales.filter { it.date == todayStr }
        "week" -> specificSales.filter { it.date >= weekAgo }
        "month" -> specificSales.filter { it.date >= monthAgo }
        else -> specificSales.filter { it.date == todayStr }
    }

    val totalSales = filteredSales.sumOf { it.amount }
    val totalProfit = filteredSales.sumOf { it.profit }
    val totalItems = filteredSales.sumOf { it.quantity }

    // Best-selling products
    val productSales = filteredSales.groupBy { it.description }
        .mapValues { (_, sales) -> sales.sumOf { it.quantity } }
        .entries.sortedByDescending { it.value }.take(5)

    // Low stock items
    val lowItems = products.filter { it.status != StockStatus.PLENTY }

    // Weekly chart data (last 7 days)
    val last7Days = (6 downTo 0).map { daysAgo ->
        val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d.time)
        val dayLabel = SimpleDateFormat("E", Locale.getDefault()).format(d.time)
        val dayTotal = specificSales.filter { it.date == dateStr }.sumOf { it.amount }
        dayLabel to dayTotal
    }

    val maxChartValue = last7Days.maxOfOrNull { it.second } ?: 1.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ulat / Reports", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Back", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green600,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Period toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("day" to "day".t(lang), "week" to "week".t(lang), "month" to "month".t(lang)).forEach { (value, label) ->
                    FilterChip(
                        selected = selectedPeriod == value,
                        onClick = {
                            selectedPeriod = value
                            viewModel.setReportPeriod(value)
                        },
                        label = { Text(label, fontWeight = if (selectedPeriod == value) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    title = "totalSales".t(lang),
                    value = "₱${String.format("%,.2f", totalSales)}",
                    color = Green600,
                    bgColor = Green50,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "profit".t(lang),
                    value = "₱${String.format("%,.2f", totalProfit)}",
                    color = Blue600,
                    bgColor = Blue50,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sales bar chart (last 7 days)
            if (selectedPeriod == "week" || selectedPeriod == "day") {
                Text(
                    "Weekly Sales Trend",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gray800,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Bar chart using Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            val barWidth = size.width / (last7Days.size * 2 + 1)
                            val chartHeight = size.height - 30f

                            last7Days.forEachIndexed { index, (_, value) ->
                                val barHeight = ((value / maxChartValue) * chartHeight).toFloat().coerceAtLeast(4f)
                                val x = barWidth * (index * 2 + 1)
                                val y = size.height - 20f - barHeight

                                drawRect(
                                    color = if (value > 0) Green600 else Gray200,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight),
                                    alpha = 0.8f
                                )
                            }

                            // Baseline
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(0f, size.height - 20f),
                                end = Offset(size.width, size.height - 20f),
                                strokeWidth = 1f
                            )
                        }

                        // Day labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            last7Days.forEach { (label, _) ->
                                Text(
                                    label.take(3),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray500,
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Best-selling products
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "bestSelling".t(lang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Gray800,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (productSales.isEmpty()) {
                        Text(
                            "noData".t(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray500
                        )
                    } else {
                        productSales.forEachIndexed { index, (name, qty) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row {
                                    Text(
                                        "#${index + 1}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Amber700
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(name, style = MaterialTheme.typography.bodyMedium, color = Gray800)
                                }
                                Text(
                                    "x$qty",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Green600
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recent transactions (collapsible)
            var showTransactions by remember { mutableStateOf(true) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "recentTransactions".t(lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Gray800
                        )
                        TextButton(onClick = { showTransactions = !showTransactions }) {
                            Text(if (showTransactions) "▲" else "▼", color = Gray500)
                        }
                    }
                    if (showTransactions) {
                        if (filteredSales.isEmpty()) {
                            Text(
                                "noTransactions".t(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray500
                            )
                        } else {
                            filteredSales.take(15).forEach { sale ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sale.description, style = MaterialTheme.typography.bodySmall, color = Gray800)
                                        Text(
                                            formatTimeAgo(sale.timestamp, lang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Gray400
                                        )
                                    }
                                    Text(
                                        "₱${String.format("%,.2f", sale.amount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Gray800
                                    )
                                }
                                HorizontalDivider(color = Gray100)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Low stock items
            var showLowStock by remember { mutableStateOf(true) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "lowStockItems".t(lang),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Gray800
                        )
                        TextButton(onClick = { showLowStock = !showLowStock }) {
                            Text(if (showLowStock) "▲" else "▼", color = Gray500)
                        }
                    }
                    if (showLowStock) {
                        if (lowItems.isEmpty()) {
                            Text(
                                "noLowStockItems".t(lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Gray500
                            )
                        } else {
                            lowItems.take(8).forEach { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(p.name, style = MaterialTheme.typography.bodySmall, color = Gray800)
                                    Text(
                                        if (p.quantity <= 0) "Out of stock" else "${p.quantity} left",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (p.quantity <= 0) Red500 else Amber700,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, name = "Reports Screen")
@Composable
fun ReportsScreenPreview() {
    com.example.sari_sari_smart.ui.theme.SariSariSmartTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            ReportsScreen(
                viewModel = remember { AppViewModel() },
                onBack = {}
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
