package com.example.sari_sari_smart.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.sari_sari_smart.ui.localization.LocalTextScale

private val LightColorScheme = lightColorScheme(
    primary = Green600,
    onPrimary = Surface,
    primaryContainer = Green50,
    onPrimaryContainer = Green800,
    secondary = Amber500,
    onSecondary = Surface,
    secondaryContainer = Amber50,
    onSecondaryContainer = Amber600,
    tertiary = Blue500,
    onTertiary = Surface,
    tertiaryContainer = Blue50,
    onTertiaryContainer = Blue600,
    error = Red600,
    onError = Surface,
    errorContainer = Red50,
    onErrorContainer = Red700,
    background = Background,
    onBackground = Gray800,
    surface = Surface,
    onSurface = Gray800,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = Gray500,
    outline = Gray200,
    outlineVariant = Gray100
)

@Composable
fun SariSariSmartTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Green600.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // Read user text-size preference (provided by MainActivity) and apply globally
    val scale by LocalTextScale.current
    val scaledTypography = remember(scale) { createScaledTypography(scale) }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = scaledTypography,
        content = content
    )
}
