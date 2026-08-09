package com.example.sari_sari_smart.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
import com.example.sari_sari_smart.ui.screens.CreditStatus
import com.example.sari_sari_smart.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.sari_sari_smart.ui.theme.SariSariSmartTheme

/**
 * Bottom sheet for quick sale entry — mirrors the "May Bumili" sheet from day.html.
 * Used in Day Mode when the Sell FAB is tapped.
 *
 * Web v2.56-2.58 parity:
 *  - Out-of-stock products are shown greyed-out but NOT selectable (tap = "no stock" toast).
 *  - Customer suggestions show a balance/limit badge, colour-coded (green/amber/red).
 *  - Live credit-limit warning banner + at/over-limit save gate with "Allow anyway" override.
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
    // Editable quantity state — tapping the quantity TextField switches to direct typing
    var isEditingQty by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("1") }

    val focusManager = LocalFocusManager.current

    // Qty selector is disabled until a product is selected (matches webapp behavior)
    val isQtySelectorDisabled = selectedProductId < 0

    // On save, reset form and stay open (matches webapp saveSale() behavior)
    // Only explicitly close via Cancel or dismiss

    val selectedProduct = products.find { it.id == selectedProductId }
    val totalAmount = selectedProduct?.let { it.sellingPrice * quantity } ?: 0.0

    // In-stock items sort ahead of out-of-stock ones so greyed rows never crowd
    // sellable items out of the visible top-8 (web v2.58 review fix).
    val filteredProducts = products.filter {
        it.name.contains(productQuery, ignoreCase = true)
    }.sortedBy { if (it.quantity <= 0) 1 else 0 }.take(8)

    val usedCustomerNames = remember(debts) {
        debts.map { it.customerName }.distinct()
    }
    val filteredCustomers = usedCustomerNames.filter {
        it.contains(customerName, ignoreCase = true)
    }.take(5)

    // Build a debt balance lookup: customer name -> total remaining balance
    val customerBalances = remember(debts) {
        debts.groupBy { it.customerName }
            .mapValues { (_, entries) -> entries.sumOf { it.remainingBalance } }
    }

    // Live credit-limit status for the typed customer + current sale amount (web v2.56).
    val creditStatus: CreditStatus? =
        if (customerName.isNotBlank()) viewModel.getCreditStatus(customerName, totalAmount) else null

    /** Colour + text for a customer suggestion badge — web suggestionBalanceHtml parity. */
    fun badgeFor(name: String, balance: Double): Pair<String, Color> {
        val limit = viewModel.getEffectiveCreditLimit(name)
        val peso = { v: Double -> "₱" + String.format("%,.2f", v) }
        return if (limit > 0) {
            val txt = "${peso(balance)} / ${peso(limit.toDouble())}"
            val color = when {
                balance == 0.0 -> Green600                    // settled
                balance >= limit -> Red500                     // over
                balance >= limit * 0.8 -> Amber700             // near
                else -> Gray800
            }
            txt to color
        } else {
            if (balance > 0) "${peso(balance)}" to Red500
            else "✓ ₱0.00" to Green600
        }
    }

    /** Localized credit warning message — web creditWarnMessage parity. */
    fun warnText(cs: CreditStatus): String {
        val peso = { v: Double -> "₱" + String.format("%,.2f", v) }
        val key = if (cs.overLimit) {
            if (cs.total > cs.limit) "creditWarnOver" else "creditWarnAtLimit"
        } else "creditWarnNear"
        return when (key) {
            "creditWarnAtLimit" -> "creditWarnAtLimit".t(lang)
                .replace("{name}", customerName)
                .replace("{limit}", peso(cs.limit.toDouble()))
            "creditWarnOver" -> "creditWarnOver".t(lang)
                .replace("{name}", customerName)
                .replace("{total}", peso(cs.total))
                .replace("{limit}", peso(cs.limit.toDouble()))
            else -> "creditWarnNear".t(lang).replace("{limit}", peso(cs.limit.toDouble()))
        }
    }

    fun finishEditing() {
        val parsed = qtyText.toIntOrNull()
        val maxQty = selectedProduct?.quantity ?: Int.MAX_VALUE
        quantity = when {
            parsed == null || parsed < 1 -> 1
            parsed > maxQty -> maxQty
            else -> parsed
        }
        qtyText = quantity.toString()
        isEditingQty = false
        focusManager.clearFocus()
    }

    fun resetForm() {
        productQuery = ""
        selectedProductId = -1
        quantity = 1
        customerName = ""
        showSuggestions = false
        isEditingQty = false
        qtyText = "1"
    }

    /** Web saveSale(force) parity: blocks at/over the credit limit unless forced. */
    fun saveSale(force: Boolean) {
        val product = selectedProduct
        if (product == null || totalAmount <= 0 || quantity <= 0 || quantity > product.quantity) {
            return
        }
        if (customerName.isNotBlank() && !force) {
            val cs = viewModel.getCreditStatus(customerName, totalAmount)
            if (cs.overLimit) {
                // Alert is already visible: the live red banner + "Allow anyway"
                // button (web v2.57 parity — snackbars are hidden behind the
                // modal sheet window, so the inline banner is the alert here).
                return
            }
        }
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
                // Ledger entry (web saveSale parity: description = product name)
                viewModel.addDebtTransaction(existingDebt.id, "debt", product.name, totalAmount)
            } else {
                val newDebt = viewModel.addDebt(
                    CustomerDebt(
                        id = 0,
                        customerName = customerName,
                        amount = totalAmount,
                        remainingBalance = totalAmount
                    )
                )
                viewModel.addDebtTransaction(newDebt.id, "debt", product.name, totalAmount)
            }
        }
        // Stay open for next sale — reset form (matches webapp saveSale() behavior)
        resetForm()
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

            // Product suggestions dropdown — out-of-stock rows are greyed and unselectable (v2.58)
            if (showSuggestions && productQuery.isNotEmpty() && selectedProductId < 0 && filteredProducts.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        filteredProducts.forEach { p ->
                            val outOfStock = p.quantity <= 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (outOfStock) 0.5f else 1f)
                                    .clickable {
                                        if (outOfStock) {
                                            // Greyed row + "no stock" label are the feedback;
                                            // no snackbar (hidden behind the modal sheet window).
                                        } else {
                                            productQuery = p.name
                                            selectedProductId = p.id
                                            showSuggestions = false
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.name, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        when {
                                            p.quantity <= 0 -> "\u26D4 ${"noStock".t(lang)}"
                                            p.quantity <= 5 -> "\u26A0\uFE0F ${p.quantity} left"
                                            else -> "\u2705 ${p.quantity} left"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            p.quantity <= 0 -> Red500
                                            p.quantity <= 5 -> Amber700
                                            else -> Green600
                                        }
                                    )
                                }
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
                // Quantity display — clickable to switch to direct typing
                if (isEditingQty && !isQtySelectorDisabled) {
                    var hasBeenFocused by remember { mutableStateOf(false) }
                    val focusRequester = remember { FocusRequester() }

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }

                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                        modifier = Modifier
                            .width(80.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) hasBeenFocused = true
                                if (!it.isFocused && hasBeenFocused) finishEditing()
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { finishEditing() }
                        )
                    )
                } else {
                    Text(
                        if (isQtySelectorDisabled) "--" else "$quantity",
                        modifier = Modifier
                            .width(60.dp)
                            .padding(horizontal = 16.dp)
                            .clickable(enabled = !isQtySelectorDisabled) {
                                isEditingQty = true
                                qtyText = quantity.toString()
                            },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (isQtySelectorDisabled) Gray300 else Color.Unspecified
                    )
                }
                FilledTonalIconButton(
                    onClick = {
                        if (selectedProduct != null && quantity < selectedProduct.quantity) quantity++
                    },
                    enabled = !isQtySelectorDisabled && selectedProduct != null && quantity < selectedProduct.quantity,
                    modifier = Modifier.alpha(alpha)
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            // Stock hint — quantity is clamped so it never exceeds available stock
            val selProduct = selectedProduct
            if (selProduct != null) {
                val clampedQty = if (isEditingQty) {
                    qtyText.toIntOrNull()?.coerceIn(1, selProduct.quantity)
                        ?: selProduct.quantity
                } else quantity
                if (selProduct.quantity <= 0) {
                    Text("\u26D4 ${"noStock".t(lang)}", style = MaterialTheme.typography.bodySmall, color = Red500)
                } else if (clampedQty > selProduct.quantity) {
                    Text("\u26A0\uFE0F Only ${selProduct.quantity} available — will be clamped on save", style = MaterialTheme.typography.bodySmall, color = Amber700)
                } else if (quantity == selProduct.quantity) {
                    Text("\u2705 Available: ${selProduct.quantity} (max)", style = MaterialTheme.typography.bodySmall, color = Green600)
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

            // Customer suggestions — balance/limit badge (web v2.56 parity)
            if (customerName.isNotEmpty() && filteredCustomers.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        filteredCustomers.forEach { name ->
                            val balance = customerBalances[name] ?: 0.0
                            val (badgeText, badgeColor) = badgeFor(name, balance)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { customerName = name }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    badgeText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeColor
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            // ── Live credit-limit warning (web v2.56/v2.57 parity) ──
            val cs = creditStatus
            if (cs != null && (cs.overLimit || cs.nearLimit)) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (cs.overLimit) Red50 else Amber50
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            warnText(cs),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (cs.overLimit) Red700 else Amber800
                        )
                        if (cs.overLimit) {
                            Spacer(modifier = Modifier.height(8.dp))
                            // "Allow anyway" — one-tap deliberate override (web saveSale(true) parity)
                            OutlinedButton(
                                onClick = { saveSale(force = true) },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Red700)
                            ) {
                                Text("creditAllowAnyway".t(lang), fontWeight = FontWeight.Bold)
                            }
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
                    onClick = { saveSale(force = false) },
                    enabled = selectedProduct != null && totalAmount > 0 && quantity > 0 && quantity <= (selectedProduct?.quantity ?: 0),
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
