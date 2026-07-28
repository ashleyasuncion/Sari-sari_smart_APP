package com.example.sari_sari_smart.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Base typography — increased to match the web app's enhanced font sizes. */
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 47.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 33.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 31.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 33.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 25.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Creates a [Typography] with all font sizes scaled by the given factor.
 * Used by [SariSariSmartTheme] to apply user text-size preference globally
 * (replaces the unused ScaledText approach).
 */
fun createScaledTypography(scale: Float): Typography {
    if (scale == 1.0f) return Typography

    return Typography(
        displayLarge = Typography.displayLarge.copy(
            fontSize = Typography.displayLarge.fontSize * scale,
            lineHeight = Typography.displayLarge.lineHeight * scale
        ),
        headlineLarge = Typography.headlineLarge.copy(
            fontSize = Typography.headlineLarge.fontSize * scale,
            lineHeight = Typography.headlineLarge.lineHeight * scale
        ),
        headlineMedium = Typography.headlineMedium.copy(
            fontSize = Typography.headlineMedium.fontSize * scale,
            lineHeight = Typography.headlineMedium.lineHeight * scale
        ),
        titleLarge = Typography.titleLarge.copy(
            fontSize = Typography.titleLarge.fontSize * scale,
            lineHeight = Typography.titleLarge.lineHeight * scale
        ),
        titleMedium = Typography.titleMedium.copy(
            fontSize = Typography.titleMedium.fontSize * scale,
            lineHeight = Typography.titleMedium.lineHeight * scale
        ),
        titleSmall = Typography.titleSmall.copy(
            fontSize = Typography.titleSmall.fontSize * scale,
            lineHeight = Typography.titleSmall.lineHeight * scale
        ),
        bodyLarge = Typography.bodyLarge.copy(
            fontSize = Typography.bodyLarge.fontSize * scale,
            lineHeight = Typography.bodyLarge.lineHeight * scale
        ),
        bodyMedium = Typography.bodyMedium.copy(
            fontSize = Typography.bodyMedium.fontSize * scale,
            lineHeight = Typography.bodyMedium.lineHeight * scale
        ),
        bodySmall = Typography.bodySmall.copy(
            fontSize = Typography.bodySmall.fontSize * scale,
            lineHeight = Typography.bodySmall.lineHeight * scale
        ),
        labelLarge = Typography.labelLarge.copy(
            fontSize = Typography.labelLarge.fontSize * scale,
            lineHeight = Typography.labelLarge.lineHeight * scale
        ),
        labelSmall = Typography.labelSmall.copy(
            fontSize = Typography.labelSmall.fontSize * scale,
            lineHeight = Typography.labelSmall.lineHeight * scale
        ),
        labelMedium = Typography.labelMedium.copy(
            fontSize = Typography.labelMedium.fontSize * scale,
            lineHeight = Typography.labelMedium.lineHeight * scale
        )
    )
}
