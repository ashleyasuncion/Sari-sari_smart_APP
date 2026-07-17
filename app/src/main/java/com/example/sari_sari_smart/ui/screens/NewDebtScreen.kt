package com.example.sari_sari_smart.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.tooling.preview.Preview
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
import com.example.sari_sari_smart.data.CustomerDebt
import com.example.sari_sari_smart.data.LocalSnackbarHost
import com.example.sari_sari_smart.data.LocalSnackbarScope
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.t
import com.example.sari_sari_smart.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDebtScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit = {}
) {
    val langState = LocalLanguage.current
    val lang = langState.value

    val snackbarHost = LocalSnackbarHost.current
    val snackbarScope = LocalSnackbarScope.current

    var customerName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }

    val usedNames = remember { viewModel.getUsedCustomerNames() }
    val suggestions = remember(customerName, usedNames) {
        if (customerName.length >= 1) {
            usedNames.filter { it.contains(customerName, ignoreCase = true) }.take(5)
        } else emptyList()
    }

    val debtAmount = amount.toDoubleOrNull() ?: 0.0
    val canSave = customerName.isNotBlank() && debtAmount > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("newDebtManual".t(lang)) },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            // ── Customer Name ───────────────────────────────────────────
            Text("customer".t(lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = customerName,
                onValueChange = {
                    customerName = it
                    showSuggestions = it.isNotEmpty()
                },
                placeholder = { Text("enterCustomerNameDebt".t(lang)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                trailingIcon = if (customerName.isNotEmpty()) {
                    { IconButton(onClick = { customerName = ""; showSuggestions = false }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    } }
                } else null,
                shape = MaterialTheme.shapes.medium
            )

            // ── Autocomplete suggestions ────────────────────────────────
            if (showSuggestions && suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        suggestions.forEach { name ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    customerName = name; showSuggestions = false
                                }.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Amount ──────────────────────────────────────────────────
            Text("debtAmount".t(lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₱") },
                shape = MaterialTheme.shapes.medium
            )

            // ── Info note ───────────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Amber50),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Text("📌", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "debtNote".t(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = Amber800
                    )
                }
            }

            // ── Save button ─────────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (canSave) {
                        val name = customerName.trim()

                        // Check if customer already exists
                        val existingDebt = viewModel.debts.value.find {
                            it.customerName.equals(name, ignoreCase = true)
                        }
                        if (existingDebt != null) {
                            viewModel.addToDebtBalance(existingDebt.id, debtAmount)
                        } else {
                            viewModel.addDebt(
                                CustomerDebt(
                                    id = 0,
                                    customerName = name,
                                    amount = debtAmount,
                                    remainingBalance = debtAmount,
                                    lastActivity = "Today"
                                )
                            )
                        }
                        snackbarScope.launch {
                            snackbarHost.showSnackbar("debtSaved".t(lang))
                        }
                        onSaved()
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = canSave,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("saveDebt".t(lang), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Preview(showBackground = true, name = "New Debt Screen")
@Composable
fun NewDebtScreenPreview() {
    com.example.sari_sari_smart.ui.theme.SariSariSmartTheme {
        NewDebtScreen(
            viewModel = remember { AppViewModel() },
            onBack = {}
        )
    }
}
