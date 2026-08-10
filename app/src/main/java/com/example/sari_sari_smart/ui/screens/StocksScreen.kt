package com.example.sari_sari_smart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sari_sari_smart.data.Product
import com.example.sari_sari_smart.data.StockStatus
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.t
import androidx.compose.ui.tooling.preview.Preview
import com.example.sari_sari_smart.ui.theme.*
import com.example.sari_sari_smart.ui.theme.SariSariSmartTheme
import com.example.sari_sari_smart.ui.components.LocalTutorialHighlightState
import com.example.sari_sari_smart.ui.components.tutorialHighlight
import java.text.NumberFormat
import java.util.*

/**
 * STOCKS SCREEN — Inventory management.
 * Matches inventory.html from the web prototype exactly.
 */
@Composable
fun StocksScreen(
    viewModel: AppViewModel,
    onAddStock: () -> Unit,
    onProductClick: (Int) -> Unit = {},
    onLaunchTutorial: (() -> Unit)? = null,
    onStartRestockDay: (() -> Unit)? = null
) {
    val langState = LocalLanguage.current
    val lang = langState.value
    val fmt = remember { NumberFormat.getCurrencyInstance(Locale("en", "PH")) }
    val highlightState = LocalTutorialHighlightState.current

    val products by viewModel.products.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    // Category filter ('' = all). Products without a category only match 'all'
    // (web v2.59 renderManageInventory parity).
    var selectedCategory by remember { mutableStateOf("") }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        val searched = if (searchQuery.isNotBlank()) {
            viewModel.searchProducts(searchQuery)
        } else {
            products
        }
        val byCategory = if (selectedCategory.isNotBlank()) {
            viewModel.getProductsByCategory(selectedCategory)
                .filter { p -> searched.any { it.id == p.id } }
        } else {
            searched
        }
        byCategory.sortedBy { it.name }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .tutorialHighlight("inventoryList", highlightState),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Search bar ──────────────────────────────────────────────────
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("searchItems".t(lang)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .tutorialHighlight("stockSearchBar", highlightState),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium
            )
        }

        // ── Category filter chips (web v2.59 renderInventoryCatFilters parity) ──
        item {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == "",
                        onClick = { selectedCategory = "" },
                        label = { Text("catAll".t(lang)) }
                    )
                }
                items(com.example.sari_sari_smart.data.ProductCatalog.CATEGORIES) { key ->
                    FilterChip(
                        selected = selectedCategory == key,
                        onClick = { selectedCategory = key },
                        label = { Text(com.example.sari_sari_smart.ui.localization.Strings.productCategoryLabel(key, lang)) }
                    )
                }
            }
        }

        // ── Add Stock button ────────────────────────────────────────────
        item {
            Button(
                onClick = onAddStock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .tutorialHighlight("addStockBtn", highlightState),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("addStock".t(lang), style = MaterialTheme.typography.titleSmall)
            }
        }

        // ── Start Restock Day button ────────────────────────────────────
        if (onStartRestockDay != null) {
            item {
                OutlinedButton(
                    onClick = onStartRestockDay,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF59E0B) // accent/amber
                    )
                ) {
                    Text("\uD83D\uDE9A Start Restock Day \uD83D\uDE9A", style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        // ── Product list ────────────────────────────────────────────────
        if (filteredProducts.isEmpty() && searchQuery.isNotBlank()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\uD83D\uDD0D", style = MaterialTheme.typography.displayMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No items match your search.", style = MaterialTheme.typography.bodyMedium, color = Gray400)
                    }
                }
            }
        }

        items(filteredProducts, key = { it.id }) { product ->
            InventoryProductCard(
                product = product,
                fmt = fmt,
                lang = lang,
                onClick = { onProductClick(product.id) }
            )
        }

        if (filteredProducts.isEmpty() && searchQuery.isBlank()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("\uD83D\uDCE6", style = MaterialTheme.typography.displayMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("noStockItems".t(lang), style = MaterialTheme.typography.bodyMedium, color = Gray400)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun InventoryProductCard(
    product: Product,
    fmt: java.text.NumberFormat,
    lang: String,
    onClick: () -> Unit,
) {
    val bgColor = when (product.status) {
        StockStatus.PLENTY -> Green100
        StockStatus.LOW -> Amber100
        StockStatus.OUT_OF_STOCK -> Red100
    }
    val statusIcon = when (product.status) {
        StockStatus.PLENTY -> "\u2705"
        StockStatus.LOW -> "\u26A0\uFE0F"
        StockStatus.OUT_OF_STOCK -> "\uD83D\uDD34"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = bgColor
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(statusIcon, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Gray800
                )
                // Brand · size sub-line (web productSubline parity) — only when
                // a brand exists; an empty brand never falls back to size/unit.
                val subline = com.example.sari_sari_smart.ui.localization.Strings.productSubline(product, lang)
                if (subline.isNotEmpty()) {
                    Text(
                        subline,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Green700,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Text(
                    "${product.quantity} ${com.example.sari_sari_smart.ui.localization.Strings.productUnitLabel(product.unit, lang)} \u2022 ${fmt.format(product.sellingPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Details", tint = Gray400, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "Stocks Screen")
@Composable
fun StocksScreenPreview() {
    SariSariSmartTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            StocksScreen(
                viewModel = remember { AppViewModel() },
                onAddStock = {},
                onProductClick = {}
            )
        }
    }
}
