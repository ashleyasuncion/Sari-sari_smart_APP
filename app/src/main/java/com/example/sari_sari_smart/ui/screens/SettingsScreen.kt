package com.example.sari_sari_smart.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.example.sari_sari_smart.data.LocalSnackbarHost
import com.example.sari_sari_smart.data.LocalSnackbarScope
import com.example.sari_sari_smart.ui.localization.AppSettings
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.LocalTextScale
import com.example.sari_sari_smart.ui.localization.t
import com.example.sari_sari_smart.ui.components.LocalTutorialHighlightState
import com.example.sari_sari_smart.ui.components.PageTutorial
import com.example.sari_sari_smart.ui.components.pageTutorials
import com.example.sari_sari_smart.ui.components.tutorialHighlight
import com.example.sari_sari_smart.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appSettings: AppSettings? = null,
    viewModel: AppViewModel? = null,
    onBack: () -> Unit,
    onResetComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val langState = LocalLanguage.current
    val scaleState = LocalTextScale.current

    val settings = appSettings ?: remember { AppSettings(context) }
    val highlightState = LocalTutorialHighlightState.current

    var storeName by remember { mutableStateOf(settings.storeName) }
    var ownerName by remember { mutableStateOf(settings.ownerName) }
    var language by remember { mutableStateOf(settings.language) }
    var selectedSize by remember { mutableStateOf(settings.textSize) }
    var defaultMarkup by remember { mutableStateOf(settings.defaultMarkup.toString()) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val snackbarHost = LocalSnackbarHost.current
    val snackbarScope = LocalSnackbarScope.current

    // Export/Import launchers
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && viewModel != null) {
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
        if (uri != null && viewModel != null) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("settings".t(settings.language), style = MaterialTheme.typography.titleLarge) },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Language
            SettingsSectionTitle("language".t(settings.language))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.tutorialHighlight("settingsLanguage", highlightState)) {
                FilterChip(
                    selected = language == "en",
                    onClick = { language = "en" },
                    label = { Text("English") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = language == "fil",
                    onClick = { language = "fil" },
                    label = { Text("Filipino") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text Size
            SettingsSectionTitle("textSize".t(settings.language))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("standard" to "standard".t(settings.language), "large" to "large".t(settings.language), "extra-large" to "extraLarge".t(settings.language)).forEach { (value, label) ->
                    FilterChip(
                        selected = selectedSize == value,
                        onClick = { selectedSize = value },
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Store Information
            SettingsSectionTitle("storeInformation".t(settings.language))
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("storeName".t(settings.language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("ownerName".t(settings.language)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    settings.language = language
                    settings.textSize = selectedSize
                    settings.storeName = storeName
                    settings.ownerName = ownerName
                    settings.defaultMarkup = defaultMarkup.toIntOrNull() ?: 20
                    langState.value = language
                    scaleState.value = settings.getTextScaleFactor()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text("saveSettings".t(settings.language), style = MaterialTheme.typography.titleSmall) }

            Spacer(modifier = Modifier.height(24.dp))

            // Tutorial Selector
            SettingsSectionTitle("tutorials".t(settings.language))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Green50),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "tutSelector".t(settings.language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Green800
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var selectedTutorial by remember { mutableStateOf<PageTutorial?>(null) }
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedTutorial?.labelKey?.t(settings.language) ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("tutSelector".t(settings.language), color = Gray400) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                pageTutorials.forEach { tut ->
                                    DropdownMenuItem(
                                        text = { Text(tut.labelKey.t(settings.language)) },
                                        onClick = {
                                            selectedTutorial = tut
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Button(
                            onClick = {
                                selectedTutorial?.let {
                                    // Launch the tutorial by navigating to its page
                                }
                            },
                            enabled = selectedTutorial != null,
                            modifier = Modifier.height(48.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("tutLaunch".t(settings.language))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Data Management
            SettingsSectionTitle("dataManagement".t(settings.language))
            OutlinedButton(
                onClick = {
                    if (viewModel != null) {
                        exportLauncher.launch("sari-sari-smart-data.json")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("exportData".t(settings.language)) }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    if (viewModel != null) {
                        importLauncher.launch(arrayOf("application/json"))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("importData".t(settings.language)) }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Red600)
            ) { Text("resetData".t(settings.language)) }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("areYouSure".t(settings.language)) },
            text = { Text("confirmReset".t(settings.language)) },
            confirmButton = {
                Button(
                    onClick = {
                        settings.clearAll()
                        showResetConfirm = false
                        // Reset global state
                        langState.value = "en"
                        scaleState.value = 1f
                        onResetComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) { Text("confirm".t(settings.language)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("cancel".t(settings.language)) }
            }
        )
    }
}

@Preview(showBackground = true, name = "Settings Screen")
@Composable
fun SettingsScreenPreview() {
    val context = LocalContext.current
    SariSariSmartTheme {
        SettingsScreen(
            appSettings = AppSettings(context),
            onBack = {}
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = Green800,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
