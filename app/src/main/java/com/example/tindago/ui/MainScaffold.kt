package com.example.tindago.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.tindago.data.LocalSnackbarHost
import com.example.tindago.data.LocalSnackbarScope
import com.example.tindago.ui.components.MomentAppHeader
import com.example.tindago.ui.navigation.BottomNavBar
import com.example.tindago.ui.screens.AppViewModel
import com.example.tindago.ui.theme.TindaGoTheme
import kotlinx.coroutines.CoroutineScope

@Composable
fun MainScaffold(
    navController: NavController,
    currentRoute: String,
    appViewModel: AppViewModel? = null,
    onDevPanelTriggered: () -> Unit = {},
    onSaleFabClick: (() -> Unit)? = null,
    greeting: String = "",
    onTutorialClick: (() -> Unit)? = null,
    onInventoryClick: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
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
                if (greeting.isNotEmpty()) {
                    MomentAppHeader(
                        greeting = greeting,
                        onTutorialClick = onTutorialClick,
                        onInventoryClick = onInventoryClick
                    )
                }
            },
            bottomBar = { BottomNavBar(navController, appViewModel = appViewModel, onDevPanelTriggered = onDevPanelTriggered, onSaleFabClick = onSaleFabClick) }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                content(paddingValues)
            }
        }
    }
}

@Preview(showBackground = true, name = "Main Scaffold")
@Composable
fun MainScaffoldPreview() {
    TindaGoTheme {
        val navController = rememberNavController()
        MainScaffold(
            navController = navController,
            currentRoute = "home"
        ) { padding ->
            Text(
                text = "Content Area",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}
