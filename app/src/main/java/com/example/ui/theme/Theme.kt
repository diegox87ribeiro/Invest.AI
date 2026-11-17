package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val InvestDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldPrimary,
    secondary = ElectricCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = CyanContainer,
    onSecondaryContainer = ElectricCyan,
    tertiary = AmberGold,
    onTertiary = ObsidianBg,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberGold,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = DeepNavySurface,
    onSurface = TextPrimary,
    surfaceVariant = CardElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = DeepNavySurface,
    surfaceContainer = CardElevated,
    surfaceContainerHigh = CardHighlight,
    outline = SubtleBorder,
    outlineVariant = SubtleBorder,
    error = CoralRed,
    onError = TextPrimary,
    errorContainer = CoralContainer,
    onErrorContainer = CoralRed
)

val InvestShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun InvestAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = InvestDarkColorScheme,
        typography = InvestTypography,
        shapes = InvestShapes,
        content = content
    )
}
