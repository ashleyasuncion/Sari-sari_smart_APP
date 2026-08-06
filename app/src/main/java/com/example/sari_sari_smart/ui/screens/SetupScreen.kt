package com.example.sari_sari_smart.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.t
import com.example.sari_sari_smart.ui.theme.Green600
import com.example.sari_sari_smart.ui.theme.Green800
import com.example.sari_sari_smart.ui.theme.SariSariSmartTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SetupScreen(
    onComplete: () -> Unit,
    onTutorialReady: () -> Unit = {}
) {
    val context = LocalContext.current
    val langState = LocalLanguage.current
    var storeName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf("en") }

    Box(
        modifier = Modifier.fillMaxSize().background(Green600)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🏪", fontSize = MaterialTheme.typography.displayLarge.fontSize)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "welcome".t(selectedLang),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                // Center-align so long translations (e.g. Filipino) stay centered
                // instead of defaulting to left alignment when they wrap.
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "setupPrompt".t(selectedLang),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(48.dp))

            // Language selector
            Text(
                text = "language".t(selectedLang),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedLang == "en",
                    onClick = { selectedLang = "en" },
                    label = { Text("English", color = MaterialTheme.colorScheme.onPrimary) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                        selectedContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedLang == "fil",
                    onClick = { selectedLang = "fil" },
                    label = { Text("Filipino", color = MaterialTheme.colorScheme.onPrimary) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f),
                        selectedContainerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                label = { Text("storeName".t(selectedLang)) },
                placeholder = { Text("storeNamePlaceholder".t(selectedLang)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                    focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("ownerName".t(selectedLang)) },
                placeholder = { Text("ownerNamePlaceholder".t(selectedLang)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                    focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val prefs = context.getSharedPreferences("sss_prefs", 0)
                    prefs.edit()
                        .putString("store_name", storeName.ifBlank { "My Store" })
                        .putString("owner_name", ownerName.ifBlank { "Owner" })
                        .putString("language", selectedLang)
                        .putBoolean("has_completed_setup", true)
                        .apply()
                    langState.value = selectedLang
                    onComplete()
                    onTutorialReady()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = Green800
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("getStarted".t(selectedLang), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Preview(showBackground = true, name = "Setup Screen")
@Composable
fun SetupScreenPreview() {
    SariSariSmartTheme {
        SetupScreen(
            onComplete = {},
            onTutorialReady = {}
        )
    }
}
