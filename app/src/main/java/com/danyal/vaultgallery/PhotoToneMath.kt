package com.danyal.vaultgallery

/** Pure tone-mapping helpers kept outside Compose so editor behavior can be regression tested. */
internal data class LevelTransform(val scale: Float, val offset: Float)

internal fun levelTransform(blackPoint: Float, whitePoint: Float): LevelTransform {
    val black = blackPoint.coerceIn(0f, 239f)
    val white = whitePoint.coerceIn(16f, 255f).coerceAtLeast(black + 16f)
    val scale = 255f / (white - black)
    return LevelTransform(scale = scale, offset = -black * scale)
}

internal data class WhiteBalanceGains(val red: Float, val green: Float, val blue: Float)

internal fun neutralWhiteBalance(red: Float, green: Float, blue: Float): WhiteBalanceGains {
    val safeRed = red.coerceAtLeast(1f)
    val safeGreen = green.coerceAtLeast(1f)
    val safeBlue = blue.coerceAtLeast(1f)
    val neutral = (safeRed + safeGreen + safeBlue) / 3f
    return WhiteBalanceGains(
        red = (neutral / safeRed).coerceIn(.55f, 1.8f),
        green = (neutral / safeGreen).coerceIn(.55f, 1.8f),
        blue = (neutral / safeBlue).coerceIn(.55f, 1.8f),
    )
}
