package com.example.sari_sari_smart.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.LocalTextScale
import com.example.sari_sari_smart.ui.localization.Strings
import com.example.sari_sari_smart.ui.theme.SariSariSmartTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MorningCheckScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: AppViewModel

    @Before
    fun setUp() {
        viewModel = AppViewModel()
        viewModel.resetAllData()
        viewModel.seedSampleData()
    }

    @Test
    fun morningGreeting_isDisplayed() {
        composeTestRule.setContent {
            val langState = mutableStateOf("en")
            SariSariSmartTheme {
                CompositionLocalProvider(
                    LocalLanguage provides langState,
                    LocalTextScale provides mutableStateOf(1f)
                ) {
                    MorningCheckScreen(
                        viewModel = viewModel,
                        onStartDay = {},
                        onNavigateToInventory = {},
                        onNavigateToDebts = {},
                        onNavigateToSettings = {},
                        onLaunchTutorial = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText(Strings.get("morningGreeting", "en"), substring = true).assertIsDisplayed()
    }

    @Test
    fun startDayButton_isDisplayed() {
        composeTestRule.setContent {
            val langState = mutableStateOf("en")
            SariSariSmartTheme {
                CompositionLocalProvider(
                    LocalLanguage provides langState,
                    LocalTextScale provides mutableStateOf(1f)
                ) {
                    MorningCheckScreen(
                        viewModel = viewModel,
                        onStartDay = {},
                        onNavigateToInventory = {},
                        onNavigateToDebts = {},
                        onNavigateToSettings = {},
                        onLaunchTutorial = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText(Strings.get("startDay", "en"), substring = true).assertIsDisplayed()
    }

    @Test
    fun stockWarningsAreDisplayed() {
        viewModel.seedSampleData()

        composeTestRule.setContent {
            val langState = mutableStateOf("fil")
            SariSariSmartTheme {
                CompositionLocalProvider(
                    LocalLanguage provides langState,
                    LocalTextScale provides mutableStateOf(1f)
                ) {
                    MorningCheckScreen(
                        viewModel = viewModel,
                        onStartDay = {},
                        onNavigateToInventory = {},
                        onNavigateToDebts = {},
                        onNavigateToSettings = {},
                        onLaunchTutorial = {}
                    )
                }
            }
        }

        // Stock warning should appear when there are out-of-stock items
        composeTestRule.onNodeWithText("stock", ignoreCase = true).assertIsDisplayed()
    }
}
