package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StoryCompanionColorScheme = darkColorScheme(
    primary = PrimaryViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF382375),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = SecondaryCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D5A),
    onSecondaryContainer = Color(0xFFB8F5FF),
    tertiary = TertiaryMagenta,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceHighlight
)

@Composable
fun StoryCompanionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StoryCompanionColorScheme,
        typography = Typography,
        content = content
    )
}
