package com.danyal.vaultgallery.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val VaultBackground = Color.Black
val VaultSurface = Color(0xFF1B1B1E)
val VaultRaised = Color(0xFF29292D)
val VaultPrimary = Color(0xFFF7F7FA)
val VaultSecondary = Color(0xFF9A9AA2)
val VaultBlue = Color(0xFF4A86F7)
val VaultOrange = Color(0xFFF36A1D)
val VaultSecure = Color(0xFF7EC8B2)

private val Colors = darkColorScheme(
    primary = VaultBlue,
    onPrimary = Color.White,
    secondary = VaultSecure,
    background = VaultBackground,
    onBackground = VaultPrimary,
    surface = VaultSurface,
    onSurface = VaultPrimary,
    surfaceVariant = VaultRaised,
    onSurfaceVariant = VaultSecondary,
    outline = Color(0xFF3A3A3E),
    error = Color(0xFFFF6B6B),
)

@Composable
fun VaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = MaterialTheme.typography.copy(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 31.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 21.sp, lineHeight = 27.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp),
        ),
        content = content,
    )
}
