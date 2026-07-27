package com.example.sari_sari_smart.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sari_sari_smart.data.CustomerDebt
import com.example.sari_sari_smart.data.SpecificSale
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.t
import com.example.sari_sari_smart.ui.screens.AppViewModel
import com.example.sari_sari_smart.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.sari_sari_smart.ui.theme.SariSariSmartTheme

/**
 * Bottom sheet for quick sale entry — mirrors the "May Bumili" sheet from day.html.
 * Used in Day Mode when the Sell FAB is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleBottomSheet(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val langState = LocalLanguage.current
    val lang = langState.value
    val products by viewModel.products.collectAsState()
    val debts by viewModel.debts.collectAsState()

    // Form state
    var productQuery by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableIntStateOf(-1) }
    var quantity by remember { mutableIntStateOf(1) }
    var customerName by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }

    // Qty selector is disabled until a product is selected (matches webapp behavior)
    val isQtySelectorDisabled = selectedProductId < 0

    // On save, reset form and stay open (matches webapp saveSale() behavior)
    // Only explicitly close via Cancel or dismiss

    val selectedProduct = products.find { it.id == selectedProductId }
    val totalAmount = selectedProduct?.let { it.sellingPrice * quantity } ?: 0.0

    val filteredProducts = products.filter {
        it.name.contains(productQuery, ignoreCase = true)
    }.take(8)

    val usedCustomerNames = remember(debts) {
        debts.map { it.customerName }.distinct()
    }
    val filteredCustomers = usedCustomerNames.filter {
        it.contains(customerName, ignoreCase = true)
    }.take(5)

    fun resetForm() {
        productQuery = ""
        selectedProductId = -1
        quantity = 1
        customerName = ""
        showSuggestions = false
    }

    ModalBottomSheet(
        onDismissRequest = { resetForm(); onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 12.dp)
            )

            Text(
                "\u2716 ${"saleSheetTitle".t(lang)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // ── Product search ──
            Text("addSpecificSale".t(lang), style = MaterialTheme.typography.labelMedium, color = Gray500)
            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = productQuery,
                onValueChange = {
                    productQuery = it
                    showSuggestions = true
                    selectedProductId = -1
                },
                placeholder = { Text("searchItems".t(lang)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Product suggestions dropdown
            if (showSuggestions && productQuery.isNotEmpty() && selectedProductId < 0 && filteredProducts.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        filteredProducts.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        productQuery = p.name
                                        selectedProductId = p.id
                                        showSuggestions = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text(
                                    "\u20B1${String.format("%,.2f", p.sellingPrice)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Green600
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            if (selectedProduct != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Green50)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(selectedProduct.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "\u20B1${String.format("%,.2f", selectedProduct.sellingPrice)} ${"eachLabel".t(lang)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Gray500
                            )
                        }
                        Text(
                            "${"stockLabel".t(lang)} ${selectedProduct.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedProduct.quantity <= 5) Red500 else Green600
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Quantity selector (disabled until product selected — matches webapp .qty-selector.disabled) ──
            Text("quantity".t(lang), style = MaterialTheme.typography.labelMedium, color = Gray500)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val alpha = if (isQtySelectorDisabled) 0.45f else 1f
                FilledTonalIconButton(
                    onClick = { if (quantity > 1) quantity-- },
                    enabled = !isQtySelectorDisabled && quantity > 1,
                    modifier = Modifier.alpha(alpha)
                ) {
                    Text("\u2212", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    if (isQtySelectorDisabled) "--" else "$quantity",
                    modifier = Modifier
                        .width(60.dp)
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = if (isQtySelectorDisabled) Gray300 else Color.Unspecified
                )
                FilledTonalIconButton(
                    onClick = {
                        if (selectedProduct == null || quantity < selectedProduct.quantity) quantity++
                    },
                    enabled = !isQtySelectorDisabled && (selectedProduct == null || quantity < (selectedProduct?.quantity ?: Int.MAX_VALUE)),
                    modifier = Modifier.alpha(alpha)
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Stock hint (matches webapp #saleStockHint)
            val selProduct = selectedProduct
            if (selProduct != null) {
                if (selProduct.quantity <= 0) {
                    Text("\uD83D\udd34 ${"noStock".t(lang)}", style = MaterialTheme.typography.bodySmall, color = Red500)
                } else if (quantity > selProduct.quantity) {
                    Text("\u26A0\uFE0F Only ${selProduct.quantity} available", style = MaterialTheme.typography.bodySmall, color = Amber700)
                } else {
                    Text("\u2705 Available: ${selProduct.quantity}", style = MaterialTheme.typography.bodySmall, color = Green600)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Customer name (for utang) ──
            Text("saleCustomerLabel".t(lang), style = MaterialTheme.typography.labelMedium, color = Gray500)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                placeholder = { Text("customerPlaceholder".t(lang)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Customer suggestions
            if (customerName.isNotEmpty() && filteredCustomers.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        filteredCustomers.forEach { name ->
                            Text(
                                name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { customerName = name }
                                    .padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Total display ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Green50)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("total".t(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "\u20B1${String.format("%,.2f", totalAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Green600
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Actions ──
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { resetForm(); onDismiss() },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("close".t(lang))
                }
                Button(
                    onClick = {
                        val product = selectedProduct
                        if (product != null && totalAmount > 0 && quantity > 0) {
                            val sale = SpecificSale(
                                id = 0, // auto-assigned
                                date = viewModel.today,
                                description = product.name,
                                amount = totalAmount,
                                quantity = quantity,
                                customerName = if (customerName.isBlank()) null else customerName,
                                profit = (product.sellingPrice - product.costPrice) * quantity
                            )
                            viewModel.addSpecificSale(sale)
                            viewModel.deductStock(product.id, quantity)

                            // Auto-create debt if customer named
                            if (customerName.isNotBlank()) {
                                val existingDebt = viewModel.debts.value.find {
                                    it.customerName.equals(customerName, ignoreCase = true)
                                }
                                if (existingDebt != null) {
                                    viewModel.addToDebtBalance(existingDebt.id, totalAmount)
                                } else {
                                    viewModel.addDebt(
                                        CustomerDebt(
                                            id = 0,
                                            customerName = customerName,
                                            amount = totalAmount,
                                            remainingBalance = totalAmount
                                        )
                                    )
                                }
                            }
                        }
                        // Stay open for next sale — reset form (matches webapp saveSale() behavior)
                        resetForm()
                    },
                    enabled = selectedProduct != null && totalAmount > 0 && quantity > 0,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("\u2714\uFE0F ${"save".t(lang)}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Sale Bottom Sheet")
@Composable
fun SaleBottomSheetPreview() {
    SariSariSmartTheme {
        SaleBottomSheet(
            viewModel = remember { AppViewModel() },
            onDismiss = {},
            onSaved = {}
        )
    }
}
