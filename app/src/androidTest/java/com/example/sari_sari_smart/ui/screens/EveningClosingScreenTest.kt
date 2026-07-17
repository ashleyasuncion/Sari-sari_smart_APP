package com.example.sari_sari_smart.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.LocalTextScale
import com.example.sari_sari_smart.ui.theme.SariSariSmartTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EveningClosingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: AppViewModel

    @Before
    fun setUp() {
        viewModel = AppViewModel()
        viewModel.resetAllData()
    }

    @Test
    fun closingTitle_isDisplayed() {
        composeTestRule.setContent {
            val langState = mutableStateOf("en")
            SariSariSmartTheme {
                CompositionLocalProvider(
                    LocalLanguage provides langState,
                    LocalTextScale provides mutableStateOf(1f)
                ) {
                    EveningClosingScreen(
                        viewModel = viewModel,
                        onComplete = {},
                        onBackToDay = {},
                        onNavigateToInventory = {},
                        onNavigateToDebts = {},
                        onLaunchTutorial = {}
                    )
                }
            }
        }

        // Closing title in English
        composeTestRule.onNodeWithText("Close the Store", substring = true).assertIsDisplayed()
    }

    @Test
    fun backToDayButton_isDisplayed() {
        composeTestRule.setContent {
            val langState = mutableStateOf("fil")
            SariSariSmartTheme {
                CompositionLocalProvider(
                    LocalLanguage provides langState,
                    LocalTextScale provides mutableStateOf(1f)
                ) {
                    EveningClosingScreen(
                        viewModel = viewModel,
                        onComplete = {},
                        onBackToDay = {},
                        onNavigateToInventory = {},
                        onNavigateToDebts = {},
                        onLaunchTutorial = {}
                    )
                }
            }
        }

        // Back to Day button in Filipino
        composeTestRule.onNodeWithText("Bumalik", substring = true).assertIsDisplayed()
    }
}
