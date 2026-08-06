package com.example.sari_sari_smart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sari_sari_smart.data.*
import com.example.sari_sari_smart.ui.components.LocalTutorialHighlightState
import com.example.sari_sari_smart.ui.components.TutorialIconButton
import com.example.sari_sari_smart.ui.components.tutorialHighlight
import com.example.sari_sari_smart.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Restock Day Screen — 2-step guided workflow for physical count correction
 * (Step 1) and purchase recording (Step 2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onNavigateToInventory: () -> Unit = {},
    onTutorialClick: (() -> Unit)? = null
) {
    val highlightState = LocalTutorialHighlightState.current
    val products by viewModel.products.collectAsState()
    val restockTemp by viewModel.restockTemp.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var purchaseSearch by remember { mutableStateOf("") }
    var purchaseCost by remember { mutableStateOf("") }
    var purchaseQty by remember { mutableStateOf("1") }
    var selectedProductId by remember { mutableStateOf<Int?>(null) }

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products.sortedBy { it.name }
        else products.filter { it.name.contains(searchQuery, ignoreCase = true) }.sortedBy { it.name }
    }

    val filteredPurchaseProducts = remember(products, purchaseSearch) {
        if (purchaseSearch.isBlank()) emptyList()
        else products.filter { it.name.contains(purchaseSearch, ignoreCase = true) }.take(6)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Restock Day 🚚", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onTutorialClick != null) TutorialIconButton(onClick = onTutorialClick, tint = Green600)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepDot(active = restockTemp.step == 1, label = "1")
                Spacer(modifier = Modifier.width(8.dp))
                HorizontalDivider(modifier = Modifier.width(60.dp))
                Spacer(modifier = Modifier.width(8.dp))
                StepDot(active = restockTemp.step == 2, label = "2")
            }

            if (restockTemp.step == 1) {
                // ═══════════════════════════════════════════════
                // STEP 1: Physical Count Correction
                // ═══════════════════════════════════════════════
                Text(
                    text = "Step 1: Check Shelves",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp).tutorialHighlight("restockStep1Section", highlightState)
                )
                Text(
                    text = "Enter the actual count you see on your shelf for each product.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search product...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().tutorialHighlight("restockSearchField", highlightState),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Correction count badge
                val correctionCount = restockTemp.corrections.count { it.oldQty != it.newQty }
                Text(
                    text = "$correctionCount/${products.size} items checked",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Product list
                LazyColumn(
                    modifier = Modifier.weight(1f).tutorialHighlight("restockProductList", highlightState),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filteredProducts.isEmpty()) {
                        item {
                            Text(
                                "No products to check.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    }
                    items(filteredProducts, key = { it.id }) { product ->
                        CorrectionItem(
                            product = product,
                            initialActual = restockTemp.corrections
                                .find { it.productEntityId == product.id }
                                ?.newQty ?: product.quantity,
                            onActualChange = { newQty ->
                                viewModel.applyCorrection(
                                    Correction(
                                        productId = product.id.toString(),
                                        productEntityId = product.id,
                                        oldQty = product.quantity,
                                        newQty = newQty
                                    )
                                )
                            }
                        )
                    }
                }

                // Continue button
                Button(
                    onClick = {
                        viewModel.applyCorrectionsToProducts()
                        viewModel.setRestockStep(2)
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).tutorialHighlight("restockContinueBtn", highlightState),
                    shape = RoundedCornerShape(12.dp),
                    enabled = true
                ) {
                    Text("Continue to Purchases →")
                }

                // Cancel button
                TextButton(
                    onClick = {
                        viewModel.cancelRestock()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Restock", color = MaterialTheme.colorScheme.error)
                }

            } else {
                // ═══════════════════════════════════════════════
                // STEP 2: Record Purchases
                // ═══════════════════════════════════════════════
                Text(
                    text = "Step 2: Record Purchases",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp).tutorialHighlight("restockStep2Section", highlightState)
                )
                Text(
                    text = "Search for a product, enter cost per unit and quantity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Product search + cost/qty form
                OutlinedTextField(
                    value = purchaseSearch,
                    onValueChange = { purchaseSearch = it },
                    placeholder = { Text("Search product...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Suggestions dropdown
                if (filteredPurchaseProducts.isNotEmpty() && purchaseSearch.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            filteredPurchaseProducts.forEach { product ->
                                Surface(
                                    onClick = {
                                        purchaseSearch = product.name
                                        selectedProductId = product.id
                                        purchaseCost = product.costPrice.toString()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${product.name} — ₱${String.format("%,.2f", product.costPrice)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cost + Qty row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchaseCost,
                        onValueChange = { purchaseCost = it },
                        label = { Text("Cost/unit (₱)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = purchaseQty,
                        onValueChange = { purchaseQty = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Add Item button
                Button(
                    onClick = {
                        val name = purchaseSearch.trim()
                        val cost = purchaseCost.toDoubleOrNull() ?: 0.0
                        val qty = purchaseQty.toIntOrNull() ?: 1
                        if (name.isBlank() || cost <= 0 || qty <= 0) {
                            scope.launch { snackbarHostState.showSnackbar("Please fill in all fields correctly.") }
                            return@Button
                        }
                        viewModel.addPurchaseToTemp(
                            PurchaseEntry(
                                productId = selectedProductId?.toString(),
                                productEntityId = selectedProductId ?: 0,
                                productName = name,
                                costPerUnit = cost,
                                qtyAdded = qty,
                                totalCost = cost * qty
                            )
                        )
                        purchaseSearch = ""
                        purchaseCost = ""
                        purchaseQty = "1"
                        selectedProductId = null
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Item")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Purchase list
                val purchases = restockTemp.purchases
                LazyColumn(
                    modifier = Modifier.weight(1f).tutorialHighlight("restockPurchaseList", highlightState),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (purchases.isEmpty()) {
                        item {
                            Text(
                                "No items added yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    }
                    itemsIndexed(purchases) { index, item ->
                        PurchaseItem(
                            entry = item,
                            onRemove = { viewModel.removePurchaseFromTemp(index) }
                        )
                    }

                    // Total row
                    if (purchases.isNotEmpty()) {
                        item {
                            val total = purchases.sumOf { it.totalCost }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Green50)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Spent", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "₱${String.format("%,.2f", total)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Green700
                                    )
                                }
                            }
                        }
                    }
                }

                // Done + Cancel buttons
                Button(
                    onClick = {
                        viewModel.completeRestock()
                        scope.launch { snackbarHostState.showSnackbar("Restock saved! Inventory updated.") }
                        onComplete()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).tutorialHighlight("restockDoneBtn", highlightState),
                    shape = RoundedCornerShape(12.dp),
                    enabled = purchases.isNotEmpty()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Done ✓")
                }

                TextButton(
                    onClick = {
                        viewModel.cancelRestock()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Restock", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StepDot(active: Boolean, label: String) {
    val bg = if (active) Green500 else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(bg, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun CorrectionItem(
    product: Product,
    initialActual: Int,
    onActualChange: (Int) -> Unit
) {
    var actualText by remember(product.id, initialActual) { mutableStateOf(initialActual.toString()) }
    val actual = actualText.toIntOrNull() ?: 0
    val diff = actual - product.quantity
    val diffColor = when {
        diff == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        diff < 0 -> Color(0xFFDC2626)
        else -> Green600
    }
    val diffLabel = when {
        diff == 0 -> "✅ Matches app"
        diff < 0 -> "🔴 ${-diff} unrecorded sales"
        else -> "🎉 +$diff extra found"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "App says: ${product.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        " → ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = actualText,
                        onValueChange = { actualText = it; onActualChange(it.toIntOrNull() ?: 0) },
                        modifier = Modifier.width(70.dp).height(48.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Text(
                text = diffLabel,
                style = MaterialTheme.typography.labelSmall,
                color = diffColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun PurchaseItem(entry: PurchaseEntry, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "Qty: ${entry.qtyAdded} × ₱${String.format("%,.2f", entry.costPerUnit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "₱${String.format("%,.2f", entry.totalCost)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Green700
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Preview(showBackground = true, name = "Restock Screen")
@Composable
fun RestockScreenPreview() {
    SariSariSmartTheme {
        RestockScreen(
            viewModel = remember { AppViewModel() },
            onBack = {},
            onComplete = {}
        )
    }
}
