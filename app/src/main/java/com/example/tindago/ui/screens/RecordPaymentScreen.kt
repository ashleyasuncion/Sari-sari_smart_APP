package com.example.tindago.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.tindago.ui.components.LocalTutorialHighlightState
import com.example.tindago.ui.components.TutorialIconButton
import com.example.tindago.ui.components.tutorialHighlight
import com.example.tindago.ui.localization.LocalLanguage
import com.example.tindago.ui.localization.t
import com.example.tindago.ui.theme.*
import com.example.tindago.data.LocalSnackbarHost
import com.example.tindago.data.LocalSnackbarScope
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    viewModel: AppViewModel,
    debtId: Int = 0,
    onBack: () -> Unit,
    onPaymentSaved: () -> Unit = {},
    onTutorialClick: (() -> Unit)? = null
) {
    val langState = LocalLanguage.current
    val lang = langState.value
    val highlightState = LocalTutorialHighlightState.current
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("en", "PH")) }
    val debts by viewModel.debts.collectAsState()

    val debt = debts.find { it.id == debtId }
    val snackbarHost = LocalSnackbarHost.current
    val snackbarScope = LocalSnackbarScope.current

    var paymentAmount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val amount = paymentAmount.toDoubleOrNull() ?: 0.0
    val remainingAfter = if (debt != null) (debt.remainingBalance - amount).coerceAtLeast(0.0) else 0.0
    val isOverpayment = amount > (debt?.remainingBalance ?: 0.0)
    val canSave = amount > 0 && !isOverpayment

    Scaffold(
        topBar = {
            // V2.68: subpage rule — Back button present → centered title.
            CenterAlignedTopAppBar(
                title = { Text("recordPayment".t(lang)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onTutorialClick != null) TutorialIconButton(onClick = onTutorialClick)
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
                Text("Customer not found", style = MaterialTheme.typography.bodyMedium, color = Gray400)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            // ── Current Balance ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Amber50),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = MaterialTheme.shapes.small,
                        color = Amber100
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("👤", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(debt.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "currentBalance".t(lang) + ": ${fmt.format(debt.remainingBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Amber700
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Payment Amount ──────────────────────────────────────────
            Text("paymentAmount".t(lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = paymentAmount,
                onValueChange = { paymentAmount = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().tutorialHighlight("rpAmountField", highlightState),
                prefix = { Text("₱") },
                shape = MaterialTheme.shapes.medium
            )

            // ── Remaining Balance Preview ───────────────────────────────
            if (amount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().tutorialHighlight("rpRemainingPreview", highlightState),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOverpayment) Red50 else Green50
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "remainingAfter".t(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverpayment) Red600 else Green700
                        )
                        Text(
                            fmt.format(remainingAfter),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverpayment) Red600 else if (remainingAfter <= 0) Green600 else Amber600
                        )
                        if (remainingAfter <= 0 && !isOverpayment) {
                            Text("fullySettled".t(lang), style = MaterialTheme.typography.bodySmall, color = Green600)
                        }
                        if (isOverpayment) {
                            Text(
                                "paymentExceeds".t(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = Red600
                            )
                        }
                    }
                }
            }

            // ── Note ────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("note".t(lang)) },
                placeholder = { Text("paymentNotePlaceholder".t(lang)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().tutorialHighlight("rpNoteField", highlightState),
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Save Button ─────────────────────────────────────────────
            Button(
                onClick = {
                    if (canSave) {
                        viewModel.recordDebtPayment(debt.id, amount, note.ifBlank { null })
                        snackbarScope.launch {
                            snackbarHost.showSnackbar("paymentSaved".t(lang))
                        }
                        onPaymentSaved()
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .tutorialHighlight("rpPayBtn", highlightState),
                enabled = canSave,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("savePayment".t(lang), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Preview(showBackground = true, name = "Record Payment")
@Composable
fun RecordPaymentScreenPreview() {
    com.example.tindago.ui.theme.TindaGoTheme {
        RecordPaymentScreen(
            viewModel = remember { AppViewModel() },
            debtId = 1,
            onBack = {}
        )
    }
}
