package com.etyhan.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 纸、墨、朱砂：一个“韩纸词典”的配色。 */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF8C1D18),
    onPrimary = Color(0xFFFFF8F1),
    primaryContainer = Color(0xFFF4DDD7),
    onPrimaryContainer = Color(0xFF3C0906),
    secondary = Color(0xFF33566B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE8EF),
    onSecondaryContainer = Color(0xFF0D2533),
    tertiary = Color(0xFF6B5A3E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEFE5D0),
    onTertiaryContainer = Color(0xFF2A2113),
    background = Color(0xFFF6F1E7),
    onBackground = Color(0xFF1B1A17),
    surface = Color(0xFFFFFCF6),
    onSurface = Color(0xFF1B1A17),
    surfaceVariant = Color(0xFFEDE6D8),
    onSurfaceVariant = Color(0xFF5B5448),
    surfaceContainerLow = Color(0xFFFCF8EF),
    surfaceContainer = Color(0xFFF8F3E8),
    surfaceContainerHigh = Color(0xFFF2ECDE),
    outline = Color(0xFFC9BFAE),
    outlineVariant = Color(0xFFE0D7C7),
    error = Color(0xFF8F2C24),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF2E2A24),
    inverseOnSurface = Color(0xFFF4EEE2),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE9B3A7),
    onPrimary = Color(0xFF4A0F0B),
    primaryContainer = Color(0xFF6B211B),
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFA8C4D6),
    onSecondary = Color(0xFF0C2533),
    secondaryContainer = Color(0xFF25404F),
    onSecondaryContainer = Color(0xFFD3E7F5),
    tertiary = Color(0xFFD6C4A4),
    onTertiary = Color(0xFF33280F),
    tertiaryContainer = Color(0xFF4A3D22),
    onTertiaryContainer = Color(0xFFF4E6C9),
    background = Color(0xFF14120F),
    onBackground = Color(0xFFEDE6D9),
    surface = Color(0xFF1C1916),
    onSurface = Color(0xFFEDE6D9),
    surfaceVariant = Color(0xFF2A2621),
    onSurfaceVariant = Color(0xFFC7BDAD),
    surfaceContainerLow = Color(0xFF1A1714),
    surfaceContainer = Color(0xFF211E1A),
    surfaceContainerHigh = Color(0xFF2A2621),
    outline = Color(0xFF4C453B),
    outlineVariant = Color(0xFF332F28),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    inverseSurface = Color(0xFFEDE6D9),
    inverseOnSurface = Color(0xFF2E2A24),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun EtyHanTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
