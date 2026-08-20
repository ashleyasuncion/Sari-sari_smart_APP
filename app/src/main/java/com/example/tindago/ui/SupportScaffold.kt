package com.example.tindago.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.example.tindago.data.LocalSnackbarHost
import com.example.tindago.data.LocalSnackbarScope
import com.example.tindago.ui.components.SupportAppHeader
import com.example.tindago.ui.navigation.BottomNavBar
import com.example.tindago.ui.screens.AppViewModel
import com.example.tindago.ui.theme.TindaGoTheme
import com.example.tindago.ui.theme.Gray400

/**
 * Support scaffold — wraps support screens (Inventory, Debts) with a
 * support-style bottom nav (Morning / Inventory / Debts).
 */
@Composable
fun SupportScaffold(
    navController: NavController,
    currentRoute: String,
    appViewModel: AppViewModel? = null,
    onDevPanelTriggered: () -> Unit = {},
    headerTitle: String = "",
    onBackClick: (() -> Unit)? = null,
    onTutorialClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(
        LocalSnackbarHost provides snackbarHostState,
        LocalSnackbarScope provides scope
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            },
            topBar = {
                if (headerTitle.isNotEmpty()) {
                    SupportAppHeader(
                        title = headerTitle,
                        onBackClick = onBackClick,
                        onTutorialClick = onTutorialClick,
                        onSettingsClick = onSettingsClick
                    )
                }
            },
            bottomBar = {
                BottomNavBar(
                    navController = navController,
                    appViewModel = appViewModel,
                    onDevPanelTriggered = onDevPanelTriggered
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                content(paddingValues)
            }
        }
    }
}

@Preview(showBackground = true, name = "Support Scaffold")
@Composable
fun SupportScaffoldPreview() {
    TindaGoTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Support Scaffold", style = MaterialTheme.typography.bodyLarge, color = Gray400)
            }
        }
    }
}
