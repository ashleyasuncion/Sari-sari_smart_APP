package com.example.sari_sari_smart.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.sari_sari_smart.SariSariApp
import com.example.sari_sari_smart.data.AppRepository
import com.example.sari_sari_smart.ui.MainScaffold
import com.example.sari_sari_smart.ui.SupportScaffold
import com.example.sari_sari_smart.ui.components.*
import com.example.sari_sari_smart.ui.localization.AppSettings
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.t
import com.example.sari_sari_smart.ui.screens.*

// ── 14-step Main Tutorial (matching web prototype app2.6) ─────────────
// Maps to the Three Moment flow: Morning → Day → Closing → Inventory → Debts → Settings
val tutorialSteps = listOf(
    TutorialStep("tutorial1", "morning"),                                   // Welcome
    TutorialStep("tutorial2", "morning", "morningStockCard"),                 // Stock card
    TutorialStep("tutorial3", "morning", "morningDebtCard"),                  // Debt card
    TutorialStep("tutorial4", "morning", "startDayBtn"),                     // Start the Day
    TutorialStep("tutorial5", "day"),                                        // Welcome to Day Mode
    TutorialStep("tutorial6", "day", "dayStatsGrid"),                       // Stats summary
    TutorialStep("tutorial7", "day", "sellFab"),                            // Sell FAB
    TutorialStep("tutorial8", "closing"),                                    // Closing screen
    TutorialStep("tutorial9", "closing", "closingEarnings"),                // Cost + earnings
    TutorialStep("tutorial10", "closing", "completeDayBtn"),                 // Day Complete
    TutorialStep("tutorial11", "inventory", "stockSearchBar"),              // Inventory search
    TutorialStep("tutorial12", "inventory", "addStockBtn"),                  // Add Stock button
    TutorialStep("tutorial13", "debts", "totalDebtCard"),                   // Debts total
    TutorialStep("tutorial14", "settings", "settingsLanguage")               // Settings
)

@Composable
fun NavGraph(
    navController: NavHostController,
    appSettings: AppSettings
) {
    val context = LocalContext.current

    // Shared ViewModel for the app
    val appViewModel: AppViewModel = viewModel()

    // Initialize Room database
    val app = context.applicationContext as SariSariApp
    val repository = remember {
        AppRepository(
            productDao = app.database.productDao(),
            dailyEntryDao = app.database.dailyEntryDao(),
            specificSaleDao = app.database.specificSaleDao(),
            customerDebtDao = app.database.customerDebtDao(),
            endOfDayDao = app.database.endOfDayDao(),
            restockLogDao = app.database.restockLogDao()
        )
    }
    LaunchedEffect(Unit) {
        appViewModel.initRepository(repository)
    }

    // Dev Panel state
    var devPanelVisible by remember { mutableStateOf(false) }
    val showDevPanel: () -> Unit = { devPanelVisible = true }

    // Sale sheet state (shown via FAB on Day mode)
    var saleSheetVisible by remember { mutableStateOf(false) }
    val openSaleSheet: () -> Unit = { saleSheetVisible = true }
    val closeSaleSheet: () -> Unit = { saleSheetVisible = false }

    // Tutorial state
    var tutorialActive by remember { mutableStateOf(false) }
    var tutorialStep by remember { mutableIntStateOf(0) }
    var tutorialReplay by remember { mutableStateOf(false) }
    var pageTutorial by remember { mutableStateOf<PageTutorial?>(null) }

    // Session guard: tutorial only fires on fresh app launch, not every navigation to MORNING
    var tutorialLaunchedThisSession by remember { mutableStateOf(false) }

    // Tutorial highlight state — tracks bounds of elements for highlight frames
    val highlightState = remember { TutorialHighlightState() }

    fun getCurrentTutorialSteps(): List<TutorialStep> {
        return pageTutorial?.let { pt ->
            (1..pt.stepCount).map { i ->
                TutorialStep("${pt.stepsKeyPrefix}$i", pt.page)
            }
        } ?: tutorialSteps
    }

    fun advanceTutorial() {
        val steps = getCurrentTutorialSteps()
        val next = tutorialStep + 1
        if (next >= steps.size) {
            tutorialActive = false; tutorialReplay = false; pageTutorial = null
            appSettings.hasCompletedTutorial = true
            return
        }
        val nextStep = steps[next]
        val currentRoute = navController.currentDestination?.route ?: ""
        val stepPage = when (currentRoute) {
            Routes.MORNING -> "morning"
            Routes.DAY -> "day"
            Routes.CLOSING -> "closing"
            Routes.INVENTORY -> "inventory"
            Routes.DEBTS -> "debts"
            Routes.HELP -> "help"
            Routes.SETTINGS -> "settings"
            Routes.ADD_STOCK -> "add_stock"
            Routes.NEW_DEBT -> "new_debt"
            Routes.RESTOCK -> "restock"
            else -> "morning"
        }
        if (nextStep.page != stepPage) {
            val route = when (nextStep.page) {
                "morning" -> Routes.MORNING; "day" -> Routes.DAY; "closing" -> Routes.CLOSING
                "inventory" -> Routes.INVENTORY; "debts" -> Routes.DEBTS
                "help" -> Routes.HELP; "settings" -> Routes.SETTINGS; "add_stock" -> Routes.addStock()
                "new_debt" -> Routes.NEW_DEBT; "restock" -> Routes.RESTOCK
                else -> Routes.MORNING
            }
            tutorialStep = next
            navController.navigate(route) {
                popUpTo(Routes.MORNING) { saveState = true }
                launchSingleTop = true
            }
        } else {
            tutorialStep = next
        }
    }

    fun startPageTutorial(tutorialId: String) {
        val tut = pageTutorials.find { it.id == tutorialId }
        if (tut != null) {
            pageTutorial = tut; tutorialActive = true; tutorialStep = 0; tutorialReplay = true
            val route = when (tut.page) {
                "morning" -> Routes.MORNING; "day" -> Routes.DAY; "closing" -> Routes.CLOSING
                "inventory" -> Routes.INVENTORY; "debts" -> Routes.DEBTS
                "help" -> Routes.HELP; "settings" -> Routes.SETTINGS; "add_stock" -> Routes.addStock()
                "new_debt" -> Routes.NEW_DEBT; "restock" -> Routes.RESTOCK
                else -> Routes.MORNING
            }
            navController.navigate(route) {
                popUpTo(Routes.MORNING) { saveState = true }; launchSingleTop = true
            }
        }
    }

    fun startTutorial(replay: Boolean = false) {
        pageTutorial = null; tutorialActive = true; tutorialStep = 0; tutorialReplay = replay
        highlightState.clear()
    }

    fun endTutorial() {
        tutorialActive = false; tutorialReplay = false; pageTutorial = null
        appSettings.hasCompletedTutorial = true
        highlightState.clear()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Provide highlight state to all screen composables
        CompositionLocalProvider(LocalTutorialHighlightState provides highlightState) {
            NavHost(navController = navController, startDestination = Routes.SPLASH) {
            // ── App flow ──────────────────────────────────────────
            composable(Routes.SPLASH) {
                SplashScreen(
                    onNavigateToSetup = { navController.navigate(Routes.SETUP) { popUpTo(Routes.SPLASH) { inclusive = true } } },
                    onNavigateToHome = { navController.navigate(Routes.MORNING) { popUpTo(Routes.SPLASH) { inclusive = true } } }
                )
            }
            composable(Routes.SETUP) {
                SetupScreen(
                    onComplete = {
                        navController.navigate(Routes.MORNING) { popUpTo(Routes.SETUP) { inclusive = true } }
                    },
                    onTutorialReady = {
                        navController.navigate(Routes.DAY) { popUpTo(Routes.SETUP) { inclusive = true } }
                        startTutorial(false)
                    }
                )
            }

            // ── Three Moments ────────────────────────────────────
            composable(Routes.MORNING) {
                LaunchedEffect(Unit) {
                    // Tutorial auto-starts only on fresh app launch (not every navigation to MORNING)
                    // Session guard: tutorialLaunchedThisSession prevents re-trigger on nav back to MORNING
                    // First-ever launch: launchCount == 1 → no skip button (isReplay = false)
                    // Subsequent launches: launchCount > 1 → skip button shown (isReplay = true)
                    if (appSettings.hasCompletedSetup && !tutorialActive && !tutorialLaunchedThisSession) {
                        tutorialLaunchedThisSession = true
                        appSettings.launchCount++
                        val isFirstLaunch = appSettings.launchCount == 1
                        startTutorial(replay = !isFirstLaunch)
                    }
                }
                val lang = LocalLanguage.current.value
                val ownerName = appSettings.ownerName
                val greetingText = "greeting".t(lang) + " " + ownerName + " \uD83D\uDC4B"
                MainScaffold(
                    navController = navController,
                    currentRoute = Routes.MORNING,
                    appViewModel = appViewModel,
                    onDevPanelTriggered = showDevPanel,
                    greeting = greetingText,
                    onTutorialClick = { startPageTutorial("home") },
                    onInventoryClick = { navController.navigate(Routes.INVENTORY) }
                ) {
                    MorningCheckScreen(
                        viewModel = appViewModel,
                        onStartDay = {
                            appViewModel.archiveDaySales()
                            appViewModel.openDay()
                            navController.navigate(Routes.DAY) {
                                popUpTo(Routes.MORNING) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToClosing = {
                            navController.navigate(Routes.CLOSING)
                        },
                        onEditClosing = {
                            appViewModel.reopenClosing()
                            navController.navigate(Routes.CLOSING)
                        },
                        onNavigateToInventory = { navController.navigate(Routes.INVENTORY) },
                        onNavigateToDebts = { navController.navigate(Routes.DEBTS) },
                        onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                        onNavigateToRestock = { navController.navigate(Routes.RESTOCK) },
                        onLaunchTutorial = { startPageTutorial("home") }
                    )
                }
            }

            composable(Routes.DAY) {
                val lang = LocalLanguage.current.value
                val ownerName = appSettings.ownerName
                val greetingText = "greeting".t(lang) + " " + ownerName + " \uD83D\uDC4B"
                MainScaffold(
                    navController = navController,
                    currentRoute = Routes.DAY,
                    appViewModel = appViewModel,
                    onDevPanelTriggered = showDevPanel,
                    onSaleFabClick = openSaleSheet,
                    greeting = greetingText,
                    onTutorialClick = { startPageTutorial("sales") },
                    onInventoryClick = { navController.navigate(Routes.INVENTORY) }
                ) {
                    DayModeScreen(
                        viewModel = appViewModel,
                        onCloseStore = { navController.navigate(Routes.CLOSING) },
                        onNavigateToInventory = { navController.navigate(Routes.INVENTORY) },
                        onOpenSaleSheet = openSaleSheet,
                        onLaunchTutorial = { startPageTutorial("sales") }
                    )
                }
            }

            composable(Routes.CLOSING) {
                val lang = LocalLanguage.current.value
                val ownerName = appSettings.ownerName
                val greetingText = "greeting".t(lang) + " " + ownerName + " \uD83D\uDC4B"
                MainScaffold(
                    navController = navController,
                    currentRoute = Routes.CLOSING,
                    appViewModel = appViewModel,
                    onDevPanelTriggered = showDevPanel,
                    greeting = greetingText,
                    onTutorialClick = { startPageTutorial("eod") },
                    onInventoryClick = { navController.navigate(Routes.INVENTORY) }
                ) {
                    EveningClosingScreen(
                        viewModel = appViewModel,
                        onComplete = {
                            navController.navigate(Routes.MORNING) {
                                popUpTo(Routes.MORNING) { inclusive = true }
                            }
                        },
                        onBackToDay = { navController.popBackStack() },
                        onNavigateToInventory = { navController.navigate(Routes.INVENTORY) },
                        onNavigateToDebts = { navController.navigate(Routes.DEBTS) },
                        onLaunchTutorial = { startPageTutorial("eod") }
                    )
                }
            }

            // ── Support screens ──────────────────────────────────
            composable(Routes.INVENTORY) {
                val lang = LocalLanguage.current.value
                SupportScaffold(
                    navController = navController,
                    currentRoute = Routes.INVENTORY,
                    appViewModel = appViewModel,
                    onDevPanelTriggered = showDevPanel,
                    headerTitle = "inventory".t(lang),
                    onBackClick = { navController.popBackStack() },
                    onTutorialClick = { startPageTutorial("stock") },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) }
                ) {
                    StocksScreen(
                        viewModel = appViewModel,
                        onAddStock = { navController.navigate(Routes.addStock()) },
                        onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                        onLaunchTutorial = { startPageTutorial("stock") },
                        onStartRestockDay = { navController.navigate(Routes.RESTOCK) }
                    )
                }
            }

            composable(Routes.ADD_STOCK) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")?.toIntOrNull()
                AddStockScreen(
                    viewModel = appViewModel,
                    productId = productId?.let { if (it >= 0) it else null },
                    defaultMarkup = appSettings.defaultMarkup,
                    onBack = { navController.popBackStack() },
                    onSaved = { }
                )
            }

            composable(Routes.DEBTS) {
                val lang = LocalLanguage.current.value
                SupportScaffold(
                    navController = navController,
                    currentRoute = Routes.DEBTS,
                    appViewModel = appViewModel,
                    onDevPanelTriggered = showDevPanel,
                    headerTitle = "debts".t(lang),
                    onBackClick = { navController.popBackStack() },
                    onTutorialClick = { startPageTutorial("debt") },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) }
                ) {
                    DebtsScreen(
                        viewModel = appViewModel,
                        onNewDebt = { navController.navigate(Routes.NEW_DEBT) },
                        onDebtClick = { debtId -> navController.navigate(Routes.customerDebtDetail(debtId)) },
                        onNavigateToReports = { navController.navigate(Routes.MORNING) },
                        onLaunchTutorial = { startPageTutorial("debt") }
                    )
                }
            }

            composable(Routes.NEW_DEBT) {
                NewDebtScreen(
                    viewModel = appViewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { }
                )
            }

            composable(Routes.CUSTOMER_DEBT_DETAIL) { backStackEntry ->
                val debtId = backStackEntry.arguments?.getString("debtId")?.toIntOrNull() ?: 0
                CustomerDebtDetailScreen(
                    viewModel = appViewModel,
                    debtId = debtId,
                    onBack = { navController.popBackStack() },
                    onRecordPayment = { id -> navController.navigate(Routes.recordPayment(id)) }
                )
            }

            composable(Routes.RECORD_PAYMENT) { backStackEntry ->
                val debtId = backStackEntry.arguments?.getString("debtId")?.toIntOrNull() ?: 0
                RecordPaymentScreen(
                    viewModel = appViewModel,
                    debtId = debtId,
                    onBack = { navController.popBackStack() },
                    onPaymentSaved = { navController.popBackStack() }
                )
            }

            composable(Routes.PRODUCT_DETAIL) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")?.toIntOrNull() ?: 0
                ProductDetailScreen(
                    viewModel = appViewModel,
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.addStock(id)) },
                    onRestock = { id -> navController.navigate(Routes.addStock(id)) },
                    onDeleted = { navController.popBackStack() }
                )
            }

            composable(Routes.HELP) {
                HelpScreen(
                    onReplayTutorial = { startTutorial(true) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onLaunchPageTutorial = { tutId -> startPageTutorial(tutId) }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    appSettings = appSettings,
                    viewModel = appViewModel,
                    onBack = { navController.popBackStack() },
                    onResetComplete = {
                        navController.navigate(Routes.SPLASH) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── Reports Screen ────────────────────────────────────
            composable(Routes.REPORTS) {
                ReportsScreen(
                    viewModel = appViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Restock Screen ────────────────────────────────────
            composable(Routes.RESTOCK) {
                RestockScreen(
                    viewModel = appViewModel,
                    onBack = { navController.popBackStack() },
                    onComplete = {
                        navController.navigate(Routes.INVENTORY) {
                            popUpTo(Routes.INVENTORY) { inclusive = true }
                        }
                    },
                    onNavigateToInventory = {
                        navController.navigate(Routes.INVENTORY) {
                            popUpTo(Routes.INVENTORY) { inclusive = true }
                        }
                    }
                )
            }
        }
        } // end CompositionLocalProvider
    }        // Tutorial overlay on top — with highlight frame support
        val currentSteps = getCurrentTutorialSteps()
        if (tutorialActive && tutorialStep < currentSteps.size) {
            val step = currentSteps[tutorialStep]
            TutorialOverlay(
                isActive = tutorialActive,
                currentStep = tutorialStep,
                totalSteps = currentSteps.size,
                isReplay = tutorialReplay,
                step = step,
                highlightState = highlightState,
                onNext = { advanceTutorial() },
                onSkip = { endTutorial() },
                onFinish = { endTutorial() }
            )
        }

    // Developer Panel
    DeveloperPanel(visible = devPanelVisible, onDismiss = { devPanelVisible = false }, viewModel = appViewModel)

    // Sale sheet (shown on Day mode)
    if (saleSheetVisible) {
        SaleBottomSheet(
            viewModel = appViewModel,
            onDismiss = closeSaleSheet,
            onSaved = closeSaleSheet
        )
    }
}
