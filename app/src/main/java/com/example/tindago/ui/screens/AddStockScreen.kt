package com.example.tindago.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.tindago.ui.components.LocalTutorialHighlightState
import com.example.tindago.ui.components.TutorialIconButton
import com.example.tindago.ui.components.tutorialHighlight
import com.example.tindago.ui.localization.LocalLanguage
import com.example.tindago.ui.localization.t
import androidx.compose.ui.tooling.preview.Preview
import com.example.tindago.ui.theme.*
import com.example.tindago.ui.theme.TindaGoTheme
import com.example.tindago.data.LocalSnackbarHost
import com.example.tindago.data.LocalSnackbarScope
import java.text.NumberFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockScreen(
    viewModel: AppViewModel,
    productId: Int? = null,
    defaultMarkup: Int = 20,
    defaultLowStockThreshold: Int = 5,
    onBack: () -> Unit,
    onSaved: () -> Unit = {},
    onTutorialClick: (() -> Unit)? = null
) {
    val langState = LocalLanguage.current
    val lang = langState.value
    val highlightState = LocalTutorialHighlightState.current
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("en", "PH")) }

    val products by viewModel.products.collectAsState()

    val existingProduct = productId?.let { viewModel.getProductById(it) }
    val isEditing = existingProduct != null
    val snackbarHost = LocalSnackbarHost.current
    val snackbarScope = LocalSnackbarScope.current

    var itemName by remember { mutableStateOf(existingProduct?.name ?: "") }
    var quantity by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf(existingProduct?.costPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var sellPrice by remember { mutableStateOf(existingProduct?.sellingPrice?.let { if (it > 0) it.toString() else "" } ?: "") }

    // ── Product identity (web v2.59 parity — units/brands/categories) ──
    var category by remember { mutableStateOf(existingProduct?.category ?: "") }
    var brand by remember { mutableStateOf(existingProduct?.brand ?: "") }
    var unit by remember { mutableStateOf(existingProduct?.unit ?: "piece") }
    var packageSize by remember { mutableStateOf(existingProduct?.packageSize ?: "") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var brandSuggestionsVisible by remember { mutableStateOf(false) }
    var sizeSuggestionsVisible by remember { mutableStateOf(false) }

    // Datalist-style suggestions from previously used values (web getUsedBrands
    // / getUsedPackageSizes parity). Suggestions are SELECTABLE — the field is
    // NOT auto-filled from history (v2.59 cleanup: no remembered-value leak).
    val usedBrands = remember(products) { viewModel.getUsedBrands() }
    val usedPackageSizes = remember(products) { viewModel.getUsedPackageSizes() }
    // Markup pre-fill: the product's ACTUAL markup when editing (so the helper
    // reflects reality), otherwise the configured default from Settings.
    var markupPercent by remember {
        mutableStateOf(
            if (isEditing && (existingProduct?.costPrice ?: 0.0) > 0) {
                val actual = Math.round(((existingProduct!!.sellingPrice / existingProduct!!.costPrice) - 1) * 100)
                if (actual >= 0) actual.toString() else defaultMarkup.toString()
            } else defaultMarkup.toString()
        )
    }
    // Web parity: once the user types a selling price manually, stop
    // auto-filling it from the markup (mirrors web _userEditedPrice).
    var userEditedPrice by remember { mutableStateOf(false) }
    // Low-stock alert threshold: the product's own value when editing, else the
    // global Settings threshold as the default for new products.
    var lowStockThreshold by remember {
        mutableStateOf((existingProduct?.lowStockThreshold ?: defaultLowStockThreshold).toString())
    }

    val cost = costPrice.toDoubleOrNull() ?: 0.0
    val markup = (markupPercent.toDoubleOrNull() ?: 20.0) / 100.0
    val suggestedPrice = if (cost > 0 && markup > 0) cost * (1 + markup) else 0.0

    // Web parity auto-fill: compute the suggested selling price from cost +
    // markup unless the user already typed a price manually. Shared by the cost
    // and markup field handlers (mirrors the web productCost/productMarkup
    // input listeners) so the two call sites can't drift apart.
    fun autoFillSellPrice() {
        val c = costPrice.toDoubleOrNull() ?: 0.0
        val m = (markupPercent.toDoubleOrNull() ?: 20.0) / 100.0
        if (c > 0 && m > 0 && !userEditedPrice) {
            sellPrice = String.format("%.2f", c * (1 + m))
        }
    }
    val profitMargin = if (cost > 0 && sellPrice.toDoubleOrNull() ?: 0.0 > 0) {
        val sp = sellPrice.toDoubleOrNull() ?: 0.0
        ((sp - cost) / cost * 100)
    } else 0.0
    val profitAmount = if (cost > 0 && sellPrice.toDoubleOrNull() ?: 0.0 > 0) {
        (sellPrice.toDoubleOrNull() ?: 0.0) - cost
    } else 0.0

    Scaffold(
        topBar = {
            // V2.68: subpage rule — Back button present → centered title.
            CenterAlignedTopAppBar(
                title = { Text(if (isEditing) "edit".t(lang) else "addStockTitle".t(lang)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Current stock info (if editing)
            if (isEditing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Green50),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📦", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("currentStock".t(lang), style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text(
                                "${existingProduct!!.quantity} ${existingProduct.unit}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Item name
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("itemName".t(lang)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().tutorialHighlight("addStockNameField", highlightState),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // ── Product Details (web v2.59 parity — units/brands/categories) ──
            Text(
                "productDetailsSection".t(lang),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Green700
            )
            Text(
                "productDetailsHint".t(lang),
                style = MaterialTheme.typography.bodySmall,
                color = Gray400
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = if (category.isBlank()) "" else com.example.tindago.ui.localization.Strings.productCategoryLabel(category, lang),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("categoryLabel".t(lang)) },
                    placeholder = { Text("catAll".t(lang), color = Gray400) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    // "All" = uncategorized ('' key), matching the web dropdown's
                    // first option so uncategorized products remain reachable.
                    DropdownMenuItem(
                        text = { Text("catAll".t(lang)) },
                        onClick = { category = ""; categoryDropdownExpanded = false }
                    )
                    com.example.tindago.data.ProductCatalog.CATEGORIES.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(com.example.tindago.ui.localization.Strings.productCategoryLabel(key, lang)) },
                            onClick = { category = key; categoryDropdownExpanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Brand input + suggestions (datalist-style, selectable only)
            OutlinedTextField(
                value = brand,
                onValueChange = {
                    brand = it
                    brandSuggestionsVisible = it.isNotEmpty()
                },
                label = { Text("brandLabel".t(lang)) },
                placeholder = { Text("brandPlaceholder".t(lang)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().tutorialHighlight("addStockBrandField", highlightState),
                shape = MaterialTheme.shapes.medium
            )
            if (brandSuggestionsVisible) {
                val matches = usedBrands.filter { it.contains(brand, ignoreCase = true) }.take(5)
                if (matches.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.small,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            matches.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { brand = suggestion; brandSuggestionsVisible = false }
                                        .padding(12.dp)
                                ) {
                                    Text(suggestion, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Unit dropdown
            ExposedDropdownMenuBox(
                expanded = unitDropdownExpanded,
                onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = com.example.tindago.ui.localization.Strings.productUnitLabel(unit, lang),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("unitLabel".t(lang)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                ExposedDropdownMenu(
                    expanded = unitDropdownExpanded,
                    onDismissRequest = { unitDropdownExpanded = false }
                ) {
                    com.example.tindago.data.ProductCatalog.UNITS.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(com.example.tindago.ui.localization.Strings.productUnitLabel(key, lang)) },
                            onClick = { unit = key; unitDropdownExpanded = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Package Size input + suggestions
            OutlinedTextField(
                value = packageSize,
                onValueChange = {
                    packageSize = it
                    sizeSuggestionsVisible = it.isNotEmpty()
                },
                label = { Text("packageSizeLabel".t(lang)) },
                placeholder = { Text("packageSizePlaceholder".t(lang)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().tutorialHighlight("addStockSizeField", highlightState),
                shape = MaterialTheme.shapes.medium
            )
            if (sizeSuggestionsVisible) {
                val matches = usedPackageSizes.filter { it.contains(packageSize, ignoreCase = true) }.take(5)
                if (matches.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.small,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column {
                            matches.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { packageSize = suggestion; sizeSuggestionsVisible = false }
                                        .padding(12.dp)
                                ) {
                                    Text(suggestion, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Quantity
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                label = { Text(if (isEditing) "additionalQty".t(lang) else "quantity".t(lang)) },
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().tutorialHighlight("addStockQtyField", highlightState),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Cost price
            OutlinedTextField(
                value = costPrice,
                onValueChange = { value ->
                    costPrice = value.filter { c -> c.isDigit() || c == '.' }
                    // Web parity: entering the cost alone auto-suggests the
                    // selling price (cost x (1 + markup)).
                    autoFillSellPrice()
                },
                label = { Text("costPerUnit".t(lang)) },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₱") },
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // ── Markup Helper ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().tutorialHighlight("addStockMarkup", highlightState),
                colors = CardDefaults.cardColors(containerColor = Green50),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Web parity: markup is a plain number input (not a slider)
                    OutlinedTextField(
                        value = markupPercent,
                        onValueChange = { value ->
                            markupPercent = value.filter { c -> c.isDigit() }
                            // Auto-calculate selling price from markup, but only
                            // while the user hasn't typed a price manually
                            autoFillSellPrice()
                        },
                        label = { Text("markupPercent".t(lang)) },
                        placeholder = { Text("20") },
                        suffix = { Text("%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )

                    if (suggestedPrice > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "markupHint".t(lang)
                                .replace("{cost}", fmt.format(cost))
                                .replace("{percent}", "$markupPercent")
                                .replace("{amount}", fmt.format(suggestedPrice - cost))
                                .replace("{price}", fmt.format(suggestedPrice)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Green700
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Selling price
            OutlinedTextField(
                value = sellPrice,
                onValueChange = {
                    sellPrice = it.filter { c -> c.isDigit() || c == '.' }
                    userEditedPrice = true
                },
                label = { Text("sellPrice".t(lang)) },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₱") },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Low-stock threshold
            OutlinedTextField(
                value = lowStockThreshold,
                onValueChange = { lowStockThreshold = it.filter { c -> c.isDigit() } },
                label = { Text("lowStockAlertLabel".t(lang)) },
                placeholder = { Text("5") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("≤ ") },
                suffix = { Text("units".t(lang)) },
                shape = MaterialTheme.shapes.medium
            )

            // ── Profit Preview ───────────────────────────────────────────
            if (cost > 0 && (sellPrice.toDoubleOrNull() ?: 0.0) > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (profitMargin >= 15) Green50 else if (profitMargin > 0) Amber50 else Red50
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("profitMargin".t(lang), style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text(
                                if (profitMargin > 0) "+$profitMargin%" else "--",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (profitMargin >= 15) Green600 else if (profitMargin > 0) Amber600 else Red600
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("profit".t(lang), style = MaterialTheme.typography.bodySmall, color = Gray500)
                            Text(
                                fmt.format(profitAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Green600
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    val name = itemName.trim()
                    val qty = quantity.toIntOrNull() ?: 1
                    val cost = costPrice.toDoubleOrNull() ?: 0.0
                    val price = sellPrice.toDoubleOrNull() ?: 0.0
                    val threshold = lowStockThreshold.toIntOrNull() ?: 5
                    if (name.isNotBlank() && price > 0) {
                        viewModel.addOrUpdateProduct(
                            name, qty, cost, price, threshold,
                            category = category, brand = brand,
                            unit = unit, packageSize = packageSize
                        )
                        snackbarScope.launch {
                            snackbarHost.showSnackbar(
                                if (isEditing) "stockUpdated".t(lang) else "productSaved".t(lang)
                            )
                        }
                        onSaved()
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .tutorialHighlight("addStockSaveBtn", highlightState),
                shape = MaterialTheme.shapes.medium,
                enabled = itemName.isNotBlank() && (sellPrice.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("save".t(lang), style = MaterialTheme.typography.titleSmall)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, name = "Add Stock Screen")
@Composable
fun AddStockScreenPreview() {
    TindaGoTheme {
        AddStockScreen(
            viewModel = remember { AppViewModel() },
            onBack = {}
        )
    }
}
