package com.tara.dailyn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = DarkText,
    primaryContainer = BrandOrangeDeep,
    onPrimaryContainer = DarkText,
    secondary = BrandOrangeSecondary,
    onSecondary = DarkText,
    secondaryContainer = DarkCard,
    onSecondaryContainer = DarkText,
    tertiary = AchievementGold,
    onTertiary = LightText,
    tertiaryContainer = ColorTokens.DarkHeroSurface,
    onTertiaryContainer = DarkText,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkBackground,
    onSurface = DarkText,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkSubtext,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkCard,
    outline = DarkDivider,
    error = ErrorRed,
    onError = DarkText
)

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = LightCard,
    primaryContainer = BrandOrangeSecondary,
    onPrimaryContainer = LightText,
    secondary = BrandOrangeSecondary,
    onSecondary = LightText,
    secondaryContainer = LightCard,
    onSecondaryContainer = LightText,
    tertiary = AchievementGold,
    onTertiary = LightText,
    tertiaryContainer = ColorTokens.LightHeroSurface,
    onTertiaryContainer = LightText,
    background = LightBackground,
    onBackground = LightText,
    surface = LightBackground,
    onSurface = LightText,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightSubtext,
    surfaceContainer = LightCard,
    surfaceContainerHigh = LightCard,
    outline = LightDivider,
    error = ErrorRed,
    onError = LightCard
)

@Composable
fun DailynTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

object ColorTokens {
    val CompletedHabit = CompletedGreen
    val CurrentStreak = BrandOrange
    val Achievement = AchievementGold
    val MissedHabit = ErrorRed
    val LightHeroSurface = Color(0xFFFFEDD5)
    val DarkHeroSurface = Color(0xFF292524)
}
