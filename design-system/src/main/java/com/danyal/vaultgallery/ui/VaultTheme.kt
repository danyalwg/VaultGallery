package com.danyal.vaultgallery.ui

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val VaultBackground = Color.Black
val VaultSurface = Color(0xFF1B1B1E)
val VaultRaised = Color(0xFF27272B)
val VaultPrimary = Color(0xFFF5F5F7)
val VaultSecondary = Color(0xFF96969F)
val VaultBlue = Color(0xFF3F7DF4)
val VaultOrange = Color(0xFFF36A1D)
val VaultSecure = VaultBlue

data class VaultSpacingTokens(
    val hairline: Dp = 1.dp,
    val compact: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 18.dp,
    val section: Dp = 24.dp,
    val touchTarget: Dp = 48.dp,
)

data class VaultComponentTokens(
    val albumRadius: Dp = 18.dp,
    val panelRadius: Dp = 24.dp,
    val dialogRadius: Dp = 28.dp,
    val compactControlRadius: Dp = 12.dp,
)

data class VaultMotionTokens(
    val reduced: Boolean,
    val instantMs: Int = 0,
    val fastMs: Int = 90,
    val standardMs: Int = 180,
    val deliberateMs: Int = 280,
) {
    fun duration(normalMs: Int): Int = if (reduced) instantMs else normalMs
}

val LocalVaultSpacing = staticCompositionLocalOf { VaultSpacingTokens() }
val LocalVaultComponents = staticCompositionLocalOf { VaultComponentTokens() }
val LocalVaultMotion = staticCompositionLocalOf { VaultMotionTokens(reduced = false) }

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

private val VaultShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val GalleryTypography = Typography().copy(
    displayLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 48.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 42.sp, lineHeight = 50.sp),
    displaySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

@Composable
fun VaultTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val animationsEnabled = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }.getOrDefault(true)
    CompositionLocalProvider(
        LocalVaultSpacing provides VaultSpacingTokens(),
        LocalVaultComponents provides VaultComponentTokens(),
        LocalVaultMotion provides VaultMotionTokens(reduced = !animationsEnabled),
    ) {
        MaterialTheme(colorScheme = Colors, shapes = VaultShapes, typography = GalleryTypography) {
            Surface(color = VaultBackground, contentColor = VaultPrimary, content = content)
        }
    }
}
