package com.motoclubgps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFF121212)
val AppSurface = Color(0xFF1E1E1E)
val AppSurfaceVariant = Color(0xFF292929)
val AppOrange = Color(0xFFFF6F00)
val AppRed = Color(0xFFD32F2F)
val AppGreen = Color(0xFF4CAF50)
val AppTextPrimary = Color(0xFFFFFFFF)
val AppTextSecondary = Color(0xFFB8B8B8)

private val Colors = darkColorScheme(
    primary = AppOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5B2500),
    onPrimaryContainer = Color(0xFFFFDBC7),
    secondary = AppGreen,
    onSecondary = Color.White,
    error = AppRed,
    onError = Color.White,
    background = AppBackground,
    onBackground = AppTextPrimary,
    surface = AppSurface,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = AppTextSecondary,
    outline = Color(0xFF5F5F5F),
)

@Composable
fun MotoClubGpsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        content = content,
    )
}
