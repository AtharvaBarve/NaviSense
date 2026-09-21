package com.example.navisense.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightBackground = Color(0xFFF8FAFC)
val SurfaceWhite = Color(0xFFFFFFFF)
val PrimaryBlue = Color(0xFF146EF5)
val TextDarkNavy = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val BorderLight = Color(0xFFE2E8F0)

val SuccessGreen = Color(0xFF16A34A)
val SuccessBg = Color(0xFFDCFCE7)

val WarningYellow = Color(0xFFD97706)
val WarningBg = Color(0xFFFEF3C7)

val DangerRed = Color(0xFFDC2626)
val DangerBg = Color(0xFFFEE2E2)

private val NaviSenseLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EDFF),
    onPrimaryContainer = PrimaryBlue,
    background = LightBackground,
    onBackground = TextDarkNavy,
    surface = SurfaceWhite,
    onSurface = TextDarkNavy,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun NaviSenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NaviSenseLightColorScheme,
        content = content
    )
}
