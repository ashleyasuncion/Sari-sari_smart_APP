package com.example.sari_sari_smart.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sari_sari_smart.ui.components.PageTutorial
import com.example.sari_sari_smart.ui.components.pageTutorials
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.t
import androidx.compose.ui.tooling.preview.Preview
import com.example.sari_sari_smart.ui.theme.*

data class HowToItem(
    val titleKey: String,
    val contentKey: String,
    val icon: ImageVector
)

private val howToItems = listOf(
    HowToItem("recordingSale", "recordingSaleContent", Icons.Default.ShoppingCart),
    HowToItem("addingProducts", "addingProductsContent", Icons.Default.Inventory2),
    HowToItem("trackingDebts", "trackingDebtsContent", Icons.Default.People),
    HowToItem("viewingReports", "viewingReportsContent", Icons.Default.Assessment),
    HowToItem("endOfDayClosing", "endOfDayClosingContent", Icons.Default.NightShelter),
    HowToItem("usingReports", "usingReportsContent", Icons.Default.BarChart),
    HowToItem("appSettings", "appSettingsContent", Icons.Default.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onReplayTutorial: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunchPageTutorial: ((String) -> Unit)? = null
) {
    val langState = LocalLanguage.current
    val lang = langState.value
    var showAbout by remember { mutableStateOf(false) }
    var showContact by remember { mutableStateOf(false) }
    var selectedTutorial by remember { mutableStateOf<PageTutorial?>(null) }
    var tutDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        HelpMenuItem(
            icon = Icons.Default.PlayArrow,
            label = "replayTutorial".t(lang),
            onClick = onReplayTutorial
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Tutorial Selector Dropdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Green50),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = Green600, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("tutSelector".t(lang), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Green800)
                }
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = tutDropdownExpanded,
                    onExpandedChange = { tutDropdownExpanded = !tutDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedTutorial?.labelKey?.t(lang) ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("tutSelector".t(lang), color = Gray400) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tutDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = tutDropdownExpanded,
                        onDismissRequest = { tutDropdownExpanded = false }
                    ) {
                        pageTutorials.forEach { tut ->
                            DropdownMenuItem(
                                text = { Text(tut.labelKey.t(lang)) },
                                onClick = {
                                    selectedTutorial = tut
                                    tutDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        selectedTutorial?.let { onLaunchPageTutorial?.invoke(it.id) }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    enabled = selectedTutorial != null,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("tutLaunch".t(lang))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "howToUse".t(lang),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        howToItems.forEach { item ->
            HowToAccordionItem(item = item, lang = lang)
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        HelpMenuItem(
            icon = Icons.Default.Email,
            label = "contactInfo".t(lang),
            onClick = { showContact = true }
        )

        HelpMenuItem(
            icon = Icons.Default.Info,
            label = "aboutApp".t(lang),
            onClick = { showAbout = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("settings".t(lang))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = Green600) },
            title = { Text("aboutApp".t(lang)) },
            text = { Text("aboutContent".t(lang)) },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("close".t(lang))
                }
            }
        )
    }

    if (showContact) {
        AlertDialog(
            onDismissRequest = { showContact = false },
            icon = { Icon(Icons.Default.Email, contentDescription = null, tint = Green600) },
            title = { Text("contactInfo".t(lang)) },
            text = { Text("contactContent".t(lang)) },
            confirmButton = {
                TextButton(onClick = { showContact = false }) {
                    Text("close".t(lang))
                }
            }
        )
    }
}

@Composable
private fun HowToAccordionItem(item: HowToItem, lang: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = if (expanded) Green50 else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 0.dp else 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(item.icon, contentDescription = null, tint = Green600, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Text(item.titleKey.t(lang), style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (expanded) FontWeight.Bold else FontWeight.Medium, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null, tint = Gray500, modifier = Modifier.size(20.dp))
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp), color = Green100)
                    Text(item.contentKey.t(lang), style = MaterialTheme.typography.bodyMedium, color = Gray500)
                }
            }
        }
    }
}

@Composable
private fun HelpMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Green600, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true, name = "Help Screen")
@Composable
fun HelpScreenPreview() {
    SariSariSmartTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            HelpScreen(onReplayTutorial = {}, onOpenSettings = {})
        }
    }
}
