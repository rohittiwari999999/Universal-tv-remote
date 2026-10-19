package com.omnitech.googletv.remote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val GoogleTvDark = Color(0xFF0B0F19)
val GoogleTvSurface = Color(0xFF131927)
val GoogleTvCard = Color(0xFF1C2438)
val GoogleTvCardBorder = Color(0xFF28344E)
val GoogleTvBlue = Color(0xFF4285F4)
val GoogleTvGreen = Color(0xFF34A853)
val GoogleTvYellow = Color(0xFFFBBC05)
val GoogleTvRed = Color(0xFFEA4335)
val GoogleTvTextPrimary = Color(0xFFF1F5F9)
val GoogleTvTextSecondary = Color(0xFF94A3B8)

private val DarkColorScheme = darkColorScheme(
    primary = GoogleTvBlue,
    secondary = GoogleTvGreen,
    tertiary = GoogleTvYellow,
    background = GoogleTvDark,
    surface = GoogleTvSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = GoogleTvTextPrimary,
    onSurface = GoogleTvTextPrimary
)

@Composable
fun GoogleTvRemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
