package com.example.sari_sari_smart.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sari_sari_smart.data.LocalSnackbarHost
import com.example.sari_sari_smart.data.LocalSnackbarScope
import com.example.sari_sari_smart.ui.screens.AppViewModel
import com.example.sari_sari_smart.ui.theme.Red500
import com.example.sari_sari_smart.ui.theme.Blue500
import com.example.sari_sari_smart.ui.theme.Green500
import com.example.sari_sari_smart.ui.theme.Amber500
import com.example.sari_sari_smart.ui.theme.SariSariSmartTheme
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Developer Panel — hidden utility accessible via double-tap Help in bottom nav.
 * Provides data viewers, test data generators, and reset actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperPanel(
    visible: Boolean,
    onDismiss: () -> Unit,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHost.current
    val snackbarScope = LocalSnackbarScope.current

    // State for sub-dialogs
    var showRawStateDialog by remember { mutableStateOf(false) }
    var showClearSelectedDialog by remember { mutableStateOf(false) }
    var showResetAllConfirm by remember { mutableStateOf(false) }

    // CSV Export launcher
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = viewModel.exportCsv()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                snackbarScope.launch { snackbarHost.showSnackbar("CSV exported successfully!") }
            } catch (e: Exception) {
                snackbarScope.launch { snackbarHost.showSnackbar("CSV export failed: ${e.message}") }
            }
        }
    }

    // Export/Import launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = viewModel.getRawStateJson().toString(2)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                snackbarScope.launch { snackbarHost.showSnackbar("Data exported successfully!") }
            } catch (e: Exception) {
                snackbarScope.launch { snackbarHost.showSnackbar("Export failed: ${e.message}") }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: return@rememberLauncherForActivityResult
                val obj = JSONObject(json)
                viewModel.importData(obj)
                snackbarScope.launch { snackbarHost.showSnackbar("Data imported successfully!") }
            } catch (e: Exception) {
                snackbarScope.launch { snackbarHost.showSnackbar("Import failed: ${e.message}") }
            }
        }
    }

    // Show sub-dialogs
    if (showRawStateDialog) {
        RawStateDialog(
            json = viewModel.getRawStateJson().toString(2),
            onDismiss = { showRawStateDialog = false }
        )
    }

    if (showClearSelectedDialog) {
        ClearSelectedDialog(
            onDismiss = { showClearSelectedDialog = false },
            onClear = { types ->
                viewModel.clearSelectedData(types)
                showClearSelectedDialog = false
                snackbarScope.launch { snackbarHost.showSnackbar("Selected data cleared.") }
            }
        )
    }

    if (showResetAllConfirm) {
        AlertDialog(
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Red500) },
            title = { Text("Reset ALL Data?") },
            text = { Text("This will permanently delete all your products, sales, debts, and settings. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllData()
                        showResetAllConfirm = false
                        onDismiss()
                        snackbarScope.launch { snackbarHost.showSnackbar("All data has been reset.") }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Red500)
                ) { Text("Reset All") }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllConfirm = false }) { Text("Cancel") }
            },
            onDismissRequest = { showResetAllConfirm = false }
        )
    }

    // Main bottom sheet
    if (visible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Text(
                    text = "Dev Tools",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // ── Section: Data Viewers ──
                SectionHeader("Data Viewers")
                DevActionItem(
                    icon = Icons.Default.Code,
                    label = "View Raw State",
                    desc = "See all app data as JSON",
                    onClick = { showRawStateDialog = true }
                )
                DevActionItem(
                    icon = Icons.Default.FileUpload,
                    label = "Export Data",
                    desc = "Save all data as JSON file",
                    onClick = { exportLauncher.launch("sari-sari-smart-data.json") }
                )
                DevActionItem(
                    icon = Icons.Default.FileDownload,
                    label = "Import Data",
                    desc = "Load data from JSON file (overwrites current)",
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
                DevActionItem(
                    icon = Icons.Default.TableChart,
                    label = "Export CSV",
                    desc = "Export data as CSV file for spreadsheets",
                    onClick = { csvExportLauncher.launch("sari-sari-smart-data.csv") }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // ── Section: Test Generators ──
                SectionHeader("Test Generators")
                DevActionItem(
                    icon = Icons.Default.ShoppingCart,
                    label = "Generate Test Sale",
                    desc = "Create a random sale from inventory",
                    onClick = {
                        viewModel.generateTestSale()
                        snackbarScope.launch { snackbarHost.showSnackbar("Test sale generated!") }
                    }
                )
                DevActionItem(
                    icon = Icons.Default.AccountBalance,
                    label = "Generate Test Debts",
                    desc = "Create 2-5 random customer debts",
                    onClick = {
                        val count = viewModel.generateTestDebts()
                        snackbarScope.launch { snackbarHost.showSnackbar("$count test debts created!") }
                    }
                )
                DevActionItem(
                    icon = Icons.Default.Dashboard,
                    label = "Bulk Add Items",
                    desc = "Add 15 random products to inventory",
                    onClick = {
                        val count = viewModel.bulkAddItems()
                        snackbarScope.launch { snackbarHost.showSnackbar("$count items added!") }
                    }
                )
                DevActionItem(
                    icon = Icons.Default.ContentPaste,
                    label = "Seed Sample Data",
                    desc = "Load default sample products & debts",
                    onClick = {
                        viewModel.seedSampleData()
                        snackbarScope.launch { snackbarHost.showSnackbar("Sample data loaded.") }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // ── Section: Restock ──
                SectionHeader("Restock")
                DevActionItem(
                    icon = Icons.Default.Inventory,
                    label = "Clear Restock Data",
                    desc = "Reset restock date & history",
                    onClick = {
                        viewModel.clearRestockData()
                        snackbarScope.launch { snackbarHost.showSnackbar("Restock data cleared.") }
                    }
                )
                DevActionItem(
                    icon = Icons.Default.CalendarToday,
                    label = "Set Restock Date to Today",
                    desc = "For testing the morning reminder",
                    onClick = {
                        viewModel.setRestockDateToday()
                        snackbarScope.launch { snackbarHost.showSnackbar("Restock date set to today.") }
                    }
                )
                DevActionItem(
                    icon = Icons.Default.History,
                    label = "View Restock Log",
                    desc = viewModel.viewRestockLogCount(),
                    onClick = {
                        snackbarScope.launch { snackbarHost.showSnackbar(viewModel.viewRestockLogCount()) }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // ── Section: Reset Actions ──
                SectionHeader("Reset Actions")
                DevActionItem(
                    icon = Icons.Default.RestartAlt,
                    label = "Reset Today's Sales",
                    desc = "Clear today's daily entry & specific sales",
                    onClick = {
                        viewModel.resetTodaySales()
                        snackbarScope.launch { snackbarHost.showSnackbar("Today's sales reset.") }
                    }
                )
                DevActionItem(
                    icon = Icons.Default.Inventory,
                    label = "Clear Inventory",
                    desc = "Remove all products from inventory",
                    onClick = {
                        viewModel.clearAllInventory()
                        snackbarScope.launch { snackbarHost.showSnackbar("Inventory cleared.") }
                    }
                )
                DevActionItem(
                    icon = Icons.Default.ClearAll,
                    label = "Clear Selected...",
                    desc = "Choose which data categories to clear",
                    onClick = { showClearSelectedDialog = true }
                )
                DevActionItem(
                    icon = Icons.Default.Warning,
                    label = "Reset ALL Data",
                    desc = "Full factory reset (irreversible)",
                    iconTint = Red500,
                    onClick = { showResetAllConfirm = true }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun DevActionItem(
    icon: ImageVector,
    label: String,
    desc: String,
    iconTint: Color = Green500,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Raw State Dialog ────────────────────────────────────────────────────

@Composable
private fun RawStateDialog(json: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Raw State (JSON)") },
        text = {
            Column {
                SelectionContainer {
                    Text(
                        text = json,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Raw State", json))
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy to Clipboard")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = {}
    )
}

// ── Clear Selected Dialog ───────────────────────────────────────────────

data class ClearableDataType(val key: String, val label: String, var checked: Boolean = false)

@Composable
private fun ClearSelectedDialog(
    onDismiss: () -> Unit,
    onClear: (List<String>) -> Unit
) {
    val types = remember {
        mutableStateListOf(
            ClearableDataType("products", "Products"),
            ClearableDataType("sales", "Sales & Daily Entries"),
            ClearableDataType("debts", "Debts & Customers"),
            ClearableDataType("eod", "End-of-Day Data")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear Selected Data") },
        text = {
            Column {
                Text("Select data types to clear:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                types.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val idx = types.indexOf(item)
                                types[idx] = item.copy(checked = !item.checked)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = item.checked,
                            onCheckedChange = { checked ->
                                val idx = types.indexOf(item)
                                types[idx] = item.copy(checked = checked)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = types.filter { it.checked }.map { it.key }
                    onClear(selected)
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Red500)
            ) { Text("Clear Selected") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true, name = "Developer Panel")
@Composable
fun DeveloperPanelPreview() {
    SariSariSmartTheme {
        DeveloperPanel(
            visible = true,
            onDismiss = {},
            viewModel = remember { AppViewModel() }
        )
    }
}
