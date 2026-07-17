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
import androidx.compose.ui.tooling.preview.Preview
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.t
import com.example.sari_sari_smart.ui.theme.*
import java.text.NumberFormat
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
    val debts by viewModel.debts.collectAsState()

    val debt = debts.find { it.id == debtId }
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
                        Text(
                            "lastActivity".t(lang) + " ${debt.lastActivity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Gray500
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Debt History ────────────────────────────────────────────
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

                    // Initial debt entry
                    DebtHistoryRow(
                        label = "initialDebt".t(lang),
                        amount = debt.amount,
                        isPositive = true,
                        fmt = fmt
                    )

                    // Payments (simulated from ViewModel data)
                    val paymentsMade = debt.amount - debt.remainingBalance
                    if (paymentsMade > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Gray100)
                        DebtHistoryRow(
                            label = "payment".t(lang),
                            amount = paymentsMade,
                            isPositive = false,
                            fmt = fmt
                        )
                    }

                    if (debt.remainingBalance > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Gray100)
                        DebtHistoryRow(
                            label = "currentBalance".t(lang),
                            amount = debt.remainingBalance,
                            isPositive = true,
                            fmt = fmt,
                            isBold = true
                        )
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
private fun DebtHistoryRow(
    label: String,
    amount: Double,
    isPositive: Boolean,
    fmt: java.text.NumberFormat,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isPositive) "➕" else "➖",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = if (isBold) Gray800 else Gray600
            )
        }
        Text(
            (if (isPositive) "" else "-") + fmt.format(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isPositive) if (isBold) Amber600 else Green600 else Red600
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
