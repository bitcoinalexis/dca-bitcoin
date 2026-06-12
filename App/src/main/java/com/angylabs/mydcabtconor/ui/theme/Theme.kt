package com.angylabs.mydcabtconor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DcaColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = NavyDeep,
    secondary = BlueElectric,
    onSecondary = TextLight,
    tertiary = CyanAccent,
    onTertiary = NavyDeep,
    background = NavyDark,
    onBackground = TextLight,
    surface = NavySurface,
    onSurface = TextLight,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextDim,
    outline = NavyBorder,
    error = DangerRed
)

@Composable
fun MyDCABTCOnorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DcaColorScheme,
        typography = Typography,
        content = content
    )
}
