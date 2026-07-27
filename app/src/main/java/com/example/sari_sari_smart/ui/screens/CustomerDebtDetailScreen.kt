package com.example.sari_sari_smart.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.sari_sari_smart.data.CustomerDebt
import com.example.sari_sari_smart.data.DebtPayment
import com.example.sari_sari_smart.data.SpecificSale
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.t
import com.example.sari_sari_smart.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDebtDetailScreen(
    viewModel: AppViewModel,
    debtId: Int = 0,
    onBack: () -> Unit,
    onRecordPayment: (Int) -> Unit = {}
) {
    val langState = LocalLanguage.current
    val lang = langState.value
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("en", "PH")) }
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()) }
    val debts by viewModel.debts.collectAsState()
    val allPayments by viewModel.payments.collectAsState()

    val debt = debts.find { it.id == debtId }
    val payments = allPayments.filter { it.debtId == debtId }.sortedBy { it.timestamp }
    val isSettled = debt?.remainingBalance != null && debt.remainingBalance <= 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(debt?.customerName ?: "customerDebt".t(lang)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Green600,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (debt == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👤", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Customer not found", style = MaterialTheme.typography.bodyMedium, color = Gray400)
                }
            }
            return@Scaffold
        }

        // Build chronological transaction list
        data class Transaction(
            val date: Long,
            val type: String, // "debt", "payment", "initial"
            val description: String,
            val amount: Double,
            val isPositive: Boolean // true = added to balance, false = deducted
        )

        fun formatTimestamp(ts: Long): String = dateFmt.format(Date(ts))

        val transactions = mutableListOf<Transaction>()

        // 1. Initial debt entry
        transactions.add(Transaction(
            date = debt.createdAt,
            type = "initial",
            description = "initialDebt".t(lang),
            amount = debt.amount,
            isPositive = true
        ))

        // 2. Payments
        payments.forEach { payment ->
            transactions.add(Transaction(
                date = payment.timestamp,
                type = "payment",
                description = payment.note ?: "payment".t(lang),
                amount = payment.amount,
                isPositive = false
            ))
        }

        // Sort by date ascending
        transactions.sortBy { it.date }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            // ── Balance Card ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSettled) Green50 else Amber50
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "currentBalance".t(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSettled) Green600 else Amber800
                    )
                    Text(
                        fmt.format(debt.remainingBalance),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSettled) Green600 else Amber600
                    )
                    if (isSettled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("fullySettled".t(lang), style = MaterialTheme.typography.bodySmall, color = Green600)
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "lastActivity".t(lang) + " ${viewModel.getLastActivity(debt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Debt History with Running Balance ────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Green600, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("debtHistory".t(lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date", style = MaterialTheme.typography.labelSmall, color = Gray400, modifier = Modifier.weight(1.3f))
                        Text("Description", style = MaterialTheme.typography.labelSmall, color = Gray400, modifier = Modifier.weight(1f))
                        Text("Amount", style = MaterialTheme.typography.labelSmall, color = Gray400, modifier = Modifier.weight(0.7f))
                        Text("Balance", style = MaterialTheme.typography.labelSmall, color = Gray400, modifier = Modifier.weight(0.7f))
                    }

                    HorizontalDivider(color = Gray100)

                    // Transaction rows with running balance
                    var runningBalance = 0.0
                    transactions.forEach { tx ->
                        runningBalance += if (tx.isPositive) tx.amount else -tx.amount
                        TransactionRow(
                            date = formatTimestamp(tx.date),
                            description = tx.description,
                            amount = tx.amount,
                            isPositive = tx.isPositive,
                            runningBalance = runningBalance,
                            fmt = fmt
                        )
                        HorizontalDivider(color = Gray100)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Summary row ──────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Debt", style = MaterialTheme.typography.labelSmall, color = Gray400)
                        Text(fmt.format(debt.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Red600)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Paid", style = MaterialTheme.typography.labelSmall, color = Gray400)
                        val totalPaid = payments.sumOf { it.amount }
                        Text(fmt.format(totalPaid), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Green600)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Record Payment Button ───────────────────────────────────
            if (!isSettled) {
                Button(
                    onClick = { onRecordPayment(debt.id) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("recordPayment".t(lang), style = MaterialTheme.typography.titleSmall)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TransactionRow(
    date: String,
    description: String,
    amount: Double,
    isPositive: Boolean,
    runningBalance: Double,
    fmt: java.text.NumberFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date
        Text(
            date,
            style = MaterialTheme.typography.bodySmall,
            color = Gray500,
            modifier = Modifier.weight(1.3f)
        )
        // Description with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                if (isPositive) "\uD83D\uDFE2" else "\uD83D\uDFE0",
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Gray700,
                maxLines = 2
            )
        }
        // Amount
        Text(
            (if (isPositive) "+" else "-") + fmt.format(amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = if (isPositive) Red600 else Green600,
            modifier = Modifier.weight(0.7f)
        )
        // Running balance
        Text(
            fmt.format(runningBalance),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (runningBalance > 0) Amber700 else Green600,
            modifier = Modifier.weight(0.7f)
        )
    }
}

@Preview(showBackground = true, name = "Customer Debt Detail")
@Composable
fun CustomerDebtDetailScreenPreview() {
    com.example.sari_sari_smart.ui.theme.SariSariSmartTheme {
        CustomerDebtDetailScreen(
            viewModel = remember { AppViewModel() },
            debtId = 1,
            onBack = {}
        )
    }
}
