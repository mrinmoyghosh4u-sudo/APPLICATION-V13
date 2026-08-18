package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGold,
    onPrimary = Color.Black,
    primaryContainer = DarkGold,
    onPrimaryContainer = TextWhite,
    secondary = SecondaryGold,
    onSecondary = Color.Black,
    tertiary = BrightGold,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkCard,
    onSurface = TextWhite,
    surfaceVariant = DarkCardSecondary,
    onSurfaceVariant = TextGray,
    outline = DarkCardBorder,
    error = LossRed,
    onError = TextWhite
)

@Composable
fun KingKhanTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
