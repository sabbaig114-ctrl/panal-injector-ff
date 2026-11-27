package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val HackerDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = ObsidianBlack,
    primaryContainer = NeonGlowContainer,
    onPrimaryContainer = NeonGreen,
    secondary = CyberMint,
    onSecondary = ObsidianBlack,
    secondaryContainer = ElevatedSurface,
    onSecondaryContainer = IceWhite,
    tertiary = AmberPending,
    onTertiary = ObsidianBlack,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberPending,
    error = CrimsonReject,
    onError = IceWhite,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonReject,
    background = ObsidianBlack,
    onBackground = IceWhite,
    surface = TerminalSurface,
    onSurface = IceWhite,
    surfaceVariant = CardSurface,
    onSurfaceVariant = MatrixSage,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = TerminalSurface,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = ElevatedSurface,
    outline = BorderEmerald,
    outlineVariant = DimSlate
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun PanelInjectorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HackerDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
