package com.example.sari_sari_smart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.sari_sari_smart.ui.localization.AppSettings
import com.example.sari_sari_smart.ui.localization.LocalLanguage
import com.example.sari_sari_smart.ui.localization.LocalTextScale
import com.example.sari_sari_smart.ui.navigation.NavGraph
import com.example.sari_sari_smart.ui.theme.SariSariSmartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appSettings = AppSettings(this)

        setContent {
            val langState = remember { mutableStateOf(appSettings.language) }
            val scaleState = remember { mutableStateOf(appSettings.getTextScaleFactor()) }

            CompositionLocalProvider(
                LocalLanguage provides langState,
                LocalTextScale provides scaleState
            ) {
                SariSariSmartTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        NavGraph(
                            navController = navController,
                            appSettings = appSettings
                        )
                    }
                }
            }
        }
    }

    // V2.70: foreground suppression — no notifications while the app is visible.
    override fun onStart() {
        super.onStart()
        com.example.sari_sari_smart.data.notifications.AppForegroundTracker.isForeground = true
    }

    override fun onStop() {
        super.onStop()
        com.example.sari_sari_smart.data.notifications.AppForegroundTracker.isForeground = false
    }
}
