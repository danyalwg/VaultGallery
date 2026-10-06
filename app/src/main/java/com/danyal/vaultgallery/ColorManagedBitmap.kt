package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import androidx.annotation.RequiresApi

/** Creates an edit surface with the source's colour-space tag instead of silently forcing sRGB. */
internal fun createEditBitmapLike(
    source: Bitmap,
    width: Int = source.width,
    height: Int = source.height,
    hasAlpha: Boolean = source.hasAlpha(),
): Bitmap = Bitmap.createBitmap(
    width.coerceAtLeast(1),
    height.coerceAtLeast(1),
    Bitmap.Config.ARGB_8888,
    hasAlpha,
    source.colorSpace ?: ColorSpace.get(ColorSpace.Named.SRGB),
).also { output ->
    if (Build.VERSION.SDK_INT >= 34 && source.hasGainmap()) {
        val original = source.gainmap ?: return@also
        if (Build.VERSION.SDK_INT < 35) {
            // Android 14 can attach an existing gain map but cannot construct a correctly
            // rescaled copy. Preserve it only when pixel geometry is unchanged.
            if (source.width == output.width && source.height == output.height) output.gainmap = original
            return@also
        }
        output.gainmap = scaledGainmap(source, output, original)
    }
}

@RequiresApi(35)
private fun scaledGainmap(source: Bitmap, output: Bitmap, original: android.graphics.Gainmap): android.graphics.Gainmap {
        val contents = original.gainmapContents
        val scaledWidth = (contents.width * output.width.toFloat() / source.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
        val scaledHeight = (contents.height * output.height.toFloat() / source.height.coerceAtLeast(1)).toInt().coerceAtLeast(1)
        val scaled = if (contents.width == scaledWidth && contents.height == scaledHeight) contents.copy(contents.config ?: Bitmap.Config.ARGB_8888, false)
        else Bitmap.createScaledBitmap(contents, scaledWidth, scaledHeight, true)
        return android.graphics.Gainmap(original, scaled)
}

internal fun createEditBitmapFromPixels(source: Bitmap, pixels: IntArray, width: Int, height: Int): Bitmap =
    createEditBitmapLike(source, width, height).apply { setPixels(pixels, 0, width, 0, 0, width, height) }
